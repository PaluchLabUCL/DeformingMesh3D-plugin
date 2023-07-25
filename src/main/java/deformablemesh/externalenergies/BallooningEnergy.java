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
package deformablemesh.externalenergies;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.*;
import deformablemesh.meshview.MeshFrame3D;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 *
 * The goal of this energy is to grow outwards, until it hits another mesh and cannot
 * grow any more.
 *
 * Created by msmith on 3/8/16.
 */
public class BallooningEnergy implements ExternalEnergy{
    final Interceptable constraint;
    final DeformableMesh3D mesh;
    final double weight;
    CurvatureCalculator calculator;

    public BallooningEnergy(Interceptable constraint, DeformableMesh3D mesh, double weight){
        this.constraint = constraint;
        this.mesh = mesh;
        this.weight = weight;
        calculator = new CurvatureCalculator(mesh);

    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {

        for(int i = 0; i<positions.length/3; i++){
            if(constraint.contains(mesh.nodes.get(i).getCoordinates())){
                double[] normal = calculator.getNormal(i);
                double area = calculator.calculateMixedArea(mesh.nodes.get(i));
                fx[i] += weight*area*normal[0];
                fy[i] += weight*area*normal[1];
                fz[i] += weight*area*normal[2];
            }

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return constraint.contains(pos)?1:0;
    }

    public static void main(String[] args){




        DeformableMesh3D mesh2 = DeformableMesh3DTools.createRectangleMesh(2, 2, 2, 1);
        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(3);
        mesh.scale(0.1, new double[]{0,0,0});
        Sphere s = new Sphere(new double[]{-0.95, 0, 0}, 1);
        Sphere s2 = new Sphere(new double[]{0.8, 0, 0}, .8);

        //InterceptingMesh3D interceptor = new InterceptingMesh3D(mesh2);
        Interceptable interceptor = new CompositeInterceptables(s, s2);


        //NewtonMesh3D mesh = new NewtonMesh3D(pos, con, triangles);
        int subdived = 0;
        for(int i = 0; i<subdived; i++){
            RayCastMesh.subDivideMesh(mesh);
        }
        mesh2.create3DObject();
        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.hideAxis();
        mesh.create3DObject();
        frame.addDataObject(mesh.data_object);
        //frame.addDataObject(mesh2.data_object);

        mesh2.calculateCurvature();

        mesh.ALPHA = 1;
        mesh.BETA = 1;
        mesh.GAMMA=150;

        mesh.reshape();
        mesh2.triangles.forEach(Triangle3D::update);
        mesh.addExternalEnergy(new BallooningEnergy(interceptor, mesh, 1));
        mesh.addExternalEnergy(new TriangleAreaDistributor(new MeshImageStack(), mesh, 1));
        int counter = 0;

        frame.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode()==KeyEvent.VK_SPACE){
                    synchronized (frame){
                        frame.notifyAll();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        synchronized (frame){
            try {
                frame.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while(counter<200) {
                BufferedImage img = frame.snapShot();
                String name = String.format("snap-%03d.png", counter);
                try {
                    ImageIO.write(img, "png", new File(name));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                mesh.update();
                counter++;
                mesh.calculateCurvature();


            }
    }

}
