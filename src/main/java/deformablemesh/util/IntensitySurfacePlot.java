package deformablemesh.util;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.PlotSurface;
import deformablemesh.track.Track;
import ij.ImagePlus;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IntensitySurfacePlot {
    public Color hot = Color.ORANGE;
    public Color cool = Color.PINK;

    final DeformableMesh3D mesh;
    final MeshImageStack stack;

    int range = 10;
    double delta;

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

    public void processAndShow(){

        double[] values = new double[mesh.nodes.size()];

        double min = Double.MAX_VALUE;
        double max = -min;
        for(int i = 0;  i<mesh.nodes.size(); i++){
            Node3D node = mesh.nodes.get(i);
            double v = sample(node);
            max = v>max?v:max;
            min = v<min?v:min;
            values[i] = v;
        }


        System.out.println("delta: " + delta + " min, max " + min + ", " + max);

        float[] colors = new float[mesh.positions.length];
        HotAndCold ci = new HotAndCold(new Color(255, 255, 255), new Color(0, 0, 0));
        ci.setMinMax(min, max);
        for(int i = 0; i<mesh.nodes.size(); i++){
            float[] f = ci.getColor(values[i]);
            System.arraycopy(f, 0, colors, 3*i, 3);
        }

        MeshFrame3D viewer = new MeshFrame3D();

        viewer.showFrame(true);
        viewer.addLights();
        viewer.setBackgroundColor(Color.BLACK);


        PlotSurface surface = new PlotSurface(mesh.positions, mesh.triangle_index, colors);
        viewer.addDataObject(surface);

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
                new IntensitySurfacePlot(track.getMesh(key), plus).processAndShow();
            }
        }
    }

}
