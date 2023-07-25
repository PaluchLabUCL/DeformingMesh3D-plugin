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
package deformablemesh.geometry;

import java.util.Iterator;

/**
 * Created by melkor on 2/8/16.
 */
public class AngleGenerator implements Iterator<double[]> {
    final int td;
    final int pd;

    int t = 0;
    int p = 0;
    double dtheta;
    double dphi;
    public AngleGenerator(int thetaDivisions, int phiDivisions){
        td = thetaDivisions;
        pd = phiDivisions;
        dtheta = Math.PI/(td-1)*2;
        dphi = Math.PI/(pd);
    }
    @Override
    public boolean hasNext() {
        return t<td&&p<pd;
    }

    @Override
    public double[] next() {
        if(p==0){
            p++;
            return new double[]{0,0,1};
        } else if(p==pd){
            t=td;
            return new double[]{0,0,-1};
        }
        double theta = dtheta*t;
        double phi = dphi*p;

        t++;
        if(t==td){
            t=0;
            p++;
        }

        return new double[]{Math.cos(theta)*Math.sin(phi), Math.sin(theta)*Math.sin(phi), Math.cos(phi)};
    }
    static Iterable<double[]> generator(int thetaDivisions, int phiDivisions){
        return () -> new AngleGenerator(thetaDivisions, phiDivisions);
    }
}
