package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.LineDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.SphereDataObject;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Point3d;
import snakeprogram3d.display3d.CanvasView;

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
 * Created by msmith on 2/17/16.
 */
public class SpherePathTesting {
    BlockingQueue<Runnable> tasks = new ArrayBlockingQueue<>(50);
    DeformableMesh3D mesh;
    MeshFrame3D frame;
    private boolean running = true;
    List<List<Node3D>> nodeMap;
    SphereDataObject target;
    Node3D start;
    LineDataObject obj;
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
        setMesh(mesh);
        new Thread(this::mainLoop).start();
        frame.addPickListener(new PointPicking());
    }

    public static void main(String[] args){
        EventQueue.invokeLater(()->{
            new SpherePathTesting().start();
        });
    }
    public void setStartPoint(Node3D start){
        this.start = start;
        frame.clearTransients();
        frame.addTransientObject(new SphereDataObject(start.getCoordinates(), 0.025));
    }
    public void findPath(Node3D a, Node3D b){
        if(target==null){
            target = new SphereDataObject(b.getCoordinates(), 0.025);
            target.setColor(0f, 0f, 1f);
            frame.addTransientObject(target);
        } else{
            double[] pt = b.getCoordinates();
            target.moveTo(pt[0], pt[1], pt[2]);
        }

        List<Node3D> path = DeformableMesh3DTools.findPath(nodeMap, a, b);
        if(obj!=null){
            frame.removeTransient(obj);
        }
        obj = new LineDataObject(path, 6);
        obj.setColor(1.0f, 0f, 0f);

        frame.addTransientObject(obj);
    }

    private void buildNodeMap(){
        nodeMap = new ArrayList<>(mesh.nodes.size());
        for(Node3D node: mesh.nodes){
            nodeMap.add(new ArrayList<>());
        }

        for(Connection3D connection: mesh.connections){
            nodeMap.get(connection.A.index).add(connection.B);
            nodeMap.get(connection.B.index).add(connection.A);
        }
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
        buildNodeMap();
        start = null;
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
        Node3D first;
        @Override
        public void updatePick(PickResult[] results, MouseEvent evt, boolean clicked) {
            if(clicked) {

                if (first == null) {
                    PickResult result = results[0];
                    PickIntersection pick = result.getIntersection(0);
                    Point3d pt = pick.getClosestVertexCoordinates();
                    result.getClosestIntersection(pt);
                    first = getClosesNode(pt.x, pt.y, pt.z);
                    final Node3D n = first;
                    post(() -> setStartPoint(n));
                } else {
                    final Node3D a = first;
                    for (PickResult result : results) {
                        PickIntersection pick = result.getIntersection(0);
                        Point3d pt = pick.getClosestVertexCoordinates();
                        result.getClosestIntersection(pt);
                        post(()->{
                            trackClosest(pt);
                        });
                    }

                    first = null;

                }
            } else if(first != null){
                for (PickResult result : results) {
                    PickIntersection pick = result.getIntersection(0);
                    Point3d pt = pick.getClosestVertexCoordinates();
                    result.getClosestIntersection(pt);
                    post(()->{
                        trackClosest(pt);
                    });
                    return;
                }
            }
        }
    }

    private void trackClosest(Point3d pt) {
        Node3D node = getClosesNode(pt.x, pt.y, pt.z);
        findPath(start, node);

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


