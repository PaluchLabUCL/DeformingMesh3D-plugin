import deformablemesh.SegmentationModel;
import deformablemesh.io.MeshWriter;
import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * For running remotely. This will open the corresponding image files. Create and deform a mesh through all frames
 * and save the mesh, never opening a gui.
 * User: msmith
 * Date: 7/31/13
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Headless3DApp {
    int thread_count = 6;
    ImagePlus original_plus;
    ImagePlus binary_plus;
    File output;
    ExecutorService workers;
    CompletionService<SegmentationModel> delegate;
    //HashMap<Integer, DeformableMesh3D> meshes;
    int ITERATIONS = 1;
    double SEGMENT_SIZE = 0.05;
    double ALPHA = 2.5;
    double GAMMA = 500;
    double PRESSURE = 0.0;
    MeshWriter writer;
    void loadConstants(Map<MeshConstants, Double> constants){
        if(constants.keySet().contains(MeshConstants.Alpha)){
            ALPHA = constants.get(MeshConstants.Alpha);
        } else{
            constants.put(MeshConstants.Alpha,ALPHA);
        }

        if(constants.keySet().contains(MeshConstants.Gamma)){
            GAMMA = constants.get(MeshConstants.Gamma);
        } else{
            constants.put(MeshConstants.Gamma,GAMMA);
        }


        if(constants.keySet().contains(MeshConstants.Pressure)){
            PRESSURE = constants.get(MeshConstants.Pressure);
        } else{
            constants.put(MeshConstants.Pressure,PRESSURE);
        }


        if(constants.keySet().contains(MeshConstants.Threads)){
            thread_count = (int)(double)constants.get(MeshConstants.Threads);
        } else{
            constants.put(MeshConstants.Threads, 1.0*thread_count);
        }

        if(constants.keySet().contains(MeshConstants.Iterations)){
           ITERATIONS = (int)(double)constants.get(MeshConstants.Iterations);
        } else{
            constants.put(MeshConstants.Iterations,1.0*ITERATIONS);
        }

        if(constants.keySet().contains(MeshConstants.SegmentSize)){
           SEGMENT_SIZE = constants.get(MeshConstants.SegmentSize);
        } else{
            constants.put(MeshConstants.SegmentSize, SEGMENT_SIZE);
        }




    }
    public Headless3DApp(ImagePlus original, ImagePlus binary, File output, Map<MeshConstants, Double> constants){
        loadConstants(constants);
        original_plus = original;
        if(original_plus.getNFrames()<thread_count){
            thread_count = original_plus.getNFrames();
        }
        binary_plus = binary;
        this.output = output;
        workers = Executors.newFixedThreadPool(thread_count);
        delegate = new ExecutorCompletionService<>(workers);
        //meshes = new HashMap<Integer, DeformableMesh3D>();
        writer = new MeshWriter(output);
        writer.open();
    }

    public void newMeshes(){
        int current = 0;
        for(int i = 0; i<thread_count; i++){
            final int frame = current;
            final SegmentationModel model = new SegmentationModel();
            delegate.submit(new Callable<SegmentationModel>(){

                public void run(){
                    model.setOriginalPlus(original_plus);
                    model.setFrame(frame);
                    //model.applyMask();
                    //model.createMesh();
                    model.setAlpha(ALPHA);
                    model.setGamma(GAMMA);
                    model.setPressure(PRESSURE);

                    model.deformMesh(ITERATIONS);
                }

                @Override
                public SegmentationModel call() throws Exception {
                    run();
                    //meshes.put(model.getCurrentFrame(), model.mesh);
                    writer.write(model.getSelectedMesh(model.getCurrentFrame()), model.getCurrentFrame());
                    return model;
                }
            });
            current++;
        }
        int total = original_plus.getNFrames();
        int finished = 0;
        while(finished<total){

            try {
                final SegmentationModel model = delegate.take().get();
                finished++;
                if(current<total){
                    final int frame = current;
                    delegate.submit(new Callable<SegmentationModel>(){

                        public void run(){
                            model.setFrame(frame);
                            //model.applyMask();
                            createAndAddMesh(model);

                            model.deformMesh(ITERATIONS);
                        }

                        @Override
                        public SegmentationModel call() throws Exception {
                            run();
                            //meshes.put(model.getCurrentFrame(), model.mesh);
                            writer.write(model.getSelectedMesh(model.getCurrentFrame()), model.getCurrentFrame());
                            return model;
                        }
                    });
                    current++;
                }

            } catch (InterruptedException e) {
                //interrupted must be shutting down.
                e.printStackTrace();
                System.exit(0);
            } catch (ExecutionException e) {
                //execution exception something went wrong.
                e.printStackTrace();
                System.exit(-1);
            }


        }

        //DeformableMesh3DTools.saveMeshes(output, meshes);
        System.exit(0);

    }

    /**
     * Creates an initialized mesh, and adds it to the model for headless
     * deformations.
     *
     * @param model
     */
    private void createAndAddMesh(SegmentationModel model) {

    }

    public static void main(String[] args){
        if(args.length!=3){
            System.out.println("usage: \n app original.tif binary.tif constants.txt");
            System.exit(1);
        }

        ImagePlus original = new ImagePlus(new File(args[0]).getAbsolutePath());
        ImagePlus binary = new ImagePlus(new File(args[1]).getAbsolutePath());

        File output = new File("headless.bmf");

        Map<MeshConstants,Double> constants = new HashMap<MeshConstants, Double>();

        try {
            MeshConstants.loadConstants(new File(args[2]),constants);
        } catch (IOException e) {
            e.printStackTrace();
        }


        Headless3DApp app = new Headless3DApp(original, binary, output, constants);

        try {
            MeshConstants.writeConstants(new File("running-constants.txt"),constants);
        } catch (IOException e) {
            e.printStackTrace();
        }

        app.newMeshes();




    }

}
enum MeshConstants{
    Alpha, Gamma, Pressure, SegmentSize, Threads, Iterations;
    static void loadConstants(File f, Map<MeshConstants, Double> map) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("utf-8")));
        String s;
        while((s=reader.readLine())!=null){
            if(s.charAt(0)=='#') continue;
            String[] vals = s.split(Pattern.quote("\t"));
            try{
                MeshConstants mc = MeshConstants.valueOf(vals[0]);
                Double d = Double.parseDouble(vals[1]);
                map.put(mc,d);
            } catch (NumberFormatException nfe){
                System.out.println(vals[0] + " did not have a correct value associated with it");
            } catch (EnumConstantNotPresentException ecnpe){
                System.out.println(vals[0] + " is not an constant. Escape comment lines with '#'");
            }
        }
        reader.close();
    }

    static void writeConstants(File f, Map<MeshConstants, Double> map) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), Charset.forName("utf-8")));
        for(MeshConstants mc: map.keySet()){

            writer.write(String.format("%s\t%f\n",mc.name(),map.get(mc)));

        }
        writer.close();
    }
}