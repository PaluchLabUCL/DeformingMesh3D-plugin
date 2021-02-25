package deformablemesh.simulations;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.externalenergies.PressureForce;
import deformablemesh.externalenergies.SofterStericMesh;
import deformablemesh.externalenergies.TriangleAreaDistributor;
import deformablemesh.externalenergies.VolumeConservation;
import deformablemesh.geometry.ConnectionRemesher;
import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.VectorField;
import deformablemesh.track.MeshTracker;
import deformablemesh.track.Track;
import deformablemesh.util.ColorSuggestions;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import lightgraph.DataSet;
import lightgraph.Graph;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class SphericalCavity {
    double pressure = 1.0;
    double steric = 0.5;


    List<DeformableMesh3D> drops = new ArrayList<>();
    List<SofterStericMesh> stericMeshes = new ArrayList();
    List<VectorField> fields = new ArrayList<>();
    //simulation flow.
    AtomicBoolean interrupted = new AtomicBoolean(false);
    long steps = 0;
    boolean recordSnapShots = false;
    int stepsPerFrame = 100;
    Semaphore limit = new Semaphore(1);
    boolean requestRemesh = false;
    CavityEnergy ce = new CavityEnergy();
    MeshFrame3D frame = new MeshFrame3D();

    class CavityEnergy implements ExternalEnergy{
        double radius = 0.014;
        double x = 0;
        double y = 0;
        double z = 0;
        double mag = 1.25/4;



        public void updateForcesGlobal(double[] positions, double[] fx, double[] fy, double[] fz){
            double mx = 0;
            double my = 0;
            double mz = 0;

            for(int i = 0; i<fx.length; i++){
                double dx = positions[3*i] - x;
                double dy = positions[3*i + 1] - y;
                double dz = positions[3*i + 2] - z;
                dz = 0;
                double d2 = dx*dx + dy*dy + dz*dz;
                if(d2 > radius*radius){
                    double d = Math.sqrt(d2);
                    dx = dx/d;
                    dy = dy/d;
                    dz = dz/d;
                    double f = Math.sqrt(d - radius)*mag;
                    mx += -dx*f;
                    my += -dy*f;
                    mz += -dz*f;
                }
            }
            mx = mx/fx.length;
            my = my/fy.length;
            mz = mz/fz.length;
            for(int i = 0; i<fx.length; i++){
                fx[i] += mx;
                fy[i] += my;
                fz[i] += mz;
            }
        }
        @Override
        public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
            updateForcesPointwise(positions, fx, fy, fz);
        }


        public void updateForcesPointwise(double[] positions, double[] fx, double[] fy, double[] fz) {
            double mx = 0;
            double my = 0;
            double mz = 0;

            for(int i = 0; i<fx.length; i++){
                double dx = positions[3*i] - x;
                double dy = positions[3*i + 1] - y;
                double dz = positions[3*i + 2] - z;
                //dz = 0;
                double d2 = dx*dx + dy*dy + dz*dz;
                if(d2 > radius*radius){
                    double d = Math.sqrt(d2);
                    dx = dx/d;
                    dy = dy/d;
                    dz = dz/d;
                    double f = Math.pow(d - radius, 2)*mag;
                    fx[i] += -dx*f;
                    fy[i] += -dy*f;
                    fz[i] += -dz*f;
                }
            }
        }

        public void updateForcesPie(double[] positions, double[] fx, double[] fy, double[] fz) {

            for(int i = 0; i<fx.length; i++){
                double dx = positions[3*i] - x;
                double dy = positions[3*i + 1] - y;
                double dz = 0;

                double d2 = dx*dx + dy*dy + dz*dz;

                if(dx < 0){
                    fx[i] += -dx*mag;
                }

                if(dy < 0){
                    fy[i] += -dy*mag;
                }

                if(d2 > radius*radius){
                    double d = Math.sqrt(d2);



                    dx = dx/d;
                    dy = dy/d;
                    dz = dz/d;
                    double f = Math.sqrt(d - radius)*mag;
                    if(dx>0) {
                        fx[i] += -dx * f;
                    }
                    if(dy>0){
                        fy[i] += -dy*f;
                    }

                    fz[i] += -dz*f;
                }



            }
        }

        @Override
        public double getEnergy(double[] pos) {
            return 0;
        }
    }
    private class Display{
        JLabel steps = new JLabel("steps: ");
        JCheckBox record = new JCheckBox("recording");
        JTextField stepsPerSnapshot = new JTextField(10);
        Display(){
            JFrame frame = new JFrame("simulation controls");
            JPanel panel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            panel.setLayout(layout);
            GridBagConstraints constraints = new GridBagConstraints();
            panel.add(steps, constraints);
            constraints.gridx = 1;
            panel.add(new JLabel("steps per snapshot: "), constraints);
            constraints.gridx = 2;
            panel.add(stepsPerSnapshot, constraints);
            constraints.gridx = 0;
            constraints.gridy = 1;
            panel.add(record, constraints);
            constraints.gridx = 1;
            stepsPerSnapshot.setText("" + stepsPerFrame);
            JButton go = new JButton("go");
            go.addActionListener(evt->{
                if(go.getText().equals("go")){
                    go.setText("stop");
                    limit.release();
                } else{
                    go.setText("go");
                    try {
                        limit.acquire();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });
            panel.add(go, constraints);

            constraints.gridx = 2;
            JButton remesh = new JButton("remesh");
            remesh.addActionListener(evt->{
                requestRemesh = true;
            });
            panel.add(remesh, constraints);
            constraints.gridx = 3;
            JButton save = new JButton("save");
            save.addActionListener(evt->{
                List<Track> tracks = new ArrayList<>();
                for(DeformableMesh3D mesh: drops){
                    Track t = new Track(ColorSuggestions.getColorName(mesh.getColor()));
                    t.addMesh(0, mesh);
                    tracks.add(t);
                }
                MeshTracker tracker = new MeshTracker();
                tracker.addMeshTracks(tracks);
                try {
                    MeshWriter.saveMeshes(new File("./simulated-spheres.bmf"), tracker);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            panel.add(save, constraints);
            frame.setContentPane(panel);
            frame.pack();
            frame.setVisible(true);
            try{
                limit.acquire();
            } catch(Exception e){

            }
            record.addActionListener(evt->{
                recordSnapShots = record.isSelected();
            });
        }



    }

    public void createCurvaturePlots(){
        int i = 0;
        for(DeformableMesh3D mesh: drops){
            CurvatureCalculator calc = new CurvatureCalculator(mesh);
            List<double[]> v = calc.calculateCurvature();
            double min = Double.MAX_VALUE;
            double max = -min;
            double ave = 0;
            for(double[] row: v){
                double c = row[3];
                min = Math.min(min, c);
                max = Math.max(max, c);
                ave += c;
            }
            ave = ave/v.size();
            DataSet set = graph.addData(new double[]{steps}, new double[]{ave});
            set.setColor(mesh.getColor());
            set = graph.addData(new double[]{ steps},new double[]{min});
            set.setColor(mesh.getColor());
            set = graph.addData(new double[]{steps}, new double[]{max});
            set.setColor(mesh.getColor());
        }
        graph.show(false);
    }

    public void plotCurvature(){
        int i = 0;
        for(DeformableMesh3D mesh: drops){
            CurvatureCalculator calc = new CurvatureCalculator(mesh);
            List<double[]> v = calc.calculateCurvature();
            double min = Double.MAX_VALUE;
            double max = -min;
            double ave = 0;
            for(double[] row: v){
                double c = row[3];
                min = Math.min(min, c);
                max = Math.max(max, c);
                ave += c;
            }
            ave = ave/v.size();
            graph.getDataSet(3*i).addPoint(steps, ave);
            graph.getDataSet(3*i + 1).addPoint(steps, min);
            graph.getDataSet(3*i + 2).addPoint(steps, max);
            i++;
        }
        graph.refresh(true);
    }
    Display display = new Display();
    ImageStack snapShots;
    ImagePlus plus;
    Graph graph = new Graph();
    public void updateDisplay(){
        display.steps.setText("steps: " + steps);

        if(recordSnapShots){

            if(steps%stepsPerFrame == 0){
                plotCurvature();

                if(snapShots!=null && snapShots.size()>=500){
                    return;
                }
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
                }
            }
        }
    }
    public void pause(){
        try{
            limit.acquire();
            limit.release();
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public void remeshMeshes(){

        drops.replaceAll( mesh -> {
            ConnectionRemesher rem = new ConnectionRemesher();
            rem.setMinAndMaxLengths(0.008, 0.019);
            DeformableMesh3D remeshed = rem.remesh(mesh);
            remeshed.GAMMA = mesh.GAMMA;
            remeshed.ALPHA = mesh.ALPHA;
            remeshed.BETA = mesh.BETA;
            remeshed.setColor(mesh.getColor());
            remeshed.setShowSurface(true);
            remeshed.addExternalEnergy(ce);
            for (ExternalEnergy e : mesh.getExternalEnergies()) {
                if (e instanceof VolumeConservation) {
                    VolumeConservation vc = (VolumeConservation) e;
                    VolumeConservation re = new VolumeConservation(remeshed, pressure);
                    re.setVolume(vc.getVolume());
                    remeshed.addExternalEnergy(re);
                    break;
                }
            }
            remeshed.create3DObject();
            frame.removeDataObject(mesh.data_object);
            frame.addDataObject(remeshed.data_object);
            return remeshed;
        });


    }
    public void simulate(){
        while(!interrupted.get()){
            pause();
            if(requestRemesh){
                remeshMeshes();
                requestRemesh = false;
            }
            step();
            steps++;
            updateDisplay();
        }
        System.out.println("broken after: " + steps);
    }

    public void step(){
        for(DeformableMesh3D mesh: drops){
            mesh.update();
        }
        for(SofterStericMesh s: stericMeshes){
            s.update();
        }
        fields.forEach(VectorField::update);

    }
    public void start(){

        frame.showFrame(true);
        frame.setBackgroundColor(new Color(0, 60, 0));
        frame.addLights();

        int n = 2;
        double r = 0.08;
        double delta = n==1 ? 1 : 2*r/(n-1);
        for(int i = 0; i<n; i++){
            for(int j = 0; j<n; j++){
                for(int k = 1; k<n; k++){

                //if(i != j || i!=4) continue;

                    double x = i*delta - r;
                    double y = j*delta - r;
                    double z = k*delta - r;
                    if(i==1 && j==1){
                        z = -z;
                    }
                    if( k > 0){
                        double xp = Math.cos(Math.PI/4)*x - Math.sin(Math.PI/4)*y;
                        double yp = Math.sin(Math.PI/4)*x + Math.cos(Math.PI/4)*y;
                        x = xp;
                        y = yp;
                    }

                    Sphere sphere = new Sphere(new double[]{x, y , z}, 0.1);

                    DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 2);
                    //mesh = new NewtonMesh3D(mesh);
                    mesh.setShowSurface(true);
                    mesh.setColor(ColorSuggestions.getSuggestion());
                    mesh.GAMMA = 100;
                    mesh.ALPHA = 1.0/2;
                    mesh.BETA = 0.1/4;
                    mesh.addExternalEnergy(ce);
                    mesh.addExternalEnergy(new VolumeConservation(mesh, pressure/4));
                    //mesh.addExternalEnergy(new PressureForce(mesh, pressure));
                    mesh.addExternalEnergy(new TriangleAreaDistributor(null, mesh, pressure));
                    mesh.reshape();
                    drops.add(mesh);
                    mesh.create3DObject();
                    frame.addDataObject(mesh.data_object);
                }
            }
        }
        prepareStericEnergies();
        createCurvaturePlots();
        /*drops.forEach(d ->{
            List<ExternalEnergy> energies = new ArrayList<>();
            for(ExternalEnergy ee: d.getExternalEnergies()){
                if ( ee instanceof SofterStericMesh){
                    energies.add(ee);
                }
            }
            VectorField field = new VectorField(d, energies);
            field.initialize();
            field.setGamma(d.GAMMA);
            fields.add(field);
            frame.addDataObject(field);
        });*/
        simulate();

    }

    public void prepareStericEnergies(){
        for(int i = 0; i<drops.size(); i++){
            for(int j = 0; j<drops.size(); j++){
                if(i == j){
                    continue;
                }
                DeformableMesh3D a = drops.get(i);
                DeformableMesh3D b = drops.get(j);
                SofterStericMesh s = new SofterStericMesh(a, b, steric);
                a.addExternalEnergy(s);
                stericMeshes.add(s);
            }
        }
    }

    public static void main(String[] args){
        new ImageJ();
        SphericalCavity sc = new SphericalCavity();
        sc.start();
    }
}
