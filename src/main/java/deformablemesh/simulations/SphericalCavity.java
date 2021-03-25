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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
        double radius = 0.3;
        double x = 0;
        double y = 0;
        double z = 0;
        double mag = 10.0;



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
    File filename = new File("simulated-mesh.bmf");
    public void saveMeshes(){
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
    }
    private class Display{
        JLabel steps = new JLabel("steps: ");
        JCheckBox record = new JCheckBox("recording");
        JTextField stepsPerSnapshot = new JTextField(10);
        JTextField radPerSnapShot = new JTextField(10);
        JCheckBox rotate = new JCheckBox("rotate view");
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
                saveMeshes();
            });
            panel.add(save, constraints);

            constraints.gridy += 1;
            constraints.gridx = 0;

            JButton but = new JButton("set radius: ");
            JTextField field = new JTextField(5);
            field.setText("" + ce.radius);
            but.addActionListener( evt->{
                double radius = Double.parseDouble(field.getText());
                ce.radius = radius;
            });
            panel.add(but, constraints);
            constraints.gridx+=1;
            panel.add(field, constraints);

            constraints.gridy += 1;
            constraints.gridx = 0;
            panel.add(rotate, constraints);
            constraints.gridx += 1;
            panel.add(new JLabel("Angle per snapshot: "), constraints);
            constraints.gridx += 1;
            panel.add(radPerSnapShot, constraints);


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

    Graph graph = new Graph();
    Graph volume = new Graph();

    public void createCurvaturePlots(){
        graph.setBackground(Color.DARK_GRAY);
        graph.setAxisColor(Color.WHITE);
        volume.setBackground(Color.DARK_GRAY);
        volume.setAxisColor(Color.WHITE);
        int i = 0;
        volume.addData(new double[]{steps}, new double[]{ 4*Math.PI/3.0*Math.pow(ce.radius, 3) });

        for(DeformableMesh3D mesh: drops){
            CurvatureCalculator calc = new CurvatureCalculator(mesh);

            List<double[]> v = calc.calculateCurvature();
            double ave = 0;
            double ave2 = 0;
            double area = 0;
            List<double[]> curves = calc.calculateCurvature();
            for(int j = 0; j <curves.size(); j++){
                double[] row = curves.get(j);
                double c = row[3];
                double ai = calc.calculateMixedArea(j);
                area += ai;
                ave += c*ai;
                ave2 += c*c*ai;

            }
            ave = ave/area;
            ave2 = Math.sqrt(ave2/area - ave*ave);

            DataSet set = graph.addData(new double[]{steps}, new double[]{ave});
            set.setColor(mesh.getColor());
            set = graph.addData(new double[]{ steps},new double[]{ave2});
            set.setColor(mesh.getColor());

            set = volume.addData( new double[]{steps}, new double[]{mesh.calculateVolume()});
            set.setColor(mesh.getColor());

        }
        graph.show(false, "curvatures");
        volume.show(false, "volumes");
    }
    public void plotCurvature(){
        int i = 0;
        volume.getDataSet(i).addPoint(steps, 4*Math.PI/3.0*Math.pow(ce.radius, 3));
        for(DeformableMesh3D mesh: drops){
            CurvatureCalculator calc = new CurvatureCalculator(mesh);

            double ave = 0;
            double ave2 = 0;
            double area = 0;
            List<double[]> curves = calc.calculateCurvature();
            for(int j = 0; j <curves.size(); j++){
                double[] row = curves.get(j);
                double c = row[3];
                double ai = calc.calculateMixedArea(j);
                area += ai;
                ave += c*ai;
                ave2 += c*c*ai;

            }
            ave = ave/area;
            ave2 = Math.sqrt(ave2/area - ave*ave);
            graph.getDataSet(2*i).addPoint(steps, ave);
            graph.getDataSet(2*i + 1).addPoint(steps, ave2);

            volume.getDataSet(i+1).addPoint(steps, mesh.calculateVolume());
            i++;
        }
        graph.refresh(true);
        volume.refresh(true);
    }
    Display display = new Display();
    ImageStack snapShots;
    ImagePlus plus;

    public void updateDisplay(){
        display.steps.setText("steps: " + steps);

        if(recordSnapShots){

            stepsPerFrame = Integer.parseInt(display.stepsPerSnapshot.getText());
            if(steps%stepsPerFrame == 0){
                plotCurvature();

                if(snapShots!=null && snapShots.size()>=500){
                    return;
                }

                if(display.rotate.isSelected()){
                    frame.rotateView(Integer.parseInt( display.radPerSnapShot.getText() ), 0 );
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
                requestRemesh = true;
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
            rem.setMinAndMaxLengths(0.02, 0.06);
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
                } else if( e instanceof PressureForce){
                    PressureForce pf = (PressureForce) e;
                    PressureForce x = new PressureForce(remeshed, pressure);
                    x.setMaxMixedArea(pf.getMaxMixedArea());
                    remeshed.addExternalEnergy(x);
                } else if( e instanceof TriangleAreaDistributor){
                    remeshed.addExternalEnergy( new TriangleAreaDistributor(null, remeshed, pressure*0.5) );
                }
            }
            remeshed.create3DObject();
            frame.removeDataObject(mesh.data_object);
            frame.addDataObject(remeshed.data_object);
            return remeshed;
        });
        stericMeshes.clear();
        prepareStericEnergies();

    }
    public void simulate(){
        long last = System.currentTimeMillis();
        while(!interrupted.get()){
            pause();
            if(requestRemesh){
                remeshMeshes();
                requestRemesh = false;
            }
            step2();
            steps++;
            updateDisplay();
            if(steps%100 == 0){
                long next = System.currentTimeMillis();
                System.out.println( (next - last)/100 + "ave ms per step");
                last = next;
            }
        }
        System.out.println("broken after: " + steps);
    }

    ExecutorService service = Executors.newCachedThreadPool();

    public void step2(){
        try {
            List<Future<Runnable>> futures = service.invokeAll(
                    drops.stream().map(d -> (Callable<Runnable>)d::partialUpdate).collect(Collectors.toList())
            );

            futures.stream().map( f-> {
                try {
                    return f.get();
                } catch (Exception e) {
                    return null;
                }
            } ).forEach(Runnable::run);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Future<?>> futures = new ArrayList<>();
        for(SofterStericMesh s: stericMeshes){
            futures.add( service.submit(s::update) );
        }
        futures.forEach( f ->{
            try{
                f.get();
            } catch(Exception e){
                //wtf.
            }
        });
        fields.forEach(VectorField::update);
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

        int nx = 2;
        int ny = 2;
        int nz = 3;
        double r = 0.2;
        double deltax = nx==1 ? 1 : 2*r/(nx-1);
        double deltay = ny==1 ? 1 : 2*r/(ny-1);
        double deltaz = nz==1 ? 1 : 2*r/(nz-1);
        double rx = nx==1 ? 0 : r;
        double ry = ny == 1 ? 0 : r;
        double rz = nz == 1 ? 0 : r;

        for(int i = 0; i<nx; i++){
            for(int j = 0; j<ny; j++){
                for(int k = 0; k<nz; k++){
                    if(i==1 && j==1){
                        continue;
                    }
                //if(i != j || i!=4) continue;
                    int a = i==1 ? 1 : 0;
                    int b = j == 1 ? 1 : 0;
                    int c = k == 1 ? 1 : 0;


                    double x = i*deltax - rx;
                    double y = j*deltay - ry;
                    double z = k*deltaz - rz;

/*
                    if(a + b + c == 3){
                        //pass
                    } else if(a + b + c != 2){
                        continue;
                    }
*/


                    //z = 0;
                    if( k == 1 ){
                        double xp = Math.cos(Math.PI)*x - Math.sin(Math.PI)*y;
                        double yp = Math.sin(Math.PI)*x + Math.cos(Math.PI)*y;
                        x = xp;
                        y = yp;
                    }

                    Sphere sphere = new Sphere(new double[]{x, y , z}, 0.1);

                    DeformableMesh3D mesh = RayCastMesh.rayCastMesh(sphere, sphere.getCenter(), 2);
                    //mesh = new NewtonMesh3D(mesh);
                    mesh.setShowSurface(true);
                    mesh.setColor(ColorSuggestions.getSuggestion());
                    mesh.GAMMA = 100;
                    mesh.ALPHA = 0.1;
                    mesh.BETA = 0;
                    mesh.addExternalEnergy(ce);
                    VolumeConservation vc = new VolumeConservation(mesh, pressure);
                    vc.setVolume(vc.getVolume()*8);
                    mesh.addExternalEnergy(vc);
                    //mesh.addExternalEnergy(new PressureForce(mesh, pressure));
                    mesh.addExternalEnergy(new TriangleAreaDistributor(null, mesh, pressure*0.01));
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
                if ( ee instanceof TriangleAreaDistributor){
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
