package deformablemesh.util;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.*;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IntensitySurfacePlot extends SurfacePlot{
    MeshImageStack stack;
    public IntensitySurfacePlot(DeformableMesh3D mesh, ImagePlus plus){
        this.mesh = mesh;
        stack = new MeshImageStack(plus);
        delta = stack.scaleToNormalizedLength(new double[]{1, 0, 0})[0];
    }

    public IntensitySurfacePlot(DeformableMesh3D mesh, MeshImageStack stack){
        this.mesh = mesh;
        this.stack = stack;
        delta = stack.scaleToNormalizedLength(new double[]{1, 0, 0})[0];

    }

    @Override
    public double sample(Node3D node){
        double[] loc = node.getCoordinates();
        double count = 0;
        double value = 0;
        for(int i = -range/2; i<=range/2; i++){
            for(int j = -range/2; j<=range/2; j++) {
                for (int k = -range / 2; k <= range / 2; k++) {

                    double[] point = {loc[0] + i * delta, loc[1] + j * delta, loc[2] + k * delta};
                    value += stack.getInterpolatedValue(point);
                    count++;
                }
            }
        }
        return value/count;
    }



    public double sample(Connection3D connection){
        double[] i_data = new double[3];
        Node3D interpolated = new Node3D(i_data, 0);
        double[] A = connection.A.getCoordinates();
        double[] B = connection.B.getCoordinates();
        double[] AB = Vector3DOps.difference(A, B);
        double l = Vector3DOps.normalize(AB);
        if(l==0){
            return 0;
        }
        int n = (int)(l/delta);
        if(n==0){
            return 0.5*(sample(connection.A) + sample(connection.B));
        } else{
            double ds = l/(n+1);
            double of = (l - ds*n)/2;
            double sum = 0;
            for(int i = 0; i<(n+1); i++){
                double s = ds*i + of;
                for(int j = 0; j<3; j++){
                    i_data[j] = A[j] + AB[j]*s;
                }
                sum += sample(interpolated);
            }
            return sum/(n+1);
        }

    }

    public double getAverageIntensityAtNodes(){
        double sum = 0;
        double area = 0;
        for(Node3D node: mesh.getConnectedNodes()){
            List<Triangle3D> neighbors = mesh.triangles.stream().filter(t->t.containsNode(node)).collect(Collectors.toList());
            double Amix = CurvatureCalculator.calculateMixedArea(node, neighbors);
            area += Amix;
            sum += sample(node)*Amix;

        }
        if(area>0) {
            return sum / area;
        } else{
            return 0;
        }
    }
    
    double getLength(Connection3D conn){
        double[] a = conn.A.getCoordinates();
        double[] b = conn.B.getCoordinates();

        return Vector3DOps.distance(a,b);
    }
    public double getAverageIntensityAtBoundary(){
        double sum = 0;
        List<Connection3D> connections = mesh.getOutterBounds();
        double length =0;
        for(Connection3D con: connections){
            double dl = getLength(con);
            sum += sample(con)*dl;
            length += dl;
        }

        if(length==0){
            return 0;
        }

        return sum/length;
    }


    public static void main(String[] args) throws IOException {

        List<Track> tracks = new ArrayList<>();
        tracks.addAll(MeshReader.loadMeshes(new File(args[0])));
        ImagePlus plus = new ImagePlus(new File(args[1]).getAbsolutePath());
        for(Track track: tracks) {
            for (Integer key : track.getTrack().keySet()) {
                new IntensitySurfacePlot(track.getMesh(key), plus).processAndShow(true);
            }
        }
    }

}
