package deformablemesh.ringdetection;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.util.Vector3DOps;

/**
 * A utility class for consistently converting from a furrow x-y plane to 3-d normalized coordinates. The project
 * is assumed that the x-y image is viewed by looking at the image.
 *
 */
public class FurrowTransformer{
    Furrow3D furrow;
    double[] vOffset;
    double[] xn;
    double[] yn;
    int[] px = new int[2];

    public final double scale;
    public final double invScale;

    public FurrowTransformer(Furrow3D furrow, MeshImageStack stack){
        this.furrow = furrow;
        double[] direction = furrow.normal;



        //set directions.
        double mag = Math.sqrt(direction[0]*direction[0] + direction[1]*direction[1]);
        scale = stack.SCALE/stack.pixel_dimensions[0];
        if(mag<1e-8){
            if(direction[2]>0){
                //such a small rotation away from the z-axis, just use the z-axis.
                xn = new double[]{1, 0, 0};
                yn = new double[]{0, 1, 0};
            } else{
                xn = new double[]{1, 0, 0};
                yn = new double[]{0, -1, 0};
            }



            px[0] = (int)(2*stack.offsets[0]*scale);
            px[1] = (int)(2*stack.offsets[1]*scale);


            invScale=1/scale;

        } else{

            double lx = 2*stack.offsets[0];
            double ly = 2*stack.offsets[1];
            double lz = 2*stack.offsets[2];

            //a perpendicular normal vector to the normal.
            xn = new double[]{-direction[1]/mag, direction[0]/mag, 0};

            yn = Vector3DOps.cross(direction, xn);

            //distance to walls on xaxis.
            Vector3DOps.normalize(yn);

            double dx =Vector3DOps.toSpan(xn[0], lx);
            double dy =Vector3DOps.toSpan(xn[1], ly);

            double m = dx<dy?dx:dy;

            px[0] = (int)(m*scale);

            dx = Vector3DOps.toSpan(yn[0], lx);
            dy = Vector3DOps.toSpan(yn[1],ly);
            double dz = Vector3DOps.toSpan(yn[2], lz);

            m = dx<dy?dx:dy;
            m = m<dz?m:dz;

            px[1] = (int)(m*scale);


            invScale = 1.0/scale;
        }

        vOffset = new double[]{
                furrow.cm[0] - (px[0]*0.5*xn[0] + px[1]*0.5*yn[0])*invScale,
                furrow.cm[1] - (px[0]*0.5*xn[1] + px[1]*0.5*yn[1])*invScale,
                furrow.cm[2] - (px[0]*0.5*xn[2] + px[1]*0.5*yn[2])*invScale
        };


    }

    public Furrow3D getFurrow(){
        return furrow;
    }

    /**
     * Using the normalized coordinates, this returns coordinates in the xy plane of the furrow.
     *
     * @param xyz normalized coordinates.
     * @return xy coordinates in the furrow plane.
     */
    public double[] getPlaneCoordinates(double[] xyz){
        double[] delta = {
                xyz[0] - vOffset[0],
                xyz[1] - vOffset[1],
                xyz[2] - vOffset[2]
        };


        return new double[]{Vector3DOps.dot(delta, xn)*scale, Vector3DOps.dot(delta, yn)*scale};
    }

    public double[] getVolumeCoordinates(double[] xy){
        return new double[]{
                (xy[0]*xn[0]+xy[1]*yn[0])*invScale + vOffset[0],
                (xy[0]*xn[1]+xy[1]*yn[1])*invScale + vOffset[1],
                (xy[0]*xn[2]+xy[1]*yn[2])*invScale + vOffset[2]
        };
    }



    public int getXCounts(){
        return px[0];
    }

    public int getYCounts(){
        return px[1];
    }

    public int getZCounts(){
        return 0;
    }

    public double distanceToPlane(double[] pt){
        return furrow.distanceTo(pt);
    }

    /**
     * TODO This should be some sort of general rotation form since the 'image' can be rotated.
     * rotates about the normal 1/4 of a turn clockwise.
     */
    public void rotatePiOver2() {
        //y -> x;
        //x -> -y;
        double t = xn[0];
        xn[0] = -yn[0];
        yn[0] = t;
        t = xn[1];
        xn[1] = -yn[1];
        yn[1] = t;
        t = xn[2];
        xn[2] = -yn[2];
        yn[2] = t;
        int n = px[0];
        px[0] = px[1];
        px[1] = n;

        vOffset = new double[]{
                furrow.cm[0] - (px[0]*0.5*xn[0] + px[1]*0.5*yn[0])*invScale,
                furrow.cm[1] - (px[0]*0.5*xn[1] + px[1]*0.5*yn[1])*invScale,
                furrow.cm[2] - (px[0]*0.5*xn[2] + px[1]*0.5*yn[2])*invScale
        };

    }

}