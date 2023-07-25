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
package deformablemesh.meshview;

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Triangle3D;
import org.scijava.java3d.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlotSurface implements DataObject {
    Shape3D surface_object;
    IndexedTriangleArray surfaces;
    BranchGroup group;
    public float[] generateNormals(double[] positions, int[] triangle_index){

        Map<Integer, List<Triangle3D>> map = new HashMap<>();
        float[] normals = new float[positions.length];
        for(int i = 0; i<positions.length/3; i++){
            map.put(i, new ArrayList<>());
        }
        for(int i = 0; i<triangle_index.length/3; i++){
            Node3D A = new Node3D(positions, triangle_index[3*i + 0]);
            Node3D B = new Node3D(positions, triangle_index[3*i + 1]);
            Node3D C = new Node3D(positions, triangle_index[3*i + 2]);

            Triangle3D tri = new Triangle3D(A,B,C);
            map.get(A.index).add(tri);
            map.get(B.index).add(tri);
            map.get(C.index).add(tri);

        }
        for(int i = 0; i<positions.length/3; i++){
            double[] n = CurvatureCalculator.calculateMeanNormal(new Node3D(positions, i), map.get(i));
            normals[3*i] = (float)n[0];
            normals[3*i + 1] = (float)n[1];
            normals[3*i + 2] = (float)n[2];
        }

        return normals;
    }

    public Appearance createAppearance(){
        Appearance a = new Appearance();
        return a;
    }
    public PlotSurface(double[] positions, int[] triangle_index, float[] colors){
        int N = positions.length/3;
        surfaces = new IndexedTriangleArray(N, GeometryArray.COORDINATES|GeometryArray.NORMALS|GeometryArray.COLOR_3, triangle_index.length);
        surfaces.setCoordinates(0,positions);
        surfaces.setCoordinateIndices(0,triangle_index);
        surfaces.setNormals(0, generateNormals(positions, triangle_index));
        surfaces.setNormalIndices(0, triangle_index);
        surfaces.setColors(0, colors);
        surfaces.setColorIndices(0, triangle_index);
        surfaces.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        surfaces.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
        surfaces.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
        surface_object = new Shape3D(surfaces);
        surface_object.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        surface_object.setAppearance(createAppearance());


        group = new BranchGroup();
        group.setCapability(BranchGroup.ALLOW_DETACH);
        group.addChild(surface_object);
    }

    @Override
    public BranchGroup getBranchGroup() {
        return group;
    }
}
