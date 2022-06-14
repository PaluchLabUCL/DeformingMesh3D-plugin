package deformablemesh;

import Jama.LUDecomposition;
import Jama.Matrix;
import deformablemesh.externalenergies.ImageEnergyType;
import deformablemesh.geometry.*;
import deformablemesh.gui.FrameListener;
import deformablemesh.gui.GuiTools;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.gui.render2d.RenderFrame2D;
import deformablemesh.io.ImportType;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.io.TrackMateAdapter;
import deformablemesh.meshview.*;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.FrameToFrameDisplacement;
import deformablemesh.track.Track;
import deformablemesh.util.*;
import deformablemesh.util.actions.ActionStack;
import deformablemesh.util.actions.StateListener;
import deformablemesh.util.actions.UndoableActions;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import lightgraph.DataSet;
import lightgraph.Graph;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * This manages the SegmentationModel and provides an interface for interacting with meshes through an action stack
 * such that changes can be un-done.
 *
 */
public class SegmentationController {

    final SegmentationModel model;

    private final ActionStack actionStack = new ActionStack();

    MeshFrame3D meshFrame3D;

    AtomicLong lastSaved = new AtomicLong(-1);
    ExceptionThrowingService main = new ExceptionThrowingService();
    private double minConnectionLength = 0.005;
    private double maxConnectionLength = 0.02;

    /**
     * Creates a controller for the supplied model.
     *
     * @param model data that will be controlled.
     */
    public SegmentationController(SegmentationModel model){
        this.model = model;
        try {
            model.setRingController(new RingController(this));
        } catch(java.awt.AWTError err){
            System.out.println("error initializing awt: " + err.getMessage());
            System.out.println("This can be due to DISPLAY env being set incorrectly.");
            err.printStackTrace();
        }
    }

    /**
     * Deformation parameter, high values limit the rate of deformation.
     * @param gamma a number greater than 0.
     */
    public void setGamma(double gamma){
        model.setGamma(gamma);
    }

    /**
     * Deformation parameter, effectively the spring-like stiffness connections.
     *
     * @param d a number greater than 0
     */
    public void setAlpha(double d) {
        model.setAlpha(d);
    }

    /**
     * For shrinking or growing a mesh.
     *
     * @param d Non-zero values create an effective force that either causes the mesh to expand (positive) or
     *          shrink (negative).
     */
    public void setPressure(double d) {
        model.setPressure(d);
    }

    /**
     * Causes a 'steric' force from neighboring meshes to prevent mesh overlap.
     *
     *
     * @param d magnitude of repulsion. 0 turns off effect.
     */
    public void setStericNeighborWeight(double d){
        model.setStericNeighborWeight(d);
    }

    /**
     * Sets the magnitude of force the image causes on the mesh.
     *
     * @param d can be positive or negative.
     */
    public void setWeight(double d) {
        model.setWeight(d);
    }

    /**
     * When a new mesh is created, it is subdivided this number of times. 0 deformations corresponds to 20 triangles.
     * Each division divides the triangles into 4, so there will be 20*4**N triangles. 5 divisions is 20480 triangles,
     * which displays fine, but not practical for deforming.
     *
     * @param d number of times mesh is subdivided.
     */
    public void setDivisions(int d) {
        model.setDivisions(d);
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

    public double getMinConnectionLength(){
        return minConnectionLength;
    }

    /**
     * Gets the minimum connection length when doing a connection remesh. Connections shorter that
     * this will be replaced if possible.
     *
     * @param mcl must be greater than 0 less than max.
     */
    public void setMinConnectionLength(double mcl){
        minConnectionLength = mcl;
    }

    public double getMaxConnectionLength(){
        return maxConnectionLength;
    }

    /**
     * Set the maximum connection length that is used when doing a connection remesh. Connections
     * longer than this length will be split.
     *
     * Should probably be more than to about double the minimum length.
     *
     * @param mcl greater than zero, greater than minimum length.
     */

    public void setMaxConnectionLength(double mcl){
        maxConnectionLength = mcl;
    }

    /**
     * Adds a listener, that gets notified every time the displayed meshes should change.
     *
     * For example during track changes where a mesh is added or removed; When mesh tracks are set;
     * When the selected track is changed; When clearTransientObjects is called.
     *
     * @param listener cannot be null.
     */
    public void addMeshListener(FrameListener listener ){

        if(listener == null) throw new NullPointerException();
        model.addMeshListener(listener);
    }

    public void removeMeshListener(FrameListener listener){
        model.removeMeshListener(listener);
    }

    public double getImageWeight() {
        return model.getImageWeight();
    }

    public int getDivisions() {
        return model.getDivisions();
    }

    /**
     * Least efficient way to clear a mesh. Attempts to locate the provided mesh in within a track
     * and remove it.
     *
     * @see SegmentationController#clearMeshFromTrack(Track, int, DeformableMesh3D)
     *
     * @param mesh mesh that will be removed from a track.
     */
    public void clearMesh(DeformableMesh3D mesh){
        for(Track track: getAllTracks()){
            if(track.containsMesh(mesh)){
                int key = track.getFrame(mesh);
                clearMeshFromTrack(track, key, mesh);
                break;
            }
        }
    }

    /**
     * @see SegmentationController#clearMeshFromTrack(Track, int, DeformableMesh3D)
     *
     * @param t track that will lose a mesh
     * @param f frame the mesh will be removed from if a mesh exists.
     */
    public void clearMeshFromTrack(Track t, int f){
        if(t.containsKey(f)) {
            DeformableMesh3D mesh = t.getMesh(f);
            clearMeshFromTrack(t, f, mesh);
        }
    }

    /**
     * Clears the mesh specified mesh from the track at the specific time frame.
     *
     * @param old track that mesh will be removed from.
     * @param f time frame.
     * @param mesh mesh to be removed.
     */
    public void clearMeshFromTrack(final Track old, final int f, final DeformableMesh3D mesh){
        actionStack.postAction(new UndoableActions(){
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

            @Override
            public String getName(){
                return "clear selected mesh";
            }
        });
    }
    /**
     * Remove the currently selected mesh, from the current frame only.
     *
     */
    public void clearSelectedMesh() {

        if(!model.hasSelectedMesh()){
            return;
        }
        final Track old = model.getSelectedTrack();
        final int f = model.getCurrentFrame();
        DeformableMesh3D mesh = old.getMesh(f);

        clearMeshFromTrack(old, f, mesh);
    }

    /**
     * Gets notified when the action stack updates the value.
     *
     * @param listener
     */
    public void addUndoStateListener(StateListener listener){
        actionStack.addStateListener(listener);
    }

    public void removeUndoStateListener(StateListener listener){
        actionStack.removeStateListener(listener);
    }
    /**
     * Moves the action stack back.
     */
    public void undo(){
        if(canUndo()){
            actionStack.undo();
        }
    }

    public void removeSelectedTrack(){
        Track track = getSelectedMeshTrack();
        if(track != null){
            removeTrack(track);
        }
    }

    public void removeTrack(Track t){
        actionStack.postAction(new UndoableActions() {
            @Override
            public void perform() {
                model.removeMeshTrack(t);
            }

            @Override
            public void undo() {
                model.addMeshTrack(t);
            }

            @Override
            public void redo() {
                model.removeMeshTrack(t);
            }
            @Override
            public String getName(){
                return "remove track: " + t.getName();
            }
        });
    }

    /**
     * moves the action stack forward.
     */
    public void redo(){
        if(canRedo()){
            actionStack.redo();
        }
    }

    /**
     * Checks if there is an action to be undone. Primarily used for the menu.
     *
     * @return
     */
    public boolean canUndo(){
        return actionStack.hasUndo();
    }

    /**
     * Checks if there is an action to be undone. Primarily used for the menu.
     *
     * @return
     */
    public boolean canRedo(){
        return actionStack.hasRedo();
    }

    /**
     * Method for submitting jobs to the models main loops. Especially for jobs that should be
     * performed after the current jobs have finished. For example when stop has been clicked
     * and deforming is indicated to stop, but it has not completed yet.
     *
     * This can be used from javascript console by declaring a function.
     *   eg.
     *
     *   controller.submit(function(){ print("Made it through the queue");});
     *
     * Would not print until everything submitted before has finished.
     * @param runnable
     */
    public void submit(Executable runnable) {
        main.submit(runnable);
    }

    /**
     * Moves to the previous image frame.
     *
     */
    public void previousFrame() {
        submit(model::previousFrame);
    }

    /**
     * Changes to the next image frame.
     */
    public void nextFrame() {
        submit(model::nextFrame);
    }

    /**
     * Sets the frame.
     *
     * @param f desired frame, silently fails if out of range.
     */
    public void toFrame(int f){ submit(()->model.setFrame(f));}

    /**
     * Creates a snapshot of the current 3d view and creates an image plus window. If this does not work
     * It is possible to reset the offscreen canvas used for taking snapshots using
     *
     * This is also available by pressing 's' when the 3D canvas window is selected.
     *
     * @see MeshFrame3D
     */
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

    /**
     * Creates an ImagePlus with snapshots from each frame. This is also available as a menu item.
     *
     * @param start the first frame to be imaged, frame 1 is 0.
     * @param end last frame, inclusive.
     */
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

    public void centerSelectedMesh(){
        DeformableMesh3D mesh = getSelectedMesh();
        if(mesh != null){
            meshFrame3D.centerView( mesh.getBoundingBox().getCenter() );
        }
    }
    /**
     * Remeshes the currently selected mesh by raycasting a sphere. The number of triangles is determined by the
     * divisions parameter.
     *
     */
    public void reMesh() {
        main.submit(()->{
            int f = model.getCurrentFrame();
            InterceptingMesh3D intercepts = new InterceptingMesh3D(model.getSelectedMesh(f));
            DeformableMesh3D newMesh = RayCastMesh.rayCastMesh(intercepts, intercepts.getCenter(), getDivisions());
            addMesh(f, newMesh);
        });
    }

    /**
     * Creates a distance transform with the currently selected image. It is expected that the image
     * is a mask with, 0 or non-zero values.
     */
    public void maskToDistanceTransform(){
        MeshImageStack stack = getMeshImageStack();
        ImageStack result = new ImageStack(stack.getWidthPx(), stack.getHeightPx() );


        for(int i = 0; i<getNFrames(); i++) {
            long start = System.currentTimeMillis();
            System.out.println("starting frame: " + i);
            getMeshImageStack().setFrame(i);
            ImagePlus frame = getMeshImageStack().getCurrentFrame();
            DistanceTransformMosaicImage dtmi = new DistanceTransformMosaicImage(frame);
            dtmi.findBlobs();
            //dtmi.createGrowingCascades();
            dtmi.createCascades();
            ImageStack frames = dtmi.createLabeledImage().getStack();
            for(int j = 1; j<=frames.getSize(); j++){
                result.addSlice(frames.getSliceLabel(j), frames.getProcessor(j));
            }
            long finished = System.currentTimeMillis() - start;
            System.out.println("finished in " + (finished/1000.0) + " seconds");
        }
        ImagePlus transformed = stack.original.createImagePlus();
        transformed.setTitle(stack.original.getShortTitle() +  "-transformed.tif");
        transformed.setStack(result, 1, stack.getNSlices(), stack.getNFrames());
        transformed.setOpenAsHyperStack(true);
        transformed.show();
    }

    /**
     * Gets the currently selected MeshImageStack.
     *
     * @return
     */
    public MeshImageStack getMeshImageStack(){
        return model.stack;
    }

    /**
     * Remeshes the current track by using the remesh connection algorithm.
     *
     * @param track track to be remeshed.
     * @param frame the frame # of the mesh to be remeshed.
     * @param minConnectionLength connections shorter that the minimum length will be removed.
     * @param maxConnectionLength connections long than the maximum length will be split in two.
     */
    public void reMeshConnections(Track track, int frame, double minConnectionLength, double maxConnectionLength){
        if(minConnectionLength > maxConnectionLength){
            System.out.println("Minimum connection length should be less than max connection length");
            return;
        }
        if(!track.containsKey(frame)){
            return;
        }
        main.submit(()->{
            ConnectionRemesher remesher =  new ConnectionRemesher();
            remesher.setMinAndMaxLengths(minConnectionLength, maxConnectionLength);

            DeformableMesh3D newMesh = remesher.remesh(track.getMesh(frame));
            setMesh(track, frame, newMesh);
        });
    }



    /**
     * Adds all of the provided meshes to the corresponding track. The tracks and meshes are associated by
     * order.
     *
     * @param tracks
     * @param frame
     * @param meshes
     */
    public void setMeshes(List<Track> tracks, int frame, List<DeformableMesh3D> meshes){
        if(tracks.size() != meshes.size()){
            throw new RuntimeException("tracks and meshes must be a 1 to 1 correspondance");
        }


        actionStack.postAction(new UndoableActions(){

            final List<Track> tcks = new ArrayList<>(tracks);
            final List<DeformableMesh3D> oldMeshes = tcks.stream().map(t -> t.getMesh(frame)).collect(Collectors.toList());
            final List<DeformableMesh3D> newer = new ArrayList<>(meshes);
            final int f = frame;
            @Override
            public void perform() {
                submit(() -> {
                    for(int i = 0; i<tcks.size(); i++){
                        model.addMeshToTrack(f, newer.get(i), tcks.get(i));
                    }
                });
            }

            @Override
            public void undo() {
                submit(()->{
                    for(int i = 0; i<tcks.size(); i++){
                        DeformableMesh3D old = oldMeshes.get(i);
                        DeformableMesh3D mesh = newer.get(i);
                        Track t = tcks.get(i);
                        if(old==null){
                            model.removeMeshFromTrack(f, mesh, t);
                        } else{
                            model.addMeshToTrack(f, old, t);
                        }

                    }
                });
            }

            @Override
            public void redo() {
                submit(() -> {
                    submit(() -> {
                        for(int i = 0; i<tcks.size(); i++){
                            model.addMeshToTrack(f, newer.get(i), tcks.get(i));
                        }
                    });
                });
            }

            @Override
            public String getName(){
                return "set meshes at " + f + " for " + tcks.size() + " tracks";
            }
        });
    }
    /**
     * Sets the provided mesh to be the mesh for the provided track at the specified frame. This will
     * replace any existing meshes in the provided frame.
     **
     * @param track track that will acquire the new mesh.
     * @param frame frame the mesh will be placed at.
     * @param mesh the mesh that will be part of the track.
     */
    public void setMesh(Track track, int frame, DeformableMesh3D mesh){

        actionStack.postAction(new UndoableActions(){

            final DeformableMesh3D old = track.getMesh(frame);
            final DeformableMesh3D newer = mesh;

            final int f = frame;

            @Override
            public void perform() {
                submit(() -> {
                    model.addMeshToTrack(f, newer, track);
                });
            }

            @Override
            public void undo() {
                submit(()->{
                    if(old==null){
                        model.removeMeshFromTrack(f, mesh, track);
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

            @Override
            public String getName(){
                return "set mesh at " + f + " for " + track.getName();
            }
        });
    }

    /**
     * Training data consists of two images. The original image, and a labelled image which is an a bit image.
     * The first bit represents the mesh, the second bit represents inside, or outside of the mesh and the last 6
     * bits are the distance transform.
     *
     * This will populate two folders, images and labels, with the respective z-stacks for each time point.
     *
     * @param start 0 based time frame. First frame inclusive.
     * @param finish 0 based time frame. Last frame inclusive.
     */
    public void generateTrainingData(int start, int finish){
        ImagePlus original = getMeshImageStack().original;
        List<Track> tracks = getAllTracks();
        Path baseFolder = Paths.get(IJ.getDirectory("Select root folder"));
        Create3DTrainingDataFromMeshes creator = new Create3DTrainingDataFromMeshes(tracks, getMeshImageStack().original);
        Path labelPath = baseFolder.resolve("labels");
        Path imagePath = baseFolder.resolve("images");
        try {
            if(!Files.exists(imagePath)){
                Files.createDirectory(imagePath);
            }
            if(!Files.exists(labelPath)){
                Files.createDirectory(labelPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("unable to create directories for output", e);
        }

        File labelFolder = labelPath.toFile();
        File imageFolder = imagePath.toFile();
        String name = original.getTitle().replace(".tif", "");

        for(int i = start; i<=finish; i++){
            String sliceName = String.format("%s-t%04d.tif", name, i);
            creator.run(i);
            ImagePlus maskPlus = original.createImagePlus();
            maskPlus.setStack(creator.getLabeledStack());
            IJ.save(maskPlus, new File(labelFolder, sliceName).getAbsolutePath());
            System.out.println("finished frame: " + i);
            //maskPlus.show();
            try {
                ImagePlus scaled = creator.getOriginalFrame(i);
                //scaled.setOpenAsHyperStack(true);
                scaled.setLut(LUT.createLutFromColor(Color.WHITE));
                IJ.save(scaled, new File(imageFolder, sliceName).getAbsolutePath());

            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Requests a labelled image that will be used with the currently opened image for generating a pair of image/labels
     * training data.
     * @param first
     * @param last
     */
    public void generateTrainingDataFromLabelledImage(int first, int last){

    }

    /**
     * Remesh the selected mesh to the provided min and max connection lengths.
     *
     * @see SegmentationController#reMeshConnections(Track, int, double, double)
     * @param minConnectionLength normalized length
     * @param maxConnectionLength normalized length
     */
    public void reMeshConnections(double minConnectionLength, double maxConnectionLength){
        int f = model.getCurrentFrame();
        Track track = model.getSelectedTrack();
        if(track == null){
            //nothing to do no track selected.
            return;
        }

        reMeshConnections(track, f, minConnectionLength, maxConnectionLength);
    }

    /**
     * Get the number of channels of the currently selected image.
     * @return
     */
    public int getNChannels(){
        return model.getNChannels();
    }

    /**
     * Gets the currently selected channel.
     *
     * @return
     */
    public int getCurrentChannel(){
        return getMeshImageStack().channel;
    }

    /**
     * Creates a plot of mesh count vs time.
     *
     */
    public void plotCellCount(){
        MeshAnalysis.plotMeshesOverTime(getAllTracks(), getMeshImageStack());
    }

    /**
     * Creates a plot with information about the frame to frame displacement of the current set of meshes.
     */
    public void plotTrackingStatistics(){
        TrackAnalysis.plotNextFrameTrackingResults(getAllTracks(), getCurrentFrame());
    }

    /**
     * Plots the volumes of meshes over time for all of the meshes. Each track is a continuous set of data points.
     *
     */
    public void plotVolumes(){
        MeshAnalysis.plotVolumesOverTime(getAllTracks(), getMeshImageStack());
    }

    /**
     * Creates two plots. Displacements vs Time per tracks and a histogram of displacements.
     */
    public void plotDisplacements(){

        MeshAnalysis.plotDisplacementsVsTime(getAllTracks(), getMeshImageStack());
        MeshAnalysis.plotDisplacementHistogram(getAllTracks(), getMeshImageStack());

    }

    /**
     * Finds the principle moments of Inertia and plots the anisotropy over time.
     *
     */
    public void plotElongationsVsTime(){
        MeshAnalysis.plotElongationsVsTime(getAllTracks());
    }

    /**
     * Shows a two vectors. One indicating the moment of inertia of the collective mesh rotation.
     * One indicates the angular momentum of an equivalent rigid body rotation.
     *
     * @return {px, py, pz}
     */
    public double[] showInertialVector(){
        List<Track> tracks = getAllTracks();
        int i = getCurrentFrame();
        double[] center0 = { 0, 0, 0};
        double[] center1 = { 0, 0, 0};

        double volume0 = 0;
        double volume1 = 0;
        for(Track t: tracks){
            if(t.containsKey(i-1) && t.containsKey(i+1)){
                DeformableMesh3D start = t.getMesh(i-1);
                DeformableMesh3D fin = t.getMesh(i+1);

                double v0 = start.calculateVolume();
                double[] c = DeformableMesh3DTools.centerAndRadius(start.nodes);
                center0[0] += c[0]*v0;
                center0[1] += c[1]*v0;
                center0[2] += c[2]*v0;
                volume0 += v0;

                double v1 = fin.calculateVolume();
                c = DeformableMesh3DTools.centerAndRadius(fin.nodes);
                center1[0] += c[0]*v1;
                center1[1] += c[1]*v1;
                center1[2] += c[2]*v1;
                volume1 += v1;

            }
        }
        //weighted center of mass for all of the objects.
        center0[0] = center0[0]/volume0;
        center0[1] = center0[1]/volume0;
        center0[2] = center0[2]/volume0;

        center1[0] = center1[0]/volume1;
        center1[1] = center1[1]/volume1;
        center1[2] = center1[2]/volume1;

        double[] moment = new double[3];

        double[][] inertialMatrix = new double[3][3];

        for(Track t: tracks){
            if(t.size()<2){
                continue;
            }
            if(t.containsKey(i-1) && t.containsKey(i+1)){
                DeformableMesh3D start = t.getMesh(i-1);
                DeformableMesh3D fin = t.getMesh(i+1);
                double[] cs = DeformableMesh3DTools.centerAndRadius(start.nodes);
                double[] cf = DeformableMesh3DTools.centerAndRadius(fin.nodes);

                double[] csp = Vector3DOps.difference(cs, center0);
                double[] cfp = Vector3DOps.difference(cf, center1);

                double[] v = Vector3DOps.difference(cfp, csp);


                double[] r = Vector3DOps.average(csp, cfp);

                double v1 = fin.calculateVolume();
                double v2 = start.calculateVolume();
                double factor = 0.5*(v1+v2);
                for(int k = 0; k<3; k++){
                    int a = k;
                    int b = (k+1)%3;
                    int c = (k+2)%3;

                    inertialMatrix[a][a] += ( r[b]*r[b] + r[c]*r[c] ) * factor;
                    inertialMatrix[b][a] += -r[b]*r[a]*factor;
                    inertialMatrix[c][a] += -r[c]*r[a]*factor;
                }

                double[] angMom = Vector3DOps.cross(r, v);
                double vave = 0.5*(v1 + v2);
                moment[0] += angMom[0]*vave;
                moment[1] += angMom[1]*vave;
                moment[2] += angMom[2]*vave;

                DeformableLine3D lines = new DeformableLine3D(Arrays.asList(cs, cf), Arrays.asList(new int[]{0,1}));

                meshFrame3D.addTransientObject(lines.getDataObject());
            }

        }

        Matrix iM = new Matrix(inertialMatrix);
        LUDecomposition lu = new LUDecomposition(iM);
        try {
            Matrix s = lu.solve(new Matrix(moment, 3));
            double [] angularVelocity = s.getRowPackedCopy();

            Vector3DOps.normalize(angularVelocity);
            Arrow a = new Arrow(1, 0.01);
            a.pointAlong(angularVelocity);
            a.moveTo(center0[0], center0[1], center0[2]);
            a.setColor(Color.RED);

            Arrow a2 = new Arrow(1, 0.01);
            a2.setColor(Color.CYAN);
            a2.moveTo(center0[0], center0[1], center0[2]);
            a2.pointAlong(moment);

            meshFrame3D.addTransientObject(a);
            meshFrame3D.addTransientObject(a2);
            return moment;
        }catch(Exception e){
            return moment;
        }
    }

    /**
     * Causes no mesh track to be selected.
     *
     */
    public void selectNone(){
        submit(() ->model.selectMeshTrack(null));
    }

    /**
     * The history can take up a lot of memory, especially for actions like deformlAllMeshes.
     *
     */
    public void clearHistory(){
        actionStack.clearHistory();
    }
    /**
     * Thresholds the current image and places meshes over the individual regions. Checks for overlap with
     * existing meshes and doesn't place meshes if there is too much overlap.
     *
     * @param level The value used for thresholding the image. Expected to work on integer images.
     * @return an ImagePlus with the thresholded image.
     */
    public ImagePlus guessMeshes(int level) {
        MeshDetector detector = new MeshDetector(getMeshImageStack());

        int frame = getCurrentFrame();
        List<Box3D> current = getAllTracks().stream().filter(
                t->t.containsKey(frame)
        ).map(
                t->t.getMesh(frame).getBoundingBox()
        ).collect(Collectors.toList());

        detector.addRegionsToAvoid(current);

        List<DeformableMesh3D> guessed = detector.guessMeshes(level);

        startNewMeshTracks(guessed);

        //add all of the guessed meshes to new tracks in this frame.
        return new ImagePlus("threshold", detector.getThreshedStack());
    }

    /**
     * Applies the connection remesh algorith to all meshes in the current frame.
     *
     * @see SegmentationController#reMeshConnections(Track, int, double, double)
     * @param minConnectionLength normalized length for min connection lengths.
     * @param maxConnectionLength normalized length for max connection lengths.
     */
    public void reMeshConnectionsAllMeshes(double minConnectionLength, double maxConnectionLength){
        if(minConnectionLength > maxConnectionLength){
            System.out.println("cannot remesh with a min length longer than a short length!");
            return;
        }
        int f = model.getCurrentFrame();
        List<Track> tracks = model.getAllTracks().stream().filter(t -> t.containsKey(f)).collect(Collectors.toList());
        submit( ()->{
            List<DeformableMesh3D> remeshed = tracks.stream().map( t->{
                DeformableMesh3D mesh = t.getMesh(f);
                ConnectionRemesher remesher =  new ConnectionRemesher();
                remesher.setMinAndMaxLengths(minConnectionLength, maxConnectionLength);
                DeformableMesh3D rep;
                try{
                    rep = remesher.remesh(mesh);
                } catch(Exception e){
                    System.err.println(e.getMessage());
                    rep = mesh;
                }
                return rep;

            }).collect(Collectors.toList());
            setMeshes(tracks, f,  remeshed);
        });
    }
    /**
     * Takes the currently selected mesh and looks for neighbors. Locates 'touching' faces and a
     * dds transient objects that show the touching surface. Also produces curvature histograms,
     * for the whole cell, and the regions touch.
     */
    public void curvatureSnapShot(){

        final Track track = model.getSelectedTrack();
        final int frame = model.getCurrentFrame();
        if(track==null || !track.containsKey(frame)) return;
        DeformableMesh3D mesh = track.getMesh(frame);

        List<Track> neighbors = model.getAllTracks().stream().filter(
                t->!track.equals(t)
        ).filter(
                t->t.containsKey(frame)
        ).collect(Collectors.toList());

        Graph curvatures = new Graph();

        CurvatureCalculator cc = new CurvatureCalculator(mesh);

        List<double[]> xy = cc.createCurvatureHistogram();

        DataSet set = curvatures.addData(xy.get(0), xy.get(1));
        set.setColor(mesh.getColor());
        set.setLabel("total");

        for(Track track2: neighbors) {
            MeshFaceObscuring face = new MeshFaceObscuring();
            face.setNeighbor(track2.getMesh(frame));

            DeformableMesh3D m2 = model.createSubMesh(mesh, face.getOverlapArea(mesh));
            m2.setColor(track2.getColor());
            m2.create3DObject();
            m2.setShowSurface(true);
            addTransientObject(m2.data_object);

            cc = new CurvatureCalculator(m2);

            xy = cc.createCurvatureHistogram();

            set = curvatures.addData(xy.get(0), xy.get(1));
            set.setColor(m2.getColor());
            set.setLabel(track2.getName());

        }

        curvatures.show(false, "Curvature Snapshot of " + getSelectedMeshName() + " frame: " + frame);

    }

    /**
     * Creates a 3D plot frame for the currently selected mesh, and plots the curvature on the surface.
     *
     * @return an object for generating a surface plot.
     */
    public SurfacePlot curvatureSurfacePlot(){

        DeformableMesh3D mesh = getSelectedMesh();
        if(mesh==null){
            return null;
        }

        return new CurvatureSurfacePlot(mesh);
    }

    /**
     * Creates an object that is used for producing an intensity plot based on the currently selected mesh and image.
     *
     * @return
     */
    public SurfacePlot intensitySurfacePlot(){
        DeformableMesh3D mesh = getSelectedMesh();
        if(mesh==null){
            return null;
        }

        IntensitySurfacePlot plot = new IntensitySurfacePlot(mesh, model.stack);
        return plot;
    }

    /**
     * Shows the volume of the currently selected mesh by adding a voxel transient object, similar to the way
     * show volume works, but the result is binary and colored.
     *
     */
    public void showBinaryBlob(){
        if(model.hasSelectedMesh()) {
            main.submit(() -> {

                int f = model.getCurrentFrame();
                DeformableMesh3D mesh = model.getSelectedMesh(f);
                ImagePlus plus = DeformableMesh3DTools.createBinaryRepresentation(model.stack, mesh);
                List<int[]> pts = new ArrayList<>();
                ImageStack stack = plus.getStack();
                int lx = Integer.MAX_VALUE;
                int ly = Integer.MAX_VALUE;
                int lz = Integer.MAX_VALUE;

                for(int j = 1; j<= plus.getNSlices(); j++){
                    ImageProcessor proc = stack.getProcessor(j);
                    final int w = proc.getWidth();
                    final int h = proc.getHeight();
                    for(int i = 0; i<w*h; i++){
                        if(proc.get(i)!=0){
                            int x = i%w;
                            int y = i/w;
                            int z = j-1;
                            pts.add(new int[]{x, y, z});
                            lx = x<lx?x:lx;
                            ly = y<ly?y:ly;
                            lz = z<lz?z:lz;
                        }
                    }
                }
                VolumeDataObject obj = new VolumeDataObject(model.getSelectedTrack().getColor());
                obj.setTextureData(model.stack, pts);
                double[] corner = model.stack.getNormalizedCoordinate(
                        new double[]{
                                lx-model.stack.offsets[0]*0.5, ly-model.stack.offsets[0]*0.5, lz-model.stack.offsets[0]*0.5
                        });
                obj.setPosition(corner[0], corner[1], corner[2]);
                meshFrame3D.addTransientObject(obj);
            });
        }
    }

    /**
     * Clears ALL of the current meshes.
     *
     */
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

            @Override
            public String getName(){
                return "restart meshes";
            }
        });
    }

    /**
     * For using save without save-as.
     *
     * @return
     */
    public File getLastSavedFile(){
        return model.getLastSavedFile();
    }

    /**
     * Adds the mesh to the currently selected track.
     * @see SegmentationController#addMesh(int frame, DeformableMesh3D mesh)
     * @param m
     */
    public void addMesh(DeformableMesh3D m){
        addMesh(model.getCurrentFrame(), m);
    }

    /**
     * Starts a new mesh track with one mesh on the specified frame.
     *
     * @param frame frame mesh is added to. Note 0 based indexing.
     * @param mesh
     */
    public void startNewMeshTrack(int frame, DeformableMesh3D mesh){

        actionStack.postAction(new UndoableActions(){
            final DeformableMesh3D m = mesh;
            final int f = frame;
            Track track;
            @Override
            public void perform() {
                submit(()->{
                    track = model.startMeshTrack(f, m);
                });
            }

            @Override
            public void undo() {
                submit( ()->{
                    model.removeMeshFromTrack(f, m, track);
                });
            }

            @Override
            public void redo() {
                submit(()->{
                    model.addMeshToTrack(f, m, track);
                });
            }
            @Override
            public String getName(){
                return "start new track";
            }
        });
    }

    /**
     * Changes the currently selected track to be the next one.
     */
    public void selectNextMeshTrack(){
        submit(model::selectNextTrack);
    }

    /**
     * Changes to the previously selected track.
     */
    public void selectPreviousMeshTrack(){
        submit(model::selectPreviousTrack);
    }
    /**
     * Overload for convenience.
     * @param track track that will be selected. can be null
     */
    public void selectMeshTrack(Track track){
        submit( () -> model.selectMeshTrack(track));
    }

    /**
     * Attempts to select the track that contains the provided mesh.
     *
     * @param mesh
     */
    public void selectMesh(DeformableMesh3D mesh) {
        submit(()->model.selectTrackWithMesh(mesh));
    }

    /**
     * Adds the provided mesh to the model. The behavior is conditional on the currently selected track.
     *
     * If there is a selected track, the mesh is added to the selected track. Replacing any previous meshes at the
     * at the provided frame.
     *
     * If there isn't a selected track, then a new track is started.
     *
     *
     * @param frame
     * @param m
     */
    public void addMesh(int frame, DeformableMesh3D m){

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

            @Override
            public String getName(){
                return "add mesh";
            }
        });
    }

    /**
     * Addes a line that follows the center of mass of the provided mesh track to the current frame.
     *
     * @param t
     */
    public void addTracer(Track t){
        int s = t.getFirstFrame();
        int e = t.getLastFrame();
        int f = getCurrentFrame();

        e = f<e ? f : e;

        List<double[]> points = new ArrayList<>();
        List<int[]> connections = new ArrayList<>();


        for(int i = s; i<=e; i++){
            if(t.containsKey(i)){
                DeformableMesh3D m = t.getMesh(i);
                int last = points.size();
                points.add(DeformableMesh3DTools.centerAndRadius(m.nodes));
                if(last > 0){
                    connections.add(new int[]{last-1, last});
                }
            }
        }
        if(connections.size() == 0) return;

        DeformableLine3D line = new DeformableLine3D(points, connections);
        line.createDataObject();
        float[] comps = t.getColor().getRGBComponents(new float[4]);
        line.data_object.setColor(comps[0], comps[1], comps[2]);
        addTransientObject(line.data_object);
    }

    /**
     * Starts a new Track for each of provided meshes. Used with guess meshes.
     * @param meshes If the list is empty, this is a non-op.
     */
    public void startNewMeshTracks(List<DeformableMesh3D> meshes){
        if(meshes.size() == 0 ) return;

        actionStack.postAction(new UndoableActions() {
            final int frame = getCurrentFrame();
            final List<Track> tracks = new ArrayList<>(meshes.size());
            @Override
            public void perform() {
                submit(()->{
                    for(DeformableMesh3D mesh: meshes){
                        Track track = model.startMeshTrack(frame, mesh);
                        tracks.add(track);
                    }
                });
            }

            @Override
            public void undo() {
                submit(()->{
                    tracks.forEach(model::removeMeshTrack);
                });
            }

            @Override
            public void redo() {
                submit(()->{
                    tracks.forEach(model::addMeshTrack);
                });
            }
            @Override
            public String getName(){
                return "Added " + tracks.size() + " mesh tracks";
            }
        });
    }
    /**
     * The provided normal and position represent a plane. This transformer is used for transforming between the
     * plane coordinates, and world coordinates.
     *
     * @param pos center of the plane
     * @param normal normal of the plane.
     * @return for transforming coordinates.
     */
    public FurrowTransformer createFurrowTransform(double[] pos, double[] normal) {
        return getMeshImageStack().createFurrowTransform(pos, normal);
    }


    public void flipFurrow(){
        model.flipFurrow();
    }
    /**
     * Creates an image based on a furrow transformer built using the provided position and normal.
     *
     * Primarily used for GUI.
     *
     * @param pos center of plane
     * @param normal direction of normal
     * @return sliced image.
     */
    public Image createSlice(double[] pos, double[] normal) {
        return getMeshImageStack().createSlice(pos, normal);
    }

    /**
     * Creates an Image based on slicing the working volume with the plane represented by this transformer.
     *
     * Primarily used for GUI.
     * @param transformer
     * @return
     */
    public Image createSlice(FurrowTransformer transformer){
        return getMeshImageStack().createSlice(transformer);
    }

    /**
     * Adds a data object to the current meshframe, the object is "transient" and will be removed
     * when clear transient objects is called.
     *
     * @param dataObject 3d display object
     */
    public void addTransientObject(DataObject dataObject) {
        submit( ()->meshFrame3D.addTransientObject(dataObject) );
    }

    /**
     * clears all transient objects, objects being displayed in the 3d viewer that were added as transient objects.
     *
     */
    public void clearTransientObjects(){
        if(meshFrame3D != null) {
            submit(() -> meshFrame3D.clearTransients());
        }
    }

    /**
     * Opens a window with volume measurements for the currently selected mesh track. If There is a furrow, then the
     * volume is split into front and back halfs.
     *
     */
    public void measureVolume() {
        model.calculateVolume();
    }


    /**
     * Iterates over all of the meshes and calculates intensity. (a bit intense.)
     */
    public void calculateAllInterfaceTimeScans(){
        submit(model::calculateInterfaceLineScans);
    }

    /**
     * Calculates the time course for the selected mesh only.
     *
     */
    public void calculateSelectedInterfaceTimeScans(){
        Track track = model.getSelectedTrack();
        if(track != null){
            submit(()->{
                model.calculateInterfaceLineScan(track);
            });
        }
    }

    /**
     * TODO remove
     * @Deprecated
     * @see deformablemesh.util.MeshAnalysis
     */
    public void calculateLineScans() {
        model.calculateLineScans();
    }

    /**
     * TODO remove
     * @Deprecated
     */
    public void showStress() {
        model.showStress();
    }

    /**
     * TODO remove
     * @Deprecated
     */
    public void showCurvature() {
        model.showCurvature();
    }

    /**
     * Shows the volume data in the meshframe. The program is much slower with the volume showing. It can be faster
     * to adjust min/max (contrast) and set the frame with the volume hidden.
     */
    public void showVolume() {
        submit(()->{
            meshFrame3D.showVolume(model.stack);
            meshFrame3D.setVisible(true);
        });
    }

    /**
     * The type of image energy that will be used.
     *
     * @param selectedItem enum to select which image energy will be created.
     */
    public void setImageEnergyType(ImageEnergyType selectedItem) {
        model.setImageEnergyType(selectedItem);
    }

    /**
     * Stops displaying the volume in the meshframe3d.
     *
     */
    public void hideVolume() {
        submit(meshFrame3D::hideVolume);
    }

    /**
     * Adjust the min/max values for clipping the image. Any values less than minDelta are transparent, and values above
     * maxDelta are opaque.
     *
     * @param minDelta
     * @param maxDelta
     */
    public void changeVolumeClipping(int minDelta, int maxDelta) {
        submit(()->meshFrame3D.changeVolumeClipping(minDelta, maxDelta));
    }

    public void showVolumeClippingDialog(){
        VolumeDataObject vdo = meshFrame3D.getVolumeDataObject();
        if(vdo!=null){
            VolumeContrastSetter setter = new VolumeContrastSetter(vdo);
            setter.setPreviewBackgroundColor(meshFrame3D.getBackgroundColor());
            setter.showDialog(meshFrame3D.getJFrame());
        }
    }

    /**
     * Deforms mesh until stopped.
     *
     */
    public void deformMesh(){
        if(model.hasSelectedMesh()) {
            deformMesh(-1);
        }
    }

    /**
     * Splits this track into two with all meshes from, and up to the current frame in one
     * track and all of the subsequent meshes in a new track.
     *
     */
    public void splitMeshTrack(){

        Track t = getSelectedMeshTrack();
        int frame = getCurrentFrame();
        if(t != null && t.containsKey(frame) && t.getLastFrame() > frame){
            String actionName = "split " + t.getName();
            UndoableActions action = new UndoableActions() {
                Map<Integer, DeformableMesh3D> original = t.getTrack();
                Map<Integer, DeformableMesh3D> result =new TreeMap<>();
                Map<Integer, DeformableMesh3D> remainder = new TreeMap<>();
                Track t1 = model.startEmptyTrack();
                @Override
                public void perform() {
                    for(Integer i: original.keySet()){
                        if(i > frame){
                            remainder.put(i, original.get(i));
                        } else{
                            result.put(i, original.get(i));
                        }
                    }
                    t1.setData(remainder);
                    t.setData(result);
                    model.addMeshTrack(t1);
                }

                @Override
                public void undo() {
                    t.setData(original);
                    model.removeMeshTrack(t1);
                }

                @Override
                public void redo() {
                    t.setData(result);
                    model.addMeshTrack(t1);
                }
                @Override
                public String getName(){
                    return actionName;
                }
            };
            actionStack.postAction(action);
        }


    }

    public void validateMeshes(){
        List<Track> tracks = getAllTracks();
        int removed = 0;
        for(Track t: tracks){
            Set<Integer> old = new HashSet<>(t.getTrack().keySet());
            for(Integer i: old){
                DeformableMesh3D mesh = t.getMesh(i);
                boolean remove = false;
                for(double d: mesh.positions){
                    if(Double.isNaN(d)){
                        remove = true;
                        break;
                    }
                }
                if(remove){
                    clearMeshFromTrack(t, i);
                    removed++;
                }
            }
        }
        System.out.println("removed: " + removed);
    }

    public void splitMesh(){
        Track track = getSelectedMeshTrack();
        int frame = getCurrentFrame();

        DeformableMesh3D mesh = track.getMesh(frame);
        Furrow3D furrow = model.getRingController().getFurrow();
        if(furrow == null || mesh == null){
            return;
        }
        InterceptingMesh3D im = new InterceptingMesh3D(mesh);
        Furrow3D reversed = new Furrow3D(furrow.cm,
                new double[]{-furrow.normal[0], -furrow.normal[1], -furrow.normal[2]}
        );

        List<List<Node3D>> nodes = furrow.splitNodes(mesh.nodes);

        clearMeshFromTrack(track, frame, mesh);
        List<DeformableMesh3D> splits = new ArrayList<>();
        for(List<Node3D> side: nodes){
            List<Interceptable> interceptables = Arrays.asList(reversed, furrow, im);
            if(side.size()==0){
                continue;
            }
            double[] c = new double[3];
            for(Node3D n: side){
                double[] xyz = n.getCoordinates();
                c[0] += xyz[0];
                c[1] += xyz[1];
                c[2] += xyz[2];
            }
            c[0] = c[0]/side.size();
            c[1] = c[1]/side.size();
            c[2] = c[2]/side.size();
            DeformableMesh3D a = RayCastMesh.rayCastMesh(interceptables, c, 2);
            splits.add(a);
        }
        startNewMeshTracks(splits);

    }

    public void deformPartialMesh(){
        deformPartialMesh(-1);
    }


    public void deformPartialMesh(int n){
        Track track = getSelectedMeshTrack();
        int frame = getCurrentFrame();

        DeformableMesh3D mesh = track.getMesh(frame);
        Furrow3D furrow = model.getRingController().getFurrow();
        if(furrow == null || mesh == null){
            return;
        }


        List<List<Node3D>> frontBack = furrow.splitNodes(mesh.nodes);
        List<Node3D> nodes = frontBack.get(1);

        if(nodes.size()==0){
            return;
        }
        actionStack.postAction(new UndoableActions(){
            final double[] positions = Arrays.copyOf(mesh.positions, mesh.positions.length);
            double[] newPositions;
            @Override
            public void perform() {
                main.submit(() -> {
                    model.removeMeshFromTrack(frame, mesh, track);
                    DeformableMesh3D subMesh =mesh.createSubMesh(nodes);
                    subMesh.create3DObject();
                    subMesh.setShowSurface(true);
                    meshFrame3D.addDataObject(subMesh.data_object);
                    model.deformMesh(subMesh, n);

                    for(int i = 0; i<nodes.size(); i++){
                        int n = nodes.get(i).index;
                        mesh.positions[3*n] = subMesh.positions[3*i];
                        mesh.positions[3*n + 1] = subMesh.positions[3*i + 1];
                        mesh.positions[3*n + 2] = subMesh.positions[3*i + 2];

                    }
                    mesh.resetPositions();
                    model.addMeshToTrack(frame, mesh, track);
                    model.selectMeshTrack(track);
                    meshFrame3D.removeDataObject(subMesh.data_object);
                    newPositions = Arrays.copyOf(mesh.positions, mesh.positions.length);
                });

            }

            @Override
            public void undo() {
                main.submit(()->{
                    mesh.setPositions(positions);
                });
            }

            @Override
            public void redo() {
                main.submit(()->{
                    mesh.setPositions(newPositions);
                });
            }

            @Override
            public String getName(){
                return "deform submesh";
            }

        });


    }

    /**
     * Adds a transient data object using a 3D volume texture and a surface with the currently
     * selected mesh geometry.
     */
    public void showTexturedMeshSurface(){
        submit(()->{

            DeformableMesh3D mesh = getSelectedMesh();
            if(mesh == null) return;
            TexturedPlaneDataObject tpdo = new TexturedPlaneDataObject(mesh, model.stack);
            meshFrame3D.addTransientObject(tpdo);

        });
    }

    /**
     * Deforms all meshes in the current frame sequentially.
     *
     */
    public void deformAllMeshes(){
        deformAllMeshes(-1);
    }

    /**
     * Deforms all meshes in the current frame for steps number of iterations, sequentially.
     * If steps is < 0 then it will deform for Integer.MAX_VALUE iterations...which will probably
     * take forever.
     *
     * @param steps
     */
    public void deformAllMeshes(int steps){


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
                        model.deformMeshes(meshes, steps);
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

                @Override
                public String getName(){
                    return "deform all meshes";
                }
            });
        }
    }

    /**
     * If a point is outside of the boundary, it is moved to the edge along that axis.
     *
     * @param mesh
     */
    public void confineMesh(final DeformableMesh3D mesh){
        final Box3D box = getBounds();
        if(box.contains(mesh.getBoundingBox())){
            return;
        }
        actionStack.postAction(new UndoableActions(){
            final double[] positions = Arrays.copyOf(mesh.positions, mesh.positions.length);
            double[] newPositions;
            @Override
            public void perform() {
                main.submit(() -> {
                    mesh.confine(box);
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

            @Override
            public String getName(){
                return "confine mesh";
            }
        });


    }

    /**
     * Hard boundaries cause a mesh to be confined after each step during deformation.
     *
     * @see SegmentationController#confineMesh(DeformableMesh3D)
     * @param v
     */
    public void setHardBoundaries(boolean v){
        model.setHardBoundaries(v);
    }

    /**
     * Deforms mesh for a set number of iterations.
     *
     * @param count number of iterations, if less than zero, it continues to deform until stopped.
     */
    public void deformMesh(final int count){
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
                main.submit(()->{
                    mesh.setPositions(positions);
                });
            }

            @Override
            public void redo() {
                main.submit(()->{
                    mesh.setPositions(newPositions);
                });
            }

            @Override
            public String getName(){
                return "deform mesh";
            }

        });
    }

    /**
     * Creates a copy of the provided mesh.
     *
     * @param mesh
     * @return
     */
    public DeformableMesh3D copyMesh(DeformableMesh3D mesh){
        return DeformableMesh3DTools.copyOf(mesh);
    }

    /**
     * Extands the track to (frame + 1) provided by copying the deformable mesh at frame.
     *
     * @param track track to be extended.
     * @param frame initial frame
     */
    public void trackMesh(final Track track, final int frame){
        final int next = frame + 1;
        final DeformableMesh3D tracking = track.getMesh(frame);
        if(tracking==null){
            return;
        }
        final DeformableMesh3D old = track.getMesh(next);
        final DeformableMesh3D newer = copyMesh(tracking);


        actionStack.postAction(new UndoableActions() {
            @Override
            public void perform() {
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

            @Override
            public String getName(){
                return "track mesh";
            }

        });
    }
    /**
     * Creates a copy of the current mesh, advances a frame and adds it to the current track.
     *
     *
     */
     public void trackMesh(){

        if(model.hasSelectedMesh() && model.hasNextFrame()){
            final int frame = model.getCurrentFrame();
            Track track = model.getSelectedTrack();
            trackMesh(track, frame);
            nextFrame();
        }

    }

    /**
     * Copies the current mesh, moves to the previous frame and adds the copy to the currently selected track.
     *
     */
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

                @Override
                public String getName(){
                    return "track mesh backwards";
                }

            });
            previousFrame();
        }
    }

    /**
     * Primarily used to stop deforming a mesh.
     */
    public void stopRunning() {
        model.stopRunning();
    }

    /**
     * Sets the current image, and sets it on channel 0. Attempts to stay on the same frame.
     *
     * @param plus new backing image.
     */
    public void setOriginalPlus(ImagePlus plus){
        setOriginalPlus(plus, 0);
    }

    /**
     * Causes the provided image to be the main backing image data. Tries to stay on the current
     * frame if possible.
     * @param plus
     */
    public void setOriginalPlus(ImagePlus plus, int channel) {
        submit(
                ()->{
                    boolean volumeShowing = meshFrame3D!=null && meshFrame3D.volumeShowing();
                    if(volumeShowing){
                        meshFrame3D.hideVolume();
                    }
                    model.setOriginalPlus(plus, channel);

                    if(volumeShowing) {
                        showVolume();
                    }
                }
        );
    }

    public void selectChannel(int c){
        setOriginalPlus(model.original_plus, c);
    }


    /**
     * Saves the current meshes
     *
     * @param f where they'll be saved.
     */
    public void saveMeshes(File f) {
        submit(()->{
            model.saveMeshes(f);
            lastSaved.set(getCurrentState());
        });
    }

    /**
     * The action stack controls the undo/redo commands. Each time a new command is posted and
     * performed it updates the id. This returns the current id of the action stack.
     * @return id of current state, incremented by one each time a new action is posed.
     */
    public long getCurrentState(){
        return actionStack.getCurrentState();
    }

    /**
     * Loads meshes and replaces the current meshes.
     *
     * @param f
     */
    public void loadMeshes(File f) {
        submit(()->{
            List<Track> replacements = MeshReader.loadMeshes(f);
            actionStack.postAction(new UndoableActions(){
                final List<Track> old = new ArrayList<>(model.getAllTracks());
                @Override
                public void perform() {
                    submit(()->{
                        model.setMeshes(replacements);
                        lastSaved.set(actionStack.getCurrentState());
                    });

                }

                @Override
                public void undo() {
                    submit(()->{
                        model.setMeshes(old);
                    });
                }

                @Override
                public void redo() {
                    submit(()->{
                        model.setMeshes(replacements);
                    });
                }

                @Override
                public String getName(){
                    return "load meshes";
                }

            });
        });

    }


    /**
     * Opens the meshfile and adds all of the meshes to the current meshes. They type
     * determines how meshes are added.
     *
     * aligned: the frame of the imported meshes does not change.
     *
     * relative: The first frame of the meshes being imported are set to be the
     * current frame. This can be good for working with selected frames of a movie.
     * Eg. Segment by hand, frames 5-10, then move to frame 5 and import relative.
     *
     * lumped: All of the meshes in the importing file are imported into the current frame.
     *
     * select: Only meshes from the current frame of the selected meshfile are imported to the current frame.
     *
     * @param f
     * @param type
     */
    public void importMeshes(File f, ImportType type) {
        submit(()->{
            List<Track> imports = MeshReader.loadMeshes(f);
            importMeshes(imports, type);
        });
    }
    public void importMeshes(List<Track> imports, ImportType type){
        submit(()->{
            int i = getCurrentFrame();
            int n = getNFrames();

            //the imports is modified to meet the supplied criteria.
            switch(type){
                case aligned:
                    //use them as they are.
                    break;
                case relative:
                    relateImports(imports);
                    break;
                case lumped:
                    lumpImports(imports);
                    break;
                case select:
                    selectImports(imports);
                default:
                    break;
            }

            actionStack.postAction(new UndoableActions(){
                final List<Track> old = new ArrayList<>(model.getAllTracks());
                @Override
                public void perform() {
                    submit(()->{
                        imports.addAll(old);
                        model.setMeshes(imports);
                    });

                }

                @Override
                public void undo() {
                    submit(()->{
                        model.setMeshes(old);
                    });
                }

                @Override
                public void redo() {
                    submit(()->{
                        model.setMeshes(imports);
                    });
                }

                @Override
                public String getName(){
                    return "import meshes: " + type.name();
                }
            });
        });
    }

    /**
     * Groups all of the meshes into the current frame. New tracks are created for
     * tracks with multiple meshes.
     *
     * @param imports
     */
    public void lumpImports(List<Track> imports) {
        Integer current = getCurrentFrame();
        List<Track> bonusTracks = new ArrayList<>();

        for(Track track: imports){
            if(track.size() == 1){
                DeformableMesh3D mesh = track.getMesh(track.getFirstFrame());
                track.remove(mesh);
                track.addMesh(current, mesh);
            } else{
                Integer key = track.containsKey(current) ? current : track.getFirstFrame();
                DeformableMesh3D mesh = track.getMesh(key);
                track.remove(mesh);
                //go through remaining meshes and create a new track for them.
                for(Map.Entry<Integer, DeformableMesh3D> entry: track.getTrack().entrySet()){
                    if(!current.equals(entry.getKey())) {
                        track.remove(entry.getValue());
                        Track bonus = new Track(track.getName() + "-" + entry.getKey());
                        bonus.addMesh(current, entry.getValue());
                        bonusTracks.add(bonus);
                    }
                }
                track.addMesh(current, mesh);
            }


        }
        imports.addAll(bonusTracks);
    }

    /**
     * Finds the earliest mesh frame, aligns that to the current frame
     * @param imports
     */
    public void relateImports(List<Track> imports) {
        int min = imports.stream().map(Track::getFirstFrame).mapToInt(i -> i).min().orElseGet(()->0);

        int current = getCurrentFrame();
        int shift = current - min;
        for(Track track: imports){
            Map<Integer, DeformableMesh3D> replacements = new HashMap<>();
            for(Map.Entry<Integer, DeformableMesh3D> entry: track.getTrack().entrySet()){
                track.remove(entry.getValue());
                Integer target = entry.getKey() + shift;
                replacements.put(target, entry.getValue());
            }
            replacements.forEach((key, value)->track.addMesh(key, value));
        }

    }

    /**
     * Reduces the provided meshes to only meshes in the currently selected frame.
     *
     * @param imports list of tracks that will be modified to only have tracks with meshes in the
     *                current frame.
     */
    public void selectImports(List<Track> imports) {
        Integer frame = getCurrentFrame();
        List<Track> toRemove = new ArrayList<>();
        for(Track track: imports){
            if(track.containsKey(frame)){
                for(Map.Entry<Integer, DeformableMesh3D> entries : track.getTrack().entrySet()){
                    if(!frame.equals(entries.getKey())){
                        track.remove(entries.getValue());
                    }
                }
            } else{
                toRemove.add(track);
            }
        }
        imports.removeAll(toRemove);
    }

    /**
     * Opens a new window that renders the provided meshes in 2D.
     *
     */
    public void renderMeshesIn2D(){
        RenderFrame2D renderer =  RenderFrame2D.createRenderingMeshFrame();

        renderer.setTracks(getAllTracks());
        renderer.setFrame(getCurrentFrame());
        FrameListener l = renderer::setFrame;
        addFrameListener(l);
        renderer.addCloseListener( ()->{
            removeFrameListener(l);
        } );
    }

    /**
     * Replaces the current tracks with the provided. Used for the mesh track manager.
     *
     * @param replacements
     */
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

                @Override
                public String getName(){
                    return "set tracks";
                }

            });
        });
    }

    /**
     * Saves all of the meshes in the current frame as an ascii stl file.
     *
     * @param f
     */
    public void exportAsStl(File f) {
        submit(()->model.exportAsStl(f));
    }

    /**
     * Export as wire-frame mesh stl file.
     *
     * @param f
     */
    public void exportAsWireframeStl(File f){
        submit(()->model.exportAsWireframeStl(f));
    }

    /**
     * Loads furrows, 3D plane 1 per frame.
     *
     * @param f
     */
    public void load3DFurrows(File f) {
        submit(()->model.load3DFurrows(f));
    }

    /**
     * Saves the furrow from each plane.
     * @param f
     */
    public void saveFurrows(File f) {
        submit(()->model.saveFurrows(f));
    }

    /**
     * For the gui to check for errors.
     *
     * @return
     */
    public List<Exception> getExecutionErrors(){
        return main.getExceptions();
    }

    /**
     * Zero based frame number. The displayed frame number is 1 based.
     * @return
     */
    public int getCurrentFrame() {
        return model.getCurrentFrame();
    }

    /**
     * Loads an imageplus from the provided file.
     *
     * @param file_name
     */
    public void loadImage(String file_name){
        ImagePlus plus = new ImagePlus(file_name);
        setOriginalPlus(plus);
    }



    /**
     * @return currently selected mesh.
     */
    public DeformableMesh3D getMesh() {
        return model.getSelectedMesh(model.getCurrentFrame());
    }

    /**
     *
     * @return currently selected mesh track.
     */
    public Track getSelectedMeshTrack(){
        return model.getSelectedTrack();
    }

    /**
     * Returns the short image name based on the full image title with the image name extension removed.
     *
     * @return
     */
    public String getShortImageName() {
        return model.getShortImageName();
    }

    /**
     * deformation parameter: represents the bending stiffness.
     * @param d
     */
    public void setBeta(double d) {
        model.setBeta(d);
    }

    public double getBeta(){
        return model.getBeta();
    }

    /**
     * Turns on an external energy that tries to normalize triangle sizes.
     *
     * @param d
     */
    public void setNormalizerWeight(double d) {
        model.setNormalizerWeight(d);
    }

    public double getNormalizeWeight() {
        return model.getNormalizeWeight();
    }

    /**
     * Creates an imageplus that is a binary image with pixes values 1 inside a mesh and 0 outside.
     *
     * @see DeformableMesh3DTools#createBinaryRepresentation(MeshImageStack, DeformableMesh3D)
     * */
    public void createBinaryImage() {
        submit(()->{
           model.createBinaryImage();
        });
    }

    /**
     * Creates a labelled image of the provided tracks. The labels are not
     * related frame to frame.
     */
    public void createLabelledImage(List<Track> tracks){
        submit( ()->{
            ImagePlus plus = DeformableMesh3DTools.createUniqueLabelsRepresentation(
                    getMeshImageStack(), tracks
            );
            plus.setOpenAsHyperStack(true);
            plus.setTitle(model.original_plus.getShortTitle() + "-labelled_image");
            plus.show();
        });

    }
    /**
     * For creating a new mesh, if there is a currently selected mesh in the current frame,
     * this starts a new mesh track. If there isn't then addMesh is used.
     *
     * @see SegmentationController#addMesh(int, DeformableMesh3D)
     *
     * @param mesh
     */
    public void initializeMesh(DeformableMesh3D mesh) {

        int f = model.getCurrentFrame();
        if(model.getSelectedMesh(f)==null){
            addMesh(f, mesh);
        } else{
            startNewMeshTrack(f, mesh);
        }
    }

    /**
     * @return an unmodifiable view of the current mesh tracks.
     */
    public List<Track> getAllTracks() {
        return model.getAllTracks();
    }

    /**
     *
     * @return The mesh from the currently selected mesh track in the current frame. Can be null.
     */
    public DeformableMesh3D getSelectedMesh() {
        return model.getSelectedMesh(model.getCurrentFrame());
    }

    /**
     *
     * @return Color that is used to display the current image as a volume in the 3D canvas.
     */
    public Color getVolumeColor() {
        return model.volumeColor;
    }

    /**
     * The color the image volume is display as, when the image volume is showing.
     *
     * @param color
     */
    public void setVolumeColor(Color color){
        model.volumeColor = color;
        showVolume();
    }

    /**
     *
     * @return the ration of distance between slices to the height of a pixel.
     */
    public double getZToYScale() {
        return model.getZToYScale();
    }

    /**
     *
     * @return {width (pixels), height(pixels), slices}
     */
    public int[] getOriginalStackDimensions() {
        return model.getOriginalStackDimensions();
    }

    /**
     * The origin in the 3d view is in the center of the original image.
     *
     * @return
     */
    public double[] getSurfaceOffsets() {

        return model.getSurfaceOffsets();

    }

    /**
     * Controls the furrow.
     *
     * @return
     */
    public RingController getRingController() {
        return model.getRingController();
    }

    /**
     * Bounds of the current image being analyzed.
     * @return
     */
    public Box3D getBounds() {
        return model.getBounds();
    }

    /**
     * Creates a mosaic image, which is like a binary image, execept each mesh is labelled with it's color instead of
     * binary.
     */
    public void createMosaicImage() {

        submit(()->{
            model.createMosaicImage();
        });

    }

    /**
     * Measures the volumes for all of the mesh tracks, creates a text window with the data like.
     * #frame\ttrack1\ttrack2 ...
     * If a track doesn't exist in a particular frame, then it's volume is -1.
     */
    public void measureAllVolumes() {
        submit(()->{
            model.measureAllVolumes();
        });
    }

    /**
     * Checks if an image is loaded.
     * @return
     */
    public boolean hasOriginalPlus() {
        return model.original_plus!=null;
    }

    /**
     * Total number of frames in the movie.
     * @return
     */
    public int getNFrames() {
        if(model.original_plus==null){
            return -1;
        }
        return model.original_plus.getNFrames();
    }

    /**
     * Toggles displaying the surface of the currently selected mesh track.
     */
    public void toggleSurface() {
        if(model.hasSelectedMesh()){
            Track track = model.getSelectedTrack();
            track.setShowSurface(!track.getShowSurface());
        }
    }
    public void createDataTable(){
        String heading = "#Trackmate-export coodinates.\n"
                       + "#x\ty\tz\t<I>\tV\tV2\n";
        List<Track> tracks = getAllTracks();
        StringBuilder builder = new StringBuilder(heading);
        MeshImageStack mis = getMeshImageStack();
        int f = getCurrentFrame();
        for(int z = 0; z<getNFrames(); z++){
            final int i = z;
            mis.setFrame(z);
            List<DeformableMesh3D> meshes = tracks.stream().filter(t->t.containsKey(i)).map(t->t.getMesh(i)).collect(Collectors.toList());
            for(DeformableMesh3D mesh: meshes){
                double[] center = TrackMateAdapter.getCenterRealCoordinates(mesh, mis);
                List<int[]> volumePixels = DeformableMesh3DTools.getContainedPixels(mis, mesh);
                double intensity = 0;
                if( volumePixels.size() > 0) {
                    for (int[] values : volumePixels) {
                        intensity += mis.getValue(values[0], values[1], values[2]);
                    }
                    intensity = intensity/volumePixels.size();
                } else{
                    double[] c = mesh.getBoundingBox().getCenter();
                    intensity = mis.getInterpolatedValue(c);
                }

                double volume = volumePixels.size() * mis.pixel_dimensions[0]*mis.pixel_dimensions[1]*mis.pixel_dimensions[2];
                double volume2 = mesh.calculateVolume()*mis.SCALE*mis.SCALE*mis.SCALE;
                builder.append( String.format(Locale.US,
                        "%d\t%f\t%f\t%f\t%f\t%f\t%f\n",
                        i,  center[0], center[1], center[2], intensity, volume, volume2));
            }

        }
        mis.setFrame(f);
        GuiTools.createTextOuputPane(builder.toString());
    }
    public void plotVolumeAveragedIntensityVsTime(){
        List<Track> tracks = getAllTracks();
        Graph plot = new Graph();
        MeshImageStack stack = getMeshImageStack();
        final int start = stack.CURRENT;

        List<List<Integer>> allTimes = new ArrayList<>(tracks.size());
        List<double[]> allIntensities = new ArrayList<>(tracks.size());
        int min = stack.getNFrames();
        int max = 0;
        for(Track track: tracks){
            allTimes.add(new ArrayList<>(track.size()));
            allIntensities.add(new double[track.size()]);
            min = Math.min(track.getFirstFrame(), min);
            max = Math.max(track.getLastFrame(), max);
        }

        for(int i = min; i<=max; i++){
            System.out.println("frame: " + i);
            stack.setFrame(i);
            for(int j = 0; j<tracks.size(); j++){
                Track t = tracks.get(j);
                if(t.containsKey(i)) {
                    DeformableMesh3D mesh = t.getMesh(i);
                    List<int[]> volumePixels = DeformableMesh3DTools.getContainedPixels(stack, mesh);
                    double intensity = 0;
                    if( volumePixels.size() > 0) {
                        for (int[] values : volumePixels) {
                            intensity += stack.getValue(values[0], values[1], values[2]);
                        }
                        intensity = intensity/volumePixels.size();
                    } else{
                        double[] c = mesh.getBoundingBox().getCenter();
                        intensity = stack.getInterpolatedValue(c);
                    }

                    List<Integer> times = allTimes.get(j);
                    allIntensities.get(j)[times.size()] = intensity;
                    times.add(i);
                }
            }
        }
        for(int j = 0; j<tracks.size(); j++){
            List<Integer> keys = allTimes.get(j);
            Track track = tracks.get(j);
            double[] times = keys.stream().mapToDouble(Double::valueOf).toArray();

            DataSet set = plot.addData(times, allIntensities.get(j));
            set.setLabel(track.getName());
            set.setColor(track.getColor());

        }

        stack.setFrame(start);
        plot.show(false, "Intensity vs Time");

    }

    /**
     * Measures the selected track through time and calculates:
     *
     * c_x, c_y, c_z: centroid position coordinates.
     * dmean: mean distance of nodes to centroid.
     * dmax: max distance of nodes to centroid.
     * dmin: min distance of nodes to centroid.
     * Eigen values and vectors
     * lambda1, lambda2, lambda3: eigen values.
     * v1_x, v1_y, v1_z: first eigen vector.
     * v2_x, v2_y, v2_z: second eigen vector.
     * v3_x, v3_y, v3_z: third eigen vector.
     *
     */
    public void measureSelected() {
        if(model.hasSelectedMesh()){
            submit(model::measureSelectedMesh);
        }
    }


    /**
     * Creates a text window with the current furrow values, position in image units and normal.
     *
     */
    public void showFurrowValues() {
        model.showFurrowValues();
    }

    /**
     * Export every mesh in the current frame as a ply file with color.
     *
     * @param f
     */
    public void exportAsPly(File f) {
        submit(()->model.exportAsPly(f));

    }

    /**
     * Gets the name of the currently selected mesh.
     *
     * @return
     */
    public String getSelectedMeshName() {
        Track t = model.getSelectedTrack();
        if(t!=null)
            return t.getName();
        else
            return "";
    }

    /**
     * Calculates the surface area over time, and the surface areas that are obscured by neighboring meshes.
     *
     * @param v cutoff distance from the center of the triangle along the normal to another triangle. Units are
     *          the units of the image.
     */
    public void calculateObscuringMeshes(double v) {

        int frame = getCurrentFrame();
        List<Track> tracks = model.getAllTracks();
        MeshImageStack stack = model.stack;
        MeshFaceObscuring.analyzeTracks(tracks, stack, frame, v/stack.SCALE);


    }

    /**
     * returns true if the current state of mesh tracks has changed.
     * @return
     */
    public boolean getMeshModified() {
        return lastSaved.get() != actionStack.getCurrentState();
    }

    /**
     * Gets the current number of deformations that have occurred.
     *
     * @return
     */
    public int getDeformationSteps(){
        return model.deformations;
    }

    /**
     * Saves the current deformation parameters to a file.
     *
     * @param f
     */
    public void saveParameters(File f) {
        submit(()->{
            PropertySaver.saveProperties(this, f);
        });
    }

    /**
     * gets deformation parameters that have been saved in a file.
     * @see PropertySaver
     * @param f
     */
    public void loadParameters(File f){
        submit(()->{
            PropertySaver.loadProperties(this, f);
        });
    }

    /**
     * When true, deforming meshes are confined at each step.
     * @param selected
     */
    public void setRigidBoundaries(boolean selected) {
        model.setHardBoundaries(selected);
    }

    /**
     * Gets the name of the next action to be undone, if it has a name.
     *
     * @return ActionStack#getUndoableActionName
     */
    public String getUndoName() {

        return actionStack.getUndoableActionName();

    }

    /**
     * Returns the name of the next action that can be redone.
     *
     * @return ActionStack#getRedoableActionName()
     */
    public String getRedoName(){
        return actionStack.getRedoableActionName();
    }

    public double getStericNeighborWeight() {

        return model.getStericNeighborWeight();
    }

    /**
     * Shows forces on the currently selected mesh. First clears any external energies\
     * then shows any updates.
     *
     */
    public void showForces() {
        DeformableMesh3D mesh = getSelectedMesh();
        if(mesh==null) return;
        mesh.clearEnergies();
        showForces(mesh);
    }

    /**
     * Creates an image that is generated by the current image energy.
     */
    public void createEnergyImage(){
        model.createEnergyImage();
    }

    /**
     * Creates a set of vectors to show how the mesh will deform based on the image energy and
     * external forces.
     *
     * The vectors are added as transient objects.
     *
     * @param mesh
     */
    public void showForces(DeformableMesh3D mesh){
        boolean clear = true;
        if(mesh.getExternalEnergies().size()>0){
            clear = false;
        } else{
            model.getExternalEnergies(mesh).forEach(mesh::addExternalEnergy);
        }

        VectorField vf = new VectorField(mesh);

        vf.initialize();

        if(clear){
            mesh.clearEnergies();
        }
        meshFrame3D.addTransientObject(vf);
    }

    /**
     * Checks for a MeshFrame3D.
     *
     * @return
     */
    public boolean has3DViewer() {
        return meshFrame3D != null;
    }

    /**
     * This is being exposed to create undoable tasks it should not be used if it can be avoided.
     *
     * @return
     */
    public SegmentationModel getModel() {
        return model;
    }

    /**
     * Tasks for the exception throwing service.
     */
    public interface Executable{
        void execute() throws Exception;
    }

    /**
     * Sets the position and normal of the furrow.
     *
     * @see RingController
     * @param center
     * @param normal
     */
    public void setFurrowForCurrentFrame(double[] center, double[] normal){

        submit(()->{
            RingController rc = model.getRingController();
            rc.setFurrow(normal, center);
        });

    }

    /**
     * Sets up the provided meshframe3d as the main threed display.
     *
     * @param meshFrame3D
     */
    public void setMeshFrame3D(MeshFrame3D meshFrame3D) {

        this.meshFrame3D = meshFrame3D;
        meshFrame3D.setBackgroundColor(model.backgroundColor);
        meshFrame3D.setSegmentationController(this);
        //for mesh only updates
        model.addMeshListener(meshFrame3D::syncMesh);


        model.addFrameListener((i)->{
            meshFrame3D.updateRingController();
            if(meshFrame3D.volumeShowing()) {
                meshFrame3D.showVolume(model.stack);
            }
            meshFrame3D.syncMesh(i);

        });

        meshFrame3D.addPickListener(new PickSelector(this));

    }

    /**
     * Returns the main 3D view. This can be used to set the background color.
     * @return
     */
    public MeshFrame3D getMeshFrame3D(){
        return meshFrame3D;
    }




    /**
     * Adds a new frame listener that gets notified whenever: nextFrame, previousFrame, setFrame, or the backing imageplus is changed.
     * @param listener
     */
    public void addFrameListener(FrameListener listener){
        model.addFrameListener(listener);
    }

    /**
     * If the currently selected mesh is available, this well look in the same
     * area in the next frame and link the best possibility.
     */
    public void linkPossibleTrack(){

        if(getSelectedMesh()!=null && getCurrentFrame() < getNFrames() - 1 ){
            Track destination = getSelectedMeshTrack();
            nextFrame();
            int frame = getCurrentFrame();
            if(destination.containsKey(frame)){
                //only a track that does not have a mesh on the next frame.
               return;
            }
            DeformableMesh3D mesh = destination.getMesh(frame - 1 );
            Box3D bb = mesh.getBoundingBox();
            List<Track> possible = getAllTracks().stream().filter(
                                        t -> t.getFirstFrame() == frame
                                    ).collect(Collectors.toList());
            double maxJi = 0;
            int dex = -1;
            for(int i = 0; i<possible.size(); i++){
                DeformableMesh3D candidate = possible.get(i).getMesh(frame);
                Box3D cb = candidate.getBoundingBox();
                Box3D union = cb.getIntersectingBox(bb);
                double uv = union.getVolume();
                if(uv == 0){
                    continue;
                }
                double ji = uv/(cb.getVolume() + bb.getVolume() - uv);
                if(ji > maxJi){
                    maxJi = ji;
                    dex = i;
                }
            }

            if (dex >= 0) {
                Track consumed = possible.get(dex);
                Map<Integer, DeformableMesh3D> data = destination.getTrack();
                Map<Integer, DeformableMesh3D> moving = consumed.getTrack();
                Map<Integer, DeformableMesh3D> combined = new TreeMap<>();
                combined.putAll(data);
                combined.putAll(moving);

                UndoableActions joinMeshes = new UndoableActions() {
                    @Override
                    public void perform() {
                        destination.setData(combined);
                        model.removeMeshTrack(consumed);
                    }

                    @Override
                    public void undo() {
                        destination.setData(data);
                        model.addMeshTrack(consumed);

                    }

                    @Override
                    public void redo() {
                        destination.setData(combined);
                        model.removeMeshTrack(consumed);
                    }
                };

                actionStack.postAction(joinMeshes);
            }
        }
    }
    public void removeFrameListener(FrameListener listener){
        model.removeFrameListener(listener);
    }

    public void autotrackAvailableTracks(){
        submit( () -> {
           FrameToFrameDisplacement ftf =  FrameToFrameDisplacement.trackAvailableFrameForward(this);
           actionStack.postAction(ftf.getPerform());
        });
    }
    public void shutdown(){
        main.submit(main::shutdown);
    }
}

/**
 * Historical class, that should be replaced, developed because of confusion regarding the way ExecutorServices
 * handle exceptions
 *
 * Executables are submitted and if they're already running on the main thread, then they're short circuited otherwise
 * they're submitted to the main executor service, which puts them in the queue for execution.
 */
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

    private void execute(SegmentationController.Executable e){
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

        final Future<?> f = main.submit(()->execute(r));

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

    public void shutdown(){
        main.shutdown();
    }



}
