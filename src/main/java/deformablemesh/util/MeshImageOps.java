package deformablemesh.util;

import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Created by msmith on 2/9/16.
 */
public class MeshImageOps {

    public static void refineBinary(ImageStack binary_stack, ImageStack original_stack){
        //first go through and find points along the edge of the binary.
        for(int k = 0; k<binary_stack.getSize(); k+=1){
            ImageProcessor proc = binary_stack.getProcessor(k+1);
            for(int i = 0; i<binary_stack.getWidth();i++){
                for(int j = 0; j<binary_stack.getHeight();j++){

                    if(proc.get(i, j)>0 && checkNeighbors(i,j,k+1,binary_stack)){
                        proc.set(i, j, 2);
                    }

                }
            }
        }
        for(int k = 0; k<binary_stack.getSize(); k+=1){
            ImageProcessor proc = binary_stack.getProcessor(k+1);
            ImageProcessor o_proc = original_stack.getProcessor(k + 1);
            for(int i = 0; i<binary_stack.getWidth();i++){
                for(int j = 0; j<binary_stack.getHeight();j++){
                    if(proc.getf(i,j)==0.25f){
                        if(o_proc.getPixelValue(i,j)==0){
                            proc.set(i, j, 0);
                        } else{
                            proc.set(i, j, 255);
                        }
                    }
                }
            }
        }

    }

    /**
     * Checks the neighborhood for a zero to see if this point lies on an edge.
     * This should be used if the pixel at x,y,z is 1. Assumes all values outside
     * of image are zero.
     *
     * @param x
     * @param y
     * @param z
     * @param stack binary image where 0 is not the object and >0 is the object.
     *
     * @return if there is a zero in the 6-box neighborhood.
     */
    static public boolean checkNeighbors(int x, int y, int z, ImageStack stack){
        ImageProcessor proc;
        //above
        if(z>1){
            proc = stack.getProcessor(z-1);
            if(proc.getf(x,y)==0) return true;
        } else{
            return true;
        }
        if(z<stack.getSize()){
            proc = stack.getProcessor(z+1);
            if(proc.getf(x,y)==0) return true;
        } else{
            return true;
        }

        proc = stack.getProcessor(z);

        if(x>0){
            if(proc.getf(x-1,y)==0) return true;
        } else{
            return true;
        }

        if(x<proc.getWidth()-1){
            if(proc.getf(x+1,y)==0) return true;
        } else{
            return true;
        }

        if(y>0){
            if(proc.getf(x, y-1)==0) return true;
        } else{
            return true;
        }

        if(y<proc.getHeight()-1){
            if(proc.getf(x, y+1)==0) return true;
        } else{
            return true;
        }

        return false;
    }

    public static void applyMask(ImageStack original_stack, ImageStack binary_stack){
        double value = 0;
        int volume = 0;
        for(int k = 0; k<binary_stack.getSize(); k+=1){
            ImageProcessor proc = binary_stack.getProcessor(k+1);
            ImageProcessor o_proc = original_stack.getProcessor(k + 1);
            for(int i = 0; i<binary_stack.getWidth();i++){
                for(int j = 0; j<binary_stack.getHeight();j++){
                    if(proc.getPixelValue(i,j)==0){
                        o_proc.setf(i,j,0);
                    } else{
                        value += o_proc.getPixelValue(i,j);
                        volume++;
                    }
                }
            }
        }

        value = value/volume;

        for(int k = 0; k<binary_stack.getSize(); k+=1){
            ImageProcessor o_proc = original_stack.getProcessor(k + 1);
            for(int i = 0; i<binary_stack.getWidth();i++){
                for(int j = 0; j<binary_stack.getHeight();j++){
                    double v = o_proc.getPixelValue(i,j);
                    if(v>value){
                        o_proc.setf(i,j,(float)(v-value));
                    } else{
                        o_proc.setf(i,j,0);
                    }
                }
            }

        }

    }

    static public double calculateVolume(ImageStack binary){
        int sum = 0;
        for(int i = 1; i<=binary.getSize(); i++){
            ImageProcessor processor = binary.getProcessor(i);
            for(int j = 0; j<binary.getWidth(); j++){
                for(int k = 0; k<binary.getHeight(); k++){
                    if(processor.getPixelValue(j,k)>0){
                        sum++;
                    }
                }
            }
        }
        return sum;
    }

    static double[] getSizeOfBlob(ImageStack binary){
        double[] xyz = new double[3];
        int volume = 0;
        for(int k = 0; k<binary.getSize(); k+=1){
            ImageProcessor proc = binary.getProcessor(k+1);
            for(int i = 0; i<binary.getWidth();i++){
                for(int j = 0; j<binary.getHeight();j++){
                    if(proc.getPixelValue(i,j)==0){
                    } else{
                        volume++;
                        xyz[0] += i;
                        xyz[1] += j;
                        xyz[2] += k;
                    }
                }
            }
        }


        for(int i = 0; i<3; i++){
            xyz[i] = xyz[i]/volume;
        }
        double[] max_l = new double[3];

        for(int k = 0; k<binary.getSize(); k+=1){
            ImageProcessor proc = binary.getProcessor(k+1);
            for(int i = 0; i<binary.getWidth();i++){
                for(int j = 0; j<binary.getHeight();j++){
                    double v = proc.getPixelValue(i,j);
                    if(v>0){
                        double lx = Math.abs(xyz[0] - i);
                        if(lx>max_l[0]){
                            max_l[0] = lx;
                        }
                        double ly = Math.abs(xyz[1] - j);
                        if(ly>max_l[1]){
                            max_l[1] = ly;
                        }
                        double lz = Math.abs(xyz[2] - k);
                        if(lz>max_l[2]){
                            max_l[2] = lz;
                        }

                    } else{
                    }
                }
            }
            //o_proc.filter(ImageProcessor.FIND_EDGES);

        }
        return max_l;

    }

    static public double[] getCenter(ImageStack binary){
        int sum = 0;

        double sumx = 0;
        double sumy = 0;
        double sumz = 0;

        for(int i = 1; i<=binary.getSize(); i++){
            ImageProcessor processor = binary.getProcessor(i);
            for(int j = 0; j<binary.getWidth(); j++){
                for(int k = 0; k<binary.getHeight(); k++){
                    if(processor.getPixelValue(j,k)>0){
                        sumx += j;
                        sumy += k;
                        sumz += i;
                        sum++;
                    }
                }
            }
        }
        return new double[]{sumx/sum,sumy/sum,sumz/sum};
    }

}
