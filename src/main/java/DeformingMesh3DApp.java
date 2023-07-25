/*-
 * #%L
 * Triangulated surface for deforming in 3D.
 * %%
 * Copyright (C) 2013 - 2023 University College London
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.gui.ControlFrame;
import deformablemesh.gui.PropertySaver;
import deformablemesh.gui.RingController;
import deformablemesh.meshview.MeshFrame3D;
import ij.ImageJ;
import ij.ImagePlus;
import jogamp.nativewindow.jawt.JAWTUtil;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Arrays;

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

    public static void setFlatLAF(){
        try {
            FlatDarculaLaf.installLafInfo();
            FlatDarkLaf.installLafInfo();
            for(UIManager.LookAndFeelInfo lafi: UIManager.getInstalledLookAndFeels()){
                System.out.println(lafi.getClassName());
            }
            UIManager.setLookAndFeel(FlatDarculaLaf.class.getName());
            //UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            //UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        }
        catch (UnsupportedLookAndFeelException e) {
            // handle exception
        }
        catch (ClassNotFoundException e) {
            // handle exception
        }
        catch (InstantiationException e) {
            // handle exception
        }
        catch (IllegalAccessException e) {
            // handle exception
        }
    }
    public static SegmentationController createDeformingMeshApplication(){
        setFlatLAF();
        JAWTUtil.getJAWT(true);
        MeshFrame3D mf3d = new MeshFrame3D();
        SegmentationModel model = new SegmentationModel();
        SegmentationController control = new SegmentationController(model);

        try{
            PropertySaver.loadProperties(control);
        } catch(Exception e){
            System.err.println("cannot load properties: " + e.getMessage());
        }
        ControlFrame controller = new ControlFrame(control);
        controller.showFrame();
        mf3d.showFrame(false);
        mf3d.addLights();
        controller.addMeshFrame3D(mf3d);
        control.setMeshFrame3D(mf3d);
        PropertySaver.positionFrames(controller, mf3d);
        return control;
    }




    private static void start3DApplication(){
        new ImageJ();

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
