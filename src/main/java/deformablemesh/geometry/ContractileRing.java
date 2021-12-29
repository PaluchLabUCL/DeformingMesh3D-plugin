package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.ringdetection.PlaneFitter;
import deformablemesh.util.Vector3DOps;

import java.util.*;

/**
 * This is a class that holds 3d line data, it will be used for applying a force to
 *
 * Created by msmith on 10/22/15.
 */
public class ContractileRing {
    List<double[]> points;
    Map<Integer, double[]> mappings = new HashMap<>();
    List<List<Node3D>> connectionMap;
    public ContractileRing(List<double[]> pts){
        points = pts;
    }

    static double square(double v){
        return v*v;
    }

    public List<Node3D> createMappings(DeformableMesh3D mesh){
        mappings.clear();
        PlaneFitter.Plane plane = PlaneFitter.findBestPlane(points);
        Furrow3D furrow = new Furrow3D(plane.center, plane.normal);
        List<Connection3D> connections = furrow.getIntersectionConnections(mesh.getConnections());
        Set<Node3D> nodes = new HashSet<>();
        for(Connection3D con: connections){

            Node3D a = con.A;
            Node3D b = con.B;

            double s1 = Math.abs(furrow.getDistance(a));
            double s2 = Math.abs(furrow.getDistance(b));

            if(s1<s2){
                nodes.add(a);
            } else{
                nodes.add(b);
            }

        }

        for(Node3D node: nodes){

            double min = Double.MAX_VALUE;
            double[] pos = node.getCoordinates();
            for(double[] p: points){
                double separation = Vector3DOps.distance(pos, p);
                if(separation<min){
                    min=separation;
                    mappings.put(node.index, p);
                }
            }



        }
        List<Node3D> ret = new ArrayList<>(nodes);
        sortByAngle(ret, plane.center, plane.normal);




        return ret;
    }

    static public void sortByAngle(List<Node3D> list, double[] center, double[] normal){
        double[] p = null;
        Iterator<Node3D> iter = list.iterator();
        double[] r0 = new double[3];
        while(p==null&&iter.hasNext()){
            double[] pt = iter.next().getCoordinates();
            //r0 is the original vector that will be used for the x-axis.
            r0[0] = pt[0] - center[0];
            r0[1] = pt[1] - center[1];
            r0[2] = pt[2] - center[2];
            double mag = Vector3DOps.normalize(r0);

            if(mag==0){
                //skip, at the center.
                continue;
            }

            double[] cross = Vector3DOps.cross(normal, r0);

            mag = Vector3DOps.normalize(cross);
            if(mag==0){
                //skip, parallel to the normal
                continue;
            }

            //x-axis is the
            r0 = Vector3DOps.cross(cross, normal);
            p = cross;
        }
        final double[] r = r0;
        final double[] r2 = p;
        //none of the points could form an angle.
        if(p==null) return;

        Collections.sort(list, (o1, o2)->{
            double[] d1 = o1.getCoordinates();
            double[] d2 = o2.getCoordinates();
            //get the coordinates in plane.
            double[] p1 = Vector3DOps.difference(d1, center);
            double[] p2 = Vector3DOps.difference(d2, center);

            double c1 = Vector3DOps.dot(r, p1);
            double s1 = Vector3DOps.dot(r2, p1);
            double theta1 = Math.atan2(s1, c1);
            theta1 = theta1<0?2*Math.PI + theta1:theta1;
            double norm = Math.sqrt(c1*c1 + s1*s1);
            c1 = c1/norm;
            s1 = s1/norm;

            double c2 = Vector3DOps.dot(r, p2);
            double s2 = Vector3DOps.dot(r2, p2);
            double theta2 = Math.atan2(s2, c2);
            theta2 = theta2<0?2*Math.PI + theta2:theta2;
            norm = Math.sqrt(c2*c2 + s2*s2);
            c2 = c2/norm;
            s2 = s2/norm;

            int q1 = getQuadrant(c1, s1);
            int q2 = getQuadrant(c2, s2);
            if(q1==q2){
                switch(q1){
                    case 0:
                    case 1:
                        return Double.compare(-c1, -c2);
                    case 2:
                    case 3:
                        return Double.compare(c1, c2);
                }
            }
            return Integer.compare(q1,q2);


        });

    }

    static public void sortPointsByAngle(List<double[]> list, double[] center, double[] normal){
        double[] p = null;
        Iterator<double[]> iter = list.iterator();
        double[] r0 = new double[3];
        while(p==null&&iter.hasNext()){
            double[] pt = iter.next();
            //r0 is the original vector that will be used for the x-axis.
            r0[0] = pt[0] - center[0];
            r0[1] = pt[1] - center[1];
            r0[2] = pt[2] - center[2];
            double mag = Vector3DOps.normalize(r0);

            if(mag==0){
                //skip, at the center.
                continue;
            }

            double[] cross = Vector3DOps.cross(normal, r0);

            mag = Vector3DOps.normalize(cross);
            if(mag==0){
                //skip, parallel to the normal
                continue;
            }

            //x-axis is the
            r0 = Vector3DOps.cross(cross, normal);
            p = cross;
        }
        final double[] r = r0;
        final double[] r2 = p;
        //none of the points could form an angle.
        if(p==null) return;

        Collections.sort(list, (d1, d2)->{
            //get the coordinates in plane.
            double[] p1 = Vector3DOps.difference(d1, center);
            double[] p2 = Vector3DOps.difference(d2, center);

            double c1 = Vector3DOps.dot(r, p1);
            double s1 = Vector3DOps.dot(r2, p1);
            double norm = Math.sqrt(c1*c1 + s1*s1);
            c1 = c1/norm;
            s1 = s1/norm;

            double c2 = Vector3DOps.dot(r, p2);
            double s2 = Vector3DOps.dot(r2, p2);
            norm = Math.sqrt(c2*c2 + s2*s2);
            c2 = c2/norm;
            s2 = s2/norm;

            int q1 = getQuadrant(c1, s1);
            int q2 = getQuadrant(c2, s2);
            if(q1==q2){
                switch(q1){
                    case 0:
                    case 1:
                        return Double.compare(-c1, -c2);
                    case 2:
                    case 3:
                        return Double.compare(c1, c2);
                }
            }
            return Integer.compare(q1,q2);


        });

    }

    static int getQuadrant(double c, double s){

        if(c>=0){
            if(s>=0){
                return 0;
            } else{
                return 3;
            }
        } else{
            if(s>=0){
                return 1;
            } else{
                return 2;
            }
        }

    }

    public List<Node3D> createMappingsDep(DeformableMesh3D mesh){
        PlaneFitter.Plane plane = PlaneFitter.findBestPlane(points);
        Furrow3D furrow = new Furrow3D(plane.center, plane.normal);
        //create a circular coordinate system in the furrow plane.


        class NodeDistance{
            NodeDistance(int p, int n){
                pt = p;
                node = n;
                double[] a = points.get(p);
                double[] b = mesh.nodes.get(n).getCoordinates();
                distance =  square(a[0] - b[0]) + square(a[1]-b[1]) + square(a[2]-b[2]);
            }

            int pt;
            int node;
            double distance;


        }
        List<NodeDistance> distances = new ArrayList<>(points.size()*mesh.nodes.size());
        for(int i = 0; i<points.size(); i++){
            for(int j = 0; j<mesh.nodes.size(); j++){
                distances.add(new NodeDistance(i,j));
            }
        }

        distances.sort((d1,d2)->Double.compare(d1.distance,d2.distance));
        Set<Integer> foundPoints = new HashSet<>();
        Set<Integer> foundNodes = new HashSet<>();
        List<NodeDistance> accepted = new ArrayList<>();
        for(NodeDistance nd: distances) {
            if ((!foundPoints.contains(nd.pt)) && (!foundNodes.contains(nd.node))) {
                foundPoints.add(nd.pt);
                accepted.add(nd);
                mappings.put(mesh.nodes.get(nd.node).getIndex(), points.get(nd.pt));
            }
            foundNodes.add(nd.node);
            if (foundPoints.size() == points.size()) {
                break;
            }
        }
            //found them all.
        Collections.sort(accepted, (a,b)->Integer.compare(a.pt, b.pt));
        List<List<Node3D>> map = DeformableMesh3DTools.generateConnectionMap(mesh);
        List<Node3D> connected = new ArrayList<>();
        for(int i = 0 ;i<accepted.size()-1; i++){
            Node3D start = mesh.nodes.get(accepted.get(i).node);
            Node3D end = mesh.nodes.get(accepted.get(i+1).node);
            connected.add(start);
            double[] s = points.get(accepted.get(i).pt);
            double[] t = points.get(accepted.get(i+1).pt);

            List<Node3D> found = DeformableMesh3DTools.findPath(map, start, end);
            if(found.size()>2){
                //interpolation is nescessary.
                double[] d = { t[0] - s[0], t[1]-s[1], t[2]-s[2] };


                double[] a = start.getCoordinates();
                double[] b = end.getCoordinates();
                double[] c = {b[0] - a[0], b[1]-a[1], b[2]-a[2]};

                double m = Vector3DOps.normalize(c);
                double cum = 0;
                for(int j = 1; j<found.size()-1; j++){
                    Node3D cNode = found.get(j);
                    double[] back = found.get(j-1).getCoordinates();
                    double[] current = cNode.getCoordinates();
                    double fraction = Vector3DOps.dot(new double[]{current[0]-back[0], current[1] - back[1], current[2]-back[2]}, c)/m;

                    if(mappings.containsKey(cNode.index)){

                    } else{
                        //connected.add(cNode);
                        if(cum<0||cum>1) cum = 0.5;
                        mappings.put(cNode.index,new double[]{
                                s[0] + d[0]*cum, s[1] + d[1]*cum, s[2] + d[2]*cum
                        } );
                    }
                }
            }
        }
        connectionMap = map;
        return connected;
    }


    final static double[]zero = {0,0,0};

    public double[] getDifference(int node, double x, double y, double z){
        double[] a = mappings.get(node);
        if(a==null){
            return zero;
        }
        return new double[]{a[0] - x, a[1]-y, a[2] - z};
    }

    public List<Node3D> refineNodes(DeformableMesh3D mesh, List<Node3D> connected) {
        List<Node3D> nodes = new ArrayList<>();
        nodes.add(connected.get(0));
        for(int i = 0; i<connected.size()-2; i++){
            Node3D a = connected.get(i);
            Node3D b = connected.get(i + 2);

            List<Node3D> found = DeformableMesh3DTools.findPath(connectionMap, a, b);
            if(found.size()==2){
            } else if(found.size()==3){
                Node3D c = connected.get(i+1);
                if(found.get(1).index==c.index){
                    nodes.add(c);
                }
            } else{
                Node3D c = connected.get(i+1);
                for(int j = 1; j<found.size()-1; j++){
                    if(found.get(j).index==c.index){
                        nodes.add(c);
                    }
                }
            }


        }
        nodes.add(connected.get(connected.size()-1));


        return nodes;
    }
}
