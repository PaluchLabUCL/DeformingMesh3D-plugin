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

import deformablemesh.util.Vector3DOps;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by msmith on 4/7/16.
 */
public class RayCastMeshTesting {

    @Test
    public void keyTest(){
        HashMap<NodeKey, Node3D> map = new HashMap<NodeKey, Node3D>();

        double[] positions = new double[3*3]; //enough room for 3 positions.
        Node3D a = new Node3D(positions, 0);
        Node3D b = new Node3D(positions, 1);
        Node3D c = new Node3D(positions, 2);

        map.put(new NodeKey(0, 1), a);
        map.put(new NodeKey(0, 2), b);
        map.put(new NodeKey(1, 0), c);

        Assert.assertEquals(map.keySet().size(), 2);
        Assert.assertEquals(map.get(new NodeKey(0,1)).index, 2);
    }

    @Test
    public void boxBoundSphere(){
        double[][] origins = {
                DeformableMesh3D.ORIGIN,
                {0.5, 0, 0},
                {0, 0.5, 0},
                {0, 0, 0.5},
                {-0.5, 0, 0},
                {0, -0.5, 0},
                {0, 0, -0.5}
        };
        DeformableMesh3D start = RayCastMesh.sphereRayCastMesh(5);

        //sphere is always better
        for(double[] origin: origins) {
            Box3D box = new Box3D(origin, 1, 1, 0.5);
            Sphere sphere = new Sphere(origin, 0.2);

            for (Node3D node : start.nodes) {

                List<Intersection> intersections = sphere.getIntersections(origin, node.getCoordinates());
                List<Intersection> boxers = box.getIntersections(origin, node.getCoordinates());
                Intersection best = RayCastMesh.bestIntersectionSpherical(intersections, origin, node.getCoordinates());
                Intersection worst = RayCastMesh.bestIntersectionSpherical(boxers, origin, node.getCoordinates());
                double s = Vector3DOps.distance(best.location, origin);
                double s2 = Vector3DOps.distance(worst.location, origin);
                Assert.assertTrue(s2 > s);
            }
        }

        //sphere is always worse.
        for(double[] origin: origins) {
            System.out.println(Arrays.toString(origin));
            Box3D box = new Box3D(origin, 1, 1, 0.5);
            Sphere sphere = new Sphere(origin, 3);

            for (Node3D node : start.nodes) {

                List<Intersection> intersections = sphere.getIntersections(origin, node.getCoordinates());
                List<Intersection> boxers = box.getIntersections(origin, node.getCoordinates());
                Intersection best = RayCastMesh.bestIntersectionSpherical(intersections, origin, node.getCoordinates());
                Intersection worst = RayCastMesh.bestIntersectionSpherical(boxers, origin, node.getCoordinates());
                double s = Vector3DOps.distance(best.location, origin);
                double s2 = Vector3DOps.distance(worst.location, origin);
                Assert.assertTrue(s > s2);
            }
        }

        //origin is outside of the boundary.

    }

    @Test
    public void semiSphereRemesh(){
        double[] bO = DeformableMesh3D.ORIGIN;
        double[] sO = new double[]{0, 0, -0.9};
        Box3D box = new Box3D(bO, 1, 1, 0.5);
        Sphere sphere = new Sphere(sO, 2);
        List<Interceptable> interceptables = new ArrayList<>();
        interceptables.add(box);
        interceptables.add(sphere);

        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(interceptables, new double[]{0, 0, box.low[2] + 20*Vector3DOps.TOL}, 3);
        for(Node3D node: mesh.nodes){
            if(!box.contains(node.getCoordinates())){
                double[] p = node.getCoordinates();

            }
            Assert.assertTrue(box.contains(node.getCoordinates()));
        }

        InterceptingMesh3D intercepts = new InterceptingMesh3D(mesh);
        interceptables.clear();

        DeformableMesh3D newMesh = RayCastMesh.rayCastMesh(intercepts, intercepts.getCenter(), 4);

        for(Node3D node: newMesh.nodes){
            if(!box.contains(node.getCoordinates())){
                double[] p = node.getCoordinates();
                System.out.println(Arrays.toString(p));

            }
            Assert.assertTrue(box.contains(node.getCoordinates()));
        }
    }
}
