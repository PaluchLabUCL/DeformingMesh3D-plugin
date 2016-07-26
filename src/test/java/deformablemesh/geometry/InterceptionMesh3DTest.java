package deformablemesh.geometry;

import deformablemesh.util.Vector3DOps;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by msmith on 4/21/16.
 */
public class InterceptionMesh3DTest {
    final double TOL = 1e-9;

    /**
     * Creates a sphere mesh and checks a range of angles that the mesh intercepts the mesh
     */
    @Test
    public void testInterceptingSphere(){
        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(0);

        InterceptingMesh3D ints = new InterceptingMesh3D(mesh);
        double[] o = ints.center;

        RayCastMesh.rayCastMesh(ints, o, 1);

        for(double[] angle: AngleGenerator.generator(25, 15)){
            List<Intersection> is = ints.getIntersections(o, angle);
            Assert.assertTrue(is.size()>=2);
            for(Intersection i: is){
                Assert.assertTrue(Vector3DOps.mag(i.location) <= 1+TOL);
            }
        }

    }
}
