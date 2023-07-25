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

import deformablemesh.geometry.Node3D;

/**
 * Created by msmith on 2/29/16.
 */
public class NodeAttractor implements ExternalEnergy{
    int index;
    double[] position;
    double[] delta = new double[3];
    double weight;
    public NodeAttractor(Node3D node, double weight){
        index = node.getIndex();
        this.weight = weight;
        position = node.getCoordinates();
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        fx[index] += weight*(position[0] - positions[3*index]);
        fy[index] += weight*(position[1] - positions[3*index+1]);
        fz[index] += weight*(position[2] - positions[3*index+2]);
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
