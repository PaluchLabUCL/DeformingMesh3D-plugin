package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.Binary;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Polygon;
import java.util.Arrays;

/**
 * This class is designed to use the binary representation to calculate the center of mass and moments of inertia
 *
 * Created on 23.08.17.
 */
public class BinaryMomentsOfInertia {
    double[] cm;
    double[] I;
    double size;
    ImagePlus binary;
    double[] pxSizes;
    public BinaryMomentsOfInertia(DeformableMesh3D mesh, MeshImageStack stack){
        pxSizes = stack.pixel_dimensions;
        binary = DeformableMesh3DTools.createBinaryRepresentation(stack, mesh);

    }
    /**
     * center of mass:
     *          c_x = sum_i [x_i m_i] / sum_i [m_i]
     *          c_y = sum_i[ y_i m_i] / sum_i [m_i]
     *          c_z = sum_i[ z_i m_i] / sum_i [m_i]
     * @return {c_x, c_y, c_z}
     */
    public double[] getCenterOfMass(){
        return cm;
    }

    private void calculateCenterOfMass(){
        cm = new double[3];
        ImageStack binaryStack = binary.getStack();
        int w = binary.getWidth();
        int h = binary.getHeight();
        double tally = 0;
        for(int i = 0; i<binary.getNSlices(); i++){
            double z = (i+0.5)*pxSizes[2];

            final byte[] bytes = (byte[]) binaryStack.getPixels(i+1);
            for(int k = 0; k<bytes.length; k++){
                if((0xff&(int)bytes[k])>0){
                    double x = (k%w + 0.5)*pxSizes[0];
                    double y = (k/w + 0.5)*pxSizes[1];
                    tally += 1;
                    cm[0] += x;
                    cm[1] += y;
                    cm[2] += z;
                }
            }
        }
        size = tally;
        cm[0] = cm[0]/tally;
        cm[1] = cm[1]/tally;
        cm[2] = cm[2]/tally;
    }

    /**
     * Moments of inertia.
     *
     *         Ixx = sum_i [x_ix_im_i]/ sum_i [m_i]
     *         Ixy = sum_i [x_iy_im_i]/ sum_i [m_i]
     *         Ixz = sum_i [x_iz_im_i]/ sum_i [m_i]
     *         Iyx = Ixy
     *         Iyy = sum_i [y_iy_im_i]/ sum_i [m_i]
     *         Iyz = sum_i [y_iz_im_i]/ sum_i [m_i]
     *         Izx = Ixz
     *         Izy = Iyz
     *         Izz = sum_i [z_iz_im_i]/ sum_i [m_i]
     *
     * @return { Ixx, Ixy, Ixz, Iyx, Iyy, Iyz, Izx, Izy, Izz};
     */
    public double[] getInertialMatrix(){
        return I;
    }

    private void calculateInertialMatrix(){
        double Ixx = 0;
        double Ixy = 0;
        double Ixz = 0;
        double Iyy = 0;
        double Iyz = 0;
        double Izz = 0;
        ImageStack binaryStack = binary.getStack();
        int w = binary.getWidth();
        int h = binary.getHeight();
        double tally = 0;
        double IxxCm = 1.0/12.0*(pxSizes[1]*pxSizes[1] + pxSizes[2]*pxSizes[2]);
        double IyyCm = 1.0/12.0*(pxSizes[0]*pxSizes[0] + pxSizes[2]*pxSizes[2]);
        double IzzCm = 1.0/12.0*(pxSizes[1]*pxSizes[1] + pxSizes[0]*pxSizes[0]);

        for(int i = 0; i<binary.getNSlices(); i++){
            double z = (i+0.5)*pxSizes[2] - cm[2];

            final byte[] bytes = (byte[]) binaryStack.getPixels(i+1);
            for(int k = 0; k<bytes.length; k++){
                if((0xff&(int)bytes[k])>0){
                    double x = (k%w + 0.5)*pxSizes[0] - cm[0];
                    double y = (k/w + 0.5)*pxSizes[1] - cm[1];
                    tally += 1;
                    Ixx += x*x + IxxCm;
                    Ixy += x*y;
                    Ixz += x*z;
                    Iyy += y*y + IyyCm;
                    Iyz += y*z;
                    Izz += z*z + IzzCm;
                }
            }
        }
        I = new double[]{
                Ixx, Ixy, Ixz,
                Ixy, Iyy, Iyz,
                Ixz, Iyz, Izz
        };
    }
    
    /**
     * Create an image and a mesh
     * @param args
     */
    public static void main(String[] args){
        ImageJ.main(args);

        ImageStack stack = new ImageStack(400, 400);
        for(int i = 0; i<20; i++){
            ImageProcessor proc = new ShortProcessor(400, 400);
            if(i>=5&&i<15){
                proc.setColor(Short.MAX_VALUE);
                Polygon p = new Polygon(new int[]{175, 175, 375, 375}, new int[]{150, 200, 200, 150}, 4);
                proc.fillPolygon(p);
            }
            stack.addSlice(proc);
        }
        ImagePlus plus = new ImagePlus("test", stack);
        Calibration cal = plus.getCalibration();
        cal.pixelDepth = 2.0;
        cal.pixelWidth = 0.5;
        cal.pixelHeight = 0.5;
        plus.setDimensions(1, 20, 1);
        plus.show();

        MeshImageStack mis = new MeshImageStack(plus);

        DeformableMesh3D mesh = DeformableMesh3DTools.createRectangleMesh(60/mis.SCALE, 30/mis.SCALE, 30/mis.SCALE, 10/mis.SCALE);
        mesh.rotate(new double[]{0, 0, 1}, new double[]{0, 0, 0}, Math.PI*0.1);
        mesh.translate(new double[]{37.5/mis.SCALE, 0, 0});
        BinaryMomentsOfInertia inertia = new BinaryMomentsOfInertia(mesh, mis);
        inertia.binary.show();
        inertia.calculateCenterOfMass();
        inertia.calculateInertialMatrix();

        System.out.println(Arrays.toString(inertia.getCenterOfMass()));
        System.out.println(Arrays.toString(inertia.getInertialMatrix()));
        System.out.println(inertia.size);
    }

}
