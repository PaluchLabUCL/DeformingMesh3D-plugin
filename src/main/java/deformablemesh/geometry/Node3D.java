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

/**
 * Essentially a pointer. It keeps a reference to the backing array of data, and the
 * starting position.
 *
 * User: msmith
 * Date: 7/2/13
 * Time: 7:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class Node3D {
    private double[] positions;
    public final int index;
    double gammaFactor = 1;
    public Node3D(double[] data, int index){
        positions = data;
        this.index = index;
    }

    public int getIndex(){
        return index;
    }


    void update(){

    }

    /**
     * Creates a double[] with the position of this node.
     *
     * @return a new double[] that contains the x,y,z coordinates of this array.
     */
    public double[] getCoordinates(){
        return new double[]{positions[3*index], positions[3*index+1], positions[3*index+2]};
    }

    /**
     * Updates the array backing this position.
     *
     * @param pos
     */
    public void setPosition(double[] pos){
        for(int i = 0; i<3; i++){
            positions[3*index + i] = pos[i];
        }
    }

    /**
     * Sets the position array that this node uses to back its data. This method is for use when updating a mesh by
     * by adding or removing points.
     *
     * @param data the array containing the position information. This array should contain all of the positions for
     *             all of the Node3D in the same mesh.
     */
    public void setBackingData(double[] data){
        positions = data;
    }

    @Override
    public boolean equals(Object o){
        if(o==null)return false;
        if(o instanceof Node3D){
            return ((Node3D)o).positions==positions && ((Node3D)o).index==index;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return index;
    }

    /**
     * Gamma affects how much the node moves. For most nodes, the gamma is determined by
     * @param gamma
     * @return
     */
    public double getGamma(double gamma) {
        return gamma*gammaFactor;
    }

    /**
     * This determines how much this node moves compared to the rest of the mesh.
     * @param factor
     */
    public void setGammaFactor(double factor){
        gammaFactor = factor;
    }
}
