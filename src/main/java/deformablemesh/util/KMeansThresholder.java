package deformablemesh.util;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.*;

/**
 * For guessing a threshold based on a kmeans algorithm.
 *
 */
public class KMeansThresholder {
    ImagePlus original;
    int levels = 10;
    List<short[]> pxData;
    Set<Integer> values;
    int ks = 4;

    /**
     * Prepares the work space for finding a kmean value.
     *
     * @param proc
     * @return
     */
    public ImageProcessor processSlice(ImageProcessor proc){
        pxData = new ArrayList<>(1);
        values = new HashSet<>();
        accumulate(proc);
        process();

        return proc;
    }

    /**
     * Creates a set of unsigned short values and collects the px as a short for analysis.
     *
     * @param ip image processor data will be taken from.
     */
    public void accumulate(ImageProcessor ip){
        short[] px = (short[])ip.getPixels();

        for(int dex = 0; dex<px.length; dex++){
            int i = px[dex]&0xffff;
            values.add(i);
        }
        pxData.add(px);

    }

    /**
     * Processes the accumulated images.
     *
     */
    public void process(){
        int[] sorted = values.stream().mapToInt(i->i).toArray();
        Arrays.sort(sorted);
        int nValues = sorted.length;
        double[] bounds = new double[ks-1];
        for(int i = 0; i<ks-1; i++){
            bounds[i] = sorted[(i+1)*nValues/ks];
        }

        double[] means = getMeans(bounds);

        for(int t = 0; t<levels; t++){
            bounds = getBoundary(means);
            getMeans(bounds);
        }



        bounds = getBoundary(means);
        for(short[] px: pxData){
            pixel:
            for(int i = 0; i<px.length; i++){
                int v = px[i]&0xffff;
                for(short j = 0; j<bounds.length; j++){
                    if(v<=bounds[j]){
                        px[i] = j;
                        continue pixel;
                    }
                }
                px[i] = (short)(ks-1);
            }
        }


    }

    /**
     * Using the boundaries between intensities calculates the means.
     *
     * @param boundaries k-1
     * @return
     */
    double[] getMeans(double[] boundaries){
        double[] means = new double[ks];

        for(short[] px: pxData){
            double[] m = getMeans(boundaries, px);
            for(int i = 0; i<ks; i++){
                means[i] += m[i];
            }
        }
        for(int i = 0; i<ks; i++){
            means[i] /= pxData.size();
        }
        return means;
    }

    double[] getBoundary(double[] means){
        double[] bounds = new double[means.length-1];
        for(int i = 0; i<bounds.length; i++){
            bounds[i] = 0.5*(means[i] + means[i+1]);
        }
        return bounds;
    }

    double[] getMeans(double[] boundary, short[] data){

        double[] updated = new double[ks];
        double[] counts = new double[ks];
        pixel:
        for(int i = 0; i<data.length; i++){
            int v = data[i]&0xffff;
            for(int j = 0; j<boundary.length; j++){
                if(v<=boundary[j]){
                    updated[j]+=v;
                    counts[j]++;
                    continue pixel;
                }
            }
            updated[ks-1]+=v;
            counts[ks-1]++;
        }

        for(int i = 0; i<ks; i++){
            double c = counts[i];
            if(c>0){
                updated[i] /= c;
            }
        }
        return updated;
    }

}
