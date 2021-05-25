package deformablemesh.geometry.modifier;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.DataCanvas;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Node;
import org.scijava.java3d.PickConeRay;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Selector implements ModificationState{
    Point3d dragging, delta;
    MeshModifier modifier;

    public Selector(MeshModifier modifier){
        this.modifier = modifier;
    }

    @Override
    public void register() {
        DataCanvas canvas = modifier.frame.getCanvas();
    }

    @Override
    public void deregister() {

    }

    @Override
    public void updatePressed(PickResult[] results, MouseEvent evt) {
        if(evt.isControlDown()){
            //entering drag mode.

            for(PickResult result: results) {
                PickConeRay ray = (PickConeRay)result.getPickShape();
                Vector3d dir = new Vector3d();
                Point3d origin = new Point3d();
                ray.getDirection(dir);
                ray.getOrigin(origin);
                System.out.println("found? " + result);
                return;
            }

        }
    }

    @Override
    public void updateReleased(PickResult[] results, MouseEvent evt) {

        if(dragging!=null && delta !=null){
            List<Node3D> selected = modifier.getSelected();
            double[] displacement = new double[3];
            delta.get(displacement);
            List<double[]> displacements = selected.stream().map(s->displacement).collect(Collectors.toList());


            //stack.postAction(new DisplaceNodesAction(mesh, selected, displacements));
        }
        dragging = null;
        delta = null;

    }

    @Override
    public void updateClicked(PickResult[] results, MouseEvent evt) {
        DeformableMesh3D mesh = modifier.mesh;
        for(PickResult result: results){
            Node node = result.getObject();
            if( mesh.data_object.getBranchGroup().indexOfChild(node) > -1 ){
                PickIntersection pick = result.getIntersection(0);
                Point3d pt = pick.getClosestVertexCoordinates();
                result.getClosestIntersection(pt);
                final Node3D n = modifier.getClosesNode(pt.x, pt.y, pt.z);
                modifier.post(() -> modifier.toggleSelectNode(n));
                return;
            }


        }

    }

    @Override
    public void updateMoved(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateDragged(PickResult[] results, MouseEvent evt) {
        BranchGroup bg = modifier.getSliceDataObject().getBranchGroup();
        List<Sphere> markers = modifier.getMarkers();
        List<Node3D> selected = modifier.getSelected();
        for(PickResult result: results){
            PickIntersection pick = result.getIntersection(0);
            Point3d pt = pick.getPointCoordinates();
            if( dragging == null ){
                dragging = pt;
            } else{
                delta = new Point3d(
                        pt.x - dragging.x,
                        pt.y - dragging.y,
                        pt.z - dragging.z
                );
                for(int i = 0; i<selected.size(); i++){
                    double[] starting = selected.get(i).getCoordinates();
                    markers.get(i).moveTo(new double[]{
                            starting[0] + delta.x, starting[1] + delta.y, starting[2] + delta.z
                    });
                }

            }
        }
    }
}
