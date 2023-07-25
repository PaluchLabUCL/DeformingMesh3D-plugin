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

import java.util.Arrays;

public class MeshInverter {
    /**
     * Creates a mesh where outside is inside and inside is out.
     * @param mesh mesh to be inverted.
     *
     * @return copy of the inverted mesh.
     */
    public static DeformableMesh3D invertMesh(DeformableMesh3D mesh){
        final double[] pts = Arrays.copyOf(mesh.positions, mesh.positions.length);
        final int[] con = Arrays.copyOf(mesh.connection_index, mesh.connection_index.length);
        final int[] tri = Arrays.copyOf(mesh.triangle_index, mesh.triangle_index.length);
        for(int i = 0; i<tri.length/3; i++){

            int s = tri[3*i];
            tri[3*i] = tri[3*i+1];
            tri[3*i + 1] = s;

        }
        return new DeformableMesh3D(pts, con, tri);

    }
}
