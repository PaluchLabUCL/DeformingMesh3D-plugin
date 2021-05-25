package deformablemesh.geometry.modifier;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.util.actions.UndoableActions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DisplaceNodesAction implements UndoableActions {
    List<double[]> originals;
    List<double[]> updated;
    List<Node3D> nodes;
    DeformableMesh3D mesh;
    public DisplaceNodesAction(DeformableMesh3D mesh, List<Node3D> nodes, List<double[]> displacements){
        this.nodes = new ArrayList<>(nodes);
        originals = nodes.stream().map(Node3D::getCoordinates).collect(Collectors.toList());
        this.updated = new ArrayList<>();
        for(int i = 0; i<displacements.size(); i++){
            double[] a = originals.get(i);
            double[] r = displacements.get(i);
            updated.add(new double[]{
                    a[0] + r[0],
                    a[1] + r[1],
                    a[2] + r[2]
            });
        }
        this.mesh = mesh;
    }

    @Override
    public void perform() {
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).setPosition(updated.get(i));
        }
        mesh.resetPositions();
    }

    @Override
    public void undo() {
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).setPosition(originals.get(i));
        }
        mesh.resetPositions();
    }

    @Override
    public void redo() {
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).setPosition(updated.get(i));
        }
        mesh.resetPositions();
    }


}
