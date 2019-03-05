package deformablemesh.util;

import deformablemesh.util.ColorInterpolator;

import java.awt.Color;

public class HotAndCold implements ColorInterpolator {
    float[] hot, cold;
    double min =0;
    double max =100;
    public HotAndCold(Color hot, Color cold){
        this.hot = hot.getRGBComponents(new float[4]);
        this.cold = cold.getRGBComponents(new float[4]);
    }

    public void setMinMax(double min, double max){
        this.min = min;
        this.max = max;
    }

    public float[] getColor(double v){

        float f = (float)((v - min)/(max - min));
        if(f<=0){
            f = 0;
        } if(f>=1){
            f = 1;
        }
        return new float[]{
                f*hot[0] + (1-f)*cold[0],
                f*hot[1] + (1-f)*cold[1],
                f*hot[2] + (1-f)*cold[2]
        };

    }


}
