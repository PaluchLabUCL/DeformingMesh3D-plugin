package deformablemesh.externalenergies;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;

public class VolumeConservation  implements ExternalEnergy{
    final DeformableMesh3D mesh;
    final double weight;
    double volume;

    CurvatureCalculator calculator;

    public VolumeConservation(DeformableMesh3D mesh, double weight){
        this.mesh = mesh;
        volume = mesh.calculateVolume(new double[]{0, 0, 1});
        double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);

        calculator = new CurvatureCalculator(mesh);

        this.weight = weight*1e8;

        System.out.println( weight/area/volume/volume/volume*2 + ", " + weight);



    }
    public double getVolume(){
        return volume;
    }

    public void setVolume(double v){
        this.volume = v;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double nv = mesh.calculateVolume(new double[]{0, 0, 1});

        double dv = (-nv + volume);
        double factor = weight*dv*dv*dv;
        for(int i = 0; i<positions.length/3; i++){
            double[] normal = calculator.getNormal(i);
            double area = calculator.calculateMixedArea(mesh.nodes.get(i));
            fx[i] += factor*area*normal[0];
            fy[i] += factor*area*normal[1];
            fz[i] += factor*area*normal[2];

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
