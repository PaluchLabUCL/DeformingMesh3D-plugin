package deformablemesh.gui.render2d;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.Connection3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import lightgraph.painters.GraphPainter;
import lightgraph.painters.PanelPainter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RenderFrame2D {
    volatile boolean keepRendering = true;
    volatile int frame;
    double [] cameraView;
    double [] cameraUp;
    double [] cameraRight;

    double[] light = {0, 0, -1};
    String svg;
    double[] center = new double[3];

    double height = 640;
    double width = 640;


    GraphPainter painter;
    Dimension preferredSize = new Dimension(640, 480);

    Image drawing = new BufferedImage(preferredSize.width, preferredSize.height, BufferedImage.TYPE_INT_ARGB);

    Camera camera;
    List<Track> tracks = new ArrayList<>();
    JPanel panel;
    Color backgoundColor = Color.WHITE;
    public RenderFrame2D(){
        setDefaultView();
        buildPanel();
    }

    static class DepthObject<T> implements Comparable<DepthObject<T>>{
        final T object;
        final double depth;
        DepthObject(T t, double d){
            object = t;
            depth = d;
        }


        @Override
        public int compareTo(DepthObject<T> b){
            return Double.compare(depth, b.depth);
        }

        T get(){
            return object;
        }
    }

    static class Camera{
        double[] forward;
        double[] up;
        double[] right;
        double width =  640;
        double extent = 1;
        double[] center = new double[3];
        Camera(double[] forward, double[] up, double[] right){
            this.forward = Arrays.copyOf(forward, 3);
            this.up = Arrays.copyOf(up, 3);
            this.right = Arrays.copyOf(right, 3);
        }

        double[] getCameraCoordinates(double[] pt){
            double scale = width/extent;
            double offsetx = width/2;
            double offsety = width/2;
            double dx = pt[0] - center[0];
            double dy = pt[1] - center[1];
            double dz = pt[2] - center[2];
            double y = up[0]*dx + up[1]*dy + up[2]*dz;
            double x = right[0]*dx + right[1]*dy + right[2]*dz;
            double z = forward[0]*dx + forward[2]*dy + forward[2]*dz;
            return new double[]{x*scale + offsetx, y*scale + offsety, z};
        }

        DepthObject<DeformableMesh3D> create(DeformableMesh3D mesh){
            double[] center = DeformableMesh3DTools.centerAndRadius(mesh.nodes);
            return new DepthObject<>(mesh, getCameraCoordinates(center)[2]);
        }

        DepthObject<Triangle3D> create(Triangle3D t){
            return new DepthObject<>(t, getCameraCoordinates(t.center)[2]);
        }

        DepthObject<Connection3D> create(Connection3D c){
            return new DepthObject<>(c, 0.5*(getCameraCoordinates(c.A.getCoordinates())[2] + getCameraCoordinates(c.B.getCoordinates())[2]));
        }

    }

    /**
     * Only meshes existing in the current frame are rendered.
     *
     * @param frame
     */
    public void setFrame(int frame){
        this.frame = frame;
    }

    public void renderConnections(Camera camera, List<Connection3D> list, Color color){

        List<DepthObject<Connection3D>> sorted = list.stream().map(camera::create).sorted().collect(Collectors.toList());
        double back = sorted.get(0).depth;
        double front = sorted.get(sorted.size()-1).depth;

        painter.startGroup();
        sorted.forEach(
                (st)->{
                    Connection3D con = st.get();
                    double[] a = camera.getCameraCoordinates(con.A.getCoordinates());
                    double[] b = camera.getCameraCoordinates(con.B.getCoordinates());

                    double lw = interpolateLineWidth(back, front, st.depth);

                    painter.setLineWidth(lw);
                    painter.setColor(color.darker());

                    painter.drawLine(a[0], a[1], b[0], b[1]);

                }
        );
        painter.endGroup();

    }

    public double interpolateLineWidth(double back, double front, double depth){

        double f = (depth - back)/(front - back);
        return 0.5 + 1.5*f;
    }

    Staging staging = new Staging();

    public void setDefaultView(){
        setViews(
                new double[]{ 1, 1, 1},
                new double[]{0, 0, 1}
        );
    }

    void setOrtherView(){
        cameraView = new double[]{0, 0, -1};
        cameraUp = new double[]{ 1, 0, 0};
        cameraRight = Vector3DOps.cross(cameraView, cameraUp);
        render();
    }

    static double[] rotateVector(double[] vector, double[] axis, double angle){
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[] x = Vector3DOps.cross(axis, vector);
        double dot = Vector3DOps.dot(axis, vector)*(1-c);

        return new double[]{
                vector[0]*c + x[0]*s + dot*axis[0],
                vector[1]*c + x[1]*s + dot*axis[1],
                vector[2]*c + x[2]*s + dot*axis[2]
        };

    }

    public void rotateViewPhi(double dphi){
        double[] up = rotateVector(cameraUp, cameraRight, dphi);
        double[] view = rotateVector(cameraView, cameraRight, dphi);
        cameraUp = up;
        cameraView = view;

        render();
    }

    public void rotateViewTheta(double dtheta){
        double[] right = rotateVector(cameraRight, cameraUp, dtheta);
        double[] view = rotateVector(cameraView, cameraUp, dtheta);
        cameraRight = right;
        cameraView = view;

        render();
    }





    static double[] scaled(double s, double[] v){
        return new double[]{v[0]*s, v[1]*s, v[2]*s};
    }
    public void setViews(double[] direction, double[] approximatelyUp){
        Vector3DOps.normalize(direction);
        Vector3DOps.normalize(approximatelyUp);
        double dot = Vector3DOps.dot(direction, approximatelyUp);
        if(dot==1){
            throw new RuntimeException("view direction and up are parallel!");
        }
        double[] remain = Vector3DOps.difference(approximatelyUp, scaled(dot, direction));
        Vector3DOps.normalize(remain);
        cameraView = direction;
        cameraUp = remain;
        cameraRight = Vector3DOps.cross(cameraView, cameraUp);

    }

    List<DeformableMesh3D> meshes = new ArrayList<>();
    public JPanel buildPanel(){
        panel = new JPanel(){

            @Override
            public void paintComponent(Graphics g){
                g.setColor(backgoundColor);
                g.fillRect(0, 0, preferredSize.width, preferredSize.height);
                g.drawImage(drawing, 0, 0, this);
            };

            @Override
            public Dimension getPreferredSize(){
                return preferredSize;
            }
        };

        MouseAdapter adapter = new MouseAdapter() {
            double[] xy = new double[2];

            @Override
            public void mousePressed(MouseEvent evt) {
                xy[0] = evt.getX();
                xy[1] = evt.getY();
            }

            @Override
            public void mouseClicked(MouseEvent evt){
                setOrtherView();
            }

            @Override
            public void mouseDragged(MouseEvent evt){
                double x = evt.getX();
                double y = evt.getY();
                double dx = x - xy[0];
                double dy = y - xy[1];
                if(dx*dx>dy*dy){
                    rotateViewTheta(dx*0.01);
                } else{
                    rotateViewPhi(-dy*0.01);
                }
                xy[0] = x;
                xy[1] = y;
            }
        };

        panel.addMouseListener(adapter);
        panel.addMouseMotionListener(adapter);

        return panel;
    }


    class Staging{
        AtomicReference<Runnable> job = new AtomicReference<>();
        public void post(Runnable j){
            job.set(j);
            synchronized (job) {
                job.notifyAll();
            }
        }

        public Runnable take() throws InterruptedException {
            synchronized (job){
                Runnable r = job.getAndSet(null);
                while(r==null) {
                    job.wait();
                    r = job.getAndSet(null);
                }
                return r;
            }
        }
    }

    void startRenderLoop(){
        Thread renderLoop = new Thread(()->{
            while(!Thread.currentThread().isInterrupted() && keepRendering){
                try{
                    staging.take().run();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        });
        renderLoop.setDaemon(true);
        renderLoop.start();
    }
    public void stopRunning(){
        keepRendering = false;
        staging.post( () -> {}); //unblock the queue.
    }
    public static RenderFrame2D createRenderingMeshFrame(){
        JFrame frame = new JFrame();
        RenderFrame2D render = new RenderFrame2D();
        render.startRenderLoop();

        frame.setContentPane(render.panel);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                render.stopRunning();
            }
        });
        return render;
    }
    public static void main(String[] args) throws IOException {
        RenderFrame2D render = createRenderingMeshFrame();
        List<Track> tracks = MeshWriter.loadMeshes(new File(args[0]));
        render.setTracks(tracks);

    }


    public void render(){
        final Camera camera = new Camera(cameraView, cameraUp, cameraRight);
        staging.post(()->renderMeshes(camera));

    }



    public void renderMeshes(Camera camera){
        BufferedImage replacement = new BufferedImage(
                preferredSize.width, preferredSize.height, BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = replacement.createGraphics();
        painter = new PanelPainter(g2d, Color.WHITE);

        Map<DeformableMesh3D, Color> colorMap = new HashMap<>();

        List<DeformableMesh3D> meshes = tracks.stream().filter(t->t.containsKey(frame)).map(t->{
            DeformableMesh3D mesh = t.getMesh(0);
            colorMap.put(mesh, t.getColor());
            return mesh;
        }).collect(Collectors.toList());

        meshes.stream().map(camera::create).sorted().map(DepthObject::get).forEach(mesh->{
            List<Connection3D> connections = pruneConnections(mesh.getConnections(), mesh.triangles);
            renderConnections(camera, connections, colorMap.get(mesh));
            renderTriangles(camera, mesh.triangles, colorMap.get(mesh));
        });

        drawing = replacement;
        panel.repaint();
    }

    List<Connection3D> pruneConnections(List<Connection3D> connections, List<Triangle3D> triangles){
        triangles.forEach(Triangle3D::update);
        Set<Connection3D> pruned = new HashSet<>(connections);
        List<Connection3D> ret = new ArrayList<>(pruned.size());

        Triangle3D[] pair = new Triangle3D[2];
        for(Connection3D con: pruned){
            int count = 0;
            for(Triangle3D tri: triangles){
                if(tri.hasConnection(con)){
                    pair[count] = tri;
                    count++;
                    if(count==2){
                        break;
                    }
                }
            }
            if(!closeEnough(pair[0].normal, pair[1].normal, 1e-6)){
                ret.add(con);
            }


        }

        return ret;

    }

    boolean closeEnough(double[] a, double[] b, double s){
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];

        return (dx*dx+dy*dy + dz*dz)<s;

    }

    public void renderTriangles(Camera camera, List<Triangle3D> list, Color color){
        Color surfaceColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
        List<DepthObject<Triangle3D>> sorted = list.stream().map(camera::create).sorted().collect(Collectors.toList());

        painter.startGroup();
        sorted.forEach(
                (st)->{
                    Triangle3D t = st.get();
                    Path2D path = new Path2D.Double();
                    double[] a = camera.getCameraCoordinates(t.getCoordinates(0));
                    path.moveTo(a[0], a[1]);
                    double[] b = camera.getCameraCoordinates(t.getCoordinates(1));
                    path.lineTo(b[0], b[1]);
                    double[] c = camera.getCameraCoordinates(t.getCoordinates(2));
                    path.lineTo(c[0], c[1]);
                    path.closePath();

                    if(Vector3DOps.dot(t.normal, camera.forward)>0) {
                        painter.setColor(surfaceColor);
                        painter.fill(path);
                    }

                }
        );
        painter.endGroup();

    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        render();

    }
}
