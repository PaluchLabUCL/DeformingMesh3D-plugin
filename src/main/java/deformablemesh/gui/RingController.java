package deformablemesh.gui;

import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.gui.meshinitialization.FurrowInitializer;
import deformablemesh.io.FurrowWriter;
import deformablemesh.ringdetection.ContractileRingDetector;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import snakeprogram.Snake;
import snakeprogram.SnakeApplication;
import snakeprogram.SnakeModel;

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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    SnakeModel snakeModel2D;
    final SnakeBoxController boxController;
    JPanel contentPane;
    FurrowInput furrowInput;
    JFrame parent;
    List<FrameListener> listeners = new ArrayList<>();
    public RingController(SegmentationController model){
        this.model = model;
        detector = new ContractileRingDetector();
        boxController = new SnakeBoxController(model.getSnakeBox());
        boxController.addListDataListener(this);
    }


    public GuiTools.LocaleNumericTextField createNumericInputField(double initial){
        JTextField ret = new JTextField();
        Dimension d = new Dimension(30, 20);
        ret.setMinimumSize(d);
        ret.setMaximumSize(d);
        ret.setPreferredSize(d);
        return new GuiTools.LocaleNumericTextField(ret, initial);
    }

    public void startUI(){

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        JPanel main_box = new JPanel();
        main_box.setLayout(new BoxLayout(main_box, BoxLayout.PAGE_AXIS));

        JPanel prow = new JPanel();
        prow.setLayout(new GridLayout(1, 4));

        JLabel p = new JLabel("position: ");
        px = createNumericInputField(0);
        py = createNumericInputField(0);
        pz = createNumericInputField(0);

        addAll(prow, p, px.getTextField(), py.getTextField(), pz.getTextField());

        JPanel drow = new JPanel();
        drow.setLayout(new GridLayout(1, 4));
        JLabel d = new JLabel("direction: ");
        dx = createNumericInputField(1);
        dy = createNumericInputField(0);
        dz = createNumericInputField(0);

        addAll(drow, d, dx.getTextField(), dy.getTextField(), dz.getTextField());

        JPanel trow = new JPanel();
        trow.setLayout(new GridLayout(1, 2));
        JLabel t = new JLabel("threshold: ");
        thresh = createNumericInputField(1500);

        addAll(trow, t, thresh.getTextField());

        JPanel buttons = new JPanel();
        //buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.setLayout(new GridLayout(3,3));
        buttons.setMinimumSize(new Dimension(200, 60));
        buttons.setPreferredSize(new Dimension(200, 60));
        buttons.setMaximumSize(new Dimension(400, 60));
        JButton set = new JButton("set");
        set.addActionListener((evt) -> setFurrowValues());

        JButton initialize = new JButton("initialize...");
        initialize.addActionListener((event)->new FurrowInitializer(parent, model, ()->{}).start());
        buttons.add(initialize);

        JButton jiggle = new JButton("jiggle");
        jiggle.addActionListener((evt)->jiggleFurrow());
        buttons.add(jiggle);
        JButton rotate = new JButton("rotate");
        rotate.addActionListener((evt)->rotateFurrow());
        buttons.add(rotate);

        JButton snake = new JButton("curve editor");
        snake.addActionListener((evt)->showSnakeEditor());
        buttons.add(snake);

        JButton syncs = new JButton("add editor snakes");
        syncs.addActionListener((evt)->syncEditorSnakes());
        buttons.add(syncs);

        frame = new JLabel("1");

        addAll(buttons, set, initialize, frame);
        addAll(main_box, prow, drow, trow, buttons);
        main_box.add(boxController.getControls());

        content.add(main_box, BorderLayout.EAST);

        /*
        SwingJSTerm term = new SwingJSTerm(this);
        content.add( term.buildUI(), BorderLayout.SOUTH);
        */
         sliceView = new Slice3DView();
        content.add(new JScrollPane(sliceView.panel), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout());
        bottom.add(histControls.panel);
        bottom.add(createFurrowInput());
        content.add(bottom, BorderLayout.SOUTH);



        contentPane = content;

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

    private void jiggleFurrow() {
        if(model.getMesh()==null) return;
        submit(() ->{
            detector.jiggleFurrow(currentFrame, model.getMesh());
            frameChanged(model.getCurrentFrame());
        });
    }

    private void syncEditorSnakes() {
        if(snakeModel2D==null){
            return;
        }

        List<Snake> snakes = snakeModel2D.getSnakes();
        for(Snake s: snakes) {
            for(Integer i: s) {
                List<double[]> pts = s.getCoordinates(i);
                if(pts.size()>0) {
                    boxController.addCurve("editor",detector.get3DCoordinatesFromFurrowPlane(pts));
                }
            }
        }



    }

    void syncSliceViewBoxController(){

        sliceView.setCurves(detector.mapTo2D(boxController.getCurves(currentFrame)));
        sliceView.panel.repaint();
    }

    private void showSnakeEditor() {
        if(snakeModel2D==null){
            snakeModel2D = SnakeApplication.createSnakeModel();
            snakeModel2D.getFrame().setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }
        snakeModel2D.loadImage(new ImagePlus("mesh program", detector.getFurrowSlice()));
        snakeModel2D.getFrame().setVisible(true);
    }

    public void loadCurves(Map<Integer, List<List<double[]>>> adding){
        boxController.loadCurves(adding);
    }

    public Map<Integer, List<List<double[]>>> getCurves(){
        return boxController.getAllCurves();
    }

    void addAll(Container container, Component ... components){
        for(Component c: components){
            container.add(c);
        }
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

    public void detectCurrentFrame(){
        submit(() -> {
            refreshValues();
            List<double[]> refind = detector.detectFrame(currentFrame);
            if(refind.size()>0){
                boxController.addCurve("auto",detector.get3DCoordinatesFromFurrowPlane(refind));
                //sliceView.addCurve(refind);
                sliceView.panel.repaint();

            }
        });
    }
    public void setFrame(int frame){
        currentFrame = frame;
        detector.setFrame(frame);
        this.frame.setText("" + frame);
        boxController.setFrame(frame);
        ImageProcessor p = detector.getFurrowSlice();
        if(p!=null){
            histControls.refresh(new Histogram(p));
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

    public void scanFurrow(int frame, DeformableMesh3D mesh){
        detector.scanFurrow(frame, mesh);
    }

    public void rotateFurrow(){
        DeformableMesh3D mesh = model.getMesh();
        if(mesh==null)return;
        submit(() -> {

                detector.rotateFurrow(currentFrame, mesh);
                frameChanged(currentFrame);
        });
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
}

