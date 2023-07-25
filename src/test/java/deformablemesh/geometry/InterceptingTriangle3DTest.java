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
package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.io.MeshReader;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import ij.plugin.FileInfoVirtualStack;
import lightgraph.Graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class InterceptingTriangle3DTest {
    public static void brokenMeshes() throws IOException {
        List<Track> tracks = MeshReader.loadMeshes(new File("/home/smithm3/Desktop/2021-10-Plate8/Tile_3/broken-binarization.bmf"));
        DeformableMesh3D mesh = tracks.get(0).getMesh(0);

        MeshImageStack mis = new MeshImageStack(FileInfoVirtualStack.openVirtual("/home/smithm3/Desktop/2021-10-Plate8/Tile_3/pred-dt3-crb-latest-Tile_3_processed_binned-2b.tif"));
        DeformableMesh3DTools.createMosaicRepresentation(mis, mis.getOriginalPlus(), tracks).show();
    }

    public static void runTriangle(InterceptingTriangle3D triangle){
        int N = 250;
        double[] x = new double[N];
        double[] y = new double[N];
        double dz = InterceptingTriangle3D.tolerance*4/(N - 1);
        for(int i = 0; i<N; i++){
            List<Intersection> sections = new ArrayList<>();
            double z = -InterceptingTriangle3D.tolerance*2 + dz*i;
            double[] origin = {0, 0,  z};
            double[] direction = Vector3DOps.xhat;

            triangle.getIntersection(origin, direction, sections);
            x[i] = z/InterceptingTriangle3D.tolerance;

            if(sections.size() > 0){
                y[i] = sections.get(0).dirty/InterceptingTriangle3D.tolerance;
            } else{
                y[i] = -1;
            }
        }

        Graph g = new Graph();
        g.addData(x, y);
        g.show(true, "Dirty: triangle 1");
    }
    public static void main(String[] args) throws Exception{

        brokenMeshes();

        /*
        double[] a = {0.1, -0.1, 0};
        double[] b = {0.1, 0, 0.1};
        double[] c = {0.1, 0.1, 0};

        runTriangle(new InterceptingTriangle3D(a, b, c));
        runTriangle(new InterceptingTriangle3D(b, c, a));
        runTriangle(new InterceptingTriangle3D(c, a, b));
        */





    }
}
