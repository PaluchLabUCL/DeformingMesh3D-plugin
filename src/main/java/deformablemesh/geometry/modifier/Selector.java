package deformablemesh.geometry.modifier;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.DataCanvas;
import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;
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
    double radius = 0.1;
    MeshFrame3D meshFrame3D;
    Sphere sphere = new Sphere(new double[] { 0.0, 0.0, 0.0}, radius);

    public Selector(MeshModifier modifier){
        this.modifier = modifier;
    }
    public DataObject getCursor(){
        return sphere.createDataObject();
    }

    @Override
    public void register() {
        if(modifier.frame != null){
            meshFrame3D = modifier.frame;
            meshFrame3D.addDataObject(getCursor());
        }
    }

    @Override
    public void deregister() {
        if(meshFrame3D != null){
            meshFrame3D.removeDataObject(getCursor());
        }
    }

    @Override
    public void updatePressed(double[] results, MouseEvent evt) {

    }

    @Override
    public void updateReleased(double[] pt, MouseEvent evt) {

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
    public void updateClicked(double[] point, MouseEvent evt) {
        sphere.moveTo(point);
        DeformableMesh3D mesh = modifier.mesh;
        List<Node3D> selectable = new ArrayList<>();
        for(Node3D node: mesh.nodes){
            double d = Vector3DOps.distance(point, node.getCoordinates());
            if(d < radius){
                selectable.add(node);
            }
        }
        modifier.selectNodes(selectable);
    }

    @Override
    public void updateMoved(double[] point, MouseEvent evt) {
        sphere.moveTo(point);
    }

    double[] getPickLocation(List<PickResult> results){
        for(PickResult result: results){
            PickIntersection pick = result.getIntersection(0);
            Point3d pt = pick.getPointCoordinates();
            return new double[] { pt.x, pt.y, pt.z};
        }
        return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }

    @Override
    public void updateDragged(double[] point, MouseEvent evt) {
        sphere.moveTo(point);
        //BranchGroup bg = modifier.getSliceDataObject().getBranchGroup();
        DeformableMesh3D mesh = modifier.mesh;
        List<Node3D> selectable = new ArrayList<>();
        for(Node3D node: mesh.nodes){
            double d = Vector3DOps.distance(point, node.getCoordinates());
            if(d < radius){
                selectable.add(node);
            }
        }
        modifier.selectNodes(selectable);
    }

    public void dragSelectedPoints(double[] point){
        List<Sphere> markers = modifier.getMarkers();
        List<Node3D> selected = modifier.getSelected();
        Point3d pt = new Point3d(point);
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

    public void setRadius(double f){
        radius = f;
        sphere.setRadius(f);
    }

}
