package deformablemesh.ringdetection;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.gui.GuiTools;
import deformablemesh.util.ConnectedComponents2D;
import deformablemesh.util.astar.AStarBasic;
import deformablemesh.util.astar.AStarXY;
import deformablemesh.util.astar.PossiblePath;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.TwoDContourDeformation;
import snakeprogram.energies.ImageEnergy;
import snakeprogram.energies.IntensityEnergy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * This will be used to detect a
 *
 * Created by msmith on 1/20/14.
 */
public class ContractileRingDetector implements Iterable<Integer>{
    MeshImageStack stack;
    ImageProcessor currentSlice, currentBinary;

    int frames;
    int slices;
    int height;
    int width;

    public double ALPHA = 1.0;
    public double BETA = 1.0;
    public double GAMMA = 1000;
    public double WEIGHT = 2.0;
    public double SIGMA = 3.0;

    //Stage stage;

    double threshold;
    Map<Integer, Furrow3D> furrows;
    Furrow3D furrow;

    public ContractileRingDetector(){
        furrows = new TreeMap<>();
    }
    public void setImageStack(MeshImageStack stack){
        this.stack = stack;
        frames = stack.getNFrames();
        slices = stack.getNSlices();
        height = stack.getHeightPx();
        width = stack.getWidthPx();

    }


    public void setThresh(double t){
        threshold=t;
    }

    ImageStack test_stack;

    public void createFurrowSlice(int frame){
        if(!furrows.containsKey(frame)) return;
        Furrow3D f = furrows.get(frame);
        FurrowTransformer transformer = new FurrowTransformer(f, stack);

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
        currentSlice=proc;

    }
    public ImageProcessor getFurrowSlice(){

        return currentSlice;
    }

    public ImageProcessor createBinarySlice(){
        if(currentSlice==null) return null;
        ImageProcessor proc = currentSlice;

        ImageProcessor binary = new ShortProcessor(proc.getWidth(), proc.getHeight());

        double[] center;
        double v;
        center = threshAndCenter(proc, binary);
        //pruneBinaryBlobs(binary);
        currentBinary = binary;
        return binary;
    }

    public List<double[]> detectFrame(int frame){
        //showSliced(frame);
        setFrame(frame);

        //create a 2d slice of plane

        double[] center;
        double v;
        do {
            center = threshAndCenter(currentSlice, currentBinary);
            v = currentBinary.get((int)center[0], (int)center[1]);
            if(v>0){
                v=0;
            }
        } while(v!=0);


        pruneBinaryBlobs(currentBinary);

        List<int[]> starts = getInitialPoints(currentBinary, center);
        ImageProcessor dup = currentBinary.duplicate();
        for(int i = 0; i<dup.getWidth()*dup.getHeight(); i++){
            if(dup.getf(i)==0){
                dup.setf(i, 255);
            } else {
                dup.setf(i, 0);
            }
        }
        if(starts.size()!=4){
            //"couldn't find all points."
        }

        List<int[]> path = generateInitialCurve(starts, dup, currentSlice);


        List<double[]> refined = refineCurve(currentSlice, path);


        return refined;
    }

    List<double[]> refineCurve(ImageProcessor proc, List<int[]> points){
        List<double[]> ret = new ArrayList<>();

        points.stream().forEach((w)->ret.add(new double[]{w[0], w[1]}));

        ImageEnergy gradient = new IntensityEnergy(proc, SIGMA);
        TwoDContourDeformation deformation = new TwoDContourDeformation(ret, gradient);
        deformation.setAlpha(ALPHA);
        deformation.setBeta(BETA);
        deformation.setGamma(GAMMA);
        deformation.setWeight(WEIGHT);
        deformation.initializeMatrix();
        for(int k = 0; k<10; k++){
            deformation.deformSnake();

            try{
                deformation.addSnakePoints(1);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        return ret;
    }

    /**
     * Generates an a-star based path for transversing the image.
     *
     * @param starts
     * @param binary
     * @param proc
     * @return
     */
    List<int[]> generateInitialCurve(List<int[]> starts, ImageProcessor binary, ImageProcessor proc){
        List<int[]> path = new ArrayList<>();



        for(int i = 0; i<starts.size(); i++){
            int[] a = starts.get(i);
            int[] b = starts.get((i+1)%4);

            AStarBasic<int[]> asb = AStarXY.createXYAStar(binary, a, b);
            asb.setGoal(b);
            PossiblePath<int[]> connected = asb.findPath(AStarXY.createXYPossiblePath(a));
            path.addAll(connected.getPath());
        }
        return path;
    }


    /**
     * Using the 'center' and trying to find four points
     * @param binary
     * @param center
     * @return
     */
    List<int[]> getInitialPoints(ImageProcessor binary, double[] center){
        int[] a;
        int h = binary.getHeight();
        int w = binary.getWidth();

        ArrayList<int[]> guesses = new ArrayList<>();

        //left
        a = new int[]{(int) center[0], (int) center[1]};
        int[] current = {a[0], a[1]};

        boolean searching = true;
        while(searching) {
            current[0] = current[0] - 1;
            if(current[0]<0){
                break;
            }
            int v = binary.get(current[0], current[1]);
            if (v > 0) {
                a[0] = current[0];
                a[1] = current[1];
                guesses.add(a);
                searching = false;
            }
        }

        //top
        a = new int[]{(int) center[0], (int) center[1]};
        current = new int[]{a[0], a[1]};

        searching = true;
        while(searching) {
            current[1] = current[1] + 1;
            if(current[1]>=h){
                break;
            }
            int v = binary.get(current[0], current[1]);
            if (v > 0) {
                a[0] = current[0];
                a[1] = current[1];
                searching = false;
                guesses.add(a);
            }
        }

        //right
        a = new int[]{(int) center[0], (int) center[1]};
        current = new int[]{a[0], a[1]};

        searching = true;
        while(searching) {
            current[0] = current[0] + 1;
            if(current[0]>=w){
                break;
            }
            int v = binary.get(current[0], current[1]);
            if (v > 0) {
                a[0] = current[0];
                a[1] = current[1];
                searching = false;
                guesses.add(a);
            }
        }


        //bottom
        a = new int[]{(int) center[0], (int) center[1]};
        current = new int[]{a[0], a[1]};

        searching = true;
        while(searching) {
            current[1] = current[1] - 1;
            if(current[1]<0){
                break;
            }
            int v = binary.get(current[0], current[1]);
            if (v > 0) {
                a[0] = current[0];
                a[1] = current[1];
                searching = false;
                guesses.add(a);

            }
        }

        return guesses;
    }

    /**
     * Finds the connected components and reduces the image to only one connected component.
     * @param binary
     */
    void pruneBinaryBlobs(ImageProcessor binary){
        ConnectedComponents2D components = new ConnectedComponents2D();

        ArrayList<double[]> centroids = components.getCentroids(binary);
        HashMap<Integer, ArrayList<int[]>> blobs = components.getPoints();
        int max = 0;
        int min = binary.getHeight()*binary.getWidth();
        int biggest = 0;
        for(Map.Entry<Integer, ArrayList<int[]>> row: blobs.entrySet()){
            ArrayList<int[]> pts = row.getValue();
            if(pts.size()>max){
                max = pts.size();
                biggest = row.getKey();
            }
            if(pts.size()<min){
                min = pts.size();
            }

        }

        for(Map.Entry<Integer, ArrayList<int[]>> row: blobs.entrySet()){
            if(row.getKey()!=biggest){
                for(int[] pt: row.getValue()){
                    binary.set(pt[0], pt[1],0);
                }
            }

        }
    }

    public List<List<double[]>> mapTo2D(List<List<double[]>> input){

        List<List<double[]>> ret = new ArrayList<>();

        if(furrow!=null) {
            FurrowTransformer transformer = new FurrowTransformer(furrow, stack);
            for (List<double[]> curve3d : input) {
                ret.add(curve3d.stream().map(i -> transformer.getPlaneCoordinates(i)).collect(Collectors.toList()));
            }
        }
        return ret;
    }

    /**
     * Apply the current threshold to the image and find the center of mass of the successful points.
     *
     * @param input input intensity values
     * @param output where the output should be placed.
     * @return {x, y } positions
     */
    double[] threshAndCenter(ImageProcessor input, ImageProcessor output){
        int w = input.getWidth();
        int h = input.getHeight();
        int n = w*h;
        double count = 0;
        double[] center = {0,0};
        for(int i = 0; i<n; i++){
            float f = input.getf(i);
            if(f>threshold){
                output.set(i, 255);
                center[0] += i%w;
                center[1] += i/w;
                count++;
            } else{
                output.set(i, 0);
            }
        }

        if(count>0) {

            center[0] = center[0] / count;
            center[1] = center[1] / count;
        }

        return center;


    }



    public Iterator<Integer> iterator(){
        return furrows.keySet().iterator();
    }

    public Furrow3D getFurrow(int i){
        return furrows.get(i);
    }

    public Furrow3D getFurrow(){
        return furrow;
    }

    public void setFrame(int frame){
        if(furrows.containsKey(frame)){
            furrow = furrows.get(frame);
            createFurrowSlice(frame);
        } else{
            furrow = null;
        }

    }

    public void putFurrow(int frame, Furrow3D furrow){
        furrows.put(frame, furrow);
        this.furrow = furrow;
    }

    public void jiggleFurrow(int frame, DeformableMesh3D mesh){
        {
            if(!furrows.keySet().contains(frame)){

                Furrow3D fresh = new Furrow3D( new double[]{0, 0, 0}, new double[]{-1.0, 0, 0} );
                fresh.create3DObject();
                furrows.put(frame, fresh);
                furrow = fresh;
            }
            boolean jiggle = true;
            double[] df = new double[]{0.01,0.0,0};
            double r = furrow.calculateRadius(mesh.getConnections());
            double rf = 0;
            double[] original = new double[]{furrow.cm[0], furrow.cm[1], furrow.cm[2]};
            while(jiggle){
                furrow.move(df);
                rf = furrow.calculateRadius(mesh.getConnections());

                if(rf<0) jiggle=false;
                if(rf>=r){
                    if(df[0]>0){
                        for(int i = 0; i<3; i++){
                            //we are finished undo last move and
                            df[i] = -df[i];
                        }
                        furrow.move(df);
                    } else{
                        for(int i = 0; i<3; i++){
                            //we are finished undo last move and
                            df[i] = -df[i];
                        }
                        furrow.move(df);
                        jiggle=false;
                    }
                } else{
                    r = rf;
                }
            }
            if(r<=0){
                //failed
                furrow.moveTo(original);
            }
        }
    }

    public void scanFurrow(int frame, DeformableMesh3D mesh){
        if(!furrows.containsKey(frame)){

            Furrow3D fresh = new Furrow3D(new double[]{0, 0, 0}, new double[]{-1.0, 0, 0});
            fresh.create3DObject();
            furrows.put(frame, fresh);
        }
        StringBuilder builder = new StringBuilder("");
        furrow.move(new double[]{-0.3,0,0});
        double[] dx = new double[]{0.6/500,0,0};
        for(int i = 0; i<500; i++){
            List<double[]> intersections = furrow.getIntersections(mesh.getConnections());
            double r = furrow.averageRadius(intersections);
            double[] ds = furrow.minimumRadiusLocation(intersections);
            furrow.move(ds);
            double nr = furrow.calculateRadius(mesh.getConnections());
            for(int j = 0;j<3;j++){
                ds[j] = -ds[j];
            }
            builder.append(String.format(Locale.US, "%f %f %f %f %f %f\n",-0.3 + i*dx[0], furrow.cm[0],furrow.cm[1],furrow.cm[2],r,nr));
            furrow.move(ds);
            furrow.move(dx);
        }
        GuiTools.createTextOuputPane(builder.toString());
    }

    public void rotateFurrow(int frame, DeformableMesh3D mesh){
        if(!furrows.containsKey(frame)){

            Furrow3D fresh = new Furrow3D(new double[]{0,-0.5,0}, new double[]{0,0.5,0});
            fresh.create3DObject();
            furrows.put(frame, fresh);
        }
        double old_r = furrow.calculateRadius(mesh.getConnections());

        boolean wobble = true;
        double dz = 0.01;

        while(wobble){

            furrow.rotateNormalZ(dz);
            double new_r = furrow.calculateRadius(mesh.getConnections());
            if(new_r>old_r){
                if(dz>0){
                    dz = -dz;
                    //undo last rotation.
                    furrow.rotateNormalZ(dz);
                } else{
                    //undo last rotation.
                    furrow.rotateNormalZ(-dz);
                    wobble = false;
                }
            }else{
                old_r = new_r;
            }
        }

        wobble = true;
        double dy = 0.01;

        while(wobble){

            furrow.rotateNormalY(dy);
            double new_r = furrow.calculateRadius(mesh.getConnections());
            if(new_r>old_r){
                if(dy>0){
                    dy = -dy;
                    //undo last rotation.
                    furrow.rotateNormalY(dy);
                } else{
                    //undo last rotation.
                    furrow.rotateNormalY(-dy);
                    wobble = false;
                }
            }else{
                old_r = new_r;
            }
        }
    }

    public void rotateScan(int frame, DeformableMesh3D mesh){
        if(!furrows.containsKey(frame)){

            Furrow3D fresh = new Furrow3D(new double[]{0,-0.5,0}, new double[]{0,0.5,0});
            fresh.create3DObject();
            furrows.put(frame, fresh);

        }
        StringBuilder builder = new StringBuilder("");
        double dtheta = 2*Math.PI/299;

        for(int i = 0; i<300; i++){
            double r = furrow.calculateRadius(mesh.getConnections());
            builder.append(String.format(Locale.US, "%f %f\n", i*dtheta, r));
            furrow.rotateNormalZ(dtheta);
        }

        GuiTools.createTextOuputPane(builder.toString());

    }

    public List<double[]> get3DCoordinatesFromFurrowPlane(List<double[]> pts){
        if(furrow==null) return Collections.<double[]>emptyList();
        FurrowTransformer transformer = new FurrowTransformer(furrow, stack);
        return pts.stream().map(transformer::getVolumeCoordinates).collect(Collectors.toList());
    }


    public Map<Integer, Furrow3D> getFurrows() {
        return furrows;
    }
}
