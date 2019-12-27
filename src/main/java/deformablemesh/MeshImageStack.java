package deformablemesh;

import deformablemesh.geometry.Box3D;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.process.ImageProcessor;

import static deformablemesh.geometry.DeformableMesh3D.ORIGIN;

/**
 *
 * This will setup normalized coordinates for an image.
 *
 * User: msmith
 * Date: 7/3/13
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class MeshImageStack {
    public double[][][] data;

    public double SCALE;
    public double[] scale_values;
    public double[] offsets;
    public double[] pixel_dimensions;
    int[] max_dex;
    ImagePlus original;

    public int CURRENT = 0;
    int FRAMES,SLICES;

    public double MIN_VALUE;
    public double MAX_VALUE;
    final private double PX;
    public MeshImageStack(){
        SCALE=1;
        scale_values=new double[]{1,1,1};
        offsets=new double[]{0,0,0};
        pixel_dimensions=new double[]{1,1,1};
        PX=1;
        data = new double[1][1][1];
        max_dex = new int[3];
    }

    public MeshImageStack(ImagePlus original){
        this.original=original;
        SLICES = original.getNSlices();
        FRAMES = original.getNFrames();
        CURRENT=0;
        int py = original.getHeight();
        int px = original.getWidth();

        data = new double[SLICES][py][px];

        max_dex = new int[]{px-1, py-1, SLICES-1};

        copyValues();

        FileInfo info = original.getFileInfo();

        double[] dim3d = new double[]{
            info.pixelWidth*px,
            info.pixelHeight*py,
            info.pixelDepth*SLICES
        };


        pixel_dimensions = new double[]{
            info.pixelWidth,
            info.pixelHeight,
            info.pixelDepth
        };

        SCALE = dim3d[0];

        for(int i = 1; i<=2; i++){
            if(dim3d[i]>SCALE){
                SCALE = dim3d[i];
            }
        }

        scale_values = new double[]{
                1/info.pixelWidth,
                1/info.pixelHeight,
                1/info.pixelDepth
        };

        offsets = new double[3];

        for(int i = 0; i<3; i++){
            offsets[i] = 0.5*dim3d[i]/SCALE;
        }

        //px should be the smallest pixel in normalized coordinates.
        double[] nPx = scaleToNormalizedLength(new double[]{1,1,1});
        PX = nPx[0] < nPx[1] ?
                nPx[0] < nPx[2] ? nPx[0] : nPx[2] :
                nPx[1] < nPx[2] ? nPx[1] : nPx[2];

        copyValues();
    }

    public int getNFrames(){
        return FRAMES;
    }

    public int getNSlices(){
        return SLICES;
    }

    public int getWidthPx(){
        if(original==null){
            return 0;
        }
        return original.getWidth();
    }

    public int getHeightPx(){
        if(original==null){
            return 0;
        }
        return original.getHeight();
    }

    /**
     * given a length in px, px, slices this scales the length to normalized
     * coordinate values.
     * @param l px, px, slices (x,y,z lengths in image slices)
     * @return scaled value.
     */
    public double[] scaleToNormalizedLength(double[] l){

        return new double[]{
                l[0]*pixel_dimensions[0]/SCALE,
                l[1]*pixel_dimensions[1]/SCALE,
                l[2]*pixel_dimensions[2]/SCALE
        };
    }

    /**
     * Finds the position in normalized coordinates using.
     *
     * @param r location in px,px,slice
     * @return normalized coordinates where center of image is at 0 and the longest axis in real units is -0.5 0.5.
     */
    public double[] getNormalizedCoordinate(double[] r){
        double[] ret = new double[3];


        for(int i = 0; i<3; i++){
            ret[i] = r[i]*pixel_dimensions[i]/SCALE - offsets[i];
        }

        return ret;
    }

    public void nextFrame(){
        if(CURRENT+1<FRAMES){
            CURRENT+=1;
            copyValues();
        }
    }

    public void previousFrame(){
        if(CURRENT>0){
            CURRENT--;
            copyValues();
        }
    }

    public void setFrame(int i){
        if(i!=CURRENT&&i<FRAMES&&i>=0){
            CURRENT=i;
            copyValues();
        }
    }

    /**
     * Copies the image data from the image stack to the double[][] backing the
     * image data that is used for obtaining values.
     *
     */
    public void copyValues(){
        int slices = original.getNSlices();
        int py = original.getHeight();
        int px = original.getWidth();

        MIN_VALUE=Double.MAX_VALUE;
        MAX_VALUE=-MIN_VALUE;

        for(int i = 0;i<slices; i++){
            ImageProcessor proc = original.getStack().getProcessor(i+CURRENT*SLICES + 1);
            for(int j = 0; j<py; j++){
                for(int k = 0; k<px; k++){
                    double v = proc.getPixelValue(k,j);
                    if(v<MIN_VALUE) MIN_VALUE=v;
                    else if(v>MAX_VALUE) MAX_VALUE=v;
                    data[i][j][k] = v;
                }
            }
        }

    }

    /**
     * For creating a backing double array of another ImagePlus that uses the same geometry as the original image
     * plus.
     *
     * @param other
     */
    public void copyValues(ImagePlus other){

        int slices = original.getNSlices();
        int py = original.getHeight();
        int px = original.getWidth();

        //MIN_VALUE=Double.MAX_VALUE;
        //MAX_VALUE=-MIN_VALUE;

        for(int i = 0;i<slices; i++){
            ImageProcessor proc = other.getStack().getProcessor(i+CURRENT*SLICES + 1);
            for(int j = 0; j<py; j++){
                for(int k = 0; k<px; k++){
                    double v = proc.getPixelValue(k,j);
                    if(v<MIN_VALUE) MIN_VALUE=v;
                    else if(v>MAX_VALUE) MAX_VALUE=v;
                    data[i][j][k] = proc.getPixelValue(k,j);
                }
            }
        }

    }

    public double getInterpolatedValue(double x, double y, double z){
        return getInterpolatedValue(new double[]{x,y,z});
    }

    final static double min_interp_value=1e-4;
    public double getInterpolatedValue(double[] xyz){
        double[] ndex = new double[3];
        int[] base = new int[3];
        double[] f = new double[3];
        for(int i = 0; i<3; i++){
            ndex[i] = SCALE*(xyz[i] + offsets[i])*scale_values[i];
            base[i] = (int)ndex[i];

            //outside of image is the same as the edge value.
            if(base[i]<0){
                base[i] = 0;
            } else if(base[i]>max_dex[i]){
                base[i] = max_dex[i];
            }
            /*
            outside of image is zero.
            if(base[i]<0||base[i]>max_dex[i]){
                //out of range
                return 0;
            }
            */


            f[i] = base[i]==max_dex[i]?0:ndex[i] - base[i];
        }

        double a = getValue(base[0],base[1], base[2]);

        
        if(f[0]>min_interp_value){
            double b = getValue(base[0]+1, base[1], base[2]);
            a = a + (b-a)*f[0];
        }

        if(f[1]>min_interp_value){
            double c = getValue(base[0],base[1]+1, base[2]);

            if(f[0]>min_interp_value){
                double d = getValue(base[0]+1, base[1]+1, base[2]);
                c = c + (d-c)*f[0];
            }
            a = a + (c-a)*f[1]; //first plane.
        }

        double v = a;
        if(f[2]>min_interp_value){

            a = getValue(base[0],base[1], base[2]+1);

            if(f[0]>min_interp_value){
                double b = getValue(base[0]+1, base[1], base[2]+1);
                a = a + (b-a)*f[0];
            }

            if(f[1]>min_interp_value){
                double c = getValue(base[0],base[1]+1, base[2]+1);

                if(f[0]>min_interp_value){
                    double d = getValue(base[0]+1, base[1]+1, base[2]+1);
                    c = c + (d-c)*f[0];
                }
                a = a + (c-a)*f[1];
            }

            v = v + (a-v)*f[2];
        }

        return v;
    }

    /**
     * Gets the value at the image coordintes x,y,z doesn't perform any sort of check.
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    public double getValue(int x, int y, int z){
        return data[z][y][x];

    }

    public double[] getCenterOfMass(){
        double sum = 0;
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        double[] r = new double[3];
        for(int i = 0; i<data[0][0].length; i++){
            for(int j = 0; j<data[0].length; j++){
                for(int k = 0; k<data.length; k++){
                    r[0] = i;
                    r[1] = j;
                    r[2] = k;
                    double v = getValue(i,j,k);
                    double[] nr = getNormalizedCoordinate(r);
                    sum += v;
                    sumx += nr[0]*v;
                    sumy += nr[1]*v;
                    sumz += nr[2]*v;

                }
            }
        }
        return new double[]{sumx/sum, sumy/sum, sumz/sum};
    }




    public double[] getImageCoordinates(double[] r) {
        double[] ret = new double[3];


        for(int i = 0; i<3; i++){
            ret[i] = (r[i] + offsets[i])*SCALE/pixel_dimensions[i];
        }

        return ret;
    }

    /**
     * Smallest pixel, in the x,y, or z direction from the input image.
     *
     * @return
     */
    public double getMinPx(){
        return PX;
    }

    public Box3D getLimits() {
        return new Box3D(ORIGIN, offsets[0]*2, offsets[1]*2, offsets[2]*2);
    }

    public static MeshImageStack getEmptyStack() {
        return  new MeshImageStack();
    }

    public MeshImageSubStack createSubStack(Box3D box){
        ImagePlus plus = samplePlus(box);
        return new MeshImageSubStack(box, this, plus);
    }

    public ImagePlus samplePlus(Box3D box){
        ImagePlus sample = original.createImagePlus();
        //ret[i] = (r[i] + offsets[i])*SCALE/pixel_dimensions[i];
        int w = (int)((box.high[0] - box.low[0])*SCALE/pixel_dimensions[0]);
        int h = (int)((box.high[1] - box.low[1])*SCALE/pixel_dimensions[1]);
        int d = (int)((box.high[2] - box.low[2])*SCALE/pixel_dimensions[2]);
        double[] xyz1 = getImageCoordinates(box.low);
        double[] xyz2 = getImageCoordinates(box.high);

        ImageStack stack = new ImageStack(w, h);
        ImageStack ori = original.getImageStack();

        for(int i = (int)xyz1[2]; i<=xyz2[2]; i++){
            ImageProcessor proc = ori.getProcessor(i);
            ImageProcessor nproc = proc.createProcessor(w, h);
            for(int j = 0; j<w; j++){
                for(int k = 0; k<h; k++){
                    nproc.set(j,k, proc.get((int)xyz1[0] + j, (int)xyz1[1] + k));
                }
            }
            stack.addSlice(nproc);
        }

        sample.setStack(stack, 1, stack.getSize(), 1);


        return sample;
    }

    public double[] getIntensityValues() {
        double[] n = new double[data.length*data[0].length*data[0][0].length];
        final int row = data[0][0].length;
        final int frame = data[0].length*row;

        for(int slice = 0; slice<data.length; slice++){
            for(int line = 0; line<data[0].length; line++){
                System.arraycopy(data[slice][line], 0, n,slice*frame + line*row,  row);
            }
        }
        return n;
    }
}

