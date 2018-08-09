package deformablemesh.gui.meshinitialization;

import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.CompositeInterceptables;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Interceptable;
import deformablemesh.geometry.Projectable;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Add spheres to create a single shape, then find the center of mass of the spheres and create
 * a mesh.
 *
 * Created by melkor on 11/18/15.
 */
public class CircularMeshInitializationDialog extends JDialog {
    SegmentationController model;
    Initializer initializer;
    Runnable callback;
    JCheckBox showMeshes;
    ThreeDCursor cursor;
    JCheckBox showCursor;
    public CircularMeshInitializationDialog(JFrame owner, SegmentationController model, Runnable callback){
        super(owner, false);
        this.model = model;
        this.callback = callback;
        initializer = new Initializer();
        cursor = new ThreeDCursor(model.getNormalizedImageWidth(), model.getNormalizedImageHeight(), model.getNormalizedImageDepth());
        cursor.addNotification(initializer::repaint);
    }

    public void start(){
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));

        JButton add = new JButton("add mesh");
        add.addActionListener(evt->{
            createMesh();
        });

        JButton finish = new JButton("finish");
        finish.addActionListener((evt)->{
            finish();
        });

        showMeshes = new JCheckBox("show meshes");
        showMeshes.setSelected(true);
        showMeshes.addActionListener((evt)->{
            showMeshes();
        });

        showCursor = new JCheckBox("show cursor");
        showCursor.setSelected(true);
        showCursor.addActionListener(evt->{
            showCursor();
        });


        JButton cancel = new JButton("cancel");
        cancel.addActionListener((evt)->{
            setVisible(false);
            afterClosing();
        });

        row.add(showCursor);
        row.add(showMeshes);
        row.add(cancel);
        row.add(add);
        row.add(finish);
        content.add(row, BorderLayout.SOUTH);


        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));

        panel.add(createHorizontalMidPlaneSelectionPanel());
        panel.add(createVerticalFacingSelectionPanel());
        panel.add(createVerticalMidPlaneSelectionPanel());

        content.add(panel, BorderLayout.CENTER);

        setContentPane(content);


        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowListener(){

            public void windowOpened(WindowEvent e) {}
            public void windowClosing(WindowEvent e) {
                afterClosing();
            }
            public void windowClosed(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowActivated(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
        });

        sanitizeContent(content);
        attachActions(content);

        showMeshes();
        setVisible(true);


    }
    private void attachActions(JComponent jcomp){
        jcomp.getInputMap().put(KeyStroke.getKeyStroke('n'), "nexted");
        jcomp.getActionMap().put("nexted", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nPressed();
            }
        });

        jcomp.getInputMap().put(KeyStroke.getKeyStroke('d'), "clear");
        jcomp.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.clearSelectedMesh();
                initializer.clearProjectableMeshes();
                showMeshes();
            }
        });
    }
    public void sanitizeContent(Container container){

        Component[] comps = container.getComponents();
        for(Component comp: comps){
            if(comp instanceof JComponent){
                JComponent jcomp = (JComponent)comp;
                attachActions(jcomp);
            }
            if(comp instanceof Container){
                sanitizeContent((Container)comp);
            }
        }
    }

    public void nPressed(){
        model.selectNextMeshTrack();
        initializer.repaint();
    }



    public void finish(){
        createMesh();
        setVisible(false);
        afterClosing();
    }

    public static DeformableMesh3D createMeshFromSpheres(List<Sphere> spheres, int divisions){
        if(spheres.size()==0){
            return null;
        }

        CompositeInterceptables collectionOfSpheres = new CompositeInterceptables(spheres);

        double[] com = new double[3];
        double v1 = 0;
        for(Sphere s: spheres){
            double[] o = s.getCenter();
            double r = s.getRadius();
            r = r*r*r;
            com[0] += o[0]*r;
            com[1] += o[1]*r;
            com[2] += o[2]*r;
            v1 += r;
        }
        com[0] /= v1;
        com[1] /= v1;
        com[2] /= v1;



        List<Interceptable> system = new ArrayList<>(2);
        system.add(collectionOfSpheres);
        return RayCastMesh.rayCastMesh(system, com, divisions);
    }

    private void createMesh(){
        List<Sphere> spheres = initializer.getSpheres();
        if(spheres.size()==0){
            return;
        }

        CompositeInterceptables collectionOfSpheres = new CompositeInterceptables(spheres);
        Box3D bounds = model.getBounds();
        double[] com = new double[3];
        double v1 = 0;
        for(Sphere s: spheres){
            double[] o = s.getCenter();
            double r = s.getRadius();
            r = r*r*r;
            com[0] += o[0]*r;
            com[1] += o[1]*r;
            com[2] += o[2]*r;
            v1 += r;
        }
        com[0] /= v1;
        com[1] /= v1;
        com[2] /= v1;

        if(!bounds.contains(com)){
            if(com[0]<bounds.low[0]){
                com[0] = bounds.low[0]+20*Vector3DOps.TOL;
            } else if(com[0]>bounds.high[0]){
                com[0] = bounds.high[0] - 20*Vector3DOps.TOL;
            }

            if(com[1]<bounds.low[1]){
                com[1] = bounds.low[1]+20*Vector3DOps.TOL;
            } else if(com[1]>bounds.high[1]){
                com[1] = bounds.high[1] - 20*Vector3DOps.TOL;
            }

            if(com[2]<bounds.low[2]){
                com[2] = bounds.low[2]+20*Vector3DOps.TOL;
            } else if(com[2]>bounds.high[2]){
                com[2] = bounds.high[2] - 20*Vector3DOps.TOL;
            }

        }

        List<Interceptable> system = new ArrayList<>(2);
        system.add(collectionOfSpheres);
        system.add(bounds);
        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(system, com, model.getDivisions());
        mesh.create3DObject();
        model.initializeMesh(mesh);
        initializer.clear();
        showMeshes();
    }

    private JPanel createHorizontalMidPlaneSelectionPanel(){

        double[] hPos = {0,0,0};
        double[] zDir = {0,0,1};
        SlicePicker xyPicker  = new SlicePicker(model, zDir, hPos );
        JPanel horizontal = xyPicker.buildView();
        xyPicker.setLabel("pick x-y points. Slider adjusts z.");
        xyPicker.setLength(model.getNormalizedImageDepth());
        initializer.addPicker(xyPicker);

        return horizontal;
    }

    private JPanel createVerticalMidPlaneSelectionPanel(){

        double[] hPos = {0,0,0};
        double[] yDir = {0,-1,0};
        SlicePicker xzPicker  = new SlicePicker(model, yDir, hPos );
        JPanel horizontal = xzPicker.buildView();
        xzPicker.setLabel("pick x-z points. Slider adjusts y.");
        xzPicker.setLength(model.getNormalizedImageHeight());
        initializer.addPicker(xzPicker);

        return horizontal;
    }

    private JPanel createVerticalFacingSelectionPanel(){

        double[] hPos = {0,0,0};
        double[] xDir = {1,0,0};
        SlicePicker yzPicker  = new SlicePicker(model, xDir, hPos );
        yzPicker.rotateView();
        JPanel horizontal = yzPicker.buildView();
        yzPicker.setLength(model.getNormalizedImageWidth());
        yzPicker.setLabel("pick z-y points. Slider adjusts x position");
        initializer.addPicker(yzPicker);

        return horizontal;
    }

    public void afterClosing(){
        callback.run();
        model.clearTransientObjects();
        dispose();

    }

    public static void main(String[] args){
        SegmentationModel model = new SegmentationModel();
        SegmentationController controls = new SegmentationController(model);
        String o = new File(args[0]).getAbsolutePath();
        ImagePlus p = new ImagePlus(o);
        p.show();
        controls.setOriginalPlus(p);

        EventQueue.invokeLater(()->{
            MeshFrame3D mf3 = new MeshFrame3D();
            mf3.showFrame(true);
            controls.setMeshFrame3D(mf3);
            controls.showVolume();
            JFrame frame = new JFrame("test");
            JButton d = new JButton("dialog");
            d.addActionListener((evt)->new CircularMeshInitializationDialog(frame, new SegmentationController(model), ()->{}).start());
            frame.add(d);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });
    }
    class Initializer implements MouseListener, MouseMotionListener {
        SphereModifier dragging;
        Map<JPanel, SlicePicker> pickers = new HashMap<>();
        ///final SlicePicker picker;
        List<Sphere> spheres = new ArrayList<>();
        double[] origin;
        List<SphereModifier> modifiers = new ArrayList<>();
        Sphere working;
        public Initializer(){
        }

        public void addPicker(SlicePicker picker){
            pickers.put(picker.view.panel, picker);
            picker.addMouseListener(this);
            picker.addMouseMotionListener(this);
            picker.addCursor(cursor);
        }


        @Override
        public void mouseClicked(MouseEvent e) {
            SlicePicker picker = pickers.get((JPanel)e.getSource());
            double ox = e.getX();
            double oy = e.getY();
            double x = ox/picker.view.getZoom();
            double y = oy/picker.view.getZoom();

            if(picker.inBounds(x,y)){
                if(working==null){
                    double[] center = picker.getNormalizedCoordinates(x, y);
                    origin = new double[]{x, y};
                    working = new Sphere(center, 0.000001);
                    addProjectable(working, Color.RED);
                    model.addTransientObject(working.createDataObject());
                } else{
                    //finish.
                    spheres.add(working);
                    SphereModifier mod = new SphereModifier(working);
                    addProjectable(mod.stretchHandle, Color.BLUE);
                    addProjectable(mod.translateHandle, Color.YELLOW);
                    modifiers.add(mod);
                    working = null;
                }

            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            SlicePicker picker = pickers.get(e.getSource());
            double ox = e.getX();
            double oy = e.getY();
            double x = ox/picker.view.getZoom();
            double y = oy/picker.view.getZoom();

            if(working==null){
                //check to modify.
                for(SphereModifier m: modifiers){
                    if(m.contains(picker.getNormalizedCoordinates(x, y))){
                        dragging = m;
                        return;
                    }


                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = null;
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }


        @Override
        public void mouseDragged(MouseEvent e) {
            if(dragging!=null){
                SlicePicker picker = pickers.get((JPanel)e.getSource());

                double ox = e.getX();
                double oy = e.getY();
                double x = ox/picker.view.getZoom();
                double y = oy/picker.view.getZoom();
                dragging.modify(picker.getNormalizedCoordinates(x,y));
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            SlicePicker picker = pickers.get(e.getSource());
            double ox = e.getX();
            double oy = e.getY();
            double x = ox/picker.view.getZoom();
            double y = oy/picker.view.getZoom();

            if(picker.inBounds(x,y)){
                if(working!=null){
                    working.setRadius(picker.getNormalizedCoordinates(x, y));
                    repaint();
                }
            }

        }
        List<Sphere> getSpheres(){
            return spheres;
        }

        void addProjectable(Projectable project, Color c){
            for(SlicePicker p: pickers.values()){
                p.addProjectable(project,c);
            }
        }
        void repaint(){
            for(SlicePicker p: pickers.values()){
                p.view.panel.repaint();
            }
        }

        void clear(){
            model.clearTransientObjects();
            for(SlicePicker p: pickers.values()){
                p.clear();
            }
            modifiers.clear();
            spheres.clear();
        }
        Map<DeformableMesh3D, ProjectableMesh> meshMap = new HashMap<>();

        public void addProjectableMesh(DeformableMesh3D mesh) {
            ProjectableMesh pm = new ProjectableMesh(mesh);

            for(SlicePicker pick: pickers.values()){
                pick.addProjectableMesh(pm, mesh);
            }

            meshMap.put(mesh, pm);
        }

        public void clearProjectableMeshes() {
            for(DeformableMesh3D mesh: meshMap.keySet()){
                for(SlicePicker pick: pickers.values()){
                    pick.removeProjectable(meshMap.get(mesh));
                }
            }
            meshMap.clear();
        }
    }

    void showCursor(){

        cursor.setVisible(showCursor.isSelected());
        repaint();
    }


    void showMeshes(){

        if(showMeshes.isSelected()) {
            model.submit(()->{
                final List<DeformableMesh3D> meshes = model.getAllTracks().stream(

                ).filter(
                        t -> t.containsKey(model.getCurrentFrame())
                ).map(
                        t -> t.getMesh(model.getCurrentFrame())
                ).collect(Collectors.toList());

                for (final DeformableMesh3D mesh : meshes) {
                    EventQueue.invokeLater(()->{
                        initializer.addProjectableMesh(mesh);
                    });
                }

            });

        } else{
            initializer.clearProjectableMeshes();
        }
    }

}

class SphereModifier{
    Sphere sphere;
    Sphere translateHandle, stretchHandle;
    double[] last;
    interface Modifier{
        void modify(double[] m);
    }

    Modifier modifier;

    public SphereModifier(Sphere s){
        translateHandle = new Sphere(s.getCenter(), s.getRadius()*0.2);
        double[] s1 = Arrays.copyOf(s.getCenter(), 3);
        s1[0] += s.getRadius();
        stretchHandle = new Sphere(s1, s.getRadius()*0.25);
        sphere = s;
    }

    public boolean contains(double[] v){
        if(translateHandle.contains(v)){
            last = v;
            modifier = this::translate;
            return true;
        }

        if(stretchHandle.contains(v)){
            last = v;
            modifier = this::stretch;
            return true;
        }
        return false;
    }

    public void translate(double[] m){
        double[] difference = Vector3DOps.difference(m, last);
        sphere.moveBy(difference);
        last = m;
        updateStretchHandle();
    }

    public void stretch(double[] m){
        double d = Vector3DOps.distance(m, sphere.getCenter());
        sphere.setRadius(d);
        updateStretchHandle();
    }

    public void updateStretchHandle(){
        stretchHandle.getCenter()[0] = sphere.getRadius() + sphere.getCenter()[0];
        stretchHandle.getCenter()[1] = sphere.getCenter()[1];
        stretchHandle.getCenter()[2] = sphere.getCenter()[2];

    }

    public void modify(double[] m){
        modifier.modify(m);
    }
}

