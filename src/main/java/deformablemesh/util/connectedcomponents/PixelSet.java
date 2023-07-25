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
package deformablemesh.util.connectedcomponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PixelSet{
    Set<Pixel> pixels = new HashSet<>();
    List<int[]> original = new ArrayList<>();
    PixelSet(){

    }
    boolean add(int[] xyz){
        if(pixels.add(new Pixel(xyz))){
            original.add(xyz);
            return true;
        }
        return false;
    }
    static class Pixel{
        final int x, y, z;
        public Pixel(int[] xyz){
            x = xyz[0];
            y = xyz[1];
            z = xyz[2];
        }
        @Override
        public int hashCode(){
            return x + y + z;
        }
        @Override
        public boolean equals(Object o){
            if(o instanceof Pixel){
                Pixel op = (Pixel)o;
                return op.x==x && op.y==y && op.z == z;
            }
            return false;
        }
    }

}
