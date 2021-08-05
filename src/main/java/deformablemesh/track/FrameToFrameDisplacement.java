package deformablemesh.track;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.io.MeshWriter;
import deformablemesh.util.ColorSuggestions;
import deformablemesh.util.Vector3DOps;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import lightgraph.Graph;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FrameToFrameDisplacement {
    List<Track> tracks;
    final Integer first, last;
    Map<Integer, Track> lastAdded = new HashMap<>();
    Map<Track, List<Mapping>> results = new HashMap<>();
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
    static double[] centerOfMass(List<DeformableMesh3D> meshes){
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

    /**
     *
     * @param starting first frame to be tracked.
     */
    public void processFrame(int starting){
        int nextFrame = starting+1;
        List<DeformableMesh3D> m1 = tracks.stream().filter(t -> t.containsKey(starting)).map(t->t.getMesh(starting)).collect(Collectors.toList());
        List<DeformableMesh3D> m2 = tracks.stream().filter(t -> t.containsKey(nextFrame)).map(t->t.getMesh( starting+1 )).collect(Collectors.toList());
        System.out.println(m1.size() + " meshes tracking to " + m2.size());

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
        List<Mapping> maps = jaccardIndex(m1, dups);
        Map<Integer, Double> bs = new HashMap<>();
        Map<Integer, Track> currentlyAdding = new HashMap<>();
        for(Mapping m: maps){
            if(m.b == -1){
                //the mesh in a does not overlap with any mesh in b... at all.
                if(!lastAdded.containsKey(m.a)){
                    //the mesh in 'a' started on starting frame.
                    Track t = createNewTrack();
                    t.addMesh(starting, m1.get(m.a));
                    results.put(t, new ArrayList<>());
                }
                continue;
            }
            if( bs.containsKey(m.b) ){
                double v = bs.get(m.b);
                if(v > m.ji){
                    continue;
                } else{
                    System.out.println("Really!!!");
                    //find the old track and remove m.b
                    for(Track t: results.keySet()){
                        if(t.containsMesh(m2.get(m.b))){
                            t.remove(m2.get(m.b));
                            bs.put(m.b, m.ji);
                        }
                    }
                }
            } else{
                bs.put(m.b, m.ji);
            }
            Track t;
            if(lastAdded.containsKey(m.a)){
                t = lastAdded.get(m.a);
            } else{
                t = createNewTrack();
                t.addMesh(starting, m1.get(m.a));
                results.put(t, new ArrayList<>());
            }

            t.addMesh(nextFrame, m2.get(m.b));
            currentlyAdding.put(m.b, t);
            results.get(t).add(m);
        }
        lastAdded = currentlyAdding;
        if( bs.size() == maps.size() ){
            System.out.println("one to one");
        } else{
            System.out.println("BORKED");
        }


    }
    public Track createNewTrack(){
        String name = ColorSuggestions.getColorName(ColorSuggestions.getSuggestion());
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
        new ImageJ();
        List<Track> meshes;
        String filename;
        if(args.length == 1) {
            filename = args[0];
            meshes = MeshWriter.loadMeshes(new File(filename));
        } else{
            filename = ij.IJ.getFilePath("select mesh file");
            meshes = MeshWriter.loadMeshes(new File(filename));
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
    public static void displacements(List<DeformableMesh3D> A, List<DeformableMesh3D> B, double[] aToB){
        double[][] distances = new double[A.size()][B.size()];
        for(int i = 0; i<A.size(); i++){
            double[] cm = new InterceptingMesh3D(A.get(i)).getCenter();
            double[] tf = {cm[0] + aToB[0], cm[1] + aToB[1], cm[2] + aToB[2]};
            for(int j = 0; j<B.size(); j++){
                double[] c2 = new InterceptingMesh3D(B.get(j)).getCenter();
                distances[i][j] = Vector3DOps.distance(tf, c2);
            }
        }

        for(int i = 0; i<distances.length; i++){
            double[] row = distances[i];
            double min = 1;
            int dex = -1;
            for(int j = 0; j<row.length; j++){

                if(row[j]<min){
                    min = row[j];
                    dex = j;
                }

            }
            System.out.println(dex + " displacement maps to " + i);
        }
        print2DMatrix(distances);
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


        print2DMatrix(ji);
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
