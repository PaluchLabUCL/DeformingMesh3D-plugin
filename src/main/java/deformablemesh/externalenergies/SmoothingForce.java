package deformablemesh.externalenergies;

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.Node3D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmoothingForce implements ExternalEnergy{
    double gamma;
    CurvatureCalculator calc;
    Map<Node3D, Set<Node3D>> neighbors = new HashMap<>();

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        //go through the curvatures, and smooth according to neighbors.
        List<double[]> curvatures = calc.calculateCurvature();
        for(int i = 0; i<fx.length; i++){
            double[] row = curvatures.get(i);
            fx[i] += -row[0];
            fy[i] += -row[1];
            fz[i] += -row[2];
        }


    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
