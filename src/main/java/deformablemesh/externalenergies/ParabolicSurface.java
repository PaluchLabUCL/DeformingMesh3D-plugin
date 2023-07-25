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

public class ParabolicSurface implements ExternalEnergy {
    double a = 2;
    double gravityMagnitude;
    double surfaceFactor;
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        for(int i = 0; i<fz.length; i++){
            double z = positions[i*3 + 2];
            double y = positions[i*3 + 1];
            double x = positions[i*3 + 0];
            double r = x*x + y*y;
            if(z<r*a){
                r = Math.sqrt(r);
                x = x/r;
                y = y/r;

                double dz_dr = 2*a*r;
                dz_dr = dz_dr*dz_dr;
                double dz = Math.sqrt(dz_dr/(1+dz_dr));
                double dr = Math.sqrt(1 - dz);
                fz[i] += -gravityMagnitude - (positions[3*i + 2]-2*r) * surfaceFactor*dr;
                fy[i] += -dz*y*surfaceFactor;
                fx[i] += -dz*x*surfaceFactor;
            }

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        if(pos[2]>0){
            return 0;
        }
        return -pos[2];
    }
}
