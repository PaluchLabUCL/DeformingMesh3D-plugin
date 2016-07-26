package deformablemesh.geometry;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by msmith on 4/8/16.
 */
public class Box3DTest {
    @Test
    public void basicTest(){
        double[][] origins = {
                DeformableMesh3D.ORIGIN,
                {0.05, 0, 0},
                {0, 0.05, 0},
                {0, 0, 0.05},
                {-0.05, 0, 0},
                {0, -0.05, 0},
                {0, 0, -0.05}
        };
        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(5);

        for(double[] origin: origins) {
            Box3D box = new Box3D(origin, 2, 2, 2);
            for (Node3D node : mesh.nodes) {
                List<Intersection> intersections = box.getIntersections(new double[]{0, 0, 0}, node.getCoordinates());
                if(intersections.size()!=2){
                    intersections = box.getIntersections(new double[]{0, 0, 0}, node.getCoordinates());
                }
                Assert.assertEquals(intersections.size(), 2);
            }
        }
    }
}
