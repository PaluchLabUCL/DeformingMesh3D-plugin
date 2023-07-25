/*-
 * #%L
 * Triangulated surface for deforming in 3D.
 * %%
 * Copyright (C) 2013 - 2023 University College London
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package deformablemesh;

import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.simulations.FillingBinaryImage;
import deformablemesh.util.connectedcomponents.ConnectedComponents3D;
import deformablemesh.util.connectedcomponents.Region;
import deformablemesh.util.connectedcomponents.RegionGrowing;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;

public class MeshDetector {
    List<Box3D> current = new ArrayList<>();
    MeshImageStack mis;
    ImageStack threshed;
    int minSize = 50;
    public MeshDetector(MeshImageStack mis){
        this.mis = mis;
    }

    public void addRegionsToAvoid(List<Box3D> regions){
        current.addAll(regions);
    }
    List<DeformableMesh3D> guessMeshes(int level){

        //Get the current image stack for this frame/channel.
        //create a thresholded version.
        long start, end;
        start = System.currentTimeMillis();
        ImageStack currentFrame = mis.getCurrentFrame().getStack();
        threshed = new ImageStack(currentFrame.getWidth(), currentFrame.getHeight());
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
                for(Box3D box: current){
                    Box3D intersection = box.getIntersectingBox(candidate);
                    double bv = box.getVolume();
                    double iv = intersection.getVolume();
                    if((iv/cv > 0.7) || (iv/bv > 0.7)){
                        toRemove.add(region);
                        for (int[] pt : points) {
                            pixels[pt[2] - 1][pt[0] + pt[1]*width] = 0;
                            //threshed.getProcessor(pt[2]).set(pt[0], pt[1], 0);
                        }

                    }
                }
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
        for(int st = 0; st<2; st++){
            rg.step();
        }
        end = System.currentTimeMillis();
        System.out.println("regions grown: " + (start - end)/1000);
        List<DeformableMesh3D> guessed = new ArrayList<>();

        start = System.currentTimeMillis();
        for (Region region : regions) {
            int label = region.getLabel();
            List<int[]> rs = region.getPoints();

            //Collections.sort(rs, (a,b)->Integer.compare(a[2], b[2]));
            ImagePlus original = mis.original;
            ImagePlus plus = original.createImagePlus();
            int w = original.getWidth();
            int h = original.getHeight();
            ImageStack new_stack = new ImageStack(w, h);

            for (int dummy = 0; dummy < original.getNSlices(); dummy++) {
                new_stack.addSlice(new ByteProcessor(w, h));
            }
            for (int[] pt : rs) {
                new_stack.getProcessor(pt[2]).set(pt[0], pt[1], 1);
            }

            plus.setStack(new_stack);
            plus.setTitle("label: " + label);
            //plus.show();

            DeformableMesh3D mesh = FillingBinaryImage.fillBinaryWithMesh(plus, rs);
            mesh.clearEnergies();
            guessed.add(mesh);
        }
        end = System.currentTimeMillis();
        System.out.println("regions meshes in " + (start - end)/1000);

        return guessed;
    }

    ImageStack getThreshedStack(){
        return threshed;
    }
}
