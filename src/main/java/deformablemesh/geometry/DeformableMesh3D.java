package deformablemesh.geometry;

import Jama.LUDecomposition;
import Jama.Matrix;
import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.meshview.DeformableMeshDataObject;
import deformablemesh.util.Vector3DOps;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 *
 * 3D mesh that will be used for segmenting cells in 3D. A collection of Nodes, triangles and connections that represent
 * a mesh in 3D
 *
 * User: msmith
 * Date: 7/2/13
 * Time: 7:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeformableMesh3D{
    public double GAMMA;
    public double ALPHA;
    public double BETA;
    LUDecomposition decomp;

    List<ExternalEnergy> energies = new ArrayList<>();

    public List<Node3D> nodes;
    public List<Triangle3D> triangles;
    public List<Connection3D> connections;
    public double[] positions;
    public int[] connection_index;
    public int[] triangle_index;

    public DeformableMeshDataObject data_object;
    private final static ExecutorService pool = Executors.newFixedThreadPool(3);
    public static final double[] ORIGIN = {0,0,0};

    private boolean showSurface;
    private Color color = Color.BLUE;
    private boolean selected;

    /**
     * Creates a deformable mesh in 3d.
     *
     * @param node_positions collection of double arrays were each array should be {x,y,z}
     * @param connection_indices collection of indexes that indicate connections between nodes.
     */
    public DeformableMesh3D(List<double[]> node_positions, List<int[]> connection_indices, List<int[]> triangle_indices){
        nodes = new ArrayList<>(node_positions.size());
        triangles = new ArrayList<>();
        connections = new ArrayList<>(connection_indices.size());
        positions = new double[node_positions.size()*3];
        connection_index = new int[2*connection_indices.size()];
        triangle_index = new int[3*triangle_indices.size()];
        //create nodes and assign their positions.
        for(int i = 0; i<node_positions.size(); i++){

            Node3D n = new Node3D(positions, i);
            nodes.add(n);

            double[] pt = node_positions.get(i);

            System.arraycopy(pt, 0, positions, i*3, 3);
        }

        //Create connections and index array.
        for(int i = 0; i<connection_indices.size(); i++){
            int[] dices = connection_indices.get(i);

            Connection3D c = new Connection3D(nodes.get(dices[0]), nodes.get(dices[1]));
            connections.add(c);
            connection_index[2*i] = dices[0];
            connection_index[2*i+1] = dices[1];
        }

        for(int i = 0; i<triangle_indices.size(); i++){
            int[] dices = triangle_indices.get(i);
            Triangle3D t = new Triangle3D(nodes.get(dices[0]), nodes.get(dices[1]), nodes.get(dices[2]));
            triangles.add(t);

            triangle_index[3*i] = dices[0];
            triangle_index[3*i+1] = dices[1];
            triangle_index[3*i+2] = dices[2];
        }



    }

    public DeformableMesh3D(double[] positions, int[] connection_indices, int[] triangle_indices) {
        this.positions = positions;
        this.connection_index = connection_indices;
        this.triangle_index = triangle_indices;

        int n = positions.length/3;
        int t = triangle_indices.length/3;
        int c = connection_indices.length/2;

        nodes = new ArrayList<>(n);
        triangles = new ArrayList<>(t);
        connections = new ArrayList<>(c);

        for(int i = 0; i<n; i++){

            Node3D nn = new Node3D(positions, i);
            nodes.add(nn);

        }

        for(int i = 0; i<c; i++){
            Node3D a = nodes.get(connection_indices[2*i]);
            Node3D b = nodes.get(connection_indices[2*i + 1]);
            Connection3D con = new Connection3D(a,b);
            connections.add(con);
        }

        for(int i = 0; i<t; i++){
            try{
                Node3D a = nodes.get(triangle_indices[3*i]);
                Node3D b = nodes.get(triangle_indices[3*i + 1]);
                Node3D cc = nodes.get(triangle_indices[3*i + 2]);

                Triangle3D tri = new Triangle3D(a,b,cc);
                triangles.add(tri);
            } catch(Exception e){
                throw new RuntimeException("Unable to load mesh: " + e.getMessage(), e);
            }
        }


    }
    public List<Node3D> getConnectedNodes(){

        return nodes.stream().filter(n->
            connections.stream().anyMatch(c->c.A.equals(n) || c.B.equals(n))
        ).collect(Collectors.toList());

    }

    static public DeformableMesh3D loadMesh(double[] positions, int[] connection_indices, int[] triangle_indices){
        return new DeformableMesh3D(positions, connection_indices, triangle_indices);
    }

    public void syncConnectionIndices(){
        connection_index = connection_index.length!=2*connections.size()?new int[2*connections.size()]:connection_index;
        for(int i = 0; i<connections.size();i++){
            Connection3D con = connections.get(i);
            if(con==null||con.A==null)
                throw new RuntimeException("malformed connections: " + con);

            connection_index[2*i] = con.A.index;
            connection_index[2*i+1] = con.B.index;
        }


    }

    public void syncTriangleIndices(){

        if(triangle_index.length!=3*triangles.size()){
            triangle_index = new int[3*triangles.size()];
        }

        for(int i = 0; i<triangles.size();i++){
            Triangle3D t = triangles.get(i);
            triangle_index[3*i] = t.A.index;
            triangle_index[3*i+1] = t.B.index;
            triangle_index[3*i+2] = t.C.index;
        }

    }
    public void create3DObject(){
        data_object = new DeformableMeshDataObject(nodes, connections, triangles, positions, connection_index, triangle_index);
//        data_object.setWireColor(color);
        data_object.setColor(color);
        data_object.setShowSurface(showSurface);
    }

    public void reshape(){
        double[][] data = new double[nodes.size()][nodes.size()];
        if(BETA>0){
            updateBetaMatrix(data);
        }

        for(Connection3D c: connections){

            c.update();
            int[] dex = c.getIndices();
            data[dex[0]][dex[0]] += ALPHA;
            data[dex[0]][dex[1]] += -ALPHA;
            data[dex[1]][dex[0]] += -ALPHA;
            data[dex[1]][dex[1]] += ALPHA;

        }


        for(Node3D n: nodes){
            data[n.index][n.index] += n.getGamma(GAMMA);
        }

        Matrix M = new Matrix(data);

        decomp = M.lu();
    }

    private void updateBetaMatrix(double[][] data){
        Map<Node3D, List<Connection3D>> noder = getCurvatureMap();
        Map<Connection3D, Set<Connection3D>> conner = getAdjacencyMap(noder);
        for(Node3D n: nodes){
            List<Connection3D> cons = noder.get(n);
            double small_factor = 1.0;
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

                    data[n.index][another.index] += -BETA*small_factor;
                    data[n.index][other.index] += -BETA*small_factor;
                    data[n.index][n.index] += 2*BETA*small_factor;
                }

                List<Connection3D> secondOrder = noder.get(other);


                double f = 1.0;
                for(Connection3D con: secondOrder){
                    if(con.equals(a) || adjacent.contains(con)){
                        continue;
                    }
                    Node3D another = con.A.equals(other)?con.B:con.A;
                    data[n.index][n.index] += BETA*f;
                    data[n.index][another.index] += BETA*f;
                    data[n.index][other.index] -= 2*BETA*f;
                }


            }

        }
    }

    /**
     * Performs the bulk of calculations for doing an update. Creates a runnable that represents
     * finally changing the positions
     * @return
     */
    public Runnable partialUpdate(){
        if(decomp==null){
            reshape();
        }

        final double[] fx = new double[nodes.size()];
        final double[] fy = new double[nodes.size()];
        final double[] fz = new double[nodes.size()];

        double[] pt;
        for(Node3D n: nodes){
            n.update();
            pt = n.getCoordinates();
            double gamma = n.getGamma(GAMMA);
            fx[n.index] += gamma*pt[0];
            fy[n.index] += gamma*pt[1];
            fz[n.index] += gamma*pt[2];
        }

        for(ExternalEnergy external: energies) {
            external.updateForces(positions, fx, fy, fz);
        }

        final Matrix FX = new Matrix(fx,nodes.size());
        Matrix deltax = decomp.solve(FX);
        double[] nx = deltax.getRowPackedCopy();

        final Matrix FY = new Matrix(fy,nodes.size());
        Matrix deltay = decomp.solve(FY);
        double[] ny = deltay.getRowPackedCopy();

        final Matrix FZ = new Matrix(fz,nodes.size());
        Matrix deltaz = decomp.solve(FZ);
        double[] nz = deltaz.getRowPackedCopy();
        return ()-> {
            for (int i = 0; i < nodes.size(); i++) {
                positions[3 * i] = nx[i];
                positions[3 * i + 1] = ny[i];
                positions[3 * i + 2] = nz[i];

            }
            if(data_object!=null){
                data_object.updateGeometry(positions);
            }
        };
    }



    public void update(){
        if(decomp==null){
            reshape();
        }

        final double[] fx = new double[nodes.size()];
        final double[] fy = new double[nodes.size()];
        final double[] fz = new double[nodes.size()];

        double[] pt;
        for(Node3D n: nodes){
            n.update();
            pt = n.getCoordinates();
            double gamma = n.getGamma(GAMMA);
            fx[n.index] += gamma*pt[0];
            fy[n.index] += gamma*pt[1];
            fz[n.index] += gamma*pt[2];
        }

        for(ExternalEnergy external: energies) {
            external.updateForces(positions, fx, fy, fz);
        }

        Future<double[]> xfuture = pool.submit(() -> {
            final Matrix FX = new Matrix(fx,nodes.size());
            Matrix deltax = decomp.solve(FX);
            return deltax.getRowPackedCopy();
        });

        Future<double[]> yfuture = pool.submit(() -> {
            final Matrix FY = new Matrix(fy,nodes.size());
            Matrix deltay = decomp.solve(FY);
            return deltay.getRowPackedCopy();
        });

        Future<double[]> zfuture = pool.submit(() -> {
            final Matrix FZ = new Matrix(fz,nodes.size());
            Matrix deltaz = decomp.solve(FZ);
            return deltaz.getRowPackedCopy();
        });


        try {
            double[] nx = xfuture.get();
            double[] ny = yfuture.get();
            double[] nz = zfuture.get();
            for(int i = 0; i<nodes.size(); i++){
                positions[3*i] = nx[i];
                positions[3*i+1] = ny[i];
                positions[3*i+2] = nz[i];

            }

        } catch (InterruptedException e) {
            System.err.println("Program was interrupted during calculations!");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.err.println("Exception Occurred During update");
            e.printStackTrace();
        }

        if(data_object!=null){
            data_object.updateGeometry(positions);
        }
    }

    public void addExternalEnergy(ExternalEnergy energy){
        energies.add(energy);
    }

    public void removeExternalEnergy(ExternalEnergy energy){
        energies.remove(energy);
    }

    public void clearEnergies(){
        energies.clear();
    }

    public double calculateVolume(){
        return DeformableMesh3DTools.calculateVolume(new double[]{1,0,0}, positions, triangles);
    }

    public double calculateVolume(double[] dir){
        return DeformableMesh3DTools.calculateVolume(dir, positions, triangles);
    }

    /**
     * Thickness is in um. The number of steps in will be approximately the number of x pixels in thickness.
     *
     * @param stack source of image data
     * @param thickness thickness to be measured.
     * @return a list of intensited values along the normal of the triangle. The middle value should be the value
     */
    public ArrayList<double[]> calculateIntensity(MeshImageStack stack, double thickness){

        ArrayList<double[]> values = new ArrayList<>(triangles.size());
        int steps = (int)(thickness/stack.pixel_dimensions[0]) + 1;
        double ds = steps>1?thickness/stack.SCALE/(steps-1):thickness/stack.SCALE;
        int[] count = new int[positions.length/3];
        float[] colors = new float[positions.length];
        double max = 0.0;
        for(Triangle3D tri: triangles){
            tri.update();
            double s = 0;
            double mx = 0;
            for(int i = 0; i<steps; i++){
                double px = stack.getInterpolatedValue(tri.center[0]-ds*i*tri.normal[0], tri.center[1]-ds*i*tri.normal[1], tri.center[2]-ds*i*tri.normal[2]);
                s += px;
                if(px>mx)mx = px;
            }

            double intensity = s/steps;
            if(mx>max){
                max = mx;
            }
            int[] dices = tri.getIndices();
            for(int dex: dices){
                count[dex] += 1;
                colors[3*dex] += mx;
                colors[3*dex + 1] += 0;
                colors[3*dex + 2] += intensity;

            }

            double[] v = new double[] {tri.center[0], tri.center[1], tri.center[2], intensity};
            values.add(v);
        }
        max = max>0?max:1;
        for(int i = 0; i<count.length; i++){

            float c = count[i]>0?1.0f/(float)max/count[i]:0;
            colors[3*i] = colors[3*i]*c;
            colors[3*i+1] = colors[3*i+1]*c;
            colors[3*i+2] = colors[3*i+2]*c;


        }

        return values;
    }

    public ArrayList<double[]> calculateStress(){

        ArrayList<double[]> values = new ArrayList<>(triangles.size());
        int[] count = new int[positions.length/3];
        float[] colors = new float[positions.length];
        double max = 0.0;
        double min = Double.MAX_VALUE;
        for(Triangle3D tri: triangles){
            tri.update();

            double area = tri.area;
            if(area>max){
                max = area;
            }

            if(area<min){
                min = area;
            }
            int[] dices = tri.getIndices();
            for(int dex: dices){
                count[dex] += 1;
                colors[3*dex] += area;
                colors[3*dex + 1] += area;
                colors[3*dex + 2] += area;

            }

            double[] v = new double[] {tri.center[0], tri.center[1], tri.center[2], area};
            values.add(v);
        }
        double da = (max - min);
        da = da>0?1/da:0;
        for(int i = 0; i<count.length; i++){

            float c = count[i]>0?1.0f*(float)da/count[i]:0;

            colors[3*i] = (colors[3*i]-(float)min)*c;
            colors[3*i+1] = (colors[3*i+1]-(float)min)*c;
            colors[3*i+2] = (colors[3*i+2]-(float)min)*c;


        }

        return values;


    }

    public List<double[]> calculateCurvature(){
        Map<Node3D, List<Triangle3D>> node_to_triangle = new HashMap<>();
        List<double[]> values = new ArrayList<>();
        for(Triangle3D tri: triangles){
            tri.update();
            addNode(tri.A, tri, node_to_triangle);
            addNode(tri.B, tri, node_to_triangle);
            addNode(tri.C, tri, node_to_triangle);


        }

        float[] colors = new float[nodes.size()*3];
        float pmax = -Float.MAX_VALUE;
        float nmax = -Float.MAX_VALUE;
        for(Node3D node: nodes){
            List<Triangle3D> t_angles = node_to_triangle.get(node);
            float positive = 0.0f;
            float negative = 0.0f;
            int count = t_angles.size();
            while(t_angles.size()>0){
                Triangle3D triangle = t_angles.get(0);

                t_angles.remove(0);
                for(Triangle3D other: t_angles){
                    double curvature = calculateCurvature(node, triangle, other);


                    if(curvature>0){
                        positive += curvature;

                    }else{
                        negative += -curvature;

                    }



                }

            }
            positive = positive/count;
            negative = negative/count;
            
            colors[3*node.index] = positive;
            colors[3*node.index+1] = 0;
            colors[3*node.index + 2] = negative;
            if(positive>pmax){
                pmax = positive;
            }
            if(negative>nmax){
                nmax = negative;
            }

        }



        pmax = pmax*1.0f;
        nmax = nmax*1.0f;
        for(int i = 0; i<nodes.size(); i++){

            colors[3*i] = colors[3*i]/pmax;
            colors[3*i] = colors[3*i]>1?1f:colors[3*i];


            colors[3*i+2] = colors[3*i+2]/nmax;
            colors[3*i+2] = colors[3*i+2]>1?1f:colors[3*i+2];

        }

        return values;
    }

    static double calculateCurvature(Node3D p, Triangle3D A, Triangle3D B){
        Node3D op = sharesConnection(p, A, B);
        if(op==null){
            return 0;
        }

        double[] dt = Vector3DOps.difference(A.center, B.center);
        double[] dnormal = Vector3DOps.difference(A.normal, B.normal);

        double dot = Vector3DOps.dot(dt,dnormal);

        double[] cross = Vector3DOps.cross(A.normal, B.normal);

        double mag = Math.sqrt(cross[0]*cross[0] + cross[1]*cross[1] + cross[2]*cross[2]);
        mag = mag<1e-16?0:mag;
        //double ds = Math.sqrt(dot(dt,dt))

        return dot<0?-mag:mag;

    }

    public void scale(double v, double[] center){
        for(Node3D node: nodes){

            double[] op = node.getCoordinates();
            for(int i = 0; i<3; i++){
                op[i] = (op[i]-center[i])*v + center[i];
            }
            node.setPosition(op);
        }
    }

    public void rotate(double[] axis, double[] center, double angle){

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double[] rotation_matrix = {
            cos + axis[0]*axis[0]*(1-cos), axis[0]*axis[1]*(1-cos) - axis[2]*sin, axis[0]*axis[2]*(1-cos) + axis[1]*sin,
            axis[1]*axis[0]*(1-cos) + axis[2]*sin, cos + axis[1]*axis[1]*(1-cos), axis[1]*axis[2]*(1-cos) - axis[0]*sin,
            axis[2]*axis[0]*(1-cos) - axis[1]*sin, axis[2]*axis[1]*(1-cos) + axis[0]*sin, cos + axis[2]*axis[2]*(1-cos)

        };

        for(Node3D node: nodes){
            double[] original_position = node.getCoordinates();

            //find the r
            for(int i = 0; i<3; i++){
                original_position[i] = original_position[i] - center[i];
            }

            double[] rotated = new double[]{0,0,0};

            for(int i = 0; i< 3; i++){
                for(int j = 0; j<3; j++){
                    rotated[i] += rotation_matrix[3*i + j]*original_position[j];
                }
            }
            for(int i = 0; i<3; i++){
                rotated[i] += center[i];
            }

            node.setPosition(rotated);
        }

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
    static private Node3D sharesConnection(Node3D n, Triangle3D a, Triangle3D b){
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
                    return as[i];
                }
            }

        }
        return null;
    }


    private void addNode(Node3D n, Triangle3D t, Map<Node3D, List<Triangle3D>> map){
        if(!map.containsKey(n)){
            map.put(n,new ArrayList<>());
        }
        map.get(n).add(t);
    }



    /**
     * Moves all of the nodes in this mesh by the displacement value.
     * @param displacement xyz displacement values.
     */
    public void translate(double[] displacement) {

        for(Node3D node: nodes){

            double[] position = node.getCoordinates();

            for(int i = 0; i<3; i++){
                position[i] += displacement[i];
            }

            node.setPosition(position);

        }

    }

    static DeformableMesh3D generateEdgeX(){
        ArrayList<double[]> points = new ArrayList<>();
        ArrayList<int[]> triangles = new ArrayList<>();
        points.add(new double[]{0,0,0});
        points.add(new double[]{1,0,0});
        points.add(new double[]{0.5,0.5*Math.sqrt(3),0});
        points.add(new double[]{-0.5, 0.5*Math.sqrt(3),0});
        points.add(new double[]{-1,0,0});
        points.add(new double[]{-0.5,0,-0.5*Math.sqrt(3)});
        points.add(new double[]{0.5,0,-0.5*Math.sqrt(3)});

        triangles.add(new int[]{0,1,2});
        triangles.add(new int[]{0,2,3});
        triangles.add(new int[]{0,3,4});
        triangles.add(new int[]{0,4,5});
        triangles.add(new int[]{0,5,6});
        triangles.add(new int[]{0,6,1});

        ArrayList<int[]> connections = new ArrayList<>();
        connections.add(new int[]{0,1});
        connections.add(new int[]{0,2});
        connections.add(new int[]{0,3});
        connections.add(new int[]{0,4});
        connections.add(new int[]{0,5});
        connections.add(new int[]{0,6});
        connections.add(new int[]{1,2});
        connections.add(new int[]{2,3});
        connections.add(new int[]{3,4});
        connections.add(new int[]{4,5});
        connections.add(new int[]{5,6});
        connections.add(new int[]{6,1});

        return new DeformableMesh3D(points, connections, triangles);
    }

    static DeformableMesh3D generateEdgeY(){
        DeformableMesh3D mesh = generateEdgeX();
        mesh.rotate(new double[]{0,0,1}, ORIGIN, Math.PI/2);
        return mesh;
    }

    static DeformableMesh3D generateEdgeZ(){
        DeformableMesh3D mesh = generateEdgeX();
        mesh.rotate(new double[]{0,-1,0}, ORIGIN, Math.PI/2);
        return mesh;
    }







    public ArrayList<double[]> calculateNormalScan(MeshImageStack stack, double thickness) {

        ArrayList<double[]> values = new ArrayList<>();

        int steps = (int)(thickness/stack.pixel_dimensions[0]) + 1;
        double ds = steps>1?thickness/stack.SCALE/(steps-1):thickness/stack.SCALE;


        for(Triangle3D tri: triangles){
            tri.update();
            double[] v = new double[2*steps + 1];
            for(int i = 0; i<2*steps+1; i++){
                double p = ds*(i-steps);
                v[i] += stack.getInterpolatedValue(tri.center[0]-p*tri.normal[0], tri.center[1]-p*tri.normal[1], tri.center[2]-p*tri.normal[2]);
            }

            values.add(v);
        }




        return values;

    }

    public void updatePositionBuffer(double[] new_data) {

        positions = new_data;

        for(Node3D node: nodes){
            node.setBackingData(new_data);
        }
    }

    public Box3D getBoundingBox(){

        double minx = Double.MAX_VALUE;
        double miny = Double.MAX_VALUE;
        double minz = Double.MAX_VALUE;

        double maxx = -Double.MAX_VALUE;
        double maxy = -Double.MAX_VALUE;
        double maxz = -Double.MAX_VALUE;

        for(Node3D node: nodes){
            double[] pt = node.getCoordinates();

            if(pt[0]<minx){
                minx = pt[0];
            }
            if(pt[0]>maxx){
                maxx = pt[0];
            }

            if(pt[1]<miny){
                miny = pt[1];
            }
            if(pt[1]>maxy){
                maxy = pt[1];
            }

            if(pt[2]<minz){
                minz = pt[2];
            }
            if(pt[2]>maxz){
                maxz = pt[2];
            }

        }

        return new Box3D(minx, miny, minz, maxx, maxy, maxz);


    }

    public List<Connection3D> getConnections() {
        return connections;
    }

    public void resetPositions(){
        if(data_object!=null){
            data_object.updateGeometry(positions);
        }
    }

    public void setPositions(double[] positions) {
        if(positions.length==this.positions.length){
            updatePositionBuffer(positions);
            if(data_object!=null){
                data_object.updateGeometry(positions);
            }
        } else{
            throw new IllegalArgumentException("The array length is not correct for this mesh: " + positions.length + ", " + this.positions.length);
        }
    }

    public double[] getCoordinates(int dex) {
        return new double[]{
                positions[3*dex], positions[3*dex+1], positions[3*dex+2]
        };
    }

    public Map<Node3D, List<Connection3D>> getCurvatureMap(){
        Map<Node3D, List<Connection3D>> map = new HashMap<>();

        for(Node3D node: nodes){
            map.put(node, new ArrayList<>());
        }

        for(Connection3D connection: connections){

            map.get(connection.A).add(connection);
            map.get(connection.B).add(connection);

        }



        return map;
    }

    public void confine(Box3D box) {
        boolean global_change = false;
        for (Node3D node : nodes) {
            double[] pt = node.getCoordinates();
            boolean changed = false;

            for (int i = 0; i < 3; i++) {
                if (pt[i] < box.low[i]) {
                    pt[i] = box.low[i];
                    changed = true;
                }
                if (pt[i] > box.high[i]) {
                    pt[i] = box.high[i];
                    changed = true;
                }
            }

            if (changed) {
                global_change = true;
                node.setPosition(pt);
            }

        }

        if(global_change && data_object!=null){
            data_object.updateGeometry(positions);
        }
    }

    public Map<Connection3D, Set<Connection3D>> getAdjacencyMap(Map<Node3D, List<Connection3D>> connectionMap){
        Map<Connection3D, Set<Connection3D>> map = new HashMap<>();
        for(Connection3D connection: connections){
            //use a set because each connection will share 2 triangles with the other connections.
            map.put(connection, new HashSet<>());
        }
        for(Triangle3D triangle: triangles){
            List<Connection3D> tCons = new ArrayList<>();
            Collection<Connection3D> a = connectionMap.get(triangle.A);
            Collection<Connection3D> b = connectionMap.get(triangle.B);
            //Collection<Connection3D> c = connectionMap.get(triangle.C);
            int count = 0;

            //two edges contain a.
            for(Connection3D aC: a){
                int open = aC.A.equals(triangle.A)? aC.B.index : aC.A.index;

                if(open == triangle.B.index || open==triangle.C.index){
                    tCons.add(aC);
                    count++;
                    if(count==2){
                        map.get(aC).add(tCons.get(0));
                        map.get(tCons.get(0)).add(aC);
                        break;
                    }
                }
            }

            //remaining edge contains b
            for(Connection3D bC: b){
                int open = bC.A.equals(triangle.B)?bC.B.index:bC.A.index;
                if(triangle.C.index == open){
                    for(Connection3D c: tCons){
                        map.get(c).add(bC);
                        map.get(bC).add(c);
                    }
                    tCons.add(bC);
                    break;
                }
            }

            if(tCons.size()!=3) throw new RuntimeException("BROKEN ADJACENCY MAP");


        }


        return map;
    }

    public void setShowSurface(boolean showSurface) {
        if(data_object!=null){
            data_object.setShowSurface(showSurface);
        }
        this.showSurface = showSurface;
    }

    public boolean isShowSurface() {
        return showSurface;
    }

    public void setColor(Color color) {
        if(this.color!=color){
            this.color = color;
            if(data_object!=null) {
                data_object.setColor(color);
            }
        }
    }

    public Color getColor(){
        return color;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    public List<Connection3D> getOutterBounds(){
        List<Connection3D> boundary = new ArrayList<>();
        for(Connection3D connection: connections){
            int count = 0;
            for(Triangle3D tri: triangles){
                if(tri.hasConnection(connection)){
                    count += 1;
                    if(count>1){
                        break;
                    }
                }
            }

            if(count==1){
                //edge
                boundary.add(connection);
            }
        }

        return boundary;

    }

    public List<ExternalEnergy> getExternalEnergies() {
        return new ArrayList<>(energies);
    }
}
