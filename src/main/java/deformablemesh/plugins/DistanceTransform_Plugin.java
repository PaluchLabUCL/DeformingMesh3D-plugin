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
package deformablemesh.plugins;

import deformablemesh.MeshImageStack;
import deformablemesh.util.DistanceTransformMosaicImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class DistanceTransform_Plugin implements PlugInFilter {
    MeshImageStack stack;
    ImagePlus original;
    @Override
    public int setup(String s, ImagePlus imagePlus) {
        stack = new MeshImageStack(imagePlus);
        original = imagePlus;
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        ImageStack result = new ImageStack(stack.getWidthPx(), stack.getHeightPx() );


        for(int i = 0; i<stack.getNFrames(); i++) {
            long start = System.currentTimeMillis();
            System.out.println("starting frame: " + i);
            stack.setFrame(i);
            ImagePlus frame = stack.getCurrentFrame();
            DistanceTransformMosaicImage dtmi = new DistanceTransformMosaicImage(frame);
            dtmi.findBlobs();
            dtmi.createCascades();
            ImageStack frames = dtmi.createLabeledImage().getStack();
            for(int j = 1; j<=frames.getSize(); j++){
                result.addSlice(frames.getSliceLabel(j), frames.getProcessor(j));
            }
            long finished = System.currentTimeMillis() - start;
        }
        ImagePlus transformed = original.createImagePlus();
        transformed.setTitle(original.getShortTitle() +  "-transformed.tif");
        transformed.setStack(result, 1, stack.getNSlices(), stack.getNFrames());
        transformed.setOpenAsHyperStack(true);
        transformed.show();
    }
}
