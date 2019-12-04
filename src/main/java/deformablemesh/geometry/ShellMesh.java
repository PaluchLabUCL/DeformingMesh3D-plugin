package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;

import java.util.Arrays;
import java.util.List;

/**
 * Creates a 'shell' mesh, which is essentially the original mesh + a mesh created by
 * shifting the nodes along the normal at the node a small displacement.
 *
 */
public class ShellMesh {
    double width = 0.01;
    DeformableMesh3D mesh;
    public ShellMesh(DeformableMesh3D mesh){
        this.mesh = mesh;

    }

    public DeformableMesh3D getShellMesh(){
        CurvatureCalculator calculator = new CurvatureCalculator(mesh);

        DeformableMesh3D outer = new DeformableMesh3D(mesh.positions, mesh.triangle_index, mesh.connection_index);
        double[] inner_positions = Arrays.copyOf(mesh.positions, mesh.positions.length);
        for(int i = 0; i<mesh.nodes.size(); i++){
            int i0 = 3*i;
            double[] normal = calculator.getNormal(i);
            inner_positions[i0 + 0] = inner_positions[i0 + 0] - width*normal[0];
            inner_positions[i0 + 1] = inner_positions[i0 + 1] - width*normal[1];
            inner_positions[i0 + 2] = inner_positions[i0 + 2] - width*normal[2];
        }
        int[] triangles = Arrays.copyOf(mesh.triangle_index, mesh.triangle_index.length);
        for(int i = 0; i<mesh.triangles.size(); i++){
            int i0 = 3*i;
            int a = triangles[i0];
            triangles[i0] = triangles[i0 + 1];
            triangles[i0+1] = a;
        }
        DeformableMesh3D inner = new DeformableMesh3D(inner_positions, mesh.connection_index, triangles);

        return DeformableMesh3DTools.mergeMeshes(Arrays.asList(outer, inner));
    }

}
