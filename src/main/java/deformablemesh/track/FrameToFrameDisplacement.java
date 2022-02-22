package deformablemesh.track;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.util.ColorSuggestions;
import deformablemesh.util.Vector3DOps;
import deformablemesh.util.actions.UndoableActions;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import lightgraph.Graph;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FrameToFrameDisplacement {
    List<Track> tracks;

    Map<Integer, Track> lastAdded = new HashMap<>();
    Map<Track, List<Mapping>> results = new HashMap<>();
    public double JI_CUTOFF = 0.001;
    boolean followCenterOfMass = true;
    final int first, last;
    public FrameToFrameDisplacement(int first, int last){
        this.first = first;
        this.last = last;
    }

    List<Track> toChange = new ArrayList<>();
    List<Map<Integer, DeformableMesh3D>> changes = new ArrayList<>();
    List<Track> toRemove = new ArrayList<>();
    public static FrameToFrameDisplacement trackFrameForward(List<Track> tracks, int frame){
        List<Track> entering = tracks.stream().filter(
                track -> track.getLastFrame() == frame
            ).collect(Collectors.toList());
        List<Track> starting = tracks.stream().filter(
                track->track.getFirstFrame()==(frame+1)
            ).collect(Collectors.toList());
        List<DeformableMesh3D> enteringMeshes = entering.stream().map(t->t.getMesh(frame)).collect(Collectors.toList());
        List<DeformableMesh3D> startingMeshes = starting.stream().map(t->t.getMesh(frame + 1)).collect(Collectors.toList());
        System.out.println(entering.size() + " can continue. " + starting.size() + " start.");

        FrameToFrameDisplacement ftf = new FrameToFrameDisplacement(frame, frame + 1);
        ftf.tracks = tracks;

        List<Mapping> mappings = ftf.getAvailableMappings(enteringMeshes, startingMeshes);


        for(Mapping map: mappings){
            Track destination = entering.get(map.a);
            Track origin = starting.get(map.b);
            ftf.toChange.add(destination);
            ftf.changes.add(origin.getTrack());
            ftf.toRemove.add(origin);
        }
        return ftf;

    }

    public UndoableActions modifyTracksAction(SegmentationController controller){
        List<Track> changing = new ArrayList<>(toChange);
        List<Map<Integer, DeformableMesh3D>> history = new ArrayList<>();
        List<Map<Integer, DeformableMesh3D>> futures = new ArrayList<>();
        List<Track> originalTracks = controller.getAllTracks();
        List<Track> updated = new ArrayList<>(tracks);
        updated.removeIf( t -> toRemove.contains(t));

        return new UndoableActions() {
            @Override
            public void perform() {
                System.out.println("modifying " + changing.size() + " tracks");
                System.out.println("leaving " + updated.size() + " from " + tracks.size() + " replacing " + originalTracks.size());
                for(int i = 0; i<changing.size(); i++){
                    Track t = changing.get(i);
                    history.add(t.getTrack());
                    t.putAll(changes.get(i));
                    futures.add(t.getTrack());

                }
                controller.getModel().setMeshes(updated);
            }

            @Override
            public void undo() {
                for(int i = 0; i<changing.size(); i++){
                    Track t = changing.get(i);
                    t.setData(history.get(i));
                }
                controller.getModel().setMeshes(originalTracks);
            }

            @Override
            public void redo() {
                for(int i = 0; i<changing.size(); i++){
                    Track t = changing.get(i);
                    t.setData(futures.get(i));
                }
                controller.getModel().setMeshes(originalTracks);
            }
        };

    }

    private List<Mapping> getAvailableMappings(List<DeformableMesh3D> enteringMeshes, List<DeformableMesh3D> startingMeshes) {
        List<Mapping> mappings = processJaccardIndexMap(enteringMeshes, startingMeshes);
        mappings.sort(Comparator.comparingDouble(m->-m.ji));

        boolean[] taken = new boolean[enteringMeshes.size()];
        boolean[] consumed = new boolean[startingMeshes.size()];
        List<Mapping> vetted = new ArrayList<>();
        for(Mapping mapping: mappings){
            if(mapping.ji < JI_CUTOFF){
                break;
            }
            if( taken[mapping.a] || consumed[mapping.b]){
                continue;
            }

            taken[mapping.a] = true;
            consumed[mapping.b] = true;
            vetted.add(mapping);

        }
        return vetted;
    }

    public FrameToFrameDisplacement(List<Track> tracks){
        this.tracks = tracks;
        int min = Integer.MAX_VALUE;
        int max = -min;
        for(Track track: tracks){
            int f = track.getFirstFrame();
            int l = track.getLastFrame();
            if(f<min){
                min = f;
            }
            if(l > max){
                max = l;
            }
        }
        first = min;
        last = max;

    }
    public static double[] centerOfMass(List<DeformableMesh3D> meshes){
        double[] c = new double[3];
        double m = 0;
        for(DeformableMesh3D mesh: meshes){
            double v = mesh.calculateVolume();
            double[] ci = new InterceptingMesh3D(mesh).getCenter();
            m += v;
            c[0] += ci[0]*v;
            c[1] += ci[1]*v;
            c[2] += ci[2]*v;
        }
        c[0] = c[0]/m;
        c[1] = c[1]/m;
        c[2] = c[2]/m;
        return c;
    }
    static class Delta implements Comparable<Delta>{
        final public double distance;
        final public int index;

        public Delta(double distance, int index){
            this.distance = distance;
            this.index = index;
        }

        @Override
        public int compareTo(Delta o) {
            return Double.compare(distance, o.distance);
        }
    }
    /**
     *
     * @param mesh
     * @param possible
     * @param delta
     * @return
     */
    static int closestCenter(DeformableMesh3D mesh, List<DeformableMesh3D> possible, double[] delta){

        double[] cm = new InterceptingMesh3D(mesh).getCenter();
        double[] tf = {cm[0] + delta[0], cm[1] + delta[1], cm[2] + delta[2]};


        double min = Double.MAX_VALUE;
        List<Delta> ds = new ArrayList<>(possible.size());
        for( int i = 0; i<possible.size(); i++){
            double[] des = new InterceptingMesh3D(possible.get(i)).getCenter();
            double m = Vector3DOps.distance(tf, des);
            ds.add(new Delta(m, i));
        }

        Collections.sort(ds);
        ds.forEach(d -> System.out.printf( "( %d, %3.3f )\t",d.index, d.distance));
        return ds.get(0).index;
    }
    public List<double[]>  processJaccardIndexMatrix(List<DeformableMesh3D> m1, List<DeformableMesh3D> m2){
        List<Mapping> mappings = processJaccardIndexMap(m1, m2);
        Map<Integer, List<Mapping>> grouped = mappings.stream().collect(Collectors.groupingBy(m->m.a));
        List<double[]> results = new ArrayList<>();
        System.out.println( grouped.size() + " // " + m1.size());
        List<Integer> keys = new ArrayList<>(grouped.keySet());
        for(Integer key: keys){
            List<Mapping> mapped = grouped.get(key);
            double[] jis = new double[m2.size()];
            for(int i = 0; i<mapped.size(); i++){
                int dex = mapped.get(i).b;
                jis[dex] = mapped.get(i).ji;

            }
            results.add(jis);
        }

        return results;
    }
    public List<Mapping> processJaccardIndexMap(List<DeformableMesh3D> m1, List<DeformableMesh3D> m2){
        System.out.println(m1.size() + " meshes tracking to " + m2.size());
        List<Mapping> maps;
        if(followCenterOfMass) {
            double[] c1 = centerOfMass(m1);
            double[] c2 = centerOfMass(m2);
            //displacement from c1 to c2.
            double[] delta = {c2[0] - c1[0], c2[1] - c1[1], c2[2] - c1[2]};
            displacements(m1, m2, delta);


            delta[0] = -delta[0];
            delta[1] = -delta[1];
            delta[2] = -delta[2];
            List<DeformableMesh3D> dups = m2.stream().map(
                    m -> new DeformableMesh3D(
                            Arrays.copyOf(m.positions, m.positions.length),
                            Arrays.copyOf(m.connection_index, m.connection_index.length),
                            Arrays.copyOf(m.triangle_index, m.triangle_index.length)
                    )
            ).collect(Collectors.toList());

            dups.forEach(m->m.translate(delta));
            maps = jaccardIndex(m1, dups);
        } else{
            maps = jaccardIndex(m1, m2);
        }


        return maps;
    }

    /**
     *
     * @param starting first frame to be tracked.
     */
    public void processFrame(int starting){
        int nextFrame = starting+1;
        List<DeformableMesh3D> m1 = tracks.stream().filter(t -> t.containsKey(starting)).map(t->t.getMesh(starting)).collect(Collectors.toList());
        List<DeformableMesh3D> m2 = tracks.stream().filter(t -> t.containsKey(nextFrame)).map(t->t.getMesh( starting+1 )).collect(Collectors.toList());
        List<Mapping> maps = processJaccardIndexMap(m1, m2);
        if(m1.size() != maps.size()){
            throw new RuntimeException("Missing mesh!");
        }



        for(Mapping map: maps){
            if(!lastAdded.containsKey(map.a)){
                //the mesh in 'a' started on starting frame.
                Track t = createNewTrack();
                t.addMesh(starting, m1.get(map.a));
                results.put(t, new ArrayList<>());
                lastAdded.put(map.a, t);
            }
        }

        Map<Integer, Double> bs = new HashMap<>();
        Map<Integer, Track> currentlyAdding = new HashMap<>();
        //lastAdded has all of the meshes from starting in working tracks.
        for(Mapping m: maps){
            if(m.b == -1){
                //this track ends, nobody overlapped it.
                continue;
            }

            if( bs.containsKey(m.b) ){
                //multiple tracks map to the same m.b.
                double v = bs.get(m.b);
                if(v > m.ji){
                    //old mapping is better.
                    continue;
                } else{
                    //find the old track that contained m.b
                    for(Track t: results.keySet()){
                        if(t.containsMesh(m2.get(m.b))){
                            //remove the mesh from other track.
                            t.remove(m2.get(m.b));
                            //remove the mapping from the results.
                            results.get(t).remove(m);
                        }
                    }
                    //overwrite old
                    bs.put(m.b, m.ji);
                    Track t = lastAdded.get(m.a);
                    t.addMesh(nextFrame, m2.get(m.b));
                    //overwrite old.
                    currentlyAdding.put(m.b, t);

                    results.get(t).add(m);
                }
            } else{
                Track t = lastAdded.get(m.a);
                t.addMesh(nextFrame, m2.get(m.b));

                currentlyAdding.put(m.b, t);
                results.get(t).add(m);
                bs.put(m.b, m.ji);

            }

        }


        lastAdded = currentlyAdding;
        if( bs.size() == maps.size() ){
            System.out.println("1:1");
        } else{
            System.out.println("difference b-a = " + (bs.size() - maps.size()));
        }
    }
    List<Color> global = new ArrayList<>();
    public Track createNewTrack(){
        Color c = ColorSuggestions.getSuggestion(global);
        global.add(c);

        String name = ColorSuggestions.getColorName(c);
        return new Track(name);
    }

    public void plot(){
        Graph g = new Graph();

        for(Track t: results.keySet()){
            List<Mapping> chained = results.get(t);
            double[] x = new double[chained.size()];
            double[] y = new double[chained.size()];
            for(int i = 0; i< chained.size(); i++){
                x[i] = i;
                y[i] = chained.get(i).ji;
            }
            g.addData(x, y).setLabel(t.getName());
        }

        g.show(false);

    }
    public String toString(){
        StringBuilder builder = new StringBuilder("#frame");
        int n = 0;
        for(Track t: results.keySet()){
            builder.append("\t" + t.name);
            int len = results.get(t).size();
            n = len>n ? len : n;
        }

        builder.append("\n");

        for(int i = 0; i<n; i++){
            builder.append(i + "");
            for(Track t: results.keySet()){
                List<Mapping> mpd = results.get(t);
                if(mpd.size()>i){
                    builder.append("\t" + mpd.get(i).ji);
                } else{
                    builder.append("\t");
                }
            }
            builder.append("\n");
        }
        return builder.toString();
    }
    public static void main(String[] args) throws IOException {
        List<Track> meshes;
        String filename;
        if(args.length == 1) {
            filename = args[0];
            meshes = MeshReader.loadMeshes(new File(filename));
        } else{
            filename = ij.IJ.getFilePath("select mesh file");
            meshes = MeshReader.loadMeshes(new File(filename));
        }
        FrameToFrameDisplacement ftfd = new FrameToFrameDisplacement(meshes);
        for(int i = ftfd.first; i<=ftfd.last; i++){
            ftfd.processFrame(i);
        }
        ftfd.saveTrack(Paths.get(filename.replace(".bmf", "-tracked.bmf")));
        System.out.print(ftfd.toString());
        ftfd.plot();
    }
    public void saveTrack(Path file){
        MeshTracker tracker = new MeshTracker();
        List<Track> tracks = new ArrayList<>(results.keySet());
        tracker.addMeshTracks(tracks);
        try {
            MeshWriter.saveMeshes(file.toFile(), tracker);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Keeps track of two indexes and their associated connection weight.
     * The weight is expected to be the jaccard index.
     */
    static class Mapping implements Comparable<Mapping>{
        final int a,b;
        final double ji;
        public Mapping(int a, int b, double ji){
            this.a = a;
            this.b = b;
            this.ji = ji;
        }

        @Override
        public int compareTo(Mapping o) {
            return Double.compare(ji, o.ji);
        }
    }
    public static double[][] displacements(List<DeformableMesh3D> A, List<DeformableMesh3D> B, double[] aToB){
        double[][] distances = new double[A.size()][B.size()];
        List<double[]> centers = B.stream().map(
                InterceptingMesh3D::new
            ).map(
                    InterceptingMesh3D::getCenter
            ).collect(
                    Collectors.toList()
            );

        for(int i = 0; i<A.size(); i++){
            double[] cm = new InterceptingMesh3D(A.get(i)).getCenter();
            double[] tf = {cm[0] + aToB[0], cm[1] + aToB[1], cm[2] + aToB[2]};
            for(int j = 0; j<B.size(); j++){
                double[] c2 = centers.get(j);
                distances[i][j] = Vector3DOps.distance(tf, c2);
            }
        }
        return distances;
    }


    static public ImagePlus generateMosaicImage(List<DeformableMesh3D> meshes){

        int w = 128;
        int h = 128;
        int d = 128;
        ImagePlus plus = new ImagePlus();
        ImageStack stack = new ImageStack(w, h);
        for(int i = 0; i<d; i++){
            stack.addSlice(new ColorProcessor(w, h));
        }
        plus.setStack(stack);
        MeshImageStack mis = new MeshImageStack(plus);

        List<Track> tracks = new ArrayList<>();

        for(int i = 0; i<meshes.size(); i++){
            DeformableMesh3D mesh = meshes.get(i);
            Track t = new Track(mesh.toString(), new Color(i + 1));
            t.addMesh(0, mesh);
            tracks.add(t);
        }
        ImagePlus mos = DeformableMesh3DTools.createMosaicRepresentation(mis, plus, tracks);
        mos.setDimensions(1, d, 1);

        return mos;
    }


    public static double[][] jaccardIndexMatrix(List<DeformableMesh3D> one, List<DeformableMesh3D> two){
        if(two.size() == 0){
            return new double[one.size()][0];
        }
        ImagePlus mos1 = generateMosaicImage(one);
        ImagePlus mos2 = generateMosaicImage(two);


        ImageStack stackA = mos1.getStack();
        ImageStack stackB = mos2.getStack();

        int[] sizes = new int[one.size() + two.size()];
        int[][] intersections = new int[one.size()][two.size()];
        int nA = one.size();
        for(int i = 1; i<= stackA.size(); i++){
            int[] pA = (int[])stackA.getPixels(i);
            int[] pB = (int[])stackB.getPixels(i);
            for(int j = 0; j<pA.length; j++){
                int xA = pA[j]&0xffffff;

                int xB = pB[j]&0xffffff;
                if(xA>0){
                    sizes[xA-1] += 1;
                }
                if(xB > 0){
                    sizes[nA + xB - 1] += 1;
                }
                if(xA > 0 && xB > 0){
                    intersections[xA - 1][xB - 1] += 1;
                }
            }
        }
        double[][] ji = new double[intersections.length][intersections[0].length];
        for(int a = 0; a<one.size(); a++){
            for(int b = 0; b<two.size(); b++){
                int intersection = intersections[a][b];
                int union = sizes[a] + sizes[nA + b] - intersection;
                if(union > 0) {
                    ji[a][b] = intersection * 1.0 / union;
                }
            }
        }
        return ji;
    }

    public static double[][] boundingBoxJaccardIndexMatrix(List<DeformableMesh3D> one, List<DeformableMesh3D> two){
        if(two.size() == 0){
            return new double[one.size()][0];
        }

        double[][] ji = new double[one.size()][two.size()];

        for(int a = 0; a<one.size(); a++){
            for(int b = 0; b<two.size(); b++){
                Box3D bA = one.get(a).getBoundingBox();
                Box3D bB = two.get(b).getBoundingBox();
                Box3D is = bA.getIntersectingBox(bB);
                double va = bA.getVolume();
                double vb = bB.getVolume();
                double vis = is.getVolume();
                ji[a][b] = vis / (va + vb - vis);
            }
        }
        return ji;
    }

    /**
     * Finds the jaccardIndexMatrix between the two lists of meshes. Finds the maximum jaccard index
     * value for all of the meshes in one, .
     * for each mesh in list one and maps it to a mesh in .
     *
     * @param one List of meshes to be tracked from.
     * @param two List of meshes to be tracked too.
     * @return a list of mappings the same length as one
     */
    public static List<Mapping> jaccardIndex(List<DeformableMesh3D> one, List<DeformableMesh3D> two){

        double[][] ji = boundingBoxJaccardIndexMatrix(one, two);
        List<Mapping> mappings = new ArrayList<>();

        for(int i = 0; i<ji.length; i++){
            double[] row = ji[i];
            double max = 0;
            int dex = -1;
            for(int j = 0; j<row.length; j++){

                if(row[j]>max){
                    max = row[j];
                    dex = j;
                }

            }
            mappings.add(new Mapping(i, dex, max));
        }

        return mappings;
    }

    public static void print2DMatrix(double[][] ji){
        System.out.print("#");
        for(int i = 0; i<ji[0].length; i++){
            System.out.printf("\t%4d", (i+1));
        }
        System.out.println();
        for(int j = 0; j<ji.length; j++){
            System.out.print( (j + 1) );
            double[] row = ji[j];
            for(int i = 0; i<row.length; i++){
                System.out.printf("\t%3.3f", row[i]);
            }
            System.out.println();
        }
    }
}
