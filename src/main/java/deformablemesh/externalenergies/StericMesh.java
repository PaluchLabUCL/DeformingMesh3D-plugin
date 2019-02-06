package deformablemesh.externalenergies;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;

public class StericMesh implements ExternalEnergy{
    InterceptingMesh3D mesh;
    final DeformableMesh3D deformableMesh;
    final double weight;
    boolean staticShape = true;
    public StericMesh(DeformableMesh3D a, double weight){
        //mesh = new InterceptingMesh3D(a);
        deformableMesh = a;
        this.weight=weight;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        if(!staticShape || mesh==null) {
            mesh = new InterceptingMesh3D(deformableMesh);
        }
        double[] pt = new double[3];
        double[] center = mesh.getCenter();
        for(int i = 0; i<fx.length; i++){
            pt[0] = positions[3*i];
            pt[1] = positions[3*i + 1];
            pt[2] = positions[3*i + 2];

            if(mesh.contains(pt)){
                double dx = pt[0] - center[0];
                double dy = pt[1] - center[1];
                double dz = pt[2] - center[2];
                double l = Math.sqrt(dx*dx + dy*dy + dz*dz);
                fx[i] += weight*dx/l;
                fy[i] += weight*dy/l;
                fz[i] += weight*dz/l;
            }


        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
