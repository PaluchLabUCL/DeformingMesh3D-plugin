package deformablemesh.geometry;

import deformablemesh.MeshImageStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * class for using
 */
public class BinaryInterceptible implements Interceptable{
    double[] center;
    List<double[]> edge;
    public BinaryInterceptible(List<int[]> pixels, MeshImageStack stack){
        double[] img = new double[3];
        center = new double[3];
        edge = new ArrayList<>();
        for(int[] px: pixels){
            img[0] = px[0];
            img[1] = px[1];
            img[2] = px[2];

            double[] nspace = stack.getNormalizedCoordinate(img);
            center[0] += nspace[0];
            center[1] += nspace[1];
            center[2] += nspace[2];

            if(isEdge(stack, px)){
                edge.add(nspace);
            }



        }
        center[0] = center[0]/pixels.size();
        center[1] = center[1]/pixels.size();
        center[2] = center[2]/pixels.size();
    }

    boolean isEdge(MeshImageStack stack, int[] pt){
        for(int i = 0; i<3; i++){
            for(int j = 0; j<3; j++){
                for(int k = 0; k<3; k++){
                    if(stack.getValue(pt[0] + i -1, pt[1] + j - 1, pt[2] + k - 1)==0){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Assuming the origin is contained within the shape, and and the pixels are points.
     * Should be updated to think of pixels as voxels.
     * @param origin
     * @param direction
     * @return
     */
    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        double min = Double.MAX_VALUE;
        double[] best = origin;
        for(double[] pt: edge){
            double dx = pt[0] - origin[0];
            double dy = pt[1] - origin[1];
            double dz = pt[2] - origin[2];
            double m = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if(m==0){
                continue;
            }
            dx = dx/m;
            dy = dy/m;
            dz = dz/m;

            dx = direction[0] - dx;
            dy = direction[1] - dy;
            dz = direction[2] - dz;

            m = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if(m<min){
                min = m;
                best = pt;
            }
        }

        return Arrays.asList(new Intersection(best, direction));
    }

    public double[] getCenter() {
        return center;
    }
}
