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
    int label;
    double[] mins = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
    double[] maxs = {-mins[0], -mins[1], -mins[2]};

    public BinaryInterceptible(List<int[]> pixels, MeshImageStack stack, int label){
        double[] img = new double[3];
        center = new double[3];
        edge = new ArrayList<>();
        this.label = label;
        for(int[] px: pixels){
            img[0] = px[0];
            img[1] = px[1];
            img[2] = px[2];

            double[] nspace = stack.getNormalizedCoordinate(img);
            center[0] += nspace[0];
            center[1] += nspace[1];
            center[2] += nspace[2];

            for(int j = 0; j<3; j++){
                mins[j] = Double.min(mins[j], nspace[j]);
                maxs[j] = Double.max(maxs[j], nspace[j]);
            }

            if(isEdge(stack, px)){
                edge.add(nspace);
            }



        }
        for(double[] pt: edge){
            for(int j = 0; j<3; j++){
                if(pt[j] < mins[j]){
                    System.out.println("too low");
                } else if(pt[j] > maxs[j]){
                    System.out.println("too high");
                }
            }
        }






        center[0] = center[0]/pixels.size();
        center[1] = center[1]/pixels.size();
        center[2] = center[2]/pixels.size();
    }


    /**
     * Image to be check. int[] pt is in px,px, slice coordinates. px are 0 based indexes
     * and slice is 1 based.
     *
     * @param stack reference values.
     * @param pt px, py, slice z points that represent the pixels in the stack.
     * @return
     */
    boolean isEdge(MeshImageStack stack, int[] pt){
        if(
                pt[0] == 0 || pt[0] == stack.getWidthPx() - 1
                || pt[1] == 0 || pt[1] == stack.getHeightPx() - 1
                || pt[2] == 1 || pt[2] == stack.getNSlices()
        ) {
            //edge of the image is an edge.
            return true;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    if (stack.getValue(pt[0] + i - 1, pt[1] + j - 1, pt[2] + k - 2) != label) {
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
