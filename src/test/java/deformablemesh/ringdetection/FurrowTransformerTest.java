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
package deformablemesh.ringdetection;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Furrow3D;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by msmith on 3/30/16.
 */
public class FurrowTransformerTest {
    @Test
    public void testUnitVectors(){
        MeshImageStack stack = new MeshImageStack();
        double[] z= {0,0,1};
        double[] x ={1, 0, 0};
        double[] mx = {-1, 0, 0};
        double[] y = {0, 1, 0};
        double[] origin = {0,0,0};

        FurrowTransformer t = new FurrowTransformer(new Furrow3D(origin, z), stack);
        Assert.assertArrayEquals(x, t.xn, 0);
        Assert.assertArrayEquals(y, t.yn, 0);

        FurrowTransformer t2 = new FurrowTransformer(new Furrow3D(origin, x), stack);
        Assert.assertArrayEquals(y, t2.xn, 0);
        Assert.assertArrayEquals(z, t2.yn, 0);

        FurrowTransformer t3 = new FurrowTransformer(new Furrow3D(origin, y), stack);
        Assert.assertArrayEquals(mx, t3.xn, 0);
        Assert.assertArrayEquals(z, t3.yn, 0);

    }
}
