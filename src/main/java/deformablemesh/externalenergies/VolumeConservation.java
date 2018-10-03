package deformablemesh.externalenergies;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Triangle3D;

public class VolumeConservation implements ExternalEnergy{
    final DeformableMesh3D mesh;
    final double weight;
    final double[] counters;
    double volume;
    public VolumeConservation(DeformableMesh3D mesh, double weight){
        this.mesh = mesh;
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
        volume = mesh.calculateVolume(new double[]{0, 0, 1});

        this.weight = weight*0.1/volume;



    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double nv = mesh.calculateVolume(new double[]{0, 0, 1});

        int[] indexes = new int[3];
        double dv = weight*(-nv + volume);
        double factor = dv;
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

                fx[a] += f_x*counters[a];
                fy[a] += f_y*counters[a];
                fz[a] += f_z*counters[a];

            pt[0] = positions[3*b];
            pt[1] = positions[3*b + 1];
            pt[2] = positions[3*b + 2];

                fx[b] += f_x*counters[b];
                fy[b] += f_y*counters[b];
                fz[b] += f_z*counters[b];

            pt[0] = positions[3*c];
            pt[1] = positions[3*c + 1];
            pt[2] = positions[3*c + 2];

                fx[c] += f_x*counters[c];
                fy[c] += f_y*counters[c];
                fz[c] += f_z*counters[c];



        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
