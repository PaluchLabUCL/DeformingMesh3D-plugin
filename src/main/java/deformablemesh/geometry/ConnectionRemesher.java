package deformablemesh.geometry;

import deformablemesh.MeshImageStack;
import deformablemesh.meshview.DeformableMeshDataObject;
import deformablemesh.meshview.MeshFrame3D;
import ij.ImagePlus;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 *
 * This class will remesh based on connection lengths. If a connection is too long it will be halved, and the two
 * neighboring triangles will be split in two. If a connection is too short, and both opposite nodes have more than 3
 * connections, then the connection will be removed.
 *
 *
 */
public class ConnectionRemesher {

    MeshFrame3D frame;

    double minLength = 0.02;
    double maxLength = 0.04;

    Map<Node3D, List<Triangle3D>> nodeToTriangle = new HashMap<>();
    Map<Node3D, List<Connection3D>> nodeToConnection = new HashMap<>();
    Map<Connection3D, List<Triangle3D>> adjacentTriangles = new HashMap<>();
    Map<Triangle3D, List<Connection3D>> triangleEdges = new HashMap<>();
    List<Node3D> nodes = new ArrayList<>();
    List<Triangle3D> triangles = new ArrayList<>();
    List<Connection3D> connections = new ArrayList<>();
    double[] positions;
    boolean openSurface = false;
    public void buildDisplay(){
        frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();
    }

    AtomicBoolean cancelled = new AtomicBoolean(false);
    public void prepareWorkSpace(DeformableMesh3D original){
        for(Node3D node: original.nodes){
            nodeToTriangle.put(node, new ArrayList<>());
            nodeToConnection.put(node, new ArrayList<>());
            nodes.add(node);
        }

        for(Triangle3D t: original.triangles){
            addTriangle(t);

        }


        for(Connection3D con: original.connections){
            addConnection(con);

            List<Triangle3D> pair = adjacentTriangles.get(con);

            List<Triangle3D> ats = nodeToTriangle.get(con.A);
            List<Triangle3D> bts = nodeToTriangle.get(con.B);

            for(Triangle3D t: ats){
                if(bts.contains(t)){
                    pair.add(t);
                    if(pair.size()==2){
                        break;
                    }
                }
            }
            if(pair.size()==1){
                System.out.println("edge of mesh!");
                openSurface = true;
            }

            for(Triangle3D t: pair){
                triangleEdges.get(t).add(con);
            }

        }


    }

    public Connection3D getShortestConnection(){
        double min = Double.MAX_VALUE;
        Connection3D s = null;
        for(Connection3D c: connections){
            c.update();
            double l = c.length;
            if(l<min){
                s = c;
                min = l;
            }
        }
        return s;
    }

    public Connection3D getShortestConnection(Set<Connection3D> unavailable){
        double min = Double.MAX_VALUE;
        Connection3D s = null;
        for(Connection3D c: connections){
            if(unavailable.contains(c)){
                continue;
            }
            c.update();
            double l = c.length;
            if(l<min){
                s = c;
                min = l;
            }
        }
        return s;
    }

    public DeformableMesh3D remesh(DeformableMesh3D original){
        prepareWorkSpace(original);
        double ml = 0;
        double mn = 1;
        double ave = 0;

        List<Connection3D> longOnes = new ArrayList<>();
        for(Connection3D con: original.connections){
            con.update();
            double l = con.length;
            if(l>maxLength){
                longOnes.add(con);
            }
            if(l>ml){
                ml = l;
            }

            if(l<mn){
                mn = l;
            }
            ave += l;

        }
        ave = ave/original.connections.size();
        if (Double.isNaN(mn) || Double.isNaN(ml) || Double.isInfinite(mn) || Double.isInfinite(ml)){
            throw new RuntimeException("Invalid mesh result: " + "min: " + mn + ", max: " + ml + ", mean: " + ave);
        }

        longOnes.sort(Comparator.comparingDouble(c -> c.length));

        positions = original.positions;
        while(longOnes.size()>0){

            double[] new_positions = new double[3*longOnes.size() + positions.length];
            System.arraycopy(positions, 0, new_positions, 0, positions.length);
            positions = new_positions;
            for(Node3D n: nodes){
                n.setBackingData(positions);
            }
            List<Connection3D> replacements = new ArrayList<>();
            for(Connection3D c: longOnes) {
                replacements.addAll(replaceLongConnection(c));
            }
            longOnes.clear();

            for(Connection3D con: replacements){
                con.update();
                double l = con.length;
                if(l>maxLength){
                    longOnes.add(con);
                }
            }

        }

        List<Connection3D> shorties = new ArrayList<>();

        for(Connection3D con: connections){
            con.update();
            double l = con.length;
            if(l>maxLength){
                longOnes.add(con);
            }
            if(l<minLength){
                shorties.add(con);
            }


        }

        Connection3D ss = getShortestConnection();

        Set<Connection3D> nonEligible = new HashSet<>();
        while(ss!=null && ss.length<minLength){
            if(removeShortConnection(ss)){
                ss = getShortestConnection(nonEligible);
            } else{
                nonEligible.add(ss);
                ss = getShortestConnection(nonEligible);
            }
        }

        nodes.sort(Comparator.comparingInt(n->n.index));
        Map<Integer, Integer> map = new HashMap<>();

        positions = new double[3*nodes.size()];
        for(int i = 0; i<nodes.size(); i++){

            double[] pt = nodes.get(i).getCoordinates();
            positions[3*i] = pt[0];
            positions[3*i+1] = pt[1];
            positions[3*i+2] = pt[2];
            map.put(nodes.get(i).index, i);
        }

        int[] triangle_indexes = new int[3*triangles.size()];
        int[] connection_indexes = new int[2*connections.size()];
        int dex = 0;
        for(Triangle3D t: triangles){
            triangle_indexes[dex++] = map.get(t.A.index);
            triangle_indexes[dex++] = map.get(t.B.index);
            triangle_indexes[dex++] = map.get(t.C.index);
        }

        dex = 0;
        for(Connection3D c: connections){
            connection_indexes[dex++] = map.get(c.A.index);
            connection_indexes[dex++] = map.get(c.B.index);
        }

        return new DeformableMesh3D(positions, connection_indexes, triangle_indexes);


    }


    Node3D getOpposite(Triangle3D t, Node3D a, Node3D b){
        Node3D[] nodes = {t.A, t.B, t.C};

        for(Node3D n: nodes){
            if(n!=a && n!=b){
                return n;
            }
        }
        throw new RuntimeException("Broken Triangle: has no nodes exclusive of " + a.index + ", " + b.index);
    }

    Connection3D getOppositeEdge(Triangle3D t, Connection3D a, Connection3D b){

        List<Connection3D> edges = triangleEdges.get(t);
        for(Connection3D c: edges){

            if (c.equals(a) || c.equals(b)) {
                continue;
            } else{
                return c;
            }

        }
        throw new RuntimeException("Triangle only contains 2 edges");

    }

    /**
     * Creating a new triangle with the same winding as 'org'.  The original triangle has
     * either ( o1, o2, e ) or ( o2, o1, e )  winding order. This can be shown by cycling the triangle.
     *
     *
     * @param org original triangle.
     * @param o1 node from original triangle.
     * @param o2 second node from original triangle.
     * @param n1 new node being added.
     * @return
     */
    Triangle3D matchedWindingTriangle(Triangle3D org, Node3D o1, Node3D o2, Node3D n1){
        Node3D[] nodes = {org.A, org.B, org.C};

        for(int i = 0; i<3; i++){

            if(nodes[i].equals(o1)){
                if(nodes[(i+1)%3].equals(o2) ){
                    return new Triangle3D(o1, o2, n1);
                } else{
                    return new Triangle3D(o2, o1, n1);
                }
            }

        }

        throw new RuntimeException(
                String.format( "Node %d is not in triangle. (%d, %d, %d",
                o1.index, org.A.index, org.B.index, org.C.index )
        );


    }

    Connection3D getMatched(Triangle3D t, Node3D n1, Node3D n2){

        for(Connection3D con: triangleEdges.get(t)){

            if(con.A.equals(n1) && con.B.equals(n2)){
                return con;
            } else if(con.B.equals(n1) && con.A.equals(n2)){
                return con;
            }

        }
        throw new RuntimeException(
            String.format("Triangle is not mapped to an edge with nodes %d and %d", n1.index, n2.index)
        );

    }
    /**
     *
     * The provided connection will be removed, and both adjacent triangles will also be removed.
     * A new node, and four new triangles, and four new connections will be added.
     *
     * @param c
     */
    List<Connection3D> replaceLongConnection(Connection3D c){
        double[] pa = c.A.getCoordinates();
        double[] pb = c.B.getCoordinates();

        double[] nPt = { 0.5*(pa[0] + pb[0]), 0.5*(pa[1] + pb[1]), 0.5*(pa[2] + pb[2])};

        Node3D node = new Node3D(positions, nodes.size());
        node.setPosition(nPt);
        nodes.add(node);
        nodeToTriangle.put(node, new ArrayList<>());
        nodeToConnection.put(node, new ArrayList<>());

        Connection3D a = new Connection3D(c.A, node);
        Connection3D b = new Connection3D(node, c.B);
        addConnection(a);
        addConnection(b);

        //either 1 (edge of mesh) or 2 triangles to be split.
        List<Triangle3D> toRemove = adjacentTriangles.get(c);

        Triangle3D first = toRemove.get(0);

        Node3D opposite1 = getOpposite(first, c.A, c.B);

        //Connection that splits previous triangle.
        Connection3D firstCon = new Connection3D(node, opposite1);
        addConnection(firstCon);

        Triangle3D firstA = matchedWindingTriangle(first, c.A, opposite1, node);
        Triangle3D firstB = matchedWindingTriangle(first, c.B, opposite1, node);
        addTriangle(firstA);
        addTriangle(firstB);

        adjacentTriangles.get(a).add(firstA);
        adjacentTriangles.get(b).add(firstB);

        adjacentTriangles.get(firstCon).add(firstA);
        adjacentTriangles.get(firstCon).add(firstB);

        //the two external edges will be shared with the new triangles.
        Connection3D remainA1 = getMatched(first, c.A, opposite1);
        Connection3D remainB1 = getMatched(first, c.B, opposite1);

        adjacentTriangles.get(remainA1).add(firstA);
        adjacentTriangles.get(remainB1).add(firstB);

        triangleEdges.get(firstA).addAll(Arrays.asList(a, firstCon, remainA1));
        triangleEdges.get(firstB).addAll(Arrays.asList(b, firstCon, remainB1));

        //If there is a second triangle that needs to be removed.
        Triangle3D second = toRemove.get(1);

        Node3D opposite2 = getOpposite(second, c.A, c.B);

        //diagonal
        Connection3D secondCon = new Connection3D(node, opposite2);
        addConnection(secondCon);

        Triangle3D secondA = matchedWindingTriangle(second, c.A, opposite2, node);
        Triangle3D secondB = matchedWindingTriangle(second, c.B, opposite2, node);
        addTriangle(secondA);
        addTriangle(secondB);

        Connection3D remainA2 = getMatched(second, c.A, opposite2);
        Connection3D remainB2 = getMatched(second, c.B, opposite2);

        //across from each other.
        adjacentTriangles.get(secondCon).add(secondA);
        adjacentTriangles.get(secondCon).add(secondB);

        //across to the other new triangles.
        adjacentTriangles.get(a).add(secondA);
        adjacentTriangles.get(b).add(secondB);

        //across external edge
        adjacentTriangles.get(remainA2).add(secondA);
        adjacentTriangles.get(remainB2).add(secondB);

        //edge map.
        triangleEdges.get(secondA).addAll(Arrays.asList(a, secondCon, remainA2));
        triangleEdges.get(secondB).addAll(Arrays.asList(b, secondCon, remainB2));


        removeTriangle(second);
        removeTriangle(first);
        removeConnection(c);

        return Arrays.asList(a, b, firstCon, secondCon);
    }

    Triangle3D remapTriangle(Triangle3D orig, Node3D replacing, Node3D replacement){
        Node3D a,b,c;

        if(orig.A.equals(replacing)){
            a = replacement;
            b = orig.B;
            c = orig.C;
        } else if(orig.B.equals(replacing)){
            a = orig.A;
            b = replacement;
            c = orig.C;

        } else if( orig.C.equals(replacing)) {
            a = orig.A;
            b = orig.B;
            c = replacement;
        } else{
            throw new RuntimeException("Node to be replaced is not found in the current triangle! " + replacing.index);
        }
        return new Triangle3D(a,b,c);
    }

    Connection3D remapConnection(Connection3D c, Node3D replacing, Node3D replacement){

        if (c.A.equals(replacing)) {
            Connection3D c3d = new Connection3D(replacement, c.B);
            if(connections.contains(c3d)){
                System.out.println("re-mapped");
            }
            return c3d;
        } else if(c.B.equals(replacing)){
            Connection3D c3d = new Connection3D(c.A, replacement);
            if(connections.contains(c3d)){
                System.out.println("wrapped");
            }
            return c3d;
        }

        throw new RuntimeException("Node does not exist to be replaced on connection. " + replacing.index);


    }

    public boolean removeShortConnection(Connection3D con){
        List<Triangle3D> triangles = adjacentTriangles.get(con);

        //both will be removed.
        for(Triangle3D t: triangles) {
            Node3D opposite = getOpposite(t, con.A, con.B);
            if (nodeToConnection.get(opposite).size() <= 3) {
                //this fault is also a triangle that is split in three.
                //the middle node, and three triangles can be removed and
                // replaced by a single triangle.
                System.out.println("Cannot remove edge.");
                return false;
            }
        }

        double[] apt = con.A.getCoordinates();
        double[] bpt = con.B.getCoordinates();
        double[] npt = {0.5*(apt[0] + bpt[0]), 0.5*(apt[1] + bpt[1]), 0.5*(apt[2] + bpt[2])};
        con.A.setPosition(npt);

        Set<Node3D> aNeighbors = nodeToConnection.get(con.A).stream().map(
            c3d ->{
                if (c3d.A.equals(con.A)) {
                    return c3d.B;
                } else{
                    return c3d.A;
                }
            }).collect(Collectors.toSet());

        List<Connection3D> mappingConnections = new ArrayList<>(nodeToConnection.get(con.B));
        int shared = 0;
        for(Connection3D forMapping: mappingConnections){
            Node3D other = forMapping.B == con.B ? forMapping.A : forMapping.B;
            if (aNeighbors.contains(other)) {
                shared++;
            }
        }
        if(shared != 2){
            //This is a necking type fault. Where the connection nodes
            // share another node.
            //This could be a location to split the mesh.
            System.out.println("non-removable edge: " + shared);
            return false;
        }
        List<Triangle3D> mappingTriangles = new ArrayList<>(nodeToTriangle.get(con.B));


        for(Triangle3D t3d: mappingTriangles){
            if(triangles.contains(t3d)){
                //will be removed, not remapped.
                continue;
            }
            Triangle3D remapped = remapTriangle(t3d, con.B, con.A);
            addTriangle(remapped);

            List<Connection3D> edges = triangleEdges.get(t3d);
            for(Connection3D edge: edges){
                List<Triangle3D> adj = adjacentTriangles.get(edge);
                adj.add(remapped);
                triangleEdges.get(remapped).add(edge);
            }
            removeTriangle(t3d);
        }

        for(Connection3D toMap: mappingConnections){
            if(toMap == con){
                continue;
            }
            List<Triangle3D> tri = adjacentTriangles.get(toMap);
            if(tri == null ){
                throw new RuntimeException("attempting to remove unmapped connection");
            }
            boolean cleanSwap = true;
            Connection3D crossEdge = null;
            for(Triangle3D t: tri){
                if(triangles.contains(t)) {
                    //get the opposite edge and connect.
                    cleanSwap = false;
                    crossEdge = getOppositeEdge(t, con, toMap);
                }
            }

            if(cleanSwap){
                Connection3D reMapped = remapConnection(toMap, con.B, con.A);
                addConnection(reMapped);
                adjacentTriangles.get(reMapped).addAll(tri);
                for(Triangle3D t: tri){
                    triangleEdges.get(t).add(reMapped);
                }
            } else{
                for(Triangle3D t: tri){
                    if(!triangles.contains(t)) {
                        adjacentTriangles.get(crossEdge).add(t);
                        triangleEdges.get(t).add(crossEdge);
                    }
                }

            }

            removeConnection(toMap);
        }

        removeConnection(con);
        triangles.forEach(this::removeTriangle);
        nodes.remove(con.B);
        return true;
    }

    public void addTriangle(Triangle3D t){
        triangles.add(t);

        nodeToTriangle.get(t.A).add(t);
        nodeToTriangle.get(t.B).add(t);
        nodeToTriangle.get(t.C).add(t);

        triangleEdges.put(t, new ArrayList<>());
    }

    public void removeTriangle(Triangle3D t){
        triangles.remove(t);

        nodeToTriangle.get(t.A).remove(t);
        nodeToTriangle.get(t.B).remove(t);
        nodeToTriangle.get(t.C).remove(t);

        List<Connection3D> edges = triangleEdges.get(t);
        for(Connection3D c: edges){
            adjacentTriangles.get(c).remove(t);
        }
        triangleEdges.remove(t);

    }

    public void addConnection(Connection3D c){
        if(connections.contains(c)){
            System.out.println("existing!");
            //throw new RuntimeException("adding existing connection");
        }
        connections.add(c);

        nodeToConnection.get(c.A).add(c);
        nodeToConnection.get(c.B).add(c);

        adjacentTriangles.put(c, new ArrayList<>());
    }
    public void removeConnection(Connection3D c){
        connections.remove(c);
        nodeToConnection.get(c.A).remove(c);
        nodeToConnection.get(c.B).remove(c);

        List<Triangle3D> ats = adjacentTriangles.get(c);
        for(Triangle3D t: ats){
            triangleEdges.get(t).remove(c);
        }
        adjacentTriangles.remove(c);
    }


    public static void main(String[] args){

        ConnectionRemesher rem = new ConnectionRemesher();
        rem.buildDisplay();

        Box3D box = new Box3D(new double[]{0, 0, 0}, 1.0, 0.5, 0.6);
        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(box, new double[]{ 0, 0, 0 }, 2);
        mesh.create3DObject();
        mesh.setShowSurface(false);

        DeformableMeshDataObject d = mesh.data_object;
        d.setWireColor(Color.BLACK);

        rem.frame.addDataObject(mesh.data_object);

        DeformableMesh3D rmesh = rem.remesh(mesh);
        //rmesh = new ConnectionRemesher().remesh(rmesh);
        rmesh.create3DObject();
        rmesh.setShowSurface(true);
        rem.frame.addDataObject(rmesh.data_object);
    }

    public void setMinAndMaxLengths(double minConnectionLength, double maxConnectionLength) {
        minLength = minConnectionLength;
        maxLength = maxConnectionLength;
    }
}
