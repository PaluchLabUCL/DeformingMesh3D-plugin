package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.StericMesh;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.NewtonMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.MeshFrame3D;

/**
 * This class is to test deforming two meshes that are influenced by "gravity", each other and a surface.
 */
public class TwoDrops {
    DeformableMesh3D a;
    DeformableMesh3D b;
    double gravityMagnitude = -0.001;
    double surfaceFactor = 0.01;
    double volumeConservation = 0.1;
    double steric = 10;
    public TwoDrops(){
        Sphere sA = new Sphere(new double[]{-0.075, 0, 0.5}, 0.1);
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2));
        a = RayCastMesh.rayCastMesh(sA, sA.getCenter(), 3);
        a.GAMMA = 500;
        a.ALPHA = 0.1;
        a.BETA = 0.1;
        a.reshape();
        a.setShowSurface(true);
        Sphere sB = new Sphere(new double[]{0.075, 0, 0.5}, 0.1);
        //b = new NewtonMesh3D(RayCastMesh.rayCastMesh(sB, sB.getCenter(), 2));
        b = RayCastMesh.rayCastMesh(sB, sB.getCenter(), 3);

        b.ALPHA = 0.1;
        b.BETA = 0.1;
        b.GAMMA = 500;
        b.reshape();
        b.setShowSurface(true);
    }

    public void prepareEnergies(){
        ExternalEnergy gravity = new ExternalEnergy(){

            @Override
            public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                for(int i = 0; i<fz.length; i++){
                    fz[i] += gravityMagnitude;
                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return pos[2];
            }
        };

        ExternalEnergy hardSurface = new ExternalEnergy(){
            double a = 2;
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
        };

        a.addExternalEnergy(gravity);
        a.addExternalEnergy(hardSurface);

        a.addExternalEnergy(new VolumeConservation(a, volumeConservation));
        a.addExternalEnergy(new StericMesh(b, steric));

        b.addExternalEnergy(gravity);
        b.addExternalEnergy(hardSurface);
        b.addExternalEnergy(new VolumeConservation(b, volumeConservation));
        b.addExternalEnergy(new StericMesh(a, steric));
    }


    public void createDisplay(){
        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        a.create3DObject();
        b.create3DObject();
        frame.addDataObject(a.data_object);
        frame.addDataObject(b.data_object);


    }

    public void step(){
        a.update();
        b.update();
    }
    public static void main(String[] args){
        TwoDrops sim = new TwoDrops();
        sim.prepareEnergies();
        sim.createDisplay();
        while(true){
            sim.step();
        }


    }

}
