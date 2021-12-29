package deformablemesh.gui.meshinitialization;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.geometry.DeformableMesh3D;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This will go through, step by reversible step and allow a user to initialize
 * a 3D mesh.
 *
 * Created by melkor on 11/18/15.
 */
public class RectangleMeshInitializingDialog extends JDialog {
    int state = 0;
    Map<Integer, String> cards = new TreeMap<>();
    CardLayout layout;
    JPanel panel;
    SegmentationController model;

    Initializer xyInitializer;
    Initializer xzInitializer;

    public RectangleMeshInitializingDialog(JFrame owner, SegmentationController model){
        super(owner, true);
        this.model = model;
    }

    public void start(){
        panel = new JPanel();
        layout = new CardLayout();
        panel.setLayout(layout);

        createHorizontalMidPlaneSelectionPanel();
        createVerticalMidplaneSelectionPanel();

        setCard(1);
        setContentPane(panel);
        pack();

        setVisible(true);

    }

    public void finish(){
        List<double[]> xyPoints = xyInitializer.getPoints();
        List<double[]> xzPoints = xzInitializer.getPoints();
        xyPoints.addAll(xzPoints);
        double[] maxes = new double[3];
        double[] mins = new double[3];
        for(double[] pt: xyPoints){
            for(int i = 0; i<3; i++){
                if(maxes[i]<pt[i]){
                    maxes[i]=pt[i];
                } else if(mins[i]>pt[i]){
                    mins[i] = pt[i];
                }

            }
        }
        double lx = maxes[0] - mins[0];
        double ly = maxes[1] - mins[1];
        double lz = maxes[2] - mins[2];

        double cx = 0.5*(maxes[0] + mins[0]);
        double cy = 0.5*(maxes[1] + mins[1]);
        double cz = 0.5*(maxes[2] + mins[2]);

        double shortest = lx > ly ?
                ly > lz ? lz : ly
                :
                lx > lz ? lx : lz;
        DeformableMesh3D mesh = DeformableMesh3DTools.createRectangleMesh(lx, ly, lz, shortest/(model.getDivisions()+1));
        mesh.translate(new double[]{cx, cy, cz});
        mesh.create3DObject();
        model.addMesh(mesh);
    }

    private void createHorizontalMidPlaneSelectionPanel(){
        state++;
        new JPanel();

        double[] hPos = {0,0,0};
        double[] zDir = {0,0,1};
        SlicePicker xyPicker  = new SlicePicker(model.getMeshImageStack(), zDir, hPos );
        JPanel horizontal = xyPicker.buildView();
        xyInitializer = new Initializer(xyPicker);

        JPanel row = new JPanel();
        horizontal.add(row, BorderLayout.SOUTH);
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        JButton next = new JButton("next");
        next.addActionListener((evt)->{
            nextCard();
        });
        JButton previous = new JButton("cancel");
        previous.addActionListener((evt)->{
            setVisible(false);
            dispose();
        });
        row.add(previous);
        row.add(next);
        horizontal.add(row, BorderLayout.SOUTH);
        String name = "horizontal";
        layout.addLayoutComponent(horizontal, name);
        panel.add(horizontal);
        cards.put(state, name);
    }

    private void createVerticalMidplaneSelectionPanel(){
        state++;

        double[] vPos = new double[]{0,0,0};
        double[] yDir = new double[]{0, 1, 0};

        SlicePicker xzPicker = new SlicePicker(model.getMeshImageStack(), yDir, vPos );
        JPanel vertical = xzPicker.buildView();

        xzInitializer = new Initializer(xzPicker);

        JPanel row = new JPanel();
        vertical.add(row, BorderLayout.SOUTH);
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        JButton next = new JButton("finished");
        next.addActionListener((evt)->{
            setVisible(false);
            finish();
            dispose();
        });


        JButton previous = new JButton("back");

        previous.addActionListener((evt)->{
            previousCard();
        });
        row.add(previous);
        row.add(next);

        vertical.add(row, BorderLayout.SOUTH);
        String name = "vertical";
        layout.addLayoutComponent(vertical, name);
        panel.add(vertical);
        cards.put(state, name);

    }



    public void setCard(int c){
        state = c;
        String name = cards.get(c);

        layout.show(panel, name);

    }

    public void previousCard(){
        state = state-1;
        layout.previous(panel);
    }

    public void nextCard(){
        state = state+1;
        layout.next(panel);
    }


    public static void main(String[] args){
        SegmentationModel model = new SegmentationModel();
        String o = new File("practice/series6-registered-scaled-z.tif").getAbsolutePath();
        model.setOriginalPlus(new ImagePlus(o));

        EventQueue.invokeLater(()->{
            JFrame frame = new JFrame("test");
            JButton d = new JButton("dialog");
            d.addActionListener((evt)->new RectangleMeshInitializingDialog(frame, new SegmentationController(model)).start());
            frame.add(d);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });
    }
    class Initializer implements MouseListener {
        double RADIUS = 3;

        final SlicePicker picker;
        List<double[]> points=new ArrayList<double[]>();

        public Initializer(SlicePicker picker){
            this.picker = picker;
            picker.addMouseListener(this);
        }


        @Override
        public void mouseClicked(MouseEvent e) {
            double ox = e.getX();
            double oy = e.getY();
            double x = ox/picker.view.getZoom();
            double y = oy/picker.view.getZoom();

            if(picker.inBounds(x,y)){
                picker.addProjectable(t->new Ellipse2D.Double(x - RADIUS, y-RADIUS, 2*RADIUS, 2*RADIUS));
                points.add(picker.getNormalizedCoordinates(x, y));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
        public List<double[]> getPoints(){
            return points;
        }
    }

}

