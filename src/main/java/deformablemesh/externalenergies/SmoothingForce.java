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

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;

import java.util.List;

public class SmoothingForce implements ExternalEnergy{
    double magnitude;
    CurvatureCalculator calc;
    public SmoothingForce(DeformableMesh3D mesh, double magnitude){
        this.magnitude = magnitude;
        calc = new CurvatureCalculator(mesh);
    }
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        //x, y, z, Kappa, nx, ny, nz.
        List<double[]> curvatures = calc.calculateCurvature();
        for(int i = 0; i<fx.length; i++){
            double[] row = curvatures.get(i);
            double f = row[3]*magnitude;
            fx[i] += -row[4]*f;
            fy[i] += -row[5]*f;
            fz[i] += -row[6]*f;
        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
