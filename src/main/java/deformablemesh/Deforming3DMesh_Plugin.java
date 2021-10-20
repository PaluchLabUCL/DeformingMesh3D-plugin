package deformablemesh;

import deformablemesh.gui.ControlFrame;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.meshview.MeshFrame3D;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.util.stream.IntStream;

/**
 * An entry point for starting the application.
 *
 * Created by msmith on 12/1/15.
 */
public class Deforming3DMesh_Plugin implements PlugInFilter {
    public static final String version = "0.7.0";
    public static SegmentationModel createDeformingMeshApplication(){

        MeshFrame3D mf3d = new MeshFrame3D();
        SegmentationModel model = new SegmentationModel();
        SegmentationController control = new SegmentationController(model);

        try {
            PropertySaver.loadProperties(control);
        } catch (IOException e) {
            System.err.println("cannot load properties: " + e.getMessage());
        }
        ControlFrame controller = new ControlFrame(control);
        controller.showFrame();
        mf3d.showFrame(false);
        mf3d.addLights();

        controller.addMeshFrame3D(mf3d);
        control.setMeshFrame3D(mf3d);

        PropertySaver.positionFrames(controller, mf3d);
        return model;
    }

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        SegmentationModel model = createDeformingMeshApplication();
        if(imagePlus != null) {

            int channel = 0;
            if(imagePlus.getNChannels()>1){
                Object[] values = IntStream.range(1, imagePlus.getNChannels()+1).boxed().toArray();
                Object channelChoice = JOptionPane.showInputDialog(
                        IJ.getInstance(),
                        "Select Channel:",
                        "Choose Channel",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        values,
                        values[0]
                );
                if(channelChoice == null) channelChoice = 1;
                channel = (Integer)channelChoice - 1;
            }



            model.setOriginalPlus(imagePlus, channel);
        }

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {

    }
}
