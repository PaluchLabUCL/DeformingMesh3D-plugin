package deformablemesh.util.astar;

import java.util.List;

public interface PossiblePath<T>{

    public T getEndPoint();
    public void addPoint(T t, double cost, double heuristic);
    public double getDistance();
    public double getHeuristic();
    public List<T> getPath();
    public PossiblePath<T> duplicate();
    public boolean sameDestination(T destination);

}
