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
package deformablemesh;

import deformablemesh.gui.ControlFrame;
import deformablemesh.gui.PropertySaver;
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
    public static final String version = "0.9.7";
    public static SegmentationController createDeformingMeshApplication(){

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
        return control;
    }

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        SegmentationController controller = createDeformingMeshApplication();
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



            controller.setOriginalPlus(imagePlus, channel);
        }

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {

    }
}
