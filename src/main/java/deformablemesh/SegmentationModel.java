package deformablemesh;

import deformablemesh.externalenergies.*;
import deformablemesh.geometry.*;
import deformablemesh.gui.FrameListener;
import deformablemesh.gui.GuiTools;
import deformablemesh.gui.RingController;
import deformablemesh.io.FurrowWriter;
import deformablemesh.io.MeshWriter;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.MeshTracker;
import deformablemesh.track.Track;
import deformablemesh.util.IntensitySurfacePlot;
import deformablemesh.util.MeshAnalysis;
import deformablemesh.util.MeshFaceObscuring;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import lightgraph.DataSet;
import lightgraph.Graph;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class SegmentationModel {
    double GAMMA = 500.0;
    double ALPHA = 1.0;
    double BETA = 0.0;
    double pressure = 0.0;
    double stericNeighborWeight;

    private double cortex_thickness = 0.3;

    boolean reshape = true;
    volatile boolean stop = false;
    volatile long deformations;
    ImagePlus original_plus;

    MeshImageStack stack;
    MeshTracker tracker;

    List<FrameListener> frameListeners = new ArrayList<>();
    List<FrameListener>  meshListeners = new ArrayList<>();

    private double image_weight;
    private int divisions = 2;
    private RingController ringController;
    public ImageEnergyType energyType;

    Color backgroundColor = Color.WHITE;
    Color volumeColor = Color.BLUE;


    private double normalize;
    private File lastSavedFile;
    private boolean hardBoundaries = false;

    public SegmentationModel(){

        tracker = new MeshTracker();
        stack = new MeshImageStack();

    }
    public long getDeformationCount(){
        return deformations;
    }
    public void deformMesh(int count){
        stop = false;
        externalEnergies();
        DeformableMesh3D selectedMesh = tracker.getSelectedMesh(getCurrentFrame());
        if(selectedMesh==null){
            GuiTools.errorMessage("No mesh selected to deform!");
        }
        if(selectedMesh.ALPHA!=ALPHA || selectedMesh.BETA!=BETA || selectedMesh.GAMMA!=GAMMA){
            reshape = true;
        }



        if(reshape){
            selectedMesh.ALPHA=ALPHA;
            selectedMesh.GAMMA=GAMMA;
            selectedMesh.BETA=BETA;
            selectedMesh.reshape();
            reshape=false;
        }
        int c = 0;
        if(count<0){
            count = Integer.MAX_VALUE;
        }
        while(!stop&&c<count){
            selectedMesh.update();
            if(hardBoundaries){
                selectedMesh.confine(getBounds());
            }
            c++;
        }

    }

    /**
     * A generic method for deforming all of the meshes provided.
     * @param meshes
     */
    public void deformMeshes(List<DeformableMesh3D> meshes){

        stop = false;
        deformations = 0;
        Map<DeformableMesh3D, List<StericMesh>> stericEnergies = new HashMap<>();

        if(stericNeighborWeight != 0){
            for(DeformableMesh3D mesh: meshes){
                stericEnergies.put(mesh, generateStericEnergies( mesh ) );
            }
        }

        //apply energies
        for(DeformableMesh3D mesh: meshes){
            mesh.clearEnergies();

            //mesh.PRESSURE = pressure;
            ExternalEnergy erg = generateImageEnergy(mesh);
            mesh.addExternalEnergy(erg);

            if(pressure!=0){
                mesh.addExternalEnergy(new PressureForce(mesh, pressure));
            }

            if(normalize!=0){
                mesh.addExternalEnergy(new TriangleAreaDistributor(stack, mesh, normalize));
            }

            if(stericNeighborWeight!=0){
                for(ExternalEnergy eg: stericEnergies.get(mesh)){
                    mesh.addExternalEnergy(eg);
                }
            }

            mesh.ALPHA=ALPHA;
            mesh.GAMMA=GAMMA;
            mesh.BETA=BETA;
            mesh.reshape();
        }

        while(!stop){
            for(DeformableMesh3D mesh: meshes){

                mesh.update();
                if(hardBoundaries){
                    mesh.confine(getBounds());
                }
                if(stop){
                    break;
                }
            }
            deformations++;
            if(stericNeighborWeight!=0) {
                for (DeformableMesh3D mesh : meshes) {
                    for (StericMesh sm : stericEnergies.get(mesh)) {
                        sm.update();
                    }
                }
            }
        }

    }


    public void stopRunning(){
        stop = true;
    }

    public void setGamma(final double gamma){
        if(GAMMA!=gamma){
            reshape=true;
            GAMMA=gamma;
        }
    }

    public void setAlpha(final double alpha){
            if(ALPHA!=alpha){

                reshape=true;
                ALPHA=alpha;
            }
    }

    public void setBeta(double beta) {
        if(BETA!=beta){

            reshape=true;
            BETA=beta;
        }
    }

    public double getBeta(){
        return BETA;
    }



    public double getGamma() {
        return GAMMA;
    }

    public double getAlpha() {
        return ALPHA;
    }


    public void showStress(){
        getSelectedMesh(getCurrentFrame()).calculateStress();
    }


    public void setOriginalPlus(final ImagePlus plus){
        original_plus = plus;
        int cf = getCurrentFrame();
        stack = new MeshImageStack(original_plus);
        stack.setFrame(cf);
        if(stack.CURRENT != cf){
            setFrame(stack.CURRENT);
        } else {
            notifyFrameListeners();
        }
    }

    public void nextFrame(){
        int i = stack.CURRENT;

        stack.nextFrame();

        if (i != stack.CURRENT) {
                notifyFrameListeners();
        }
    }

    private void notifyFrameListeners(){
        for(FrameListener listener: frameListeners){
            listener.frameChanged(stack.CURRENT);
        }

    }

    public void setFrame(final int i){
        if( i>=0 && i< stack.FRAMES && i != stack.CURRENT ) {
            stack.setFrame(i);
            notifyFrameListeners();
        }
    }

    public void previousFrame(){
        int i = stack.CURRENT;
        stack.previousFrame();

        if(i!=stack.CURRENT){
            notifyFrameListeners();
        }
    }

    public void setNormalizerWeight(double d) {
        normalize = d;
    }

    public void setPressure(final double p) {
        pressure = p;
    }

    public double getPressure() {
        return pressure;
    }

    public void setStericNeighborWeight(double d){
        stericNeighborWeight = d;
    }

    public double getStericNeighborWeight(){
        return stericNeighborWeight;
    }
    public void saveMeshes(final File f) throws IOException {
        lastSavedFile = null;
        MeshWriter.saveMeshes(f, tracker);
        lastSavedFile = f;
    }

    public void exportAsStl(File f) throws IOException {
        if(f==null){
            return;
        }
        MeshWriter.saveStlMesh(f, tracker.getAllMeshTracks(), stack.offsets, stack.SCALE, getCurrentFrame());
    }

    public void exportAsWireframeStl(File f) throws IOException {
        if(f==null){
            return;
        }

        MeshWriter.exportToStlWireframe(f, tracker.getAllMeshTracks(), stack.offsets, stack.SCALE, getCurrentFrame());

    }

    public DeformableMesh3D getSelectedMesh(int frame){
        return tracker.getSelectedMesh(frame);
    }


    public int getCurrentFrame(){
        return stack.CURRENT;
    }


    public Track getSelectedTrack(){
        return tracker.getSelectedTrack();
    }

    public void calculateVolume() {
        new MeshAnalysis(getSelectedTrack().getTrack(), ringController.getFurrows(), stack).measureVolume();
    }

    public void load3DFurrows(final File f) {

        Map<Integer, Furrow3D> newFurrows = FurrowWriter.loadFurrows(f, stack);

        for(Integer i: newFurrows.keySet()){
            ringController.setFurrow(i, newFurrows.get(i));
        }

    }


    public void saveFurrows(File f){
        ringController.writeFurrows(f, stack);
    }

    public void createOutput() {
        new MeshAnalysis(getSelectedTrack().getTrack(), ringController.getFurrows(), stack).createOutput(cortex_thickness);
    }

    public void notifyMeshListeners() {
        int i = getCurrentFrame();
        for(FrameListener listener: meshListeners){
            listener.frameChanged(i);
        }
    }

    public Track startMeshTrack(int frame, DeformableMesh3D freshMesh){
        Track track = tracker.createNewMeshTrack(frame, freshMesh);
        notifyMeshListeners();
        return track;
    }

    public void calculateActinIntensity() {
        new MeshAnalysis(
                tracker.getSelectedTrack().getTrack(),
                ringController.getFurrows(),
                stack
        ).calculateAverageIntensity(getCurrentFrame(), cortex_thickness);
    }



    public void setCortexThickness(double d) {

        cortex_thickness = d;

    }

    public double getCortexThickness() {
        return cortex_thickness;
    }

    public void calculateLineScans() {
        new MeshAnalysis(tracker.getSelectedTrack().getTrack(), ringController.getFurrows(), stack).calculateLineScans(getCurrentFrame(), cortex_thickness);
    }

    public void showCurvature() {
        tracker.getSelectedMesh(getCurrentFrame()).calculateCurvature();
    }

    public void setWeight(double d) {

        image_weight=d;

    }

    public double getImageWeight() {
        return image_weight;
    }

    public void setDivisions(int i){
        divisions = i;
    }

    public int getDivisions(){
        return divisions;
    }

    public ExternalEnergy generateImageEnergy(DeformableMesh3D mesh){
        ExternalEnergy erg;
        switch(energyType){
            case PerpendicularIntensity:
                erg = new PerpendicularIntensityEnergy(stack, mesh, getImageWeight());
                break;
            case PerpendicularGradient:
                erg = new PerpendicularGradientEnergy(stack, mesh, getImageWeight());
                break;
            case SmoothingForce:
                erg = new SmoothingForce(mesh, getImageWeight());
            case None:
            default:
                erg = new ExternalEnergy(){
                    @Override
                    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                        return;
                    }
                    @Override
                    public double getEnergy(double[] pos) {
                        return 0;
                    }
                };
        }

        return erg;
    }

    /**
     * The energy type will be used to decide which image energy is add during deformations.
     *
     * @param i
     */
    public void setImageEnergyType(ImageEnergyType i){
        energyType = i;
    }


    public void setRingController(RingController ringController) {

        this.ringController = ringController;
        frameListeners.add(
            (i)->{
                ringController.setStack(stack);
                ringController.frameChanged(i);
            }
        );
    }

    public RingController getRingController() {
        return ringController;
    }

    public void externalEnergies(){
        DeformableMesh3D selectedMesh = getSelectedMesh(getCurrentFrame());
        selectedMesh.clearEnergies();

        //mesh.PRESSURE = pressure;
        if(image_weight!=0) {
            ExternalEnergy erg = generateImageEnergy(selectedMesh);
            selectedMesh.addExternalEnergy(erg);
        }
        if(pressure!=0){
            selectedMesh.addExternalEnergy(new PressureForce(selectedMesh, pressure));
        }

        if(normalize!=0){
            selectedMesh.addExternalEnergy(new TriangleAreaDistributor(stack, selectedMesh, normalize));
        }

        if(stericNeighborWeight!=0){
            List<StericMesh> segs = generateStericEnergies(selectedMesh);
            for(ExternalEnergy eg: segs){
                selectedMesh.addExternalEnergy(eg);
            }
        }


    }

    public List<ExternalEnergy> getExternalEnergies( DeformableMesh3D selectedMesh){
        System.out.println(image_weight + ", " + pressure);
        List<ExternalEnergy> energies = new ArrayList<>();
        //mesh.PRESSURE = pressure;
        if(image_weight!=0) {
            ExternalEnergy erg = generateImageEnergy(selectedMesh);
            energies.add(erg);
        }
        if(pressure!=0){
            energies.add(new PressureForce(selectedMesh, pressure));
        }

        if(normalize!=0){
            energies.add(new TriangleAreaDistributor(stack, selectedMesh, normalize));
        }

        if(stericNeighborWeight!=0){
            List<StericMesh> segs = generateStericEnergies(selectedMesh);
            energies.addAll(segs);
        }

        return energies;
    }

    /**
     * returns a list of all the external energies, except 'ring energy' affecting the currently selected mesh.
     *
     * @return
     */
    public List<ExternalEnergy> getExternalEnergies(){
        DeformableMesh3D selectedMesh = getSelectedMesh(getCurrentFrame());

        List<ExternalEnergy> energies = new ArrayList<>();
        //mesh.PRESSURE = pressure;
        if(image_weight!=0) {
            ExternalEnergy erg = generateImageEnergy(selectedMesh);
            energies.add(erg);
        }
        if(pressure!=0){
            energies.add(new PressureForce(selectedMesh, pressure));
        }

        if(normalize!=0){
            energies.add(new TriangleAreaDistributor(stack, selectedMesh, normalize));
        }

        if(stericNeighborWeight!=0){
            List<StericMesh> segs = generateStericEnergies(selectedMesh);
            energies.addAll(segs);
        }

        return energies;
    }

    private List<StericMesh> generateStericEnergies(DeformableMesh3D mesh) {
        List<Track> tracks = tracker.getAllMeshTracks();
        List<StericMesh> es = new ArrayList<>(tracks.size());
        for(Track track: tracks){
            if(!track.containsMesh(mesh) && track.containsKey(stack.CURRENT) ){

                es.add(new SofterStericMesh(mesh, track.getMesh(stack.CURRENT), stericNeighborWeight));

            }
        }
        return es;
    }

    public Image createSlice(double[] pos, double[] normal) {

        FurrowTransformer transformer = new FurrowTransformer(new Furrow3D(pos, normal), stack);

        return createSlice(transformer);

    }

    public Image createSlice(FurrowTransformer transformer) {

        int xcounts = transformer.getXCounts();
        int ycounts = transformer.getYCounts();
        ImageProcessor proc = new FloatProcessor(xcounts, ycounts);
        double[] pt = new double[2];

        for (int i = 0; i < xcounts; i++) {
            for (int j = 0; j < ycounts; j++) {

                pt[0] = i;
                pt[1] = j;
                double v = stack.getInterpolatedValue(transformer.getVolumeCoordinates(pt));
                try {
                    proc.setf(i, j, (float) v);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        return proc.getBufferedImage();

    }

    public FurrowTransformer createFurrowTransform(double[] pos, double[] normal){

        return new FurrowTransformer(new Furrow3D(pos, normal), stack);

    }



    public void setMeshes(List<Track> meshes) {

        tracker.clearMeshes();
        tracker.addMeshTracks(meshes);
        notifyMeshListeners();
        lastSavedFile=null;
    }



    Pattern p = Pattern.compile("(\\.\\w+)$");
    public String getShortImageName() {
        if(original_plus==null){
            return "ImageNull";
        }
        String original = original_plus.getTitle();
        Matcher m = p.matcher(original);
        int l = 0;
        if(m.find()){
            l = m.group(0).length();
        }
        return original.substring(0, original.length()-l);
    }


    public double getNormalizeWeight() {
        return normalize;
    }

    public void createBinaryImage() {
        ImagePlus plus = DeformableMesh3DTools.createBinaryRepresentation(stack, original_plus, tracker.getSelectedTrack().getTrack());
        plus.show();
    }
    public void createEnergyImage(){
        if(!hasSelectedMesh()) return;
        List<ExternalEnergy> energies = getExternalEnergies();
        ImagePlus plus = original_plus.createImagePlus();
        int w = original_plus.getWidth();
        int h = original_plus.getHeight();
        ImageStack newStack = new ImageStack(w, h);

        for(int i = 1; i<=stack.getNSlices(); i++){
            ImageProcessor proc = new FloatProcessor(w, h);
            for(int j = 0; j<w; j++){
                for(int k = 0; k<h; k++){
                    double[] nc = stack.getNormalizedCoordinate(new double[]{j, k, i});
                    double e = energies.stream().mapToDouble(ee -> ee.getEnergy(nc)).sum();
                    proc.setf(j, k, (float)e);
                }
            }
            newStack.addSlice(proc);
        }
        plus.setStack(newStack, 1, stack.getNSlices(), 1);
        plus.show();

    }
    public void createMosaicImage() {
        ImagePlus plus = DeformableMesh3DTools.createMosaicRepresentation(stack, original_plus, tracker.getAllMeshTracks());
        plus.setTitle("mosaic: " + original_plus.getShortTitle());
        plus.show();
    }

    public boolean hasSelectedMesh() {
        if(tracker.hasSelectedTrack()){
            return tracker.getSelectedMesh(getCurrentFrame())!=null;
        }
        return false;
    }

    public void selectTrackWithMesh(DeformableMesh3D mesh){
        tracker.selectTrackContainingMesh(mesh);
        notifyMeshListeners();
    }

    public void removeMeshFromTrack(int frame, DeformableMesh3D mesh, Track track) {
        tracker.removeMeshFromTrack(frame, mesh, track);
        notifyMeshListeners();
    }

    public void addMeshToTrack(int f, DeformableMesh3D mesh, Track track) {
        tracker.addMeshToTrack(f, mesh, track);
        notifyMeshListeners();
    }

    public void selectNextTrack() {
        tracker.selectNextTrack();
        notifyMeshListeners();
    }

    /**
     *
     * @return an unmodifiable list of all the mesh tracks.
     */
    public List<Track> getAllTracks() {
        return tracker.getAllMeshTracks();
    }

    public void addFrameListener(FrameListener listener) {
        frameListeners.add(listener);
    }

    public void addMeshListener(FrameListener listener){
        meshListeners.add(listener);
    }

    public void removeMeshListener(FrameListener listener){
        meshListeners.remove(listener);
    }

    public double[] getSurfaceOffsets() {
        return new double[]{0, 0, -stack.offsets[2]};
    }

    public double getZToYScale() {
        return stack.pixel_dimensions[2]/stack.pixel_dimensions[1];
    }

    public int[] getOriginalStackDimensions() {
        return new int[]{original_plus.getWidth(), original_plus.getHeight(), original_plus.getNSlices()};
    }

    public Box3D getBounds(){
        return stack.getLimits();
    }


    public void measureAllVolumes() {
        MeshAnalysis.calculateAllVolumes(tracker.getAllMeshTracks(), stack);
    }

    public void measureSelectedMesh(){
        Track track = getSelectedTrack();
        StringBuilder builder = new StringBuilder(
                String.format(
                        "#Deformable Mesh Output. plugin version %s\n#\n",
                        Deforming3DMesh_Plugin.version
                )
        );

        builder.append("#\tc_x, c_y, c_z: centroid position coordinates.\n");
        builder.append("#\tdmean: mean distance of nodes to centroid.\n");
        builder.append("#\tdmax: max distance of nodes to centroid.\n");
        builder.append("#\tdmin: min distance of nodes to centroid.\n");
        builder.append("#\t Eigen values and vectors\n");
        builder.append("#\tlambda1, lambda2, lambda3: eigen values.\n");
        builder.append("#\tv1_x, v1_y, v1_z: first eigen vector.\n");
        builder.append("#\tv2_x, v2_y, v2_z: second eigen vector.\n");
        builder.append("#\tv3_x, v3_y, v3_z: third eigen vector.\n");
        builder.append("#\t<I>: average intensity.\n");
        builder.append("#\n");
        builder.append("#Frame\tVolume\tArea\t<I>\tc_x\tc_y\tc_z\tdmean\tdmax\tdmin\t");
        builder.append("lambda1\tlambda2\tlambda3\tv1_x\tv1_y\tv1_z\tv2_x\tv2_y\tv2_z\tv3_x\tv3_y\tv3_z\n");
        for(int j = 0; j<original_plus.getNFrames(); j++){
            if(!track.containsKey(j)){
                continue;
            }

            DeformableMesh3D mesh = getSelectedMesh(j);
            double volume = DeformableMesh3DTools.calculateVolume(new double[]{0, 0, 1}, mesh.positions, mesh.triangles);
            volume = volume*stack.SCALE * stack.SCALE*stack.SCALE;
            double are = DeformableMesh3DTools.calculateSurfaceArea(mesh);
            are = are * stack.SCALE*stack.SCALE;

            double[] centroid = DeformableMesh3DTools.centerAndRadius(mesh.nodes);
            double[] minMax = DeformableMesh3DTools.findMinMax(mesh.nodes, centroid);
            for(int i = 0; i<centroid.length; i++){
                centroid[i] = centroid[i]*stack.SCALE;
            }
            BinaryMomentsOfInertia bmi = new BinaryMomentsOfInertia(mesh, stack);
            double intensity = bmi.measureAverageIntensity();
            List<double[]> ev = bmi.getEigenVectors();
            double[] eigenValues = ev.get(3);

            minMax[0] = minMax[0]*stack.SCALE;
            minMax[1] = minMax[1]*stack.SCALE;

            builder.append(String.format(Locale.US, "%d\t", j+1));
            builder.append(String.format(Locale.US, "%f\t", volume));
            builder.append(String.format(Locale.US, "%f\t", are));
            builder.append(String.format(Locale.US, "%f\t", intensity));
            builder.append(String.format(Locale.US, "%f\t%f\t%f\t", centroid[0], centroid[1], centroid[2]));
            builder.append(String.format(Locale.US, "%f\t%f\t%f\t", centroid[3], minMax[1], minMax[0]));
            builder.append(String.format(Locale.US, "%f\t%f\t%f\t", eigenValues[0], eigenValues[1], eigenValues[2]));

            for(int i = 0; i<3; i++){
                double[] v = ev.get(i);
                builder.append(String.format(Locale.US, "%f\t%f\t%f\t", v[0], v[1], v[2]));
            }
            builder.append("\n");
        }
        GuiTools.createTextOuputPane(builder.toString());

    }

    public boolean hasNextFrame() {
        //internally 0 indexed compared to ij and display, which is 1 based.
        return original_plus.getNFrames()-1>getCurrentFrame();

    }

    /**
     * Creates a text window with the current furrow values scaled to the image units.
     *
     */
    public void showFurrowValues() {

       double scale = stack.SCALE;
       StringBuilder builds = new StringBuilder("#position and normal using the image units.\n");
       builds.append("#frame\tx(unit)\ty(unit)\tz(unit)nx\tny\tnz\n");

       for(int i = 0; i<original_plus.getNFrames(); i++){
           Furrow3D f = ringController.getFurrow(i);

           if(f==null){continue;}

           builds.append(String.format(Locale.US,
                   "%d\t%f\t%f\t%f\t%f\t%f\t%f\n", (i+1),
                   f.cm[0]*scale, f.cm[1]*scale, f.cm[2]*scale,
                   f.normal[0], f.normal[1], f.normal[2]
           ));

       }

       GuiTools.createTextOuputPane(builds.toString());

    }

    public void exportAsPly(File f) throws IOException {
        MeshWriter.exportToPly(f, tracker.getAllMeshTracks(), getCurrentFrame(), stack.offsets, 1.0);
    }

    public File getLastSavedFile() {
        return lastSavedFile;
    }

    public void setLastSavedFile(File lastSavedFile) {
        this.lastSavedFile = lastSavedFile;
    }

    public boolean isHardBoundaries() {
        return hardBoundaries;
    }

    public void setHardBoundaries(boolean hardBoundaries) {
        this.hardBoundaries = hardBoundaries;
    }


    public void calculateInterfaceLineScan(Track track){
        List<Track> tracks = getAllTracks();
        calculateInterfaceLineScan(tracks.indexOf(track), tracks);
    }

    public void calculateInterfaceLineScan(int index, List<Track> tracks){
        Track target = tracks.get(index);
        Map<Integer, Map<Integer, double[]>> values = new TreeMap<>();

        for(int j = 0; j<tracks.size(); j++){
            values.put(j, new TreeMap<>());
        }
        for(Integer key: target.getTrack().keySet()){
            DeformableMesh3D m1 = target.getMesh(key);
            double s0 = DeformableMesh3DTools.calculateSurfaceArea(m1);
            double v0 = DeformableMesh3DTools.calculateVolume(Vector3DOps.zhat, m1.positions, m1.triangles);
            double c0 = CurvatureCalculator.calculateAverageCurvature(m1);

            IntensitySurfacePlot isp = new IntensitySurfacePlot(m1, stack);
            double i_ave = isp.getAverageIntensityAtNodes();
            values.get(index).put(key, new double[]{key, s0, v0, c0, 0, i_ave});

            for(int j = 0; j<tracks.size(); j++){
                if(index==j) continue;
                Track neighbor = tracks.get(j);


                if(neighbor.containsKey(key)){

                    DeformableMesh3D m2 = neighbor.getMesh(key);
                    MeshFaceObscuring mfo = new MeshFaceObscuring();
                    mfo.setNeighbor(m2);
                    Set<Triangle3D> triangles = mfo.getOverlapArea(m1);
                    DeformableMesh3D sharedFaces = createSubMesh(m1, triangles);

                    double c = CurvatureCalculator.calculateAverageCurvature(sharedFaces);
                    double s1 = DeformableMesh3DTools.calculateSurfaceArea(sharedFaces);
                    isp = new IntensitySurfacePlot(sharedFaces, stack);
                    double i_edge = isp.getAverageIntensityAtBoundary();
                    double i_all = isp.getAverageIntensityAtNodes();

                    values.get(j).put(key, new double[]{key, s1, 0, c, i_edge, i_all});
                }
            }
        }
        String[] labels = {"frame", "surface area", "volume", "curvature", "edge intensity", "average intensity"};
        int nGraphs = labels.length-1;
        String name= target.getName();
        for(int j = 0; j<nGraphs; j++){
            Graph g = new Graph();
            for(Integer key: values.keySet()){
                Map<Integer, double[]> points = values.get(key);
                if(points.size()==0){
                    continue;
                }
                double[] x = new double[points.size()];
                double[] y = new double[points.size()];
                int k = 0;
                for(Integer frame: points.keySet()){
                    x[k] = points.get(frame)[0];
                    y[k] = points.get(frame)[j+1];
                    k++;
                }
                DataSet set = g.addData(x,y);
                set.setLabel(tracks.get(key).getName());

            }
            g.setTitle("Epoc vs " + labels[j+1] + " for track: " + target.getName());

            g.show(false, "Epoc vs " + labels[j+1] + " for track: " + target.getName());
        }

    }

    public void calculateInterfaceLineScans() {

        List<Track> tracks = getAllTracks();

        for(int i = 0; i<tracks.size(); i++){
            calculateInterfaceLineScan(i, tracks);
        }
    }

    public DeformableMesh3D createSubMesh(DeformableMesh3D mesh, Set<Triangle3D> triangles){

        int[] triIndexes = new int[triangles.size() * 3];
        int[] conIndexes = new int[triangles.size() * 3 * 2];
        int dex = 0;
        for (Triangle3D tri : triangles) {
            int[] indices = tri.getIndices();
            triIndexes[dex] = indices[0];
            triIndexes[dex + 1] = indices[1];
            triIndexes[dex + 2] = indices[2];
            conIndexes[dex * 2] = indices[0];
            conIndexes[dex * 2 + 1] = indices[1];
            conIndexes[dex * 2 + 2] = indices[1];
            conIndexes[dex * 2 + 3] = indices[2];
            conIndexes[dex * 2 + 4] = indices[2];
            conIndexes[dex * 2 + 5] = indices[0];

            dex += 3;
        }
        return new DeformableMesh3D(mesh.positions, conIndexes, triIndexes);
    }

    public void removeMeshTrack(Track track) {
        if(track.isEmpty()){
            tracker.removeTrack(track);
        }
        else{
            List<Integer> frames = new ArrayList<>(track.getTrack().keySet());
            for(Integer frame: track.getTrack().keySet()){
                removeMeshFromTrack(frame, track.getMesh(frame), track);
            }
        }
    }

    public void addMeshTrack(Track track){
        tracker.addTrack(track);
    }


    public void removeFrameListener(FrameListener listener) {
        frameListeners.remove(listener);
    }
}






