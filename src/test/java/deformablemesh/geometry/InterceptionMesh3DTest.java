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

import deformablemesh.util.Vector3DOps;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by msmith on 4/21/16.
 */
public class InterceptionMesh3DTest {
    final double TOL = 1e-9;

    /**
     * Creates a sphere mesh and checks a range of angles that the mesh intercepts the mesh
     */
    @Test
    public void testInterceptingSphere(){
        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(0);

        InterceptingMesh3D ints = new InterceptingMesh3D(mesh);
        double[] o = ints.center;

        RayCastMesh.rayCastMesh(ints, o, 1);

        for(double[] angle: AngleGenerator.generator(25, 15)){
            List<Intersection> is = ints.getIntersections(o, angle);
            Assert.assertTrue(is.size()>=2);
            for(Intersection i: is){
                Assert.assertTrue(Vector3DOps.mag(i.location) <= 1+TOL);
            }
        }

    }
}
