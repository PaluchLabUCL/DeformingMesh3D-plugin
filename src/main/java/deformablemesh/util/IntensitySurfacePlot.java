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

public class IntensitySurfacePlot extends SurfacePlot{
    MeshImageStack stack;
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

    @Override
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
