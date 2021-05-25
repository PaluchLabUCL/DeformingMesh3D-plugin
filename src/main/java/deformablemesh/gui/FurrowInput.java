package deformablemesh.gui;

import deformablemesh.geometry.Furrow3D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by msmith on 2/12/16.
 */
public class FurrowInput extends JPanel {
    Dimension fixedSize;
    double markX;
    double markY;
    Ellipse2D marker;

    final static double border = 20;
    final static double sphereRadius = 60;
    static Ellipse2D sphere = new Ellipse2D.Double(border, border, 2*sphereRadius, 2*sphereRadius);

    final static double bandWidth = 60;
    final static double bandHeight = 2*sphereRadius;
    static Rectangle2D band = new Rectangle2D.Double(2*border + 2*sphereRadius, border, bandWidth, bandHeight);
    final static Color clearBlack = new Color(0,0,0,0);
    final static Color clearWhite = new Color(255, 255, 255, 0);
    final static Color midBlack = new Color(0,0,0,50);

    static BufferedImage background = createBackgroundImage();
    final static double markerRadius = 10;
    static int grating = 10;
    static int delta = (int)(bandHeight/grating);
    static int gratingThickness = (int)(bandHeight*0.5/delta);
    static int gratingBorder = 2;
    double velocity = 0.0;



    List<PlaneChangeListener> listeners = new ArrayList<>();

    int scrollOffset = 0;

    double[] normal;

    public FurrowInput(){

        marker = new Ellipse2D.Double(markX+sphere.getCenterX() - markerRadius, markY+sphere.getCenterY() - markerRadius, 2*markerRadius, 2*markerRadius);
        fixedSize = new Dimension( background.getWidth(), background.getHeight());
        MarkerListener m = new MarkerListener();
        addMouseListener(m);
        addMouseMotionListener(m);

        BandListener b = new BandListener();
        addMouseListener(b);
        addMouseMotionListener(b);
    }
    final static Color lightBlue = new Color(80, 80, 255);
    final static Color darkBlue = new Color(0, 0, 200);
    @Override
    protected void paintComponent(Graphics g){
        Graphics2D g2d = (Graphics2D)g;
        g2d.drawImage(background, 0, 0, this);


        g2d.setPaint(getMarkerPaint());

        g2d.fill(marker);


        g2d.setColor(Color.BLACK);
        g2d.draw(marker);
        int x = (int)band.getX()+gratingBorder;
        int w = (int)band.getWidth() - 2*gratingBorder;
        int bh = (int)bandHeight;
        int bottom = (int)(band.getY() + band.getHeight());
        int top = (int)band.getY();
        GradientPaint p = new GradientPaint(0f, top, Color.BLACK, 0f, (bottom+top)/2, midBlack, true);
        g2d.setPaint(p);

        for(int i = 0; i<grating; i++){
            int y = (i*delta + scrollOffset)%bh;
            if(y<0){
                y += bandHeight;
            }
            y = y + top;
            int t = y + gratingThickness>bottom?
                    y  - bottom :
                    gratingThickness;
            if(t>0){
                g2d.fillRect(x, y, w, t);
            }

        }



    }
    final static float[] fractions = {0.0f, 0.8f, 1.0f};
    final static Color[] colors = {lightBlue, Color.BLUE, darkBlue};
    final static float c1X = (int)(3*border)+5;
    final static float c1Y = (int)(2*border)+5;
    final static float w2 = (int)(0.5*sphereRadius);
    final static Paint pd = new RadialGradientPaint(c1X, c1Y, w2, fractions, colors );
    Paint getMarkerPaint(){
        return pd;
    }

    static BufferedImage createBackgroundImage(){
        int height = (int)(2*border + 2*sphereRadius);
        int width = (int)(3*border + 2*sphereRadius + bandWidth);
        BufferedImage bg = new BufferedImage( width, height, BufferedImage.TYPE_4BYTE_ABGR );

        Graphics2D g2d = bg.createGraphics();
        //g2d.setColor(Color.WHITE);
        //g2d.fillRect(0,0,width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.RED);
        g2d.fill(sphere);

        int c1X = (int)(2*border);
        int c1Y = (int)(2*border);
        int w2 = (int)(2.5*sphereRadius);
        float[] fractions = {0.0f, 1.0f};
        Color[] colors = {clearBlack, Color.BLACK};
        Paint p = new RadialGradientPaint(c1X, c1Y, w2, fractions, colors );
        g2d.setPaint(p);
        g2d.fill(sphere);

        g2d.setColor(Color.BLACK);
        g2d.draw(sphere);
        int middleY = (int)sphere.getCenterY();
        int middleX = (int)sphere.getCenterX();
        int leftX = (int)sphere.getX();
        int topY = (int)sphere.getY();
        g2d.drawLine(middleX, topY, middleX, topY+(int)sphere.getHeight());
        g2d.drawLine(leftX, middleY, leftX + (int)sphere.getWidth(), middleY);
        int steps = 6;
        double dtheta = Math.PI/2/steps;
        int l = 3;
        for(int i = 0; i<steps; i++){
            int dx = (int)(0.5*sphere.getWidth()*Math.sin((i+1)*dtheta));
            g2d.setColor(midBlack);
            g2d.drawOval(middleX-dx, middleY-dx, 2*dx, 2*dx );
            g2d.setColor(Color.BLACK);
            g2d.drawLine(middleX+dx, middleY-l, middleX+dx, middleY+l );
            g2d.drawLine(middleX-dx, middleY-l, middleX-dx, middleY+l );
            g2d.drawLine(middleX-l, middleY+dx, middleX+l, middleY+dx);
            g2d.drawLine(middleX-l, middleY-dx, middleX+l, middleY-dx);
        }

        c1X = (int)(3*border);
        c1Y = (int)(2*border);
        w2 = (int)(0.25*sphereRadius);
        fractions = new float[]{0.0f, 1.0f};
        colors = new Color[]{Color.WHITE, clearWhite};
        p = new RadialGradientPaint(c1X, c1Y, w2, fractions, colors );
        g2d.setPaint(p);
        g2d.fill(sphere);

        //draw band.
        g2d.setColor(Color.RED);
        g2d.fill(band);
        g2d.setColor(Color.black);
        g2d.draw(band);

        float y1 = (float)(band.getY());
        float y2 = (float)(band.getY() + band.getHeight()*0.5);
        p = new GradientPaint(0f, y1, Color.BLACK, 0f, y2, clearBlack, true);
        g2d.setPaint(p);
        g2d.fill(band);

        //g2d.fill(band);


        g2d.dispose();

        return bg;
    }

    @Override
    public Dimension getPreferredSize(){
        return fixedSize;
    }
    @Override
    public Dimension getMaximumSize(){
        return fixedSize;
    }
    @Override
    public Dimension getMinimumSize(){
        return fixedSize;
    }

    class MarkerListener extends MouseAdapter {
        boolean dragging;
        Point last;
        @Override
        public void mousePressed(MouseEvent e) {
            if(marker.contains(e.getPoint())){
                dragging=true;
                last = e.getPoint();
            }
        }
        @Override
        public void mouseReleased(MouseEvent e){
            dragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(dragging){
                last = e.getPoint();
                //double nx = marker.getCenterX() + dx;
                //double ny = marker.getCenterY() + dy;
                double nx = e.getX();
                double ny = e.getY();

                if(sphere.contains(nx, ny)){
                    markTo(nx, ny);
                    updateNormal();
                    notifyDataListeners();
                } else{
                    double dx = nx - sphere.getCenterX();
                    double dy = ny - sphere.getCenterY();
                    double m = Math.sqrt(dx*dx + dy*dy);
                    double scale = sphere.getWidth()*0.5/m;
                    nx = sphere.getCenterX() + dx*scale;
                    ny = sphere.getCenterY() + dy*scale;
                    markTo(nx, ny);
                    updateNormal();
                    notifyDataListeners();
                }
            }
        }
    }

    class BandListener extends MouseAdapter {
        boolean dragging;
        Point last;
        @Override
        public void mousePressed(MouseEvent e) {
            if(band.contains(e.getPoint())){
                dragging=true;
                last = e.getPoint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(dragging){
                double dy = e.getY() - last.getY();
                last = e.getPoint();
                scrollYUnits(dy);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e){
            dragging = false;
        }

    }

    public void scrollYUnits(double y){
        scrollOffset += (int)y;
        velocity = 0.0025*y;
        repaint();
        notifyDataListeners();
    }

    /**
     * Moves the marker to the specified position from image coordinates.
     *
     * @param nx
     * @param ny
     */
    public void markTo(double nx, double ny){
        markX = (nx - sphere.getCenterX())/sphereRadius;
        markY = (ny - sphere.getCenterY())/sphereRadius;
        if(markX*markX + markY*markY>1){
            double f = 1/Math.sqrt(markX*markX + markY*markY);
            markX = f*markX;
            markY = f*markY;
        }
        marker.setFrame(nx - markerRadius, ny - markerRadius, 2*markerRadius, 2*markerRadius);
        repaint();
    }

    public void notifyDataListeners(){
        if(listeners.size()>0&&normal!=null){

            if(velocity!=0) {
                double dx = normal[0] * velocity;
                double dy = normal[1] * velocity;
                double dz = normal[2] * velocity;
                velocity = 0;
                for (PlaneChangeListener l : listeners) {
                    l.updatePosition(dx, dy, dz);
                }
            }
            for(PlaneChangeListener l: listeners){
                l.setNormal(normal);
            }
        }
    }

    public void updateNormal(){
        normal = new double[]{
            getXComponent(),
            markX,
            markY,
        };



    }

    public double getXComponent(){
        double m = 1 - markX*markX - markY*markY;
        if(m<0){
            //by this point we have tried to normalize so lets approximate.
            markX = markX + 0.5*markX*m;
            markY = markY + 0.5*markY*m;

        }
        return m<=0?0:Math.sqrt(m);
    }

    public void setMarkFromNormal(double[] n){
        normal = n;
        double x = n[1]*sphereRadius + sphere.getCenterX();
        double y = n[2]*sphereRadius + sphere.getCenterY();
        markTo(x, y);
    }

    public void setFurrow(Furrow3D furrow){
        setMarkFromNormal(furrow.normal);
    }

    public void addPlaneChangeListener(PlaneChangeListener l){
        listeners.add(l);
    }

    public void removePlaneChangeListener(PlaneChangeListener l){
        listeners.remove(l);
    }

    public static void main(String[] args){
        JFrame frame = new JFrame("testing furrow control");
        FurrowInput input = new FurrowInput();
        frame.add(input);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public interface PlaneChangeListener{
        void setNormal(double[] n);
        void updatePosition(double dx, double dy, double dz);
    }
}
