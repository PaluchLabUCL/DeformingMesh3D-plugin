package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.LineDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.SphereDataObject;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.utils.geometry.Text2D;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import snakeprogram3d.display3d.CanvasView;
import snakeprogram3d.display3d.DataObject;
import snakeprogram3d.display3d.MoveableSphere;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by msmith on 21.09.17.
 */
public class MeshModifier {

    BlockingQueue<Runnable> tasks = new ArrayBlockingQueue<>(50);
    DeformableMesh3D mesh;
    MeshFrame3D frame;
    private boolean running = true;
    List<Node3D> selected = new ArrayList<>();
    List<MoveableSphere> markers = new ArrayList<>();
    LineDataObject obj;
    Furrow3D furrow;

    public void start(){
        frame = new MeshFrame3D();
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("file");
        bar.add(menu);
        JMenuItem open = new JMenuItem("open");
        menu.add(open);
        open.addActionListener(evt->{
            FileDialog fd = new FileDialog(frame.getJFrame());
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            String f = fd.getFile();
            if(f!=null){
                File file = new File(fd.getDirectory(), f);
                try {
                    List<Track> loaded = MeshWriter.loadMeshes(file);
                    final DeformableMesh3D replacement = loaded.get(0).getMesh(0);
                    post(()->{
                        setMesh(replacement);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });


        frame.showFrame(true);
        frame.getJFrame().setJMenuBar(bar);



        frame.setBackgroundColor(new Color(40, 40, 80));
        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(3);
        mesh.setColor(Color.YELLOW);
        mesh.setShowSurface(false);
        setMesh(mesh);
        new Thread(this::mainLoop).start();
        frame.addPickListener(new PointPicking());
        frame.addLights();

        furrow = new Furrow3D(new double[]{0,0,0}, new double[]{0, -1, 0});
        furrow.create3DObject();
        frame.addDataObject(furrow.getDataObject());
    }

    public static void main(String[] args){

        EventQueue.invokeLater(()->{
            MeshModifier mod = new MeshModifier();
            mod.start();
            List<Track> tracks = null;
            try {
                tracks = MeshWriter.loadMeshes(new File("sample.bmf"));
                DeformableMesh3D mesh = tracks.get(0).getMesh(tracks.get(0).getFirstFrame());
                mod.setMesh(mesh);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



    }



    public void setMesh(DeformableMesh3D mesh){
        if(this.mesh!=null){
            frame.removeDataObject(this.mesh.data_object);
        }
        if(mesh.data_object==null){
            mesh.create3DObject();
        }
        frame.addDataObject(mesh.data_object);
        this.mesh = mesh;
    }

    public void post(Runnable r){
        tasks.add(r);
    }

    public void mainLoop(){
        Thread.currentThread().setName("main");

        while(running){
            try {
                tasks.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
                running=false;
            }
        }

        System.exit(0);
    }
    class PointPicking implements CanvasView {
        @Override
        public void updatePick(PickResult[] results, MouseEvent evt, boolean clicked) {
            if(clicked) {
                    PickResult result = results[0];
                    PickIntersection pick = result.getIntersection(0);
                    Point3d pt = pick.getClosestVertexCoordinates();
                    result.getClosestIntersection(pt);
                    final Node3D n = getClosesNode(pt.x, pt.y, pt.z);
                    post(() -> toggleSelectNode(n));
            }
            //System.out.println(evt);
        }
    }

    void toggleSelectNode(Node3D n){

        if(selected.contains(n)){
            //deselected.
            int i = selected.indexOf(n);
            selected.remove(i);
            MoveableSphere m = markers.get(i);
            markers.remove(m);
            frame.removeDataObject(m);
        } else{
            selected.add(n);
            MoveableSphere s = new MoveableSphere(0.025);
            double[] pt = n.getCoordinates();
            s.moveTo(new Point3d(pt[0], pt[1], pt[2]));
            markers.add(s);
            frame.addDataObject(s);

        }



    }

    Node3D getClosesNode(double x, double y, double z){
        double min = Double.MAX_VALUE;
        double[] pt = {x,y,z};
        Node3D closest = mesh.nodes.get(0);
        for(Node3D node: mesh.nodes){
            double[] p = node.getCoordinates();
            double mag = Vector3DOps.square(p[0]-pt[0]) +
                    Vector3DOps.square(p[1]-pt[1]) +
                    Vector3DOps.square(p[2]-pt[2]);
            if(mag<min){
                min = mag;
                closest = node;
            }
        }

        return closest;

    }
}
