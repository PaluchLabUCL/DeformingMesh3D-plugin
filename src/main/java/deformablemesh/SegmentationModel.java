package deformablemesh;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.GradientEnergy;
import deformablemesh.externalenergies.ImageEnergyType;
import deformablemesh.externalenergies.IntensityEnergy;
import deformablemesh.externalenergies.PerpendicularGradientEnergy;
import deformablemesh.externalenergies.PerpendicularIntensityEnergy;
import deformablemesh.externalenergies.PressureForce;
import deformablemesh.externalenergies.TriangleAreaDistributor;
import deformablemesh.geometry.BinaryMomentsOfInertia;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.SnakeBox;
import deformablemesh.gui.FrameListener;
import deformablemesh.gui.GuiTools;
import deformablemesh.gui.RingController;
import deformablemesh.io.FurrowWriter;
import deformablemesh.io.MeshWriter;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.MeshTracker;
import deformablemesh.track.Track;
import deformablemesh.util.MeshAnalysis;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import snakeprogram3d.MultipleSnakesStore;
import snakeprogram3d.Snake;
import snakeprogram3d.SnakeIO;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 *  Anything public that involves modifying synchronized data is automatically submitted to the
 *  main event loop. Getters that return volatile data will return the data at the current state,
 *  not at the state the data will be in when execution has completed. Attempting to change volatile
 *  data will be placed in the main event loop and updated after all other tasks have completed updating.
 *
 * User: msmith
 * Date: 7/16/13
 * Time: 11:06 AM
 */
public class SegmentationModel {
    double GAMMA = 500.0;
    double ALPHA = 1.0;
    double BETA = 0.0;
    double pressure = 0.0;
    private double cortex_thickness = 0.3;

    boolean reshape = true;
    volatile boolean stop = false;

    ImagePlus original_plus;
    ImageStack original_stack;

    MeshImageStack stack;
    MeshTracker tracker;

    List<FrameListener> frameListeners = new ArrayList<>();
    List<FrameListener>  meshListeners = new ArrayList<>();

    private double image_weight;
    private int divisions = 2;
    public SnakeBox snakeBox;
    private RingController ringController;

    public ImageEnergyType energyType;

    Color backgroundColor = Color.WHITE;
    Color volumeColor = Color.BLUE;


    private double normalize;

    public SegmentationModel(){

        tracker = new MeshTracker();
        stack = new MeshImageStack();
        snakeBox = new SnakeBox();
        snakeBox.setScale(stack);
        frameListeners.add(i->syncOriginalStack());
        frameListeners.add(snakeBox);

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
        long begin = System.currentTimeMillis();
        int c = 0;
        if(count<0){
            count = Integer.MAX_VALUE;
        }
        while(!stop&&c<count){
            selectedMesh.update();
            c++;
        }
    }

    /**
     * A generic method for deforming all of the meshes provided.
     * @param meshes
     */
    public void deformMeshes(List<DeformableMesh3D> meshes){

        stop = false;

        //apply energies
        for(DeformableMesh3D mesh: meshes){
            mesh.clearEnergies();

            //mesh.PRESSURE = pressure;
            ExternalEnergy erg = generateImageEnergy();
            mesh.addExternalEnergy(erg);

            if(pressure!=0){
                mesh.addExternalEnergy(new PressureForce(mesh, pressure));
            }

            if(normalize!=0){
                mesh.addExternalEnergy(new TriangleAreaDistributor(stack, mesh, normalize));

            }

            snakeBox.addRingEnergy(stack.CURRENT, mesh);

            mesh.ALPHA=ALPHA;
            mesh.GAMMA=GAMMA;
            mesh.BETA=BETA;
            mesh.reshape();
        }

        while(!stop){
            for(DeformableMesh3D mesh: meshes){

                mesh.update();
                if(stop){
                    break;
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


    public void saveCurvesAsSnakes(File f){
        Map<Integer, List<List<double[]>>> curves = ringController.getCurves();
        MultipleSnakesStore store = new MultipleSnakesStore();
        for(Integer i: curves.keySet()){
            for(List<double[]> curve: curves.get(i)){
                Snake snake = new Snake();
                snake.addCoordinates(
                    i, curve.stream().map(
                    point->{
                            double[] p = stack.getImageCoordinates(point);

                            //snakes also don't account for the scale factor.
                            p[2] *= getZToYScale();

                            //snakes have an offset.
                            p[2] -= stack.offsets[2];
                            return p;
                        }
                    ).collect(
                        Collectors.toList()
                    )
                );
                store.addSnake(snake);
            }
        }
        try {
            SnakeIO.writeSnakes(new HashMap<>(), store, f.getAbsoluteFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadCurvesFromSnakes(File f) throws Exception {
        Map<Integer, List<List<double[]>>> target = new TreeMap<>();
        MultipleSnakesStore snakes = SnakeIO.loadSnakes(f.getAbsolutePath());
        for(Snake snake: snakes){
            for(Integer i: snake){
                if(!target.containsKey(i)){
                    target.put(i, new ArrayList<>());
                }
                target.get(i).add(
                    snake.getCoordinates(i).stream().map(pt->{
                        pt[2] += stack.offsets[2];
                        pt[2] /=getZToYScale();
                        return stack.getNormalizedCoordinate(pt);
                    }

                    ).collect(
                        Collectors.toList()
                    )
                );
            }
        }
        ringController.loadCurves(target);
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
        stack = new MeshImageStack(original_plus);
        snakeBox.setScale(stack);
        syncOriginalStack();
        notifyFrameListeners();

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
        if(i>=0&&i<stack.FRAMES&&i != stack.CURRENT) {
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


    private void syncOriginalStack(){
        if(stack==null) return;

        int start = stack.CURRENT*stack.SLICES + 1;
        original_stack = new ImageStack(original_plus.getWidth(),original_plus.getHeight());
        ImageStack bs = original_plus.getStack();
        for(int i = 0; i<stack.SLICES; i++){
            original_stack.addSlice(bs.getSliceLabel(start+i), bs.getProcessor(start + i));
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

    public void saveMeshes(final File f) throws IOException {
        MeshWriter.saveMeshes(f, tracker);
    }

    public void exportAsStl(File f) throws IOException {
        if(f==null){
            return;
        }
        MeshWriter.saveStlMesh(f, tracker.getSelectedMesh(getCurrentFrame()), stack.offsets, stack.SCALE);
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

    public ExternalEnergy generateImageEnergy(){
        ExternalEnergy erg;
        switch(energyType){
            case Intensity:
                erg = new IntensityEnergy(stack, getImageWeight());
                break;
            case Gradient:
                erg = new GradientEnergy(stack, getImageWeight());
                break;
            case PerpendicularIntensity:
                erg = new PerpendicularIntensityEnergy(stack, getSelectedMesh(getCurrentFrame()), getImageWeight());
                break;
            case PerpendicularGradient:
                erg = new PerpendicularGradientEnergy(stack, getSelectedMesh(getCurrentFrame()), getImageWeight());
                break;
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

        ExternalEnergy erg = generateImageEnergy();
        selectedMesh.addExternalEnergy(erg);

        if(pressure!=0){
            selectedMesh.addExternalEnergy(new PressureForce(selectedMesh, pressure));
        }

        if(normalize!=0){
            selectedMesh.addExternalEnergy(new TriangleAreaDistributor(stack, selectedMesh, normalize));

        }

        snakeBox.addRingEnergy(stack.CURRENT, selectedMesh);

    }

    public double getCurveWeight() {

        return snakeBox.getCurveWeight();
    }

    public void setCurveWeight(double v){
        snakeBox.setCurveWeight(v);
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

    public List<Track> getAllMeshes() {
        return new ArrayList<>(tracker.getAllMeshTracks());
    }



    public void setMeshes(List<Track> meshes) {
        tracker.clearMeshes();
        tracker.addMeshTracks(meshes);
        notifyMeshListeners();
    }



    Pattern p = Pattern.compile("(\\.\\w+)$");
    public String getShortImageName() {
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


    public List<Track> getAllTracks() {
        return tracker.getAllMeshTracks();
    }

    public void addFrameListener(FrameListener listener) {
        frameListeners.add(listener);
    }

    public void addMeshListener(FrameListener listener){
        meshListeners.add(listener);
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
        builder.append("#\n");
        builder.append("#Frame\tVolume\tArea\tc_x\tc_y\tc_z\tdmean\tdmax\tdmin\t");
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
            List<double[]> ev = bmi.getEigenVectors();
            double[] eigenValues = ev.get(3);

            minMax[0] = minMax[0]*stack.SCALE;
            minMax[1] = minMax[1]*stack.SCALE;

            builder.append(String.format(Locale.US, "%d\t", j+1));
            builder.append(String.format(Locale.US, "%f\t", volume));
            builder.append(String.format(Locale.US, "%f\t", are));
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

        return original_plus.getNFrames()>getCurrentFrame();

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
        MeshWriter.exportToPly(f, tracker.getAllMeshTracks(), getCurrentFrame(), stack.offsets, stack.SCALE);
    }
}






