package deformablemesh.gui.meshinitialization;

import deformablemesh.SegmentationController;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Projectable;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.gui.Drawable;
import deformablemesh.gui.Slice3DView;
import deformablemesh.ringdetection.FurrowTransformer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by msmith on 2/5/16.
 */
public class SlicePicker{
    Slice3DView view;
    SegmentationController model;
    double[] pos;
    double[] normal;
    Rectangle2D bounds;
    FurrowTransformer transformer;
    int transpose = 0;
    JLabel label;
    Map<Projectable, Drawable> projectDrawingMapper = new HashMap<>();
    double length = 1;
    ThreeDCursor cursor;
    public SlicePicker(SegmentationController m, double[] normal, double[] center){

        model = m;
        pos = center;
        this.normal = normal;
        transformer = model.createFurrowTransform(pos, normal);

    }

    /**
     * Sets the limits that this picker can choose, ranging from -l/2 to +l/2. This defaults to 1.
     * @param l
     */
    public void setLength(double l){
        length = l;
    }

    /**
     *
     * @param cursor
     */
    public void addCursor(ThreeDCursor cursor){
        view.addDrawable(cursor.getDrawable(transformer));
        this.cursor = cursor;
    }

    public void setSliderValue(int v){
        double f = (0.0001*v - 0.5)*length;

        pos[0] = f*normal[0];
        pos[1] = f*normal[1];
        pos[2] = f*normal[2];
        cursor.toPosition(f, normal);
        transformer = model.createFurrowTransform(pos, normal);
        for(int i = 0; i<transpose; i++){
            transformer.rotatePiOver2();
        }
        view.setSlice(model.createSlice(transformer));
        view.panel.repaint();

    }
    public void rotateView(){
        transpose =(transpose+1)%4;
    }

    public JPanel buildView(){
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        view = new Slice3DView();

        transformer = model.createFurrowTransform(pos, normal);
        for(int i = 0; i<transpose; i++){
            transformer.rotatePiOver2();
        }

        Image img = model.createSlice(transformer);

        view.setSlice(img);
        bounds = new Rectangle2D.Double(0, 0, img.getWidth(view.panel), img.getHeight(view.panel));
        JScrollPane scroll = new JScrollPane(view.panel);
        scroll.setMaximumSize(new Dimension(600, 600));
        container.add(scroll, BorderLayout.CENTER);

        label = new JLabel("Select the center plane, and click on the extreme points.");
        label.setMaximumSize(new Dimension(600, 30));

        container.add(label, BorderLayout.NORTH);
        JSlider slider = new JSlider(0, 10000);
        slider.setValue(5000);
        slider.setOrientation(JSlider.VERTICAL);
        slider.addChangeListener(evt->{

            int v = slider.getValue();
            setSliderValue(v);

        });
        slider.setMaximumSize(new Dimension(30, 600));
        container.add(slider, BorderLayout.EAST);
        return container;
    }

    public void addProjectable(Projectable p){
        Drawable d = g->g.draw(p.getProjection(transformer));
        projectDrawingMapper.put(p, d);
        view.addDrawable(d);
    }

    public void addProjectable(Projectable p, Color c){
        Drawable d = g->{
            g.setColor(c);
            g.draw(p.getProjection(transformer));
        };
        projectDrawingMapper.put(p, d);
        view.addDrawable(d);
    }

    public void removeProjectable(Projectable p){
        if(projectDrawingMapper.containsKey(p)){
            view.removeDrawable(projectDrawingMapper.get(p));
        }
    }

    public void clear(){
        projectDrawingMapper.clear();
        view.clear();
        view.addDrawable(cursor.getDrawable(transformer));
    }



    public void addMouseListener(MouseListener ml){
        view.panel.addMouseListener(ml);
    }

    public double[] getNormalizedCoordinates(double ox, double oy){
        return transformer.getVolumeCoordinates(new double[]{ox, oy});
    }

    public boolean inBounds(double x, double y){
        return bounds.contains(x,y);
    }


    public void addMouseMotionListener(MouseMotionListener initializer) {
        view.panel.addMouseMotionListener(initializer);
    }

    public void setLabel(String label){
        this.label.setText(label);
    }

    public double[] getNormal() {
        return normal;
    }

    public double getScale() {
        return transformer.scale;
    }

    public void addProjectableMesh(ProjectableMesh pm, DeformableMesh3D mesh) {
        Drawable d = g->{
            if(mesh.isSelected()){
                g.setColor(Color.GREEN);
            } else{
                g.setColor(mesh.getColor());
            }
            g.draw(pm.getProjection(transformer));
        };
        projectDrawingMapper.put(pm, d);
        view.addDrawable(d);
    }
}
