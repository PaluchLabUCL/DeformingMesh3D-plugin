package deformablemesh;

import Jama.LUDecomposition;
import Jama.Matrix;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.ImageEnergyType;
import deformablemesh.geometry.*;
import deformablemesh.gui.FrameListener;
import deformablemesh.gui.GuiTools;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.gui.render2d.RenderFrame2D;
import deformablemesh.io.ImportType;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.*;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.simulations.FillingBinaryImage;
import deformablemesh.track.Track;
import deformablemesh.util.*;
import deformablemesh.util.actions.ActionStack;
import deformablemesh.util.actions.UndoableActions;
import deformablemesh.util.connectedcomponents.ConnectedComponents3D;
import deformablemesh.util.connectedcomponents.Region;
import deformablemesh.util.connectedcomponents.RegionGrowing;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import lightgraph.DataSet;
import lightgraph.Graph;
import lightgraph.GraphPoints;

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
 * Created by msmith on 2/11/16.
 */
public class SegmentationController {

    final SegmentationModel model;

    private ActionStack actionStack = new ActionStack();

    MeshFrame3D meshFrame3D;

    AtomicLong lastSaved = new AtomicLong(-1);
    ExceptionThrowingService main = new ExceptionThrowingService();
    private double minConnectionLength = 0.005;
    private double maxConnectionLength = 0.02;

    /**
     * Creates a controller for the supplied model.
     *
     * @param model
     */
    public SegmentationController(SegmentationModel model){
        this.model = model;
        model.setRingController(new RingController(this));
    }

    /**
     * Deformation parameter, high values limit the rate of deformation.
     * @param gamma
     */
    public void setGamma(double gamma){
        model.setGamma(gamma);
    }

    /**
     * Deformation parameter, effectively the spring-like stiffness connections.
     *
     * @param d
     */
    public void setAlpha(double d) {
        model.setAlpha(d);
    }

    /**
     * Non-zero values create an effective force that either causes the mesh to expand (positive) or shrink (negative).
     * @param d
     */
    public void setPressure(double d) {
        model.setPressure(d);
    }

    /**
     * Causes a 'steric' force that other meshes will prevent inclusion by causing a force directed from their center
     * on any nodes that enter.
     *
     *
     * @param d
     */
    public void setStericNeighborWeight(double d){
        model.setStericNeighborWeight(d);
    }

    /**
     * @Depracated
     *
     * @param d
     */
    public void setCortexThickness(double d) {
        model.setCortexThickness(d);
    }

    /**
     * Sets the magnitude of force the image causes on the mesh.
     *
     * @param d
     */
    public void setWeight(double d) {
        model.setWeight(d);
    }

    /**
     * When a new mesh is created, it is subdivided this number of times. 0 deformations corresponds to 20 triangles.
     * Each division divides the triangles into 4, so there will be 20*4**N triangles. 5 divisions is 20480 triangles,
     * which displays fine, but not practical for deforming.
     *
     * TODO: Add a limit.
     * @param d
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
    public void setMinConnectionLength(double mcl){
        minConnectionLength = mcl;
    }

    public double getMaxConnectionLength(){
        return maxConnectionLength;
    }

    public void setMaxConnectionLength(double mcl){
        maxConnectionLength = mcl;
    }

    /**
     * Adds a listener, that gets notified every time a track changes: when a mesh is added or removed, all of the meshes are set
     * the track selection is changed. Also notified when 'clear transients' is used to remove transient objects added
     * to meshframe.
     *
     * @param listener
     */
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

    /**
     * Least efficient way to clear a mesh. Removes the mesh one time from a single track.
     *
     * @param mesh
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
     *
     * @param f
     * @param t
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
     * Moves the action stack back.
     */
    public void undo(){
        if(canUndo()){
            actionStack.undo();
        }
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
     * Changes image frame.
     *
     */
    public void previousFrame() {
        submit(model::previousFrame);
    }

    /**
     * Changes image frame.
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
     * Creates a snapshot of the current 3d view and creates an image plus window.
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
     * Creates an imageplus with snapshots from each frame.
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
            dtmi.createGrowingCascades();
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
    public MeshImageStack getMeshImageStack(){
        return model.stack;
    }


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
     * Sets the provided mesh to be the mesh for the provided track at the specified frame.
     *
     * undo-ably.
     *
     * @param track
     * @param frame
     * @param mesh
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
    public void autotrack(){

    }
    /**
     * Starts processing a single frame at a time.
     *
     * @param start 0 based time frame. First frame inclusive.
     * @param finish 0 based time frame. Last frame inclusive.
     */
    public void generateTrainingData(int start, int finish){
        //frames
        //output folder;
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

    public void reMeshConnections(double minConnectionLength, double maxConnectionLength){
        int f = model.getCurrentFrame();
        Track track = model.getSelectedTrack();
        if(track == null){
            //nothing to do no track selected.
            return;
        }

        reMeshConnections(track, f, minConnectionLength, maxConnectionLength);
    }

    public int getNChannels(){
        return model.getNChannels();
    }
    public int getCurrentChannel(){
        return getMeshImageStack().channel;
    }
    public void plotCellCount(){
        MeshAnalysis.plotMeshesOverTime(getAllTracks(), getMeshImageStack());
    }

    public void plotTrackingStatistics(){
        TrackAnalysis.plotNextFrameTrackingResults(getAllTracks(), getCurrentFrame());
    }
    public void plotVolumes(){
        MeshAnalysis.plotVolumesOverTime(getAllTracks(), getMeshImageStack());
    }

    public void plotDisplacements(){

        MeshAnalysis.plotDisplacementsVsTime(getAllTracks(), getMeshImageStack());
        MeshAnalysis.plotDisplacementHistogram(getAllTracks(), getMeshImageStack());

    }

    public void plotElongationsVsTime(){
        MeshAnalysis.plotElongationsVsTime(getAllTracks());
    }

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

            double momentum = Vector3DOps.normalize(moment);
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

    public void selectNone(){
        submit(() ->model.selectMeshTrack(null));
    }

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
     * Remeshes all meshes in the current frame.
     * @param minConnectionLength
     * @param maxConnectionLength
     */
    public void reMeshConnectionsAllMeshes(double minConnectionLength, double maxConnectionLength){
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
     * Takes the currently selected mesh and looks for neighbors. Locates 'touching' faces and adds transient objects
     * That show the touching surface. Also produces curvature histograms, for the whole cell, and the regions touch.
     *
     *
     */
    public void curvatureSnapShot(){

        main.submit(()->{
            _curvatureSnapShot();
        });

    }

    /**
     * Creates a 3D plot frame for the currently selected mesh, and plots the curvature on the surface.
     *
     * TODO change this to return an object similar to IntensitySurfacePlot
     *
     */
    public SurfacePlot curvatureSurfacePlot(){

        DeformableMesh3D mesh = getSelectedMesh();
        if(mesh==null){
            return null;
        }

        CurvatureSurfacePlot plot = new CurvatureSurfacePlot(mesh);
        return plot;


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

    private void _curvatureSnapShot(){
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

        //cc.setMaxCurvature(5);
        //cc.setMinCurvature(-5);

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

            //cc.setMaxCurvature(5);
            //cc.setMinCurvature(-5);

            xy = cc.createCurvatureHistogram();

            set = curvatures.addData(xy.get(0), xy.get(1));
            set.setColor(m2.getColor());
            set.setLabel(track2.getName());

        }

        curvatures.show(false, "Curvature Snapshot of " + getSelectedMeshName() + " frame: " + frame);
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

    public void selectNextMeshTrack(){
        submit(model::selectNextTrack);
    }

    /**
     * Overload for convenience.
     * @param track
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

    public void startNewMeshTracks(List<DeformableMesh3D> meshes){

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

    /**
     * Creates an image based on a furrow transformer built using the provided position and normal.
     *
     * Primarily used for GUI.
     *
     * @param pos center of plane
     * @param normal direction of normal
     * @return
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
     * Adds a data object to the current meshframe, the object is "transient" and can be cleared independent of the
     * model.
     *
     * @param dataObject
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
     * Iterates over all of the messes and performs the calculation. (a bit intense.)
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
                    model.deformMesh(subMesh);

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

    public void showTexturedMeshSurface(){
        submit(()->{

            DeformableMesh3D mesh = getSelectedMesh();
            TexturedPlaneDataObject tpdo = new TexturedPlaneDataObject(mesh, model.stack);
            meshFrame3D.addTransientObject(tpdo);

        });
    }

    /**
     * Deforms all of the meshes in the current frame sequentially.
     *
     */
    public void deformAllMeshes(){


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
     * Extands the track to (frame + 1) provided by copying the deformable mesh at frame
     * d
     * @param track
     * @param frame
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
     * Causes the provided image to be the main backing image data. The image needs to be 1 channel, with xyz it can
     * also have frames.
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
     * Development. This is for isolating regions to see them better. Possibly will not be finished.
     *
     */
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

    public long getCurrentState(){
        return actionStack.getCurrentState();
    }

    /**
     * Loads meshes and replaces the current meshes. This is undo-able.
     *
     * @param f
     */
    public void loadMeshes(File f) {
        submit(()->{
            List<Track> replacements = MeshWriter.loadMeshes(f);
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
     * Opens the meshfile and adds all of the meshes to the current meshes.
     *
     * @param f
     * @param type
     */
    public void importMeshes(File f, ImportType type){
        boolean relative = true;
        submit(()->{
            int i = getCurrentFrame();
            int n = getNFrames();

            List<Track> imports = MeshWriter.loadMeshes(f);
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
                    return "import meshes";
                }
            });
        });
    }

    /**
     * Groups all of the meshes into the current frame and creates new tracks as necessary.
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
    public void renderMeshesIn2D(){
        RenderFrame2D renderer =  RenderFrame2D.createRenderingMeshFrame();

        renderer.setTracks(getAllTracks());
        renderer.setFrame(getCurrentFrame());

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
     * Turns on an image energy that tries to normalize triangles.
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
     * Creates an imageplus that is a binary image with pixes inside of a mesh 1 outside 0.
     *
     * @see DeformableMesh3DTools#createBinaryRepresentation(MeshImageStack, DeformableMesh3D)
     * */
    public void createBinaryImage() {
        submit(()->{
           model.createBinaryImage();
        });
    }

    /**
     * For creating a new mesh, if there is a currently selected mesh in the current frame, this starts a new mesh track.
     * If there isn't then addMesh is used.
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

    public DeformableMesh3D getSelectedMesh() {
        return model.getSelectedMesh(model.getCurrentFrame());
    }

    /**
     * Used to notify the mesh tracks have changed.
     */
    public void notifyMeshListeners() {
        submit(()->{
            model.notifyMeshListeners();
        });
    }

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

    public void createEnergyImage(){
        model.createEnergyImage();
    }
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
     * Opens the file f and adds the current meshes to the file. Assumes the current image is a
     * viewbox *within* the image associated with the provided mesh file.
     * @param meshFile destination.
     * @param viewBox The current meshes will be scaled and translated to fit in the
     */
    public void exportTo(File meshFile, double[] viewBox) {

    }

    public boolean has3DViewer() {
        return meshFrame3D != null;
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

    public void removeFrameListener(FrameListener listener){
        model.removeFrameListener(listener);
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
