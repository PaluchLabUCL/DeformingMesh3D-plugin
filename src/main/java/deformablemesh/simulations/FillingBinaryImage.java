package deformablemesh.simulations;

import deformablemesh.MeshImageStack;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.TriangleAreaDistributor;
import deformablemesh.geometry.*;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.VolumeDataObject;
import deformablemesh.track.Track;
import deformablemesh.util.DistanceTransformMosaicImage;
import ij.ImageJ;
import ij.ImagePlus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A small class for testing energies to make a mesh swell and fill a binary boundary.
 *
 * - This poses a couple of questions. Should the outward growing pressure turn off when the
 * mesh exceeds the boundaries of the cavity?
 * - Should the boundary apply a force, that will balance with the outward force.
 * - Should the cavity be a distance transform. Either on the outside, or the inside or both.
 *
 */
public class FillingBinaryImage {
    int frame;
    MeshImageStack stack;
    List<DeformableMesh3D> meshes;
    public FillingBinaryImage(MeshImageStack mis, List<DeformableMesh3D> meshes){
        stack = mis;
        this.meshes = meshes;
    }
    public void showDeformation(){
        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(false);

        for(DeformableMesh3D mesh: meshes){
            mesh.create3DObject();
            frame.addDataObject(mesh.data_object);
        }



    }

    public void setFrame(int frame){
        this.frame = frame;
    }

    public static void main(String[] args) throws IOException {
        /*new ImageJ();
        ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        DistanceTransformMosaicImage dtmi = new DistanceTransformMosaicImage(plus);
        dtmi.findBlobs();

        dtmi.createCascades();
        ImagePlus p2 = dtmi.createLabeledImage();
        p2.show();*/
        ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        MeshImageStack mis = new MeshImageStack(plus);
        List<Track> tracks = MeshWriter.loadMeshes(Paths.get(args[1]).toFile());
        for(int i = 0; i<mis.getNFrames(); i++){
            final int fi = i;
            List<DeformableMesh3D> meshes = tracks.stream().filter(
                    t -> t.containsKey(fi)
            ).map(
                    t->t.getMesh(fi)
            ).collect(Collectors.toList());
            FillingBinaryImage fbi = new FillingBinaryImage(mis, meshes);
            fbi.setFrame(fi);
            fbi.showDeformation();
        }
    }

    public static DeformableMesh3D fillBinaryWithMesh(ImagePlus plus, List<int[]> points){
        System.out.println("filling binary blob");

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
        BinaryInterceptible bi = new BinaryInterceptible(points, new MeshImageStack(plus), 1);
        double[] c = stack.getNormalizedCoordinate(xyz);
        double pv = stack.pixel_dimensions[0]*stack.pixel_dimensions[1]*stack.pixel_dimensions[2];
        double r = Math.cbrt(points.size()*pv*3.0/4/Math.PI)/stack.SCALE;
        //a = new NewtonMesh3D(RayCastMesh.rayCastMesh(sA, sA.getCenter(), 2));
        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(bi, bi.getCenter(), 2);
        mesh.GAMMA = 1000;
        mesh.ALPHA = 1.0;
        mesh.BETA = 0.0;

        ConnectionRemesher remesher =  new ConnectionRemesher();
        remesher.setMinAndMaxLengths(0.01, 0.025);
        mesh = remesher.remesh(mesh);

        //mesh.reshape();

        //double pressure = 1;

        //mesh.addExternalEnergy(new FillingForce(mesh, pressure, stack));

        int count = 0;

        //while(count<400){
        //    mesh.update();
            /*if(count==300){
                mesh.clearEnergies();
                RayCastMesh.subDivideMesh(mesh);
                mesh.reshape();
                mesh.addExternalEnergy(new FillingForce(mesh, pressure, stack));
            }*/
        //    count++;
        //}
        return mesh;


    }


}


