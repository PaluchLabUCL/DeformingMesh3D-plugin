package deformablemesh.util;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.gui.GuiTools;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.PlotSurface;
import deformablemesh.track.Track;
import ij.ImagePlus;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IntensitySurfacePlot {
    Color high = new Color(255, 255, 0);
    Color low = new Color(0, 0, 55);
    int range = 10;


    double min = Double.MAX_VALUE;
    double max = -min;

    final DeformableMesh3D mesh;
    final MeshImageStack stack;


    double delta;
    MeshFrame3D viewer;
    float[] colors;
    double[] values;

    public IntensitySurfacePlot(DeformableMesh3D mesh, ImagePlus plus){
        this.mesh = mesh;
        stack = new MeshImageStack(plus);
        delta = stack.scaleToNormalizedLength(new double[]{1, 0, 0})[0];
    }

    public IntensitySurfacePlot(DeformableMesh3D mesh, MeshImageStack stack){
        this.mesh = mesh;
        this.stack = stack;
        delta = stack.scaleToNormalizedLength(new double[]{1, 0, 0})[0];

    }

    public void setHighColor(Color high) {
        this.high = high;
    }
    public Color getHighColor(){
        return high;
    }

    public Color getLow() {
        return low;
    }

    public void setLow(Color low) {
        this.low = low;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min){
        this.min = min;
    }

    public void setMax(double max){
        this.max = max;
    }

    public double getMax(){
        return max;
    }



    /**
     * Calculates all of the local intensities at each node.
     */
    public void process(){
        values = new double[mesh.nodes.size()];


        for(int i = 0;  i<mesh.nodes.size(); i++){
            Node3D node = mesh.nodes.get(i);
            double v = sample(node);
            max = v>max?v:max;
            min = v<min?v:min;
            values[i] = v;
        }


    }


    public void showValuesWindow(){
        StringBuilder builder = new StringBuilder("#n\tx\ty\tz\ti\tc\n");
        float[] comps = new float[4];
        for(int i = 0;  i<mesh.nodes.size(); i++){
            double[] pt = mesh.nodes.get(i).getCoordinates();
            double v = values[i];
            System.arraycopy(colors, 3*i, comps, 0, 3);
            int c = new Color(comps[0], comps[1], comps[2]).getRGB();
            builder.append(String.format(Locale.US, "%d\t%f\t%f\t%f\t%f\t%d\n", i, pt[0], pt[1], pt[2], v, c ));

        }

        GuiTools.createTextOuputPane(builder.toString());
    }

    public void show(){
        show(false);
    }



    public void show(boolean exitOnClose){
        viewer = new MeshFrame3D();

        viewer.showFrame(exitOnClose);
        viewer.addLights();
        viewer.setBackgroundColor(Color.BLACK);

        JFrame frame = viewer.getJFrame();
        JMenuBar bar = new JMenuBar();
        JMenu data = new JMenu("data");
        JMenuItem showValues = new JMenuItem("show values");
        showValues.addActionListener(e->{
            showValuesWindow();
        });
        data.add(showValues);
        bar.add(data);
        frame.setJMenuBar(bar);

        colors = new float[mesh.positions.length];
        HotAndCold ci = new HotAndCold(high, low);
        ci.setMinMax(min, max);
        for(int i = 0; i<mesh.nodes.size(); i++){
            float[] f = ci.getColor(values[i]);
            System.arraycopy(f, 0, colors, 3*i, 3);
        }

        PlotSurface surface = new PlotSurface(mesh.positions, mesh.triangle_index, colors);
        viewer.addDataObject(surface);
    }

    public MeshFrame3D processAndShow(boolean exitOnClose){

        process();
        show(exitOnClose);

        return viewer;

    }

    public double sample(Node3D node){
        double[] loc = node.getCoordinates();
        double count = 0;
        double value = 0;
        for(int i = -range/2; i<=range/2; i++){
            for(int j = -range/2; j<=range/2; j++) {
                for (int k = -range / 2; k <= range / 2; k++) {

                    double[] point = {loc[0] + i * delta, loc[1] + j * delta, loc[2] + k * delta};
                    value += stack.getInterpolatedValue(point);
                    count++;
                }
            }
        }
        return value/count;
    }


    public static void main(String[] args) throws IOException {

        List<Track> tracks = new ArrayList<>();
        tracks.addAll(MeshWriter.loadMeshes(new File(args[0])));
        ImagePlus plus = new ImagePlus(new File(args[1]).getAbsolutePath());
        for(Track track: tracks) {
            for (Integer key : track.getTrack().keySet()) {
                new IntensitySurfacePlot(track.getMesh(key), plus).processAndShow(true);
            }
        }
    }

}
