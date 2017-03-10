import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.gui.ControlFrame;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.meshview.MeshFrame3D;
import ij.ImageJ;
import ij.ImagePlus;
import jogamp.nativewindow.jawt.JAWTUtil;

import javax.swing.JFrame;
import java.awt.EventQueue;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 *
 * For development of a 3d version of the deforming mesh.
 *
 * User: msmith
 * Date: 7/2/13
 * Time: 8:01 AM
 */
public class DeformingMesh3DApp{
    static File input;
    public static SegmentationController createDeformingMeshApplication(){
        JAWTUtil.getJAWT(true);
        MeshFrame3D mf3d = new MeshFrame3D();
        mf3d.showFrame(false);
        mf3d.addLights();
        SegmentationModel model = new SegmentationModel();
        SegmentationController control = new SegmentationController(model);
        try{
            PropertySaver.loadProperties(control);
        } catch(Exception e){
            System.err.println("cannot load properties: " + e.getMessage());
        }
        ControlFrame controller = new ControlFrame(control);
        controller.showFrame();
        RingController ring_control = new RingController(control);
        ring_control.startUI();
        controller.addTabbedPanel(ring_control.getContentPane(controller.getFrame()), "furrow");
        control.setMeshFrame3D(mf3d);
        model.setRingController(ring_control);
        PropertySaver.positionFrames(controller, mf3d);
        return control;
    }




    private static void start3DApplication(){
        ImageJ.main(new String[]{});

        SegmentationController controls = createDeformingMeshApplication();

        if(input!=null) {
            String o = input.getAbsolutePath();
            controls.setOriginalPlus(new ImagePlus(o));
        }
    }
    public static void main(String[] args){
        if(args.length>0){
            input = new File(args[0]);
        }
        EventQueue.invokeLater(()->start3DApplication());


    }


}
