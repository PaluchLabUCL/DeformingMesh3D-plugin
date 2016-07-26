package deformablemesh.externalenergies;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.List;

/**
 * Created by msmith on 2/26/16.
 */
public class TriangleAreaDistributor implements ExternalEnergy{
    List<Triangle3D> triangles;
    double ds;
    double weight;
    static int counter = 0;
    DeformableMesh3D mesh;
    public TriangleAreaDistributor(MeshImageStack stack, DeformableMesh3D mesh, double weight){
        triangles = mesh.triangles;
        //ds = stack.getMinPx();
        this.weight = weight;
        this.mesh =mesh;
    }
    final static double areaFactor = Math.sqrt(4/Math.sqrt(3));
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double ave = 0;
        for(Triangle3D triangle: triangles){
            triangle.update();
            ave += triangle.area;
        }
        ave = ave/triangles.size();
        double anot = areaFactor*Math.sqrt(ave);

        double[] s = new double[3];
        double[] f1=new double[3], f2=new double[3], f3 = new double[3];
        int[] i = new int[3];
        double r, force;
        for(Triangle3D triangle: triangles){
            triangle.getIndices(i);

            //b to a
            s[0] = positions[3*i[0]] - positions[3*i[1]];
            s[1] = positions[3*i[0]+1] - positions[3*i[1]+1];
            s[2] = positions[3*i[0]+2] - positions[3*i[1]+2];

            r = Vector3DOps.normalize(s);
            force = (anot - r)*weight;

            f1[0] = force*s[0];
            f1[1] = force*s[1];
            f1[2] = force*s[2];

            f2[0] = -force*s[0];
            f2[1] = -force*s[1];
            f2[2] = -force*s[2];

            //c to b
            s[0] = positions[3*i[1]] - positions[3*i[2]];
            s[1] = positions[3*i[1]+1] - positions[3*i[2]+1];
            s[2] = positions[3*i[1]+2] - positions[3*i[2]+2];

            r = Vector3DOps.normalize(s);
            force = (anot - r)*weight;

            f2[0] += force*s[0];
            f2[1] += force*s[1];
            f2[2] += force*s[2];

            f3[0] = -force*s[0];
            f3[1] = -force*s[1];
            f3[2] = -force*s[2];

            //a to c
            s[0] = positions[3*i[2]] - positions[3*i[0]];
            s[1] = positions[3*i[2]+1] - positions[3*i[0]+1];
            s[2] = positions[3*i[2]+2] - positions[3*i[0]+2];

            r = Vector3DOps.normalize(s);
            force = (anot - r)*weight;

            f3[0] += force*s[0];
            f3[1] += force*s[1];
            f3[2] += force*s[2];

            f1[0] -= force*s[0];
            f1[1] -= force*s[1];
            f1[2] -= force*s[2];

            fx[i[0]] += f1[0];
            fy[i[0]] += f1[1];
            fz[i[0]] += f1[2];

            fx[i[1]] += f2[0];
            fy[i[1]] += f2[1];
            fz[i[1]] += f2[2];

            fx[i[2]] += f3[0];
            fy[i[2]] += f3[1];
            fz[i[2]] += f3[2];

        }

    }
    static ImagePlus getGaussianSpot(){
        int w = 400;
        int h = 400;
        double s = 10000;
        ImageStack stack = new ImageStack(w, h);
        for(int slice = 0; slice<3; slice++) {
            short[] pixels = new short[400 * 400];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {

                    int dex = j * w + i;
                    double r = Vector3DOps.square(i - w / 2) + Vector3DOps.square(j - h / 2);
                    pixels[dex] = (short) (Short.MAX_VALUE * (Math.exp(-0.5 * r / s)));


                }
            }
            ImageProcessor proc = new ShortProcessor(w, h, pixels, null );
            stack.addSlice(proc);
        }

        return new ImagePlus("spot", stack);
    }
    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }

    public static void main(String[] args){
        double l = 1;
        double w = 1;

        double[] pos = {
           -0.5*w, 0.5*l, 0,         0.5*w, 0.5*l, 0,
                         0, 0, 0,
           -0.5*w, -0.5*l, 0,        0.5*w, -0.5*l, 0
        };
        int[] con = {
                0, 1,
                0, 2,
                0, 3,
                1, 2,
                1, 4,
                2, 3,
                2, 4,
                3, 4
        };

        int[] triangles = {
                0, 1, 2,
                0, 2, 3,
                1, 4, 2,
                2, 4, 3
        };


        DeformableMesh3D mesh = new DeformableMesh3D(pos, con, triangles);
        //NewtonMesh3D mesh = new NewtonMesh3D(pos, con, triangles);
        int subdived = 4;
        for(int i = 0; i<subdived; i++){
            RayCastMesh.subDivideMesh(mesh);
        }

        MeshFrame3D frame = new MeshFrame3D();
        ImagePlus spot = getGaussianSpot();
        spot.show();
        MeshImageStack stack = new MeshImageStack(spot);
        frame.showFrame(true);
        frame.hideAxis();
        mesh.create3DObject();
        frame.addDataObject(mesh.data_object);
        mesh.ALPHA = 0.1;
        mesh.BETA = 0;
        mesh.GAMMA=1500;
        mesh.reshape();
        /*mesh.addExternalEnergy(new NodeAttractor(mesh.nodes.get(0), 1000));
        mesh.addExternalEnergy(new NodeAttractor(mesh.nodes.get(1), 1000));
        mesh.addExternalEnergy(new NodeAttractor(mesh.nodes.get(3), 1000));
        mesh.addExternalEnergy(new NodeAttractor(mesh.nodes.get(4), 1000));
        */
        //mesh.addExternalEnergy(new IntensityEnergy(stack, 0.001));
        //mesh.addExternalEnergy(new GradientEnergy(stack, 0.001));
        for(Node3D node: mesh.nodes){

            double[] p = node.getCoordinates();
            if(p[0]==0.5*w||p[0]==-0.5*w||p[1]==0.5*l||p[1]==-0.5*l){
                mesh.addExternalEnergy(new NodeAttractor(node, 1000));
            }

        }
        //mesh.addExternalEnergy(new NodeAttractor(mesh.nodes.get(2), 1000));
        //mesh.addExternalEnergy(new TriangleAreaDistributor(null, mesh, 1));
        //mesh.addExternalEnergy(new PressureForce(mesh, -0.1));
        while(true){
            mesh.update();
            counter++;
            mesh.calculateCurvature();
        }
    }
}
