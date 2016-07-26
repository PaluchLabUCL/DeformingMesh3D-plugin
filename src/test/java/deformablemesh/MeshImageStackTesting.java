package deformablemesh;

import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by msmith on 4/21/16.
 */
public class MeshImageStackTesting {

    public static ImagePlus testStack(){
        ImageStack stack = new ImageStack(10, 30);
        for(int i = 0; i<5; i++){
            ImageProcessor improc = new ShortProcessor(10, 30);
            for(int j = 0; j<10; j++){
                for(int k = 0; k<30; k++){
                    int s = 2*j + ((2*k)<<4) + ((2*i)<<8);
                    improc.set(j,k,s);
                }
            }
            stack.addSlice("no label",improc);
        }

        ImagePlus imp = new ImagePlus("original",stack);
        FileInfo info = imp.getFileInfo();
        info.pixelHeight = 0.25;
        info.pixelWidth = 0.25;
        info.pixelDepth = 0.5;

        imp.setFileInfo(info);

        return imp;

    }

    @Test
    public void sampleTest(){

        ImagePlus original = testStack();

        MeshImageStack mesh_stack = new MeshImageStack(original);

        double[] normalized;
        double[] img;

        //center
        normalized = new double[]{0,0,0};

        //exact -ish
        img = mesh_stack.getImageCoordinates(normalized);

        //pixel
        double[] img2 = {(int)img[0], (int)img[1], (int)img[2]};

        double v = mesh_stack.getInterpolatedValue(mesh_stack.getNormalizedCoordinate(img2));

        double i = original.getStack().getProcessor((int)img2[2]+1).getPixelValue((int)img2[0], (int)img2[1]);

        Assert.assertEquals(i, v, Vector3DOps.TOL);
    }

}
