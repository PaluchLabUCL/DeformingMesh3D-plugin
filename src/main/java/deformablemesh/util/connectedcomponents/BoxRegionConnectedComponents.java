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
package deformablemesh.util.connectedcomponents;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.io.MeshWriter;
import deformablemesh.simulations.FillingBinaryImage;
import deformablemesh.track.Track;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * For combining tiled regions of connected components.
 */
public class BoxRegionConnectedComponents {
    Map<Integer, List<Region>> regions;
    int N = 4;
    static class OffsetStack{
        final ImageStack stack;
        final int x, y;
        OffsetStack(int x, int y, int w, int h, ImageStack full){
            stack = new ImageStack(w, h);
            this.x = x;
            this.y = y;
            for(int i = 0; i<full.getSize(); i++){
                ImageProcessor proc = full.getProcessor(i+1);
                proc.setRoi(x, y, w, h);
                stack.addSlice(proc.crop());
            }
        }
    }
    public BoxRegionConnectedComponents() {

    }



    List<Region> getRegions(ImageStack threshed){
        int wi = threshed.getWidth()/N;
        int hi = threshed.getHeight()/N;
        List<OffsetStack> chunks = new ArrayList<>();
        for(int i = 0; i<N; i++){
            for(int j = 0; j<N; j++){
                int w = wi;
                int h = hi;
                if(i == N-1){
                    //last column
                    w = threshed.getWidth() - wi*(N-1);
                }
                if(j == N - 1){
                    //last row.
                    h = threshed.getHeight() - hi*(N-1);
                }

                OffsetStack stack = new OffsetStack(wi*i, hi*j, w, h, threshed);
                chunks.add(stack);
            }
        }
        List<Region> results = new ArrayList<>();
        Set<Integer> labels = new HashSet<>();

        for(OffsetStack stack: chunks){
            List<Region> regions = ConnectedComponents3D.getRegions(stack.stack);
            int offset = labels.size();
            for(Region region: regions){
                if(stack.x != 0 || stack.y != 0){
                    region.translate(stack.x, stack.y, 0);
                }
                int label = region.label + offset;
                while (labels.contains(label)) {
                    label = label+1;
                }
                region.label = label;
                labels.add(label);
                results.add(region);
            }
        }

        return results;
    }

    public List<DeformableMesh3D> guessMeshes(MeshImageStack stack){
        int cutoff = 200;
        int frame = 0;
        int level = 1;

        //Get the current image stack for this frame/channel.
        //create a thresholded version.
        long start, end;
        start = System.currentTimeMillis();
        ImagePlus original = stack.getCurrentFrame();
        ImageStack currentFrame = original.getStack();
        ImageStack threshed = new ImageStack(currentFrame.getWidth(), currentFrame.getHeight());
        for(int i = 1; i<= currentFrame.size(); i++){
            ImageProcessor proc = currentFrame.getProcessor(i).convertToShort(false);
            proc.threshold(level);
            threshed.addSlice(proc);
        }
        end = System.currentTimeMillis();
        System.out.println("prepared binary image: " + (end - start)/1000);
        start = System.currentTimeMillis();
        List<Region> regions = getRegions(threshed);
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
        int width = threshed.getWidth();

        for (Region region : regions) {
            Integer key = region.getLabel();
            List<int[]> points = region.getPoints();


            if (points.size() < cutoff) {
                small++;
                toRemove.add(region);
                for (int[] pt : points) {
                    pixels[pt[2] - 1][pt[0] + pt[1]*width] = 0;
                }
            } else {
                for (int[] pt : points) {
                    pixels[pt[2] - 1][pt[0] + pt[1]*width] = (short)key.shortValue();
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
        int nlev = 2*level/3;
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
        System.out.println("regions grown: " + (end - start)/1000);
        List<DeformableMesh3D> guessed = new ArrayList<>();

        start = System.currentTimeMillis();

        for(Region region: regions){
            int label = region.getLabel();
            List<int[]> rs = region.getPoints();

            //Collections.sort(rs, (a,b)->Integer.compare(a[2], b[2]));
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
        System.out.println("regions meshes in " + (end - start)/1000);
        //add all of the guessed meshes to new tracks in this frame.
        return guessed;
    }
    public static void main(String[] args) throws IOException {
        ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        BoxRegionConnectedComponents brcc = new BoxRegionConnectedComponents();

        List<DeformableMesh3D> guessedMeshes = brcc.guessMeshes(new MeshImageStack(plus));
        List<Track> tracks = new ArrayList<>();
        int a = 1;
        for(DeformableMesh3D mesh: guessedMeshes){
            Track t = new Track("" + a);
            t.addMesh(0, mesh);
            tracks.add(t);
        }
        MeshWriter.saveMeshes(new File("testings-region-based-cc.bmf"), tracks);

    }

}
