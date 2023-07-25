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

import java.io.File;
import java.io.IOException;
import java.util.List;

class InterceptingTriangle3D {
    double[] u, v;
    double[] normal;
    double[] nhat;
    double area;

    double[] a, b, c;
    double oneOver4ASquared;

    public final static double tolerance = 1e-10;

    /**
     * For intercepting a triangle.
     *
     *
     * @param triangle Uses the 3 points of the triangle to define the intercepting triangle.
     */
    public InterceptingTriangle3D(Triangle3D triangle) {
        this(
                triangle.A.getCoordinates(),
                triangle.B.getCoordinates(),
                triangle.C.getCoordinates()
        );

    }

    /**
     * Three points to represent a triangle that will be interecepted.
     *
     * This will create the normalized set of coordinates u, v for determining if a point on in the plane
     * lies within the triangle.
     *
     * @param a
     * @param b
     * @param c
     */
    public InterceptingTriangle3D(double[] a, double[] b, double[] c) {
        u = Vector3DOps.difference(b, a);
        v = Vector3DOps.difference(c, a);

        //three points define a triangle and a plain
        normal = Vector3DOps.cross(u, v);
        nhat = new double[] {normal[0], normal[1], normal[2]};
        area = Vector3DOps.normalize(nhat)/2;
        oneOver4ASquared = 1.0 / Vector3DOps.dot(normal, normal);

        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * Finds the Intersection of a ray originating at the origin and traveling along the direction.
     * The resulting intersection will have a distance from the original along the direction.
     *
     * It will also have a "dirty" value which represents if the intersection is with a
     * tolerance distance of the edge.
     *
     * The test for intersection is calculated using the technique of Computing the Barycentric
     * Coordinates of a Projected Point[1]. There is also a tolerance within which the point is
     * ambiguous. This is represented by the 'dirty' value.
     *
     *
     *
     * cite:
     *    [1]   Heidrich, Wolfgang.  "Computing the barycentric coordinates of a projected point."
     *          Journal of Graphics Tools 10.3 (2005): 9-12.
     *
     * @param origin origin of intercepting ray
     * @param direction direction of intercepting ray
     * @param result where any intersections will be placed.
     */
    void getIntersection(double[] origin, double[] direction, List<Intersection> result) {
        //project of direction along normal.
        double rn = Vector3DOps.dot(direction, nhat);
        if (rn == 0) {
            //parallel.
            return;
        }
        //distance along normal to plane.
        double d = Vector3DOps.dot(nhat, Vector3DOps.difference(a, origin));
        //distance along direction to plane.
        double s = d / rn;

        //point of plane intersection.
        double[] intercept = Vector3DOps.add(origin, direction, s);


        double[] w = Vector3DOps.difference(intercept, a);

        //calculate the bary centric coordinates.
        double b2 = Vector3DOps.dot( Vector3DOps.cross(u, w), normal) * oneOver4ASquared;
        double b1 = Vector3DOps.dot( Vector3DOps.cross(w, v), normal) * oneOver4ASquared;
        double b0 = 1 - b1 - b2;

        //this will include points that are with tolerance of the triangle.
        if (b2 >= -tolerance && b1 >= -tolerance && b0 >= -tolerance ) {
            Intersection toAdd = new Intersection(intercept, nhat);
            result.add(toAdd);
            double dirty =
                            ( b2 < tolerance ? tolerance - b2 : 0 ) +
                            ( b1 < tolerance ? tolerance - b1 : 0 ) +
                            (b0 < tolerance ? tolerance - b0 : 0 );
            toAdd.setDirty(dirty);

        }

    }
}
