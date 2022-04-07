package deformablemesh.examples;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.DeformableMeshDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.track.Track;
import deformablemesh.util.GroupDynamics;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StabilizeMeshes {
    final static double[] origin = {0, 0, 0};

    public static void main(String[] args) throws IOException {
        List<Track> tracks = MeshReader.loadMeshes(new File(args[0]));

        List<List<DeformableMesh3D>> stacked = new ArrayList<>();

        int min = tracks.stream().mapToInt(t->t.getFirstFrame()).min().getAsInt();
        int max = tracks.stream().mapToInt(t->t.getLastFrame()).max().getAsInt();

        List<double[]> rotations = new ArrayList<>();
        List<DeformableMesh3D> previous = null;
        List<double[]> cmTransform = new ArrayList<>();
        for(int i = min; i<=max; i++){
            int frame = i;
            List<DeformableMesh3D> meshes = tracks.stream().filter(
                    t->t.containsKey(frame)
            ).map(
                    t->t.getMesh(frame)
            ).collect(Collectors.toList());
            stacked.add(meshes);

            double[] cm = GroupDynamics.getCenterOfMass(meshes);
            cm[0] = -cm[0];
            cm[1] = -cm[1];
            cm[2] = -cm[2];
            meshes.forEach(m -> {
                m.translate(cm);
                for(double[] rotation: rotations){
                    m.rotate(rotation, origin, rotation[3]);
                }
            });
            cmTransform.add(cm);



            if(previous != null ) {
                List<Track> filtered = tracks.stream().filter(
                        t-> t.containsKey(frame - 1) && t.containsKey(frame)
                    ).collect(Collectors.toList());

                List<DeformableMesh3D> first = filtered.stream().map(t->t.getMesh(frame-1)).collect(Collectors.toList());
                List<DeformableMesh3D> second = filtered.stream().map(t->t.getMesh(frame)).collect(Collectors.toList());


                double[] rotation = GroupDynamics.getAxisRotation(first, second);

                double angle = Vector3DOps.normalize(rotation);
                System.out.println( angle + " along " + Arrays.toString(rotation));

                rotations.add(new double[]{rotation[0], rotation[1], rotation[2], -angle});
                meshes.forEach(m->m.rotate(rotation, origin, -angle));
            }


            previous = meshes;

        }
        String name = args[0].replace(".bmf", "-aligned.bmf");
        MeshWriter.saveMeshes(new File(name), tracks);

        //additional meshes can be transformed.
        for(int i = 1; i<args.length; i++){
            List<Track> at = MeshReader.loadMeshes(new File(args[i]));
            int dex = 0;
            for(int j = min; j<=max; j++){
                int frame = j;
                double[] cm = cmTransform.get(dex);
                for(Track t: at){
                    if(t.containsKey(j)){
                        DeformableMesh3D m = t.getMesh(j);
                        m.translate(cm);
                        for(int k = 0; k<dex; k++){
                            double[] rot = rotations.get(k);
                            m.rotate(rot, origin, rot[3]);
                        }
                    }
                }
                dex++;
            }
            MeshWriter.saveMeshes(new File(args[i].replace(".bmf", "-aux-aligned.bmf")), at );
        }


    }



    List<Track> tracks;

}
