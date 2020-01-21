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
import deformablemesh.util.Vector3DOps;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ManyDrops {

    List<DeformableMesh3D> meshes = new ArrayList<>();
    List<StericMesh> stericMeshes = new ArrayList<>();
    public void start(){

        for(int i = 0; i<1; i++){
            for(int j = 0; j<1; j++){
                Sphere sphere = new Sphere(new double[]{i*0.1 - 0.4, j*0.1 - 0.4, 0.2}, 0.05);
                DeformableMesh3D mesh = new NewtonMesh3D(RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 1));
                mesh.setShowSurface(true);

                mesh.GAMMA = 1000;
                mesh.ALPHA = 1;
                mesh.BETA = 0.5;
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
    double gravityMagnitude = 0.1;
    double surfaceFactor = 10.;
    double volumeConservation = 1;
    double steric = 0.0;
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
        HeightMapSurface hms = generateSurface();

        if(gravityMagnitude != 0) {
            mesh.addExternalEnergy(gravity);

        }

        if(surfaceFactor != 0){
            mesh.addExternalEnergy(hms);
        }

        if(volumeConservation != 0) {
            mesh.addExternalEnergy(new VolumeConservation(mesh, volumeConservation));
        }

        if(steric != 0){

            for(int i = 0; i<meshes.size(); i++){
                DeformableMesh3D a = meshes.get(i);
                for(int j = i+1; j<meshes.size(); j++){
                    DeformableMesh3D b = meshes.get(j);
                    StericMesh asm = new StericMesh(a, b, steric);
                    StericMesh bsm = new StericMesh(b, a, steric);

                    a.addExternalEnergy(asm);
                    b.addExternalEnergy(bsm);

                    stericMeshes.add(asm);
                    stericMeshes.add(bsm);

                }
            }

        }


    }

    public void step(){

        for(DeformableMesh3D mesh: meshes){
            mesh.update();
        }
        stericMeshes.forEach(StericMesh::update);

    }


    public void run(){
        start();
        createDisplay();
        while(true){
            step();
        }
    }
    HeightMapSurface generateSurface(){
        int N = 100;
        double d = 2.0/(N-1);
        double[][] pitted = new double[N][N];
        for(int i = 0; i<N; i++){
            for(int j = 0; j<N; j++){
                double x = d*i - 1;
                double y = d*j - 1;
                double s = Math.sin(x*Math.PI);
                double c = Math.cos(y*Math.PI/2);
                pitted[j][i] =  -0.1*s*s*c*c;
            }
        }

        return new HeightMapSurface(pitted, surfaceFactor);
    }


    public static void main(String[] args){

        new ManyDrops().run();


    }
}

