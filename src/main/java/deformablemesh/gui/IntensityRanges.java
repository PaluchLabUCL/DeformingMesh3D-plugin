package deformablemesh.gui;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Box3D;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.VolumeDataObject;
import ij.ImageStack;
import ij.process.ImageProcessor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class IntensityRanges {
    private final Histogram histogram;
    HistogramPanel panel;
    double lowIntensity = 0;
    double highIntensity = 0;

    public interface Contrastable{
        public void setMinMax(double min, double max);
    }
    List<Contrastable> listeners = new ArrayList<>();

    public IntensityRanges(double[] intensityValues){
        histogram = new Histogram(intensityValues);
        panel = new HistogramPanel(histogram);

    }

    public IntensityRanges(ImageStack stack){
        this(intensityRanges(stack));
    }

    public void addContrastableListener(Contrastable c){
        listeners.add(c);
    }

    static public double[] intensityRanges(ImageStack stack){

        int frame = stack.getHeight()*stack.getWidth();
        int n = stack.getSize()*frame;

        double[] values = new double[n];

        for(int slice = 1; slice<=stack.getSize(); slice++){
            ImageProcessor proc = stack.getProcessor(slice);
            for(int i = 0; i<frame; i++){
                values[i + frame*(slice-1)] = proc.get(i);
            }
        }
        return values;
    }

    public JPanel getPanel(){
        return panel;
    }

    public void finished(){


    }


    class Histogram{
        double minValue, maxValue;
        final int[] bins;
        final double[] values;
        final double[] backingValues;
        double binMax = 0;

        public Histogram(double[] inputValues){
            this(100, inputValues);
        }

        public Histogram(int nBins, double[] inputValues){
            bins = new int[nBins];
            values = new double[nBins];
            backingValues = inputValues;

            minValue = Double.MAX_VALUE;
            maxValue = -Double.MAX_VALUE;
            for(double v: backingValues){
                minValue = v < minValue ? v : minValue;
                maxValue = v > maxValue ? v : maxValue;
            }

            for(int i = 0; i<bins.length; i++){
                //bins[i] = 0;
                values[i] = minValue + (i + 0.5)*(maxValue - minValue)/bins.length;
            }

            double range = nBins/(maxValue - minValue);

            for(int i = 0; i<inputValues.length; i++){
                double v = inputValues[i];
                int dex = (int)((v - minValue)*range);
                dex = dex>=bins.length?bins.length-1:dex;
                int bin = ++bins[dex];
                if(bin > binMax){
                    binMax = bin;
                }

            }

            System.out.println(binMax + " of bins, from " + minValue + " to " + maxValue);
            lowIntensity = 0.1*(maxValue - minValue) + minValue;
            highIntensity = 0.9*(maxValue - minValue) + minValue;

        }

        public double getValue(int index){
            return values[index];
        }

        public double getBin(int index){
            return bins[index];
        }


    }

    class HistogramPanel extends JPanel {
        Histogram gram;
        int drawWidth = 480;
        int drawHeight = 96;
        double fractionOfHeight = 0.8;
        int padding = 10;
        Marker minCutoff = new Marker(padding, drawHeight + 2*padding);
        Marker maxCutoff = new Marker(padding, drawHeight + 2*padding);
        Marker dragging;
        public HistogramPanel(Histogram g){
            gram = g;
            minCutoff.setX( intensityToPx( lowIntensity ) );
            maxCutoff.setX( intensityToPx( highIntensity ) );
            MouseAdapter histogramIntensityAdapter = new MouseAdapter(){

                @Override
                public void mousePressed(MouseEvent evt){
                    if(minCutoff.contains(evt.getPoint())){
                        dragging = minCutoff;
                    } else if(maxCutoff.contains(evt.getPoint())){
                        dragging = maxCutoff;
                    }
                    System.out.println(dragging);

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(dragging != null){
                        double low = getIntensityAt(minCutoff.getX());
                        double high = getIntensityAt(maxCutoff.getX());

                        for(Contrastable c: listeners){
                            c.setMinMax(low, high);
                        }

                        dragging = null;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {

                    if(dragging!=null){
                        double x = e.getX();
                        if(x<=padding){
                            x = padding;
                        } else if(x> drawWidth+padding){
                            x = drawWidth + padding;
                        }

                        dragging.setX(x);
                        panel.repaint();
                    }
                }
            };
            addMouseListener( histogramIntensityAdapter );
            addMouseMotionListener( histogramIntensityAdapter );

        }
        @Override
        public Dimension getPreferredSize(){
            return getMinimumSize();
        }
        @Override
        public Dimension getMaximumSize(){
            return getMinimumSize();
        }

        @Override
        public Dimension getMinimumSize(){
            return new Dimension(drawWidth + 2*padding, drawHeight + 2*padding);
        }

        @Override
        public void paintComponent(Graphics g){
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, drawWidth + 2*padding, drawHeight + 2*padding);
            double scaledHeight = drawHeight*fractionOfHeight;
            int n = gram.bins.length;

            double xmin = 0;
            double xmax = n;

            double ymin = 0;
            double ymax = gram.binMax;

            double dx = drawWidth*1.0/(xmax - xmin);
            double dy = scaledHeight/(ymax - ymin);
            int w = (int)dx;
            w = w==0 ? 1:w;


            g.setColor(Color.MAGENTA);
            for(int i = 0; i<n; i++){
                double fx = (i - xmin)/(xmax - xmin);
                int ox = ((int)(fx*drawWidth));

                int b = (int)gram.getBin(i);
                if(b==0) continue;

                double fy = (b - ymin)/(ymax - ymin);
                int h = (int)(scaledHeight*fy);
                h = h == 0 ? 1 : h;
                int oy = (int)drawHeight - h;
                g.fillRect(ox + padding, oy - padding, w, h);
            }


            minCutoff.draw((Graphics2D)g);
            maxCutoff.draw((Graphics2D)g);


        }

        public double getIntensityAt(double x){
            double xt = (x - padding)*(histogram.maxValue - histogram.minValue)/drawWidth;
            return xt;
        }

        public int intensityToPx(double i){
            double fx = (i - histogram.minValue)/(histogram.maxValue - histogram.minValue);
            return (int)(fx*drawWidth + padding);
        }

        public int getPxFromIndex(int index){

            return index*drawWidth/histogram.bins.length + padding;

        }

        public int getBinIndexFromPx(double x){
            int n = histogram.bins.length;
            int index = (int)( n*(x - padding)/drawWidth );
            index = index == n ? n-1 : index;
            return index;
        }

    }
    class Marker{

        double x;

        Ellipse2D top, bottom;
        double diameter;
        double height;
        public Marker(double diameter, double height){
            top = new Ellipse2D.Double(0, 0, diameter, diameter);
            bottom = new Ellipse2D.Double(0, height-diameter, diameter, diameter);
            this.diameter = diameter;
            this.height = height;
        }

        public boolean contains(Point2D p){
            return top.contains(p) || bottom.contains(p);
        }
        public double getX() {
            return x;
        }

        public void setX(double v) {
            System.out.println("moving to: " + v);
            x = v;
            top.setFrame(x-diameter/2, 0, diameter, diameter);
            bottom.setFrame(x - diameter/2, height - diameter, diameter, diameter);
        }

        public void draw(Graphics2D g){
            g.setColor(Color.WHITE);
            g.drawLine((int)x, 0, (int)x, (int)height);
            g.setColor(Color.RED);
            g.fill(top);
            g.fill(bottom);

        }

    }

}
