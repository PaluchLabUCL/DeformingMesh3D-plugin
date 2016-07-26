package deformablemesh.geometry;

import java.util.List;

/**
 * Created by msmith on 2/8/16.
 */
public interface Interceptable {
    List<Intersection> getIntersections(double[] origin, double[] direction);
    default boolean contains(double[] point){
        return false;
    }
}
