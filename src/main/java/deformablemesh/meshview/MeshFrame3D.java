package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.SnakeBox;
import deformablemesh.gui.RingController;
import deformablemesh.track.Track;
import org.scijava.java3d.AmbientLight;
import org.scijava.java3d.BoundingSphere;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.PointLight;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;
import snakeprogram3d.display3d.CanvasView;
import snakeprogram3d.display3d.DataCanvas;
import snakeprogram3d.display3d.DataObject;
import snakeprogram3d.display3d.ThreeDSurface;
import snakeprogram3d.display3d.VolumeTexture;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
public class MeshFrame3D {
    DataCanvas canvas;
    JFrame frame;
    Axis3D axis;
    Map<Object, DataObject> observedObjects = new HashMap<>();
    List<DataObject> transientObjects = new ArrayList<>();

    private SegmentationController segmentationController;

    List<DeformableMesh3D> showing = new ArrayList<>();
    ThreeDSurface surface;

    double[][][] texture_data;
    boolean showingVolume = false;
    double min = 0;
    double max = 1;


    SnakeBox snakeBox;
    RingController ringController;


    public MeshFrame3D(){

    }

    public void showFrame(boolean exit_on_close){
        frame = new JFrame();
        frame.setSize(800, 800);
        if(exit_on_close)
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GraphicsConfiguration gc = DataCanvas.getBestConfigurationOnSameDevice(frame);
        Color3f background = new Color3f(1.0f,0.0f,1.0f);
        canvas = new DataCanvas(gc, background);
        canvas.rotateView(0, 314);
        frame.add(canvas);
        frame.setVisible(true);
        showAxis();


    }

    public void addLights(){
        BoundingSphere bounds =	new BoundingSphere (new Point3d(0, 0.0, 0.0), 5.0);

        PointLight light = new PointLight(new Color3f(1f, 1f, 1f), new Point3f(0, 0, 1), new Point3f(1.0f, 1.0f, 0f));
        light.setInfluencingBounds(bounds);

        AmbientLight amber = new AmbientLight(new Color3f(new float[]{
                0.5f, 0.5f, 0.5f
        }));
        amber.setInfluencingBounds(bounds);

        addDataObject(() -> {
            BranchGroup bg = new BranchGroup();
            bg.addChild(light);
            bg.addChild(amber);
            return bg;
        });
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

        addKeyListener(new KeyListener(){

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                switch(c){
                    case ' ':
                        toggleAxis();
                        break;
                    case 's':
                        snapShot();
                        break;
                    case 'n':
                        segmentationController.selectNextMeshTrack();
                        break;
                    case 'o':
                        segmentationController.toggleSurface();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
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
        showingVolume=true;
        setTextureData(stack);
        updateVolume();

    }

    public void setTextureData(MeshImageStack stack){
        int d = stack.data.length;
        int h = stack.data[0].length;
        int w = stack.data[0][0].length;

        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }
        for(int i = 0; i<d; i++){
            for(int j = 0; j<h; j++){
                for(int k = 0; k<w; k++){

                    double v = stack.data[i][j][k];
                    texture_data[k][h-j-1][i] = v;


                }
            }
        }
    }

    public void showEnergy(MeshImageStack stack, ExternalEnergy erg) {
        showingVolume = true;
        int d = stack.data.length;
        int h = stack.data[0].length;
        int w = stack.data[0][0].length;

        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }

        for(int i = 0; i<d; i++){
            for(int j = 0; j<h; j++){
                for(int k = 0; k<w; k++){

                    //double v = stack.data[i][j][k];
                    double v = erg.getEnergy(stack.getNormalizedCoordinate(new double[]{k,j,i}));
                    texture_data[k][h-j-1][i] = v;


                }
            }
        }

        updateVolume();

    }

    /**
     * Creates the 3D representation of the data in "texture_data"
     *
     */
    public void updateVolume(){
        if(!showingVolume) return;
        Color volumeColor = segmentationController.getVolumeColor();
        VolumeTexture volume = new VolumeTexture(texture_data, min, max, new Color3f(volumeColor));
        if(surface==null){
            double scale = segmentationController.getZToYScale();
            int[] sizes = segmentationController.getOriginalStackDimensions();
            surface = new ThreeDSurface(volume, sizes[0], sizes[1], sizes[2], scale);
            double[] offsets = segmentationController.getSurfaceOffsets();
            addDataObject(surface, offsets[0], offsets[1], offsets[2]);
        } else{
            surface.setTexture(volume);
        }
    }

    public JFrame getJFrame() {
        return frame;
    }

    public void changeVolumeClipping(int minDelta, int maxDelta) {
        min += minDelta*0.05;
        max += maxDelta*0.05;
        updateVolume();
    }

    public void hideVolume() {
        if(surface!=null){
            removeDataObject(surface);
            surface=null;
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


