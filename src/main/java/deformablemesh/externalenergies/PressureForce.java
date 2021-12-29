package deformablemesh.externalenergies;

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;

/**
 * Created by msmith on 2/29/16.
 */
public class PressureForce implements ExternalEnergy{
    double PRESSURE;
    CurvatureCalculator calculator;
    double max_mixed_area;
    double area;
    double sigma;
    public PressureForce(DeformableMesh3D mesh, double pressure){
        calculator =  new CurvatureCalculator(mesh);

        for(Node3D n: mesh.nodes){
            area += calculator.calculateMixedArea(n);
        }

        max_mixed_area = area/mesh.nodes.size()*4;
        PRESSURE = pressure;
    }

    public void setMaxMixedArea(double m){
        max_mixed_area = m;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        //should we divide by the area?
        double factor = PRESSURE*0.3;

        double areaSum = 0;
        double maxed = 0;
        for(int i = 0; i<positions.length/3; i++){
            double[] normal = calculator.getNormal(i);
            double area_i = calculator.calculateMixedArea(i);
            double f;
            f = factor*Math.sqrt(area_i);

            fx[i] += f*normal[0];
            fy[i] += f*normal[1];
            fz[i] += f*normal[2];

            areaSum += area_i;
        }
        this.area = areaSum;
    }



    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }

    public double getMaxMixedArea() {
        return max_mixed_area;
    }
}
