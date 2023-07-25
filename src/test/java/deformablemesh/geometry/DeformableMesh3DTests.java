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
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by msmith on 4/21/16.
 */
public class DeformableMesh3DTests {
    final static double TOL = 1e-8;
    @Test
    public void volumeTest(){
        DeformableMesh3D mesh = DeformableMesh3DTools.createTestBlock();

        double v1 = mesh.calculateVolume(new double[]{1, 0, 0});

        double v2 = mesh.calculateVolume(new double[]{0,1,0});
        double v3 = mesh.calculateVolume(new double[]{0, 0, 1});

        Assert.assertEquals(v1, v2, TOL);
        Assert.assertEquals(v1, v3, TOL);

    }

    @Test
    public void testMeshConstructor(){
        DeformableMesh3D mesh = DeformableMesh3DTools.createTestBlock();
        DeformableMesh3D mesh2 = new DeformableMesh3D(mesh.positions, mesh.connection_index, mesh.triangle_index);

        for(int i = 0; i<mesh.triangles.size(); i++){
            Assert.assertEquals(mesh.triangles.get(i), mesh2.triangles.get(i));
        }

    }

    @Test
    public void testCurvature(){

        DeformableMesh3D xMesh = DeformableMesh3D.generateEdgeX();
        xMesh.scale(0.5, DeformableMesh3D.ORIGIN);
        xMesh.translate(new double[]{1, 0, 0});
        List<double[]> xCurves = xMesh.calculateCurvature();

        DeformableMesh3D yMesh = DeformableMesh3D.generateEdgeY();
        yMesh.scale(0.5, DeformableMesh3D.ORIGIN);
        yMesh.translate(new double[]{2, 1, 0});
        yMesh.calculateCurvature();
        List<double[]> yCurves = yMesh.calculateCurvature();

        for(int i = 0; i<xCurves.size(); i++){
            Assert.assertArrayEquals(xCurves.get(i), yCurves.get(i), TOL);
        }

        DeformableMesh3D zMesh = DeformableMesh3D.generateEdgeZ();
        zMesh.scale(0.5, DeformableMesh3D.ORIGIN);
        zMesh.translate(new double[]{0, 0, -1});
        zMesh.calculateCurvature();

    }

    @Test
    public void testRotations(){
        double[] xaxis = {1, 0, 0};

        DeformableMesh3D box = DeformableMesh3DTools.createRectangleMesh(2, 1, 1, 0.5);
        double before = DeformableMesh3DTools.calculateVolume(box.triangles);
        box.rotate(new double[]{0, Math.sqrt(2)/2, Math.sqrt(2)/2}, new double[]{0,0,0}, Math.PI*0.15);
        double after = DeformableMesh3DTools.calculateVolume(box.triangles);
        Assert.assertEquals(before, after, 1e-2);
    }

}
