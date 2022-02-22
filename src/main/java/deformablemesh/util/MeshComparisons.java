package deformablemesh.util;

import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import lightgraph.Graph;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class MeshComparisons {
    static double ds = 1.0/128.0;
    static double dv = ds*ds*ds;

    static double getMeshOverlap(DeformableMesh3D a, DeformableMesh3D b){
        Box3D boxA = a.getBoundingBox();
        Box3D boxB = b.getBoundingBox();
        Box3D intersection = boxA.getIntersectingBox(boxB);
        double w = intersection.high[0] - intersection.low[0];
        double h = intersection.high[1] - intersection.low[1];
        double d = intersection.high[2] - intersection.low[2];
        if( w < ds || h < ds || d < ds){
            return 0;
        }
        int xSteps = (int)(w/ds + 0.5);
        int ySteps = (int)(h/ds + 0.5);
        int zSteps = (int)(d/ds + 0.5);
        double overlap = 0;
        double vA = a.calculateVolume();
        double vB = b.calculateVolume();
        InterceptingMesh3D imA = new InterceptingMesh3D(a);
        InterceptingMesh3D imB = new InterceptingMesh3D(b);
        for(int i = 0; i<xSteps; i++){
            for(int j = 0; j<ySteps; j++){
                for(int k = 0; k<zSteps; k++){
                    double[] pt = {
                            intersection.low[0] + ds*(i + 0.5),
                            intersection.low[1] + ds*(j + 0.5),
                            intersection.low[2] + ds*(k + 0.5)
                    };

                    if(imA.contains(pt) && imB.contains(pt)){
                        overlap += dv;
                    }
                }
            }
        }

        return (overlap)/(vA + vB - overlap);
    }

    static double getBoundOverlap(DeformableMesh3D a, DeformableMesh3D b){
        Box3D boxA = a.getBoundingBox();
        Box3D boxB = b.getBoundingBox();
        double vA = boxA.getVolume();
        double vB = boxB.getVolume();
        Box3D intersection = boxA.getIntersectingBox(boxB);
        double vi = intersection.getVolume();

        return vi / (vA + vB - vi);
    }
    public static void main(String[] args) throws IOException {

        List<Track>  tracks = MeshReader.loadMeshes(new File(args[0]));
        int low = Integer.MAX_VALUE;
        int high = 0;
        for(int i = 0; i<tracks.size(); i++){

            high = Math.max(tracks.get(i).getLastFrame(), high);
            low = Math.min(low, tracks.get(i).getFirstFrame());

        }
        long start = System.currentTimeMillis();

        Graph plot = new Graph();
        for(int i = low; i <= high; i++){
            final Integer frame = i;
            List<DeformableMesh3D> meshes = tracks.stream().filter(
                    t->t.containsKey(frame)
            ).map(
                    t->t.getMesh(frame)
            ).collect(Collectors.toList());
            for(int j = 0; j<meshes.size(); j++){
                for( int k = j + 1; k < meshes.size(); k++){
                    DeformableMesh3D a = meshes.get(j);
                    DeformableMesh3D b = meshes.get(k);

                    plot.addData(new double[]{getMeshOverlap(a, b)}, new double[] {getBoundOverlap(a, b)});

                }
            }

        }
        long duration = System.currentTimeMillis() - start;

        plot.setYRange(0, 1);

        plot.show(true, "overlaps: " + (duration/1000l) );

    }
}
