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

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;

public class VolumeConservation  implements ExternalEnergy{
    final DeformableMesh3D mesh;
    final double weight;
    double volume;

    CurvatureCalculator calculator;

    public VolumeConservation(DeformableMesh3D mesh, double weight){
        this.mesh = mesh;
        volume = mesh.calculateVolume(new double[]{0, 0, 1});
        double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);

        calculator = new CurvatureCalculator(mesh);

        this.weight = weight*1e8;

        System.out.println( weight/area/volume/volume/volume*2 + ", " + weight);



    }
    public double getVolume(){
        return volume;
    }

    public void setVolume(double v){
        this.volume = v;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double nv = mesh.calculateVolume(new double[]{0, 0, 1});

        double dv = (-nv + volume);
        double factor = weight*dv*dv*dv;
        for(int i = 0; i<positions.length/3; i++){
            double[] normal = calculator.getNormal(i);
            double area = calculator.calculateMixedArea(mesh.nodes.get(i));
            fx[i] += factor*area*normal[0];
            fy[i] += factor*area*normal[1];
            fz[i] += factor*area*normal[2];

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
