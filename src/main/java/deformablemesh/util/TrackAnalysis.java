package deformablemesh.util;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.FrameToFrameDisplacement;
import deformablemesh.track.Track;
import lightgraph.DataSet;
import lightgraph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrackAnalysis {


    static public void plotNextFrameTrackingResults(List<Track> tracks, int frame){

        List<DeformableMesh3D> current = tracks.stream().filter(
                t->t.containsKey(frame)
        ).map(
                t->t.getMesh(frame)
        ).collect(Collectors.toList()); List<DeformableMesh3D> next = tracks.stream().filter(
                t->t.containsKey(frame + 1)
        ).map(
                t->t.getMesh(frame + 1)
        ).collect(Collectors.toList());
        if(current.size() == 0 || next.size() == 0){
            System.out.println("no mappings");
        }
        FrameToFrameDisplacement ffd = new FrameToFrameDisplacement();
        List<double[]> jardardIndexMatrix = ffd.processJaccardIndexMatrix(current, next);

        Graph g = new Graph();
        for(double[] line: jardardIndexMatrix){
            double[] x = new double[line.length];
            for(int i = 0; i<x.length; i++){
                x[i] = i;
            }
            DataSet d = g.addData(x, line);

        }

        g.show(false, "Frame to frame JI values");
    }
}
