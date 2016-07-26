package deformablemesh.util.astar;

/**
 * An estimate of the cost to get find a path between two points.
 *
 * Created by msmith on 2/4/14.
 */
public interface BasicHeuristic<T> {

    /**
     * An estimate of cost to get from point a to a final destination.
     *
     * @param a
     * @return
     */
    public double getCost(T a);

}
