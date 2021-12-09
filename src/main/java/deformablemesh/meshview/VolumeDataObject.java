package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Box3D;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by smithm3 on 23/02/18.
 */
public class VolumeDataObject implements DataObject {
    Color color;
    Sizeable3DSurface surface;
    MultiChannelVolumeTexture volume;
    double[][][] texture_data;
    double scale;
    /*size of the backing texture block, (w, h, d), essentially pixels.*/
    int[] sizes;
    double[] offsets;
    double[] lengths;

    BranchGroup branchGroup;
    TransformGroup tg;
    double min  = 0;
    double max = 1;

    double tLow = 1;
    double tHigh = 1;

    public VolumeDataObject(Color c) {
        color = c;
        offsets = new double[]{0,0,0};
    }

    public void setColor(Color c){
        color = c;
    }

    /**
     * Sets the position of the lowest corner. .
     * @param x
     * @param y
     * @param z
     */
    public void setPosition(double x, double y, double z){
        offsets = new double[]{x , y, z};

        if(tg!=null){
            Transform3D tt = new Transform3D();
            tg.getTransform(tt);

            Vector3d n = new Vector3d(x, y, z);

            tt.setTranslation(n);

            tg.setTransform(tt);
        }
    }

    /**
     * For creating a volume representing the all of the pixes of the provided mesh image stack.
     *
     */
    public void setTextureData(VolumeDataObject vdo, int[] low, int[] high){
        int lowx = low[0];
        int highx = high[0] - 1;
        int lowy = low[1];
        int highy = high[1] - 1;
        int lowz = low[2];
        int highz = high[2] - 1;

        int d = highz - lowz + 1;
        int h = highy - lowy + 1;
        int w = highx - lowx + 1;

        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }

        sizes = new int[]{w, h, d};
        double[] unit = {sizes[0], sizes[1], sizes[2]};
        //size of the texture backing data in normalized units.
        lengths = vdo.lengths;
        for(int z = 0; z<d; z++){
            for(int y = 0; y<h; y++){
                for(int x = 0; x<w; x++){
                    texture_data[x][y][z] = vdo.texture_data[low[0] +  x][low[1] + y][low[2] + z];
                }
            }
        }
        offsets = vdo.offsets;
        updateVolume();
    }

    /**
     * For creating a volume representing the all of the pixes of the provided mesh image stack.
     *
     * @param stack
     */
    public void setTextureData(MeshImageStack stack){
        int lowx = 0;
        int highx = stack.getWidthPx() - 1;
        int lowy = 0;
        int highy = stack.getHeightPx() - 1;
        int lowz = 0;
        int highz = stack.getNSlices() - 1;

        int d = highz - lowz + 1;
        int h = highy - lowy + 1;
        int w = highx - lowx + 1;

        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }

        sizes = new int[]{w, h, d};
        double[] unit = {sizes[0], sizes[1], sizes[2]};
        //size of the texture backing data in normalized units.
        lengths = stack.scaleToNormalizedLength(new double[]{sizes[0], sizes[1], sizes[2]});
        for(int z = 0; z<d; z++){
            for(int y = 0; y<h; y++){
                for(int x = 0; x<w; x++){
                    texture_data[x][h - y - 1][z] = stack.getValue(x, y, z);
                }
            }
        }
        setPosition(0, 0, -stack.offsets[2]);
        updateVolume();
    }


    /**
     * For creating a volume that shows part of an image stack.
     * #TODO fix: this shouldn't change anything regarding the geometry.
     *
     * @param stack
     * @param pts
     */
    public void setTextureData(MeshImageStack stack, List<int[]> pts){
        int lowx = 0;
        int highx = stack.getWidthPx() - 1;
        int lowy = 0;
        int highy = stack.getHeightPx() - 1;
        int lowz = 0;
        int highz = stack.getNSlices() - 1;
        IntSummaryStatistics iss = pts.stream().mapToInt(i->i[2]).summaryStatistics();
        System.out.println(iss.getMin() + ", " + iss.getMax() );
        int d = highz - lowz + 1;
        int h = highy - lowy + 1;
        int w = highx - lowx + 1;

        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }
        sizes = new int[]{w, h, d};

        for(int[] pt: pts){
            texture_data[pt[0]-lowx][h - pt[1] + lowy - 1][pt[2]-lowz-1] = 255;
        }
        lengths = stack.scaleToNormalizedLength(new double[]{sizes[0], sizes[1], sizes[2]});
        updateVolume();
    }

    public void setMinMaxRange(double min, double max){
        this.min = min;
        this.max = max;
        updateVolume();
    }

    public void setTransparencyTrim(double low, double high){
        tLow = low;
        tHigh = high;
        if(volume != null){
            volume.setTransparencyRange(0, tLow, tHigh);
            surface.setTexture(volume);
        }
    }
    /**
     * Creates the 3D representation of the data in "texture_data"
     *
     */
    public void updateVolume(){
        Color volumeColor = color;
        //size of the texture backing data.

         volume = new MultiChannelVolumeTexture(texture_data, min, max, new Color3f(volumeColor));


        if(surface==null){
            /*
             * The surface is positioned such that the origin corner is at -lengths[0]/2, -lengths[1]/2, 0
             */
            surface = new Sizeable3DSurface(volume, sizes, lengths);

            tg = new TransformGroup();
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

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

    public double[] getMaxRangeMinMax(){
        return volume.getMaxRangeMinMax(0);
    }
    public double[] getClampedMinMax(){
        return volume.getAbsoluteMinMax(0);
    }
    public double[] getMinMax() {
        return new double[] {min, max};
    }
}
