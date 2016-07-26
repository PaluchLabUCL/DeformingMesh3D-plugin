package deformablemesh.util.astar;

public interface History<T>{
    public double visited(T t);
    public void visit(T t, double d);
}
