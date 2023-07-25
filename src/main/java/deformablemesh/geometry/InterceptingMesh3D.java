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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * For performing Ray Casting on a deformable mesh.
 *
 * Created by msmith on 2/9/16.
 */
public class InterceptingMesh3D implements Interceptable{

    final DeformableMesh3D mesh;
    List<InterceptingTriangle3D> triangles;
    double[] center;
    Box3D bounds;
    public InterceptingMesh3D(DeformableMesh3D mesh){
        mesh.triangles.forEach(Triangle3D::update);
        this.mesh = mesh;
        triangles = mesh.triangles.stream().map(InterceptingTriangle3D::new).collect(Collectors.toList());
        center= new double[3];
        double[] a;
        double sum = 0;
        bounds = mesh.getBoundingBox();
        for(Triangle3D triangle: mesh.triangles){
            double area = triangle.area;
            if(area<=0){
                System.out.println("broken triangle!");
            }
            a = triangle.A.getCoordinates();
            center[0] += a[0]*area;
            center[1] += a[1]*area;
            center[2] += a[2]*area;
            a = triangle.B.getCoordinates();
            center[0] += a[0]*area;
            center[1] += a[1]*area;
            center[2] += a[2]*area;
            a = triangle.C.getCoordinates();
            center[0] += a[0]*area;
            center[1] += a[1]*area;
            center[2] += a[2]*area;
            sum += 3*area;
        }

        double f = 1.0/(sum);
        if(Double.isNaN(f)){
            System.out.println("broken mesh!");
            double[] bb = bounds.getCenter();
            center[0] = bb[0];
            center[1] = bb[1];
            center[2] = bb[2];
        } else {
            center[0] *= f;
            center[1] *= f;
            center[2] *= f;
        }
    }

    /**
     * Checks if pt is contained in this meshes bounds.
     *
     * @param pt
     * @return
     */
    public boolean boundsContains(double[] pt){
        return bounds.contains(pt);
    }
    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        List<Intersection> sections = new ArrayList<>();
        for(InterceptingTriangle3D triangle: triangles){
            List<Intersection> old = new ArrayList(sections);
            triangle.getIntersection(origin, direction, sections);
            /*for(int j = 0; j<old.size(); j++) {
                for (int i = old.size(); i < sections.size(); i++) {
                    double m = Vector3DOps.mag(Vector3DOps.difference(sections.get(i).location, sections.get(j).location));
                    if (m <= 0) {
                        System.out.println("borked " + m);
                    }
                }
            }*/
        }
        return sections;
    }

    public double[] getCenter() {
        return center;
    }

    @Override
    public boolean contains(double[] pt) {
        if(!bounds.contains(pt)){
            return false;
        }
        List<Intersection> ints = getIntersections(pt, Vector3DOps.zhat);
        ints.sort((a,b)->Double.compare(a.location[2], b.location[2]));

        //start outside
        boolean outside = true;

        //cross intersections until we pass our point.
        for(Intersection inter: ints){
            if(inter.location[2]>pt[2]){
                //we have passed our point.
                break;
            } else{
                boolean facingUp = inter.surfaceNormal[2]>0;
                if(facingUp){
                    //surface is facing up. means we just left.
                    outside=true;
                } else{
                    outside=false;
                }
            }
        }
        return !outside;
    }
}

