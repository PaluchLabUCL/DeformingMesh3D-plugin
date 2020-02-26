package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.StericMesh;
import deformablemesh.externalenergies.TriangleAreaDistributor;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.NewtonMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.Arrow;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ManyDrops {
    final static Color[] colors = {
        Color.WHITE,
        Color.GREEN,
        Color.YELLOW,
        Color.RED,
        Color.PINK,
        Color.ORANGE,
        Color.BLACK,
        Color.MAGENTA
    };
    List<DeformableMesh3D> meshes = new ArrayList<>();
    List<VectorField> forces = new ArrayList<>();
    List<MeanFieldStericEnergy> quasiStericMeshes = new ArrayList<>();
    List<StericMesh> stericMeshes = new ArrayList<>();
    HeightMapSurface surface;
    MeshFrame3D frame;

    public void start(){
        int cdex = 0;
        for(int i = 0; i<4; i++){
            for(int j = 0; j<4; j++){
                //if(i != j || i!=4) continue;
                Sphere sphere = new Sphere(new double[]{i*0.125, j*0.125 , 0.06}, 0.05);
                //DeformableMesh3D mesh = new NewtonMesh3D(RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 2));
                DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 2);
                mesh.setShowSurface(true);

                mesh.GAMMA = 1500;
                mesh.ALPHA = 1.0;
                mesh.BETA = 0.5;
                mesh.reshape();
                meshes.add(mesh);
            }
        }

        for(DeformableMesh3D mesh: meshes){
            //VectorField following = new VectorField(mesh);
            //following.initialize();
            //forces.add(following);
            prepareEnergies(mesh);

        }

        surface = generateSurface();
        surface.createHeighMapDataObject();
        surface.surfaceGeometry.setShowSurface(true);
        surface.surfaceGeometry.data_object.setWireColor(Color.BLUE);

    }

    public void createDisplay(){
        frame = new MeshFrame3D();

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();
        int cdex = 0;
        for(DeformableMesh3D mesh: meshes){
            mesh.create3DObject();
            Color c = colors[cdex++%colors.length];
            mesh.data_object.setWireColor(c);
            mesh.data_object.setColor(c);
            frame.addDataObject(mesh.data_object);
        }
        forces.forEach(force ->{
            force.initialize();
            force.vectors.forEach(frame::addDataObject);
        });

        frame.addDataObject(surface.surfaceGeometry.data_object);

            }
    double gravityMagnitude = 0.0015625;
    double surfaceFactor = 10.;
    double volumeConservation = 10;
    double steric = 2.0;
    public void prepareEnergies(DeformableMesh3D mesh){
        ExternalEnergy gravity = new ExternalEnergy(){

            @Override
            public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                for(int i = 0; i<fz.length; i++){
                    fz[i] += -gravityMagnitude;
                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return pos[2];
            }
        };

        ExternalEnergy hardSurface = new ExternalEnergy(){

            @Override
            public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                for(int i = 0; i<fx.length; i++){
                    double z = positions[i*3 + 2];
                    if(z<0){
                        fz[i] += -z*(-gravityMagnitude + surfaceFactor);
                    }
                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return 0;
            }
        };
        surface = generateSurface();

        if(gravityMagnitude != 0) {
            mesh.addExternalEnergy(gravity);

        }

        if(surfaceFactor != 0){
            mesh.addExternalEnergy(surface);
        }

        if(volumeConservation != 0) {
            mesh.addExternalEnergy(new VolumeConservation(mesh, volumeConservation));
        }

        if(steric != 0){
            addSurfaceStericMeshEnergy(mesh);

        }
    }

    void addSurfaceStericMeshEnergy(DeformableMesh3D mesh){
        for(int i = 0; i<meshes.size(); i++){

            DeformableMesh3D b = meshes.get(i);
            if(b==mesh){
                continue;
            }
            StericMesh sm = new StericMesh(b, mesh, steric);
            b.addExternalEnergy(sm);

            stericMeshes.add(sm);

        }
    }

    void addMeanFieldStericEnergy(DeformableMesh3D mesh){
        MeanFieldStericEnergy mse = new MeanFieldStericEnergy(mesh, steric);
        for(int i = 0; i<meshes.size(); i++){
            DeformableMesh3D b = meshes.get(i);
            if(b!=mesh) {
                b.addExternalEnergy(mse);
            }

        }
        quasiStericMeshes.add(mse);
    }

    public void step(){

        for(DeformableMesh3D mesh: meshes){
            mesh.update();
        }
        forces.forEach(VectorField::update);



        stericMeshes.forEach(sm -> sm.update());

    }


    public void run(){
        new ImageJ();
        start();
        createDisplay();
        Pacer pacer = new Pacer();
        ImageStack snapShots=null;
        ImagePlus plus = null;
        while(true){
            pacer.step();
            if(pacer.taken%15000 == 1){

                BufferedImage img = frame.snapShot();
                ImageProcessor p = new ColorProcessor(img);
                if(snapShots==null){
                    snapShots = new ImageStack(p.getWidth(), p.getHeight());
                    snapShots.addSlice(p);

                } else{
                    snapShots.addSlice(p);
                    if(plus==null){
                        plus = new ImagePlus("snapshots", snapShots);
                        plus.show();
                    }
                    if(snapShots.size() > 200){
                        pacer.state.set(0);
                        pacer.taken = 0;
                    }
                }
            }

            step();

        }
    }
    HeightMapSurface generateSurface(){
        int N = 100;
        double d = 2.0/(N-1);
        double[][] pitted = new double[N][N];
        for(int i = 0; i<N; i++){
            for(int j = 0; j<N; j++){
                double x = d*i - 1;
                double y = d*j - 1;
                double s = Math.sin(x*Math.PI);
                double c = Math.cos(y*Math.PI/2);
                pitted[j][i] =  -0.5*s*s*c*c;
            }
        }

        return new HeightMapSurface(pitted, surfaceFactor);
    }


    public static void main(String[] args){

        new ManyDrops().run();


    }
}

class MeanFieldStericEnergy implements ExternalEnergy{
    DeformableMesh3D source;
    CurvatureCalculator calc;
    double cx, cy, cz;
    double r_cutoff;
    double k;
    public MeanFieldStericEnergy(DeformableMesh3D mesh, double k){
        source = mesh;
        this.k = k;
        calc = new CurvatureCalculator(source);
    }

    public void update(){
        cx = 0;
        cy = 0;
        cz = 0;

        double cx2 = 0;
        double cy2 = 0;
        double cz2 = 0;

        double Atot = 0;
        for(Node3D node: source.nodes){
            double[] pt = node.getCoordinates();
            double amx = calc.calculateMixedArea(node);
            cx += pt[0]*amx;
            cy += pt[1]*amx;
            cz += pt[2]*amx;

            cx2 += pt[0]*pt[0]*amx;
            cy2 += pt[1]*pt[1]*amx;
            cz2 += pt[2]*pt[2]*amx;

            Atot += amx;
        }

        cx = cx / Atot;
        cy = cy / Atot;
        cz = cz / Atot;

        cx2 = cx2 / Atot;
        cy2 = cy2 / Atot;
        cz2 = cz2 / Atot;
        r_cutoff = 1.2*Math.sqrt( cx2 + cy2 + cz2 - (cx*cx + cy*cy + cz*cz));
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        for(int i = 0; i<fx.length; i++){
            double x = positions[i*3];
            double y = positions[i*3 + 1];
            double z = positions[i*3 + 2];

            double[] dr = {
                    x - cx,
                    y - cy,
                    z - cz
            };
            double m = Vector3DOps.mag(dr);
            if(m>r_cutoff){
                continue;
            }
            double p = 2*( r_cutoff - m )/r_cutoff;

            double f = k*p*p*p;
            fx[i] += f*dr[0]/m;
            fy[i] += f*dr[1]/m;
            fz[i] += f*dr[2]/m;

        }
    }

    @Override
    public double getEnergy(double[] pos) {
        return 0;
    }
}

class Pacer{
    JButton step = new JButton(">|");
    JButton play = new JButton("|>");
    JLabel label = new JLabel(String.format("%08d", 0));
    AtomicInteger state = new AtomicInteger(0);
    JFrame frame = new JFrame("pacer");
    int taken = 0;
    public Pacer(){

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content,  BoxLayout.LINE_AXIS));
        step.addActionListener(evt->{
            state.set(1);
            synchronized (state){
                state.notifyAll();
            }
        });
        play.addActionListener(evt->{
            if(play.getText().equals("|>")){
                play.setText("St");
                state.set(Integer.MAX_VALUE);
                synchronized (state){
                    state.notifyAll();
                }
            } else{
                play.setText("|>");
                state.set(0);
            }
        });
        content.add(step);
        content.add(play);
        content.add(label);
        frame.setContentPane(content);
        frame.pack();
        frame.setVisible(true);
    }

    public void step(){
        synchronized (state){
            while(state.get()==0){
                try {
                    state.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            taken += 1;
            final int update = taken;
            EventQueue.invokeLater(()->{
                label.setText(String.format("%08d", update));
            });
            state.decrementAndGet();
            return;
        }
    }
}




class VectorField{
    DeformableMesh3D mesh;
    List<Arrow> vectors;

    double scale = 0.1;

    public VectorField(DeformableMesh3D mesh){
        this.mesh = mesh;
        vectors = new ArrayList<>();
    }
    public void initialize(){
        int n = mesh.nodes.size();
        if(mesh!=null){
            List<ExternalEnergy> energies = mesh.getExternalEnergies();
            final double[] fx = new double[n];
            final double[] fy = new double[n];
            final double[] fz = new double[n];



            for(ExternalEnergy external: energies) {
                external.updateForces(mesh.positions, fx, fy, fz);
            }

            for(int i = 0; i<n; i++){
                Node3D node = mesh.nodes.get(i);
                double[] pt = node.getCoordinates();
                double[] f = {fx[i], fy[i], fz[i]};
                double m = Vector3DOps.normalize(f);
                if(m<1e-4){
                    continue;
                }
                Arrow a = new Arrow(1, 0.0625);
                a.pointAlong(f);
                m = m*scale;
                a.moveTo(pt[0] + f[0]*m*0.5, pt[1] + f[1]*m*0.5, pt[2] + f[2]*m*0.5);
                a.setScale(m);
                vectors.add(a);
            }

        }

    }

    public void update(){

        int n = mesh.nodes.size();
        if(mesh!=null){
            List<ExternalEnergy> energies = mesh.getExternalEnergies();
            final double[] fx = new double[n];
            final double[] fy = new double[n];
            final double[] fz = new double[n];



            for(ExternalEnergy external: energies) {
                external.updateForces(mesh.positions, fx, fy, fz);
            }

            for(int i = 0; i<n; i++){
                Node3D node = mesh.nodes.get(i);
                double[] pt = node.getCoordinates();
                double[] f = {fx[i], fy[i], fz[i]};
                double m = Vector3DOps.normalize(f);
                if(m<1e-4){
                    continue;
                }
                m = m*scale;
                Arrow a = vectors.get(i);
                a.moveTo(pt[0] + f[0]*m*0.5, pt[1] + f[1]*m*0.5, pt[2] + f[2]*m*0.5);
                a.pointAlong(f);
                a.setScale(m);
            }

        }
    }
}
