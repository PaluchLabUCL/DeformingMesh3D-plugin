package deformablemesh.geometry;

import deformablemesh.MeshImageStack;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Utility class for generating a mesh. This will use 2 different techniques. One based on a
 * binary image and one based on
 *
 * User: msmith
 * Date: 11/20/13
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class RayCastMesh {
    public static DeformableMesh3D fiveTriangleSphere(){
        ArrayList<double[]> points = new ArrayList<double[]>();
        ArrayList<int[]> connections = new ArrayList<int[]>();
        ArrayList<int[]> triangles = new ArrayList<int[]>();

        points.add(new double[]{0, 0 ,1});

        for(int i = 0; i<5; i++){

            double phi = Math.PI/3.0;
            double theta = 2*Math.PI/5.0*i;
            double z = Math.cos(phi);
            double rho = Math.sin(phi);

            double x = Math.cos(theta)*rho;
            double y = Math.sin(theta)*rho;

            points.add(new double[]{x,y,z});
            connections.add(new int[]{0,i+1});
        }

        for(int i = 0; i<5; i++){
            int first = i+1;
            int second = (i+1)%5 + 1;
            connections.add(new int[]{first, second});
            triangles.add(new int[]{0,first,second});
        }

        for(int i = 0; i<5; i++){

            double phi = 2*Math.PI/3.0;
            double theta = 2*Math.PI/5.0*(i+0.5);
            double z = Math.cos(phi);
            double rho = Math.sin(phi);

            double x = Math.cos(theta)*rho;
            double y = Math.sin(theta)*rho;

            points.add(new double[]{x,y,z});
            int above_behind = i+1;
            int above_forward = (i+1)%5 + 1;
            int current = i+6;
            connections.add(new int[]{above_behind,current});
            connections.add(new int[]{above_forward,current});
            triangles.add(new int[]{current, above_forward, above_behind});

            int next = (i+1)%5 + 6;
            connections.add(new int[]{current, next});
            triangles.add(new int[]{current, next, above_forward});

        }

        points.add(new double[]{0,0,-1});

        for(int i = 0; i<5; i++){
            int last = points.size()-1;
            int current = i+6;
            int next = (i+1)%5 + 6;
            connections.add(new int[]{current, last});
            triangles.add(new int[]{current, last, next});

        }

        return new DeformableMesh3D(points, connections, triangles);

    }

    public static DeformableMesh3D rayCastMesh(Interceptable object, double[] origin, int divisions){
        DeformableMesh3D start = sphereRayCastMesh(divisions);
        for(Node3D node: start.nodes){

            List<Intersection> intersections = object.getIntersections(origin, node.getCoordinates());
            Intersection best = bestIntersectionSpherical(intersections, origin, node.getCoordinates());
            node.setPosition(best.location);

        }

        return start;
    }

    /**
     * Checks for interceptsion of all of the included objects. The closest intersection of independent objects is where
     * the point is placed. Or it is left at the origin.
     *
     * @param objects
     * @param origin
     * @param divisions
     * @return
     */
    public static DeformableMesh3D rayCastMesh(List<Interceptable> objects, double[] origin, int divisions){

        DeformableMesh3D start = sphereRayCastMesh(divisions);

        for(Node3D node: start.nodes){
            double min = Double.POSITIVE_INFINITY;
            double[] loc = origin;
            for(Interceptable object: objects) {
                List<Intersection> intersections = object.getIntersections(origin, node.getCoordinates());

                Intersection best = bestIntersectionSpherical(intersections, origin, node.getCoordinates());

                double sep = Vector3DOps.distance(best.location, origin);
                if(sep<min){
                    loc = best.location;
                    min = sep;
                }
            }
            node.setPosition(loc);

        }

        return start;
    }


    /**
     * Find the furthest away exiting node. If there were no intersections, then INFINITY is used.
     *
     * @param ints already found intersection locations.
     * @param origin center of the raycast
     * @param normal direction of the ray
     * @return The farthest away outwardly facing ray/intersection.
     */
    static Intersection bestIntersectionSpherical(List<Intersection> ints, double[] origin, double[] normal){
        if(ints.size()==0){
            return Intersection.inf(normal);
        }
        Intersection best=null;
        double b = 0;
        for(Intersection section: ints){
            double[] distance = Vector3DOps.difference(section.location, origin);
            double dot = Vector3DOps.dot(distance, normal);

            //check if it is a forward travelling ray.
            if(dot>=0){
                //now check the normals.
                double s = Vector3DOps.dot(normal, section.surfaceNormal);
                if(s>0){
                    if(dot>=b){
                        best = section;
                        b = dot;
                    }
                }
            }
        }

        if(best==null){
            best= Intersection.inf(normal);
        }

        return best;

    }

    public static DeformableMesh3D sixTriangleSphere(){
        ArrayList<double[]> points = new ArrayList<double[]>();
        ArrayList<int[]> connections = new ArrayList<int[]>();
        ArrayList<int[]> triangles = new ArrayList<int[]>();

        points.add(new double[]{0, 0 ,1});

        for(int i = 0; i<6; i++){

            double phi = Math.PI/3.0;
            double theta = 2*Math.PI/6.0*i;
            double z = Math.cos(phi);
            double rho = Math.sin(phi);

            double x = Math.cos(theta)*rho;
            double y = Math.sin(theta)*rho;

            points.add(new double[]{x,y,z});
            connections.add(new int[]{0,i+1});
        }

        for(int i = 0; i<6; i++){
            int first = i+1;
            int second = (i+1)%6 + 1;
            connections.add(new int[]{first, second});
            triangles.add(new int[]{0,first,second});
        }

        for(int i = 0; i<6; i++){

            double phi = 2*Math.PI/3.0;
            double theta = 2*Math.PI/6.0*(i+0.5);
            double z = Math.cos(phi);
            double rho = Math.sin(phi);

            double x = Math.cos(theta)*rho;
            double y = Math.sin(theta)*rho;

            points.add(new double[]{x,y,z});
            int above_behind = i+1;
            int above_forward = (i+1)%6 + 1;
            int current = i+7;
            connections.add(new int[]{above_behind,current});
            connections.add(new int[]{above_forward,current});
            triangles.add(new int[]{current, above_forward, above_behind});

            int next = (i+1)%6 + 7;
            connections.add(new int[]{current, next});
            triangles.add(new int[]{current, next, above_forward});

        }

        points.add(new double[]{0,0,-1});

        for(int i = 0; i<6; i++){
            int last = points.size()-1;
            int current = i+7;
            int next = (i+1)%6 + 7;
            connections.add(new int[]{current, last});
            triangles.add(new int[]{current, last, next});

        }

        return new DeformableMesh3D(points, connections, triangles);

    }


    public static DeformableMesh3D sphereRayCastMesh(int divisions){

        DeformableMesh3D sphere = fiveTriangleSphere();
        for(int i = 0; i<divisions; i++) subDivideMesh(sphere);

        for(Node3D node: sphere.nodes){
            double[] pos = node.getCoordinates();
            double mag = Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
            if(mag==0) continue;
            mag = 1.0/mag;
            pos[0] = pos[0]*mag;
            pos[1] = pos[1]*mag;
            pos[2] = pos[2]*mag;

            node.setPosition(pos);

        }


        return sphere;

    }

    /**
     * Quadruple the number of triangles in a mesh.
     * @param mesh
     */
    public static void subDivideMesh(DeformableMesh3D mesh){

        int original_points = mesh.nodes.size();

        int n_con = mesh.connections.size();

        double[] new_data = new double[n_con*3 + original_points*3];

        System.arraycopy(mesh.positions,0,new_data,0, mesh.positions.length);

        mesh.updatePositionBuffer(new_data);

        HashMap<NodeKey, Node3D> new_nodes = new HashMap<NodeKey, Node3D>();

        ArrayList<Connection3D> new_connections = new ArrayList<Connection3D>();

        for(int i = 0; i<mesh.connections.size(); i++){
            Connection3D con = mesh.connections.get(i);
            double[] a = con.A.getCoordinates();
            double[] b = con.B.getCoordinates();

            double[] c = new double[]{
                    0.5*(a[0] + b[0]),
                    0.5*(a[1] + b[1]),
                    0.5*(a[2] + b[2])
            };

            Node3D n = new Node3D(new_data, mesh.nodes.size());
            mesh.nodes.add(n);
            n.setPosition(c);

            new_nodes.put(new NodeKey(con.A.index, con.B.index), n);

            Connection3D aCon = new Connection3D(con.A, n);
            Connection3D bCon = new Connection3D(n, con.B);

            new_connections.add(aCon);
            new_connections.add(bCon);

        }
        mesh.connections.clear();
        mesh.connections.addAll(new_connections);

        ArrayList<Triangle3D> new_triangles = new ArrayList<Triangle3D>();
        for(Triangle3D triangle: mesh.triangles){

            Node3D a = new_nodes.get(new NodeKey(triangle.A, triangle.B));
            Node3D b = new_nodes.get(new NodeKey(triangle.B, triangle.C));
            Node3D c = new_nodes.get(new NodeKey(triangle.C, triangle.A));

            new_triangles.add(new Triangle3D(triangle.A, a, c));
            new_triangles.add(new Triangle3D(a, triangle.B, b));
            new_triangles.add(new Triangle3D(c, b, triangle.C));
            new_triangles.add(new Triangle3D(a, b, c));

            //triangle.A = a;
            //triangle.B = b;
            //triangle.C = c;

            mesh.connections.add(new Connection3D(a,b));
            mesh.connections.add(new Connection3D(b,c));
            mesh.connections.add(new Connection3D(c,a));

        }

        mesh.triangles.clear();
        mesh.triangles.addAll(new_triangles);

        mesh.syncConnectionIndices();
        mesh.syncTriangleIndices();

    }

    public static void main(String[] args){
        MeshFrame3D frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.showAxis();
        DeformableMesh3D mesh = sphereRayCastMesh(2);
        mesh.create3DObject();
        frame.addDataObject(mesh.data_object);

        mesh.calculateStress();
        mesh.calculateCurvature();


    }



    public static DeformableMesh3D sphericalRayCastBinaryStack(MeshImageStack stack, int divisions) {
        DeformableMesh3D mesh = sphereRayCastMesh(divisions);

        double max_x = stack.offsets[0];
        double max_y = stack.offsets[1];
        double max_z = stack.offsets[2];

        double[] pos = new double[3];
        double[] center = stack.getCenterOfMass();
        if(!(stack.getInterpolatedValue(center)>0)){
            return mesh;
        }

        for(Node3D node: mesh.nodes){

            //the sphere is normalized so the coordinates are the direction.
            double[] dir = node.getCoordinates();

            //find the starting point.
            double x_scale = max_x/dir[0];
            x_scale = Double.isNaN(x_scale)?Double.MAX_VALUE:Math.abs(x_scale);
            double y_scale = max_y/dir[1];
            y_scale = Double.isNaN(y_scale)?Double.MAX_VALUE:Math.abs(y_scale);
            double z_scale = max_z/dir[2];
            z_scale = Double.isNaN(z_scale)?Double.MAX_VALUE:Math.abs(z_scale);

            double max_length = Math.abs(x_scale)>Math.abs(y_scale)?y_scale:x_scale;
            max_length = z_scale>max_length?max_length:z_scale;



            pos[0] = max_length*dir[0];
            pos[1] = max_length*dir[1];
            pos[2] = max_length*dir[2];

            double value = stack.getInterpolatedValue(pos);
            if(value>0){
                node.setPosition(pos);
                continue;
            }

            boolean scanning = true;
            double last = max_length;

            double length = max_length;
            double min_length = max_length/500;

            while(scanning){
                length -= min_length;
                pos[0] = length*dir[0] + center[0];
                pos[1] = length*dir[1] + center[1];
                pos[2] = length*dir[2] + center[2];
                double new_value = stack.getInterpolatedValue(pos);
                if(new_value>0){
                    //good
                    length += min_length;
                    scanning = false;
                } else{
                    //keep going.
                }
                if(length>=max_length) scanning=false;

            }

            last = length;

            pos[0] = dir[0]*last + center[0];
            pos[1] = dir[1]*last + center[1];
            pos[2] = dir[2]*last + center[2];

            node.setPosition(pos);

        }
        return mesh;




    }


}

/**
 * For mapping Node3D to a Connection3D that it split during subdivision. The order does not matter, the
 * indexes are sorted
 */
class NodeKey{
    int a;
    int b;

    /**
     * Connects to nodes to each other by index.
     *
     * @param a index of first node
     * @param b index of second node
     */
    NodeKey(int a, int b){
        if(a==b)
            throw new IllegalArgumentException("Cannot connect the same nodes");
        this.a = a>b?a:b;
        this.b = a>b?b:a;
    }

    /**
     * Connects each node.
     *
     * @param A .
     * @param B .
     */
    NodeKey(Node3D A, Node3D B){
        int a = A.index;
        int b = B.index;
        if(a==b)
            throw new IllegalArgumentException("Cannot connect the same nodes");
        this.a = a>b?a:b;
        this.b = a>b?b:a;
    }

    @Override
    public int hashCode(){
        return a + (b<<16);
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof NodeKey){
            NodeKey onk = (NodeKey)obj;
            return (onk.a==a&&onk.b==b);
        }

        return false;
    }


}