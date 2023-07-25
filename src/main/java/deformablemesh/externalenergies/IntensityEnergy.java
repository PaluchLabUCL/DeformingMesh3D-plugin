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
package deformablemesh.externalenergies;

import deformablemesh.MeshImageStack;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Creates an image energy the drives the mesh towards (or away from) the regions of maximum intensity.
 *
 * User: msmith
 * Date: 7/3/13
 * Time: 1:42 PM
 */
public class IntensityEnergy implements ExternalEnergy {

    MeshImageStack stack;
    double weight;
    double dr;
    public IntensityEnergy(MeshImageStack stack, double image_weight){
        this.stack = stack;


        dr = stack.getMinPx();

        weight = image_weight;
    }
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double[] working = new double[3];
        for(int i = 0; i<positions.length/3; i++){

            double x = positions[3*i + 0];
            double y = positions[3*i + 1];
            double z = positions[3*i + 2];
            working[0] = x;
            working[1] = y;
            working[2] = z;



            fx[i] += getGradient(working, 0)*weight;
            fy[i] += getGradient(working, 1)*weight;
            fz[i] += getGradient(working, 2)*weight;


        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return stack.getInterpolatedValue(pos);
    }


    double getGradient(double[] xyz, int direction){
        double v = 0;
        double o = xyz[direction];

        xyz[direction] += dr;
        v += stack.getInterpolatedValue(xyz);
        xyz[direction] = o - dr;
        v -= stack.getInterpolatedValue(xyz);
        xyz[direction] = o;
        return v;
    }


    public static void main(String[] args){
        ImageJ.main(args);
        ImagePlus imp = new ImagePlus("practice/sphere-2.tif");
        imp.show();
        MeshImageStack stack = new MeshImageStack(imp);
        IntensityEnergy grad = new IntensityEnergy(stack, 1.0);
        ImageStack x = new ImageStack(imp.getWidth(), imp.getHeight());
        ImageStack y = new ImageStack(imp.getWidth(), imp.getHeight());
        ImageStack z = new ImageStack(imp.getWidth(), imp.getHeight());

        int last = imp.getNSlices();
        for(int i = 1; i<=last; i++){
            ImageProcessor px = new FloatProcessor(imp.getWidth(), imp.getHeight());
            ImageProcessor py = new FloatProcessor(imp.getWidth(), imp.getHeight());
            ImageProcessor pz = new FloatProcessor(imp.getWidth(), imp.getHeight());
            for(int j = 0; j<imp.getHeight(); j++){
                for(int k = 0; k<imp.getWidth(); k++){
                    double[] pt = stack.getNormalizedCoordinate(new double[]{k, j, i-1});
                    px.setf(k,j,(float)grad.getGradient(pt, 0));
                    py.setf(k,j,(float)grad.getGradient(pt, 1));
                    pz.setf(k,j,(float)grad.getGradient(pt, 2));
                }
            }
            x.addSlice(px);
            y.addSlice(py);
            z.addSlice(pz);
        }
        new ImagePlus("gx", x).show();
        new ImagePlus("gy", y).show();
        new ImagePlus("gz", z).show();

    }
}
