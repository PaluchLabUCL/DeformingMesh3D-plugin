package deformablemesh.plugins;

import deformablemesh.MeshImageStack;
import deformablemesh.util.DistanceTransformMosaicImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class DistanceTransform_Plugin implements PlugInFilter {
    MeshImageStack stack;
    ImagePlus original;
    @Override
    public int setup(String s, ImagePlus imagePlus) {
        stack = new MeshImageStack(imagePlus);
        original = imagePlus;
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        ImageStack result = new ImageStack(stack.getWidthPx(), stack.getHeightPx() );


        for(int i = 0; i<stack.getNFrames(); i++) {
            long start = System.currentTimeMillis();
            System.out.println("starting frame: " + i);
            stack.setFrame(i);
            ImagePlus frame = stack.getCurrentFrame();
            DistanceTransformMosaicImage dtmi = new DistanceTransformMosaicImage(frame);
            dtmi.findBlobs();
            dtmi.createCascades();
            ImageStack frames = dtmi.createLabeledImage().getStack();
            for(int j = 1; j<=frames.getSize(); j++){
                result.addSlice(frames.getSliceLabel(j), frames.getProcessor(j));
            }
            long finished = System.currentTimeMillis() - start;
        }
        ImagePlus transformed = original.createImagePlus();
        transformed.setTitle(original.getShortTitle() +  "-transformed.tif");
        transformed.setStack(result, 1, stack.getNSlices(), stack.getNFrames());
        transformed.setOpenAsHyperStack(true);
        transformed.show();
    }
}
