package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.*;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import lightgraph.DataSet;
import lightgraph.Graph;
import lightgraph.GraphPoints;
import snakeprogram.util.TextWindow;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class for detecting how much of a mesh is obscured by other meshes.
 *
 */
public class MeshFaceObscuring {
    List<InterceptingMesh3D> meshes;
    double cutoff = 1.0;

    public void setNeighbors(List<DeformableMesh3D> meshes){
        this.meshes = meshes.stream().map(InterceptingMesh3D::new).collect(Collectors.toList());
    }
    public void setNeighbor(DeformableMesh3D mesh){
        this.meshes = new ArrayList<>(1);
        meshes.add(new InterceptingMesh3D(mesh));
    }

    public Set<Triangle3D> getOverlapArea(DeformableMesh3D mesh){
        Set<Triangle3D> touching = new HashSet<>();

        for(InterceptingMesh3D neigh: meshes){
            for(Triangle3D t: mesh.triangles){
                t.update();
                if(naiveCheck(t, neigh)){
                    touching.add(t);
                }


            }
        }

        return touching ;
    }

    /**
     *
     * @param t
     * @param mesh
     * @return
     */
    public boolean naiveCheck(Triangle3D t, InterceptingMesh3D mesh){
        if(mesh.contains(t.center)){
            return true;
        }
        List<Intersection> sections = mesh.getIntersections(t.center, t.normal);
        for(Intersection section: sections){
            double dot = Vector3DOps.dot(t.normal, Vector3DOps.difference(section.location, t.center));
            if(dot>0 && dot<cutoff){
                return true;
            }
        }
        return false;
    }



    static public void analyzeTracks(List<Track> tracks, MeshImageStack stack, int frame, double cutoff){
        List<DeformableMesh3D> meshes = tracks.stream().filter(
                                                t->t.containsKey(frame)
                                            ).map(
                                                t->t.getMesh(frame)
                                            ).collect(Collectors.toList());

        List<String> colorNames = tracks.stream().map(t->t.getName()).collect(Collectors.toList());
        double factor = stack.SCALE*stack.SCALE;
        StringBuilder build = new StringBuilder("#Overlap data v0.1\n");
        build.append("#color-name\ttotal-area(unit^2)\toverlap-area(unit^2)\n");
        for(int i = 0; i<meshes.size();i++){

            MeshFaceObscuring finder =  new MeshFaceObscuring();
            finder.cutoff = cutoff;
            List<DeformableMesh3D> minusOne = new ArrayList<>(meshes);

            DeformableMesh3D mesh = meshes.get(i);
            minusOne.remove(mesh);
            finder.setNeighbors(minusOne);
            Set<Triangle3D> triangles = finder.getOverlapArea(mesh);
            double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);
            double covered = 0;
            for(Triangle3D t: triangles){
                covered += t.area;
            }
            build.append(String.format("%s\t%s\t%s\n", colorNames.get(i), Double.toString(area*factor), Double.toString(covered*factor)));


        }

        TextWindow window = new TextWindow("covered meshes", build.toString());
        window.display();
    }

    /**
     * Analyzes the provided tracks.
     * @param target
     * @param others
     * @param cutoff
     * @return
     */
    static public List<double[]> analyzeTracks(Track target, List<Track> others, double cutoff){
        List<double[]> overlaps = new ArrayList<>();
        for(Integer frame: target.getTrack().keySet()){
            MeshFaceObscuring finder =  new MeshFaceObscuring();
            finder.cutoff = cutoff;
            //1 over lap  per track plus frame plus total area.
            double[] values = new double[others.size() + 2];
            overlaps.add(values);
            DeformableMesh3D mesh = target.getMesh(frame);
            double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);
            values[0] = frame;
            values[1] = area;

            int dex = 0;
            for(Track ot: others){
                double ol = 0;
                if(ot.containsKey(frame)) {
                    finder.setNeighbor(ot.getMesh(frame));
                    Set<Triangle3D> triangles = finder.getOverlapArea(mesh);
                    for(Triangle3D t: triangles){
                        ol += t.area;
                    }
                }
                values[2 + dex] = ol;
                dex++;
            }

        }


        return overlaps;
    }
    static class TrackData implements Iterable<double[]>{
        List<double[]> values = new ArrayList<>();
        String name;
        Color c;
        public TrackData(String name, Color c){
            this.name = name;
            this.c = c;
        }
        public void add(double[] data){
            values.add(data);
        }
        public double[] get(int i ){
            return values.get(i);
        }

        public int size(){
            return values.size();
        }

        @Override
        public Iterator<double[]> iterator() {
            return values.iterator();
        }
    }

    static void plot(List<TrackData> values, String title){
        Graph vol = new Graph();
        Graph a = new Graph();
        vol.setTitle("volume vs time");
        a.setTitle("Surface Area/ S_0");
        for(TrackData plot: values){
            double zero = 0;
            for(double[] d: plot){
                if(d[0]>zero){
                    zero = d[0];
                }
            }

            int n = plot.size();
            double[] data = new double[n];
            double[] time = new double[n];

            for(int i = 0; i<n; i++){
                double[] row = plot.get(i);
                data[i] = row[1];
                time[i] = row[0] - zero;
            }

            DataSet set = vol.addData(time, data);
            set.setColor(plot.c);
            set.setLabel(plot.name);

            data = new double[n];
            time = new double[n];
            for(int i = 0; i<n; i++){
                double[] row = plot.get(i);
                double snot = Math.pow(6*row[1]/Math.PI, 2/3.0)*Math.PI;
                data[i] = row[2]/snot;
                time[i] = row[0] - zero;
            }
            set = a.addData(time, data);
            set.setColor(plot.c);
            set.setLabel(plot.name);


        }

        vol.show(true, title + " volume");
        a.show(true, title + "area");


    }
    public static void analyzeTimeCourses(File meshFile) throws IOException {
        List<Track> tracks =  MeshReader.loadMeshes(meshFile);
        analyzeTimeCourses(tracks, meshFile.getName());
    }

    public static void analyzeTimeCourses(List<Track> tracks, String meshFileName){

        double CUTOFF= 0.0001;

        List<TrackData> data = new ArrayList<>();

        Graph overlapping = new Graph();
        overlapping.setTitle("overlaping faces: " + meshFileName);
        Map<String, GraphPoints> pointsMap = new HashMap<>();
        int index = 0;
        List<GraphPoints> gp = GraphPoints.getGraphPoints();
        for(Track track: tracks){
            pointsMap.put(track.getName(), gp.get(index));
            index++;
        }

        for(int i = 0; i<tracks.size(); i++){
            Track track = tracks.get(i);

            List<Track> otherTracks = tracks.stream().filter(t->!t.equals(track)).collect(Collectors.toList());
            List<double[]> values = MeshFaceObscuring.analyzeTracks(track, otherTracks, CUTOFF);
            for(int j = 0; j<otherTracks.size(); j++){

                double[] frames = new double[values.size()];
                double[] lap = new double[values.size()];
                boolean plotThis=false;


                for(int k = 0; k<values.size(); k++){
                    double[] row = values.get(k);
                    frames[k] = row[0];
                    lap[k] = row[2 + j];
                    if(lap.length>i && lap[i]>0){
                        plotThis = true;
                    }
                }
                if(plotThis){
                    Track other = otherTracks.get(j);
                    String n1 = track.getName();
                    String n2 = other.getName();
                    Color c = track.getColor();
                    GraphPoints points = pointsMap.get(n2);
                    DataSet set = overlapping.addData(frames, lap);
                    set.setLabel(n1 + "//" + n2);
                    set.setColor(c);
                    set.setPoints(points);
                }

            }




            TrackData currentData = new TrackData(track.getName(), track.getColor());
            List<Integer> frames = new ArrayList<>();
            for(Integer frame: track.getTrack().keySet()){
                DeformableMesh3D mesh = track.getMesh(frame);
                double volume = mesh.calculateVolume();
                double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);
                double[] dat = {
                        frame, volume, area
                };
                currentData.add(dat);
                frames.add(frame);
            }

            Graph curvs = new Graph();
            int start = frames.size()-11;
            if(start<0) start = 0;

            for(int j = start; j<frames.size(); j++){
                DeformableMesh3D mesh = track.getMesh(frames.get(j));
                CurvatureCalculator cc = new CurvatureCalculator(mesh);
                cc.setMinCurvature(-10);
                cc.setMaxCurvature(10);

                List<double[]> measured = cc.createCurvatureHistogram();
                DataSet set = curvs.addData(measured.get(0), measured.get(1));

                int a = frames.size() - j;
                set.setColor(new Color(25*(11-a), 150, 150));
                set.setLabel("" + frames.get(j));
            }

            curvs.show(false, track.getName());

            data.add(currentData);

        }
        overlapping.show(true, "Overlap " + meshFileName);
        plot(data, meshFileName);
    }

}
