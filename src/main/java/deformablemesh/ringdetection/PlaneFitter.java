package deformablemesh.ringdetection;

import deformablemesh.util.Vector3DOps;
import org.orangepalantir.leastsquares.Fitter;
import org.orangepalantir.leastsquares.Function;
import org.orangepalantir.leastsquares.fitters.LinearFitter;

import java.util.List;

/**
 * Created by msmith on 1/5/16.
 */
public class PlaneFitter {
    public static class Plane{
        public double[] normal;
        public double[] center;
    }

    public static Plane findBestPlane(List<double[]> points){
        Function pFun = new PlaneFunction();
        Fitter fits = new LinearFitter(pFun);


        int j = points.size()/3;
        int k = 2*points.size()/3;

        double[] guess = normalFromThree(points.get(0), points.get(j), points.get(k));


        double[][] inputs = new double[points.size()][];
        double[] zs = new double[points.size()];
        double[] origin = new double[3];

        for(int i = 0; i<points.size(); i++){
            double[] pt = points.get(i);
            inputs[i] = pt;
            origin[0] += pt[0];
            origin[1] += pt[1];
            origin[2] += pt[2];
        }
        origin[0] /= points.size();
        origin[1] /= points.size();
        origin[2] /= points.size();

        double offset = origin[0]*guess[0] + origin[1] * guess[1] + origin[2]*guess[2];

        fits.setParameters(new double[]{guess[0], guess[1], guess[2], offset});
        fits.setData(inputs, zs);

        try{
            fits.fitData();
        } catch(Exception exc){
            //why do I even try.
        }

        double[] parameters = fits.getParameters();
        double[] normal = new double[]{parameters[0], parameters[1], parameters[2]};
        double factor = Vector3DOps.normalize(normal);
        parameters[0] = normal[0];
        parameters[1] = normal[1];
        parameters[2] = normal[2];
        parameters[3] = parameters[3]/factor;

        //give our origin should be close to a point on the plane.
        double error = pFun.evaluate(origin, parameters);
        if(error!=0){
            origin[0] -= error*normal[0];
            origin[1] -= error*normal[1];
            origin[2] -= error*normal[2];
        }
        Plane p = new Plane();
        p.normal = normal;
        p.center = origin;
        return p;
    }

    private static double[] normalFromThree(double[] a, double[] b, double[] c) {

        double[] r1 = Vector3DOps.difference(a, b);
        double[] r2 = Vector3DOps.difference(c, b);
        double[] ret = Vector3DOps.cross(r1, r2);
        double m = Vector3DOps.normalize(ret);
        return m>0?ret: new double[]{1, 0, 0};
    }

}


class PlaneFunction implements Function{

    @Override
    public double evaluate(double[] values, double[] parameters) {
        return values[0]*parameters[0] + values[1]*parameters[1] + values[2]*parameters[2] + parameters[3];
    }

    @Override
    public int getNParameters() {
        return 4;
    }

    @Override
    public int getNInputs() {
        return 3;
    }
}
