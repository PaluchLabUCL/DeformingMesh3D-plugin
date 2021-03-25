package deformablemesh.meshview;

import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.Track;
import ij.plugin.filter.PlugInFilter;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
