package deformablemesh;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.FileInfoVirtualStack;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * This class will transform a mesh in normalized coordiantes of one image into the normalized coordinates of another
 * image.
 *
 * The origin units are assumed to be in "pixels" of the displayed image, I've interpretted that as
 * px, px, slice. Given an original image with bounding box, [(0, 0, 0), (W, H, D)] then another
 * bounding box [ (x0, y0, z0), (w, h, d) ] that is contained within the origin bounding box.
 *
 * The resulting origin is (-x0, -y0, -z0).
 */
public class BoundingBoxTransformer {

    //origin of bounding boxes in real units.
    double[] o1, o2;
    MeshImageStack source, destination;

    /**
     * This will transform normalized coordinates of the source image to normalized coordinates of the destination image.
     *
     * @param source image that the mesh currently exists in.
     * @param destination image that the mesh will represent
     */
    public BoundingBoxTransformer( MeshImageStack source, MeshImageStack destination){
        this.source = source;
        Calibration cal = source.original.getCalibration();
        //origin in real units
        o1 = new double[]{ cal.xOrigin * cal.pixelWidth, cal.yOrigin * cal.pixelHeight, cal.zOrigin * cal.pixelDepth};
        this.destination = destination;
        cal = destination.original.getCalibration();
        o2 = new double[]{ cal.xOrigin * cal.pixelWidth, cal.yOrigin * cal.pixelHeight, cal.zOrigin * cal.pixelDepth};

    }

    /**
     * Transforms a point from the source coordinates to a point in the destination coordiantes.
     *
     * @param pt3 the location of a point in the source MeshImageStack
     * @return location of the same point in the destination MeshImageStack
     */
    public double[] transform(double[] pt3){
        //px, px, slice
        double[] r1 = source.getImageCoordinates(pt3);
        Calibration cal = source.original.getCalibration();

        //real unit coordinate.
        r1[0] = r1[0]*cal.pixelWidth;
        r1[1] = r1[1]*cal.pixelHeight;
        r1[2] = r1[2]*cal.pixelDepth;

        //real coordinates global space
        double[] R = Vector3DOps.add(r1, o1, -1);

        //real coordinates destination space
        double[] r2 = Vector3DOps.add(R, o2, 1);
        cal = destination.original.getCalibration();

        r2[0] = r2[0]/cal.pixelWidth;
        r2[1] = r2[1]/cal.pixelHeight;
        r2[2] = r2[2]/cal.pixelDepth;
        //r2 is image coordinates destination space.

        return destination.getNormalizedCoordinate(r2);

    }

    /**
     * Applies "transform" to all of the points in the mesh.
     *
     * @param mesh that gets transformed.
     */
    public void transformMesh(DeformableMesh3D mesh){
        for(Node3D node: mesh.nodes){
            node.setPosition(transform(node.getCoordinates()));
        }
    }

    /**
     * Applies transformMesh to all of the meshes in the track.
     *
     * @param track to be transformed.
     */
    public void transformTrack(Track track){
        for(Integer i: track.getTrack().keySet()){
            transformMesh(track.getMesh(i));
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(Paths.get(".").toRealPath());

        ImagePlus plus = FileInfoVirtualStack.openVirtual("");
        ImagePlus plus2 = FileInfoVirtualStack.openVirtual( "");

        MeshImageStack one = new MeshImageStack(plus);
        MeshImageStack two = new MeshImageStack(plus2);

        BoundingBoxTransformer bbt = new BoundingBoxTransformer(one, two);
        double[] x1 = Arrays.copyOf(one.offsets, 3);
        double[] x2 = {-x1[0], -x1[1], -x1[2]};

        double[] x1p = bbt.transform(x1);
        double[] x2p = bbt.transform(x2);
        double[] x3p = bbt.transform(new double[]{0, 0, 0});

        //BoundingBoxTransformer inv = new BoundingBoxTransformer(two, one);
        //double[] x1pp = inv.transform(x1p);

        System.out.println(Arrays.toString( x2p ));
        System.out.println(Arrays.toString( x3p ));
        System.out.println(Arrays.toString( x1p ));
        //after transforming the real width should not have changed.
        double realWidth = (x1p[0] - x2p[0]) * two.SCALE;
        System.out.println("real world with of one transformed to two: " + realWidth );
        //System.out.println(Arrays.toString( x1pp ));
        bbt = new BoundingBoxTransformer(two, one);
        double[] x1pp = bbt.transform(x1p);
        double[] x2pp = bbt.transform(x2p);
        double[] x3pp = bbt.transform(x3p);

        //BoundingBoxTransformer inv = new BoundingBoxTransformer(two, one);
        //double[] x1pp = inv.transform(x1p);

        System.out.println(Arrays.toString( x2pp ));
        System.out.println(Arrays.toString( x3pp ));
        System.out.println(Arrays.toString( x1pp ));

    }

}
