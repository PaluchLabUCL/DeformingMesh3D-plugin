package deformablemesh.geometry;

import java.util.Arrays;

public class MeshInverter {
    /**
     * Creates a mesh where outside is inside and inside is out.
     * @param mesh mesh to be inverted.
     *
     * @return copy of the inverted mesh.
     */
    public static DeformableMesh3D invertMesh(DeformableMesh3D mesh){
        final double[] pts = Arrays.copyOf(mesh.positions, mesh.positions.length);
        final int[] con = Arrays.copyOf(mesh.connection_index, mesh.connection_index.length);
        final int[] tri = Arrays.copyOf(mesh.triangle_index, mesh.triangle_index.length);
        for(int i = 0; i<tri.length/3; i++){

            int s = tri[3*i];
            tri[3*i] = tri[3*i+1];
            tri[3*i + 1] = s;

        }
        return new DeformableMesh3D(pts, con, tri);

    }
}
