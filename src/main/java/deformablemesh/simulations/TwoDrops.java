package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.StericMesh;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.*;
import deformablemesh.meshview.MeshFrame3D;
import lightgraph.Graph;

import java.awt.Color;
import java.util.*;

/**
 * This class is to test deforming two meshes that are influenced by "gravity", each other and a surface.
 */
public class TwoDrops {
    DeformableMesh3D a;
    DeformableMesh3D b;
    double gravityMagnitude = 0.00001;
    double surfaceFactor = 0.0002;
    double volumeConservation = 0.01;
    double steric = 0.0;
    double sticky = 0.01;
    List<StickyVertex> links = new ArrayList<>();
    public TwoDrops(){
        Sphere sA = new Sphere(new double[]{-0.1, 0, 0.5}, 0.1);
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2));
        a = RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2);
        a.GAMMA = 1;
        a.ALPHA = 0.01;
        a.BETA = 0.0;
        a.reshape();
        a.setShowSurface(true);
        a.setColor(Color.RED);
        Sphere sB = new Sphere(new double[]{0.1, 0, 0.5}, 0.1);
        //b = new NewtonMesh3D(RayCastMesh.rayCastMesh(sB, sB.getCenter(), 2));
        b = RayCastMesh.rayCastMesh(sB, sB.getCenter(), 2);

        b.ALPHA = 0.01;
        b.BETA = 0.001;
        b.GAMMA = 1;
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


        if(gravityMagnitude != 0) {
            a.addExternalEnergy(gravity);
            b.addExternalEnergy(gravity);

        }

        if(surfaceFactor != 0){
            a.addExternalEnergy(hardSurface);
            b.addExternalEnergy(hardSurface);
        }

        if(volumeConservation != 0) {
            a.addExternalEnergy(new VolumeConservation(a, volumeConservation));
            b.addExternalEnergy(new VolumeConservation(b, volumeConservation));
        }

        if(steric != 0){
            a.addExternalEnergy(new StericMesh(a, b, steric));
            b.addExternalEnergy(new StericMesh(b, a, steric));
        }


    }

    Set<Integer> stuckA = new HashSet<>();
    Set<Integer> stuckB = new HashSet<>();
    double cutoff = 0.0001;
    public void stickyCell(DeformableMesh3D sticky, DeformableMesh3D neighbor){
        int nA = sticky.positions.length/3;
        int nB = neighbor.positions.length/3;



        for(int i = 0; i<nA; i++){
            if(stuckA.contains(i)) continue;
            for(int j = 0; j<nB; j++){
                if(stuckB.contains(j)) continue;

                double[] a = sticky.getCoordinates(i);
                double[] b = neighbor.getCoordinates(j);

                double dx = b[0] - a[0];
                double dy = b[1] - a[1];
                double dz = b[2] - a[2];

                if(dx*dx + dy*dy + dz*dz < cutoff){
                    stuckA.add(i);
                    stuckB.add(j);
                    sticky.addExternalEnergy(new StickyVertex(i, neighbor.nodes.get(j), this.sticky));
                    neighbor.addExternalEnergy(new StickyVertex(j, sticky.nodes.get(i), this.sticky));
                }

            }
        }


    }

    public void stickVertexes(DeformableMesh3D a, DeformableMesh3D b){

        Set<Node3D> possibleA = new HashSet<>();
        Set<Node3D> possibleB = new HashSet<>();

        for(Node3D node: b.nodes){
            double[] pt = node.getCoordinates();
            if(pt[0]<0.05){
                possibleB.add(node);
            }
        }

        for(Node3D node: a.nodes){
            double[] pt = node.getCoordinates();
            if(pt[0]>-0.05){
                possibleA.add(node);
            }
        }
        System.out.println(possibleA.size() + ", " + possibleB.size());

        List<NodePair> pairs = new ArrayList<>();
        for(Node3D node: possibleA){

            for(Node3D other: possibleB){
                pairs.add(new NodePair(node, other));
            }

        }
        Collections.sort(pairs);

        List<NodePair> stuck = new ArrayList<>();
        for(NodePair pair: pairs){
            if(possibleA.contains(pair.a) && possibleB.contains(pair.b)){
                possibleA.remove(pair.a);
                possibleB.remove(pair.b);
                stuck.add(pair);
            }

            if(possibleA.isEmpty() || possibleB.isEmpty()){
                break;
            }

        }

        System.out.println(stuck.size());
        Graph graph = new Graph();

        for(NodePair pair: stuck){

            a.addExternalEnergy(new StickyVertex(pair.a.index, pair.b, sticky));
            b.addExternalEnergy(new StickyVertex(pair.b.index, pair.a, sticky));

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

            d = dy*dy + dz*dz;

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
        if(sticky!=0){
            int before = stuckA.size();
            stickyCell(a,b);
            if(stuckA.size() > before){
                System.out.println("stuck!");
            }
        }
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
    double potential_energy = 0;
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
        potential_energy = 0.5*k*(dx*dx + dy*dy + dz*dz);

    }

    @Override
    public double getEnergy(double[] pos) {
        return potential_energy;
    }
}