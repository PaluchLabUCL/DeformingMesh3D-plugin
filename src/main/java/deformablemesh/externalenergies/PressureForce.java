package deformablemesh.externalenergies;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Triangle3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msmith on 2/29/16.
 */
public class PressureForce implements ExternalEnergy{
    double PRESSURE;
    List<Triangle3D> triangles = new ArrayList<>();
    double MAX_AREA;
    public PressureForce(DeformableMesh3D mesh, double pressure){
        PRESSURE = pressure;
        triangles.addAll(mesh.triangles);
        for(Triangle3D triangle: triangles){
            triangle.update();
            MAX_AREA+=triangle.area;
        }
        MAX_AREA *= 2;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        int[] indexes = new int[3];
        double sum = 0;
        for(Triangle3D t: triangles) {

            t.update();
            sum += t.area;
        }
        double factor = PRESSURE/3.0;
        if(sum>MAX_AREA){
            factor = factor*Math.exp(-(sum-MAX_AREA));
        }

        for(Triangle3D t: triangles){
            t.getIndices(indexes);
            int a = indexes[0];
            int b = indexes[1];
            int c = indexes[2];

            double f_x = t.normal[0]* factor;
            double f_y = t.normal[1]* factor;
            double f_z = t.normal[2]* factor;

            fx[a] += f_x;
            fx[b] += f_x;
            fx[c] += f_x;

            fy[a] += f_y;
            fy[b] += f_y;
            fy[c] += f_y;

            fz[a] += f_z;
            fz[b] += f_z;
            fz[c] += f_z;

        }
    }



    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
