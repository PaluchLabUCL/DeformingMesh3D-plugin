package deformablemesh.externalenergies;

/**
 *
 *
 * User: msmith
 * Date: 7/3/13
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExternalEnergy {

    void updateForces(double[] positions, double[] fx, double[] fy, double[] fz);
    double getEnergy(double[] pos);

}
