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

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WireframeMesh {
    DeformableMesh3D mesh;
    double width = 0.001;
    public WireframeMesh(DeformableMesh3D mesh){

        this.mesh = mesh;

    }
    public void setWidth(double width){
        this.width = width;
    }
    public DeformableMesh3D getWireFrameMesh(){
        List<Connection3D> connection3DList = mesh.getConnections();

        List<DeformableMesh3D> wireFrameConnections = connection3DList.stream().map(
                this::fromConnection
        ).filter(o -> o!=null ).collect(
                Collectors.toList()
        );

        return DeformableMesh3DTools.mergeMeshes(wireFrameConnections);

    }

    DeformableMesh3D fromConnection(Connection3D connection){

        double[] ptA = connection.A.getCoordinates();
        double[] ptB = connection.B.getCoordinates();
        double[] r = Vector3DOps.difference(ptA, ptB);
        double l = Vector3DOps.normalize(r);

        if(l>0){
            double[] center = Vector3DOps.add(ptA, ptB, 1);
            center[0] = center[0]/2;
            center[1] = center[1]/2;
            center[2] = center[2]/2;

            double[] cross = Vector3DOps.cross(r, Vector3DOps.zhat);
            double sin_theta = Vector3DOps.normalize(cross);
            double cos_theta = Vector3DOps.dot(r, Vector3DOps.zhat);


            DeformableMesh3D wire = DeformableMesh3DTools.createTestBlock(width, width, l);
            wire.translate(center);

            if(sin_theta != 0){
                wire.rotate(cross, center, - Math.atan2(sin_theta, cos_theta));
            }

            return wire;


        }
        return null;
    }

    public static void main(String[] args){
        //start with a single triangular mesh.
        List<double[]> positions = Arrays.asList(
                new double[]{1, 0, 0},
                new double[]{1, 1, 0},
                new double[]{0, 1, 0}
        );
        List<int[]> connections = Arrays.asList(
                new int[]{0, 1},
                new int[]{1, 2},
                new int[]{2, 0}
        );

        List<int[]> triangles = Arrays.asList(
                new int[]{0, 1, 2}
        );

        //DeformableMesh3D mesh = new DeformableMesh3D( positions, connections, triangles);
        //DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(1);
        DeformableMesh3D mesh = DeformableMesh3DTools.createTestBlock(0.25, 0.25, 0.25);
        mesh.ALPHA = 1;
        mesh.BETA = 0.00001;
        mesh.GAMMA = 100;

        MeshFrame3D frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();

        mesh.create3DObject();

        frame.addDataObject(mesh.data_object);
        mesh.setShowSurface(true);
        mesh.setColor(Color.RED);


        WireframeMesh wire = new WireframeMesh(mesh);

        DeformableMesh3D wfMesh = wire.getWireFrameMesh();
        wfMesh.GAMMA = mesh.GAMMA;
        wfMesh.BETA = mesh.BETA;
        wfMesh.ALPHA = mesh.ALPHA;

        wfMesh.create3DObject();
        frame.addDataObject(wfMesh.data_object);
        wfMesh.setColor(Color.BLUE);
        wfMesh.setShowSurface(true);
        for(int i = 0; i<100; i++) {
            mesh.update();
            wfMesh.update();
            try{
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
