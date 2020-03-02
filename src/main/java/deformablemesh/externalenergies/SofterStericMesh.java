package deformablemesh.externalenergies;

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.geometry.Intersection;
import deformablemesh.util.Vector3DOps;

import java.util.List;
import java.util.stream.Collectors;

public class SofterStericMesh extends StericMesh {
    CurvatureCalculator curve;
    public SofterStericMesh(DeformableMesh3D id, DeformableMesh3D neighbor, double weight) {
        super(id, neighbor, weight);
        curve = new CurvatureCalculator(id);
    }

    public double[] getNormal(Integer i){
        double[] n = curve.getNormal(i);

        return n;
    }

    static class RotatedIntersection{
        double[] surface;
        double dot;

        /**
         * Creates an intersection that doesn't need a location because it is assumed to be along the normal.
         *
         * @param origin
         * @param normal
         * @param s
         */
        RotatedIntersection(double[] origin, double[] normal, Intersection s){
            double dx = s.location[0] - origin[0];
            double dy = s.location[1] - origin[1];
            double dz = s.location[2] - origin[2];

            dot = Vector3DOps.dot(new double[]{dx, dy , dz}, normal);
            surface = s.surfaceNormal;
        }
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        if(!staticShape || mesh==null) {
            mesh = new InterceptingMesh3D(deformableMesh);
        }


        double[] pt = new double[3];
        double[] center = mesh.getCenter();
        for(int i = 0; i<fx.length; i++){
            pt[0] = positions[3*i];
            pt[1] = positions[3*i + 1];
            pt[2] = positions[3*i + 2];

            if(mesh.boundsContains(pt)){
                double[] normal = getNormal(i);
                List<RotatedIntersection> intersections = mesh.getIntersections(pt, normal).stream().map(
                        is -> new RotatedIntersection(pt, normal, is)
                    ).collect(Collectors.toList());

                //sort the intersections by their distance from pt along the normal axis.
                intersections.sort((a,b)->Double.compare(a.dot, b.dot));

                //start outside
                boolean outside = true;

                //cross intersections until we pass our point.
                double penetration = 0;
                for(RotatedIntersection inter: intersections){
                    if(inter.dot>0){
                        //In the rotated intersection frame. the origin is zero.
                        break;
                    } else{
                        boolean facingUp = Vector3DOps.dot(inter.surface, normal)>0;
                        if(facingUp){
                            //surface is facing up. means we just left.
                            outside=true;
                        } else{
                            outside=false;
                            penetration = inter.dot;
                        }
                    }
                }
                if(!outside){
                    fx[i] += penetration*weight*normal[0]*100;
                    fy[i] += penetration*weight*normal[1]*100;
                    fz[i] += penetration*weight*normal[2]*100;
                }
            }
        }
    }

}
