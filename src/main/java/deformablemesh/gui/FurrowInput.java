package deformablemesh.gui;

import deformablemesh.geometry.Furrow3D;
import deformablemesh.util.Vector3DOps;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An input for scrolling and directing a plane in 3D.
 *
 * Created by msmith on 2/12/16.
 */
public class FurrowInput extends JPanel {
    Dimension fixedSize;
    double markX;
    double markY;
    Ellipse2D marker;

    final static double border = 15;
    final static double sphereRadius = 45;
    static Ellipse2D sphere = new Ellipse2D.Double(border, border, 2*sphereRadius, 2*sphereRadius);

    final static double bandWidth = 30;
    final static double bandHeight = 2*sphereRadius;
    static Rectangle2D band = new Rectangle2D.Double(2*border + 2*sphereRadius, border, bandWidth, bandHeight);
    final static Color clearBlack = new Color(0,0,0,0);
    final static Color clearWhite = new Color(255, 255, 255, 0);
    final static Color midBlack = new Color(0,0,0,50);
    final static Color active = new Color(150, 120, 100);

    static BufferedImage background = createBackgroundImage();
    final static double markerRadius = 10;
    static int grating = 5;
    static int delta = (int)(bandHeight/grating);
    static int gratingThickness = (int)(bandHeight*0.5/delta);
    static int gratingBorder = 2;
    double velocity = 0.0;
    List<PlaneChangeListener> listeners = new ArrayList<>();
    int scrollOffset = 0;
    double[] normal;
    final static Color lightBlue = new Color(80, 80, 255);
    final static Color darkBlue = new Color(0, 0, 200);
    boolean hovering = false;
    boolean sphereHovering = false;

    boolean wheelHovering = false;

    MarkerListener markListener;

    public FurrowInput(){

        marker = new Ellipse2D.Double(markX+sphere.getCenterX() - markerRadius, markY+sphere.getCenterY() - markerRadius, 2*markerRadius, 2*markerRadius);
        fixedSize = new Dimension( background.getWidth(), background.getHeight());
        markListener = new MarkerListener();
        addMouseListener(markListener);
        addMouseMotionListener(markListener);

        BandListener b = new BandListener();
        addMouseListener(b);
        addMouseMotionListener(b);
    }
    public void drawMarker(Graphics2D g2d){
        Paint markerFill;
        if(markListener.dragging){
            markerFill = new Color(200, 200, 200);
        } else if( markListener.hover){
            markerFill = new Color(255, 255, 230);
        } else if( sphereHovering ){
            markerFill = new Color(200, 200, 200);
        } else{
            markerFill = new Color(150, 150, 150);
        }
        g2d.setPaint(markerFill);
        g2d.fill(marker);

        double dotWidth = 5;
        Ellipse2D ell = new Ellipse2D.Double(
                marker.getX() + dotWidth,
                marker.getY() + dotWidth,
                marker.getWidth() - 2*dotWidth,
                marker.getHeight() - 2*dotWidth
        );
        g2d.setPaint(active);
        g2d.fill(ell);
        g2d.draw(marker);

    }

    void drawZoomWheel(Graphics2D g2d){
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

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.drawImage(background, 0, 0, this);

        if(hovering){
            g2d.setColor(new Color(50, 50, 0));
            g2d.draw(sphere);
        }

        drawPlane(g2d, marker.getCenterX(), marker.getCenterY());
        drawCurve(g2d, marker.getCenterX(), marker.getCenterY());
        drawMarker(g2d);

        drawZoomWheel(g2d);



    }


    static Path2D createBottomCurve(double x, double y, double w, double h, double trim){
        Path2D path = new Path2D.Double();
        path.moveTo(x-w/2, y);
        path.curveTo(x - w/2 + trim, y + h/2, x + w/2 - trim, y + h/2 , x + w/2, y);
        return path;
    }

    static Path2D createTopCurve(double x, double y, double w, double h, double trim){
        Path2D path = new Path2D.Double();
        path.moveTo(x-w/2, y);
        path.curveTo(x - w/2 + trim, y - h/2, x + w/2 - trim    , y - h/2 , x + w/2, y);
        return path;
    }

    static void drawFrontCurves(Graphics2D g2d, double middleX, double middleY, int steps){
        double dtheta = Math.PI/2/steps;
        g2d.setColor(Color.LIGHT_GRAY);
        for(int i = 0; i<steps; i++){
            double dx = (0.5*sphere.getWidth()*Math.sin((i+1)*dtheta));
            double dy = (0.5*sphere.getHeight()*Math.cos((i+1)*dtheta));
            g2d.draw(createBottomCurve(middleX, middleY - dy, 2*dx, 0.3*2*dx, 0.3*2*dx));
            g2d.draw(createBottomCurve(middleX, middleY + dy, 2*dx, 0.3*2*dx, 0.3*2*dx));
        }
    }
    static double fs = 0.2;
    static double[] getXYZ(double ix, double iy){
        double R = sphere.getWidth()/2;
        double cx = sphere.getCenterX();
        double cy = sphere.getCenterY();

        double a = (1 + fs*fs);
        double b = 2*fs*(iy - cy);
        double c = (ix - cx)*(ix -cx) + (iy - cy)*(iy-cy) - R*R;

        double x = ix - cx;
        double y = (-b - Math.sqrt(b*b - 4*a*c))/(2*a);
        double z = -fs*y + (cy - iy);
        return new double[]{x, y, z};
    }
    static public void drawPlane(Graphics2D g2d, double ix, double iy){
        double cx = sphere.getCenterX();
        double cy = sphere.getCenterY();
        double l = sphere.getWidth()/4;
        double[] points = {
            -l, 0, -l,
            -l, 0, +l,
            +l, 0, +l,
            +l, 0, -l
        };
        double[] startingNormal = {0, -1, 0};
        //plane pointing in the +x direction.
        double[] xyz = getXYZ(ix, iy);
        Vector3DOps.normalize(xyz);
        double[] axis = Vector3DOps.cross(xyz, startingNormal);

        double sin = Vector3DOps.normalize(axis);
        double cos = Vector3DOps.dot(xyz, startingNormal);

        double[] rotation_matrix = {
                cos + axis[0]*axis[0]*(1-cos), axis[0]*axis[1]*(1-cos) - axis[2]*sin, axis[0]*axis[2]*(1-cos) + axis[1]*sin,
                axis[1]*axis[0]*(1-cos) + axis[2]*sin, cos + axis[1]*axis[1]*(1-cos), axis[1]*axis[2]*(1-cos) - axis[0]*sin,
                axis[2]*axis[0]*(1-cos) - axis[1]*sin, axis[2]*axis[1]*(1-cos) + axis[0]*sin, cos + axis[2]*axis[2]*(1-cos)

        };

        for(int index = 0; index<points.length/3; index++){
            double[] original_position = Arrays.copyOfRange(points, index*3, index*3 + 3);

            double[] rotated = new double[]{0,0,0};

            for(int i = 0; i< 3; i++){
                for(int j = 0; j<3; j++){
                    rotated[i] += rotation_matrix[3*i + j]*original_position[j];
                }
            }
            for(int i = 0; i<3; i++){
                points[3*index + i] = rotated[i];
            }
        }
        //First rotate about z-axis, then rotate about x-y projected axis.
        Path2D path = new Path2D.Double();
        double x = cx + points[0];
        double y = cy + fs*points[1] - points[2];
        path.moveTo(x, y);
        for(int i = 1; i<=4; i++){
            x = points[(3*i)%points.length] + cx;
            y = fs*points[(3*i + 1)%points.length] - points[(3*i + 2)%points.length] + cy;
            path.lineTo( x, y );
        }
        g2d.setColor(new Color(100, 0, 0, 75));
        g2d.fill(path);
        g2d.setColor(new Color(100, 0, 0, 75));
        g2d.draw(path);
    }
    /**
     *
     * @param g2d
     * @param ix
     * @param iy
     */
    static void drawCurve( Graphics2D g2d, double ix, double iy){
        //where on the sphere are we?
        double fs = 0.2;
        double R = sphere.getWidth()/2;
        double cx = sphere.getCenterX();
        double cy = sphere.getCenterY();

        double a = (1 + fs*fs);
        double b = 2*fs*(iy - cy);
        double c = (ix - cx)*(ix -cx) + (iy - cy)*(iy-cy) - R*R;

        double x = ix - cx;
        double y = (-b - Math.sqrt(b*b - 4*a*c))/(2*a);
        double z = -fs*y + (cy - iy);

        double r = Math.sqrt(R*R - z*z);
        double h = fs*r;

        double ex = cx - r;
        double ey = cy - z - h;

        g2d.setColor(Color.WHITE);

        float[] fractions = {0.0f, 1.0f};

        Color[] colors = { new Color(255, 0, 255, 75), Color.GREEN};
        if((int)(2*h) > 0){
            //otherwise the gradient paint throws an error.
            Paint p = new LinearGradientPaint((int) cx, (int) (ey), (int) cx, (int) (ey + 2 * h), fractions, colors);
            g2d.setPaint(p);


            Ellipse2D latitude = new Ellipse2D.Double(ex, ey, 2 * r, 2 * h);
            g2d.draw(latitude);
        }

        //double rootB = (-b - Math.sqrt(b*b - 4*))
        double width = Math.abs(ix - cx);

        Paint longPaint;

        if(ix - cx == 0){
            longPaint = colors[colors.length - 1];
        } else if(ix > cx && (int)width != 0){
            longPaint = new LinearGradientPaint(
                    (int)(cx-width), (int)cy, (int)(cx + width), (int)(cy), fractions, colors
            );
        } else{
            int d = (int)width == 0 ? 1: (int)width;
            longPaint =  new LinearGradientPaint(
                    (int)(cx + d), (int)cy, (int)(cx - d), (int)(cy), fractions, colors
            );
        }

        g2d.setPaint(longPaint);
        Ellipse2D longitude = new Ellipse2D.Double(cx - width, cy - R, 2*width, 2*R);
        g2d.draw(longitude);
    }

    static void drawBackCurves(Graphics2D g2d, double middleX, double middleY, int steps){
        double dtheta = Math.PI/2/steps;
        int l = 3;
        g2d.setColor(new Color(255,255, 255, 50));
        for(int i = 0; i<steps; i++){
            double dx = (0.5*sphere.getWidth()*Math.sin((i+1)*dtheta));
            double dy = (0.5*sphere.getHeight()*Math.cos((i+1)*dtheta));

            g2d.draw(createTopCurve(middleX, middleY - dy, 2*dx, 0.3*2*dx, 0.3*2*dx));
            g2d.draw(createTopCurve(middleX, middleY + dy, 2*dx, 0.3*2*dx, 0.3*2*dx));
        }
    }

    static BufferedImage createBackgroundImage(){
        int height = (int)(2*border + 2*sphereRadius);
        int width = (int)(3*border + 2*sphereRadius + bandWidth);
        BufferedImage bg = new BufferedImage( width, height, BufferedImage.TYPE_4BYTE_ABGR );

        Graphics2D g2d = bg.createGraphics();
        //g2d.setColor(Color.WHITE);
        //g2d.fillRect(0,0,width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //g2d.setColor(Color.RED);
        //g2d.fill(sphere);

        int c1X = (int)(2*border);
        int c1Y = (int)(2*border);
        int w2 = (int)(2.5*sphereRadius);
        float[] fractions = {0.0f, 1.0f};
        Color[] colors = {clearBlack, new Color(0, 0, 0, 150)};
        Paint p = new RadialGradientPaint(c1X, c1Y, w2, fractions, colors );
        g2d.setPaint(p);
        g2d.fill(sphere);

        Ellipse2D ring = new Ellipse2D  .Double(sphere.getX()-3, sphere.getY()-3, sphere.getWidth()+5, sphere.getHeight()+5);
        g2d.setStroke(new BasicStroke(5f));
        g2d.setColor(Color.WHITE);
        g2d.draw(ring);
        g2d.setStroke(new BasicStroke(1f));
        g2d.setColor(Color.GRAY);
        Ellipse2D outter = new Ellipse2D  .Double(sphere.getX()-5, sphere.getY()-5, sphere.getWidth()+10, sphere.getHeight()+10);
        g2d.draw(outter);
        //g2d.draw(sphere);
        int middleY = (int)sphere.getCenterY();
        int middleX = (int)sphere.getCenterX();
        int leftX = (int)sphere.getX();
        int topY = (int)sphere.getY();
        //g2d.drawLine(middleX, topY, middleX, topY+(int)sphere.getHeight());
        //g2d.drawLine(leftX, middleY, leftX + (int)sphere.getWidth(), middleY);
        int steps = 3;
        double dtheta = Math.PI/2/steps;


        drawBackCurves(g2d, middleX, middleY, steps);
        drawAxis(g2d, middleX, middleY);
        drawFrontCurves(g2d, middleX, middleY, steps);

        c1X = (int)(3*border);
        c1Y = (int)(2*border);
        w2 = (int)(0.25*sphereRadius);
        fractions = new float[]{0.0f, 1.0f};
        colors = new Color[]{Color.WHITE, clearWhite};
        p = new RadialGradientPaint(c1X, c1Y, w2, fractions, colors );
        g2d.setPaint(p);
        g2d.fill(sphere);

        //draw band.
        g2d.setColor(active);
        g2d.fill(band);
        g2d.setColor(Color.black);
        g2d.draw(band);

        float y1 = (float)(band.getY());
        float y2 = (float)(band.getY() + band.getHeight()*0.25);
        Paint sphbg = new GradientPaint(0f, y1, Color.BLACK, 0f, y2, clearBlack, false);
        g2d.setPaint(sphbg);
        g2d.fill(band);

        y1 = (float)(band.getY() + band.getHeight());
        y2 = (float)(band.getY() + band.getHeight()*0.75);
        sphbg = new GradientPaint(0f, y1, Color.BLACK, 0f, y2, clearBlack, false);
        g2d.setPaint(sphbg);
        g2d.fill(band);
        //g2d.fill(band);


        g2d.dispose();

        return bg;
    }

    private static void drawAxis(Graphics2D g2d, int middleX, int middleY) {
        int l = 3;

        Path2D axis = new Path2D.Double();
        axis.moveTo(middleX, middleY);
        axis.lineTo(middleX, middleY - 0.5*sphere.getHeight() - l);
        axis.moveTo(middleX, middleY);
        axis.lineTo(middleX + 0.5*sphere.getWidth(), middleY + 0.1*sphere.getHeight());
        axis.moveTo(middleX, middleY);
        axis.lineTo(middleX - 0.5*sphere.getWidth(), middleY + 0.2*sphere.getHeight());

        //Stroke s = g2d.getStroke();
        //g2d.setStroke(new BasicStroke(2f));
        //g2d.setColor(new Color(100, 100, 255));
        //g2d.draw(axis);
        //g2d.setStroke(s);

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
        boolean hover = false;
        @Override
        public void mouseMoved(MouseEvent e){
            if(sphere.contains(e.getPoint())){
                if(!sphereHovering) {
                    sphereHovering = true;
                    repaint();
                }
            } else{
                if(sphereHovering){
                    sphereHovering = false;
                    repaint();
                }
            }

            if(marker.contains(e.getPoint())){
                if(!hover){
                    hover = true;
                    repaint();
                }
            } else{
                if(hover){
                    hover = false;
                    repaint();
                }
            }

        }
        @Override
        public void mouseEntered(MouseEvent e) {
            hovering = true;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent evt){
            hovering = false;
            sphereHovering = false;
            repaint();
        }

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
