package deformablemesh.geometry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Object should be created with the surface facing outwards. This class is a convenience to create a surface facing
 * inwards. For example a mesh that stops ray-casting from passing through it the normals would face in.
 *
 * Essentially the outside is now the inside.
 * Created by msmith on 4/8/16.
 */
public class InsideOutInterceptable implements Interceptable {
    Interceptable regular;
    public InsideOutInterceptable(Interceptable interceptable){
        regular = interceptable;
    }
    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        return regular.getIntersections(origin, direction).stream().map(InsideOutInterceptable::invert).collect(Collectors.toList());
    }
    static Intersection invert(Intersection section){
        return new Intersection(section.location, invert(section.surfaceNormal));
    }

    static double[] invert(double[] d){
        return new double[]{-d[0], -d[1], -d[2]};
    }
}
