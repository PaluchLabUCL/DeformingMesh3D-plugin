package deformablemesh.geometry;

import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.PlotSurface;
import deformablemesh.track.Track;
import deformablemesh.util.HotAndCold;
import deformablemesh.util.Vector3DOps;
import lightgraph.DataSet;
import lightgraph.Graph;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
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

    static public double[] calculateMeanNormal(Node3D node, List<Triangle3D> triangles){

        double[] normal = new double[3];

        for(Triangle3D triangle: triangles){
            triangle.update();
            Node3D[] nodes = {triangle.A, triangle.B, triangle.C};
            int dex = 0;
            for(int i = 0; i<3; i++){
                if(nodes[i].index==node.index){
                    dex = i;
                }
            }

            double[] a = nodes[dex].getCoordinates();
            double[] b = nodes[(dex+1)%3].getCoordinates();
            double[] c = nodes[(dex+2)%3].getCoordinates();

            double[] ab = new double[3];
            double[] bc = new double[3];
            double[] ca = new double[3];

            double mab = 0;
            double mbc = 0;
            double mca = 0;

            for(int i = 0;i<3; i++){
                ab[i] = b[i] - a[i];
                bc[i] = c[i] - b[i];
                ca[i] = a[i] - c[i];

                mab += ab[i]*ab[i];
                mbc += bc[i]*bc[i];
                mca += ca[i]*ca[i];
            }

            double[] abCrossBc = Vector3DOps.cross(ab, bc);
            double abDotBc = Vector3DOps.dot(ab, bc);
            double mx1 = Vector3DOps.mag(abCrossBc);
            double cotB = - abDotBc / mx1;
            double bcDotCa = Vector3DOps.dot(bc, ca);
            double[] bcCrossCa = Vector3DOps.cross(bc, ca);
            double mx2 = Vector3DOps.mag(bcCrossCa);
            double cotC = - bcDotCa/mx2;

            double v;
            if(mbc>(mab + mca)){
                v = triangle.area/2;
            } else if (mca>(mab + mbc) || mab>(mbc + mca)){
                v = triangle.area/4;
            } else{
                v = 0.125*(mab*cotB + mca*cotC);
            }

            for(int i = 0; i<3; i++){
                normal[i] += triangle.normal[i]*v;
            }


        }

        Vector3DOps.normalize(normal);

        return normal;

    }



    /**
     *
     *
     * @param node
     * @param triangles
     * @return
     */
    static public double[] calculateMeanCurvatureNormal(Node3D node, List<Triangle3D> triangles){
        double Amixed= 0;
        double[] kappa = new double[3];

        double[] normal = new double[3];
        for(Triangle3D triangle: triangles){
            triangle.update();
            Node3D[] nodes = {triangle.A, triangle.B, triangle.C};
            int dex = 0;
            for(int i = 0; i<3; i++){
                if(nodes[i].index==node.index){
                    dex = i;
                }
            }

            double[] a = nodes[dex].getCoordinates();
            double[] b = nodes[(dex+1)%3].getCoordinates();
            double[] c = nodes[(dex+2)%3].getCoordinates();

            double[] ab = new double[3];
            double[] bc = new double[3];
            double[] ca = new double[3];

            double mab = 0;
            double mbc = 0;
            double mca = 0;

            for(int i = 0;i<3; i++){
                ab[i] = b[i] - a[i];
                bc[i] = c[i] - b[i];
                ca[i] = a[i] - c[i];

                mab += ab[i]*ab[i];
                mbc += bc[i]*bc[i];
                mca += ca[i]*ca[i];
            }

            double[] abCrossBc = Vector3DOps.cross(ab, bc);
            double abDotBc = Vector3DOps.dot(ab, bc);
            double mx1 = Vector3DOps.mag(abCrossBc);
            double cotB = - abDotBc / mx1;
            double bcDotCa = Vector3DOps.dot(bc, ca);
            double[] bcCrossCa = Vector3DOps.cross(bc, ca);
            double mx2 = Vector3DOps.mag(bcCrossCa);
            double cotC = - bcDotCa/mx2;
            for(int i = 0; i<3; i++){
                kappa[i] += 0.5*cotC*(-ab[i]);
                kappa[i] += 0.5*cotB*(ca[i]);

            }

            double v;
            if(mbc>(mab + mca)){
                v = triangle.area/2;
            } else if (mca>(mab + mbc) || mab>(mbc + mca)){
                v = triangle.area/4;
            } else{
                v = 0.125*(mab*cotB + mca*cotC);
            }
            Amixed += v;
            for(int i = 0; i<3; i++){
                normal[i] += triangle.normal[i]*v;
            }


        }

        Vector3DOps.normalize(normal);

        for(int i = 0; i<kappa.length; i++){
            kappa[i] = kappa[i]/(Amixed);
        }

        return kappa;
    }
    public double[] getNormalAndCurvature(Node3D node, List<Triangle3D> triangles){
        double[] kappa = calculateMeanCurvatureNormal(node, triangles);
        double[] normal = calculateMeanNormal(node, triangles);
        double dot = Vector3DOps.dot(kappa, normal);
        return new double[]{normal[0], normal[1], normal[2], dot};
    }

    public List<double[]> calculateCurvature(){
        List<double[]> values = new ArrayList<>();


        for(Triangle3D tri: mesh.triangles){
            tri.update();
            addNode(tri.A, tri, node_to_triangle);
            addNode(tri.B, tri, node_to_triangle);
            addNode(tri.C, tri, node_to_triangle);
        }

        System.out.println("nodes: " + mesh.nodes.size());

        for(Node3D node: mesh.nodes){
            List<Triangle3D> t_angles = node_to_triangle.get(node);
            //all touching triangles.
            if(t_angles==null) continue;

            double[] curvature = getNormalAndCurvature(node, t_angles);
            double[] pt = node.getCoordinates();
            values.add(new double[]{pt[0], pt[1], pt[2], curvature[3], curvature[0], curvature[1], curvature[2] });


        }
        return values;
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

    public static void main(String[] args) throws IOException {
        List<Track> tracks = new ArrayList<>();
        tracks.addAll(MeshWriter.loadMeshes(new File(args[0])));
        Track t = new Track(new ArrayList<>());
        DeformableMesh3D ex = RayCastMesh.sphereRayCastMesh(4);

        tracks.add(t);
        //t.addMesh(1, ex);

        Graph plot = new Graph();


        Graph xs = new Graph();
        Graph ys = new Graph();
        Graph zs = new Graph();
        for(Track track: tracks){
            for(Integer key: track.getTrack().keySet()){

                MeshFrame3D viewer = new MeshFrame3D();

                viewer.showFrame(true);
                viewer.addLights();
                viewer.setBackgroundColor(Color.BLACK);


                DeformableMesh3D mesh = track.getMesh(key);
                //mesh.create3DObject();
                //viewer.addDataObject(mesh.data_object);



                CurvatureCalculator calc = new CurvatureCalculator(mesh);

                List<double[]> curvatures = calc.calculateCurvature();

                List<double[]> curves = calc.createCurvatureHistogram(curvatures);

                double[] x = new double[curvatures.size()];
                double[] y = new double[curvatures.size()];
                double[] z = new double[curvatures.size()];
                double[] kh = new double[curvatures.size()];

                double kave = 0;
                double kave_2 = 0;
                double kmin = 0;
                double kmax = 0;

                for(double[] row: curvatures){
                    kave += row[3];
                    kave_2 += row[3]*row[3];
                    kmax = row[3]>kmax?row[3]:kmax;
                    kmin = row[3]<kmin?row[3]:kmin;
                }

                kave = kave/curvatures.size();
                kave_2 = kave_2/curvatures.size() - kave*kave;

                double sigma = Math.sqrt(kave_2);

                kmax = kmax>sigma + kave?sigma + kave:kmax;
                kmin = kmin<kave - sigma?kave - sigma :kmin;

                HotAndCold neg = new HotAndCold(Color.BLUE, Color.BLACK);
                HotAndCold pos = new HotAndCold(Color.ORANGE, Color.BLACK);

                if(kmin<0){
                    kmin = -kmin;
                }

                neg.setMinMax(0, kmin);
                pos.setMinMax(0, kmax);

                float[] colors = new float[mesh.positions.length];
                for (int i = 0; i<curvatures.size(); i++){
                    double[] row = curvatures.get(i);
                    float[] c;
                    if(row[3]<0){
                        c = neg.getColor(-row[3]);
                    } else{
                        c = pos.getColor(row[3]);
                    }
                    System.arraycopy(c, 0, colors, 3*i, 3);

                }

                PlotSurface surface = new PlotSurface(mesh.positions, mesh.triangle_index, colors);
                viewer.addDataObject(surface);

                for(int i = 0; i<curvatures.size(); i++){
                    double[] row = curvatures.get(i);




                    x[i] = row[0];
                    y[i] = row[1];
                    z[i] = row[2];
                    kh[i] = row[3];

                    double scale = row[3]/kmax;
                    if(scale>1){
                        scale = 1;
                    }

                    double factor = 0.1*scale;


                    /*
                    double[] xyz = {
                            x[i],
                            y[i],
                            z[i],
                            x[i] + factor*row[4],
                            y[i] + factor*row[5],
                            z[i] + factor*row[6]
                    };
                    Node3D ptA = new Node3D(xyz, 0);
                    Node3D ptB = new Node3D(xyz, 1);

                    LineDataObject obj = new LineDataObject(Arrays.asList(ptA, ptB), 0.5f);
                    viewer.addDataObject(obj);
                    */
                }

                xs.addData(x,kh).setLabel(track.getName());
                ys.addData(y,kh).setLabel(track.getName());
                zs.addData(z,kh).setLabel(track.getName());

                DataSet set = plot.addData(curves.get(0), curves.get(1));
                set.setLabel(track.getName());

            }

        }
        plot.show(true);
        xs.show(false, "xaxis");
        ys.show(false, "yaxis");
        zs.show(false, "zaxis");

    }


}

