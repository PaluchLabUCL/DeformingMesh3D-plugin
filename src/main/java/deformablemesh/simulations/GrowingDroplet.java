package deformablemesh.simulations;

import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.MeshFrame3D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A simulation where the volume increase, when the surface area to volume reaches a certain cutoff, the droplet will
 * divide by using a projecting remesh +
 */
public class GrowingDroplet {
    static final double volumeConservation = 1.0;
    static final double surfaceFactor = 1.0;
    MeshFrame3D frame;
    List<MeshWorkspace> meshes = new ArrayList<>();


    static class MeshWorkspace{
        DeformableMesh3D mesh;
        VolumeConservation vc;
        double start;
        double bonus = 0;
        public MeshWorkspace(DeformableMesh3D mesh){
            vc = new VolumeConservation(mesh, volumeConservation);
            start = vc.getVolume();
        }
        public void prepare(){
            bonus += 0.001;
            if(bonus <= 1)
            vc.setVolume(start*(1 + bonus));
        }
        public void step(){
            mesh.update();
        }
        public boolean finished(){
            return false;
        }
    }
    public void initialize(){
        Sphere sphere = new Sphere(new double[]{0, 0, 0.0}, 0.05);

        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 2);
        //mesh = new NewtonMesh3D(mesh);
        mesh.setShowSurface(true);
        System.out.println(mesh.nodes.size());
        mesh.GAMMA = 100;
        mesh.ALPHA = 1.0;
        mesh.BETA = 0.0;
        mesh.reshape();

        meshes.add(new MeshWorkspace(mesh));
    }

    public void step(){
        for(MeshWorkspace workspace: meshes){
            workspace.prepare();
        }
        List<MeshWorkspace> finito = new ArrayList<>();
        for(MeshWorkspace workspace: meshes){
            workspace.step();
            if(workspace.finished()){
                finito.add(workspace);
            }
        }

        //remove workspace/mesh and steric energies.




    }

    public void createDisplay(){
        frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();
        int cdex = 0;
        for(MeshWorkspace mw: meshes){
            DeformableMesh3D mesh = mw.mesh;
            mesh.create3DObject();
            Color c = Color.WHITE;
            mesh.data_object.setWireColor(c);
            mesh.data_object.setColor(c);
            frame.addDataObject(mesh.data_object);
        }
        HeightMapSurface surface = generateSurface();
        frame.addDataObject(surface.surfaceGeometry.data_object);

    }
    public void simulate(){

    }

    public HeightMapSurface generateSurface(){
        int N = 100;
        double d = 2.0/(N-1);
        double[][] pitted = new double[N][N];
        for(int i = 0; i<N; i++){
            for(int j = 0; j<N; j++){
                double x = d*i - 1;
                double y = d*j - 1;
                double s = Math.cos(x*Math.PI*3/2);
                double c = Math.cos(y*Math.PI*3/2);
                pitted[j][i] =  -0.5*s*s*c*c;
            }
        }

        return new HeightMapSurface(pitted, surfaceFactor);
    }
    public static void main(String[] args){
        GrowingDroplet drop = new GrowingDroplet();
        drop.simulate();
    }

}
