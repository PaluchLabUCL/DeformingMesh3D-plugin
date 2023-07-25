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
public class SphereTest {
    @Test
    public void testIntersections(){
        double r = 0.5;
        double tolerance = 1e-6;
        double[] center= new double[]{0, 0, 0};
        Sphere s = new Sphere(center, r);
        for(double[] fs: AngleGenerator.generator(8, 5)){
            for (int i = 0; i < 10; i++) {
                int count = 0;
                double st = 2.5/9*i - 1.25;
                for (double[] normal : AngleGenerator.generator(8, 5)) {
                    double[] origin = new double[]{fs[0]*st*r, fs[1]*st*r, fs[2]*st*r};
                    List<Intersection> is = s.getIntersections(origin, normal);
                    count += is.size();
                    for (Intersection section : is) {
                        //check that points lie on the sphere.
                        double sep = Vector3DOps.distance(section.location, center);

                        Assert.assertEquals(sep, r, tolerance);

                        double dot = Vector3DOps.dot(
                                section.surfaceNormal,
                                Vector3DOps.difference(section.location, center)
                        );

                        Assert.assertEquals(1, Vector3DOps.mag(section.surfaceNormal), tolerance);
                        Assert.assertEquals(r, dot, tolerance);
                    }
                }
            }
        }
    }

}
