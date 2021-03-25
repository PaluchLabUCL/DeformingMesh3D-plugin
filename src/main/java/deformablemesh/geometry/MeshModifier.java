package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.CanvasView;
import deformablemesh.meshview.LineDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.SphereDataObject;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import deformablemesh.util.actions.ActionStack;
import deformablemesh.util.actions.UndoableActions;
import ij.ImagePlus;
import org.scijava.java3d.*;
import org.scijava.java3d.utils.geometry.Text2D;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * Created by msmith on 21.09.17.
 */
public class MeshModifier {
    BlockingQueue<Runnable> tasks = new ArrayBlockingQueue<>(50);
    DeformableMesh3D mesh;
    MeshFrame3D frame;
    MeshImageStack mis;
    private boolean running = true;
    List<Node3D> selected = new ArrayList<>();
    List<Sphere> markers = new ArrayList<>();
    LineDataObject obj;
    Furrow3D furrow;
    StateManager manager;
    ActionStack stack = new ActionStack();

    enum InteractionMode{
        drag,
        selection,
        sculpt,
        view;


    }
    InteractionMode mode = InteractionMode.view;

    private class StateManager implements MeshFrame3D.HudDisplay{

        StateManager() {
        }

        public void selectViewMode(){
            mode = InteractionMode.view;
            frame.setCanvasControllerEnabled(true);
        }


        public void selectDragMode(){
            frame.setCanvasControllerEnabled(false);
            mode = InteractionMode.drag;
        }

        public void setSculptMode(){
            mode = InteractionMode.sculpt;
            frame.setCanvasControllerEnabled(false);
        }

        Color background = new Color(100, 100, 100, 100);
        Color foreground = Color.BLACK;

        public void draw(Graphics2D g){
            int w  = frame.getJFrame().getContentPane().getWidth();
            int h = frame.getJFrame().getContentPane().getHeight();
            g.setColor(background);
            g.fillRect(5, 5, w - 5, 100);

            g.setColor(foreground);
            g.drawString("Modifying Mesh: ", 20, 40);
            g.drawString(mode.name(), 20, 60);

        }


    }

    private void deselectNode(int index){
        selected.remove(index);
        Sphere m = markers.get(index);
        markers.remove(m);
        frame.removeDataObject(m.createDataObject());
    }

    public void selectNone(){
        List<Node3D> nodes = new ArrayList<>(selected);
        for(int i = nodes.size()-1; i>=0; i--){
            deselectNode(i);
        }
    }

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
        //DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(3);
        //mesh.setColor(Color.YELLOW);
        //mesh.setShowSurface(false);
        //setMesh(mesh);
        new Thread(this::mainLoop).start();
        frame.addPickListener(new PointPicking());
        frame.addLights();

        furrow = new Furrow3D(new double[]{0,0,0}, new double[]{0, -1, 0});
        furrow.create3DObject();
        frame.addDataObject(furrow.getDataObject());
        manager = new StateManager();
        frame.setHud(manager);
        frame.addKeyListener(getKeyListener());
    }

    private KeyListener getKeyListener(){

        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()){
                    case KeyEvent.VK_A:
                        if(e.isControlDown()){
                            selectNone();
                        }
                        break;
                    case KeyEvent.VK_S:
                        manager.setSculptMode();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        };

    }

    public static void main(String[] args){

        EventQueue.invokeLater(()->{
            MeshModifier mod = new MeshModifier();
            ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
            mod.setImage(plus);
            mod.start();

            try {

                List<Track> tracks = MeshWriter.loadMeshes(new File(args[1]));
                DeformableMesh3D mesh = tracks.get(0).getMesh(tracks.get(0).getFirstFrame());
                mod.setMesh(mesh);

            } catch (IOException e) {
                e.printStackTrace();
            }

        });



    }

    public void setImage(ImagePlus plus){
        mis = new MeshImageStack(plus);

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
        Point3d dragging, delta;

        @Override
        public void updatePressed(PickResult[] results, MouseEvent evt) {
            if(evt.isControlDown()){
                //entering drag mode.
                manager.selectDragMode();
                furrow.getDataObject().getBranchGroup().setPickable(true);
                mesh.data_object.getBranchGroup().setPickable(false);
                for(PickResult result: results) {
                    PickConeRay ray = (PickConeRay)result.getPickShape();
                    Vector3d dir = new Vector3d();
                    Point3d origin = new Point3d();
                    ray.getDirection(dir);
                    ray.getOrigin(origin);
                    furrow.setDirection(new double[]{dir.x, dir.y, dir.z});

                    return;
                }

            }
        }

        @Override
        public void updateReleased(PickResult[] results, MouseEvent evt) {
            manager.selectViewMode();
            furrow.getDataObject().getBranchGroup().setPickable(false);
            mesh.data_object.getBranchGroup().setPickable(true);
            if(dragging!=null && delta !=null){

                if(selected.size()==0){
                    System.out.println("no nodes selected");
                }

                double[] displacement = new double[3];
                delta.get(displacement);
                List<double[]> displacements = selected.stream().map(s->displacement).collect(Collectors.toList());


                stack.postAction(new DisplaceNodesAction(mesh, selected, displacements));
            }
            dragging = null;
            delta = null;
        }

        @Override
        public void updateClicked(PickResult[] results, MouseEvent evt) {
            System.out.println("clicked");
            for(PickResult result: results){
                Node node = result.getObject();
                if( mesh.data_object.getBranchGroup().indexOfChild(node) > -1 ){
                    PickIntersection pick = result.getIntersection(0);
                    Point3d pt = pick.getClosestVertexCoordinates();
                    result.getClosestIntersection(pt);
                    final Node3D n = getClosesNode(pt.x, pt.y, pt.z);
                    post(() -> toggleSelectNode(n));
                    return;
                }


            }
        }

        @Override
        public void updateMoved(PickResult[] results, MouseEvent evt) {
            switch(mode){
                case sculpt:
                    System.out.println("scultping");
                    break;
            }
        }

        @Override
        public void updateDragged(PickResult[] results, MouseEvent evt) {
            BranchGroup bg = furrow.getDataObject().getBranchGroup();
            TransformGroup tg = (TransformGroup)bg.getChild(0);
            Transform3D t = new Transform3D();
            tg.getTransform(t);
            for(PickResult result: results){
                if(tg.indexOfChild(result.getObject())>-1){
                    //drug on furrow.
                    PickIntersection pick = result.getIntersection(0);

                    Point3d pt = pick.getPointCoordinates();
                    t.transform(pt);
                    if( dragging == null ){
                        dragging = pt;
                    } else{
                        delta = new Point3d(
                                pt.x - dragging.x,
                                pt.y - dragging.y,
                                pt.z - dragging.z
                        );
                        for(int i = 0; i<selected.size(); i++){
                            double[] starting = selected.get(i).getCoordinates();
                            markers.get(i).moveTo(new double[]{
                                    starting[0] + delta.x, starting[1] + delta.y, starting[2] + delta.z
                            });
                        }

                    }
                }
            }


        }
    }



    void toggleSelectNode(Node3D n){

        if(selected.contains(n)){
            //deselected.
            int i = selected.indexOf(n);
            deselectNode(i);
        } else{
            selected.add(n);
            double[] pt = n.getCoordinates();
            Sphere s = new Sphere(pt, 0.025);

            markers.add(s);
            frame.addDataObject(s.createDataObject());
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

class DisplaceNodesAction implements UndoableActions{
    List<double[]> originals;
    List<double[]> updated;
    List<Node3D> nodes;
    DeformableMesh3D mesh;
    public DisplaceNodesAction(DeformableMesh3D mesh, List<Node3D> nodes, List<double[]> displacements){
        this.nodes = new ArrayList<>(nodes);
        originals = nodes.stream().map(Node3D::getCoordinates).collect(Collectors.toList());
        this.updated = new ArrayList<>();
        for(int i = 0; i<displacements.size(); i++){
            double[] a = originals.get(i);
            double[] r = displacements.get(i);
            updated.add(new double[]{
                    a[0] + r[0],
                    a[1] + r[1],
                    a[2] + r[2]
            });
        }
        this.mesh = mesh;
    }

    @Override
    public void perform() {
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).setPosition(updated.get(i));
        }
        mesh.resetPositions();
    }

    @Override
    public void undo() {
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).setPosition(originals.get(i));
        }
        mesh.resetPositions();
    }

    @Override
    public void redo() {
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).setPosition(updated.get(i));
        }
        mesh.resetPositions();
    }
}
