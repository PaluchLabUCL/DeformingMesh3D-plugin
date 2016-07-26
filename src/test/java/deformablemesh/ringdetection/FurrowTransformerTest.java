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
