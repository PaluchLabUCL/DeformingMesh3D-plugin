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
import java.util.List;
import java.util.stream.Collectors;

public class ShrinkRegion {
    byte[] volume;
    int[] dim;
    int[] org;
    int label;
    List<int[]> points = new ArrayList<>();
    public ShrinkRegion(Region r){
        points = new ArrayList<>(r.getPoints());
        dim = new int[] {r.hx - r.lx, r.hy - r.ly, r.hz - r.lz};
        org = new int[] {r.lx, r.ly, r.lz};
        volume = new byte[r.hx*r.hy*r.hz];
        for(int[] pt: points){
            volume[getIndex(pt)] = 1;
        }
        label = r.label;
    }
    int getIndex(int[] xyz){
        return getIndex(xyz[0], xyz[1], xyz[2]);
    }
    int getIndex(int x, int y, int z){
        return (x - org[0]) + (y - org[1])*dim[0] + (z - org[2])*dim[0]*dim[1];
    }
    boolean isEdge(int[] xyz){
        for(int i = -1; i<=1; i++){
            int z = xyz[2] + i;
            if(z<org[2] || z>=dim[2] + org[2]){
                return true;
            }
            for(int j = -1; j<=1; j++){
                int y = xyz[1] + j;
                if(y<org[1] || y>=dim[1] + org[1]){
                    return true;
                }
                for(int k = -1; k<=1; k++){

                    if(i==0 && j == 0 && k == 0){
                        continue;
                    }

                    int x = xyz[0] + k;
                    if(x<org[0] || x>=dim[0] + org[0]){
                        return true;
                    }
                    int l = getIndex(x, y, z);

                    if( volume[l] == 0){
                        return true;
                    }

                }
            }
        }
        return false;
    }
    public void step(){
        List<int[]> removing = points.stream().filter(this::isEdge).collect(Collectors.toList());
        System.out.println("removing: " + removing.size() + "of" + points.size() + " pixels");
        removing.forEach(this::remove);
    }

    public void remove(int[] xyz){
        points.remove(xyz);
        volume[getIndex(xyz)] = 0;
    }
    public Region generateRegion(){
        return new Region(label, points);
    }
}
