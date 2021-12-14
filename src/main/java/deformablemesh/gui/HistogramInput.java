package deformablemesh.gui;

import ij.process.ImageProcessor;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Created by msmith on 2/12/16.
 */
class HistogramInput {
    final BufferedImage img = new BufferedImage(138, 50, BufferedImage.TYPE_INT_ARGB);
    final JPanel panel;
    final int border = 5;
    Histogram gram;
    final RingController rc;
    Color highlight = new Color(0, 0, 0, 175);
    public HistogramInput(RingController rc){
        this.rc = rc;
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0,0, img.getWidth(), img.getHeight());
        g2d.dispose();
        gram = new Histogram();
        panel = new JPanel(){
            @Override
            public void paintComponent(Graphics g){
                int h = getHeight();
                int w = getWidth();
                int padX = (img.getWidth() - w)/2;
                int padY = (img.getHeight() - h)/2;
                int baseline = 3*img.getHeight()/4 + padY;
                g.drawImage(img, padX, padY, this);

                String sel = String.format("%3.3f", rc.getThresh());
                double f = gram.getFraction(rc.getThresh());
                int start = (int)(img.getWidth()*f);

                Rectangle2D b = g.getFontMetrics().getStringBounds(sel, g);
                g.setColor(highlight);
                g.fillRect(start - 1, (int)(baseline - b.getHeight() - 1), (int)b.getWidth() + 2, (int)b.getHeight() + 2);
                g.setColor(Color.WHITE);
                g.drawString(sel, start, baseline);

            }
        };
        panel.setSize(new Dimension(img.getWidth(), img.getHeight()));
        panel.setMinimumSize(new Dimension(img.getWidth(), img.getHeight()));
        panel.setMaximumSize(new Dimension(img.getWidth(), img.getHeight()));
        panel.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));

        panel.addMouseListener(new MouseListener(){

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                int bins = gram.getNBins();
                double scale = bins / (img.getWidth() - 2 * border);
                int i = (int)(( e.getX() - border )*scale);

                if(i>=0&&i<bins) {
                    rc.setThreshold(gram.getValue(i));
                    panel.repaint();
                } else if(i<0){
                    rc.setThreshold(gram.minValue);
                    panel.repaint();
                } else if(i >= bins){
                    rc.setThreshold(gram.maxValue);
                    panel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

    }
    public void refresh(ImageProcessor p){

        gram=new Histogram(p, (img.getWidth() - 2*border));
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0,0,img.getWidth(), img.getHeight());
        g2d.setColor(Color.GREEN);
        int height = img.getHeight();
        int n = img.getWidth() - 2*border;
        for(int i = 0; i<n; i++){
            g2d.drawLine(i+border, height, i+border, height-gram.bins[i]);
        }

        String max = String.format("%3.3f", gram.maxValue);
        Rectangle2D mxb = g2d.getFontMetrics().getStringBounds(max, g2d);
        String min = String.format("%3.3f", gram.minValue);
        Rectangle2D mnb = g2d.getFontMetrics().getStringBounds(min, g2d);

        int baseline = (int)(mxb.getHeight() + 1);
        int mxx = (int)(img.getWidth() - mxb.getWidth());
        g2d.setColor(highlight);
        g2d.fillRect(0, 0, (int)mnb.getWidth() + 2, baseline + 1);
        g2d.fillRect(0, mxx - 1, (int)mxb.getWidth() + 2, baseline + 1);

        g2d.setPaint(Color.WHITE);
        g2d.drawString(min, 1, baseline);

        g2d.drawString(max, mxx, baseline);
        g2d.dispose();
        panel.repaint();
    }
}

class Histogram{
    double minValue, maxValue;
    final int[] bins;
    final double[] values;
    public Histogram(){
        bins = new int[0];
        values = new double[0];
    }
    public Histogram(ImageProcessor proc, int nBins){
        bins = new int[nBins];
        values = new double[nBins];

        minValue = Double.MAX_VALUE;
        maxValue = -Double.MAX_VALUE;
        final int w = proc.getWidth();
        final int h = proc.getHeight();
        for(int i = 0; i<w*h; i++){
            float v = proc.getf(i);
            if(v<minValue) minValue = v;
            if(v> maxValue) maxValue = v;

        }

        for(int i = 0; i<bins.length; i++){
            bins[i] = 0;
            values[i] = minValue + (i + 0.5)*(maxValue -minValue)/bins.length;
        }

        double range = nBins/(maxValue - minValue);
        int maxBin = 0;
        for(int i = 0; i<w*h; i++){
            float v = proc.getf(i);
            int dex = (int)((v - minValue)*range);
            dex = dex>nBins-1?nBins-1:dex;
            bins[dex]++;
            if(bins[dex]>maxBin){
                maxBin=bins[dex];
            }
        }

    }
    public double getValue(int index){
        return values[index];
    }
    public static void main(String[] args){

    }

    public int getNBins() {
        return bins.length;
    }

    public double getFraction(double thresh) {
        return (thresh - minValue)/(maxValue - minValue);
    }
}

