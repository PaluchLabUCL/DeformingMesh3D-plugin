package deformablemesh.externalenergies;

import deformablemesh.MeshImageStack;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Created by msmith on 1/20/16.
 */
public class GradientEnergy implements ExternalEnergy {

    MeshImageStack stack;
    double weight;
    double dr;

    public GradientEnergy(MeshImageStack stack, double image_weight){
        this.stack = stack;

        dr = stack.getMinPx();


        weight = image_weight;
    }
    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double[] working = new double[3];
        for(int i = 0; i<positions.length/3; i++){

            double x = positions[3*i + 0];
            double y = positions[3*i + 1];
            double z = positions[3*i + 2];
            working[0] = x;
            working[1] = y;
            working[2] = z;



            fx[i] += get2ndGradient(working, 0)*weight;
            fy[i] += get2ndGradient(working, 1)*weight;
            fz[i] += get2ndGradient(working, 2)*weight;

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        double v = getGradient(pos);
        return v;
    }


    double getGradient(double[] xyz){
        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];
        double dx = 0;
        double dy = 0;
        double dz = 0;
        for(int i = 0; i<2; i++){
            xyz[0] = x + (2*i-1)*dr;
            dx += (2*i-1)*stack.getInterpolatedValue(xyz);
            xyz[0] = x;
            xyz[1] = y + (2*i - 1)*dr;
            dy += (2*i-1)*stack.getInterpolatedValue(xyz);
            xyz[1] = y;
            xyz[2] = z + (2*i - 1)*dr;
            dz += (2*i-1)*stack.getInterpolatedValue(xyz);
            xyz[2] = z;

        }
        double v = abs(dx) + abs(dy) + abs(dz);

        return v;
    }

    double abs(double v){
        if(v==0)return 0;

        return v<0?-v:v;
    }

    double get2ndGradient(double[] xyz, int direction){
        double v = 0;
        double o = xyz[direction];

        for(int i = 0; i<2; i++){
            xyz[direction] = o + (2*i-1)*dr;
            v += (2*i-1)*getGradient(xyz);

        }
        xyz[direction] = o;
        return v;
    }

    public static void main(String[] args){
        ImageJ.main(args);
        ImagePlus imp = new ImagePlus("practice/sphere-2.tif");
        imp.show();
        MeshImageStack stack = new MeshImageStack(imp);
        GradientEnergy grad = new GradientEnergy(stack, 1.0);
        ImageStack x = new ImageStack(imp.getWidth(), imp.getHeight());
        ImageStack y = new ImageStack(imp.getWidth(), imp.getHeight());
        ImageStack z = new ImageStack(imp.getWidth(), imp.getHeight());

        int last = imp.getNSlices();
        for(int i = 1; i<=last; i++){
            ImageProcessor px = new FloatProcessor(imp.getWidth(), imp.getHeight());
            ImageProcessor py = new FloatProcessor(imp.getWidth(), imp.getHeight());
            ImageProcessor pz = new FloatProcessor(imp.getWidth(), imp.getHeight());
            for(int j = 0; j<imp.getHeight(); j++){
                for(int k = 0; k<imp.getWidth(); k++){
                    double[] pt = stack.getNormalizedCoordinate(new double[]{k, j, i-1});
                    px.setf(k,j,(float)grad.get2ndGradient(pt, 0));
                    py.setf(k,j,(float)grad.get2ndGradient(pt, 1));
                    pz.setf(k,j,(float)grad.get2ndGradient(pt, 2));
                }
            }
            x.addSlice(px);
            y.addSlice(py);
            z.addSlice(pz);
        }
        new ImagePlus("gx", x).show();
        new ImagePlus("gy", y).show();
        new ImagePlus("gz", z).show();

    }

}
