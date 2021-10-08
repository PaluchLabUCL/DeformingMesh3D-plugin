package deformablemesh.gui.render2d;

import deformablemesh.geometry.*;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import ij.IJ;
import ij.ImageJ;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class RaycastRender implements Runnable {
    int width = 512;
    int height = 512;
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    double CUTOFF = 1e-8;
    JPanel panel;
    InterceptingMesh3D im;
    Box3D bottom;
    Box3D chip;
    public RaycastRender(){
        chip = new Box3D(new double[]{0, -1 - 0.25, 0}, 2, 0.5, 2);
        Graphics g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
    }
    public void setPanel(JPanel panel){
        this.panel = panel;
    }

    public void ping(double[] c){
        int[] loc = getPxCoordinates(c);
        if(loc[0] >= 0 && loc[0] < width && loc[1] >= 0 && loc[1] < height){
            img.setRGB(loc[0], loc[1], Color.BLUE.getRGB());
        }

    }

    public void setMesh(DeformableMesh3D mesh){
        im = new InterceptingMesh3D(mesh);

        double lowest = 1;
        for(Node3D node: mesh.nodes){
            double[] c = node.getCoordinates();
            ping(c);

            double z  = c[2];
            if(z < lowest){
                lowest = z;
            }
        }
        System.out.println("lowest: " + lowest);
        bottom = new Box3D( new double[]{0., 0., lowest-CUTOFF/2}, 2, 2, CUTOFF);

    }
    static class Ray{
        double[] pt; double[] dir; double r, g, b;
        public Ray(double[] pt, double[] dir){
            this.pt = pt;
            this.dir = dir;
            r = 10;
            g = 10;
            b = 10;
        }

        public Ray(double[] pt, double[] dir, Ray r){
            this.pt = pt;
            this.dir = dir;
            this.r = r.r/2;
            this.g = r.g/2;
            this.b = r.b/2;
        }

    }
    int[] getPxCoordinates(double[] normalized){
        int cx = width/2;
        int cy = height/2;

        //zero is actual size, -1 is bigger and + is further.
        double w = 1/( 1 + normalized[1]);

        int x = (int)( w*normalized[0]*cy ) + cx;
        int y = cy - (int)(w*normalized[2]*cy );
        if(x >= width){
            x = -1;
        }
        if(y>= height){
            y = -1;
        }

        return new int[]{x, y};
    }

    public int accumulate(Ray r, int rgb){
        int alpha = rgb&(0xff000000);
        int red = (int)r.r + ((rgb&0xff0000)>>16);
        red = red>255? 255 : red;
        int green = (int)r.g + ((rgb&0xff00)>>8);
        green = green > 255 ? 255 : green;
        int blue = (int)r.b + (rgb&0xff);
        blue = blue > 255 ? 255 : blue;

        return alpha + (red<<16) + (green<<8) + blue;
    }

    double[] scatter(double[] dir, double[] surfaceNormal){
        double[] c = Vector3DOps.cross(dir, surfaceNormal);

        double theta = Vector3DOps.normalize(c);
        if(theta == 0){
            //parallel.
        }
        double dot = -Vector3DOps.dot(dir, surfaceNormal);
        double[] pc = { surfaceNormal[0]*dot, surfaceNormal[1]*dot, surfaceNormal[2]*dot };
        double[] per = Vector3DOps.difference(dir, pc);
        double[] res = Vector3DOps.add(per, pc, -1);





        return res;

    }
    Random ng = new Random();

    class PointLight{
        final double[] dir, src, px, py;
        double intensity = 4;
        double cone = Math.PI;

        PointLight(double[] src, double[] target){

            this.src = src;

            this.dir = Vector3DOps.difference(target, src);
            Vector3DOps.normalize(this.dir);

            double[] pi = Vector3DOps.cross(new double[]{1, 0, 0}, dir);
            double l = Vector3DOps.normalize(pi);
            if(l<CUTOFF){
                pi = Vector3DOps.cross(new double[]{0, 1, 0}, dir);
                l = Vector3DOps.normalize(pi);
            }
            px = pi;
            py = Vector3DOps.cross(dir, px);
            Vector3DOps.normalize(py);

        }

        Ray originalRay(){
            Ray r = new Ray(src, randomDirection());
            r.r = intensity;
            r.g = intensity;
            r.b = intensity;

            return r;
        }

        double[] randomDirection(){
            double phi = 1 - Math.cos(ng.nextDouble()*cone);
            double theta = Math.PI*2*ng.nextDouble();
            double cx = Math.cos(theta)*Math.sin(phi);
            double cy = Math.sin(theta)*Math.sin(phi);
            double cz = Math.cos(phi);

            double[] v = {
                    px[0]*cx+ py[0]*cy + dir[0]*cz,
                    px[1]*cx + py[1]*cy + dir[1]*cz,
                    px[2]*cx + py[2]*cy + dir[2]*cz
            };
            double m = Vector3DOps.normalize(v);
            return v;
        }

    }



    public void run(){
        while(true){
            List<PointLight> lights = new ArrayList<>();
            lights.add(new PointLight(new double[]{1, -1, 1}, new double[]{-1, 1, -1}));
            lights.add(new PointLight(new double[]{-1, -1, 1}, new double[]{1, 1, -1}));
            lights.add( new PointLight(new double[]{0, 0, 1}, new double[]{0, 0, -1}) );

            double hits = 0;
            int rays = 100;
            for(int i = 0; i<rays; i++){

                Deque<Ray> castRays = new ArrayDeque<>();
                for(PointLight light: lights){
                    castRays.add(light.originalRay());
                }
                while(castRays.size() > 0) {
                    Ray r = castRays.pop();
                    List<Intersection> intersections = im.getIntersections(r.pt, r.dir);


                    Intersection closest = null;
                    double min = Double.MAX_VALUE;

                    for (Intersection intersection : intersections) {
                        if (Vector3DOps.dot(intersection.surfaceNormal, r.dir) >= 0) {
                            continue;
                        }
                        double d = Vector3DOps.dot(r.dir, Vector3DOps.difference(intersection.location, r.pt));
                        if (d < min && d>CUTOFF) {
                            closest = intersection;
                            min = d;
                        }
                    }

                    if (closest == null) {
                        //bottom
                        List<Intersection> bis = bottom.getIntersections(r.pt, r.dir);
                        for (Intersection is : bis) {
                            if (Vector3DOps.dot(is.surfaceNormal, r.dir) >= 0) {
                                continue;
                            }
                            double d = Vector3DOps.dot(r.dir, Vector3DOps.difference(is.location, r.pt));
                            if (d < min && d>CUTOFF) {
                                closest = is;
                                min = d;
                            }
                        }

                    }

                    if (closest == null) {
                        //camera
                        for (Intersection is : chip.getIntersections(r.pt, r.dir)) {
                            if (Vector3DOps.dot(is.surfaceNormal, r.dir) >= 0) {
                                continue;
                            }
                            double d =  Vector3DOps.dot(r.dir, Vector3DOps.difference(is.location, r.pt));
                            if (d < min && r.pt[1] > -0.99) {
                                int[] loc = getPxCoordinates(r.pt);

                                if(loc[0]>=0 && loc[1] >= 0){
                                    closest = is;
                                    min = d;
                                    img.setRGB(loc[0], loc[1], accumulate(r, img.getRGB(loc[0], loc[1])));
                                    hits++;
                                    break;
                                }
                            }
                        }
                    } else{
                        double[] scattered = scatter(r.dir, closest.surfaceNormal);
                        double[] delta = Vector3DOps.difference(new double[]{0, -1, 0}, closest.location);
                        double n = Vector3DOps.normalize(delta);

                        r = new Ray(closest.location, scattered, r);

                        if(r.r + r.b + r.g < 1){
                            r = null;
                        }

                    }

                    if( closest == null){
                        r = null;
                    }
                }

            }
            //System.out.println("paint: " + (hits/rays));
            panel.repaint();

        }
    }
    public static void main(String[] args) throws IOException {
        new ImageJ();
        List<Track> tracks = MeshWriter.loadMeshes(new File(IJ.getFilePath("select mesh file")));
        DeformableMesh3D mesh = tracks.stream().map(t-> t.getMesh(t.getFirstFrame())).findFirst().orElseThrow(IOException::new);
        RaycastRender rr = new RaycastRender();
        JPanel panel = new JPanel(){
            @Override
            public void paintComponent( Graphics g){
                super.paintComponent(g);
                g.drawImage(rr.img, 0, 0, this);
            }
            @Override
            public Dimension getPreferredSize(){
                return new Dimension(rr.img.getWidth(), rr.img.getHeight());
            }
        };
        rr.setPanel(panel);
        rr.setMesh(mesh);
        JFrame frame = new JFrame();
        frame.setContentPane(panel);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        Thread t = new Thread(rr);
        t.setDaemon(true);
        t.start();
    }

}
