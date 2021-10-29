package deformablemesh.geometry;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.gui.meshinitialization.CircularMeshInitializationDialog;
import deformablemesh.util.Vector3DOps;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.Binary;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    MeshImageStack mis;
    public BinaryMomentsOfInertia(DeformableMesh3D mesh, MeshImageStack stack){
        pxSizes = stack.pixel_dimensions;
        pxSizes = new double[]{pxSizes[0]/stack.SCALE, pxSizes[1]/stack.SCALE, pxSizes[2]/stack.SCALE};
        binary = DeformableMesh3DTools.createBinaryRepresentation(stack, mesh);
        mis = stack;
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
                    Ixx += (y*y + z*z) + IxxCm;
                    Ixy += -x*y;
                    Ixz += -x*z;
                    Iyy += (x*x + z*z) + IyyCm;
                    Iyz += -y*z;
                    Izz += (x*x + y*y) + IzzCm;
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
     * Returns the list of eigen vectors, and the eigen values.
     *
     * @return [ {v1}, {v2}, {v3}, {lambda1, lambda2, lambda3} ]
     */
    public List<double[]> getEigenVectors(){
        if(cm==null){
            calculateCenterOfMass();
            calculateInertialMatrix();
        }
        Matrix mat = new Matrix(I, 3);
        EigenvalueDecomposition ed = mat.eig();
        List<double[]> values = new ArrayList<>(4);
        double[] ev = ed.getRealEigenvalues();
        double[] vecs = ed.getV().getColumnPackedCopy();
        for(int i = 0; i<3; i++){
            double[] vec = new double[3];
            for(int j = 0; j<3; j++){
                vec[j] = vecs[3*i + j];
            }
            Vector3DOps.normalize(vec);
            values.add(vec);
        }
        values.add(ev);
        return values;
    }
    /**
     * Create an image and a mesh
     * @param args
     */
    public static void main(String[] args){
        twoSphereCheck();
    }

    public static void reactangleCheck(String[] args){
        ImageJ.main(args);

        ImageStack stack = new ImageStack(400, 400);
        for(int i = 0; i<40; i++){
            ImageProcessor proc = new ShortProcessor(400, 400);

            stack.addSlice(proc);
        }
        ImagePlus plus = new ImagePlus("test", stack);
        Calibration cal = plus.getCalibration();
        cal.pixelDepth = 2.0;
        cal.pixelWidth = 0.5;
        cal.pixelHeight = 0.5;
        plus.setDimensions(1, 20, 1);
        //plus.show();

        MeshImageStack mis = new MeshImageStack(plus);

        DeformableMesh3D mesh = DeformableMesh3DTools.createRectangleMesh(60/mis.SCALE, 30/mis.SCALE, 30/mis.SCALE, 10/mis.SCALE);
        double[] axis = new double[] {0, 0, 1};
        mesh.rotate(axis, new double[]{0, 0, 0}, Math.PI*0.15);
        mesh.translate(new double[]{37.5/mis.SCALE, 0, 0});
        BinaryMomentsOfInertia inertia = new BinaryMomentsOfInertia(mesh, mis);
        List<double[]> values = inertia.getEigenVectors();
        inertia.binary.show();
        System.out.println("area: " + DeformableMesh3DTools.calculateSurfaceArea(mesh)*Math.pow(mis.SCALE, 2));
        System.out.println("volume: " + DeformableMesh3DTools.calculateVolume(new double[]{1, 0, 0}, mesh.positions, mesh.triangles)*Math.pow(mis.SCALE, 2));

        System.out.println(Arrays.toString(inertia.getCenterOfMass()));
        System.out.println(Arrays.toString(inertia.getInertialMatrix()));
        System.out.println(inertia.size);
        double[] eig = values.get(3);
        for(int i = 0; i<3; i++){
            double[] vec = values.get(i);
            System.out.print(eig[i] + " :: ");
            for(int j = 0; j<3; j++){

                System.out.print(vec[j] + "\t");

            }
            System.out.print("\n");
        }
    }

    public static void twoSphereCheck(){

        ImageStack stack = new ImageStack(400, 400);
        for(int i = 0; i<40; i++){
            ImageProcessor proc = new ShortProcessor(400, 400);

            stack.addSlice(proc);
        }
        ImagePlus plus = new ImagePlus("test", stack);
        Calibration cal = plus.getCalibration();
        cal.pixelDepth = 2.0;
        cal.pixelWidth = 0.5;
        cal.pixelHeight = 0.5;
        plus.setDimensions(1, 20, 1);
        //plus.show();

        MeshImageStack mis = new MeshImageStack(plus);

        Sphere a = new Sphere(new double[] {-0.05, -0.05, 0}, 0.1);
        Sphere b = new Sphere(new double[] {0.05, 0.05, 0}, 0.1);
        List<Sphere> spheres = Arrays.asList(a, b);

        DeformableMesh3D mesh = CircularMeshInitializationDialog.createMeshFromSpheres(spheres, 2);

        BinaryMomentsOfInertia inertia = new BinaryMomentsOfInertia(mesh, mis);
        List<double[]> values = inertia.getEigenVectors();
        inertia.binary.show();
        System.out.println("area: " + DeformableMesh3DTools.calculateSurfaceArea(mesh)*Math.pow(mis.SCALE, 2));
        System.out.println("volume: " + DeformableMesh3DTools.calculateVolume(new double[]{1, 0, 0}, mesh.positions, mesh.triangles)*Math.pow(mis.SCALE, 2));

        System.out.println(Arrays.toString(inertia.getCenterOfMass()));
        System.out.println(Arrays.toString(inertia.getInertialMatrix()));
        System.out.println(inertia.size);
        double[] eig = values.get(3);
        for(int i = 0; i<3; i++){
            double[] vec = values.get(i);
            System.out.print(eig[i] + " :: ");
            for(int j = 0; j<3; j++){

                System.out.print(vec[j] + "\t");

            }
            System.out.print("\n");
        }
    }

    public double measureAverageIntensity() {
        ImageStack stack = binary.getStack();
        double sum = 0;
        double count = 0;
        double[] pxSpace = new double[3];
        for(int i = 1; i<=binary.getStackSize(); i++){
            ImageProcessor binProc = stack.getProcessor(i);
            pxSpace[2] = i;
            for(int j = 0; j<stack.getWidth(); j++){
                for(int k = 0; k<stack.getHeight(); k++){
                    if(binProc.get(j, k)!=0){
                        pxSpace[0] = j;
                        pxSpace[1] = k;

                        count++;
                        sum += mis.getInterpolatedValue(mis.getNormalizedCoordinate(pxSpace));

                    }
                }
            }
        }
        return sum/count;

    }

    public double volume() {

        return size*pxSizes[0]*pxSizes[1]*pxSizes[2];
    }
}
