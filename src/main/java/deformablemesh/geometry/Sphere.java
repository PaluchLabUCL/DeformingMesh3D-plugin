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

import deformablemesh.meshview.SphereDataObject;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.Vector3DOps;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * For creating new meshes.
 *
 * Created by msmith on 2/8/16.
 */
public class Sphere implements Projectable, Interceptable{
    double[] center;
    double radius;

    SphereDataObject obj;
    public Sphere(double[] center, double radius){
        this.center = center;
        this.radius = radius;
    }

    /**
     * Creates a new data object or re-uses the old one.
     * @return
     */
    public SphereDataObject createDataObject(){
        if(obj==null){
            obj=new SphereDataObject(center, radius);
        }
        return obj;
    }


    public Shape getProjection(FurrowTransformer transformer){
        double[] xy = transformer.getPlaneCoordinates(center);
        double s = transformer.distanceToPlane(center);
        double r = 0;
        if(radius>s){
            r = Math.sqrt(radius*radius - s*s)*transformer.scale;
        }
        return new Ellipse2D.Double(xy[0]-r, xy[1]-r, 2*r, 2*r);

    }
    public double[] getCenter(){
        return center;
    }

    public void setRadius(double r){
        radius = r;
        update();
    }

    public double getRadius(){
        return radius;
    }

    public void setRadius(double[] pt2){
        setRadius(Vector3DOps.mag(Vector3DOps.difference(pt2, center)));
    }

    public void update(){
        if(obj!=null){
            obj.setRadius(radius);
            obj.moveTo(center[0], center[1], center[2]);
        }
    }

    public void moveTo(double[] pt){
        center[0] = pt[0];
        center[1] = pt[1];
        center[2] = pt[2];
        update();
    }


    public void moveBy(double[] delta){
        center[0] += delta[0];
        center[1] += delta[1];
        center[2] += delta[2];
        update();
    }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        List<Intersection> intersections = new ArrayList<>();
        //line from center to origin.
        double[] r = Vector3DOps.difference(center, origin);
        double s = Vector3DOps.dot(r, direction);
        double m = Vector3DOps.mag(r);
        double distance = m*m - s*s;
        if(distance>0){
            distance = Math.sqrt(distance);
        } else if(distance>-10*Vector3DOps.TOL){
            distance = 0;
        } else{
            throw new RuntimeException("Error in Sphere interception out of tolerance!");
        }
        if(distance>radius){
            return intersections;
        }

        double arm = Math.sqrt(radius*radius - distance*distance);

        double[] pt1 = {
                origin[0] + (s-arm)*direction[0],
                origin[1] + (s-arm)*direction[1],
                origin[2] + (s-arm)*direction[2]
        };

        double[] d1 = Vector3DOps.difference(pt1, center);
        Vector3DOps.normalize(d1);
        intersections.add(new Intersection(pt1, d1));

        double[] pt2 = {
                origin[0] + (s+arm)*direction[0],
                origin[1] + (s+arm)*direction[1],
                origin[2] + (s+arm)*direction[2]
        };

        double[] d2 = Vector3DOps.difference(pt2, center);
        Vector3DOps.normalize(d2);
        intersections.add(new Intersection(pt2, d2));


        return intersections;
    }

    @Override
    public boolean contains(double[] v) {
        return Vector3DOps.proximity(v, center, radius);
    }
}
