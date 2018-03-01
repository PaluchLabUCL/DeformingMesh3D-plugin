package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;
import snakeprogram3d.display3d.DataObject;
import snakeprogram3d.display3d.ThreeDSurface;
import snakeprogram3d.display3d.VolumeTexture;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by smithm3 on 23/02/18.
 */
public class VolumeDataObject implements DataObject {
    Color color;
    Sizeable3DSurface surface;
    double[][][] texture_data;
    double scale;
    int[] sizes;
    double[] offsets;
    BranchGroup branchGroup;

    public VolumeDataObject(Color c) {
        color = c;
        offsets = new double[]{0,0,0};
    }
    public void setPosition(double x, double y, double z){
        offsets = new double[]{x, y, z};
    }

    public void setTextureData(MeshImageStack stack, List<int[]> pts){
        int lowx = Integer.MAX_VALUE;
        int highx = -lowx;
        int lowy = Integer.MAX_VALUE;
        int highy = highx;
        int lowz = Integer.MAX_VALUE;
        int highz = -lowz;

        for(int[] pt: pts){
            lowx = pt[0]<lowx?pt[0]:lowx;
            lowy = pt[1]<lowy?pt[1]:lowy;
            lowz = pt[2]<lowz?pt[2]:lowz;

            highx = pt[0]>highx?pt[0]:highx;
            highy = pt[1]>highy?pt[1]:highy;
            highz = pt[2]>highz?pt[2]:highz;
        }

        int d = highz - lowz + 1;
        int h = highy - lowy + 1;
        int w = highx - lowx + 1;

        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }
        sizes = new int[]{w, h, d};

        for(int[] pt: pts){
            texture_data[pt[0]-lowx][pt[1]-lowy][pt[2]-lowz] = 255;
        }
        updateVolume(stack);

    }

    /**
     * Creates the 3D representation of the data in "texture_data"
     *
     */
    public void updateVolume(MeshImageStack original){


        Color volumeColor = color;
        int min = 0;
        int max = 1;
        double[] unit = {sizes[0], sizes[1], sizes[2]};
        double[] lengths = original.scaleToNormalizedLength(unit);
        VolumeTexture volume = new VolumeTexture(texture_data, min, max, new Color3f(volumeColor));
        if(surface==null){

            surface = new Sizeable3DSurface(volume, sizes, lengths);

            final TransformGroup tg = new TransformGroup();

            Transform3D tt = new Transform3D();
            tg.getTransform(tt);

            Vector3d n = new Vector3d(offsets[0], offsets[1], offsets[2]);

            tt.setTranslation(n);

            tg.setTransform(tt);
            tg.addChild(surface.getBranchGroup());
            branchGroup = new BranchGroup();
            branchGroup.addChild(tg);
            branchGroup.setCapability(BranchGroup.ALLOW_DETACH);

        } else{
            surface.setTexture(volume);
        }
    }

    @Override
    public BranchGroup getBranchGroup() {
        return branchGroup;
    }
}
