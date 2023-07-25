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
package deformablemesh.meshview;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.io.MeshReader;
import deformablemesh.io.MeshWriter;
import deformablemesh.track.Track;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.Material;
import org.scijava.vecmath.Color3f;

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
    int frame = 0;
    List<Track> tracks = new ArrayList<>();
    ConfigMesh config;;
    DeformableMesh3D selectedMesh;
    public MeshViewer(){

        MeshFrame3D viewer = new MeshFrame3D();
        viewer.showFrame(true);
        viewer.addLights();
        meshFrame = viewer;
        config = new ConfigMesh(meshFrame);
        config.buildGui();
    }



    public void addMeshTrack(Track track){
        tracks.add(track);
        if(track.containsKey(frame)){
            DeformableMesh3D mesh = track.getMesh(frame);
            mesh.setSelected(false);
            mesh.setShowSurface(true);
            mesh.create3DObject();
            //tracks are not being managed through the segmentation controller.
            //so the wirecolor needs to be set!?
            mesh.data_object.setWireColor(track.getColor());
            meshFrame.addDataObject(mesh.data_object);
            config.addMesh(mesh);
        }

    }

    public static void main(String[] args) throws Exception{
        MeshViewer viewer = new MeshViewer();



        List<Track> tracks = MeshReader.loadMeshes(Paths.get(args[0]).toFile());
        tracks.forEach(viewer::addMeshTrack);
        viewer.meshFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
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


class ConfigMesh{
    List<DeformableMesh3D> meshes = new ArrayList<>();
    JPanel meshlist;
    MeshFrame3D viewer;
    public ConfigMesh(MeshFrame3D viewer){
        this.viewer = viewer;
    }
    float clamp(float v){
        if(v<0) return 0;
        if(v>1) return 1;
        return v;
    }
    float[] adjust(float[] c, float v){
        if(v<0) return new float[]{0, 0, 0};
        if(v<1){
            return new float[]{c[0]*v, c[1]*v, c[2]*v};
        }
        return new float[]{
                clamp(c[0] + (v-1)),
                clamp(c[1] + (v-1)),
                clamp(c[2] + (v-1))
        };
    }

    public JPanel buildAppearancePanel(){
        JSlider diffuse = new JSlider(JSlider.VERTICAL);
        JSlider emmisive = new JSlider(JSlider.VERTICAL);
        JSlider ambient = new JSlider(JSlider.VERTICAL);
        JSlider specular = new JSlider(JSlider.VERTICAL);
        JButton set = new JButton("apply");
        JTextField shinyness = new JTextField("1.0");
        set.addActionListener(evt->{
            float d = diffuse.getValue()/50.f;
            float e = emmisive.getValue()/50.f;
            float a = ambient.getValue()/50.f;
            float s = specular.getValue()/50.f;
            float shine = Float.parseFloat(shinyness.getText());
            System.out.println(d + ", " + e + ", " + a + ", " + s);
            for(DeformableMesh3D mesh: meshes){
                Appearance app = new Appearance();
                float[] rgb = mesh.getColor().getRGBComponents(new float[4]);

                Color3f ambientColor = new Color3f(adjust(rgb, a));
                Color3f emmisiveColor = new Color3f(adjust(rgb, e));
                Color3f diffuseColor = new Color3f(adjust(rgb, d));

                Color3f specularColor = new Color3f(adjust(rgb, s));
                Material mat = new Material(
                        ambientColor,
                        emmisiveColor,
                        diffuseColor,
                        specularColor,
                        shine);
                app.setMaterial(mat);
                mesh.data_object.setSurfaceAppearance(app);
            }


        });

        GridBagLayout layout = new GridBagLayout();
        JPanel sliders = new JPanel(layout);
        GridBagConstraints con = new GridBagConstraints();
        con.gridx = 0;
        con.gridy = 0;

        sliders.add(new JLabel("D"), con);
        con.gridx++;
        sliders.add(new JLabel("E"), con);
        con.gridx++;
        sliders.add(new JLabel("A"), con);
        con.gridx++;
        sliders.add(new JLabel("S"), con);

        con.gridy++;
        con.gridheight = 3;
        con.gridx = 0;
        sliders.add(diffuse, con);
        con.gridx++;
        sliders.add(emmisive, con);
        con.gridx++;
        sliders.add(ambient, con);
        con.gridx++;
        sliders.add(specular, con);

        con.gridx = 0;
        con.gridy += 3;
        con.gridheight = 1;
        con.gridwidth = 3;

        sliders.add(shinyness, con);
        con.gridx+=3;
        sliders.add(set, con);
        return sliders;
    }

    public void buildGui(){
        JFrame frame = new JFrame();
        JPanel content = buildPanel();
        frame.setContentPane(content);
        frame.setSize(640, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    public void addMesh(DeformableMesh3D mesh){
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));

        JCheckBox surface = new JCheckBox("surface");
        surface.setSelected(mesh.isShowSurface());
        controls.add(surface);

        controls.add(buildColorButton(mesh));
        meshlist.add(controls);
        meshlist.invalidate();
        meshes.add(mesh);
    }

    JButton buildColorButton(DeformableMesh3D target){
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

        return button;
    }
    public JPanel buildPanel(){
        JPanel main = new JPanel(new BorderLayout());
        meshlist = new JPanel();
        meshlist.setLayout(new BoxLayout(meshlist, BoxLayout.PAGE_AXIS));
        main.add(new JScrollPane(meshlist), BorderLayout.EAST);
        main.add(buildAppearancePanel(), BorderLayout.WEST);
        JSlider directional = new JSlider();
        main.add(directional, BorderLayout.NORTH);

        directional.addChangeListener(evt->{

            float f = directional.getValue()/100.f;
            viewer.setDirectionalBrightness(f);
        });
        JSlider ambient = new JSlider();
        main.add(ambient, BorderLayout.SOUTH);
        ambient.addChangeListener(evt->{
            float f = ambient.getValue()/100.f;
            viewer.setAmbientBrightness(f);
        });
        return main;
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
