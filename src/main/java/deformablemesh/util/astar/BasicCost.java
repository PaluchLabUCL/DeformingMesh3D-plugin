package deformablemesh.util.astar;

/**
 * Actual cost that the current path will be.
 *
 * Created by msmith on 2/4/14.
 */
public interface BasicCost<T> {

    /**
     * This is used for next step predictions.
     *
     * @param a starting point
     * @param b ending point
     * @return the actual cost to move from a to b.
     */
    double step(T a, T b);

}
