package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.StericMesh;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.*;
import deformablemesh.meshview.MeshFrame3D;

import java.awt.Color;
import java.util.*;

/**
 * This class is to test deforming two meshes that are influenced by "gravity", each other and a surface.
 */
public class TwoDrops {
    DeformableMesh3D a;
    DeformableMesh3D b;
    double gravityMagnitude = 0.001;
    double surfaceFactor = 1;
    double volumeConservation = 10.0;
    double steric = 0.1;
    double sticky = 100.0;
    public TwoDrops(){
        Sphere sA = new Sphere(new double[]{-0.075, 0, 0.5}, 0.1);
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 1));
        a = RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2);
        a.GAMMA = 500;
        a.ALPHA = 1;
        a.BETA = 1;
        a.reshape();
        a.setShowSurface(true);
        a.setColor(Color.RED);
        Sphere sB = new Sphere(new double[]{0.075, 0, 0.5}, 0.1);
        //b = new NewtonMesh3D(RayCastMesh.rayCastMesh(sB, sB.getCenter(), 1));
        b = RayCastMesh.rayCastMesh(sB, sB.getCenter(), 2);

        b.ALPHA = 1;
        b.BETA = 1;
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
        //a.addExternalEnergy(new StericMesh(b, steric));

        b.addExternalEnergy(gravity);
        b.addExternalEnergy(hardSurface);
        b.addExternalEnergy(new VolumeConservation(b, volumeConservation));
        //b.addExternalEnergy(new StericMesh(a, steric));

        stickVertexes(a,b);
    }
    public void stickVertexes(DeformableMesh3D a, DeformableMesh3D b){
        InterceptingMesh3D ia = new InterceptingMesh3D(a);
        Set<Node3D> intersecting = new HashSet<>();

        for(Node3D node: b.nodes){
            double[] pt = node.getCoordinates();
            if(ia.contains(pt)){
                intersecting.add(node);
            }
        }

        List<NodePair> pairs = new ArrayList<>();
        for(Node3D node: intersecting){

            for(Node3D other: a.nodes){
                pairs.add(new NodePair(node, other));
            }

        }
        Collections.sort(pairs);

        List<NodePair> stuck = new ArrayList<>();
        for(NodePair pair: pairs){

            if(intersecting.remove(pair.a)){
                stuck.add(pair);
                if(intersecting.size()==0){
                    break;
                }
            }

        }

        System.out.println(stuck.size());

        for(NodePair pair: stuck){

            a.addExternalEnergy(new StickyVertex(pair.b.index, pair.a, sticky));
            b.addExternalEnergy(new StickyVertex(pair.a.index, pair.b, sticky));
        }

    }
    static class NodePair implements Comparable<NodePair>{
        double d;
        Node3D a;
        Node3D b;
        NodePair(Node3D a, Node3D b){
            this.a = a;
            this.b = b;
            double[] pa = a.getCoordinates();
            double[] pb = b.getCoordinates();

            double dx = pa[0] - pb[0];
            double dy = pa[1] - pb[1];
            double dz = pa[2] - pb[2];

            d = dx*dx + dy*dy + dz*dz;

        }

        @Override
        public int compareTo(NodePair o) {
            return Double.compare(d, o.d);
        }
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

class StickyVertex implements ExternalEnergy{
    int affected;
    Node3D other;
    double k;
    public StickyVertex(int affected, Node3D other, double k){
        this.affected = affected;
        this.other = other;
        this.k = k;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double[] pt = other.getCoordinates();
        double dx = positions[3*affected] - pt[0];
        double dy = positions[3*affected + 1] - pt[1];
        double dz = positions[3*affected + 2] - pt[2];

        fx[affected] += -dx*k;
        fy[affected] += -dy*k;
        fz[affected] += -dz*k;


    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}