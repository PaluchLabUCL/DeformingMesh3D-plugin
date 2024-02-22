package deformablemesh.examples;

import deformablemesh.MeshDetector;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Box3D;
import deformablemesh.util.connectedcomponents.ConnectedComponents3D;
import deformablemesh.util.connectedcomponents.Region;
import deformablemesh.util.connectedcomponents.RegionGrowing;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PredictFromDistanceTransform {

    public static void main(String[] args){
        Path ip = Paths.get(args[0]).toAbsolutePath();
        Path op = ip.getParent().resolve(ip.getFileName().toString().replace(".tif", "-labelled.tif"));


        ImagePlus distanceTransform = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        int level = Integer.parseInt(args[1]);
        int minSize = 5;

        MeshImageStack mis = new MeshImageStack(distanceTransform);
        ImageStack stack = new ImageStack(mis.getWidthPx(), mis.getHeightPx());
        for(int frame = 0; frame<distanceTransform.getNFrames(); frame++){
            mis.setFrame(frame);
            long start, end;
            start = System.currentTimeMillis();
            ImageStack currentFrame = mis.getCurrentFrame().getStack();
            ImageStack threshed = new ImageStack(currentFrame.getWidth(), currentFrame.getHeight());
            for(int i = 1; i<= currentFrame.size(); i++){
                ImageProcessor proc = currentFrame.getProcessor(i).convertToShort(false);
                proc.threshold(level);
                threshed.addSlice(proc);
            }
            end = System.currentTimeMillis();
            System.out.println("prepared binary image: " + (end - start)/1000);
            start = System.currentTimeMillis();
            List<Region> regions = ConnectedComponents3D.getRegions(threshed);
            end = System.currentTimeMillis();
            System.out.println(regions.size() + " regions detected in " + (end - start)/1000);

            Integer biggest = -1;
            int size = 0;
            List<Region> toRemove = new ArrayList<>();
            int small = 0;

            start = System.currentTimeMillis();

            short[][] pixels = new short[threshed.getSize()][];
            for(int i = 0; i<threshed.getSize(); i++){
                pixels[i] = (short[])threshed.getPixels(i+1);
            }

            for (Region region : regions) {
                Integer key = region.getLabel();
                List<int[]> points = region.getPoints();

                int width = threshed.getWidth();

                if (points.size() < minSize) {
                    small++;
                    toRemove.add(region);
                    for (int[] pt : points) {
                        pixels[pt[2] - 1][pt[0] + pt[1]*width] = 0;
                        //threshed.getProcessor(pt[2]).set(pt[0], pt[1], 0);
                    }
                } else {
                    double[] rmin = mis.getNormalizedCoordinate(region.getLowCorner());
                    double[] rmax = mis.getNormalizedCoordinate(region.getHighCorner());

                    Box3D candidate = new Box3D(rmin[0], rmin[1], rmin[2], rmax[0], rmax[1], rmax[2]);
                    double cv = candidate.getVolume();
                    boolean obstructed = false;

                    for (int[] pt : points) {
                        pixels[pt[2] - 1][pt[0] + pt[1]*width] = (short)key.shortValue();
                        //threshed.getProcessor(pt[2]).set(pt[0], pt[1], key);
                    }
                }
                if (points.size() > size) {
                    size = points.size();
                    biggest = key;
                }

            }
            System.out.println(small + " to small. Biggest: " + biggest + " size of: " + size);
            for (Region region : toRemove) {
                regions.remove(region);
            }
            end = System.currentTimeMillis();
            System.out.println( "removed small in: " + (end - start)/1000);

            start = System.currentTimeMillis();
            ImageStack growing = new ImageStack(currentFrame.getWidth(), currentFrame.getHeight());
            int nlev = 1;
            for(int i = 1; i<= currentFrame.size(); i++){
                ImageProcessor proc = currentFrame.getProcessor(i).convertToShort(false);
                proc.threshold(nlev);
                growing.addSlice(proc);
            }
            RegionGrowing rg = new RegionGrowing(threshed, growing);
            rg.setRegions(regions);
            while( rg.getFrontierSize() > 0 ){
                rg.step();
            }
            end = System.currentTimeMillis();
            System.out.println("regions grown: " + (end - start)/1000);
            int w = threshed.getWidth();
            int h = threshed.getHeight();
            int n = h*w;
            for(int i = 0; i<threshed.getSize(); i++){
                ColorProcessor proc = new ColorProcessor(w, h);
                short[] labels = (short[])threshed.getProcessor(i+1).getPixels();
                for(int j = 0; j<n; j++){
                    int rgb = (( (labels[j]*137)%255 )<<16) +(( (labels[j]*61)%255 )<<8) + ( (labels[j]*13)%255 );
                    proc.set(j, rgb);
                }
                stack.addSlice(proc);
            }
        }




        ImagePlus plus = mis.getOriginalPlus().createImagePlus();
        plus.setStack(stack);
        plus.setDimensions(1, mis.getNSlices(), mis.getNFrames());
        IJ.save(plus, op.toString());
    }
}
