package deformablemesh.gui;

import ij.process.ImageProcessor;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * Created by msmith on 2/12/16.
 */
class HistogramInput {
    final BufferedImage img = new BufferedImage(295, 100, BufferedImage.TYPE_INT_ARGB);
    final JPanel panel;
    final int border = 20;
    Histogram gram;
    final RingController rc;
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
                g.drawImage(img, 0, 0, this);
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

                int i = e.getX() - border;
                if(i>=0&&i<255) {
                    rc.setThreshold(gram.getValue(i));
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
    public void refresh(Histogram h){
        gram=h;
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0,0,img.getWidth(), img.getHeight());
        g2d.setColor(Color.GREEN);
        int height = img.getHeight();

        for(int i = 0; i<255; i++){
            g2d.drawLine(i+border, height, i+border, height-h.bins[i]);
        }
        g2d.setPaint(Color.WHITE);
        int center = img.getHeight()/2;
        g2d.drawString(String.format("%3.3f", h.minValue), 0, center);
        g2d.drawString(String.format("%3.3f", h.maxValue), 200, center);
        g2d.dispose();
        panel.repaint();
    }
}

class Histogram{
    double minValue, maxValue;
    final int[] bins = new int[255];
    final double[] values = new double[255];
    public Histogram(){}
    public Histogram(ImageProcessor proc){

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

        double range = 255/(maxValue - minValue);
        int maxBin = 0;
        for(int i = 0; i<w*h; i++){
            float v = proc.getf(i);
            int dex = (int)((v - minValue)*range);
            dex = dex>254?254:dex;
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
}

