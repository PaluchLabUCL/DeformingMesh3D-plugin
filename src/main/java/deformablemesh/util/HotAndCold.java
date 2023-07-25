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

import java.awt.Color;

public class HotAndCold implements ColorInterpolator {
    float[] hot, cold;
    double min =0;
    double max =100;
    public HotAndCold(Color hot, Color cold){
        this.hot = hot.getRGBComponents(new float[4]);
        this.cold = cold.getRGBComponents(new float[4]);
    }

    public void setMinMax(double min, double max){
        this.min = min;
        this.max = max;
    }

    public float[] getColor(double v){

        float f = (float)((v - min)/(max - min));
        if(f<=0){
            f = 0;
        } if(f>=1){
            f = 1;
        }
        return new float[]{
                f*hot[0] + (1-f)*cold[0],
                f*hot[1] + (1-f)*cold[1],
                f*hot[2] + (1-f)*cold[2]
        };

    }


}
