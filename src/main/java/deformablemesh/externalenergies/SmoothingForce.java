package deformablemesh.externalenergies;

import deformablemesh.geometry.Connection3D;
import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmoothingForce implements ExternalEnergy{
    double magnitude;
    CurvatureCalculator calc;
    public SmoothingForce(DeformableMesh3D mesh, double magnitude){
        this.magnitude = magnitude;
        calc = new CurvatureCalculator(mesh);
    }
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        //x, y, z, Kappa, nx, ny, nz.
        List<double[]> curvatures = calc.calculateCurvature();
        for(int i = 0; i<fx.length; i++){
            double[] row = curvatures.get(i);
            double f = row[3]*magnitude;
            fx[i] += -row[4]*f;
            fy[i] += -row[5]*f;
            fz[i] += -row[6]*f;
        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
