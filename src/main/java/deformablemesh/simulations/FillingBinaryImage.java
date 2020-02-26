package deformablemesh.simulations;

import deformablemesh.MeshImageStack;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.TriangleAreaDistributor;
import deformablemesh.geometry.*;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.VolumeDataObject;
import ij.ImagePlus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small class for testing energies to make a mesh swell and fill a binary boundary.
 */
public class FillingBinaryImage {


    public static DeformableMesh3D fillBinaryWithMesh(ImagePlus plus, List<int[]> points){
        MeshImageStack stack = new MeshImageStack(plus);

        double[] xyz = new double[3];
        for(int[] pt: points){
            xyz[0] += pt[0];
            xyz[1] += pt[1];
            xyz[2] += pt[2];
        }
        xyz[0] = xyz[0]/points.size();
        xyz[1] = xyz[1]/points.size();
        xyz[2] = xyz[2]/points.size();

        double[] c = stack.getNormalizedCoordinate(xyz);
        double pv = stack.pixel_dimensions[0]*stack.pixel_dimensions[1]*stack.pixel_dimensions[2];
        double r = Math.cbrt(points.size()*pv*3.0/4/Math.PI)/stack.SCALE;

        Sphere sA = new Sphere(c, r);
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2));
        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2);
        mesh.GAMMA = 1000;
        mesh.ALPHA = 1.0;
        mesh.BETA = 0.0;
        mesh.reshape();

        double pressure = 1;

        mesh.addExternalEnergy(new FillingForce(mesh, pressure, stack));

        int count = 0;

        while(count<400){
            mesh.update();
            if(count==300){
                mesh.clearEnergies();
                RayCastMesh.subDivideMesh(mesh);
                mesh.reshape();
                mesh.addExternalEnergy(new FillingForce(mesh, pressure, stack));

            }
            count++;
        }
        System.out.println("special meshed");
        return mesh;


    }

    public static void main(String[] args){
        ImagePlus binary = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());

        MeshImageStack stack = new MeshImageStack(binary);


        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.addLights();


        Sphere sA = new Sphere(new double[]{0, 0, 0}, 0.1);
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2));
        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2);
        mesh.GAMMA = 1000;
        mesh.ALPHA = 1.0;
        mesh.BETA = 0.0;
        mesh.reshape();
        mesh.setShowSurface(true);
        mesh.create3DObject();
        frame.addDataObject(mesh.data_object);

        binary.show();

        double pressure = 1;

        mesh.addExternalEnergy(new FillingForce(mesh, pressure, stack));

        int count = 0;
        while(count<400){
            mesh.update();
            if(count==300){
                frame.removeDataObject(mesh.data_object);
                mesh.clearEnergies();
                RayCastMesh.subDivideMesh(mesh);

                mesh.reshape();
                mesh.create3DObject();
                frame.addDataObject(mesh.data_object);
                mesh.addExternalEnergy(new FillingForce(mesh, pressure, stack));

            }

            try{
                ImageIO.write(frame.snapShot(), "PNG", new File(String.format("snaps/snap-%03d.png", count)));
            } catch (Exception e){

            }

            count++;
        }
        System.out.println("done");
    }

}

class FillingForce implements ExternalEnergy {
    double PRESSURE;
    Map<Integer, List<Triangle3D>> adjacencyMap = new HashMap<>();
    MeshImageStack binaryStack;
    Box3D bounds;
    public FillingForce(DeformableMesh3D mesh, double pressure, MeshImageStack stack){
        PRESSURE = pressure;

        for(int i = 0; i<mesh.nodes.size(); i++){
            adjacencyMap.put(i, new ArrayList<>());
        }

        int[] dexes = new int[3];
        for(Triangle3D triangle: mesh.triangles){
            triangle.getIndices(dexes);
            for(int i = 0; i<3; i++){
                adjacencyMap.get(dexes[i]).add(triangle);
            }
        }
        this.binaryStack = stack;
        bounds = stack.getLimits();
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {




        for(int i = 0; i<fx.length; i++){

            if(
                    bounds.contains(new double[]{positions[3*i], positions[3*i+1], positions[3*i + 2]})
                            &&  grow(positions[3*i], positions[3*i+1], positions[3*i + 2]) ){

                double factor = PRESSURE/3.0/adjacencyMap.get(i).size();

                for(Triangle3D t : adjacencyMap.get(i)){
                    t.update();

                    double f_x = t.normal[0]* factor;
                    double f_y = t.normal[1]* factor;
                    double f_z = t.normal[2]* factor;

                    fx[i] += f_x;
                    fy[i] += f_y;
                    fz[i] += f_z;
                }
            }
        }
    }

    protected boolean grow(double x, double y, double z){
        if ( binaryStack.getInterpolatedValue(x, y, z) != 0 ){
            return true;
        }
        return false;
    }
    protected double scale(double x, double y, double z){
        double v= binaryStack.getInterpolatedValue(x,y,z);
        v = v<0?0:v;
        return v>1?1:v;
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
