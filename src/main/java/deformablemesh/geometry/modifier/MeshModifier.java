package deformablemesh.geometry.modifier;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.BinaryMeshGenerator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Sphere;
import deformablemesh.gui.FurrowInput;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.CanvasView;
import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.LineDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.TexturedPlaneDataObject;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import deformablemesh.util.actions.ActionStack;
import deformablemesh.util.actions.UndoableActions;
import ij.ImagePlus;
import org.scijava.java3d.*;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    LineDataObject obj;
    Furrow3D furrow;
    FurrowInput fi;
    StateManager manager;
    ActionStack stack = new ActionStack();
    double SELECTED_NODE_RADIUS = 0.005;
    Sculptor sculptor = new Sculptor(this);
    Selector selector = new Selector(this);
    ModificationState state;
    TexturedPlaneDataObject slice;
    List<Sphere> markers = new ArrayList<>();

    public List<Sphere> getMarkers() {
        return markers;
    }

    public List<Node3D> getSelected() {

        return selected;

    }

    public FurrowTransformer getFurrowTransformer() {
        return new FurrowTransformer(furrow, mis);
    }

    public void postAction(DisplaceNodesAction displaceNodesAction) {
        stack.postAction(displaceNodesAction);
    }

    public void scrollFurrow(double preciseWheelRotation) {

        fi.scrollYUnits(preciseWheelRotation);

    }

    private class StateManager implements MeshFrame3D.HudDisplay{

        StateManager() {
            state = selector;
        }



        public void registerState(ModificationState selector){
            state.deregister();
            selector.register();
            state = selector;
        }

        public void selectViewMode(){
            registerState(selector);
        }


        public void selectDragMode(){
        }

        public void setSculptMode(){
            registerState(sculptor);
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
            g.drawString(state.getName(), 20, 60);

        }




    }

    public void takeControl(ModificationState recipient ){
        frame.setCanvasControllerEnabled(false);
    }

    public void releaseControl(ModificationState owner){
        frame.setCanvasControllerEnabled(true);
    }

    DataObject getSliceDataObject(){
        return slice;
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

        new Thread(this::mainLoop).start();

        frame.addPickListener(new PointPicking());
        frame.addLights();

        furrow = new Furrow3D(new double[]{0,0,0}, new double[]{0, -1, 0});
        manager = new StateManager();
        frame.setHud(manager);
        frame.addKeyListener(getKeyListener());

        additionalControls();
    }
    public void selectNode(int index){
        Node3D node = mesh.nodes.get(index);
        if(!selected.contains(node)){
            selected.add(node);
            double[] pt = node.getCoordinates();
            Sphere s = new Sphere(pt, SELECTED_NODE_RADIUS);

            markers.add(s);
            frame.addDataObject(s.createDataObject());
        }
    }

    public void additionalControls(){

        fi = new FurrowInput();
        fi.addPlaneChangeListener(new FurrowInput.PlaneChangeListener() {
            @Override
            public void setNormal(double[] n) {
                furrow.setDirection(n);
                syncFurrowView();
            }

            @Override
            public void updatePosition(double dx, double dy, double dz) {
                furrow.move(new double[]{dx, dy, dz});
                syncFurrowView();
            }
        });
        fi.setFurrow(furrow);

        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 2;
        content.add(fi, constraints);
        JButton selectAllButton = new JButton("select all");
        selectAllButton.addActionListener(evt ->{
            for(int i = 0; i<mesh.nodes.size(); i++) {
                selectNode(i);
            }
        });
        JButton selectNone = new JButton("select none");
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        content.add(selectAllButton, constraints);
        constraints.gridx = 1;
        content.add(selectNone, constraints);



        JFrame frame = new JFrame("editor controls");

        frame.setContentPane(content);
        frame.pack();
        frame.setVisible(true);

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

    public void syncFurrowView(){
        FurrowTransformer ft = new FurrowTransformer(furrow, mis);
        int w = mis.getWidthPx();
        int h = mis.getHeightPx();
        double[] p0 = ft.getVolumeCoordinates(new double[]{0, 0});
        double[] p1 = ft.getVolumeCoordinates(new double[]{0, h});
        double[] p2 = ft.getVolumeCoordinates(new double[]{w, h});
        double[] p3 = ft.getVolumeCoordinates(new double[]{w, 0});
        double[] res = {
                p0[0], p0[1], p0[2],
                p1[0], p1[1], p1[2],
                p2[0], p2[1], p2[2],
                p3[0], p3[1], p3[2]
        };
        slice.updateGeometry(res);
    }

    public static void main(String[] args){

        EventQueue.invokeLater(()->{
            MeshModifier mod = new MeshModifier();
            mod.start();

            try {
                ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
                mod.setImage(plus);
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
        if(slice == null){
            DeformableMesh3D texturedPlaneGeometry = BinaryMeshGenerator.getQuad(
                    new double[]{0,0,0},
                    new double[]{1, 0, 0},
                    new double[]{0, 1, 0}
            );
            slice = new TexturedPlaneDataObject(texturedPlaneGeometry, mis);
            frame.addDataObject(slice);
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
        public void updatePressed(PickResult[] results, MouseEvent evt) {
            state.updatePressed(results, evt);
        }

        @Override
        public void updateReleased(PickResult[] results, MouseEvent evt) {
            state.updateReleased(results, evt);
        }

        @Override
        public void updateClicked(PickResult[] results, MouseEvent evt) {
            state.updateClicked(results, evt);
        }

        @Override
        public void updateMoved(PickResult[] results, MouseEvent evt) {
            state.updateMoved(results, evt);
        }



        @Override
        public void updateDragged(PickResult[] results, MouseEvent evt) {
            state.updateDragged(results, evt);
        }
    }

    /**
     * Looks for the position of the pick on the furrow plane. Returns null if none of
     * the intersections are on the plane.
     *
     * @param results
     * @return
     */
    double[] getPlanePosition(PickResult[] results){
        System.out.println(results.length);
        for(PickResult result: results){
            PickIntersection pick = result.getIntersection(0);
            Point3d p = pick.getPointCoordinates();

            if(result.getObject() == slice.getShape()){
                return new double[]{p.x, p.y, p.z};
            }


        }
        System.out.println("returning null");
        return null;
    }

    void toggleSelectNode(Node3D n){

        if(selected.contains(n)){
            //deselected.
            int i = selected.indexOf(n);
            deselectNode(i);
        } else{
            selected.add(n);
            double[] pt = n.getCoordinates();
            Sphere s = new Sphere(pt, SELECTED_NODE_RADIUS);

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

