package deformablemesh.gui;

import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.Projectable;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.gui.meshinitialization.FurrowInitializer;
import deformablemesh.io.FurrowWriter;
import deformablemesh.ringdetection.ContractileRingDetector;
import deformablemesh.ringdetection.FurrowTransformer;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Links together the contractile ring detector,
 *
 * Created by msmith on 4/14/14.
 */
public class RingController implements FrameListener, ListDataListener {
    final ContractileRingDetector detector;
    public SegmentationController model;
    GuiTools.LocaleNumericTextField px, py, pz, dx, dy, dz, thresh;
    JLabel frame;
    int currentFrame;
    Slice3DView sliceView;
    HistogramInput histControls = new HistogramInput(this);
    JPanel contentPane;
    FurrowInput furrowInput;
    JFrame parent;
    List<FrameListener> listeners = new ArrayList<>();
    List<ProjectableMesh> selectableMeshes = new ArrayList<>();
    boolean furrowShowing;

    public RingController(SegmentationController model){
        this.model = model;
        detector = new ContractileRingDetector();
    }

    public void showFurrow(){
        furrowShowing = true;
        setFurrowValues();
    }

    public void hideFurrow(){
        furrowShowing = false;
        getFurrow().removeDataObject();

        frameChanged(model.getCurrentFrame());
    }

    public GuiTools.LocaleNumericTextField createNumericInputField(double initial){
        JTextField ret = new JTextField();
        Dimension d = new Dimension(30, 20);
        ret.setMinimumSize(d);
        ret.setMaximumSize(d);
        ret.setPreferredSize(d);
        return new GuiTools.LocaleNumericTextField(ret, initial);
    }

    /**
     * All of the data is tied to the display elements! This needs to be refactored out.
     *
     */
    public void deprecatedInitialization(){
        px = createNumericInputField(0.0);
        py = createNumericInputField(0.0);
        pz = createNumericInputField(0.0);
        dx = createNumericInputField(0);
        dy = createNumericInputField(1.);
        dz = createNumericInputField(0);
        thresh = createNumericInputField(100);
    }
    public void startUI(){
        deprecatedInitialization();
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        JPanel buttons = new JPanel();
        buttons.setLayout( new BoxLayout(buttons, BoxLayout.PAGE_AXIS));
        JButton showFurrowButton = new JButton("show");
        showFurrowButton.addActionListener(evt->{
            if(showFurrowButton.getText().equals("show")){
                showFurrowButton.setText("hide");
                showFurrow();
            } else{
                showFurrowButton.setText("show");
                hideFurrow();
            }
        });
        buttons.add(showFurrowButton);
        JButton initialize = new JButton("init furrow");
        initialize.addActionListener((event)->new FurrowInitializer(parent, model, ()->{}).start());
        buttons.add(initialize);

        frame = new JLabel("1");
        buttons.add(frame);

        JButton prev = new JButton("prev");
        prev.setMnemonic(KeyEvent.VK_COMMA);
        prev.addActionListener(evt->model.previousFrame());
        buttons.add(prev);

        JButton next = new JButton("next");
        next.setMnemonic(KeyEvent.VK_PERIOD);
        next.addActionListener(evt->model.nextFrame());
        buttons.add(next);

        content.add(buttons, BorderLayout.EAST);

        sliceView = new Slice3DView();
        content.add(new JScrollPane(sliceView.panel), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout());
        bottom.add(histControls.panel);
        bottom.add(createFurrowInput());
        content.add(bottom, BorderLayout.SOUTH);



        contentPane = content;
        activateSliceViewMouseListener();
    }

    public JPanel getContentPane(JFrame parent){
        this.parent = parent;
        return contentPane;
    }

    public FurrowInput createFurrowInput(){
        furrowInput = new FurrowInput();

        furrowInput.addPlaneChangeListener(new FurrowInput.PlaneChangeListener(){

            @Override
            public void setNormal(double[] n) {
                double[] pos = getInputPosition();
                setFurrow(n, pos);
            }

            @Override
            public void updatePosition(double dx, double dy, double dz) {
                double[] pos = getInputPosition();
                pos[0] += dx;
                pos[1] += dy;
                pos[2] += dz;
                setFurrow(getInputNormal(), pos);
            }
        });


        return furrowInput;
    }

    public void activateSliceViewMouseListener(){
        sliceView.addMouseListener( new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {

                Furrow3D furrow = getFurrow();

                if(furrow == null) return;

                FurrowTransformer t = new FurrowTransformer(
                        furrow, model.getMeshImageStack()
                );

                Point2D pt = sliceView.getScaledLocation(e.getPoint());

                DeformableMesh3D mesh = model.getSelectedMesh();
                for(ProjectableMesh m: selectableMeshes){

                    if(m.getMesh() == mesh){
                        continue;
                    }

                    Shape s = m.continuousPaths(t);

                    if(s.contains(pt)){
                        model.selectMesh(m.getMesh());
                        sliceView.repaint();
                        break;
                    }
                }
            }
        });
    }


    void syncSliceViewBoxController(){
        sliceView.  repaint();
    }

    public double[] getInputNormal(){
        double[] dir = new double[]{
                dx.getValue(),
                dy.getValue(),
                dz.getValue()
        };

        double d = dir[0]*dir[0] + dir[1]*dir[1] + dir[2]*dir[2];
        d = Math.sqrt(d);
        dir[0] = dir[0]/d;
        dir[1] = dir[1]/d;
        dir[2] = dir[2]/d;

        return dir;
    }

    public double[] getInputPosition(){
        double[] pos = new double[]{
                px.getValue(),
                py.getValue(),
                pz.getValue()
        };
        return pos;
    }

    public void setFurrowValues(){
        double[] dir = getInputNormal();

        dx.setValue(dir[0]);
        dy.setValue(dir[1]);
        dz.setValue(dir[2]);

        double[] pos = getInputPosition();

        setFurrow(dir, pos);

    }

    public void setFrame(int frame){
        currentFrame = frame;
        detector.setFrame(frame);
        this.frame.setText("" + (frame+1));
        refreshFurrow();
        ImageProcessor p = detector.getFurrowSlice();
        if(p!=null){
            sliceView.clear();
            Furrow3D furrow = detector.getFurrow(frame);
            if(furrow !=null ) {
                //manage mesh drawing!
                List<ProjectableMesh> meshes = model.getAllTracks().stream().filter(
                        t->t.containsKey(frame)
                ).map(
                        t -> t.getMesh(frame)
                ).map(ProjectableMesh::new).collect(Collectors.toList());
                selectableMeshes.clear();
                selectableMeshes.addAll(meshes);

                List<Drawable> projections = meshes.stream().map(pm -> {
                    return new Drawable() {
                        @Override
                        public void draw(Graphics2D g2d) {
                            FurrowTransformer ft = new FurrowTransformer(
                                    furrow, model.getMeshImageStack()
                            );
                            Shape shape = pm.getProjection( ft );

                            DeformableMesh3D selected = model.getSelectedMesh();
                            if(pm.getMesh() == selected){
                                g2d.setColor(Color.WHITE);
                                g2d.draw(shape);
                            }else{
                                g2d.setColor(pm.getColor());
                                g2d.draw(shape);
                            }
                        }
                    };
                }).collect(Collectors.toList());
                sliceView.addDrawables(projections);
            }

            histControls.refresh(p);
            sliceView.setSlice(p.getBufferedImage());
            detector.setThresh(thresh.getValue());
            ImageProcessor b = detector.createBinarySlice();
            sliceView.setBinary(b.getBufferedImage());
            refreshFurrow();
        }
    }
    public void setThreshold(double v){
        thresh.setValue(v);
        detector.setThresh(v);
        ImageProcessor b = detector.createBinarySlice();
        sliceView.setBinary(b.getBufferedImage());
    }

    public void refreshValues(){
        final double[] dir = new double[]{
                dx.getValue(),
                dy.getValue(),
                dz.getValue()
        };

        final double[] pos = new double[]{
                px.getValue(),
                py.getValue(),
                pz.getValue()
        };

        final double threshold = thresh.getValue();
        submit(() ->  detector.setThresh(threshold));
    }


    public ContractileRingDetector getDetector(){
        return detector;
    }

    public void loadImage(MeshImageStack stack){
        detector.setImageStack(stack);
    }

    public void submit(Runnable r){
        model.submit(r::run);
    }

    public void setFurrow(int frame, Furrow3D furrow) {
        double[] center = furrow.cm;
        double[] normal = furrow.normal;
        dx.setValue(normal[0]);
        dy.setValue(normal[1]);
        dz.setValue(normal[2]);

        px.setValue(center[0]);
        py.setValue(center[1]);
        pz.setValue(center[2]);

        detector.putFurrow(frame, furrow);
        frameChanged(model.getCurrentFrame());
    }

    public Furrow3D getFurrow() {
        return detector.getFurrow();
    }

    public Furrow3D getFurrow(int i){
        return detector.getFurrow(i);
    }

    public void writeFurrows(File f, MeshImageStack stack){
        FurrowWriter.writeFurrows(f, stack, detector);
    }

    public void refreshFurrow(){
        Furrow3D f = detector.getFurrow(currentFrame);
        if(f==null){
            return;
        }
        px.setValue(f.cm[0]);
        py.setValue(f.cm[1]);
        pz.setValue(f.cm[2]);
        dx.setValue(f.normal[0]);
        dy.setValue(f.normal[1]);
        dz.setValue(f.normal[2]);
        furrowInput.setFurrow(f);
    }

    public void setFurrow(double[] dir, double[] pos){
        Furrow3D furrow = detector.getFurrow();
        if(furrow!=null) {
            furrow.moveTo(pos);
            furrow.setDirection(dir);
        } else{
            furrow = new Furrow3D(pos, dir);
            detector.putFurrow(model.getCurrentFrame(), furrow);
        }
        frameChanged(model.getCurrentFrame());

    }

    @Override
    public void frameChanged(int i) {
        setFrame(i);
        for(FrameListener listener: listeners){
            listener.frameChanged(i);
        }
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        contentsChanged(e);
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        contentsChanged(e);
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        syncSliceViewBoxController();
    }

    public Map<Integer, Furrow3D> getFurrows() {
        return detector.getFurrows();
    }

    public void setStack(MeshImageStack stack) {
        detector.setImageStack(stack);
    }

    public void addFrameListener(FrameListener listener){
        listeners.add(listener);
    }

    public double getThresh() {
        return thresh.getValue();
    }

    public boolean isFurrowShowing() {
        return furrowShowing;
    }
}

