package deformablemesh.meshview;

import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Point3d;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;

public class FurrowOrientationListener implements CanvasView{

    SegmentationController controller;
    DeformableMesh3D selected;
    public FurrowOrientationListener(SegmentationController controller){
        this.controller = controller;
        selected = controller.getSelectedMesh();
    }


    @Override
    public void updatePressed(PickResult[] results, MouseEvent evt) {
        
    }

    @Override
    public void updateReleased(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateClicked(PickResult[] results, MouseEvent evt) {
        EventQueue.invokeLater(()->{
            //This happens while iterating over the pick listeners. Force to be last.
            controller.removeFurrowOrientationListener(this);
        });
    }

    @Override
    public void updateMoved(PickResult[] results, MouseEvent evt) {
        if(selected.data_object == null) return;
        Furrow3D furrow = controller.getRingController().getFurrow();
        if(furrow == null) return; //probably not true.
        double[] center = furrow.cm;
        int possible = 0;
        for(PickResult result: results){
            if(selected.data_object!=null){
                GeometryArray array = result.getGeometryArray();
                if(selected.data_object.lines==array || selected.data_object.surface_object.getGeometry()==array){
                    PickIntersection pick = result.getIntersection(0);
                    Point3d pt = pick.getPointCoordinates();
                    double[] apt = { pt.x, pt.y, pt.z};
                    double[] diff = Vector3DOps.difference(apt, center);
                    double mag = Vector3DOps.normalize(diff);
                    if(mag > 0 && possible == 0){
                        possible++;
                        controller.setFurrowForCurrentFrame(center, diff);
                    }
                }
            }
        }
    }

    @Override
    public void updateDragged(PickResult[] results, MouseEvent evt) {

    }
}
