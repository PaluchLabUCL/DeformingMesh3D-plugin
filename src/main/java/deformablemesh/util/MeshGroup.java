package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.DeformableMesh3D;

import java.util.List;

public class MeshGroup {
    public double volume;
    public double[] cm;
    private List<DeformableMesh3D> meshes;

    public MeshGroup(List<DeformableMesh3D> meshes) {
        cm = new double[3];
        for (DeformableMesh3D mesh : meshes) {
            double v0 = mesh.calculateVolume();
            double[] c = DeformableMesh3DTools.centerAndRadius(mesh.nodes);
            cm[0] += c[0] * v0;
            cm[1] += c[1] * v0;
            cm[2] += c[2] * v0;
            volume += v0;
        }

        cm[0] = cm[0] / volume;
        cm[1] = cm[1] / volume;
        cm[2] = cm[2] / volume;
        this.meshes = meshes;
    }


    public void translate(double[] dx) {
        meshes.forEach(m -> translate(dx));
    }

    public void rotateMeshes(double[] axisAngle, double[] center) {
        double[] axis = new double[3];
        axis[0] = axisAngle[0];
        axis[1] = axisAngle[1];
        axis[2] = axisAngle[2];
        double angle = axisAngle[3];
        double mag = Vector3DOps.normalize(axis);
        meshes.forEach(m -> m.rotate(axis, center, angle));
    }

}
