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
package deformablemesh.util;

import deformablemesh.MeshImageStack;
import deformablemesh.util.connectedcomponents.ConnectedComponents2D;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.List;
import java.util.Map;

abstract public class Create3DTrainingData {
    ImagePlus original;
    double relativeDepth;
    MeshImageStack stack;
    ImageStack mask;
    ImageStack membrane;
    ImageStack distance;

    Create3DTrainingData(ImagePlus original){
        this.original = original;
        stack = new MeshImageStack(original);
        Calibration cb = original.getCalibration();
        if(cb.pixelHeight != cb.pixelWidth){
            System.out.println("warning! x and y resolutions are not equal");
        }
        relativeDepth = cb.pixelDepth / cb.pixelWidth;
    }

    abstract public void run(int frameNumber);

    /**
     * Creates a binary stack (ByteProcessor#threshold(int)) from the provided labelled image. First a threshold
     * is used, then a fill holes is used.
     *
     * @param mosaic labelled image, where non-zero values represent a different cell/mesh
     * @return stack of masked images.
     */
    public ImageStack createMaskImage(ImageStack mosaic){
        ImageStack mask = new ImageStack(mosaic.getWidth(), mosaic.getHeight());
        for(int i = 1; i<=mosaic.size(); i++){
            ByteProcessor p = mosaic.getProcessor(i).convertToByteProcessor();
            p.threshold(0);
            fillHoles(p);
            mask.addSlice(p);

        }
        return mask;
    }
    /**
     * Fills holes, by inverting the processor (1-&gt; 0 and 0-&gt;1) then labels the connected components. All of the regions
     * that do not touch the edge are set to 255.
     *
     * @param p binary in put processor.
     */
    public void fillHoles(ImageProcessor p){
        ImageProcessor p2 = p.duplicate();
        p2.invert();
        ConnectedComponents2D cc = new ConnectedComponents2D(p2);
        Map<Integer, List<int[]>> points = cc.getRegions();
        for(Integer i: points.keySet()){
            List<int[]> pixels = points.get(i);

            if(!touchesEdge(pixels, p.getWidth(), p.getHeight())){
                for(int[] px: pixels){
                    p.set(px[0], px[1], 255);
                }
            }
        }
    }
    /**
     * checks if *any* of the provided pixels touch the edge of the box bound by x ∈ [0, width) and y ∈ [0, height)
     *
     * @param pixels x,y pairs of points.
     * @param width upper bounds for x. x=0 or x=width-1 touches.
     * @param height upper bounds for y. y=0 or y=height-1 touches.
     * @return true if any pixel touches and edge, false otherwise.
     */
    public boolean touchesEdge(List<int[]> pixels, int width, int height){

        for(int[] px: pixels){
            if(px[0]==0 || px[1]==0 || px[0]==width-1 || px[1]==height-1){
                return true;
            }
        }

        return false;

    }

    /**
     * Creates a "binary" image, 255 or 0, of the membrane by checking neighboring pixels for either being
     * background or another blob.
     *
     * @param mosaic stack of Labelled images. Each label corresponds to a separate cell/mesh
     *
     * @return stack stack of images with 255 for membrane and 0 for non-membrane.
     */
    public ImageStack createMembraneImage(ImageStack mosaic){
        int w = mosaic.getWidth();
        int h = mosaic.getHeight();
        ImageStack membrane = new ImageStack(w, h);
        System.out.println("starting membrane " + mosaic.size());

        for(int i = 1; i<=mosaic.size(); i++){
            ByteProcessor p = new ByteProcessor(w, h);
            ImageProcessor mos = mosaic.getProcessor(i);
            for(int j = 0; j<w; j++){

                for(int k = 0; k<h; k++){
                    int px = mos.get(j, k);
                    if(px!=0){
                        if(j==0 || k==0 || j==w-1 || k==h-1){
                            p.set(j, k, 255);
                            continue;
                        }
                        px_check:
                        for(int dn = -1; dn<=1; dn++){
                            for(int dm = -1; dm<=1; dm++){
                                if(mos.get(j+dn, k+dm)!=px){
                                    p.set(j, k, 255);
                                    break px_check;
                                }
                            }
                        }
                    }

                }

            }
            membrane.addSlice(p);

        }
        return membrane;
    }

    public ImageStack createDistanceMapImage(ImageStack mosaic){
        System.out.println("starting transform");
        ImagePlus container = original.createImagePlus();
        container.setStack(mosaic);
        DistanceTransformMosaicImage dtmi = new DistanceTransformMosaicImage(container);
        dtmi.findBlobs();
        int scale = (int)relativeDepth;
        if(scale < 1){
            scale = 1;
        }
        dtmi.createScaledCascades(scale); //TODO make this non-fixed.
        System.out.println("Creating image!");
        ImagePlus plus = dtmi.createLabeledImage();
        System.out.println("Image Created");
        return plus.getStack();
    }

    public void showCurrent(){
        new ImagePlus("mask", mask).show();
        new ImagePlus("distance", distance).show();
        new ImagePlus("outlines", membrane).show();
    }

    public ImageStack getLabeledStack() {
        return mask;
    }

    /**
     * returns a single time point multi-channel image.
     *
     * @param tp
     * @return
     */
    public ImagePlus getOriginalFrame(Integer tp) {
        ImagePlus frame = original.createImagePlus();
        ImageStack stack = new ImageStack(original.getWidth(), original.getHeight());
        int n = original.getNSlices();
        int c = original.getNChannels();
        ImageStack originStack = original.getStack();
        int tpSize = n*c;

        for(int i = 1; i<=tpSize; i++){
            stack.addSlice(originStack.getProcessor( ( tp ) * ( c * n ) + i ) );
        }
        frame.setStack(stack, c, n, 1);
        return frame;
    }
}
