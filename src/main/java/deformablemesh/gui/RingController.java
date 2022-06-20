package deformablemesh.gui;

import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.geometry.modifier.MeshModifier;
import deformablemesh.io.FurrowWriter;
import deformablemesh.ringdetection.ContractileRingDetector;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.Track;
import ij.process.ImageProcessor;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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


    static class DoubleValue{
        double value;
        public DoubleValue(double v){ this.value = v;}
        public void setValue(double v){ value = v;}
        public double getValue(){ return value; };
    }
    DoubleValue px = new DoubleValue(0);
    DoubleValue py = new DoubleValue(0);
    DoubleValue pz = new DoubleValue(0);
    DoubleValue dx = new DoubleValue(0);
    DoubleValue dy = new DoubleValue(0);
    DoubleValue dz = new DoubleValue(1);

    double thresh = 128;
    JLabel frame;
    int currentFrame;
    private Slice3DView sliceView;

    HistogramInput histControls;
    JPanel contentPane;
    FurrowInput furrowInput;
    JFrame parent;
    List<FrameListener> listeners = new ArrayList<>();
    List<ProjectableMesh> selectableMeshes = new ArrayList<>();
    boolean furrowShowing;
    private boolean showTexture;

    MeshModifier modifier;

    public RingController(SegmentationController model){
        this.model = model;
        detector = new ContractileRingDetector();
        sliceView = new Slice3DView();
        activateSelectMeshMode();
    }

    public void setCursorRadius(double r) {
        if(modifier == null) return;
        modifier.setCursorRadius(r);
    }

    public void showFurrow(boolean textured){
        furrowShowing = true;
        showTexture = textured;
        setFurrowValues();
    }

    public void hideFurrow(){
        furrowShowing = false;
        Furrow3D furrow = getFurrow();
        if(furrow != null){
            furrow.removeDataObject();
            frameChanged(model.getCurrentFrame());
        }

    }

    public boolean modifyingMesh(){
        return modifier != null;
    }

    /**
     * Startes the select nodes activity.
     *
     * @param evt
     */
    public void selectNodes(ActionEvent evt){

        if(modifier == null){
            if(model.getSelectedMesh() == null){
                return;
            }
            modifier = new MeshModifier();
            modifier.setMeshFrame3D(model.getMeshFrame3D());
            modifier.setFurrow(getFurrow());

            modifier.activate3DFramePicker();

            setSliceListener(new MouseAdapter(){
                @Override
                public void mousePressed(MouseEvent evt){
                    double[] pt = getNormalizedVolumeCoordiante(evt.getPoint());
                    modifier.updatePressed(pt, evt);
                }
                @Override
                public void mouseReleased(MouseEvent evt){
                    double[] pt = getNormalizedVolumeCoordiante(evt.getPoint());
                    modifier.updateReleased(pt, evt);
                }
                @Override
                public void mouseClicked(MouseEvent evt){
                    double[] pt = getNormalizedVolumeCoordiante(evt.getPoint());
                    modifier.updateClicked(pt, evt);
                }
                @Override
                public void mouseMoved(MouseEvent evt){
                    double[] pt = getNormalizedVolumeCoordiante(evt.getPoint());
                    modifier.updateMoved(pt, evt);
                }
                @Override
                public void mouseDragged(MouseEvent evt){
                    double[] pt = getNormalizedVolumeCoordiante(evt.getPoint());
                    modifier.updateDragged(pt, evt);
                }
            });
            modifier.setMesh( model.getSelectedMesh() );
        }
        modifier.setSelectNodesMode();
    }

    public void sculptClicked(ActionEvent evt){
        if(modifier==null ) return;
        modifier.setSculptMode();
    }

    public JPanel getHistControlsPanel(){
        if(histControls == null){
            histControls = new HistogramInput(this);
        }
        return histControls.panel;
    }

    public void finishedClicked(ActionEvent evt){
        if(modifier==null) return;

        modifier.deactivate();
        DeformableMesh3D original = modifier.getOriginalMesh();
        Track host = model.getAllTracks().stream().filter(t->t.containsMesh(original)).findFirst().orElse(null);
        if(host != null){
            System.out.println("found mesh updating track!");
            int frame = host.getFrame(original);
            model.setMesh(host, frame, modifier.getMesh());
        }
        modifier = null;
        activateSelectMeshMode();
    }
    public void cancel(){
        if(modifier==null) return;

        modifier.deactivate();
        modifier = null;
        activateSelectMeshMode();
    }

    public boolean isTextureShowing(){
        return showTexture;
    }


    public Slice3DView getSliceView(){
        return sliceView;
    }

    double[] getNormalizedVolumeCoordiante(Point p){
        FurrowTransformer t = new FurrowTransformer(getFurrow(), model.getMeshImageStack());
        Point2D furrowPos = sliceView.getScaledLocation(p);
        double[] pt = t.getVolumeCoordinates(
                new double[]{furrowPos.getX(), furrowPos.getY()});
        return pt;
    }

    public JPanel getContentPane(JFrame parent){
        this.parent = parent;
        return contentPane;
    }

    /**
     * Creates a furrow input that is tied to this ring controller.
     *
     *
     * @return
     */
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

    MouseAdapter currentControls;

    public void setSliceListener( MouseAdapter adapter){

        if(adapter == currentControls){
            return;
        } else if(currentControls != null){
            sliceView.removeMouseAdapter(currentControls);
        }

        sliceView.addMouseAdapter(adapter);
        currentControls = adapter;
    }

    public void activateSelectMeshMode(){

        setSliceListener( new MouseAdapter(){
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
        sliceView.repaint();
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
            detector.setThresh(thresh);
            ImageProcessor b = detector.createBinarySlice();
            sliceView.setBinary(b.getBufferedImage());
            refreshFurrow();
        }
    }
    public void setThreshold(double v){
        thresh = v;
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

        final double threshold = thresh;
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
            furrow.showTexture(showTexture);
            furrow.setGeometry( pos, dir);
        } else{
            furrow = new Furrow3D(pos, dir);
            furrow.showTexture(showTexture);
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

        return thresh;
    }

    public boolean isFurrowShowing() {
        return furrowShowing;
    }
}