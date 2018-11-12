package deformablemesh;

import deformablemesh.externalenergies.ImageEnergyType;
import deformablemesh.geometry.*;
import deformablemesh.gui.FrameListener;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.PickSelector;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.Track;
import deformablemesh.util.MeshFaceObscuring;
import deformablemesh.util.actions.ActionStack;
import deformablemesh.util.actions.UndoableActions;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import snakeprogram3d.display3d.DataObject;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by msmith on 2/11/16.
 */
public class SegmentationController {
    final SegmentationModel model;
    private ActionStack actionStack = new ActionStack();
    MeshFrame3D meshFrame3D;
    boolean meshModified = false;

    ExceptionThrowingService main = new ExceptionThrowingService();

    public SegmentationController(SegmentationModel model){
        this.model = model;
    }

    public void setGamma(double gamma){
        model.setGamma(gamma);
    }

    public void setAlpha(double d) {
        model.setAlpha(d);
    }

    public void setPressure(double d) {
        model.setPressure(d);
    }

    public void setCortexThickness(double d) {
        model.setCortexThickness(d);
    }

    public void setWeight(double d) {
        model.setWeight(d);
    }

    public void setDivisions(int d) {
        model.setDivisions(d);
    }

    public void setCurveWeight(double d) {
        model.setCurveWeight(d);
    }

    public double getGamma() {
        return model.getGamma();
    }

    public double getAlpha() {
        return model.getAlpha();
    }

    public double getPressure() {

        return model.getPressure();
    }

    public void addMeshListener(FrameListener listener ){
        model.addMeshListener(listener);
    }

    public void removeMeshListener(FrameListener listener){
        model.removeMeshListener(listener);
    }



    public double getCortexThickness() {

        return model.getCortexThickness();
    }

    public double getImageWeight() {
        return model.getImageWeight();
    }

    public int getDivisions() {
        return model.getDivisions();
    }

    public double getCurveWeight() {
        return model.getCurveWeight();
    }

    public void clearSelectedMesh() {

        if(!model.hasSelectedMesh()){
            return;
        }

        actionStack.postAction(new UndoableActions(){

            final Track old = model.getSelectedTrack();
            final int f = model.getCurrentFrame();
            DeformableMesh3D mesh = old.getMesh(f);
            @Override
            public void perform() {
                submit(() -> {
                    model.removeMeshFromTrack(f, mesh, old);
                });
            }

            @Override
            public void undo() {
                submit(()->{
                    model.addMeshToTrack(f, mesh, old);
                });

            }

            @Override
            public void redo() {
                submit(() -> {
                    model.removeMeshFromTrack(f, mesh, old);
                });
            }
        });

    }

    public void undo(){
        if(canUndo()){
            actionStack.undo();
        }
    }

    public void redo(){
        if(canRedo()){
            actionStack.redo();
        }
    }

    public boolean canUndo(){
        return actionStack.hasUndo();
    }

    public boolean canRedo(){
        return actionStack.hasRedo();
    }

    /**
     * Method for submitting jobs to the models main loops. Especially for jobs that should be
     * performed after the current jobs have finished. For example when stop has been clicked
     * and deforming is indicated to stop, but it has not completed yet.
     *
     * @param runnable
     */
    public void submit(Executable runnable) {
        main.submit(runnable);
    }

    public void previousFrame() {
        submit(model::previousFrame);
    }

    public void nextFrame() {
        submit(model::nextFrame);
    }

    public void toFrame(int f){ submit(()->model.setFrame(f));}

    public void takeSnapShot() {
        submit(()-> {
                    BufferedImage img = meshFrame3D.snapShot();
                    ImagePlus plus = new ImagePlus(
                            "snapshot" + System.currentTimeMillis(),
                            new ColorProcessor(img));
                    plus.show();
                }
            );
    }

    public void recordSnapshots(int start, int end){
        submit(()->{
            ImageStack stack = null;
            for(int i = start; i<=end; i++){
                model.setFrame(i);
                BufferedImage img = meshFrame3D.snapShot();
                ImageProcessor proc = new ColorProcessor(img);
                if(stack==null){
                    stack = new ImageStack(proc.getWidth(), proc.getHeight());
                }
                stack.addSlice("snapshot " + i, proc);
            }
            if(stack!=null){
                new ImagePlus("snapshots", stack).show();
            }
        });
    }

    public void reMesh() {
        main.submit(()->{
            int f = model.getCurrentFrame();
            InterceptingMesh3D intercepts = new InterceptingMesh3D(model.getSelectedMesh(f));
            DeformableMesh3D newMesh = RayCastMesh.rayCastMesh(intercepts, intercepts.getCenter(), getDivisions());
            addMesh(f, newMesh);
        });
    }

    public void binaryScaleRemesh(){
        main.submit(()->{
            int f = model.getCurrentFrame();
            DeformableMesh3D mesh = model.getSelectedMesh(f);
            DeformableMesh3D newMesh = BinaryMeshGenerator.remesh(mesh, model.stack);
            addMesh(f, newMesh);
        });
    }
    public void restartMeshes(){
        actionStack.postAction(new UndoableActions() {
            final List<Track> old = new ArrayList<>(model.getAllTracks());
            final List<Track> newTrack = new ArrayList<>();
            @Override
            public void perform() {
                submit(()->model.setMeshes(newTrack));
            }

            @Override
            public void undo() {
                submit(()->model.setMeshes(old));
            }

            @Override
            public void redo() {
                submit(()->model.setMeshes(newTrack));
            }
        });
    }

    public File getLastSavedFile(){
        return model.getLastSavedFile();
    }
    public void addMesh(DeformableMesh3D m){
        meshModified=true;
        addMesh(model.getCurrentFrame(), m);
    }

    public void startNewMeshTrack(int frame, DeformableMesh3D mesh){
        meshModified=true;
        actionStack.postAction(new UndoableActions(){
            DeformableMesh3D m = mesh;
            int f = frame;
            Track track;
            @Override
            public void perform() {
                submit(()->{
                    track = model.startMeshTrack(f, m);
                });
            }

            @Override
            public void undo() {
                submit(()->model.removeMeshFromTrack(f, m, track));
            }

            @Override
            public void redo() {
                submit(()->model.addMeshToTrack(f, m, track));
            }
        });
    }
    public void selectNextMeshTrack(){
        submit(model::selectNextTrack);
    }

    public void selectMesh(DeformableMesh3D mesh) {
        submit(()->model.selectTrackWithMesh(mesh));
    }

    public void addMesh(int frame, DeformableMesh3D m){
        meshModified=true;
        actionStack.postAction(new UndoableActions(){

            final DeformableMesh3D old = model.getSelectedMesh(frame);
            final DeformableMesh3D newer = m;
            Track track = model.getSelectedTrack();

            final int f = frame;

            @Override
            public void perform() {
                submit(() -> {
                    if(track==null){
                        track = model.startMeshTrack(frame, newer);
                    } else {
                        model.addMeshToTrack(f, newer, track);
                    }
                });
            }

            @Override
            public void undo() {
                submit(()->{
                    if(old==null){
                        model.removeMeshFromTrack(f, m, track);
                    } else{
                        model.addMeshToTrack(f, old, track);
                    }
                });

            }

            @Override
            public void redo() {
                submit(() -> {
                    model.addMeshToTrack(f, newer, track);
                });
            }
        });
    }

    public FurrowTransformer createFurrowTransform(double[] pos, double[] normal) {
        return model.createFurrowTransform(pos, normal);
    }

    public Image createSlice(double[] pos, double[] normal) {
        return model.createSlice(pos, normal);
    }

    public Image createSlice(FurrowTransformer transformer){
        return model.createSlice(transformer);
    }

    public void addTransientObject(DataObject dataObject) {
        submit( ()->meshFrame3D.addTransientObject(dataObject) );
    }

    public void clearTransientObjects(){
        submit( ()->meshFrame3D.clearTransients());
    }

    public void measureVolume() {
        model.calculateVolume();
    }

    public void createOutput() {
        model.createOutput();
    }

    public void calculateActinIntensity() {

        model.calculateActinIntensity();

    }

    public void calculateLineScans() {
        model.calculateLineScans();
    }

    public void showStress() {
        model.showStress();
    }

    public void showCurvature() {
        model.showCurvature();
    }

    public void showVolume() {
        submit(()->{
            meshFrame3D.showVolume(model.stack);
            meshFrame3D.setVisible(true);
        });
    }

    public void showEnergy() {
        submit(()->{
            meshFrame3D.showEnergy(model.stack, model.generateImageEnergy());
        });
    }

    public void setImageEnergyType(ImageEnergyType selectedItem) {
        model.setImageEnergyType(selectedItem);
    }

    public void hideVolume() {
        submit(meshFrame3D::hideVolume);
    }

    public void changeVolumeClipping(int minDelta, int maxDelta) {
        submit(()->meshFrame3D.changeVolumeClipping(minDelta, maxDelta));
    }

    /**
     * Deforms mesh until stopped.
     *
     */
    public void deformMesh(){
        meshModified=true;
        if(model.hasSelectedMesh()) {
            deformMesh(-1);
        }
    }
    public void deformAllMeshes(){
        meshModified=true;
        final List<DeformableMesh3D> meshes = new ArrayList<>();
        List<Track> tracks = model.getAllTracks();
        Integer frame = model.getCurrentFrame();
        for(Track t: tracks){
            if(t.containsKey(frame)){
                meshes.add(t.getMesh(frame));
            }
        }
        if(meshes.size()>0){
            final List<double[]> allPositions = new ArrayList<>();
            final List<double[]> newPositions = new ArrayList<>();
            for(DeformableMesh3D mesh: meshes){
                allPositions.add(Arrays.copyOf(mesh.positions, mesh.positions.length));
            }
            actionStack.postAction(new UndoableActions(){
                @Override
                public void perform() {
                    main.submit(() -> {
                        model.deformMeshes(meshes);
                        for(DeformableMesh3D mesh: meshes){
                            newPositions.add(Arrays.copyOf(mesh.positions, mesh.positions.length));
                        }
                    });

                }

                @Override
                public void undo() {
                    main.submit(()->{
                        for(int i = 0; i<meshes.size(); i++){
                            meshes.get(i).setPositions(allPositions.get(i));
                        }
                    });
                }

                @Override
                public void redo() {

                    main.submit(()->{
                        for(int i = 0; i<meshes.size(); i++){
                            meshes.get(i).setPositions(newPositions.get(i));
                        }
                    });

                }
            });
        }
    }
    public void deformMesh(final int count){
        meshModified = true;
        actionStack.postAction(new UndoableActions(){
            final DeformableMesh3D mesh = model.getSelectedMesh(model.getCurrentFrame());
            final double[] positions = Arrays.copyOf(mesh.positions, mesh.positions.length);
            double[] newPositions;
            @Override
            public void perform() {
                main.submit(() -> {
                    model.deformMesh(count);
                    newPositions = Arrays.copyOf(mesh.positions, mesh.positions.length);
                });

            }

            @Override
            public void undo() {
                main.submit(()->mesh.setPositions(positions));
            }

            @Override
            public void redo() {
                main.submit(()->mesh.setPositions(newPositions));
            }
        });
    }

    public DeformableMesh3D copyMesh(DeformableMesh3D mesh){
        return DeformableMesh3DTools.copyOf(mesh);
    }
    public void trackMesh(){

        if(model.hasSelectedMesh() && model.hasNextFrame()){
            actionStack.postAction(new UndoableActions() {
                final int frame = model.getCurrentFrame();
                final int next = frame + 1;
                final DeformableMesh3D old = model.getSelectedMesh(next);
                final DeformableMesh3D newer = copyMesh(model.getSelectedMesh(frame));
                Track track = model.getSelectedTrack();

                @Override
                public void perform() {
                    System.out.println(frame + " to: " + next);
                    submit(() -> {
                            model.addMeshToTrack(next, newer, track);
                    });
                }

                @Override
                public void undo() {
                    submit(()->{
                        if(old==null){
                            model.removeMeshFromTrack(next, newer, track);
                        } else{
                            model.addMeshToTrack(next, old, track);
                        }
                    });

                }

                @Override
                public void redo() {
                    submit(() -> {
                        model.addMeshToTrack(next, newer, track);
                    });
                }
            });
            nextFrame();
        }

    }

    public void trackMeshBackwards(){

        if(model.hasSelectedMesh() && model.getCurrentFrame()>0){
            actionStack.postAction(new UndoableActions() {
                final int frame = model.getCurrentFrame();
                final int previous = frame - 1;
                final DeformableMesh3D old = model.getSelectedMesh(previous);
                final DeformableMesh3D newer = copyMesh(model.getSelectedMesh(frame));
                Track track = model.getSelectedTrack();

                @Override
                public void perform() {
                    submit(() -> {
                        model.addMeshToTrack(previous, newer, track);
                    });
                }

                @Override
                public void undo() {
                    submit(()->{
                        if(old==null){
                            model.removeMeshFromTrack(previous, newer, track);
                        } else{
                            model.addMeshToTrack(previous, old, track);
                        }
                    });

                }

                @Override
                public void redo() {
                    submit(() -> {
                        model.addMeshToTrack(previous, newer, track);
                    });
                }
            });
            previousFrame();
        }
    }

    public void stopRunning() {
        model.stopRunning();
    }

    public void setOriginalPlus(ImagePlus plus) {

        submit(

                ()->{
                    boolean volumeShowing = meshFrame3D.volumeShowing();

                    if(volumeShowing){
                        meshFrame3D.hideVolume();
                    }
                    model.setOriginalPlus(plus);
                    if(volumeShowing) {
                        showVolume();
                    }
                }
        );
    }
    public void isolateMesh(){
        submit( ()->{
            DeformableMesh3D mesh = model.getSelectedMesh(model.getCurrentFrame());
            if(mesh!=null){
                Box3D box = mesh.getBoundingBox();
                MeshImageStack stack = model.stack.createSubStack(box);
                MeshFrame3D viewer = new MeshFrame3D();
                viewer.showFrame(false);
                viewer.setSegmentationController(this);
                viewer.showVolume(stack);
            }
        });
    }
    public void saveMeshes(File f) {
        submit(()->{
            model.saveMeshes(f);
            meshModified = false;
        });
    }

    public void loadMeshes(File f) {
        submit(()->{
            List<Track> replacements = MeshWriter.loadMeshes(f);
            actionStack.postAction(new UndoableActions(){
                final List<Track> old = new ArrayList<>(model.getAllTracks());
                @Override
                public void perform() {
                    submit(()->{
                        model.setMeshes(replacements);
                        meshModified = false;
                    });

                }

                @Override
                public void undo() {
                    submit(()->model.setMeshes(old));
                }

                @Override
                public void redo() {
                    submit(()->model.setMeshes(replacements));
                }
            });
        });

    }

    public void importMeshes(File f){
        submit(()->{
            List<Track> imports = MeshWriter.loadMeshes(f);
            actionStack.postAction(new UndoableActions(){
                final List<Track> old = new ArrayList<>(model.getAllTracks());
                @Override
                public void perform() {
                    submit(()->{
                        imports.addAll(old);
                        model.setMeshes(imports);
                        meshModified = true;
                    });

                }

                @Override
                public void undo() {
                    submit(()->model.setMeshes(old));
                }

                @Override
                public void redo() {
                    submit(()->model.setMeshes(imports));
                }
            });
        });
    }

    public void setMeshTracks(List<Track> replacements){
        submit(()->{
            actionStack.postAction(new UndoableActions(){
                final List<Track> old = new ArrayList<>(model.getAllTracks());
                @Override
                public void perform() {
                    submit(()->{
                        model.setMeshes(replacements);
                    });

                }

                @Override
                public void undo() {
                    submit(()->model.setMeshes(old));
                }

                @Override
                public void redo() {
                    submit(()->model.setMeshes(replacements));
                }
            });
        });
    }

    public void exportAsStl(File f) {
        submit(()->model.exportAsStl(f));
    }

    public void load3DFurrows(File f) {
        submit(()->model.load3DFurrows(f));
    }

    public void saveFurrows(File f) {
        submit(()->model.saveFurrows(f));
    }

    public void saveCurvesAsSnakes(File f) {
        submit(()->model.saveCurvesAsSnakes(f));
    }

    public void loadCurvesFromSnakes(File f) {
        submit(()->{
            model.loadCurvesFromSnakes(f);
        });
    }

    public List<Exception> getExecutionErrors(){
        return main.getExceptions();
    }

    public int getCurrentFrame() {
        return model.getCurrentFrame();
    }

    public void loadImage(String file_name){
        ImagePlus plus = new ImagePlus(file_name);
        setOriginalPlus(plus);
    }

    public SnakeBox getSnakeBox() {
        return model.snakeBox;
    }

    public DeformableMesh3D getMesh() {
        return model.getSelectedMesh(model.getCurrentFrame());
    }

    public String getShortImageName() {
        return model.getShortImageName();
    }

    public void setBeta(double d) {
        model.setBeta(d);
    }

    public double getBeta(){
        return model.getBeta();
    }

    public void setNormalizerWeight(double d) {
        model.setNormalizerWeight(d);
    }

    public double getNormalizeWeight() {
        return model.getNormalizeWeight();
    }

    public void createBinaryImage() {
        submit(()->{
           model.createBinaryImage();
        });
    }

    public void initializeMesh(DeformableMesh3D mesh) {
        meshModified=true;
        int f = model.getCurrentFrame();
        if(model.getSelectedMesh(f)==null){
            addMesh(f, mesh);
        } else{
            startNewMeshTrack(f, mesh);
        }
    }

    /**
     *
     * @return an unmodifiable view of the current mesh tracks.
     */
    public List<Track> getAllTracks() {
        return model.getAllTracks();
    }

    public DeformableMesh3D getSelectedMesh() {
        return model.getSelectedMesh(model.getCurrentFrame());
    }

    public void notifyMeshListeners() {
        submit(()->{
            meshModified=true;
            model.notifyMeshListeners();
        });
    }

    public Color getVolumeColor() {
        return model.volumeColor;
    }

    public double getZToYScale() {
        return model.getZToYScale();
    }

    public int[] getOriginalStackDimensions() {
        return model.getOriginalStackDimensions();
    }

    public double[] getSurfaceOffsets() {

        return model.getSurfaceOffsets();

    }

    public RingController getRingController() {
        return model.getRingController();
    }

    public Box3D getBounds() {
        return model.getBounds();
    }

    public void createMosaicImage() {

        submit(()->{
            model.createMosaicImage();
        });

    }

    public void measureAllVolumes() {
        submit(()->{
            model.measureAllVolumes();
        });
    }

    public boolean hasOriginalPlus() {
        return model.original_plus!=null;
    }

    public int getNFrames() {
        if(model.original_plus==null){
            return -1;
        }
        return model.original_plus.getNFrames();
    }

    public void toggleSurface() {
        if(model.hasSelectedMesh()){
            Track track = model.getSelectedTrack();
            track.setShowSurface(!track.getShowSurface());
        }
    }

    public void measureSelected() {
        if(model.hasSelectedMesh()){
            submit(model::measureSelectedMesh);
        }
    }

    public void showFurrowValues() {
        model.showFurrowValues();
    }

    public void exportAsPly(File f) {
        submit(()->model.exportAsPly(f));

    }

    public String getSelectedMeshName() {
        Track t = model.getSelectedTrack();
        if(t!=null)
            return t.getName();
        else
            return "";
    }

    public void calculateObscuringMeshes(double v) {

        int frame = getCurrentFrame();
        List<Track> tracks = model.getAllTracks();
        MeshImageStack stack = model.stack;
        MeshFaceObscuring.analyzeTracks(tracks, stack, frame, v/stack.SCALE);


    }

    public boolean getMeshModified() {
        return meshModified;
    }

    public void saveParameters(File f) {
        submit(()->{
            PropertySaver.saveProperties(this, f);
        });
    }

    public void loadParameters(File f){
        submit(()->{
            PropertySaver.loadProperties(this, f);
        });
    }


    public interface Executable{
        void execute() throws Exception;
    }

    public void setFurrowForCurrentFrame(double[] center, double[] normal){

        submit(()->{
            RingController rc = model.getRingController();
            rc.setFurrow(normal, center);
        });

    }
    public void setMeshFrame3D(MeshFrame3D meshFrame3D) {

        this.meshFrame3D = meshFrame3D;
        meshFrame3D.setBackgroundColor(model.backgroundColor);
        meshFrame3D.setSegmentationController(this);
        //for mesh only updates
        model.addMeshListener(meshFrame3D::syncMesh);
        model.snakeBox.addFrameListener((i)->meshFrame3D.updateSnakeBox());


        model.addFrameListener((i)->{
            meshFrame3D.updateRingController();
            if(meshFrame3D.volumeShowing()) {
                meshFrame3D.showVolume(model.stack);
            }
            meshFrame3D.syncMesh(i);

        });

        meshFrame3D.addPickListener(new PickSelector(this));

    }

    public MeshFrame3D getMeshFrame3D(){
        return meshFrame3D;
    }


    /**
     * Normalized length of the image along the z-axis.
     *
     * @return
     */
    public double getNormalizedImageDepth(){
        return model.stack.offsets[2]*2;
    }

    /**
     * Normalized length of the image along the x-axis.
     *
     * @return
     */
    public double getNormalizedImageWidth(){
        return model.stack.offsets[0]*2;
    }

    /**
     * Normalized length of the image along the y-axis.
     *
     * @return
     */
    public double getNormalizedImageHeight(){
        return model.stack.offsets[1]*2;
    }

    public void addFrameListener(FrameListener listener){
        model.addFrameListener(listener);
    }


}

class ExceptionThrowingService{
    ExecutorService main;
    Thread main_thread;
    final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
    ExceptionThrowingService(){
        main = Executors.newSingleThreadExecutor();
        main.submit(() -> {
            main_thread = Thread.currentThread();
            main_thread.setName("My Main Thread");
        });
    }

    public void execute(SegmentationController.Executable e){
        try{
            e.execute();
        } catch (Exception exc) {
            System.err.println("Exception enqueued");
            throw new RuntimeException(exc);
        }
    }


    public void submit(final SegmentationController.Executable r){

        if(Thread.currentThread()==main_thread){
            execute(r);
            return;
        }

        final Future f = main.submit(()->execute(r));

        main.submit(() -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                synchronized (exceptions){
                    exceptions.add(e);
                }
            }
        });
    }

    public List<Exception> getExceptions(){
        synchronized(exceptions) {
            List<Exception> excs = new ArrayList<>(exceptions);
            exceptions.clear();
            return excs;
        }
    }



}
