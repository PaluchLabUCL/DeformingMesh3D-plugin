package deformablemesh.geometry;

import deformablemesh.ringdetection.FurrowTransformer;

import java.awt.Shape;

/**
 * Created by msmith on 2/8/16.
 */
public interface Projectable {
    Shape getProjection(FurrowTransformer transformer);
}
