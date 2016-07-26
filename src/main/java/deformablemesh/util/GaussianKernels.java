package deformablemesh.util;

/**
 * Created by msmith on 2/1/16.
 */
public class GaussianKernels {
    static int width = 5;
    static double sigma = 2;
    static public double[] blurred1DKernel(){
        double factor = 1/Math.sqrt(2*Math.PI)/sigma;
        double[] kernel1 = new double[2*width + 1];
        double s = 1.0/sigma/sigma/2;
        for(int i = 0; i<kernel1.length; i++){
            double x = i - width;

            double f= Math.exp(-x*x*s);

            kernel1[i] = factor*f;

        }
        return kernel1;
    }


    public static double[] firstDerivative1DKernel() {
        double[] kernel1 = new double[2*width + 1];
        double s = 1.0/sigma/sigma/2;
        double factor = -1/Math.sqrt(2*Math.PI)/sigma;
        double sum = 0;
        for(int i = 0; i<kernel1.length; i++){
            double x = i - width;

            double f= -x*s*2*Math.exp(-x*x*s);

            kernel1[i] = f*factor;
            sum += Math.abs(kernel1[i]);
        }

        for(int i = 0; i<kernel1.length; i++){
            kernel1[i] = kernel1[i]/sum;
        }
        return kernel1;

    }

    public static double[] secondDerivative1DKernel() {
        double[] kernel1 = new double[2*width + 1];
        double s = 1.0/sigma/sigma/2;
        double factor = 1/Math.sqrt(2*Math.PI)/sigma;
        for(int i = 0; i<kernel1.length; i++){
            double x = i - width;

            double f= Math.exp(-x*x*s);

            kernel1[i] = factor*(x*x*s*s*4 - s*2)*f;

        }
        return kernel1;
    }
}
