package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.gui.IntensityRanges;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * For loading and viewing meshes.
 *
 * Created by msmith on 6/16/16.
 */
public class MeshViewer {
    MeshFrame3D meshFrame;
    public MeshViewer(){

        MeshFrame3D viewer = new MeshFrame3D();
        viewer.showFrame(true);
        viewer.addLights();
        meshFrame = viewer;
    }

    public void addDeformableMesh(DeformableMesh3D mesh){
        meshFrame.addDataObject(mesh.data_object);
    }

    public static void main(String[] args){
        MeshViewer viewer = new MeshViewer();

        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(0);
        mesh.create3DObject();



        viewer.addDeformableMesh(mesh);
        ConfigMesh config = new ConfigMesh(mesh);
        config.buildGui();

        ImagePlus vol = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        VolumeDataObject vdo = new VolumeDataObject(Color.GREEN);
        MeshImageStack stack = new MeshImageStack(vol);
        vdo.setTextureData(stack);
        viewer.meshFrame.addDataObject(vdo);

        viewer.meshFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='v'){

                    VolumeContrastSetter sets = new VolumeContrastSetter(stack);
                    sets.setPreviewBackgroundColor(viewer.meshFrame.getBackgroundColor());
                    sets.setVolumeColor(vdo.color);
                    sets.showDialog(viewer.meshFrame.frame);

                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

    }

    public void buildMenu(){
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("file");
        bar.add(bar);

        meshFrame.frame.setJMenuBar(bar);

    }



}


class VolumeContrastSetter{
    VolumeSamplerPanel preview;
    IntensityRanges range;
    MeshImageStack mis;
    JDialog dialog;
    Color previewBackgroundColor = Color.BLACK;
    Color volumeColor = Color.WHITE;

    public VolumeContrastSetter(MeshImageStack mis){
        this.mis = mis;
    }

    public void setVolumeColor(Color c){
        volumeColor = c;
        if(preview != null) {
            preview.vdo.setColor(c);
        }
    }

    public void setPreviewBackgroundColor(Color c){
        previewBackgroundColor = c;
        if(preview != null){
            preview.mf3d.setBackgroundColor(previewBackgroundColor);
        }
    }

    public void showDialog(Frame parent){
        JDialog dialog = new JDialog(parent, "adjust volume contrast");
        dialog.setModal(true);

        range = new IntensityRanges(mis.getIntensityValues());

        dialog.add(range.getPanel(), BorderLayout.NORTH);
        dialog.add(create3DPreviewer(dialog), BorderLayout.CENTER);
        dialog.add(createButtons(), BorderLayout.SOUTH);
        preview.showSubSample(mis);
        range.addContrastableListener(preview::setMinMaxClipping);

        dialog.pack();
        dialog.setVisible(true);
    }

    public JPanel createButtons(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        JButton accept = new JButton("accept");
        JButton cancel = new JButton("cancel");
        panel.add(Box.createHorizontalGlue());
        panel.add(accept);
        panel.add(cancel);

        return panel;
    }

    public Component create3DPreviewer(Window frame) {

        preview = new VolumeSamplerPanel(frame);
        preview.showSubSample(mis);
        return preview.panel;
    }

    class VolumeSamplerPanel{
        MeshFrame3D mf3d;
        Component panel;
        VolumeDataObject vdo;
        public VolumeSamplerPanel(Window parent){
            mf3d = new MeshFrame3D();

            panel = mf3d.asJPanel(parent);
            System.out.println(previewBackgroundColor);
            mf3d.setBackgroundColor(previewBackgroundColor);
            mf3d.showAxis();
        }

        void showSubSample(MeshImageStack stack){
            vdo = new VolumeDataObject(volumeColor);
            double[] l = stack.scaleToNormalizedLength(new double[]{64, 0, 0});

            vdo.setTextureData(

                    stack.createSubStack(
                            new Box3D(new double[]{0, 0, 0}, l[0], l[0], l[0] )
                    )

            );

            mf3d.addDataObject(vdo);
        }

        void setMinMaxClipping(double min, double max){
            vdo.setMinMaxRange(min/255, max/255);
        }

    }

}

class ConfigMesh{
    DeformableMesh3D target;
    public ConfigMesh(DeformableMesh3D target){
        this.target = target;
    }

    public void buildGui(){
        JFrame frame = new JFrame();
        JPanel content = buildPanel();
        frame.setContentPane(content);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public JPanel buildPanel(){
        JPanel controls = new JPanel();
        controls.setLayout(new GridBagLayout());
        JCheckBox surface = new JCheckBox("surface");
        controls.add(surface);

        JButton button = new JButton(getColorBox(target.getColor()));
        button.addActionListener(evt->{

            final JDialog dialog = new JDialog(null, "Choose a Color", JDialog.ModalityType.APPLICATION_MODAL);
            final JColorChooser chooser = new JColorChooser(target.getColor());
            Container cont = dialog.getContentPane();
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row,BoxLayout.LINE_AXIS));
            JButton cancel = new JButton("cancel");
            row.add(cancel);
            cancel.addActionListener(evt2->{
                dialog.setVisible(false);
            });

            JButton accept = new JButton("accept");
            accept.addActionListener(evt2->{
                        target.setColor(chooser.getColor());
                        button.setIcon(getColorBox(target.getColor()));
                        dialog.setVisible(false);
                    }
            );

            row.add(accept);
            cont.add(row, BorderLayout.SOUTH);

            cont.add(chooser, BorderLayout.CENTER);
            dialog.pack();
            dialog.setVisible(true);

        });

        controls.add(button);

        surface.addActionListener(evt->{
            boolean checked = surface.isSelected();
            target.setShowSurface(checked);
        });
        return controls;

    }

    Icon getColorBox(Color c){

        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D)img.getGraphics();
        g2d.setColor(c);
        g2d.fillRect(0,0,32, 32);
        ImageIcon ico = new ImageIcon(img);
        return ico;
    }


}
