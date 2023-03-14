package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.BinaryMomentsOfInertia;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.gui.GuiTools;
import deformablemesh.track.Track;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import lightgraph.DataSet;
import lightgraph.Graph;
import lightgraph.GraphPoints;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by msmith on 2/9/16.
 */
public class MeshAnalysis {
    Map<Integer, DeformableMesh3D> meshes;
    Map<Integer, Furrow3D> furrows;
    MeshImageStack stack;

    public MeshAnalysis(Map<Integer, DeformableMesh3D> meshes, Map<Integer, Furrow3D> furrows, MeshImageStack stack){
        this.meshes = meshes;
        this.furrows = furrows;
        this.stack = stack;
    }

    /**
     *
     * @param tracks
     */
    public static void plotElongationsVsTime(List<Track> tracks) {
        Graph elongationPlot = new Graph();
        int w = 64;
        int h = 64;
        int d = 64;
        ImagePlus plus = new ImagePlus();
        ImageStack stack = new ImageStack(w, h);
        for(int i = 0; i<d; i++){
            stack.addSlice(new ByteProcessor(w, h));
        }
        plus.setStack(stack);
        MeshImageStack mis = new MeshImageStack(plus);

        for(Track t: tracks){

            DataSet d2 = null;
            for(int i = t.getFirstFrame(); i<=t.getLastFrame(); i++){
                if(t.containsKey(i)){
                    DeformableMesh3D start = t.getMesh(i);
                    BinaryMomentsOfInertia bmi = new BinaryMomentsOfInertia(start, mis);

                    List<double[]> vectors = bmi.getEigenVectors();
                    double[] ln = vectors.get(3);

                    if(d2 == null){
                        d2 = elongationPlot.addData(new double[0], new double[0]);
                        d2.setLabel(t.getName());
                        d2.setColor(t.getColor());
                    }
                    double lmin = ln[0] < ln[1] ?
                            ln[0] < ln[2] ? ln[0] : ln[2] :
                            ln[1] < ln[2] ? ln[1] : ln[2];

                    double e = (ln[0] + ln[1] + ln[2])/lmin;
                    d2.addPoint(i, e);
                }
            }
        }
        elongationPlot.show(false, "Elongation per Frame");
    }


    public void createOutput(double cortex_thickness){
        StringBuilder builder = new StringBuilder("#frame\tleft volume\tright volume\ttotal volume\tfurrow\tleft intensity\tright intensity\n");
        for(Integer i: meshes.keySet()){
            DeformableMesh3D mes = meshes.get(i);
            Furrow3D fur = furrows.get(i);
            List<List<Triangle3D>> sides = fur.splitMesh(mes.triangles);

            List<Triangle3D> left = sides.get(0);
            List<Triangle3D> right = sides.get(1);

            double[] up = new double[]{0,0,1};
            double v = DeformableMesh3DTools.calculateVolume(up, mes.positions, mes.triangles);
            double lv = DeformableMesh3DTools.calculateVolume(up, mes.positions, left);
            double rv = DeformableMesh3DTools.calculateVolume(up, mes.positions, right);
            double fr = fur.calculateRadius(mes.connections);
            stack.setFrame(i);
            double left_intensity = DeformableMesh3DTools.calculateAverageIntensity(stack, left, cortex_thickness );
            double right_intensity = DeformableMesh3DTools.calculateAverageIntensity(stack, right, cortex_thickness);
            builder.append(String.format("%d\t%f\t%f\t%f\t%f\t%f\t%f\n",i ,lv ,rv ,v, fr, left_intensity, right_intensity ));
            mes.calculateIntensity(stack,cortex_thickness);
        }
        final String op = builder.toString();
        EventQueue.invokeLater(() -> GuiTools.createTextOuputPane(op));
    }

    public void calculateAverageIntensity(int i, double cortex_thickness){
        DeformableMesh3D mesh = meshes.get(i);
        ArrayList<double[]> values =  mesh.calculateIntensity(stack, cortex_thickness);
        final StringBuilder builder = new StringBuilder("");
        for(double[] d: values){
            builder.append(String.format(Locale.US, "%f\t%f\t%f\t%f\n",d[0], d[1], d[2], d[3]));
        }
        EventQueue.invokeLater(
                new Runnable(){
                    public void run(){
                        GuiTools.createTextOuputPane(builder.toString());
                    }
                });

    }

    public void calculateLineScans(int frame, double cortex_thickness){
        DeformableMesh3D mesh = meshes.get(frame);
        ArrayList<double[]> values =  mesh.calculateNormalScan(stack, cortex_thickness);
        StringBuilder builder = new StringBuilder("");
        for(double[] d: values){
            for(int i =0 ; i<d.length; i++){
                builder.append(String.format(Locale.US, "%d\t%f\n",i,d[i]));
            }
        }
        GuiTools.createTextOuputPane(builder.toString());
    }
    /**
     * Ripped from psuedocortex dividing cell.
     *
     * @param y
     * @return
     */
    static double g2(double y){
        double rad = Math.cbrt(1 - Math.sqrt(y * (1 + y) * (1 + y) * (1 + y)) + y * (2 + y));
        return 1 - rad/(1+y) - 1/rad;
    }

    public static double radius(double v, double rc){
        double arg = 16*(1+v)*(1+v)/(rc*rc*rc*rc*rc*rc);
        double g = g2(arg);
        return rc/Math.sqrt(1-g*g);
    }

    public void measureVolume(){
        StringBuilder builder = new StringBuilder("#frame\tback volume\tfront volume\ttotal volume\tfurrow\n");
        for(Integer i: meshes.keySet()){
            DeformableMesh3D mes = meshes.get(i);
            Furrow3D fur = furrows.get(i);
            double v;
            double fv, bv, fr;
            double factor = stack.SCALE*stack.SCALE*stack.SCALE;
            if(fur!=null){
                double[] up = Vector3DOps.getPerpendicularNormalizedVector(fur.normal);
                List<List<Triangle3D>> sides = fur.splitMesh(mes.triangles);
                List<Triangle3D> front = sides.get(0);
                List<Triangle3D> back = sides.get(1);
                fv = DeformableMesh3DTools.calculateVolume(up, mes.positions, front);
                bv = DeformableMesh3DTools.calculateVolume(up, mes.positions, back);
                fr = fur.calculateRadius(mes.connections);
                v = DeformableMesh3DTools.calculateVolume(up, mes.positions, mes.triangles);
            } else{
                fv = -1;
                bv = -1;
                fr = -1;
                v = DeformableMesh3DTools.calculateVolume(new double[]{0,0,1}, mes.positions, mes.triangles);
            }
            fv = fv>0?fv*factor:fv;
            bv = bv>0?bv*factor:bv;
            builder.append(String.format(Locale.US, "%d\t%f\t%f\t%f\t%f\n",i ,bv ,fv ,v*factor, fr*stack.SCALE ));
        }
        final String op = builder.toString();
        EventQueue.invokeLater(() -> GuiTools.createTextOuputPane(op));
    }

    /**
     * Plots the number of meshes over time.
     *
     * @param tracks
     * @param mis
     */
    public static void plotMeshesOverTime(List<Track> tracks, MeshImageStack mis){
        double[] ret = new double[mis.getNFrames()];
        double[] frame = new double[mis.getNFrames()];
        for(int i = 0; i<mis.getNFrames(); i++){
            frame[i] = i;
            int s = 0;
            for(Track t: tracks){
                if(t.containsKey(i)){
                    s++;
                }
            }

            ret[i] = s;
        }
        Graph plot = new Graph();
        plot.addData(frame, ret);
        plot.show(false, "Number of cells vs time");
    }

    /**
     * Creates a plot of the volumes over time. Tracked cells are connected.
     * @param tracks
     * @param mis
     */
    public static void plotVolumesOverTime(List<Track> tracks, MeshImageStack mis){
        Graph volumePlot = new Graph();
        volumePlot.setXTicCount(6);
        volumePlot.setYTicCount(6);
        volumePlot.setXLabel("Frame No.");
        String unit = mis.getUnits();
        if(unit == null){
            unit = "au";
        }
        volumePlot.setYLabel("Volume ( " + unit + "^3 )");
        volumePlot.setTitle("Volumes vs Time");

        for(Track t: tracks){
            DataSet d2 = null;
            for(int i = t.getFirstFrame(); i<=t.getLastFrame(); i++){
                if(t.containsKey(i)){
                    DeformableMesh3D mesh = t.getMesh(i);
                    double volume = mesh.calculateVolume()*Math.pow(mis.SCALE, 3);
                    if(d2 == null){
                        d2 = volumePlot.addData(new double[0], new double[0]);
                        d2.setLabel(t.getName());
                        d2.setColor(t.getColor());
                        if(t.getFirstFrame() == 0 && t.getLastFrame() == mis.getNFrames()-1){
                            d2.setPoints(GraphPoints.filledTriangles());
                        }
                    }
                    d2.addPoint(i, volume);
                }
            }
        }
        volumePlot.show(false, "Volumes per Frame");
    }

    public static void plotDisplacementHistogram(List<Track> tracks, MeshImageStack meshImageStack){
        double min = 0;
        double max = 10.0;
        int bins = 100;

        double[] x = new double[bins];
        double delta = (max - min )/bins;
        for(int i = 0; i<x.length; i++){
            x[i] = min + (i + 0.5)*delta;
        }

        Graph displacementPlot = new Graph();
        double[] count = new double[bins];
        for(Track t: tracks) {
            if (t.size() < 2) {
                continue;
            }
            List<double[]> di = new ArrayList<>();
            for (int i = t.getFirstFrame(); i <= t.getLastFrame(); i++) {
                if (t.containsKey(i) && t.containsKey(i + 1)) {
                    DeformableMesh3D start = t.getMesh(i);
                    DeformableMesh3D fin = t.getMesh(i + 1);
                    double[] cs = DeformableMesh3DTools.centerAndRadius(start.nodes);
                    double[] cf = DeformableMesh3DTools.centerAndRadius(fin.nodes);
                    double d =
                            Vector3DOps.normalize(
                                    Vector3DOps.difference(cs, cf)
                            ) * meshImageStack.SCALE;

                    di.add(new double[]{i + 0.5, d});
                }
            }
            if (di.size() > 0) {

                for (double[] d : di) {
                    int dex = (int) ((d[1] - min) / delta);
                    if (dex == bins) {
                        dex = dex - 1;
                    }
                    if (dex < 0 || dex > bins) {
                        //throw it away.
                        continue;
                    }
                    count[dex] += 1;
                }

            }
        }
        DataSet histo = displacementPlot.addData(x, count);
        histo.setPoints( GraphPoints.filledSquares());
        displacementPlot.show(false, "histogram of displacements");
    }

    public static void plotDisplacementsVsTime(List<Track> tracks, MeshImageStack meshImageStack) {
        Graph displacementPlot = new Graph();
        displacementPlot.setTitle("Displacements over Time");

        displacementPlot.setXRange(0, 135);
        displacementPlot.setXTicCount(6);
        displacementPlot.setYRange(0, 10);
        displacementPlot.setYTicCount(6);
        displacementPlot.setXLabel("Frame No.");
        String unit = meshImageStack.getUnits();
        if(unit == null){
            unit = "au";
        }
        displacementPlot.setYLabel("Displacement ( " + unit + " )");
        displacementPlot.setTitle("Displacements vs Time");

        for(Track t: tracks){
            if(t.size()<2){
                continue;
            }
            DataSet d2 = null;
            for(int i = t.getFirstFrame(); i<=t.getLastFrame(); i++){
                if(t.containsKey(i) && t.containsKey(i+1)){
                    DeformableMesh3D start = t.getMesh(i);
                    DeformableMesh3D fin = t.getMesh(i+1);
                    double[] cs = DeformableMesh3DTools.centerAndRadius(start.nodes);
                    double[] cf = DeformableMesh3DTools.centerAndRadius(fin.nodes);
                    if(d2 == null){
                        d2 = displacementPlot.addData(new double[0], new double[0]);
                        d2.setLabel(t.getName());
                        d2.setColor(t.getColor());
                        if(t.getFirstFrame() == 0 && t.getLastFrame() == meshImageStack.getNFrames()-1){
                            d2.setPoints(GraphPoints.filledTriangles());
                        }
                    }
                    double d =
                            Vector3DOps.normalize(
                                Vector3DOps.difference(cs, cf)
                            ) * meshImageStack.SCALE;

                    d2.addPoint(i + 0.5, d);
                }
            }
        }
        displacementPlot.show(false, "Displacement per Frame");
    }


    /**
     * Generates a text window with the time series for all of the tracks provided.
     *
     * @param tracks
     * @param stack
     */
    public static void calculateAllVolumes(List<Track> tracks, MeshImageStack stack){
        StringBuilder builder = new StringBuilder("#Volume at each time point.\n");
        builder.append("#frame");
        for(Track t: tracks){
            builder.append("\t");
            builder.append(t.getName());
        }
        builder.append("\n");

        double factor = stack.SCALE * stack.SCALE * stack.SCALE;
        for(int i = 0; i<stack.getNFrames(); i++) {
            builder.append(String.format("%d", i));
            for (Track t : tracks) {
                double v;
                if(t.containsKey(i)){
                    DeformableMesh3D mes = t.getMesh(i);
                    v = mes.calculateVolume();
                    v*=factor;
                } else{
                    v = -1;
                }
                builder.append(String.format(Locale.US, "\t%f", v));
            }
            builder.append("\n");
        }

        final String op = builder.toString();
        EventQueue.invokeLater(() -> GuiTools.createTextOuputPane(op));

    }
}
