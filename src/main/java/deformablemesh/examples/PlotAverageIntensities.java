package deformablemesh.examples;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import ij.ImagePlus;
import lightgraph.DataSet;
import lightgraph.Graph;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlotAverageIntensities {

    public static void main(String[] args) throws IOException {
        ImagePlus plus = new ImagePlus(new File(args[0]).getAbsolutePath());
        List<Track> tracks = MeshReader.loadMeshes(new File(args[1]));
        MeshImageStack stack = new MeshImageStack(plus, 20, 1);

        final int start = stack.CURRENT;
        Graph plot = new Graph();
        for(Track track: tracks){
            double[] times = new double[track.size()];
            double[] intensities = new double[track.size()];
            int index = 0;
            for(Integer i: track.getTrack().keySet()){
                stack.setFrame(i);
                times[index] = i;
                DeformableMesh3D mesh = track.getMesh(i);
                List<int[]> volumePixels = DeformableMesh3DTools.getContainedPixels(stack, mesh);
                double intensity = 0;
                for(int[] values : volumePixels){
                    intensity += stack.getValue(values[0], values[1], values[2]);
                }
                intensity = intensity/volumePixels.size();
                intensities[index] = intensity;
                index++;
            }
            DataSet set = plot.addData(times, intensities);
            set.setLabel(track.getName());
            set.setColor(track.getColor());
        }
        stack.setFrame(start);
        plot.show(false, "Intensity vs Time");

    }
}
