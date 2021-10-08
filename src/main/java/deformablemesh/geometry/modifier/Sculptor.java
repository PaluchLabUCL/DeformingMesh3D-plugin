package deformablemesh.geometry.modifier;

import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Sphere;
import deformablemesh.meshview.DataCanvas;
import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.Vector3DOps;
import org.scijava.java3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class Sculptor implements ModificationState {
    double cursorRadius = 0.05;
    Sphere sphere = new Sphere(new double[] { 0.0, 0.0, 0.0}, cursorRadius);
    MeshModifier modifier;
    Set<Node3D> ignoring = new HashSet<>();
    Map<Node3D, Sphere> markers = new HashMap<>();
    MeshFrame3D meshFrame3D = null;

    public Sculptor(MeshModifier modifier){
        this.modifier = modifier;
    }
    public DataObject getCursor(){
        return sphere.createDataObject();
    }

    double[] shifted(double[] point){
        double[] normal  = modifier.furrow.normal;
        return Vector3DOps.add(point, normal, 0.875*sphere.getRadius());
    }

    public void cursorMoved(double[] planePosition) {
        if(planePosition != null) {
            sphere.moveTo(planePosition);
        }
    }

    /**
     * When this sculptor is registered, it attaches to the modifier's meshframe 3D, if one exists.
     *
     */
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
            for(Sphere sphere: markers.values()){
                meshFrame3D.removeDataObject(sphere.createDataObject());
            }
            meshFrame3D.setCanvasControllerEnabled(true);
        }
    }

    @Override
    public void updatePressed(double[] pt, MouseEvent evt) {
        //double[] pt = modifier.getPlanePosition(results);
        if(pt==null){
            return;
        }
        if(meshFrame3D != null){
            meshFrame3D.setCanvasControllerEnabled(false);
        }
        sphere.moveTo(shifted(pt));
        ignoring.addAll(containedNodes());
    }

    @Override
    public void updateReleased(double[] point, MouseEvent evt) {
        ignoring.clear();

        if(markers.size() == 0){
            return;
        }
        List<Node3D> nodes = new ArrayList<>(markers.size());
        List<double[]> deltas = new ArrayList<>(markers.size());
        for(Map.Entry<Node3D, Sphere> marked : markers.entrySet()){
            Sphere s = marked.getValue();
            Node3D n = marked.getKey();
            if(meshFrame3D != null){
                meshFrame3D.removeDataObject(s.createDataObject());
                meshFrame3D.setCanvasControllerEnabled(true);
            }
            double[] delta = Vector3DOps.difference(s.getCenter(), n.getCoordinates());
            nodes.add(n);
            deltas.add(delta);
        }
        markers.clear();
        modifier.postAction(new DisplaceNodesAction(modifier.mesh, nodes, deltas));

    }

    @Override
    public void updateClicked(double[] point, MouseEvent evt) {

    }

    @Override
    public void updateMoved(double[] pt, MouseEvent evt) {
        //cursor should follow here.
        //double[] pt = modifier.getPlanePosition(results);
        if(pt==null){
            return;
        }
        sphere.moveTo(shifted(pt));
    }
    void moveContained(){
        List<Node3D> contained = containedNodes();
        //FurrowTransformer ft = modifier.getFurrowTransformer();
        Furrow3D furrow = modifier.furrow;
        for(int i = 0; i<contained.size(); i++){
            Sphere mark;
            Node3D node = contained.get(i);
            if (markers.containsKey(node)) {
                mark = markers.get(node);
            } else{
                mark = new Sphere(node.getCoordinates(), modifier.SELECTED_NODE_RADIUS);
                if(meshFrame3D != null){
                    modifier.frame.addDataObject(mark.createDataObject());
                }
                markers.put(node, mark);
            }
            double[] pt = mark.getCenter();

            double[] delta = Vector3DOps.difference(pt, sphere.getCenter());
            double z = Vector3DOps.dot(delta, furrow.normal);
            double ri = Math.sqrt(sphere.getRadius()*sphere.getRadius() - z*z);

            //position along center axis of cursor, || to the normal of the furrow.
            double[] curse_i = Vector3DOps.add(sphere.getCenter(), furrow.normal, z);
            double[] d = Vector3DOps.difference(pt, curse_i);
            double l = Vector3DOps.normalize(d);

            double[] newPosition = Vector3DOps.add(curse_i, d, ri);

            //the radius in lengths of image units in the plane.
            mark.moveTo(newPosition);

        }
        if(contained.size() > 0) {
            modifier.mesh.resetPositions();
        }
    }
    @Override
    public void updateDragged(double[] pt, MouseEvent evt) {
        //double[] pt = modifier.getPlanePosition(results);
        if(pt==null){
            return;
        }
        sphere.moveTo(shifted(pt));
        moveContained();

    }
    boolean contained(Node3D n){
        double[] pt;
        if(markers.containsKey(n)){
            pt = markers.get(n).getCenter();
        } else{
            pt = n.getCoordinates();
        }

        double r = sphere.getRadius();
        double[] cPt = sphere.getCenter();

        return Vector3DOps.proximity(pt, cPt, r);
    }
    List<Node3D> containedNodes(){
        return modifier.mesh.nodes.stream().filter( this::contained ).collect(Collectors.toList());
    }


}
