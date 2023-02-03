package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import deformablemesh.util.connectedcomponents.ConnectedComponents2D;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Create3DTrainingDataFromMeshes extends Create3DTrainingData{
    List<Track> tracks;

    public Create3DTrainingDataFromMeshes(List<Track> tracks, ImagePlus plus){
        super(plus);
        this.tracks = tracks;
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
     * Creates Cerberus training data. Which will be isotropic in dimension and contain 3 aspects. Distance Transform
     * Membrane and
     * @param args
     */
    public static void main(String[] args) throws IOException {
        new ImageJ();
        File base;
        base = args.length>=2? new File(args[0]) : new File(ij.IJ.getFilePath("select image"));
        String meshFileName = args.length>=2? args[1] : ij.IJ.getFilePath("select mesh file for " + base.getName());

        if(meshFileName==null) return;

        File meshFile = new File(meshFileName);
        List<Track> tracks = MeshReader.loadMeshes(meshFile);



        //plus = createScaledImagePlus(plus);

        ImagePlus original = ij.IJ.openImage(base.getAbsolutePath());;
        original.show();
        Path baseFolder = args.length>2 ? Paths.get(args[2]) : Paths.get(IJ.getDirectory("Select root folder"));
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
            System.out.println("running " + sliceName);
            creator.run(i);
            ImagePlus maskPlus = original.createImagePlus();
            maskPlus.setStack(creator.getLabeledStack());
            IJ.save(maskPlus, new File(labelFolder, sliceName).getAbsolutePath());
            System.out.println("saved labels: " + sliceName);
            try {
                ImagePlus scaled = creator.getOriginalFrame(i);
                scaled.setLut(LUT.createLutFromColor(Color.WHITE));
                scaled.setOpenAsHyperStack(true);
                IJ.save(scaled, new File(imageFolder, sliceName).getAbsolutePath());
                System.out.println("saved copy");
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("Finished Everything!");
        System.exit(0);
    }
}
