package deformablemesh.geometry;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import ij.ImagePlus;
import ij.ImageStack;

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
                if(bytes[k]>0){
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
        for(int i = 0; i<binary.getNSlices(); i++){
            double z = (i+0.5)*pxSizes[2];

            final byte[] bytes = (byte[]) binaryStack.getPixels(i+1);
            for(int k = 0; k<bytes.length; k++){
                if(bytes[k]>0){
                    double x = (k%w + 0.5)*pxSizes[0];
                    double y = (k/w + 0.5)*pxSizes[1];
                    tally += 1;
                    Ixx += x*x;
                    Ixy += x*y;
                    Ixz += x*z;
                    Iyy += y*y;
                    Iyz += y*z;
                    Izz += z*z;
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




    }

}
