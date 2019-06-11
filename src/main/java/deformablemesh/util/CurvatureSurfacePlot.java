package deformablemesh.util;

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;

import java.util.List;

/**
 * Created by msmith on 05/06/19.
 */
public class CurvatureSurfacePlot extends SurfacePlot{
    List<double[]> values;
    public CurvatureSurfacePlot(DeformableMesh3D mesh){
        this.mesh = mesh;
    }

    void generateCurvatures(){
        CurvatureCalculator calc = new CurvatureCalculator(mesh);
        values = calc.calculateCurvature();

    }

    @Override
    public double sample(Node3D node) {
        if(values == null){
            generateCurvatures();
        }
        return values.get(node.index)[3];
    }
}
