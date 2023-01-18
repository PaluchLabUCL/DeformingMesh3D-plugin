package deformablemesh.examples;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.MeshInverter;
import deformablemesh.io.MeshReader;
import deformablemesh.meshview.DeformableMeshDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.track.Track;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import loci.poi.ddf.EscherChildAnchorRecord;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SliceMeshes {
    public static void scale(DeformableMesh3D mesh){
        double[] center = {0, 0, 0};
        for(int i = 0; i<mesh.positions.length; i+=3){
            center[0] += mesh.positions[i];
            center[1] += mesh.positions[i+1];
            center[2] += mesh.positions[1+2];
        }
        int n = mesh.positions.length/3;
        center[0] = center[0]/n;
        center[1] = center[1]/n;
        center[2] = center[2]/n;

    }
    static List<Track> load(String s){
        try{
            return MeshReader.loadMeshes(new File(s));
        } catch (Exception e){
            System.out.println("failed to load: " + s + " returning empty list");
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) throws Exception{
        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.setBackgroundColor(Color.WHITE);
        frame.addLights();
        frame.hideAxis();
        frame.getJFrame().setSize(1024, 1024);
        new ImageJ();

        List<List<Track>> loaded = Arrays.stream(args).map(SliceMeshes::load).collect(Collectors.toList());
        frame.setViewParameters(new double[]{0, 0, 0, 1.2, 0, 0, 1, 0});
        ImageStack stack = null;
        for(int i = 0; i<14; i++){
            int set = 0;
            Color[] colors = {new Color(0, 0, 0), new Color(100, 100, 100)};
            Furrow3D furrow = new Furrow3D(new double[]{0, 0, -0.25 + i*0.05}, new double[]{0, 0.6, 0.8});
            for( List<Track> tracks: loaded){

                final int colorSet = set;
                final Integer fno = 1;

                tracks.stream().filter(t->t.containsKey(fno)).forEach(t->{
                    DeformableMesh3D m = t.getMesh(fno);
                    scale(m);
                    List<DeformableMesh3D> fb = furrow.sliceMesh(m);
                    System.out.println(fb.size());
                    DeformableMesh3D f = fb.get(1);

                    if(f.triangles.size() == 0){
                        return;
                    }
                    f.create3DObject();
                    f.data_object.setWireColor(new Color(0, 0, 0, 0));
                    f.setColor(t.getColor());
                    f.setShowSurface(true);
                    frame.addTransientObject(f.data_object);
                    if(f.triangles.size() > 0) {
                        if(fb.get(0).triangles.size() > 0){
                            DeformableMesh3D inside = MeshInverter.invertMesh(f);
                            inside.create3DObject();
                            inside.setColor(colors[colorSet]);
                            inside.data_object.setWireColor(new Color(0, 0, 0, 0));
                            inside.setShowSurface(true);
                            frame.addTransientObject(inside.data_object);
                        }

                    }
                });
                set++;

            }
            ImageProcessor p = new ColorProcessor(frame.snapShot());
            if(stack == null ){
                stack = new ImageStack(p.getWidth(), p.getHeight());
            }
            stack.addSlice(p);
            frame.clearTransients();
        }
        new ImagePlus("sliced", stack).show();


    }
}
