package deformablemesh.externalenergies;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.util.Vector3DOps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class used for calculating steric forces between meshes.
 */
public class StericMesh implements ExternalEnergy{
    InterceptingMesh3D mesh;
    final DeformableMesh3D deformableMesh;
    final DeformableMesh3D id;
    final double weight;
    boolean staticShape = true;
    Map<Integer, Set<Triangle3D>> map = new HashMap<>();
    public StericMesh(DeformableMesh3D id, DeformableMesh3D neighbor, double weight){
        //mesh = new InterceptingMesh3D(a);
        deformableMesh = neighbor;
        this.id = id;
        this.weight=weight;
        if(id.triangles==null){
            System.out.println(id.positions.length + ", " + id.connection_index.length + ", " + id.triangle_index.length);
        }
        for(Triangle3D t: id.triangles){
            int[] dexs = t.getIndices();
            for(Integer i: dexs){
                if(!map.containsKey(i)){
                    map.put(i, new HashSet<>());
                }
                map.get(i).add(t);
            }
        }
    }
    public void update(){
        mesh = new InterceptingMesh3D(deformableMesh);
    }


    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        if(!staticShape || mesh==null) {
            mesh = new InterceptingMesh3D(deformableMesh);
        }


        double[] pt = new double[3];
        double[] center = mesh.getCenter();
        for(int i = 0; i<fx.length; i++){
            pt[0] = positions[3*i];
            pt[1] = positions[3*i + 1];
            pt[2] = positions[3*i + 2];
            double[] normal = new double[3];

            if(mesh.contains(pt)){
                double dx = pt[0] - center[0];
                double dy = pt[1] - center[1];
                double dz = pt[2] - center[2];
                double l = Math.sqrt(dx*dx + dy*dy + dz*dz);

                double norm = getNormal(i, normal);
                if(norm==0){
                    continue;
                }
                double dot = dx*normal[0] + dy*normal[1] + dz*normal[2];
                dot = dot<0?-1:1;

                fx[i] += dot*weight*normal[0];
                fy[i] += dot*weight*normal[1];
                fz[i] += dot*weight*normal[2];
            }


        }
    }

    /**
     * Calculates the normal by averaging the normals of the connecting triangles.
     *
     * @param i node index
     * @param result where the normal will be recorded.
     * @return the magnitude of the summed normal.
     */
    public double getNormal(Integer i, double[] result){
        result[0] = 0;
        result[1] = 0;
        result[2] = 0;
        Set<Triangle3D> triangles = map.get(i);
        for(Triangle3D t: triangles){
            result[0] += t.normal[0];
            result[1] += t.normal[1];
            result[2] += t.normal[2];
        }
        double n = 1.0/triangles.size();
        result[0] *= n;
        result[1] *= n;
        result[2] *= n;
        double norm = Vector3DOps.normalize(result);

        if(norm==0){
            result[0] = 0;
            result[1] = 0;
            result[2] = 0;
        }

        return norm;

    }



    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
