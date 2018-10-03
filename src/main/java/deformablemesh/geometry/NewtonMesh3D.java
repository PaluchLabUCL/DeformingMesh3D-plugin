package deformablemesh.geometry;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.util.Vector3DOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by msmith on 3/4/16.
 */
public class NewtonMesh3D extends DeformableMesh3D {
    public NewtonMesh3D(ArrayList<double[]> node_positions, ArrayList<int[]> connection_indices, ArrayList<int[]> triangle_indices) {
        super(node_positions, connection_indices, triangle_indices);
    }

    public NewtonMesh3D(double[] positions, int[] connection_indices, int[] triangle_indices) {
        super(positions, connection_indices, triangle_indices);
    }
    public NewtonMesh3D(DeformableMesh3D mesh){
        super(mesh.positions, mesh.connection_index, mesh.triangle_index);
    }

    private Map<Node3D, List<Connection3D>> noder;
    private Map<Connection3D, Set<Connection3D>> conner;
    @Override
    public void reshape(){
        noder = getCurvatureMap();
        conner = getAdjacencyMap(noder);
    }

    @Override
    public void update(){
        if(decomp==null){
            reshape();
        }

        final double[] fx = new double[nodes.size()];
        final double[] fy = new double[nodes.size()];
        final double[] fz = new double[nodes.size()];

        for(ExternalEnergy external: energies) {
            external.updateForces(positions, fx, fy, fz);
        }
        if(ALPHA!=0) {
            for (Connection3D c : connections) {

                double[] r = Vector3DOps.difference(c.A.getCoordinates(), c.B.getCoordinates());
                double l = Vector3DOps.normalize(r);
                if (l == 0) {
                    continue;
                }

                //node A
                fx[c.A.index] += -l * ALPHA * r[0];
                fy[c.A.index] += -l * ALPHA * r[1];
                fz[c.A.index] += -l * ALPHA * r[2];

                //node B
                fx[c.B.index] += l * ALPHA * r[0];
                fy[c.B.index] += l * ALPHA * r[1];
                fz[c.B.index] += l * ALPHA * r[2];


            }
        }

        if(BETA!=0){
            for(Node3D n: nodes){
                List<Connection3D> cons = noder.get(n);
                for(int i = 0; i<cons.size(); i++){
                    Connection3D a = cons.get(i);
                    Node3D other = a.A.equals(n)?a.B:a.A;
                    Set<Connection3D> adjacent = conner.get(a);
                    for(int j = i+1; j<cons.size(); j++){
                        //pairs of first order connections.
                        Connection3D b = cons.get(j);
                        if(adjacent.contains(b)){
                            continue;
                        }
                        Node3D another = b.A.equals(n)?b.B:b.A;
                        double[] s1 = Vector3DOps.difference(n.getCoordinates(), other.getCoordinates());
                        double l1 = Vector3DOps.normalize(s1);

                        double[] s2 = Vector3DOps.difference(another.getCoordinates(), n.getCoordinates());
                        double l2 = Vector3DOps.normalize(s2);

                        if(l1==0||l2==0){
                            continue;
                        }

                        double[] sum = Vector3DOps.add(s2, s1, 1);
                        double dot = Vector3DOps.dot(s1, s2);
                        if(dot+1==0){
                            continue;
                        }
                            double factor = 0.5*(dot - 1)/(dot+1);

                        double[] delta = Vector3DOps.difference(s2, s1);

                        fx[n.index] += BETA*delta[0];
                        fy[n.index] += BETA*delta[1];
                        fz[n.index] += BETA*delta[2];

                        fx[other.index] += BETA*(sum[0]*factor - 0.5*delta[0]);
                        fy[other.index] += BETA*(sum[1]*factor - 0.5*delta[1]);
                        fz[other.index] += BETA*(sum[2]*factor - 0.5*delta[2]);

                        fx[another.index] -= BETA*(sum[0]*factor + 0.5*delta[0]);
                        fy[another.index] -= BETA*(sum[1]*factor + 0.5*delta[1]);
                        fz[another.index] -= BETA*(sum[2]*factor + 0.5*delta[2]);

                    }

                }

            }
        }


        double drag = 1/GAMMA;
        for(int i = 0; i<fx.length; i++){
            positions[i*3] += fx[i]*drag;
            positions[i*3+1] += fy[i]*drag;
            positions[i*3+2] += fz[i]*drag;

        }
        if(data_object!=null){
            data_object.updateGeometry(positions);
        }
    }
}
