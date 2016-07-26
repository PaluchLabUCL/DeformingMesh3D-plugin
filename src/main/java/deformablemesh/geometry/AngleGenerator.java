package deformablemesh.geometry;

import java.util.Iterator;

/**
 * Created by melkor on 2/8/16.
 */
public class AngleGenerator implements Iterator<double[]> {
    final int td;
    final int pd;

    int t = 0;
    int p = 0;
    double dtheta;
    double dphi;
    public AngleGenerator(int thetaDivisions, int phiDivisions){
        td = thetaDivisions;
        pd = phiDivisions;
        dtheta = Math.PI/(td-1)*2;
        dphi = Math.PI/(pd);
    }
    @Override
    public boolean hasNext() {
        return t<td&&p<pd;
    }

    @Override
    public double[] next() {
        if(p==0){
            p++;
            return new double[]{0,0,1};
        } else if(p==pd){
            t=td;
            return new double[]{0,0,-1};
        }
        double theta = dtheta*t;
        double phi = dphi*p;

        t++;
        if(t==td){
            t=0;
            p++;
        }

        return new double[]{Math.cos(theta)*Math.sin(phi), Math.sin(theta)*Math.sin(phi), Math.cos(phi)};
    }
    static Iterable<double[]> generator(int thetaDivisions, int phiDivisions){
        return () -> new AngleGenerator(thetaDivisions, phiDivisions);
    }
}
