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
package deformablemesh.meshview;

import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.Track;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;

/**
 * Created by smithm3 on 24/05/18.
 */
public class PickSelector implements CanvasView {
    SegmentationController controller;

    public PickSelector(SegmentationController c){
        this.controller = c;
    }

    public void updatePick(PickResult[] results, MouseEvent evt, boolean clicked) {
        if(clicked) {
            int frame = controller.getCurrentFrame();
            DeformableMesh3D selected = controller.getSelectedMesh();
            for(PickResult result: results){
                for(Track track: controller.getAllTracks()){
                    if(track.containsKey(frame)){
                        DeformableMesh3D mesh = track.getMesh(frame);
                        if(mesh==selected){
                            continue;
                        }
                        if(mesh.data_object!=null){
                            GeometryArray array = result.getGeometryArray();
                            if(mesh.data_object.lines==array || mesh.data_object.surface_object.getGeometry()==array){
                                controller.selectMesh(mesh);
                                return;
                            }

                        }
                    }
                }

            }
        }

    }

    @Override
    public void updatePressed(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateReleased(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateClicked(PickResult[] results, MouseEvent evt) {
        updatePick(results, evt, true);

    }

    @Override
    public void updateMoved(PickResult[] results, MouseEvent evt) {
        updatePick(results, evt, false);
    }

    @Override
    public void updateDragged(PickResult[] results, MouseEvent evt) {

    }
}
