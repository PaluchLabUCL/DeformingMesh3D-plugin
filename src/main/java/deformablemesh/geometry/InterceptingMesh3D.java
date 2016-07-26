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

    public InterceptingMesh3D(DeformableMesh3D mesh){
        mesh.triangles.forEach(Triangle3D::update);
        this.mesh = mesh;
        triangles = mesh.triangles.stream().map(InterceptingTriangle3D::new).collect(Collectors.toList());
        center= new double[3];
        double[] a;
        double sum = 0;
        for(Triangle3D triangle: mesh.triangles){
            double area = triangle.area;
            if(area<=0){
                //this sucks.
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
        center[0] *=f;
        center[1] *=f;
        center[2] *=f;
    }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        List<Intersection> sections = new ArrayList<>();
        for(InterceptingTriangle3D triangle: triangles){
            triangle.getIntersection(origin, direction, sections);
        }

        return sections;
    }

    public double[] getCenter() {
        return center;
    }

    @Override
    public boolean contains(double[] pt) {
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

class InterceptingTriangle3D{
    double[] u, v;
    double[] normal;
    double[] a,b,c;
    double uv,uu, vv;
    double denom;
    final static double tolerance = 1e-8;
    public InterceptingTriangle3D(Triangle3D triangle){
        this(
            triangle.A.getCoordinates(),
            triangle.B.getCoordinates(),
            triangle.C.getCoordinates()
        );

    }
    public InterceptingTriangle3D(double[] a, double[] b, double[] c){
        u = Vector3DOps.difference(b, a);
        v = Vector3DOps.difference(c, a);
        //three points define a triangle and a plain
        normal = Vector3DOps.cross(u, v);
        Vector3DOps.normalize(normal);
        this.a = a;
        this.b = b;
        this.c = c;
        uv = Vector3DOps.dot(u, v);
        uu = Vector3DOps.dot(u, u);
        vv = Vector3DOps.dot(v, v);
        denom = 1.0/(uv*uv - uu*vv);

    }

    void getIntersection(double[] origin, double[] direction, List<Intersection> result){
        //project of direction along normal.
        double rn = Vector3DOps.dot(direction, normal);
        if(rn==0){
            //parallel.
            return;
        }
        //distance along normal to plane.
        double d = Vector3DOps.dot(normal, Vector3DOps.difference(a, origin));
        double s = d/rn;
        double[] intercept = Vector3DOps.add(origin, direction, s);
        double[] inPlane = Vector3DOps.difference(intercept, a);

        double wv = Vector3DOps.dot(inPlane,v);
        double wu = Vector3DOps.dot(inPlane, u);

        double s1 = (uv*wv - vv*wu)*denom;
        double t1 = (uv*wu - uu*wv)*denom;

        if(s1>=-tolerance&&t1>=-tolerance&&t1+s1<=1+2*tolerance){
            result.add(new Intersection(intercept, normal));
        }

    }




}