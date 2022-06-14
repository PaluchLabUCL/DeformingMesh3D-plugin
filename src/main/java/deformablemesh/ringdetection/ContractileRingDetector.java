package deformablemesh.ringdetection;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Furrow3D;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This used to be a way to initialize a contractile ring. It is becoming a tool for
 * reviewing segmentations.
 *
 * It stores a time series of planes.
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

    int frame;

    double threshold;
    Map<Integer, Furrow3D> furrows;
    private Furrow3D furrow;

    public ContractileRingDetector(){

        furrows = new TreeMap<>();
        stack = MeshImageStack.getEmptyStack();

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

    public void createFurrowSlice(int frame){
        Furrow3D f;
        if(furrows.containsKey(frame)){
            f = furrows.get(frame);
        } else {
            f = furrow;
        }
        if(f == null) return;
        FurrowTransformer transformer = new FurrowTransformer(f, stack);

        int xcounts = transformer.getXCounts();
        int ycounts = transformer.getYCounts();
        if(xcounts<1){xcounts =1;}
        if(ycounts<1){ycounts =1;}
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
        return furrows.get(frame);
    }

    public void setFrame(int frame){
        this.frame = frame;
        if(furrows.containsKey(frame)){
            furrow = furrows.get(frame);
            createFurrowSlice(frame);
        } else if(furrow != null) {
            createFurrowSlice(frame);
        }
    }

    public void putFurrow(int frame, Furrow3D furrow){
        furrows.put(frame, furrow);
        this.furrow = furrow;
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
