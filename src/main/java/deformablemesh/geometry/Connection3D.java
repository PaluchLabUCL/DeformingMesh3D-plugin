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
 * User: msmith
 * Date: 7/2/13
 * Time: 7:56 AM
 */
public class Connection3D {
    final public Node3D A, B;
    double length;

    Connection3D(Node3D a, Node3D b){
        A=a;
        B=b;
    }

    void update(){
        double[] p1 = A.getCoordinates();
        double[] p2 = B.getCoordinates();
        double x = p1[0] - p2[0];
        double y = p1[1] - p2[1];
        double z = p1[2] - p2[2];
        length = Math.sqrt(x*x + y*y + z*z);
    }
    int[] getIndices(){
        return new int[]{A.index, B.index};
    }

    @Override
    public int hashCode(){
        return A.index+B.index;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Connection3D){
            Connection3D oc = (Connection3D)o;
            return A.index==oc.A.index?
                    B.index==oc.B.index :
                    A.index==oc.B.index && B.index==oc.A.index;
        }
        return false;
    }

}
