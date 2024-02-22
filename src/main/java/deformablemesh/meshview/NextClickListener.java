package deformablemesh.meshview;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.Track;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;

public class NextClickListener implements CanvasView{
    SynchronousQueue<Optional<DeformableMesh3D>> clicked = new SynchronousQueue<>();
    List<DeformableMesh3D> meshes;
    public NextClickListener(List<DeformableMesh3D> meshes){
        this.meshes = meshes;
    }
    @Override
    public void updatePressed(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateReleased(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateClicked(PickResult[] results, MouseEvent evt) {
        //int frame = controller.getCurrentFrame();
        //DeformableMesh3D selected = controller.getSelectedMesh();

        for(PickResult result: results) {
            for (DeformableMesh3D mesh : meshes) {
                if (mesh.data_object != null) {
                    GeometryArray array = result.getGeometryArray();
                    if (mesh.data_object.lines == array || mesh.data_object.surface_object.getGeometry() == array) {
                        try {
                            clicked.put(Optional.of(mesh));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }

                }
            }
        }
        try {
            clicked.put(Optional.empty());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMoved(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateDragged(PickResult[] results, MouseEvent evt) {

    }

    /**
     * This causes a race condition.
     */
    public DeformableMesh3D getMesh(){
        try {
            Optional<DeformableMesh3D> opt = clicked.take();
            return opt.orElse(null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
