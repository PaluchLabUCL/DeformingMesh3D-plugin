package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.SnakeBox;
import deformablemesh.gui.GuiTools;
import deformablemesh.gui.RingController;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.WindowManager;
import org.scijava.java3d.*;
import org.scijava.vecmath.*;
import snakeprogram3d.display3d.CanvasView;
import snakeprogram3d.display3d.DataCanvas;
import snakeprogram3d.display3d.DataObject;
import snakeprogram3d.display3d.ThreeDSurface;
import snakeprogram3d.display3d.VolumeTexture;

import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class for creating a frame to view mesh(es).
 *
 * User: msmith
 * Date: 7/2/13
 * Time: 8:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class    MeshFrame3D {
    DataCanvas canvas;

    JFrame frame;
    Axis3D axis;
    Map<Object, DataObject> observedObjects = new HashMap<>();
    List<DataObject> transientObjects = new ArrayList<>();

    public Color getBackgroundColor() {
        return canvas.getCanvasBackgroundColor();
    }

    public VolumeDataObject getVolumeDataObject() {
        return vdo;
    }

    @FunctionalInterface
    public static interface HudDisplay{
        void draw(Graphics2D g);
    }
    HudDisplay hud = g->{};
    private SegmentationController segmentationController;

    List<DeformableMesh3D> showing = new ArrayList<>();

    boolean showingVolume = false;
    VolumeDataObject vdo;

    SnakeBox snakeBox;
    RingController ringController;

    DataObject lights;
    float ambient = 0.75f;
    float directional = 0.1f;

    List<ChannelVolume> channelVolumes = new ArrayList<>();

    public MeshFrame3D(){

    }

    public void addChannelVolume(ChannelVolume cv){
        channelVolumes.add(cv);
        segmentationController.addFrameListener(cv);
        addDataObject(cv.vdo);
    }

    public void removeChannelVolume(ChannelVolume cv){
        channelVolumes.remove(cv);
        segmentationController.removeFrameListener(cv);
        removeDataObject(cv.vdo);
    }

    public List<ChannelVolume> getChannelVolumes(){
        return Collections.unmodifiableList(channelVolumes);
    }


    public void createNewChannelVolume(){
        ImagePlus plus = GuiTools.selectOpenImage(frame);
        Color c = JColorChooser.showDialog(null, "Select Color", Color.WHITE);
        if(plus != null && c != null){
            ChannelVolume cv = new ChannelVolume(new MeshImageStack(plus), c);
            addChannelVolume(cv);
        }
    }

    public void chooseToremoveChannelVolume(){
        if(channelVolumes.size() == 0 ) return;
        Object[] choices = channelVolumes.toArray();

        Object option = JOptionPane.showInputDialog(
                frame,
                "Select Channel to Remove:",
                "Choose Channel",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if(option instanceof ChannelVolume) {
            removeChannelVolume((ChannelVolume) option);
        }
    }

    public void chooseToContrastChannelVolume(){
        if(channelVolumes.size() == 0 ) return;
        Object[] choices = channelVolumes.toArray();

        Object option = JOptionPane.showInputDialog(
                frame,
                "Select Channel to adjust Contrast:",
                "Choose Channel",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if(option == null) return;

        if(option instanceof ChannelVolume) {
            ChannelVolume volume = (ChannelVolume) option;
            VolumeContrastSetter setter = new VolumeContrastSetter(volume.vdo);
            setter.setPreviewBackgroundColor(getBackgroundColor());
            setter.showDialog(getJFrame());
        }
    }

    /**
     * Tries to look along the normal value provided. As in the normal provided would be pointed towards the user
     * after this function has been called.
     *
     * The x,y,z components will be normalized before calculating.
     *
     * @param x component of normal
     * @param y
     * @param z
     */
    public void lookTowards(double x, double y, double z){
        double m = Math.sqrt(x*x + y*y + z*z);
        if(m==0) throw new RuntimeException("cannot lookTowards zero length vector");
        double[] up = canvas.getUp();
        double[] n = new double[]{x/m, y/m, z/m};
        if(Math.abs(Vector3DOps.dot(up, n))<1e-3){
            up = Vector3DOps.getPerpendicularNormalizedVector(n);
        }
        canvas.lookTowards(n , up);
    }

    /**
     * Tries to look along the normal value provided, with the up axis used for up.
     *
     * @param normal normalized vector that to be looked along.
     * @param up vector that will nearly 'up' when looking along.
     */
    public void lookTowards(double[] normal, double[] up){
        canvas.lookTowards(normal , up);
    }

    /**
     * rotates the data canvas view.
     * @see DataCanvas#rotateView(int, int)
     *
     * @param dx rotation about veritical view.
     * @param dy rotation about horizontal view.
     */
    public void rotateView(int dx, int dy){
        canvas.rotateView(dx, dy);
    }

    /**
     * All of the values necessary to have the same view.
     *
     * @return @see DataCanvas#getViewParameters
     */
    public double[] getViewParameters(){
        double[] arr = canvas.getViewParameters();
        return canvas.getViewParameters();
    }
    /**
     * Restores a previous view.
     *
     * @return @see snakeprogram3d.display3d.DataCanvas#setViewParameters
     */
    public void setViewParameters(double[] parameters){
        canvas.setViewParameters(parameters);
    }

    public void showFrame(boolean exit_on_close){
        frame = new JFrame();
        frame.setSize(800, 800);
        if(exit_on_close)
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GraphicsConfiguration gc = DataCanvas.getBestConfigurationOnSameDevice(frame);
        Color3f background = new Color3f(1.0f,0.0f,1.0f);
        canvas = new DataCanvas(gc, background){
            @Override
            public void postRender(){
                J3DGraphics2D g = getGraphics2D();
                hud.draw(g);
                g.flush(false);
            }
        };
        frame.setTitle("DM3D: 3d canvas");
        frame.setIconImage(GuiTools.getIcon());
        frame.add(canvas);
        frame.setVisible(true);
        showAxis();


    }

    /**
     * For drawing graphics on the rendered screen.
     *
     * @param hud
     */
    public void setHud(HudDisplay hud){
        this.hud = hud;
        canvas.repaint();
    }

    public void setNoHud(){
        this.hud = g->{};
        canvas.repaint();
    }

    public Component asJPanel(Window parent){
        GraphicsConfiguration gc = DataCanvas.getBestConfigurationOnSameDevice(parent);
        Color3f background = new Color3f(1.0f,0.0f,1.0f);
        canvas = new DataCanvas(gc, background){
            @Override
            public void postRender(){
                super.postRender();
                J3DGraphics2D g = getGraphics2D();
                hud.draw(g);
                g.flush(false);
            }
            @Override
            public Dimension getPreferredSize(){
                return new Dimension(480, 480);
            }

        };

        return canvas;
    }
    public void removeLights(){
        removeDataObject(lights);
    }
    public void changeAmbientBrightness(float delta){

        ambient = ambient + delta;
        if(ambient<0) ambient = 0;
        if(ambient>1) ambient = 1;
        addLights();
    }
    public void changeDirectionalBrightness(float delta){

        directional = directional + delta;
        if(directional<0) directional = 0;
        if(directional>1) directional = 1;
        addLights();
    }

    public void addLights(){
        AmbientLight amber = new AmbientLight(new Color3f(new float[]{
                ambient, ambient, ambient
        }));

        BoundingSphere bounds =	new BoundingSphere (new Point3d(0, 0.0, 0.0), 25.0);
        amber.setInfluencingBounds(bounds);
        DirectionalLight light1 = new DirectionalLight(new Color3f(directional, directional, directional), new Vector3f(1f, 0f, 1f));
        DirectionalLight light2= new DirectionalLight(new Color3f(directional, directional, directional), new Vector3f(-1f, 0f, 1f));
        DirectionalLight light3= new DirectionalLight(new Color3f(directional, directional, directional), new Vector3f(0f, 1f, 1f));

        light1.setInfluencingBounds(bounds);
        light2.setInfluencingBounds(bounds);
        light3.setInfluencingBounds(bounds);
        if(lights!=null){
            removeLights();
        }

        BranchGroup bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_DETACH);
        bg.addChild(amber);
        bg.addChild(light1);
        bg.addChild(light2);
        bg.addChild(light3);

        lights = () -> bg;

        addDataObject(lights);
    }


    public void showAxis(){
        axis = new Axis3D();
        addDataObject(axis);
    }

    public void hideAxis(){
        removeDataObject(axis);
        axis=null;
    }

    public void addDataObject(DataObject obj){
        canvas.addObject(obj);
    }
    HashMap<DataObject, DataObject> transformed = new HashMap<DataObject, DataObject>();

    public void addDataObject(DataObject object, double dx, double dy, double dz){
        final TransformGroup tg = new TransformGroup();

        Transform3D tt = new Transform3D();
        tg.getTransform(tt);

        Vector3d n = new Vector3d(dx, dy, dz);

        tt.setTranslation(n);

        tg.setTransform(tt);
        tg.addChild(object.getBranchGroup());
        final BranchGroup bg = new BranchGroup();
        bg.addChild(tg);
        bg.setCapability(BranchGroup.ALLOW_DETACH);

        DataObject obj = new DataObject() {
            @Override
            public BranchGroup getBranchGroup() {
                return bg;
            }
        };
        transformed.put(object, obj);
        canvas.addObject(obj);

    }

    public void observeObject(Object key, DataObject obj){
        if(observedObjects.containsKey(key)){
            removeDataObject(observedObjects.get(key));
            if(obj==null){
                observedObjects.remove(key);
            }
        }
        if(obj!=null){
            observedObjects.put(key, obj);
            addDataObject(obj);
        }
    }

    public void removeDataObject(DataObject mesh) {
        if(transformed.containsKey(mesh)){
            canvas.removeObject(transformed.get(mesh));
            transformed.remove(mesh);
            return;
        }
        canvas.removeObject(mesh);
    }

    public BufferedImage snapShot(){
        return canvas.snapShot();
    }

    public void addTransientObject(DataObject o){
        transientObjects.add(o);
        addDataObject(o);
    }

    public void clearTransients(){
        transientObjects.forEach(canvas::removeObject);
        transientObjects.clear();
    }

    public void addKeyListener(KeyListener kl) {
        canvas.addKeyListener(kl);
    }

    public void removeKeyListener(KeyListener kl){
        canvas.removeKeyListener(kl);
    }

    /**
     * For enabling and disabling the default controller.
     * @param v
     */
    public void setCanvasControllerEnabled(boolean v){
        canvas.setDefaultControllerEnabled(v);
    }


    public void toggleAxis() {
        if(axis==null){
            showAxis();
        } else{
            hideAxis();
        }
    }

    public void recordShot() {
        BufferedImage img = snapShot();
        try {
            ImageIO.write(img, "PNG", new File("snapshot-" + System.currentTimeMillis() + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setBackgroundColor(Color bg){

        canvas.changeBackgroundColor(bg);
    }

    public void setVisible(boolean t){
        frame.setVisible(t);
    }

    public void addPickListener(CanvasView listener) {
        canvas.addSnakeListener(listener);
    }

    public void removeTransient(DataObject obj) {
        boolean i = transientObjects.remove(obj);
        if(i){
            canvas.removeObject(obj);
        }
    }



    public void setSegmentationController(SegmentationController control){
        segmentationController = control;
        snakeBox = control.getSnakeBox();
    }

    public void syncMesh(int currentFrame){
        List<Track> tracks = segmentationController.getAllTracks();

        Set<DeformableMesh3D> current = tracks.stream().filter(t->t.containsKey(currentFrame)).map(t->t.getMesh(currentFrame)).collect(Collectors.toSet());
        List<DeformableMesh3D> toRemove = new ArrayList<>(current.size());

        for(DeformableMesh3D mesh: showing){
            if(!current.contains(mesh)){
                toRemove.add(mesh);
            }
        }
        for(DeformableMesh3D mesh: toRemove){
            removeDataObject(mesh.data_object);
            showing.remove(mesh);
        }

        DeformableMesh3D selectedMesh = segmentationController.getSelectedMesh();

        for(Track track: tracks){
            if(!track.containsKey(currentFrame)) continue;
            DeformableMesh3D mesh = track.getMesh(currentFrame);
            if(!showing.contains(mesh)){
                if(mesh.data_object==null){
                    mesh.create3DObject();
                }
                if(mesh==selectedMesh){
                    mesh.data_object.setWireColor(Color.GREEN);
                } else{
                    mesh.data_object.setWireColor(track.getColor());
                }
                addDataObject(mesh.data_object);
                showing.add(mesh);
            } else{
                if(mesh==selectedMesh){
                    mesh.data_object.setWireColor(Color.GREEN);
                } else{
                    mesh.data_object.setWireColor(track.getColor());
                }
            }

        }

    }

    /**
     * Backs the volume texture date with the supplied image stack.
     * TODO qualify whether the stack/texture data has changed.
     * @param stack
     */
    public void showVolume(MeshImageStack stack){
        if(stack.getWidthPx()==0 || stack.getHeightPx()==0 || stack.getNSlices()==0){
            //no volume data ignore request.
            return;
        }

        if(showingVolume==false){
            showingVolume=true;
            if(vdo==null){
                vdo = new VolumeDataObject(segmentationController.getVolumeColor());
            }
            vdo.setTextureData(stack);
            addDataObject(vdo);
        } else{
            vdo.setTextureData(stack);
        }


    }



    public void showEnergy(MeshImageStack stack, ExternalEnergy erg) {
        showingVolume = true;
        int d = stack.getNSlices();
        int h = stack.getHeightPx();
        int w = stack.getWidthPx();

        if(vdo==null){
            vdo = new VolumeDataObject(segmentationController.getVolumeColor());
            vdo.setTextureData(stack);
        }

        for(int i = 0; i<d; i++){
            for(int j = 0; j<h; j++){
                for(int k = 0; k<w; k++){

                    //double v = stack.data[i][j][k];
                    double v = erg.getEnergy(stack.getNormalizedCoordinate(new double[]{k,j,i}));
                    vdo.texture_data[k][h-j-1][i] = v;


                }
            }
        }

        vdo.updateVolume();

    }


    public JFrame getJFrame() {
        return frame;
    }

    public void changeVolumeClipping(int minDelta, int maxDelta) {
        if(vdo!=null){
            double[] mnMx = vdo.getMinMax();
            double min = mnMx[0] + minDelta*0.05;
            double max = mnMx[1] + maxDelta*0.05;
            vdo.setMinMaxRange(min, max);
        }
    }

    public void hideVolume() {
        if(vdo!=null){
            removeDataObject(vdo);
            vdo = null;
        }
        showingVolume=false;
    }

    List<ContractileRingDataObject> lines = new ArrayList<>();

    public void updateSnakeBox(){
        for(ContractileRingDataObject l: lines) {
            removeDataObject(l);
        }
        lines.clear();
        List<List<double[]>> curves = snakeBox.getCurves();
        for(List<double[]> pts : curves) {
            if(pts.size()==0) continue;
            ContractileRingDataObject snakeline = new ContractileRingDataObject(pts);
            addDataObject(snakeline, 0, 0, 0);
            lines.add(snakeline);
        }
    }


    public void updateRingController(){

        RingController rc = segmentationController.getRingController();
        if(rc!=ringController){
            ringController=rc;
            ringController.addFrameListener((i)->{
                updateRingController();
            });
        }

        if(ringController.getFurrow()!=null) {
            Furrow3D furrow = ringController.getFurrow();
            if(furrow.getDataObject()==null){
                furrow.create3DObject();
            }
            observeObject(ringController, ringController.getFurrow().getDataObject());

        } else{
            observeObject(ringController, null);
        }
    }

    public boolean volumeShowing() {
        return showingVolume;
    }


}


