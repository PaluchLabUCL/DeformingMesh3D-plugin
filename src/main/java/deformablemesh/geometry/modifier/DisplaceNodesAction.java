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
