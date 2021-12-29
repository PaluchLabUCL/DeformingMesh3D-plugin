package deformablemesh.gui;

import ij.ImageStack;
import ij.process.ImageProcessor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class IntensityRanges {
    private final Histogram histogram;
    HistogramPanel panel;
    double lowIntensity = 0;
    double highIntensity = 0;
    List<Contrastable> listeners = new ArrayList<>();

    public interface Contrastable{
        public void setMinMax(double min, double max);
    }

    public IntensityRanges(double[][][] intensityValues){
        histogram = new Histogram(intensityValues);
        panel = new HistogramPanel(histogram);
    }

    public void setClipValues(double min, double max ){
        System.out.println("min: " + min + ", max: "  + max);
        lowIntensity = min*(histogram.maxValue-histogram.minValue) + histogram.minValue;
        highIntensity = max*(histogram.maxValue-histogram.minValue) + histogram.minValue;
        panel.setMarkerPositions();
    }
    double getClip(double intensity){
        return (intensity-histogram.minValue)/(histogram.maxValue - histogram.minValue);
    }
    public void setIntensityExtremes(double low, double high){
        lowIntensity = low;
        highIntensity = high;
        double clow = getClip(low);
        double chigh = getClip(high);

        for(Contrastable c: listeners){
            c.setMinMax(clow, chigh);
        }
    }

    public double[] getClipValues(){
        double f = 1/(histogram.maxValue - histogram.minValue);
        return new double[]{ lowIntensity*f, highIntensity*f };
    }
    /**
     * Probably wont be used.
     *
     * @param stack
     */
    public IntensityRanges(ImageStack stack){
        this(intensityRanges(stack));
    }

    public void addContrastableListener(Contrastable c){
        listeners.add(c);
    }

    /**
     * Flatten the provided image stack into a 1d range of values for creating a histogram.
     *
     * @param stack an image stack that values will be extracted from.
     *
     * @return the same values but in 1d.
     */
    static public double[][][] intensityRanges(ImageStack stack){

        int frame = stack.getHeight()*stack.getWidth();
        int n = stack.getSize()*frame;

        double[] values = new double[n];

        for(int slice = 1; slice<=stack.getSize(); slice++){
            ImageProcessor proc = stack.getProcessor(slice);
            for(int i = 0; i<frame; i++){
                values[i + frame*(slice-1)] = proc.get(i);
            }
        }
        return new double[][][]{{values}};
    }

    public JPanel getPanel(){
        return panel;
    }

    public void finished(){


    }


    class Histogram{
        double minValue, maxValue;
        final int[] bins; //elements per bin.
        final double[] values; //value per bin
        final double[][][] backingValues; //full values represented by histogram.
        double binMax = 0;

        public Histogram(double[][][] inputValues){
            this(120, inputValues);
        }

        public Histogram(int nBins, double[][][] inputValues){
            bins = new int[nBins];
            values = new double[nBins];
            backingValues = inputValues;

            minValue = Double.MAX_VALUE;
            maxValue = -Double.MAX_VALUE;
            for(double[][] slice: backingValues) {
                for(double[] row: slice) {
                    for (double v : row) {
                        minValue = v < minValue ? v : minValue;
                        maxValue = v > maxValue ? v : maxValue;
                    }
                }
            }

            for(int i = 0; i<bins.length; i++){
                //bins[i] = 0;
                values[i] = minValue + (i + 0.5)*(maxValue - minValue)/bins.length;
            }

            double range = nBins/(maxValue - minValue);

            for(double[][] slice: backingValues) {
                for(double[] row: slice) {
                    for (double v : row) {
                        int dex = (int) ((v - minValue) * range);
                        dex = dex >= bins.length ? bins.length - 1 : dex;
                        int bin = ++bins[dex];
                        if (bin > binMax) {
                            binMax = bin;
                        }
                    }
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
        Color histogramBackground = new Color(50, 0, 0);
        int drawWidth = 480;
        int drawHeight = 96;
        double fractionOfHeight = 0.8;
        int padding = 25;
        Marker minCutoff = new Marker(padding/2, drawHeight + 2*padding);
        Marker maxCutoff = new Marker(padding/2, drawHeight + 2*padding);
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
                        minCutoff.setDragging(true);
                        maxCutoff.setDragging(false);
                        repaint();
                    } else if(maxCutoff.contains(evt.getPoint())){
                        dragging = maxCutoff;
                        minCutoff.setDragging(false);
                        maxCutoff.setDragging(true);
                        repaint();
                    }

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(dragging != null){
                        double low = getIntensityAt(minCutoff.getX());
                        double high = getIntensityAt(maxCutoff.getX());
                        setIntensityExtremes(low, high);


                        dragging = null;
                    }
                    minCutoff.setDragging(false);
                    maxCutoff.setDragging(false);
                    repaint();

                }

                @Override
                public void mouseMoved(MouseEvent evt){
                    if(maxCutoff.findWithin(evt.getPoint()) || minCutoff.findWithin(evt.getPoint())){
                        repaint();
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
            g.setColor(histogramBackground);
            g.fillRect(0, 0, drawWidth + 2*padding, drawHeight + 2*padding);
            g.setColor(Color.DARK_GRAY);
            int rx = 4;
            int sep = 3;
            g.drawRoundRect(padding/3, padding/3, drawWidth+padding + padding/3 , drawHeight + padding + padding/3 , rx, rx);
            g.drawRoundRect(padding/3 - sep, padding/3 - sep, drawWidth+padding + padding/3 + 2*sep, drawHeight + padding + padding/3  + 2*sep, rx, rx);
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


            g.setColor(Color.CYAN);
            for(int i = 0; i<n; i++){
                double fx = (i - xmin)/(xmax - xmin);
                int ox = ((int)(fx*drawWidth));

                int b = (int)gram.getBin(i);
                if(b==0) continue;

                double fy = (b - ymin)/(ymax - ymin);
                int h = (int)(scaledHeight*fy);
                h = h == 0 ? 1 : h;
                int oy = (int)drawHeight - h + padding;
                g.fillRect(ox + padding, oy, w, h);
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
        public void setMarkerPositions(){
            minCutoff.setX( intensityToPx( lowIntensity ) );
            maxCutoff.setX( intensityToPx( highIntensity ) );
            repaint();
        }
    }
    class Marker{

        double x;

        Ellipse2D top, bottom;
        double diameter;
        double height;
        boolean hovering = false;
        boolean dragging = false;

        public Marker(double diameter, double height){
            top = new Ellipse2D.Double(0, diameter/2, diameter, diameter);
            bottom = new Ellipse2D.Double(0, height-diameter/2, diameter, diameter);
            this.diameter = diameter;
            this.height = height;
        }

        public boolean contains(Point2D p){
            return top.contains(p) || bottom.contains(p);
        }
        public double getX() {
            return x;
        }
        public void setHover(boolean v){
            hovering = v;
        }
        public void setDragging(boolean v){
            dragging = v;
        }
        public void setX(double v) {
            x = v;
            top.setFrame(x-diameter/2, diameter/2, diameter, diameter);
            bottom.setFrame(x - diameter/2, height - 3*diameter/2, diameter, diameter);
        }

        public void highlightEllipse(Ellipse2D e, Graphics2D g){
            float x = (float)e.getCenterX();
            float y = (float)e.getCenterY();
            float r = (float)e.getWidth()/2+5;
            g.setPaint(new RadialGradientPaint(x, y, r, new float[]{0.7f, 1.0f} ,  new Color[]{
                    Color.WHITE,
                    new Color(255, 255, 255, 0)
            }));
            Ellipse2D e2 = new Ellipse2D.Double(x - r, y - r, 2*r, 2*r);
            g.fill(e2);
            decorateEllipse(e, g);

        }
        public void drawHover(Graphics2D g){
            g.setStroke(new BasicStroke(2.0f));
            drawMarkerLine(g);
            highlightEllipse(top, g);
            highlightEllipse(bottom, g);
        }

        public void drawDragging(Graphics2D g){
            g.setStroke(new BasicStroke(2.0f));

            drawMarkerLine(g);

            g.setColor(Color.RED);
            g.fill(top);
            g.fill(bottom);
        }
        Color shaded = new Color(0, 100, 100);
        private void decorateEllipse(Ellipse2D e, Graphics2D g){
            float x = (float)e.getCenterX();
            float y = (float)e.getCenterY();
            float r = (float)e.getWidth()*2;
            g.setPaint(new RadialGradientPaint(x, y, r, new float[]{0.0f, 1.0f} ,  new Color[]{
                    Color.RED,
                    shaded
            }));
            g.fill(e);
            g.setPaint(Color.RED);
            g.draw(e);
        }

        public void drawMarkerLine(Graphics2D g){
            g.setColor(Color.WHITE);

            g.drawLine((int)top.getCenterX(), (int)top.getCenterY(), (int)bottom.getCenterX(), (int)bottom.getCenterY());
        }

        public void drawPlain(Graphics2D g){
            g.setStroke(new BasicStroke(2.0f));
            drawMarkerLine(g);
            decorateEllipse(top, g);
            decorateEllipse(bottom, g);

        }

        public void draw(Graphics2D g){
            if(dragging){
                drawDragging(g);
            } else if(hovering){
                drawHover(g);
            } else{
                drawPlain(g);
            }

        }

        /**
         * Checks if the point is within the control area ( top or bottom ) sets the marker to hovering if it is.
         * @param point the point to be testted.
         * @return true if the value changes.
         */
        public boolean findWithin(Point point) {
            if(top.contains(point) || bottom.contains(point)){
                if(hovering){
                    return false;
                } else{
                    return hovering = true;
                }
            } else{
                if(hovering){
                    hovering = false;
                    return true;
                } else{
                    return false;
                }
            }

        }
    }

    public static void main(String[] args){
        JFrame frame = new JFrame("testing instensity range input");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int N = 10000;
        double[] range = new double[N];
        for(int i = 0; i<N/2; i++){

            double s, u, v;
            do{
                u = 2*Math.random() - 1;
                v = 2*Math.random() - 1;
                s = u*u + v*v;

            } while(s==0||s>=1);
            double f = Math.sqrt( -2 * Math.log(s)/s);
            range[2*i] = u*f + 2;
            range[2*i+1] = v*f + 2;
        }
        IntensityRanges ranger = new IntensityRanges(new double[][][]{{range}});
        frame.add(ranger.panel);
        frame.pack();
        frame.setVisible(true);
    }

}
