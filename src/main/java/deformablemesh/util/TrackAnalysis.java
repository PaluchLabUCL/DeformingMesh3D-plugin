/*-
 * #%L
 * Triangulated surface for deforming in 3D.
 * %%
 * Copyright (C) 2013 - 2023 University College London
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package deformablemesh.util;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.FrameToFrameDisplacement;
import deformablemesh.track.Track;
import lightgraph.DataSet;
import lightgraph.Graph;

import java.util.List;
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
        FrameToFrameDisplacement ffd = new FrameToFrameDisplacement(frame);
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
