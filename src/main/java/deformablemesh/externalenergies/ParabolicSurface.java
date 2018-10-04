package deformablemesh.externalenergies;

public class ParabolicSurface implements ExternalEnergy {
    double a = 2;
    double gravityMagnitude;
    double surfaceFactor;
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        for(int i = 0; i<fz.length; i++){
            double z = positions[i*3 + 2];
            double y = positions[i*3 + 1];
            double x = positions[i*3 + 0];
            double r = x*x + y*y;
            if(z<r*a){
                r = Math.sqrt(r);
                x = x/r;
                y = y/r;

                double dz_dr = 2*a*r;
                dz_dr = dz_dr*dz_dr;
                double dz = Math.sqrt(dz_dr/(1+dz_dr));
                double dr = Math.sqrt(1 - dz);
                fz[i] += -gravityMagnitude - (positions[3*i + 2]-2*r) * surfaceFactor*dr;
                fy[i] += -dz*y*surfaceFactor;
                fx[i] += -dz*x*surfaceFactor;
            }

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        if(pos[2]>0){
            return 0;
        }
        return -pos[2];
    }
}
