package deformablemesh;

import deformablemesh.gui.ControlFrame;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.meshview.MeshFrame3D;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.IOException;

/**
 * An entry point for starting the application.
 *
 * Created by msmith on 12/1/15.
 */
public class Deforming3DMesh_Plugin implements PlugInFilter {
    public static final String version = "0.36-dev";
    public static SegmentationModel createDeformingMeshApplication(){
        MeshFrame3D mf3d = new MeshFrame3D();
        mf3d.showFrame(false);

        SegmentationModel model = new SegmentationModel();
        SegmentationController controls = new SegmentationController(model);
        try {
            PropertySaver.loadProperties(controls);
        } catch (IOException e) {
            System.err.println("cannot load properties: " + e.getMessage());
        }
        ControlFrame controller = new ControlFrame(controls);
        controller.showFrame();
        RingController ring_control = new RingController(controls);
        ring_control.startUI();
        controller.addTabbedPanel(ring_control.getContentPane(controller.getFrame()), "furrow");
        controls.setMeshFrame3D(mf3d);
        model.setRingController(ring_control);
        PropertySaver.positionFrames(controller, mf3d);
        return model;
    }

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        SegmentationModel model = createDeformingMeshApplication();
        model.setOriginalPlus(imagePlus);

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {

    }
}
