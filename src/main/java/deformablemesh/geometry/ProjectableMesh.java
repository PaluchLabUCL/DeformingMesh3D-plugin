package deformablemesh.geometry;

import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.Vector3DOps;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by msmith on 6/23/16.
 */
public class ProjectableMesh implements Projectable{
    final DeformableMesh3D mesh;
    public ProjectableMesh(DeformableMesh3D mesh){
        this.mesh = mesh;
    }
    @Override
    public Shape getProjection(FurrowTransformer transformer) {
        Path2D path = new Path2D.Double();
        //slice mesh
        //List<double[]> nodes = getPlaneNodes(transformer);
        List<double[]> nodes = getSlicedTriangles(transformer);
        if(nodes.size()==0){
            return path;
        }
        for(int i=0; i<nodes.size()/2; i++){
            double[] start = nodes.get(i*2);
            double[] plane = transformer.getPlaneCoordinates(start);
            path.moveTo(plane[0], plane[1]);
            plane = transformer.getPlaneCoordinates(nodes.get(i*2+1));
            path.lineTo(plane[0], plane[1]);
        }
        /*
        //create a PolyLine
        double[] start = nodes.get(0);
        double[] plane = transformer.getPlaneCoordinates(start);
        int n = nodes.size();
        path.moveTo(plane[0], plane[1]);
        for(int i =1; i<=n; i++){

            plane = transformer.getPlaneCoordinates(nodes.get(i%n));
            path.lineTo(plane[0], plane[1]);

        }

        if(n>1) {
            path.closePath();
        }
        */
        return path;
    }

    public List<double[]> getSlicedTriangles(FurrowTransformer transformer){
        List<double[]> ret = new ArrayList<>();
        Furrow3D furrow = transformer.getFurrow();
        //gets all of the connections that are sliced by the furrow.
        List<Connection3D> connections = furrow.getIntersectionConnections(mesh.getConnections());
        if(connections.size()<2){
            return ret;
        }
        List<Connection3D> contained = new ArrayList<>(3);
        List<Connection3D> drawn = new ArrayList<>();
        for(Triangle3D triangle: mesh.triangles){
            contained.clear();
            for(Connection3D connection: connections){
                if(triangle.hasConnection(connection)){
                    contained.add(connection);
                }
                if(contained.size()==2){
                    double[] a = getIntersectionPlaneCoordinates(furrow, contained.get(0));
                    double[] b = getIntersectionPlaneCoordinates(furrow, contained.get(1));
                    ret.add(a);
                    ret.add(b);
                    break;
                }
            }

        }

        return ret;
    }


    public double[] getIntersectionPlaneCoordinates(Furrow3D furrow, Connection3D con){
        Node3D a = con.A;
        Node3D b = con.B;

        double s1 = furrow.getDistance(a);
        double s2 = furrow.getDistance(b);

        double[] ptA = a.getCoordinates();

        double[] ptB = b.getCoordinates();
        double[] diff = Vector3DOps.difference(ptB, ptA);

        double f = s1/(s1 + s2);
        return new double[] {
                ptA[0] + f*diff[0],
                ptA[1] + f*diff[1],
                ptA[2] + f*diff[2]
        };

    }

    public List<double[]> getPlaneNodes(FurrowTransformer transformer){
        List<double[]> ret = new ArrayList<>();

        Furrow3D furrow = transformer.getFurrow();
        List<Connection3D> connections = furrow.getIntersectionConnections(mesh.getConnections());
        if(connections.size()<2){
            return ret;
        }
        Set<double[]> nodes = new HashSet<>();
        double[] center = new double[3];
        for(Connection3D con: connections){

            Node3D a = con.A;
            Node3D b = con.B;

            double s1 = furrow.getDistance(a);
            double s2 = furrow.getDistance(b);

            double[] ptA = a.getCoordinates();

            double[] ptB = b.getCoordinates();
            double[] diff = Vector3DOps.difference(ptB, ptA);

            double f = s1/(s1 + s2);
            double[] pt = {
                ptA[0] + f*diff[0],
                ptA[1] + f*diff[1],
                ptA[2] + f*diff[2]
            };
            nodes.add(pt);

            center[0] += pt[0];
            center[1] += pt[1];
            center[2] += pt[2];
        }

        center[0] /= nodes.size();
        center[1] /= nodes.size();
        center[2] /= nodes.size();



        ret.addAll(nodes);


        ContractileRing.sortPointsByAngle(ret, center, furrow.normal);




        return ret;
    }


}
