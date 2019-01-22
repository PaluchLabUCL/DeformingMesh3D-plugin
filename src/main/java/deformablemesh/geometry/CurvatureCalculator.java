package deformablemesh.geometry;

import deformablemesh.util.Vector3DOps;

import java.util.*;

public class CurvatureCalculator {
    DeformableMesh3D mesh;
    Map<Node3D, List<Triangle3D>> node_to_triangle = new HashMap<>();
    double min_curv = -1;
    double max_curv = -1;

    public CurvatureCalculator(DeformableMesh3D mesh){
        this.mesh = mesh;
    }

    private void addNode(Node3D n, Triangle3D t, Map<Node3D, List<Triangle3D>> map){
        if(!map.containsKey(n)){
            map.put(n,new ArrayList<>());
        }
        map.get(n).add(t);
    }

    /**
     * Checks if triangles a and b share a connection via node n. It is assumed that both triangles contain
     * n so just the second node is being looked for.
     *
     * @param n the node shared by both triangles.
     * @param a one triangle
     * @param b one triangle
     * @return the other node both triangles share.
     */
    static private boolean nodeSharesCommonEdge(Node3D n, Triangle3D a, Triangle3D b){
        Node3D[] as = new Node3D[]{a.A,a.B,a.C};
        Node3D[] bs = new Node3D[]{b.A,b.B,b.C};
        for(int i = 0; i<3; i++){
            if(as[i]==n){
                continue;
            }
            for(int j = 0; j<3; j++){
                if(bs[j]==n){
                    continue;
                }
                if(as[i]==bs[j]){
                    return true;
                }
            }

        }
        return false;
    }

    public List<double[]> calculateCurvature(){
        List<double[]> values = new ArrayList<>();


        for(Triangle3D tri: mesh.triangles){
            tri.update();
            addNode(tri.A, tri, node_to_triangle);
            addNode(tri.B, tri, node_to_triangle);
            addNode(tri.C, tri, node_to_triangle);
        }


        for(Node3D node: mesh.nodes){
            List<Triangle3D> t_angles = node_to_triangle.get(node);
            //all touching triangles.
            if(t_angles==null) continue;

            double[] cs = new double[t_angles.size()];
            double[] as = new double[t_angles.size()];

            for(int i = 0; i<t_angles.size(); i++){

                Triangle3D triangle = t_angles.get(i);
                as[i] = triangle.area;
                for(int j = 0; j<t_angles.size(); j++){
                    if(i==j) continue;
                    Triangle3D other = t_angles.get(j);
                    if(nodeSharesCommonEdge(node, triangle, other)){
                        cs[i] += calculateCurvature(node, triangle, other);
                    }
                }


            }
            double curvature = 0;
            double area = 0;
            for(int i = 0; i<t_angles.size(); i++){
                curvature += cs[i]*as[i];
                area += as[i];
            }
            double[] pt = node.getCoordinates();
            curvature = curvature/area;

            values.add(new double[]{pt[0], pt[1], pt[2], curvature});



        }

        return values;
    }


    private double calculateCurvature(Node3D p, Triangle3D A, Triangle3D B){


        double[] dt = Vector3DOps.difference(A.center, B.center);
        double[] dnormal = Vector3DOps.difference(A.normal, B.normal);

        double dot = Vector3DOps.dot(dt,dnormal);

        double[] cross = Vector3DOps.cross(A.normal, B.normal);

        double mag = Math.sqrt(cross[0]*cross[0] + cross[1]*cross[1] + cross[2]*cross[2]);
        mag = mag<1e-16?0:mag;
        //double ds = Math.sqrt(dot(dt,dt))

        return dot<0?-mag:mag;

    }

    public List<double[]> createCurvatureHistogram(List<double[]> curvatures){
        int N = 50;
        double[] x = new double[N];
        double[] y = new double[N];

        if(min_curv==max_curv){
            min_curv = curvatures.get(0)[3];
            max_curv = min_curv;
            for(double[] row: curvatures){
                if(row[3]<min_curv){
                    min_curv = row[3];
                }
                if(row[3]>max_curv){
                    max_curv = row[3];
                }
            }
        }
        double dK = (max_curv - min_curv)/N;
        for(int i = 0; i<N; i++){
            x[i] = (i+0.5)*dK + min_curv;
        }
        for(double[] row: curvatures){
            int index = (int)((row[3] - min_curv)/(dK));

            if(index == N && row[3]==max_curv){
                index = N-1;
            }

            if(index>=N || index<0){
                continue;
            }

            y[index] += 1;
        }


        return Arrays.asList(x,y);
    }

    public List<double[]> createCurvatureHistogram(){

        List<double[]> curvatures = calculateCurvature();
        return createCurvatureHistogram(curvatures);
    }

    public void setMinCurvature(double i) {
        min_curv = i;
    }

    public void setMaxCurvature(double m){
        max_curv = m;
    }
}
