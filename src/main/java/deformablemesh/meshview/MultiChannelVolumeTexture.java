/*-
 * #%L
 * Triangulated surface for deforming in 3D.
 * %%
 * Copyright (C) 2013 - 2023 University College London
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package deformablemesh.meshview;

import org.scijava.java3d.ImageComponent;
import org.scijava.java3d.ImageComponent3D;
import org.scijava.java3d.Texture;
import org.scijava.java3d.Texture3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector4f;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.List;

public class MultiChannelVolumeTexture extends Texture3D{
    private List<double[][][]> textures = new ArrayList<>();

    //image indecies
    private int xDim,yDim,zDim;

    //float XDIM, YDIM, ZDIM;

    //WHITE:
    private List<Calibration> calibrations = new ArrayList<>();

    public double[] getMinMax(int channel) {

        Calibration c = calibrations.get(channel);
        return new double[] {c.requestedMin, c.requestedMax};

    }

    public double[] getAbsoluteMinMax(int channel){
        Calibration c = calibrations.get(channel);

        return new double[] {c.clampedMin, c.clampedMax};
    }

    public double[] getMaxRangeMinMax(int channel) {
        Calibration c = calibrations.get(channel);
        return new double[] {c.min, c.max};

    }


    static class Calibration{
        double clampedMin;
        double clampedMax;
        double requestedMin, requestedMax;
        double min;
        double max;
        boolean scaled = true;

        double clear = 0;
        double opaque = 5;

        Color3f color;

        /**
         * Sets the clamp min and max range, if the value is scaled then the range is set
         * relative to the min/max values if the range is not scaled then the clamp
         * values.
         *
         * is rmin and rmax.
         *
         * @param rmin
         * @param rmax
         */
        void setRange(double rmin, double rmax){
            requestedMin = rmin;
            requestedMax = rmax;
            if(scaled){
                clampedMin = min + (max - min)*rmin;
                clampedMax = min + (max - min)*rmax;
            } else{
                clampedMin = rmin;
                clampedMax = rmax;
            }
        }

        void setTransparencyRange( double clear, double opaque){
            this.clear = clear;
            this.opaque = opaque;
        }

        /**
         * Determines the transparency of the provided value by
         * @param scale
         * @return
         */
        public int alphaFromScale( double scale){

            if(scale < clear){
                return 0;
            } if(scale > opaque){
                return 255;
            }
            return (int)(255 * (scale - clear)/(opaque - clear));
        }
    }

    public boolean matchesShape(double[][][] data){
        return xDim == data.length && yDim == data[0].length && zDim == data[0][0].length;
    }

    /**
     * Creates a MultiChannelVolumeTexture with a single channel.
     *
     * @param double3d
     * @param cl_min
     * @param cl_max
     * @param c
     */
    public MultiChannelVolumeTexture(double[][][] double3d, double cl_min, double cl_max, Color3f c){
        super(Texture.BASE_LEVEL, Texture.RGBA, double3d.length, double3d[0].length, double3d[0][0].length);
        setCapability(ALLOW_IMAGE_WRITE);
        textures.add(double3d);

        this.xDim = double3d.length;
        this.yDim = double3d[0].length;
        this.zDim = double3d[0][0].length;


        Calibration cal = new Calibration();
        cal.color = c;
        calibrations.add(cal);

        findMinAndMaxValues(cal, double3d);
        cal.setRange(cl_min, cl_max);

        clamp();

        setEnable(true);
        setMinFilter(Texture.BASE_LEVEL_LINEAR);
        setMagFilter(Texture.BASE_LEVEL_LINEAR);
        setBoundaryModeS(Texture.CLAMP);
        setBoundaryModeT(Texture.CLAMP);
        setBoundaryModeR(Texture.CLAMP);

    }

    public void addChannel(double[][][] channelValues, double cl_min, double cl_max, Color3f c){


        Calibration cal = new Calibration();
        cal.color = c;
        calibrations.add(cal);

        findMinAndMaxValues(cal, channelValues);
        cal.setRange(cl_min, cl_max);

        clamp();
    }

    /**
     * Finds the max and min values in the image.
     *
     */
    private void findMinAndMaxValues(Calibration cal, double[][][] double3d) {
        cal.min = Double.MAX_VALUE;
        cal.max = -Double.MAX_VALUE;
        for (int k = 0; k < zDim; k++) {
            for (int j = 0; j < yDim; j++) {
                for (int i = 0; i < xDim; i++) {
                    if (double3d[i][j][k] > cal.max) cal.max = double3d[i][j][k];
                    if (double3d[i][j][k] < cal.min) cal.min = double3d[i][j][k];
                }
            }
        }
    }

    /**
     * Adds ints a and b together as unsigned bytes 0 -> 255 twos compliment. [ -128, 127 ] becomes
     * [0, 255] then they're added, and clipped at 255.
     *
     * @param a
     * @param b
     * @return
     */
    static byte accumulate(int a, int b){
        int s = (a & 0xff) + (b & 0xff);
        s = s>255 ? 255 : s;
        return (byte)s;
    }

    public void refresh(){
        clamp();
    }
    /**
     *
     * Creates the data for the Texture3D
     *
     */
    private void clamp() {
        ImageComponent3D pArray = new ImageComponent3D(ImageComponent.FORMAT_RGBA, xDim, yDim, zDim);


        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

        ComponentColorModel colorModel =
                new ComponentColorModel(colorSpace, true, false,
                        Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        WritableRaster raster =
                colorModel.createCompatibleWritableRaster(xDim, yDim);

        BufferedImage bImage =
                new BufferedImage(colorModel, raster, false, null);

        byte[] byteData = ((DataBufferByte)raster.getDataBuffer()).getData();

        //COLORS: [0;255] 0 - black, 255 - white
        //TRANSP: [0;255] 0 - fully transparent, 255 - opaque


        for (int z = 0; z < zDim; z++) {

            for(int channel = 0; channel < textures.size(); channel ++ ) {
                int index = 0;
                double[][][] double3d = textures.get(channel);
                Calibration cal = calibrations.get(channel);
                final Vector4f color4f = new Vector4f(cal.color.x, cal.color.y, cal.color.z, 1.f);
                int notFirst = channel==0?0:1;
                for (int y = 0; y < yDim; y++) {
                    for (int x = 0; x < xDim; x++) {

                        double data = double3d[x][y][z];
                        if (data < cal.clampedMin) data = cal.clampedMin;
                        if (data > cal.clampedMax) data = cal.clampedMax;
                        double scale = (data - cal.clampedMin) / (cal.clampedMax - cal.clampedMin);

                        Vector4f v = new Vector4f(color4f);
                        v.scale((float) scale);
                        //Vector4f v = new Vector4f(color4f.x, color4f.y, color4f.z, (float)(scale*255));
                        //R
                        byteData[index] = accumulate(byteData[index]*notFirst, (int)(v.x*255));
                        index++;
                        //G
                        byteData[index] = accumulate(byteData[index]*notFirst, (int)(v.y*255));
                        index++;
                        //B
                        byteData[index] = accumulate(byteData[index]*notFirst, (int)(v.z*255));
                        index++;
                        //transparency
                        int a = cal.alphaFromScale(scale);
                        byteData[index] = accumulate(byteData[index]*notFirst, a);
                        index++;
                    }
                }

                pArray.set(z, bImage);

            }
        }

        setImage(0, pArray);
    }

    public void setColor(int channel, double x, double y, double z){
        Calibration cal = calibrations.get(channel);
        cal.color = new Color3f((float)x,(float)y,(float)z);
    }

    /**
     * Sets the transparency to the range associated relative to the clamped min and max.
     * 0 and 1 will be same range.
     * @param channel
     * @param low
     * @param high
     */
    public void setTransparencyRange(int channel, double low, double high){
        Calibration c = calibrations.get(channel);
        c.setTransparencyRange(low, high);
        clamp();
    }

}
