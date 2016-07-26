package deformablemesh.externalenergies;


import deformablemesh.geometry.ContractileRing;

/**
 *
 * This energy will attract a mesh to a curve, eg a contractile ring.
 *
 * Created by msmith on 10/22/15.
 */
public class CurveBasedEnergy implements ExternalEnergy{
    double k;
    ContractileRing ring;
    public CurveBasedEnergy(ContractileRing ring, double weight){
        this.ring=ring;
        k=weight;
    }
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        for(int i = 0; i<positions.length/3; i++){
            int n = 3*i;
            double[] f = ring.getDifference(i, positions[n], positions[n+1], positions[n+2]);
            fx[i] += f[0]*k;
            fy[i] += f[1]*k;
            fz[i] += f[2]*k;
        }

    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
