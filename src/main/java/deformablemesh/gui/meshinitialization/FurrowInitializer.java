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
package deformablemesh.gui.meshinitialization;

import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Projectable;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.gui.RingController;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.Vector3DOps;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by msmith on 3/31/16.
 */
public class FurrowInitializer extends JDialog {


    SegmentationController segmentationController;
    RingController rings;

    Initializer initializer;
    Runnable callback;
    JCheckBox showMeshes;
    public FurrowInitializer(JFrame owner, SegmentationController model, Runnable callback){
        super(owner, false);
        this.segmentationController = model;
        this.callback = callback;
        initializer = new Initializer();
    }

    public void start(){
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        JButton next = new JButton("finish");
        next.addActionListener((evt)->{
            finish();
        });
        JButton previous = new JButton("cancel");
        previous.addActionListener((evt)->{
            setVisible(false);
            afterClosing();
        });
        row.add(previous);
        row.add(next);
        showMeshes = new JCheckBox("show meshes");
        showMeshes.setSelected(true);
        showMeshes.addActionListener(evt->{
            showMeshes();
        });
        row.add(showMeshes);
        content.add(row, BorderLayout.SOUTH);


        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));

        panel.add(createHorizontalMidPlaneSelectionPanel());
        panel.add(createVerticalFacingSelectionPanel());
        panel.add(createVerticalMidPlaneSelectionPanel());

        content.add(panel, BorderLayout.CENTER);

        setContentPane(content);


        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowListener(){

            public void windowOpened(WindowEvent e) {}
            public void windowClosing(WindowEvent e) {
                afterClosing();
            }
            public void windowClosed(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowActivated(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
        });

        setVisible(true);
        showMeshes();
    }

    public void finish(){

        setVisible(false);
        double[] c = initializer.getCenter();
        double[] n = initializer.getNormal();
        segmentationController.setFurrowForCurrentFrame(c , n);

        afterClosing();
    }

    private JPanel createHorizontalMidPlaneSelectionPanel(){
        double[] hPos = {0,0,0};
        double[] zDir = {0,0,1};
        SlicePicker xyPicker  = new SlicePicker(segmentationController.getMeshImageStack(), zDir, hPos );
        JPanel horizontal = xyPicker.buildView();
        xyPicker.setLabel("pick x-y points. Slider adjusts z.");
        initializer.addPicker(xyPicker);

        return horizontal;
    }

    private JPanel createVerticalMidPlaneSelectionPanel(){

        double[] hPos = {0,0,0};
        double[] yDir = {0,-1,0};
        SlicePicker xzPicker  = new SlicePicker(segmentationController.getMeshImageStack(), yDir, hPos );
        JPanel horizontal = xzPicker.buildView();
        xzPicker.setLabel("pick x-z points. Slider adjusts y.");
        initializer.addPicker(xzPicker);

        return horizontal;
    }

    void showMeshes(){

        if(showMeshes.isSelected()) {
            segmentationController.submit(()->{
                final List<DeformableMesh3D> meshes = segmentationController.getAllTracks().stream(

                ).filter(
                        t -> t.containsKey(segmentationController.getCurrentFrame())
                ).map(
                        t -> t.getMesh(segmentationController.getCurrentFrame())
                ).collect(Collectors.toList());

                for (final DeformableMesh3D mesh : meshes) {
                    EventQueue.invokeLater(()->{
                        initializer.addProjectableMesh(mesh);
                    });
                }

            });

        } else{
            initializer.clearProjectableMeshes();
        }
    }

    private JPanel createVerticalFacingSelectionPanel(){

        double[] hPos = {0,0,0};
        double[] xDir = {1,0,0};
        SlicePicker yzPicker  = new SlicePicker(segmentationController.getMeshImageStack(), xDir, hPos );
        yzPicker.rotateView();
        JPanel horizontal = yzPicker.buildView();
        yzPicker.setLabel("pick z-y points. Slider adjusts x position");
        initializer.addPicker(yzPicker);

        return horizontal;
    }

    public void afterClosing(){
        callback.run();
        dispose();
    }

    class Initializer implements MouseListener, MouseMotionListener {

        Map<JPanel, SlicePicker> pickers = new HashMap<>();
        CircleModifier A;
        CircleModifier B;
        CircleModifier dragging;
        Map<DeformableMesh3D, ProjectableMesh> meshMap = new HashMap<>();


        public Initializer(){
        }

        public void addPicker(SlicePicker picker){
            pickers.put(picker.view.panel, picker);
            picker.addMouseListener(this);
            picker.addMouseMotionListener(this);
        }

        public double[] getCenter(){
            return new double[]{
                    0.5*(A.position[0] + B.position[0]),
                    0.5*(A.position[1] + B.position[1]),
                    0.5*(A.position[2] + B.position[2]),
            };
        }


        @Override
        public void mouseClicked(MouseEvent e) {
            SlicePicker picker = pickers.get((JPanel)e.getSource());
            double ox = e.getX();
            double oy = e.getY();
            double x = ox/picker.view.getZoom();
            double y = oy/picker.view.getZoom();

            if(picker.inBounds(x,y)){

                if(A==null) {
                    double[] center = picker.getNormalizedCoordinates(x, y);
                    double s = picker.getScale();

                    A = new CircleModifier(center, s);
                    addProjectable(A, Color.RED);
                } else if(B==null) {
                    double[] center = picker.getNormalizedCoordinates(x, y);
                    double s = picker.getScale();

                    B = new CircleModifier(center, s);
                    addProjectable(B, Color.BLUE);

                }

            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            SlicePicker picker = pickers.get(e.getSource());
            double ox = e.getX();
            double oy = e.getY();
            double x = ox/picker.view.getZoom();
            double y = oy/picker.view.getZoom();
            double[] normalized = picker.getNormalizedCoordinates(x, y);
            //check to modify.

            if(A!=null&&A.contains(normalized, picker.getNormal())) {
                dragging = A;
            } else if(B!=null&&B.contains(normalized, picker.getNormal())) {
                dragging = B;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = null;
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }


        @Override
        public void mouseDragged(MouseEvent e) {
            if(dragging!=null){
                SlicePicker picker = pickers.get((JPanel)e.getSource());

                double ox = e.getX();
                double oy = e.getY();
                double x = ox/picker.view.getZoom();
                double y = oy/picker.view.getZoom();
                dragging.modify(picker.getNormalizedCoordinates(x,y), picker.getNormal());
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }

        public void addProjectableMesh(DeformableMesh3D mesh) {
            ProjectableMesh pm = new ProjectableMesh(mesh);

            for(SlicePicker pick: pickers.values()){
                pick.addProjectableMesh(pm, mesh);
            }

            meshMap.put(mesh, pm);
        }

        public void clearProjectableMeshes() {
            for(DeformableMesh3D mesh: meshMap.keySet()){
                for(SlicePicker pick: pickers.values()){
                    pick.removeProjectable(meshMap.get(mesh));
                }
            }
            meshMap.clear();
        }

        void addProjectable(Projectable project, Color c){
            for(SlicePicker p: pickers.values()){
                p.addProjectable(project, c);
            }
        }

        void repaint(){
            for(SlicePicker p: pickers.values()){
                p.view.panel.repaint();
            }
        }

        public double[] getNormal() {
            double[] n = Vector3DOps.difference(B.position, A.position);
            Vector3DOps.normalize(n);
            return n;
        }
    }

    static double square(double v){
        return v*v;
    }

}

class CircleModifier implements Projectable{
    double[] position;
    double radius;
    double DRAW_RADIUS = 5;
    public CircleModifier(double[] location, double scale){
        position = location;
        radius = DRAW_RADIUS/scale;
    }

    @Override
    public Shape getProjection(FurrowTransformer transformer) {
        double[] xy = transformer.getPlaneCoordinates(position);

        return new Ellipse2D.Double(xy[0] - DRAW_RADIUS, xy[1] - DRAW_RADIUS, 2*DRAW_RADIUS, 2*DRAW_RADIUS);

    }

    /**
     * Check if the point at loc is within the distance radius along the plane normal.
     *
     * @param loc
     * @param normal
     * @return
     */
    public boolean contains(double[] loc, double[] normal){
        double[] r = Vector3DOps.difference(loc, position);
        double proj = Vector3DOps.dot(normal, r);
        double s = Vector3DOps.dot(r, r);

        return s - proj*proj<radius*radius;

    }

    public void modify(double[] point, double[] normal){
        double[] r = Vector3DOps.difference(point, position);
        double proj = Vector3DOps.dot(r, normal);
        double[] delta = Vector3DOps.add(r, normal, -proj);
        position[0] += delta[0];
        position[1] += delta[1];
        position[2] += delta[2];
    }
}
