package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import deformablemesh.util.connectedcomponents.ConnectedComponents2D;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.Resizer;
import ij.process.*;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Create3DTrainingDataFromMeshes {
    List<Track> tracks;
    MeshImageStack stack;
    ImagePlus original;
    ImageStack mask;
    ImageStack membrane;
    ImageStack distance;
    boolean categoricalDistanceLabels = true;
    public Create3DTrainingDataFromMeshes(List<Track> tracks, ImagePlus plus){
        this.tracks = tracks;
        this.stack = new MeshImageStack(plus);
        original = plus;


    }

    public void showCurrent(){
        new ImagePlus("mask", mask).show();
        new ImagePlus("distance", distance).show();
        new ImagePlus("outlines", membrane).show();
    }

    public void run(int meshFrameNumber){
        int w = original.getWidth();
        int h = original.getHeight();

        ImageStack output = new ImageStack(original.getWidth(), original.getHeight());
        int n = original.getNSlices();

        for(int j = 0; j<n; j++){
            output.addSlice(new ColorProcessor(w, h));
        }
        double total = tracks.size();
        Set<Integer> used = new HashSet<>();
        for(Track t: tracks){
            if(t.containsKey(meshFrameNumber)){
                Integer color = t.getColor().getRGB();
                while (used.contains(color)) {
                    color++;
                }
                used.add(n);
                DeformableMesh3DTools.mosaicBinary(stack, output, t.getMesh(meshFrameNumber), color);
            }
        }
        mask = createMaskImage(output);
        System.out.println("masked'd");

        membrane = createMembraneImage(output);
        System.out.println("membraned");

        distance = createDistanceMapImage(output);
        System.out.println("distance map'd");

        for(int i = 1; i<=mask.size(); i++){
            ImageProcessor maskProcessor = mask.getProcessor(i);
            ImageProcessor membraneProcessor = membrane.getProcessor(i);
            ImageProcessor distanceProcessor = distance.getProcessor(i);

            for(int k = 0; k<w*h; k++){
                int msk = maskProcessor.get(k);
                int mem = membraneProcessor.get(k);
                int d = distanceProcessor.get(k);
                //bit 0->background 1->membrane, 2->mask, 3+->distance
                if(msk==0) {
                    maskProcessor.set(k, 1);
                    //msk=0;
                } else{
                    maskProcessor.set(k, 4);
                    msk = 4;
                }
                if(mem != 0){
                    maskProcessor.set(k, 2);
                } else if(d!=0) {
                    d = d << 3;
                    maskProcessor.set(k, d + msk);
                }

            }
        }

    }

    /**
     * Creates a binary stack (ByteProcessor#threshold(int)) from the provided labelled image. First a threshold
     * is used, then a fill holes is used.
     *
     * @param mosaic labelled image, where non-zero values represent a different cell/mesh
     * @return stack of masked images.
     */
    public ImageStack createMaskImage(ImageStack mosaic){
        ImageStack mask = new ImageStack(mosaic.getWidth(), mosaic.getHeight());
        for(int i = 1; i<=mosaic.size(); i++){
            ByteProcessor p = mosaic.getProcessor(i).convertToByteProcessor();
            p.threshold(0);
            fillHoles(p);
            mask.addSlice(p);

        }
        return mask;
    }


    /**
     * Fills holes, by inverting the processor (1-> 0 and 0->1) then labels the connected components. All of the regions
     * that do not touch the edge are set to 255.
     *
     * @param p binary in put processor.
     */
    public void fillHoles(ImageProcessor p){
        ImageProcessor p2 = p.duplicate();
        p2.invert();
        ConnectedComponents2D cc = new ConnectedComponents2D(p2);
        Map<Integer, List<int[]>> points = cc.getRegions();
        for(Integer i: points.keySet()){
            List<int[]> pixels = points.get(i);

            if(!touchesEdge(pixels, p.getWidth(), p.getHeight())){
                for(int[] px: pixels){
                    p.set(px[0], px[1], 255);
                }
            }
        }
    }

    /**
     * checks if *any* of the provided pixels touch the edge of the box bound by x ∈ [0, width) and y ∈ [0, height)
     *
     * @param pixels x,y pairs of points.
     * @param width upper bounds for x. x=0 or x=width-1 touches.
     * @param height upper bounds for y. y=0 or y=height-1 touches.
     * @return true if any pixel touches and edge, false otherwise.
     */
    public boolean touchesEdge(List<int[]> pixels, int width, int height){

        for(int[] px: pixels){
            if(px[0]==0 || px[1]==0 || px[0]==width-1 || px[1]==height-1){
                return true;
            }
        }

        return false;

    }

    /**
     * Creates a "binary" image, 255 or 0, of the membrane by checking neighboring neighboring pixels for either being
     * background or another blob.
     *
     * @param mosaic stack of Labelled images. Each label corresponds to a separate cell/mesh
     *
     * @return stack stack of images with 255 for membrane and 0 for non-membrane.
     */
    public ImageStack createMembraneImage(ImageStack mosaic){
        int w = mosaic.getWidth();
        int h = mosaic.getHeight();
        ImageStack membrane = new ImageStack(w, h);
        System.out.println("starting membrane " + mosaic.size());

        for(int i = 1; i<=mosaic.size(); i++){
            ByteProcessor p = new ByteProcessor(w, h);
            ImageProcessor mos = mosaic.getProcessor(i);
            for(int j = 0; j<w; j++){

                for(int k = 0; k<h; k++){
                    int px = mos.get(j, k);
                    if(px!=0){
                        if(j==0 || k==0 || j==w-1 || k==h-1){
                            p.set(j, k, 255);
                            continue;
                        }
                        px_check:
                        for(int dn = -1; dn<=1; dn++){
                            for(int dm = -1; dm<=1; dm++){
                                if(mos.get(j+dn, k+dm)!=px){
                                    p.set(j, k, 255);
                                    break px_check;
                                }
                            }
                        }
                    }

                }

            }
            membrane.addSlice(p);

        }
        return membrane;
    }

    public ImageStack createDistanceMapImage(ImageStack mosaic){
        System.out.println("starting transform");
        ImagePlus container = original.createImagePlus();
        container.setStack(mosaic);
        DistanceTransformMosaicImage dtmi = new DistanceTransformMosaicImage(container);
        System.out.println("starting");
        dtmi.findBlobs();
        System.out.println("blobbed");
        dtmi.createCascades();
        System.out.println("cascading");
        //dtmi.createScaledCascades(2); //TODO make this non-fixed.
        System.out.println("Creating image!");
        ImagePlus plus = dtmi.createLabeledImage();
        System.out.println("Image Created");
        return plus.getStack();
    }

    /**
     * Creates Cerberus training data. Which will be isotropic in dimension and contain 3 aspects. Distance Transform
     * Membrane and
     * @param args
     */

    public static void main(String[] args) throws IOException {
        new ImageJ();
        File base;
        base = args.length==2? new File(args[0]) : new File(ij.IJ.getFilePath("select image"));
        String meshFileName = args.length==2? args[1] : ij.IJ.getFilePath("select mesh file for " + base.getName());

        if(meshFileName==null) return;

        File meshFile = new File(meshFileName);
        List<Track> tracks = MeshWriter.loadMeshes(meshFile);



        //plus = createScaledImagePlus(plus);
        ImagePlus original = ij.IJ.openImage(base.getAbsolutePath());;
        original.show();
        Path baseFolder = Paths.get(IJ.getDirectory("Select root folder"));
        Create3DTrainingDataFromMeshes creator = new Create3DTrainingDataFromMeshes(tracks, original);

        File labelFolder = baseFolder.resolve("labels").toFile();
        if(!labelFolder.exists()){
            labelFolder.mkdirs();
        }
        File imageFolder = baseFolder.resolve("images").toFile();
        if(!imageFolder.exists()){
            imageFolder.mkdirs();
        }
        String name = original.getTitle().replace(".tif", "");

        for(int i = 0; i < original.getNFrames(); i++){
            String sliceName = String.format("%s-t%04d.tif", name, i);
            creator.run(i-1);
            ImagePlus maskPlus = original.createImagePlus();
            maskPlus.setStack(creator.getLabeledStack());
            IJ.save(maskPlus, new File(labelFolder, sliceName).getAbsolutePath());
            System.out.println("finished working");
            //maskPlus.show();
            try {
                ImagePlus scaled = creator.getOriginalFrame(i);
                scaled.setLut(LUT.createLutFromColor(Color.WHITE));
                scaled.setOpenAsHyperStack(true);
                IJ.save(scaled, new File(imageFolder, sliceName).getAbsolutePath());

            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public ImagePlus getOriginalFrame(Integer tp) {
        System.out.println("getting frame: " + tp);
        ImagePlus frame = original.createImagePlus();
        ImageStack stack = new ImageStack(original.getWidth(), original.getHeight());
        int n = original.getNSlices();
        System.out.println("t " + tp + " n " + n +" of " + original.getStackSize());
        int c = original.getNChannels();
        ImageStack originStack = original.getStack();
        int tpSize = n*c;

        for(int i = 1; i<=tpSize; i++){
            stack.addSlice(originStack.getProcessor( ( tp ) * ( c * n ) + i ) );
        }
        frame.setStack(stack, c, n, 1);
        return frame;
    }

    public ImageStack getLabeledStack() {
        return mask;
    }
}
