package deformablemesh.geometry;

import deformablemesh.util.Vector3DOps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static deformablemesh.util.Vector3DOps.TOL;

/**
 * A 3D box, with edges along x-y-z axis.
 *
 * Created by msmith on 5/28/14.
 */
public class Box3D implements Interceptable{
    public double[] low;
    public double[] high;
    List<AxisPlane> planes = new ArrayList<>();

    public Box3D(double[] center, double width, double length, double height){
        low = new double[]{
                center[0] - 0.5*width,
                center[1] - 0.5*length,
                center[2] - 0.5*height
        };

        high = new double[]{
                center[0] + 0.5*width,
                center[1] + 0.5*length,
                center[2] + 0.5*height
        };
        createPlanes();
    }

    public Box3D(double minx, double miny, double minz, double maxx, double maxy, double maxz) {
        low = new double[]{minx, miny, minz};
        high = new double[]{maxx, maxy, maxz};
        createPlanes();
    }

    void createPlanes(){
        AxisPlane plusX = new AxisPlane(
            new double[]{high[0] - TOL, low[1] + high[1], low[2] + high[2]},
            Vector3DOps.xhat
        );
        planes.add(plusX);

        AxisPlane minusX = new AxisPlane(
            new double[]{low[0] + TOL, low[1] + high[1], low[2] + high[2]},
            Vector3DOps.nxhat
        );
        planes.add(minusX);

        AxisPlane plusY = new AxisPlane(
            new double[]{low[0] + high[1], high[1] - TOL, low[2] + high[2]},
            Vector3DOps.yhat
        );
        planes.add(plusY);

        AxisPlane minusY = new AxisPlane(
                new double[]{high[0] + low[0], low[1] + TOL, low[2] + high[2]},
                Vector3DOps.nyhat
        );
        planes.add(minusY);

        AxisPlane plusZ = new AxisPlane(
                new double[]{high[0] + low[0], low[1] + high[1], high[2]- TOL},
                Vector3DOps.zhat
        );
        planes.add(plusZ);

        AxisPlane minusZ = new AxisPlane(
                new double[]{high[0] + low[0], low[1] + high[1], low[2]+ TOL},
                Vector3DOps.nzhat
        );
        planes.add(minusZ);
   }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        List<Intersection> intersections = new ArrayList<>();
        for(AxisPlane plane: planes){
            for(Intersection section: plane.getIntersections(origin, direction)){
                if(contains(section.location)){
                    intersections.add(section);
                }
            }
        }
        return intersections;
    }

    public boolean contains(double[] point){

        return point[0]>=low[0] && point[0]<=high[0]
                && point[1]>=low[1] && point[1]<=high[1]
                && point[2]>=low[2] && point[2]<=high[2];

    }

}

class AxisPlane implements Interceptable{
    double[] position;
    double[] normal;

    List<Intersection> sections = new ArrayList<>(1);

    public AxisPlane(double[] pos, double[] norm){
        position = pos;
        normal = norm;
    }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        sections.clear();

        double[] r = Vector3DOps.difference(position, origin);
        double dot = Vector3DOps.dot(r, normal);
        double cosTheta = Vector3DOps.dot(direction, normal);
        if(cosTheta!=0){
            //dot is the distance along the normal.
            double m = dot/cosTheta;

            sections.add(new Intersection(
                    Vector3DOps.add(origin, direction, m),
                    normal
            ));
        } else{
            //parallel
            sections.add(Intersection.inf(normal));
        }

        return Collections.unmodifiableList(sections);
    }
}
