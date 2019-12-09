package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a 'shell' mesh, which is essentially the original mesh + a mesh created by
 * shifting the nodes along the normal at the node a small displacement.
 *
 */
public class ShellMesh {
    double tol = 1e-2;
    double width = 0.01;
    DeformableMesh3D mesh;
    public ShellMesh(DeformableMesh3D mesh){
        this.mesh = mesh;

    }
    static void invertMesh(DeformableMesh3D old){

        int[] triangles = old.triangle_index;
        for(int i = 0; i<old.triangles.size(); i++){
            int i0 = 3*i;
            int a = triangles[i0];
            triangles[i0] = triangles[i0 + 1];
            triangles[i0+1] = a;
        }

    }

    /**
     *
     * @param mesh
     * @param width
     * @param steps
     */
    static void shiftPositionsAlongNormal(DeformableMesh3D mesh, double width, int steps){
        CurvatureCalculator calculator = new CurvatureCalculator(mesh);
        double  dw = width/steps;
        for (int j = 0; j<steps; j++) {
            for (int i = 0; i < mesh.nodes.size(); i++) {
                int i0 = 3 * i;
                double[] normal = calculator.getNormal(i);

                mesh.positions[i0 + 0] = mesh.positions[i0 + 0] + dw * normal[0];
                mesh.positions[i0 + 1] = mesh.positions[i0 + 1] + dw * normal[1];
                mesh.positions[i0 + 2] = mesh.positions[i0 + 2] + dw * normal[2];


            }
        }
    }


    public DeformableMesh3D getShellMesh(){
        DeformableMesh3D outer = mesh;
        double err = 0;
        int max = 128;
        int current = 8;
        DeformableMesh3D inner = DeformableMesh3DTools.copyOf(outer);
        shiftPositionsAlongNormal(inner, -width, current);
        do{
            DeformableMesh3D b = DeformableMesh3DTools.copyOf(outer);
            shiftPositionsAlongNormal(b, -width, current*2);

            err  = difference(inner, b);
            current = current*2;
            inner = b;
            if(current>max){
                System.out.println("Max translation attempts used.");
                break;
            }
        } while( err > tol );
        invertMesh(inner);
        return DeformableMesh3DTools.mergeMeshes(Arrays.asList(outer, inner));
    }
    static public void checkErrorRate(int N){
        DeformableMesh3D standard = RayCastMesh.sphereRayCastMesh(4);
        DeformableMesh3D steps = DeformableMesh3DTools.copyOf(standard);
        DeformableMesh3D stepsx2 = DeformableMesh3DTools.copyOf(standard);

        shiftPositionsAlongNormal(steps, 0.05, N);
        shiftPositionsAlongNormal(stepsx2, 0.05, N*2);

        System.out.println(N + ", " + difference(steps, stepsx2));

    }

    static double difference(DeformableMesh3D a, DeformableMesh3D b){
        double sm = 0;
        for(int i = 0; i<a.positions.length; i++){
            double v = a.positions[i] - b.positions[i];
            sm += v*v;
        }
        return sm;
    }

    static void scanErrors(){
        checkErrorRate(1);
        checkErrorRate(3);
        checkErrorRate(4);
        checkErrorRate(10);
        checkErrorRate(20);
        checkErrorRate(50);
        checkErrorRate(100);
        checkErrorRate(1000);
    }
    public static void main(String[] args){
        MeshFrame3D frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();

        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(4);

        ShellMesh shell = new ShellMesh(mesh);
        DeformableMesh3D invertedMesh = shell.getShellMesh();

        mesh.setColor(new Color(255, 0, 0, 100));
        mesh.create3DObject();

        invertedMesh.create3DObject();

        invertedMesh.setShowSurface(true);
        invertedMesh.setColor(Color.YELLOW);

        frame.addDataObject(mesh.data_object);
        frame.addDataObject(invertedMesh.data_object);


    }

}
