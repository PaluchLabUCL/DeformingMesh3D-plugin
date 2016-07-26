package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by msmith on 4/21/16.
 */
public class DeformableMesh3DTests {
    final static double TOL = 1e-8;
    @Test
    public void volumeTest(){
        DeformableMesh3D mesh = DeformableMesh3DTools.createTestBlock();

        double v1 = mesh.calculateVolume(new double[]{1, 0, 0});

        double v2 = mesh.calculateVolume(new double[]{0,1,0});
        double v3 = mesh.calculateVolume(new double[]{0, 0, 1});

        Assert.assertEquals(v1, v2, TOL);
        Assert.assertEquals(v1, v3, TOL);

    }

    @Test
    public void testMeshConstructor(){
        DeformableMesh3D mesh = DeformableMesh3DTools.createTestBlock();
        DeformableMesh3D mesh2 = new DeformableMesh3D(mesh.positions, mesh.connection_index, mesh.triangle_index);

        for(int i = 0; i<mesh.triangles.size(); i++){
            Assert.assertEquals(mesh.triangles.get(i), mesh2.triangles.get(i));
        }

    }

    @Test
    public void testCurvature(){

        DeformableMesh3D xMesh = DeformableMesh3D.generateEdgeX();
        xMesh.scale(0.5, DeformableMesh3D.ORIGIN);
        xMesh.translate(new double[]{1, 0, 0});
        List<double[]> xCurves = xMesh.calculateCurvature();

        DeformableMesh3D yMesh = DeformableMesh3D.generateEdgeY();
        yMesh.scale(0.5, DeformableMesh3D.ORIGIN);
        yMesh.translate(new double[]{2, 1, 0});
        yMesh.calculateCurvature();
        List<double[]> yCurves = yMesh.calculateCurvature();

        for(int i = 0; i<xCurves.size(); i++){
            Assert.assertArrayEquals(xCurves.get(i), yCurves.get(i), TOL);
        }

        DeformableMesh3D zMesh = DeformableMesh3D.generateEdgeZ();
        zMesh.scale(0.5, DeformableMesh3D.ORIGIN);
        zMesh.translate(new double[]{0, 0, -1});
        zMesh.create3DObject();
        zMesh.calculateCurvature();

    }

}
