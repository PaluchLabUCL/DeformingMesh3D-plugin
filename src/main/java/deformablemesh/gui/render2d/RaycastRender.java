package deformablemesh.gui.render2d;

import deformablemesh.geometry.*;
import deformablemesh.io.MeshReader;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class RaycastRender implements Runnable {
    int width = 1024;
    int height = 1024;
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    double CUTOFF = 1e-8;
    JPanel panel;
    List<InterceptingMesh3D> meshes = new ArrayList<>();
    List<Color> colors = new ArrayList<>();
    Box3D bottom;
    Box3D chip;
    public RaycastRender(){
        chip = new Box3D(new double[]{0, -1 - 0.25, 0}, 0.25, 0.5, 0.25);
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

    public void setMeshes(List<DeformableMesh3D> meshes){
        double lowest = 1;
        for( DeformableMesh3D mesh: meshes){
            InterceptingMesh3D im = new InterceptingMesh3D(mesh);

            for(Node3D node: mesh.nodes){
                double[] c = node.getCoordinates();
                //ping(c);

                double z  = c[2];
                if(z < lowest){
                    lowest = z;
                }
            }
            this.meshes.add(im);
            colors.add(mesh.getColor());
            System.out.println(mesh.getColor());
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
            this.r = r.r*0.75;
            this.g = r.g*0.75;
            this.b = r.b*0.75;
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

        double dot = Vector3DOps.dot(dir, surfaceNormal);
        double[] pc = { surfaceNormal[0]*dot, surfaceNormal[1]*dot, surfaceNormal[2]*dot };
        double[] per = Vector3DOps.difference(dir, pc);
        double[] res = Vector3DOps.add(per, pc, -1);




        return res;

    }
    Random ng = new Random();

    class PointLight{
        final double[] dir, src, px, py;
        double intensity = 64;
        double cone = Math.PI*0.15;

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
        ExecutorService service = Executors.newFixedThreadPool(20);
        double hits = 0;
        while(true){
            List<PointLight> lights = new ArrayList<>();
            lights.add(new PointLight(new double[]{2, -1, 3}, new double[]{0, 0, -0.37}));
            lights.add(new PointLight(new double[]{-2, -1, 3}, new double[]{0, 0, -0.37}));
            lights.add( new PointLight(new double[]{0, -3, 3}, new double[]{0, 0, -0.37}) );


            int rays = 1000;
            long start = System.currentTimeMillis();
            long emitted = 0;
            for(int i = 0; i<rays; i++){

                Deque<Ray> castRays = new ArrayDeque<>();
                for(PointLight light: lights){
                    castRays.add(light.originalRay());
                }
                while(castRays.size() > 0) {
                    Ray r = castRays.pop();
                    emitted++;

                    Intersection closest = null;
                    double min = Double.MAX_VALUE;


                    List<Future<List<Intersection>>> futures = meshes.stream().map(
                            m -> service.submit(
                                    () ->m.getIntersections(r.pt, r.dir)
                            )).collect(Collectors.toList());
                    int dex = 0;
                    int scatteringDex = -1;
                    for(Future<List<Intersection>> future: futures){
                        try {
                            List<Intersection> intersections = future.get();
                            for (Intersection intersection : intersections) {
                                if (Vector3DOps.dot(intersection.surfaceNormal, r.dir) >= 0) {
                                    continue;
                                }
                                double d = Vector3DOps.dot(r.dir, Vector3DOps.difference(intersection.location, r.pt));
                                if (d < min && d>CUTOFF) {
                                    closest = intersection;
                                    min = d;
                                    scatteringDex = dex;
                                }
                            }
                        } catch(Exception e){
                            throw new RuntimeException(e);
                        }
                        dex++;
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
                        Color sc;
                        if(scatteringDex < 0){
                            sc = Color.WHITE;
                        } else{
                            sc = colors.get(scatteringDex);
                        }
                        float[] comps = sc.getRGBComponents(new float[4]);

                        double[] scattered = scatter(r.dir, closest.surfaceNormal);
                        double[] delta = Vector3DOps.difference(new double[]{0, -1, 0}, closest.location);
                        double n = Vector3DOps.normalize(delta);
                        Ray next = new Ray(closest.location, scattered, r);
                        next.r = next.r*(0.5*comps[0] + 0.5);
                        next.g = next.g*(0.5*comps[1] + 0.5);
                        next.b = next.b*(0.5*comps[2] + 0.5);

                        double dot = Vector3DOps.dot(closest.surfaceNormal, delta);
                        if( dot > 0.0) {

                            Ray diffuse = new Ray(closest.location, delta, r);
                            diffuse.r = r.r*comps[0]*dot;
                            diffuse.g = r.g*comps[1]*dot;
                            diffuse.b = r.b*comps[2]*dot;

                            if (diffuse.r + diffuse.g + diffuse.b >= 1) {
                                castRays.add(diffuse);
                            }
                        }
                        if(next.r + next.b + next.g >= 1){
                            castRays.add(next);
                        }
                    }
                }
            }
            System.out.println("hits: " + hits + "rays cast: " + emitted + " in: " + ( (System.currentTimeMillis() - start)/1000.0 ) + " seconds");
            panel.repaint();
            if(hits > 1_000_000_000){
                return;
            }
        }
    }
    public static void main(String[] args) throws IOException {
        new ImageJ();
        List<Track> tracks = MeshReader.loadMeshes(new File(IJ.getFilePath("select mesh file")));
        List<DeformableMesh3D> mesh = tracks.stream().filter(t->t.containsKey(0)).map(t-> t.getMesh(0)).collect(Collectors.toList());
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
        rr.setMeshes(mesh);
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
