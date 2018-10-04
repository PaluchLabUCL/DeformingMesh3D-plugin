package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.StericMesh;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.NewtonMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.MeshFrame3D;

import java.awt.Color;

/**
 * This class is to test deforming two meshes that are influenced by "gravity", each other and a surface.
 */
public class TwoDrops {
    DeformableMesh3D a;
    DeformableMesh3D b;
    double gravityMagnitude = 0.01;
    double surfaceFactor = 1;
    double volumeConservation = 10.0;
    double steric = 100;
    public TwoDrops(){
        Sphere sA = new Sphere(new double[]{-0.075, 0, 0.5}, 0.1);
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 1));
        a = RayCastMesh.rayCastMesh(sA, sA.getCenter(), 1);
        a.GAMMA = 500;
        a.ALPHA = 10;
        a.BETA = 0.0;
        a.reshape();
        a.setShowSurface(true);
        a.setColor(Color.RED);
        Sphere sB = new Sphere(new double[]{0.075, 0, 0.5}, 0.1);
        //b = new NewtonMesh3D(RayCastMesh.rayCastMesh(sB, sB.getCenter(), 1));
        b = RayCastMesh.rayCastMesh(sB, sB.getCenter(), 1);

        b.ALPHA = 0.1;
        b.BETA = 0.1;
        b.GAMMA = 500;
        b.reshape();
        b.setColor(Color.BLUE);
        b.setShowSurface(true);
    }

    public void prepareEnergies(){
        ExternalEnergy gravity = new ExternalEnergy(){

            @Override
            public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                for(int i = 0; i<fz.length; i++){
                    fz[i] += -gravityMagnitude;
                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return pos[2];
            }
        };

        ExternalEnergy hardSurface = new ExternalEnergy(){

            @Override
            public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                for(int i = 0; i<fx.length; i++){
                    double z = positions[i*3 + 2];
                    if(z<0){
                        fz[i] += gravityMagnitude + surfaceFactor;
                    }
                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return 0;
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
        frame.addLights();
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
