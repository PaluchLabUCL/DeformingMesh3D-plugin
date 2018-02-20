package deformablemesh;

import deformablemesh.geometry.Box3D;
import ij.ImagePlus;

/**
 * Created by smithm3 on 20/02/18.
 */
public class MeshImageSubStack extends MeshImageStack{
    final Box3D region;
    final MeshImageStack backing;

    public MeshImageSubStack(Box3D region, MeshImageStack backing, ImagePlus sample){
        super(sample);
        this.region = region;
        this.backing = backing;
    }


}
