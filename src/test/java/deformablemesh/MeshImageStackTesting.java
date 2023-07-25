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
package deformablemesh;

import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by msmith on 4/21/16.
 */
public class MeshImageStackTesting {

    public static ImagePlus testStack(){
        ImageStack stack = new ImageStack(10, 30);
        for(int i = 0; i<5; i++){
            ImageProcessor improc = new ShortProcessor(10, 30);
            for(int j = 0; j<10; j++){
                for(int k = 0; k<30; k++){
                    int s = 2*j + ((2*k)<<4) + ((2*i)<<8);
                    improc.set(j,k,s);
                }
            }
            stack.addSlice("no label",improc);
        }

        ImagePlus imp = new ImagePlus("original",stack);
        FileInfo info = imp.getFileInfo();
        info.pixelHeight = 0.25;
        info.pixelWidth = 0.25;
        info.pixelDepth = 0.5;

        imp.setFileInfo(info);

        return imp;

    }

    @Test
    public void sampleTest(){

        ImagePlus original = testStack();

        MeshImageStack mesh_stack = new MeshImageStack(original);

        double[] normalized;
        double[] img;

        //center
        normalized = new double[]{0,0,0};

        //exact -ish
        img = mesh_stack.getImageCoordinates(normalized);

        //pixel
        double[] img2 = {(int)img[0], (int)img[1], (int)img[2]};

        double v = mesh_stack.getInterpolatedValue(mesh_stack.getNormalizedCoordinate(img2));

        double i = original.getStack().getProcessor((int)img2[2]+1).getPixelValue((int)img2[0], (int)img2[1]);

        Assert.assertEquals(i, v, Vector3DOps.TOL);
    }

}
