package deformablemesh.examples;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.MeshInverter;
import deformablemesh.gui.FurrowInput;
import deformablemesh.io.MeshReader;
import deformablemesh.meshview.DeformableMeshDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.StationaryViews;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import loci.poi.ddf.EscherChildAnchorRecord;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SliceMeshes {
    List<List<Track>> loaded;
    ImageStack stack = null;
    MeshFrame3D frame;

    public SliceMeshes(MeshFrame3D frame, List<List<Track>> tracks){
        this.frame = frame;
        this.loaded = tracks;
    }

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

    public void slice(Furrow3D furrow) {
        int set = 0;
        Color[] colors = {new Color(0, 0, 0), new Color(100, 100, 100, 200)};
        frame.clearTransients();
        frame.removeDataObject(furrow.getDataObject());
        for (List<Track> tracks : loaded) {

            final int colorSet = set;
            final Integer fno = 1;

            tracks.stream().filter(t -> t.containsKey(fno)).forEach(t -> {
                DeformableMesh3D m = t.getMesh(fno);
                scale(m);
                List<DeformableMesh3D> fb = furrow.sliceMesh(m);
                DeformableMesh3D f = fb.get(1);

                if (f.triangles.size() == 0) {
                    return;
                }
                f.create3DObject();
                f.data_object.setWireColor(new Color(0, 0, 0, 0));
                Color oc = t.getColor();
                if(colorSet == 1) {
                    Color nc = new Color(255, 255, 0, 125);
                    f.setColor(nc);
                } else{
                    f.setColor(new Color(150, 150, 255));
                }
                f.setShowSurface(true);
                frame.addTransientObject(f.data_object);
                if (f.triangles.size() > 0) {
                    if (fb.get(0).triangles.size() > 0) {
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
        if (stack == null) {
            stack = new ImageStack(p.getWidth(), p.getHeight());
            stack.addSlice(p);
        } else{
            stack.addSlice(p);
            if(stack.size() == 2){
                show();
            }
        }
        frame.addDataObject(furrow.getDataObject());
    }

    public void show(){
        ImagePlus plus = new ImagePlus("stacks", stack);
        plus.setOpenAsHyperStack(true);
        plus.show();
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
        frame.getCanvas().setView(StationaryViews.XZ);
        double[] vp = frame.getViewParameters();
        vp[3] = 1.2;
        frame.setViewParameters(vp);
        SliceMeshes sm = new SliceMeshes(frame, loaded);
        Furrow3D furrow = new Furrow3D(new double[]{0,0,0}, new double[]{0,0,1});
        FurrowInput fi = new FurrowInput();
        fi.addPlaneChangeListener(new FurrowInput.PlaneChangeListener() {
            @Override
            public void setNormal(double[] n) {
                furrow.setDirection(n);
            }

            @Override
            public void updatePosition(double dx, double dy, double dz) {
                furrow.cm[0] = furrow.cm[0] + dx;
                furrow.cm[1] = furrow.cm[1] + dy;
                furrow.cm[2] = furrow.cm[2] + dz;
                furrow.updateGeometry();
            }
        });
        JFrame frame2 = new JFrame("furrow controller");
        frame2.setContentPane(fi);
        frame2.pack();
        frame2.setVisible(true);
        furrow.create3DObject();
        frame.addDataObject(furrow.getDataObject());
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar() == KeyEvent.VK_SPACE){
                    sm.slice(furrow);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });



    }
}
