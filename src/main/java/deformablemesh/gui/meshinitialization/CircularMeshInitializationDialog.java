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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

    JPanel grid;
    JTabbedPane tabs;
    JPanel content;
    JPanel host;
    JComponent showing;

    public CircularMeshInitializationDialog(JFrame owner, SegmentationController model, Runnable callback){
        super(owner, false);
        this.model = model;
        this.callback = callback;
        initializer = new Initializer();
        cursor = new ThreeDCursor(model.getNormalizedImageWidth(), model.getNormalizedImageHeight(), model.getNormalizedImageDepth());
        cursor.addNotification(initializer::repaint);

    }

    public void start(){
        content = new JPanel();

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true);
        content.getInputMap().put(enter, "ADD_MESH");
        content.getActionMap().put("ADD_MESH", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createMesh();
            }
        });

        content.setLayout(new BorderLayout());
        host = new JPanel();

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

        Map<String, JPanel> views = new HashMap<>();
        views.put("xy", createHorizontalMidPlaneSelectionPanel());
        views.put("zy", createVerticalFacingSelectionPanel());
        views.put("xz", createVerticalMidPlaneSelectionPanel());

        JButton gridView = new JButton("grid");
        gridView.addActionListener(evt->{

            grid.add(views.get("xy"));
            grid.add(views.get("zy"));
            grid.add(views.get("xz"));
            setView(grid);
        });

        JButton tabbedView = new JButton("tab view");
        tabbedView.addActionListener(evt->{

            for(String key: views.keySet()){
                tabs.add(key, views.get(key));
            }
            setView(tabs);
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
        row.add(Box.createHorizontalGlue());
        row.add(gridView);
        row.add(tabbedView);
        content.add(row, BorderLayout.SOUTH);



        grid = new JPanel();
        grid.setLayout(new GridLayout(2, 2));
        grid.add(views.get("xy"));
        grid.add(views.get("zy"));
        grid.add(views.get("xz"));
        showing = grid;
        //host.add(grid);
        content.add(showing, BorderLayout.CENTER);
        tabs = new JTabbedPane();


        setContentPane(content);


        pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();


        if(size.width>=0.8*screenSize.width || size.height>=0.8*screenSize.height){
            int nw = (int)(size.width*0.8);
            int nh = (int)(size.height*0.8);

            if(nw>size.width){
                nw = size.width;
            }
            if(nh>size.height){
                nh = size.height;
            }
            setSize(nw, nh);
        }


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

        showMeshes();
        setVisible(true);

    }

    public void setView(JComponent panel){
        Dimension d = host.getSize();
        panel.setMaximumSize(d);
        panel.setMinimumSize(d);
        panel.setPreferredSize(d);
        panel.invalidate();

        content.remove(showing);
        showing = panel;
        content.add(panel);

        content.invalidate();
        validate();
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
            if(e.isConsumed()){
                return;
            }
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

