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

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.track.Track;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.util.HashSet;
import java.util.Set;

public class Create3DTrainingDataFromLabelledImage extends Create3DTrainingData{
    MeshImageStack labels;
    public Create3DTrainingDataFromLabelledImage(ImagePlus original, ImagePlus labels){
        super(original);
        this.labels = new MeshImageStack(labels);
    }

    @Override
    public void run(int frameNumber) {
        int w = original.getWidth();
        int h = original.getHeight();

        ImageStack output = labels.getCurrentFrame().getImageStack();
        mask = createMaskImage(output);
        System.out.println("masked'd");

        membrane = createMembraneImage(output);
        System.out.println("membraned");

        distance = createDistanceMapImage(output);
        System.out.println("distance map'd");

        for(int i = 1; i<=mask.size(); i++){
            ImageProcessor maskProcessor = mask.getProcessor(i);
            ImageProcessor membraneProcessor = membrane.getProcessor(i);
            ImageProcessor distanceProcessor = distance.getProcessor(i);

            for(int k = 0; k<w*h; k++){
                int msk = maskProcessor.get(k);
                int mem = membraneProcessor.get(k);
                int d = distanceProcessor.get(k);
                //bit 0->background 1->membrane, 2->mask, 3+->distance
                if(msk==0) {
                    maskProcessor.set(k, 1);
                    //msk=0;
                } else{
                    maskProcessor.set(k, 4);
                    msk = 4;
                }
                if(mem != 0){
                    maskProcessor.set(k, 2);
                } else if(d!=0) {
                    d = d << 3;
                    maskProcessor.set(k, d + msk);
                }

            }
        }

    }
}
