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

import java.util.Arrays;

/**
 * Created by msmith on 5/21/14.
 */
public class Vector3DOps {
    public final static double[] zhat = {0,0,1};
    public final static double[] yhat = {0,1,0};
    public final static double[] xhat = {1, 0, 0};
    public final static double[] nzhat = {0,0,-1};
    public final static double[] nyhat = {0,-1,0};
    public final static double[] nxhat = {-1, 0, 0};
    public final static double TOL = 1e-16;
    public static double[] cross(double[] a, double[] b){
        return new double[]{
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - b[2]*a[0],
                a[0]*b[1] - a[1]*b[0]
        };

    }

    public static double dot(double[] a, double[] b){
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    public static double sqrt(double v){
        return Math.sqrt(v);
    }

    /**
     * Normalizes the vector in place and returns the original length.
     * @param v
     * @return
     */
    public static double normalize(double[] v ){
        double m = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        v[0] /= m; v[1]/=m; v[2]/=m;
        return m;
    }
    public static double square(double a){
        return a*a;
    }
    public static double distance(double[] a, double[] b){
        return Math.sqrt(square(a[0]-b[0])+square(a[1]-b[1])+square(a[2]-b[2]));
    }

    public static double mag(double[] v){
        return sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
    }

    /**
     *
     * @param minuend positive value
     * @param subtrahend negative value
     * @return minuend - subtrahend
     */
    public static double[] difference(double[] minuend, double[] subtrahend) {
        return new double[]{minuend[0] - subtrahend[0], minuend[1] - subtrahend[1], minuend[2] - subtrahend[2]};
    }

    /**
     * How many dr's does it take to fill an l. Returns the absolute value of dr/l or Infinity if dr==0.
     *
     * @param dr chunk size.
     * @param l length to be spanned.
     *
     */
    public static double toSpan(double dr, double l){
        return dr == 0 ? Double.POSITIVE_INFINITY : dr<0 ? -l/dr : l/dr;
    }

    /**
     * Displacing point a by the distance f along vector b.
     *
     * @param a starting point.
     * @param b translation vector
     * @param f distance translated.
     * @return a new double containing a + fb.
     */
    public static double[] add(double[] a, double[] b, double f){
        return new double[]{
                a[0] + f*b[0],
                a[1] + f*b[1],
                a[2] + f*b[2]
        };
    }

    /**
     * Checks if a and be are within the range of r by summing the square of differences
     * and squaring r.
     *
     * @param a center of sphere
     * @param b point to be checked
     * @param r radius
     * @return if the distance between a and b is less than or equal to r.
     */
    static public boolean proximity(double[] a, double[] b, double r){
        return square(a[0]-b[0]) + square(a[1]-b[1]) + square(a[2]-b[2]) <= r*r;
    }

    static public double minLength(double[] l){
        return l[0] > l[1] ?
            l[1] > l[2] ? l[2] : l[1]
                :
            l[0] > l[2] ? l[2] : l[0];
    }

    /**
     * Finds the shortest component of the input vector, creates a unit vector
     * along that axis and then uses the cross product to get a mutually exclusive vector.
     *
     * @param o
     * @return
     */
    public static double[] getPerpendicularNormalizedVector(double[] o){
        double lx = abs(o[0]);
        double ly = abs(o[1]);
        double lz = abs(o[2]);

        double[] result = new double[3];
        if(lx<ly){
            if(lz<lx){
                result[2] = 1;
            } else{
                result[0] = 1;
            }
        } else{
            if(lz<ly){
                result[2] = 1;
            } else{
                result[1] = 1;
            }
        }
        double[] normal = cross(o, result);
        normalize(normal);

        return normal;
    }

    public static double abs(double v){
        if(v==0) return 0;
        return v<0?-v:v;
    }

    public static double[] average(double[]... v ) {
        int l = v.length;
        double[] f = new double[v[0].length];
        for(double[] c : v){
            f[0] += c[0];
            f[1] += c[1];
            f[2] += c[2];
        }
        f[0] = f[0]/l;
        f[1] = f[1]/l;
        f[2] = f[2]/l;

        return f;
    }
    static public double[] rotatePoint( double[] p, double[] axisAngle, double[] center){

        double[] p0 = difference(p, center);
        double[] axis = Arrays.copyOf(axisAngle, 3);
        double angle = normalize(axis);

        double[] e1 = getPerpendicularNormalizedVector(axis);
        double[] e2 = Vector3DOps.cross(axis, e1);
        double parallel = Vector3DOps.dot(axis, p0);
        double p1 = dot(e1, p0);
        double p2 = dot(e2, p0);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double p1p = p1*cos - p2*sin;
        double p2p = p1*sin + p2*cos;
        return add( add( add(center, axis, parallel), e1, p1p), e2, p2p);
    }
}
