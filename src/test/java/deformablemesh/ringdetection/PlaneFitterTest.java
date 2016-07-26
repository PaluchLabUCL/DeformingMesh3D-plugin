package deformablemesh.ringdetection;

import deformablemesh.util.Vector3DOps;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by msmith on 4/21/16.
 */
public class PlaneFitterTest {
    final static double NOISE = 0.0;
    static List<double[]> noisyCircle(double[] normal){
        int dex = 0;
        double min = Math.abs(normal[0]);
        if(Math.abs(normal[1])<min){
            dex=1;
            min=normal[1];
        }
        if(Math.abs(normal[2])<min){
            dex=2;
        }
        double[] t1 = new double[3];
        t1[dex] = 1;
        System.out.println(Arrays.toString(t1) + Arrays.toString(normal));
        double dot = Vector3DOps.dot(t1, normal);
        System.out.println(dot);
        t1[0] -= dot*normal[0];
        t1[1] -= dot*normal[1];
        t1[2] -= dot*normal[2];

        double[] t2 = Vector3DOps.cross(normal, t1);
        System.out.println(Arrays.toString(t1) + Arrays.toString(t2)+ Arrays.toString(normal));
        List<double[]> points = new ArrayList<>();
        Random ng = new Random(1);
        double dtheta = 2*Math.PI/100;
        for(int i = 0; i<100; i++){
            double noise = NOISE*(0.5 - ng.nextDouble());
            double r = 5*(1 - Math.exp(-i/50)) + 1;
            double x = r*Math.sin(i*dtheta);
            double y = r*Math.cos(i*dtheta);

            points.add(new double[]{
                    x*t1[0] + y*t2[0] + noise*normal[0],
                    x*t1[1] + y*t2[1] + noise*normal[1],
                    x*t1[2] + y*t2[2] + noise*normal[2]
            });


        }
        return points;
    }

    @Test
    public void fitterWithCircleTest(){
        double[] normal = new double[]{Math.sqrt(2)/2, Math.sqrt(2)/2, 0};
        List<double[]> points = noisyCircle(normal);


        PlaneFitter.Plane p = PlaneFitter.findBestPlane(points);
        Assert.assertArrayEquals(normal, p.normal, Vector3DOps.TOL);


    }
}
