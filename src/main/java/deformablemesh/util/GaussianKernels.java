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

/**
 * Created by msmith on 2/1/16.
 */
public class GaussianKernels {
    static int width = 5;
    static double sigma = 2;
    static public double[] blurred1DKernel(){
        double factor = 1/Math.sqrt(2*Math.PI)/sigma;
        double[] kernel1 = new double[2*width + 1];
        double s = 1.0/sigma/sigma/2;
        for(int i = 0; i<kernel1.length; i++){
            double x = i - width;

            double f= Math.exp(-x*x*s);

            kernel1[i] = factor*f;

        }
        return kernel1;
    }


    public static double[] firstDerivative1DKernel() {
        double[] kernel1 = new double[2*width + 1];
        double s = 1.0/sigma/sigma/2;
        double factor = -1/Math.sqrt(2*Math.PI)/sigma;
        double sum = 0;
        for(int i = 0; i<kernel1.length; i++){
            double x = i - width;

            double f= -x*s*2*Math.exp(-x*x*s);

            kernel1[i] = f*factor;
            sum += Math.abs(kernel1[i]);
        }
        for(int i = 0; i<kernel1.length; i++){
            kernel1[i] = kernel1[i]/sum;
        }
        return kernel1;

    }

    public static double[] secondDerivative1DKernel() {
        double[] kernel1 = new double[2*width + 1];
        double s = 1.0/sigma/sigma/2;
        double factor = 1/Math.sqrt(2*Math.PI)/sigma;
        for(int i = 0; i<kernel1.length; i++){
            double x = i - width;

            double f= Math.exp(-x*x*s);

            kernel1[i] = factor*(x*x*s*s*4 - s*2)*f;

        }
        return kernel1;
    }
}
