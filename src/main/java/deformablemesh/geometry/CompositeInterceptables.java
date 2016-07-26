package deformablemesh.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by melkor on 3/9/16.
 */
public class CompositeInterceptables implements Interceptable{
    List<Interceptable> objects= new ArrayList<>();

    public CompositeInterceptables(Collection<? extends Interceptable> collection){
        objects.addAll(collection);
    }

    public CompositeInterceptables(Interceptable... items){
        for(Interceptable item: items){
            objects.add(item);
        }
    }

    public void addInterceptable(Interceptable i){
        objects.add(i);
    }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        List<Intersection> ret = new ArrayList<>();
        for(Interceptable i: objects){
            ret.addAll(i.getIntersections(origin, direction));
        }
        return ret;
    }

    @Override
    public boolean contains(double[] point){
        for(Interceptable i: objects){
            if(i.contains(point)){
                return true;
            }
        }
        return false;
    }
}
