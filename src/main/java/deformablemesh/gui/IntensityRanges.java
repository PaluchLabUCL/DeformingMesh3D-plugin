package deformablemesh.gui;

import ij.ImageStack;
import ij.process.ImageProcessor;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

public class IntensityRanges {

    class Histogram{
        double min,max;
        final int[] bins;
        final double[] values;
        final double[] backingValues;
        double binMax = 0;

        public Histogram(double[] inputValues){
            this(255, inputValues);
        }

        public Histogram(int nBins, double[] inputValues){
            bins = new int[nBins];
            values = new double[nBins];
            backingValues = inputValues;

            min = Double.MAX_VALUE;
            max = -Double.MAX_VALUE;
            for(double v: backingValues){
                if(v<min) min = v;
                if(v>max) max = v;
            }

            for(int i = 0; i<bins.length; i++){
                bins[i] = 0;
                values[i] = min + (i + 0.5)*(max-min)/bins.length;
            }

            double range = nBins/(max - min);

            for(int i = 0; i<inputValues.length; i++){
                double v = inputValues[i];
                int dex = (int)((v - min)*range);
                dex = dex>254?254:dex;
                bins[dex]++;
                if(bins[dex]>binMax){
                    binMax=bins[dex];
                }
            }
        }

        public double getValue(int index){
            return values[index];
        }



    }

    class HistogramPanel extends JPanel {
        Histogram gram;
        public HistogramPanel(Histogram g){
            gram = g;
        }

        @Override
        public void paintComponent(Graphics g){

            Dimension dim = getSize();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, dim.width, dim.height);

            int n = gram.bins.length;

            double xmin = 0;
            double xmax = n;

            double ymin = 0;
            double ymax = gram.binMax;

            double dx = dim.width/(xmax - xmin);
            double dy = dim.height/(ymax - ymin);

            g.setColor(Color.MAGENTA);
            for(int i = 0; i<n; i++){
                int ox = (int)(i*dx - xmin*dx);
                int w = (int)(dx);
                if(w==0) w=1;
                int h = (int)(gram.values[i]*dy - ymin*dy);
                int oy = dim.height - h - 1;
                g.fillRect(ox, oy, w, h);
            }


        }

    }

    public IntensityRanges(double[] intensityValues){

    }

    static public void showHistogramRangeInput(List<ImageProcessor> stack){

        int n = stack.size()*stack.get(0).getWidth()*stack.get(0).getHeight();
        double[] values = new double[n];

        int dex = 0;
        for(ImageProcessor proc: stack){
            int nSlc = proc.getWidth()*proc.getHeight();
            int w = proc.getWidth();
            for(int j = 0; j<nSlc; j++){

                values[dex++] = proc.getPixelValue(j%w, j/w);

            }

        }


    }


    public void finished(){


    }


}
