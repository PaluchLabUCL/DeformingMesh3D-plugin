package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.track.Track;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.util.HashSet;
import java.util.Set;

public class Create3DTrainingDataFromLabelledImage extends Create3DTrainingData{
    MeshImageStack labels;
    public Create3DTrainingDataFromLabelledImage(ImagePlus original, ImagePlus labels){
        super(original);
        this.labels = new MeshImageStack(labels);
    }

    @Override
    public void run(int frameNumber) {
        int w = original.getWidth();
        int h = original.getHeight();

        ImageStack output = labels.getCurrentFrame().getImageStack();
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
}
