package deformablemesh.experimental;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Window;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is an experimental class derived from https://github.com/odinsbane/JavaTF2ModelRunner
 *
 * A small server can be started that will let predictions be made remotely. They can also be
 * made locally by specifying "localhost" if the server is started on the local computer.
 *
 */
public class RemotePrediction implements PlugInFilter {
    Socket server;
    ImagePlus toProcess;
    ProgressDialog progress;
    static class ProgressDialog extends JDialog{
        JLabel status;
        JProgressBar progress;
        ProgressDialog(Window w, int total){
            super(w);
            JPanel panel = new JPanel(new BorderLayout());
            status = new JLabel("preparing " + total + " frames");
            progress = new JProgressBar(0, 2*total);
            panel.add(status, BorderLayout.SOUTH);
            panel.add(progress, BorderLayout.CENTER);
            setContentPane(panel);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }
        public void updateStatus(String s){
            status.setText(s);
        }
    }
    @Override
    public int setup(String s, ImagePlus imagePlus) {
        GenericDialog gd = new GenericDialog("Select host");
        gd.addStringField("hostname", "", 30);
        gd.addStringField("port", "5050", 6);
        gd.showDialog();
        String hostname = gd.getNextString();
        int port = Integer.parseInt( gd.getNextString() );
        try {
            server = new Socket(hostname, port);
        } catch (IOException e) {
            e.printStackTrace();
            return DONE;
        }
        toProcess = imagePlus;
        ImageJ ij = IJ.getInstance();
        progress = new ProgressDialog(ij, toProcess.getNFrames());
        progress.pack();
        progress.setVisible(true);
        if(ij != null) {
            int x = ij.getX();
            int y = ij.getY();
            int h = ij.getHeight();
            progress.setLocation(x, y + h / 2);
        }
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {

        try {
            process(toProcess);
            progress.updateStatus("Finished!");

        } catch (IOException e) {
            progress.updateStatus("Failed with IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (ExecutionException e) {
            progress.updateStatus("Failed with ExecutionException: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            progress.updateStatus("Failed with Interrupted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(ImagePlus plus) throws IOException, ExecutionException, InterruptedException {
        ExecutorService sending = Executors.newFixedThreadPool(1);
        List<Future<Integer>> finishing = new ArrayList<>();
        OutputStream os = server.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        final InputStream in = server.getInputStream();
        DataInputStream din = new DataInputStream(in);
        dos.writeInt(plus.getNFrames());
        for(int i = 0; i<plus.getNFrames(); i++){
            final int frame = i;
            Future<Integer> future = sending.submit(()->{
                try {
                    byte[] data = FloatRunner.getImageData(plus, frame);
                    dos.writeInt(plus.getNChannels());
                    dos.writeInt(plus.getWidth());
                    dos.writeInt(plus.getHeight());
                    dos.writeInt(plus.getNSlices());
                    os.write(data);
                    return frame;
                } catch(IOException e){
                    throw new RuntimeException(e);
                }
            });
            finishing.add(future);
        }
        progress.updateStatus("Finished preparing awaiting results");
        List<ImagePlus> pluses = new ArrayList<>();
        for(Future<Integer> result: finishing){
            int frame = result.get();
            int previous = progress.progress.getValue();
            progress.progress.setValue( previous + 1);
            progress.updateStatus("compiling frame " + previous);
            int outputs = din.readInt();

            for(int i = 0; i<outputs; i++){
                int c = din.readInt();
                int w = din.readInt();
                int h = din.readInt();
                int s = din.readInt();
                byte[] buffer = new byte[c*w*h*s*4];
                int read = 0;
                while(read < buffer.length){
                    int r = din.read(buffer, read, buffer.length - read);
                    if(r<0) break;

                    read += r;
                }
                ImagePlus op = FloatRunner.toImage(buffer, c, w, h, s, plus);

                if(frame == 0){
                    op.setTitle(i + " created from " + plus.getShortTitle());
                    op.show();
                    pluses.add(op);
                } else{
                    ImagePlus or = pluses.get(i);
                    ImageStack stack = or.getStack();
                    ImageStack fresh = op.getStack();
                    int nc = or.getNChannels();
                    int ns = or.getNSlices();
                    for(int j = 1; j<=fresh.size(); j++){
                        stack.addSlice(fresh.getProcessor(j));
                    }
                    or.setStack(stack,nc, ns, (frame + 1));
                    or.setOpenAsHyperStack(true);
                }

            }
            progress.progress.setValue( previous + 1);
            progress.updateStatus("compiled frame " + previous + " with " + outputs + " outputs");

        }
    }
    public static void main(String... args){
        ImageJ ij = IJ.getInstance();
        ProgressDialog progress = new ProgressDialog(ij, 5);
        progress.pack();
        progress.setVisible(true);
    }
}

class FloatRunner {
    static byte[] getImageData(ImagePlus plus, int frame){
        int c = plus.getNChannels();
        int s = plus.getNSlices();
        int w = plus.getWidth();
        int h = plus.getHeight();
        ImageStack stack = plus.getStack();

        int frame_offset = c*s*frame;

        byte[] data = new byte[4 * w*h*s*c];
        FloatBuffer buffer = ByteBuffer.wrap(data).asFloatBuffer();
        for(int i = 0; i<c*s; i++){
            FloatProcessor proc = stack.getProcessor(frame_offset + 1 + i).convertToFloatProcessor();
            buffer.put( (float[])proc.getPixels());
        }

        return data;
    }
    public static ImagePlus toImage(byte[] data, int oc, int ow, int oh, int os, ImagePlus original){
        ImagePlus dup = original.createImagePlus();
        ImageStack stack = new ImageStack(ow, oh);
        int slices = oc*os;
        FloatBuffer buffer = ByteBuffer.wrap(data).asFloatBuffer();
        for(int i = 0; i<slices; i++){
            ImageProcessor proc = new FloatProcessor(ow, oh);
            float[] pixels = new float[ow*oh];
            buffer.get(pixels, 0, pixels.length);
            proc.setPixels(pixels);
            stack.addSlice(proc);
        }

        dup.setStack(stack, oc, os, 1);
        if(dup.getNSlices() != original.getNSlices() || dup.getHeight() != original.getHeight() || dup.getWidth() != original.getWidth()){
            Calibration c0 = original.getCalibration();
            Calibration c1 = dup.getCalibration();
            c1.pixelDepth = c0.pixelDepth*dup.getNSlices() / original.getNSlices();
            c1.pixelWidth = c0.pixelWidth*dup.getWidth() / original.getWidth();
            c1.pixelHeight = c0.pixelHeight*dup.getHeight() / original.getHeight();
            dup.setCalibration(c1);
        }

        return dup;
    }

}