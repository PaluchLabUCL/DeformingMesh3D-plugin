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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by msmith on 3/10/16.
 */
public class Vector3DOpsTest {
    final static double[] x = {1, 0, 0};
    final static double[] y = {0, 1, 0};
    final static double[] z = {0, 0, 1};
    final static double[] zero = {0, 0, 0};

    /**
     * Checks the cross product definitions
     *      x cross y = z
     *      y cross z = x
     *      z cross x = y
     *      x cross x = 0
     *      y cross y = 0
     *      z cross z = 0
     */
    @Test
    public void testCrossProductDefinitions(){


        Assert.assertArrayEquals(Vector3DOps.cross(x, y), z, 0);
        Assert.assertArrayEquals(Vector3DOps.cross(y, z), x, 0);
        Assert.assertArrayEquals(Vector3DOps.cross(z, x), y, 0);

        Assert.assertArrayEquals(Vector3DOps.cross(x, x), zero, 0);
        Assert.assertArrayEquals(Vector3DOps.cross(y, y), zero, 0);
        Assert.assertArrayEquals(Vector3DOps.cross(z, z), zero, 0);

    }

    /**
     * Checks the directions x, y, z such that x dot x = 1; x dot y = 0; etc.
     */
    @Test
    public void testDotDefinitions(){

        Assert.assertEquals(Vector3DOps.dot(x, x), 1.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(y, y), 1.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(z, z), 1.0, 0.0);

        Assert.assertEquals(Vector3DOps.dot(x, y), 0.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(y, x), 0.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(x, z), 0.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(z, x), 0.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(y, z), 0.0, 0.0);
        Assert.assertEquals(Vector3DOps.dot(z, y), 0.0, 0.0);

    }

    @Test
    public void testPerpendicularNormalVector(){

        double[] values = {-0.5, 0.5};

        for(double x: values){
            for(double y: values){
                for(double z: values){
                    testVectorForPerpendicularNormalization(new double[]{x,y,z});
                }
            }
        }

    }

    @Test
    public void testProximity(){
        double[] values = {-0.1, 0, 0.1};
        for(double x: values){
            for(double y: values){
                for(double z: values){
                    testProximityVector(new double[]{x,y,z});
                }
            }
        }
    }

    public void testProximityVector(double[] origin){
        double[] values = {-0.5, 0.5};

        for(double x: values){
            for(double y: values){
                for(double z: values){
                    double m = Vector3DOps.distance(new double[]{x,y,z}, origin);
                    double[] r = Vector3DOps.difference(new double[]{x,y,z}, origin);
                    Assert.assertEquals(Vector3DOps.mag(r), m, 1e-12);
                    Assert.assertTrue(Vector3DOps.proximity(new double[]{x,y,z}, origin, m*1.01));
                    Assert.assertFalse(Vector3DOps.proximity(new double[]{x,y,z}, origin, m*0.5));
                }
            }
        }

    }

    public void testVectorForPerpendicularNormalization(double[] sample){
        double[] normal = Vector3DOps.getPerpendicularNormalizedVector(sample);
        //perpendicular
        Assert.assertEquals(Vector3DOps.dot(normal, sample), 0, 1e-16);
        //normal
        Assert.assertEquals(Vector3DOps.mag(normal), 1, 1e-12);


        Assert.assertEquals(Vector3DOps.mag(Vector3DOps.cross(sample, normal)), Vector3DOps.mag(sample), 1e-16 );

        //normalize.
        double m = Vector3DOps.normalize(sample);
        Assert.assertEquals(Vector3DOps.mag(Vector3DOps.cross(sample, normal)), 1, 1e-12 );

    }

    @Test
    public void testToSpan(){

        double[] values = {-0.1,0.1};
        for(double x: values){
            for(double y: values){
                for(double z: values){
                    double[] measure = new double[]{x, y, z};
                    double l = Vector3DOps.sqrt(Vector3DOps.dot(measure, measure));
                    double[] x2 = Vector3DOps.add(measure, measure, 2);
                    double l2 = Vector3DOps.mag(x2);
                    Assert.assertEquals(Vector3DOps.toSpan(l, l2), 3, 1e-12);
                    double[] z1 = Vector3DOps.add(x2, measure, -1);
                    double l3 = Vector3DOps.mag(z1);
                    Assert.assertEquals(Vector3DOps.toSpan(l, l3), 2, 1e-12);

                }
            }
        }

    }

    @Test
    public void minLengthCheck(){
        double[] a = {0.25, 1, 1};
        double[] b = {1, 0.25, 1};
        double[] c = {1, 1, 0.25};
        Assert.assertEquals(Vector3DOps.minLength(a), 0.25, 0);
        Assert.assertEquals(Vector3DOps.minLength(b), 0.25, 0);
        Assert.assertEquals(Vector3DOps.minLength(c), 0.25, 0);
        Assert.assertEquals(1.0/Vector3DOps.abs(0), 1.0/Vector3DOps.abs(-0), 0);
    }

    @Test
    public void testDotSquare(){
        double[] a = {0.25, 1, 1};
        Assert.assertEquals(Vector3DOps.dot(a,a), Vector3DOps.square(a[0]) + Vector3DOps.square(a[1]) + Vector3DOps.square(a[2]), 0 );
        Assert.assertEquals(Vector3DOps.dot(x, Vector3DOps.zhat), 0, 0);
        Assert.assertEquals(Vector3DOps.dot(y, Vector3DOps.zhat), 0, 0);
        Assert.assertEquals(Vector3DOps.dot(z, Vector3DOps.zhat), 1, 0);
    }
}
