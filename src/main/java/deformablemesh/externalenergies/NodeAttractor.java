package deformablemesh.externalenergies;

import deformablemesh.geometry.Node3D;

/**
 * Created by msmith on 2/29/16.
 */
public class NodeAttractor implements ExternalEnergy{
    int index;
    double[] position;
    double[] delta = new double[3];
    double weight;
    public NodeAttractor(Node3D node, double weight){
        index = node.getIndex();
        this.weight = weight;
        position = node.getCoordinates();
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        fx[index] += weight*(position[0] - positions[3*index]);
        fy[index] += weight*(position[1] - positions[3*index+1]);
        fz[index] += weight*(position[2] - positions[3*index+2]);
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}
