package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.StericMesh;
import deformablemesh.externalenergies.TriangleAreaDistributor;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.NewtonMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.MeshFrame3D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ManyDrops {

    List<DeformableMesh3D> meshes = new ArrayList<>();

    public void start(){

        for(int i = 0; i<10; i++){
            for(int j = 0; j<10; j++){
                Sphere sphere = new Sphere(new double[]{i*0.1 - 0.4, j*0.1 - 0.4, 0.2}, 0.05);
                DeformableMesh3D mesh = new NewtonMesh3D(RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 1));
                mesh.setShowSurface(true);

                mesh.GAMMA = 100;
                mesh.ALPHA = 0.1;
                mesh.BETA = 0.01;
                mesh.reshape();
                prepareEnergies(mesh);
                meshes.add(mesh);


            }
        }

    }

    public void createDisplay(){
        MeshFrame3D frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();
        for(DeformableMesh3D mesh: meshes){
            mesh.create3DObject();
            frame.addDataObject(mesh.data_object);
        }


    }
    double gravityMagnitude = 0.01;
    double surfaceFactor = 100.1;
    double volumeConservation = 1;
    public void prepareEnergies(DeformableMesh3D mesh){
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
                        fz[i] += -z*(-gravityMagnitude + surfaceFactor);
                    }
                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return 0;
            }
        };


        if(gravityMagnitude != 0) {
            mesh.addExternalEnergy(gravity);

        }

        if(surfaceFactor != 0){
            mesh.addExternalEnergy(hardSurface);
        }

        if(volumeConservation != 0) {
            mesh.addExternalEnergy(new VolumeConservation(mesh, volumeConservation));
        }


    }

    public void step(){

        for(DeformableMesh3D mesh: meshes){
            mesh.update();
        }

    }


    public void run(){
        start();
        createDisplay();
        while(true){
            step();
        }
    }

    public static void main(String[] args){

        new ManyDrops().run();


    }
}
