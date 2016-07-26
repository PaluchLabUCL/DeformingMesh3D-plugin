package deformablemesh.geometry;

import deformablemesh.util.Vector3DOps;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by msmith on 4/21/16.
 */
public class SphereTest {
    @Test
    public void testIntersections(){
        double r = 0.5;
        double tolerance = 1e-6;
        double[] center= new double[]{0, 0, 0};
        Sphere s = new Sphere(center, r);
        for(double[] fs: AngleGenerator.generator(8, 5)){
            for (int i = 0; i < 10; i++) {
                int count = 0;
                double st = 2.5/9*i - 1.25;
                for (double[] normal : AngleGenerator.generator(8, 5)) {
                    double[] origin = new double[]{fs[0]*st*r, fs[1]*st*r, fs[2]*st*r};
                    List<Intersection> is = s.getIntersections(origin, normal);
                    count += is.size();
                    for (Intersection section : is) {
                        //check that points lie on the sphere.
                        double sep = Vector3DOps.distance(section.location, center);

                        Assert.assertEquals(sep, r, tolerance);

                        double dot = Vector3DOps.dot(
                                section.surfaceNormal,
                                Vector3DOps.difference(section.location, center)
                        );

                        Assert.assertEquals(1, Vector3DOps.mag(section.surfaceNormal), tolerance);
                        Assert.assertEquals(r, dot, tolerance);
                    }
                }
            }
        }
    }

}
