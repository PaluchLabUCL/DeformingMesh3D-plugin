package deformablemesh.meshview;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.RayCastMesh;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

/**
 * For loading and viewing meshes.
 *
 * Created by msmith on 6/16/16.
 */
public class MeshViewer {


    public static void main(String[] args){
        MeshFrame3D viewer = new MeshFrame3D();
        viewer.showFrame(true);

        DeformableMesh3D mesh = RayCastMesh.sphereRayCastMesh(0);

        mesh.create3DObject();


        viewer.addLights();



        viewer.addDataObject(mesh.data_object);
        ConfigMesh config = new ConfigMesh(mesh);
        config.buildGui();


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
