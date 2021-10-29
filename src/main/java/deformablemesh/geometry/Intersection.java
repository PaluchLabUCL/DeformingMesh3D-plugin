package deformablemesh.geometry;

/**
 * Created by msmith on 2/8/16.
 */
public class Intersection {
    private static double[] INFINITY = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    public double[] location;
    public double[] surfaceNormal;
    public double dirty = 0;
    /**
     * Provides the relevant information about where a colision occured.
     *
     * @param pos location of the intersection.
     * @param n normal of the surface being intersected.
     */
    public Intersection(double[] pos, double[] n){
        location = pos;
        surfaceNormal = n;

    }
    public void setDirty(double d){
        dirty = d;
    }
    public static Intersection inf(double[] normal){
        return new Intersection(INFINITY, normal);
    }

}
