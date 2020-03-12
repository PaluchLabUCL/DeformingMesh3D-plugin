package deformablemesh.simulations;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.SofterStericMesh;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.*;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * A simulation where the volume increase, when the surface area to volume reaches a certain cutoff, the droplet will
 * divide by using a projecting remesh +
 */
public class GrowingDroplet {
    static final double volumeConservation = 1.0;
    static final double surfaceFactor = 4.0;
    final static double gravityMagnitude = 0.125;
    final static double stericWeight = 1.0;
    MeshFrame3D frame;
    List<MeshWorkspace> meshes = new ArrayList<>();
    List<MeshWorkspace> enqueued = new ArrayList<>();
    HeightMapSurface hms;
    MeshImageStack backdrop;
    ExternalEnergy gravity = new ExternalEnergy(){

        @Override
        public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
            for(int i = 0; i<fz.length; i++){
                fz[i] += -gravityMagnitude/fx.length;
            }
        }

        @Override
        public double getEnergy(double[] pos) {
            return pos[2];
        }
    };
    class MeshWorkspace{
        DeformableMesh3D mesh;
        VolumeConservation vc;
        double start;
        double bonus = 0;
        boolean finished = false;
        Map<DeformableMesh3D, SofterStericMesh> stericEnergies = new HashMap<>();
        public MeshWorkspace(DeformableMesh3D mesh){
            vc = new VolumeConservation(mesh, volumeConservation);
            start = vc.getVolume();
            this.mesh = mesh;
            mesh.addExternalEnergy(vc);
        }
        public void prepare(){

            bonus += 0.001;
            if(bonus <= 1) {
                vc.setVolume(start * (1 + bonus));
            } else{
                finished = true;
                List<DeformableMesh3D> daughters = splitMesh(mesh);
                daughters.forEach(d->addMesh(d));
            }
        }
        public void step(){
            mesh.update();
        }
        public boolean finished(){
            return finished;
        }

        public void addStericEnergy(DeformableMesh3D other) {
            SofterStericMesh sm = new SofterStericMesh(mesh, other, stericWeight);
            mesh.addExternalEnergy(sm);
            stericEnergies.put(other, sm);
        }

        public void removeStericEnergy(DeformableMesh3D other){
            SofterStericMesh m = stericEnergies.get(other);
            mesh.removeExternalEnergy(m);
        }
    }
    public void initialize(){
        hms = generateSurface();
        Sphere sphere = new Sphere(new double[]{0, 0, -0.2}, 0.05);
        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 2);
        //mesh = new NewtonMesh3D(mesh);
        mesh.setShowSurface(true);
        addMesh(mesh);

        ImageStack stack = new ImageStack(128, 128);
        for(int i = 0; i<128; i++){
            stack.addSlice(new ByteProcessor(128, 128));
        }
        ImagePlus plus = new ImagePlus("back drop", stack);
        plus.setDimensions(1, 128, 1);
        backdrop = new MeshImageStack(plus);
    }

    List<DeformableMesh3D> splitMesh(DeformableMesh3D mesh){
        List<DeformableMesh3D> daughters = new ArrayList<>();
        InterceptingMesh3D im = new InterceptingMesh3D(mesh);

        double[] c2 = new double[3];
        for(Node3D node: mesh.nodes){
            double[] npt = node.getCoordinates();
            c2[0] += npt[0];
            c2[1] += npt[1];
            c2[2] += npt[2];
        }
        c2[0] = c2[0]/mesh.nodes.size();
        c2[1] = c2[1]/mesh.nodes.size();
        c2[2] = c2[2]/mesh.nodes.size();
        double[] n = new double[]{ -1, 0, 0};
        double[] n2 = {-n[0], -n[1], -n[2]};
        System.out.println(Arrays.toString(c2) + ", " + Arrays.toString(c2));
        Furrow3D furrowA = new Furrow3D(c2, n);
        Furrow3D furrowB = new Furrow3D(c2, n2);




        List<Interceptable> first = Arrays.asList(im);
        DeformableMesh3D a = RayCastMesh.rayCastMesh(Arrays.asList(im, furrowA), Vector3DOps.add(c2, n2, 0.02), 2);
        DeformableMesh3D b = RayCastMesh.rayCastMesh(Arrays.asList(im, furrowB), Vector3DOps.add(c2, n, 0.02), 2);

        daughters.add(a);
        daughters.add(b);
        return daughters;
    }

    public void addMesh(DeformableMesh3D mesh){
        mesh.GAMMA = 100;
        mesh.ALPHA = 1.0;
        mesh.BETA = 0.0;
        mesh.reshape();

        enqueued.add(new MeshWorkspace(mesh));
        mesh.addExternalEnergy(hms);
        mesh.addExternalEnergy(gravity);
    }
    private void includeWorkspace(MeshWorkspace workspace){
        workspace.mesh.create3DObject();
        frame.addDataObject(workspace.mesh.data_object);
        if(stericWeight!=0) {
            for (MeshWorkspace existing : meshes) {
                workspace.addStericEnergy(existing.mesh);
                existing.addStericEnergy(workspace.mesh);
            }
        }
        meshes.add(workspace);

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

        for(MeshWorkspace finished: finito ){
            meshes.remove(finished);
            for(MeshWorkspace notFinished: meshes){
                notFinished.removeStericEnergy(finished.mesh);
            }
            if(frame != null && finished.mesh.data_object != null){
                frame.removeDataObject(finished.mesh.data_object);
            }
        }
        enqueued.forEach(this::includeWorkspace);
        enqueued.clear();
        //remove workspace/mesh and steric energies.




    }

    public void createDisplay(){
        frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();
        int cdex = 0;

        hms.surfaceGeometry.create3DObject();
        frame.addDataObject(hms.surfaceGeometry.data_object);
    }
    public void simulate(){
        initialize();
        createDisplay();
        int taken = 0;
        ImageStack stack = null;
        while(true){
            step();
            taken++;
            if(taken%10000 == 1){
                BufferedImage img = frame.snapShot();
                if(stack==null){
                    stack = new ImageStack(img.getWidth(), img.getHeight());
                    stack.addSlice(new ColorProcessor(img));
                } else if(stack.size()==1){
                    stack.addSlice(new ColorProcessor(img));
                    ImagePlus plus = new ImagePlus("single drop movie", stack);
                    plus.show();
                } else{
                    stack.addSlice(new ColorProcessor(img));
                    if(stack.size()==100){
                        break;
                    }
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }



    public HeightMapSurface generateSurface(){
        int N = 100;
        double d = 2.0/(N-1);
        double[][] pitted = new double[N][N];
        for(int i = 0; i<N; i++){
            for(int j = 0; j<N; j++){
                double x = d*i - 1;
                double y = d*j - 1;
                double s = Math.cos(x*Math.PI*1/2);
                double c = Math.cos(y*Math.PI*1/2);
                pitted[j][i] =  -0.25*s*s*c*c;
            }
        }
        HeightMapSurface surface = new HeightMapSurface(pitted, surfaceFactor);
        surface.createHeighMapDataObject();
        return surface;

    }
    public static void main(String[] args){
        GrowingDroplet drop = new GrowingDroplet();
        drop.simulate();
    }

}
