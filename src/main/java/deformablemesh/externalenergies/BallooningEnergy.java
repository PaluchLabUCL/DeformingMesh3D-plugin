package deformablemesh.externalenergies;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.CompositeInterceptables;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Interceptable;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.geometry.Triangle3D;
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
    final double[] counters;
    public BallooningEnergy(Interceptable constraint, DeformableMesh3D mesh, double weight){
        this.constraint = constraint;
        this.mesh = mesh;
        this.weight = weight;
        counters = new double[mesh.nodes.size()];
        int[] indexes = new int[3];
        for(Triangle3D tri: mesh.triangles){
            tri.getIndices(indexes);
            for(int dex: indexes){
                counters[dex]++;
            }
        }
        for(int i = 0; i<counters.length; i++){
            if(counters[i]>0){
                counters[i]=1/counters[i];
            }
        }
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        int[] indexes = new int[3];

        double factor = weight;
        double[] pt = new double[3];
        for(Triangle3D t: mesh.triangles){
            t.update();
            t.getIndices(indexes);
            int a = indexes[0];
            int b = indexes[1];
            int c = indexes[2];

            double f_x = t.normal[0]* factor;
            double f_y = t.normal[1]* factor;
            double f_z = t.normal[2]* factor;

            pt[0] = positions[3*a];
            pt[1] = positions[3*a + 1];
            pt[2] = positions[3*a + 2];

            if(constraint.contains(pt)) {
                fx[a] += f_x*counters[a];
                fy[a] += f_y*counters[a];
                fz[a] += f_z*counters[a];
            }

            pt[0] = positions[3*b];
            pt[1] = positions[3*b + 1];
            pt[2] = positions[3*b + 2];

            if(constraint.contains(pt)){
                fx[b] += f_x*counters[b];
                fy[b] += f_y*counters[b];
                fz[b] += f_z*counters[b];
            }

            pt[0] = positions[3*c];
            pt[1] = positions[3*c + 1];
            pt[2] = positions[3*c + 2];

            if(constraint.contains(pt)){
                fx[c] += f_x*counters[c];
                fy[c] += f_y*counters[c];
                fz[c] += f_z*counters[c];

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
