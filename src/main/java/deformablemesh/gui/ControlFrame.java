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
package deformablemesh.gui;

import deformablemesh.BoundingBoxTransformer;
import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.experimental.RemotePrediction;
import deformablemesh.externalenergies.ImageEnergyType;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.gui.meshinitialization.CircularMeshInitializationDialog;
import deformablemesh.gui.meshinitialization.FurrowInitializer;
import deformablemesh.io.ImportType;
import deformablemesh.io.MeshReader;
import deformablemesh.io.TrackMateAdapter;
import deformablemesh.meshview.HotKeyDelegate;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.track.MeshTrackManager;
import deformablemesh.track.Track;
import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.FileInfoVirtualStack;
import ij.plugin.filter.PlugInFilter;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * GUI commands -&gt; Delegate to segmentation controller. GUI based actions should be funneled through this class
 * for enabling/disabling the UI.
 *
 *
 * User: msmith
 * Date: 7/16/13
 * Time: 10:35 AM
 */
public class ControlFrame implements ReadyObserver, FrameListener {
    SegmentationController segmentationController;
    Map<String, ParameterControl> parameterControls = new HashMap<>();
    boolean ready = true;
    ArrayList<JComponent> buttons = new ArrayList<>();
    JButton deformButton;
    FrameIndicator frameIndicator = new FrameIndicator();
    private JFrame frame;
    JTabbedPane tabbedPane;
    JPanel mainContent;
    JMenuItem undo, redo;
    public static Component instance;
    Component mainDisplay;

    SwingJSTerm terminal;
    Dimension pm = new Dimension(29, 29);
    JLabel message = new JLabel("");
    HotKeyDelegate mf3DInterface;
    RingController ringController;

    static String showPlane = "show plane";
    static String hidePlane = "hide plane";
    public ControlFrame( SegmentationController model){
        this.segmentationController = model;
        segmentationController.addUndoStateListener(this::updateUndoRedo);
        try{
            terminal = new SwingJSTerm(model);
        } catch (Throwable e){
            System.out.println("Javascript not configured properly");
            //can be a couple errors.
        }

        ringController = model.getRingController();

    }
    public void showFrame(){
        frame = new JFrame("DM3D: control panel");
        frame.setIconImage(GuiTools.getIcon());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

        mainContent = createContentPane();
        addTabbedPanel(mainContent, "main");

        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        contentPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        frame.setContentPane(contentPanel);
        frame.setJMenuBar(createMenu(frame));


        frame.pack();
        frame.setVisible(true);
        instance=frame;
        segmentationController.addFrameListener(this);
    }
    public JFrame getFrame(){
        return frame;
    }
    public void addMeshFrame3D( MeshFrame3D frame){
        mf3DInterface = new HotKeyDelegate(frame, segmentationController, this);
    }
    private JPanel createStatusPanel(){
        JPanel status = new JPanel();
        status.setLayout(new BoxLayout(status, BoxLayout.PAGE_AXIS));

        JPanel colorStatusRow = new JPanel();
        colorStatusRow.add(new JLabel("selected mesh: " ));
        JLabel selectedColorLabel = new JLabel("");
        colorStatusRow.add(selectedColorLabel);
        status.add(colorStatusRow);
        status.add(message);
        segmentationController.addMeshListener(i->{
            selectedColorLabel.setText(segmentationController.getSelectedMeshName());
        });
        return status;
    }

    /**
     *    | indicator | |deform| |clear|
     *    |           | |init|   |raycast|
     *
     *
     *
     * @return
     */
    private JPanel createButtonPanel(){

        JPanel imageControls = buildImageStatus();
        JPanel meshControls = buildMeshControls();
        JPanel volumeDisplay = buildVolumeControls();
        JPanel energyDisplay = buildDeformControls();
        JPanel furrowDisplay = buildFurrowControls();
        //JPanel radiusSlider = buildRadiusValueSelector();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints bcon = new GridBagConstraints();
        bcon.fill=GridBagConstraints.HORIZONTAL;
        bcon.gridwidth = 2;
        buttonPanel.add(imageControls, bcon);
        bcon.gridy = 1;
        buttonPanel.add(volumeDisplay, bcon);

        bcon.gridwidth = 2;
        bcon.gridx = 0;
        bcon.gridy = 2;
        buttonPanel.add(meshControls, bcon);

        JPanel subPanel = new JPanel(new GridBagLayout());
        subPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
        GridBagConstraints sc = new GridBagConstraints();
        sc.fill = GridBagConstraints.HORIZONTAL;
        subPanel.add(energyDisplay, sc);

        sc.gridx = 1;
        sc.gridheight=2;

        //subPanel.add(radiusSlider, sc);
        sc.gridx = 0;
        sc.gridy = 1;
        sc.gridheight = 1;
        subPanel.add(furrowDisplay, sc);

        bcon.gridx = 0;
        bcon.gridy = 3;
        buttonPanel.add(subPanel, bcon);

        buttonPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        return buttonPanel;
    }

    private JPanel buildImageStatus(){
        JPanel topBottom = new JPanel(new GridBagLayout());
        GridBagConstraints bcon = new GridBagConstraints();
        bcon.gridwidth = 3;
        topBottom.add(frameIndicator.imageName, bcon);

        bcon.gridwidth = 1;
        bcon.gridx = 0;
        bcon.gridy = 1;
        topBottom.add(new JLabel("frame: "), bcon);
        bcon.gridx = 1;
        topBottom.add(frameIndicator.getTextField(), bcon);
        bcon.gridx = 2;
        topBottom.add(frameIndicator.getMaxLabel(), bcon);
        bcon.gridy = 2;
        bcon.gridx = 0;
        topBottom.add( createButtonPrevious(), bcon);
        bcon.gridx = 1;
        topBottom.add( createButtonNext(), bcon);
        bcon.gridx = 2;
        topBottom.add(frameIndicator.channelLabel, bcon);
        topBottom.setOpaque(true);

        return topBottom;
    }

    private JPanel buildMeshControls(){
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints bcon = new GridBagConstraints();
        buttonPanel.add( createButtonInitializeMesh2(), bcon );
        bcon.gridx = 1;
        buttonPanel.add(createButtonDeform(), bcon );
        bcon.gridx = 2;
        buttonPanel.add( createButtonClearMesh(), bcon );
        bcon.gridy = 1;
        bcon.gridx = 0;
        bcon.gridwidth = 3;
        JPanel remButtonUnits = createRemeshPanel();
        buttonPanel.add( remButtonUnits, bcon);
        buttonPanel.setOpaque(true);
        return buttonPanel;
    }

    private JPanel buildVolumeControls(){
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints bcon = new GridBagConstraints();

        buttonPanel.add( createButtonShowVolume(), bcon);
        bcon.gridx = 1;
        buttonPanel.add( createButtonAdjustVolumeContrast(), bcon);
        bcon.gridy = 1;
        bcon.gridx = 0;
        buttonPanel.add( createButtonAdjustMinimum(), bcon);
        bcon.gridx = 1;
        buttonPanel.add( createButtonAdjustMaximum(), bcon);
        return buttonPanel;
    }

    private JPanel buildFurrowControls(){
        JPanel components = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        JButton showFurrowButton = new JButton(showPlane);
        buttons.add(showFurrowButton);

        JCheckBox box = new JCheckBox("textured");


        box.addActionListener(evt ->{
            if(ringController.furrowShowing){
                ringController.showFurrow(box.isSelected());
            }
        });

        showFurrowButton.addActionListener(evt->{
            if(showFurrowButton.getText().equals(showPlane)){
                showFurrowButton.setText(hidePlane);
                ringController.showFurrow(box.isSelected());
            } else{
                showFurrowButton.setText(showPlane);
                ringController.hideFurrow();
            }
        });
        JButton initialize = new JButton("init");
        initialize.setToolTipText("initlialize furrow location");
        buttons.add(initialize);
        initialize.addActionListener(
                (event)->new FurrowInitializer(frame, segmentationController, ()->{}).start()
        );
        JButton center = new JButton("center");
        center.setToolTipText("move plane to center of image or selected mesh");
        buttons.add(center);
        center.addActionListener(evt->{
            DeformableMesh3D mesh = segmentationController.getSelectedMesh();
            if(mesh == null){
                ringController.setFurrow(ringController.getInputNormal(), new double[]{0, 0, 0});
            } else{
                double[] loc = DeformableMesh3DTools.centerAndRadius(mesh.nodes);
                ringController.setFurrow(ringController.getInputNormal(), loc);
            }
        });

        JButton split = new JButton("split mesh");
        split.addActionListener(evt->segmentationController.splitMesh());
        buttons.add(split);
        JButton nodeSelect = new JButton("select");
        nodeSelect.setToolTipText("select nodes for manual editing");
        buttons.add(nodeSelect);

        JButton finish = new JButton("finish");
        JButton cancel = new JButton("cancel");
        JButton sculpt = new JButton("sculpt");
        nodeSelect.addActionListener(evt->{
            ringController.selectNodes(evt);
            if(ringController.modifyingMesh()){
                setReady(false);

                //buttons.forEach(b->b.setEnabled(false));
                EventQueue.invokeLater(() -> {
                    sculpt.setEnabled(true);
                    finish.setEnabled(true);
                    cancel.setEnabled(true);
                });


            }

        });
        buttons.add(sculpt);
        buttons.add(cancel);
        buttons.add(finish);
        sculpt.addActionListener( evt -> {
            if(ringController.modifyingMesh()) {
                ringController.sculptClicked(evt);
                nodeSelect.setEnabled(true);
                sculpt.setEnabled(false);
            }
        });
        finish.addActionListener(evt->{
            ringController.finishedClicked(evt);
            finished();
        });

        cancel.addActionListener(evt->{
            ringController.cancel();
            finished();
        });
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel showSplit = new JPanel(new GridLayout(2, 2));
        showSplit.add(showFurrowButton);
        showSplit.add(split);
        showSplit.add(box);
        components.add(showSplit, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        components.add( ringController.getHistControlsPanel() , gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridheight = 3;
        components.add( ringController.createFurrowInput(), gbc);

        JPanel keys = new JPanel(new GridLayout(3, 2));
        keys.add(center);
        keys.add(initialize);
        keys.add(nodeSelect);
        keys.add(sculpt);
        keys.add(cancel);
        keys.add(finish);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        components.add(keys, gbc);

        return components;
    }

    private JPanel buildDeformControls(){
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(true);
        GridBagConstraints bcon = new GridBagConstraints();
        bcon.fill = GridBagConstraints.HORIZONTAL;
        bcon.gridwidth = 3;

        buttonPanel.add( createEnergySelector(), bcon);
        bcon.gridy = 1;
        bcon.gridwidth = 2;
        buttonPanel.add( createRigidBoundaryCheckbox(), bcon );
        bcon.gridx = 2;
        bcon.gridwidth = 1;
        buttonPanel.add( createButtonShowForces(), bcon);
        bcon.gridx = 3;
        buttonPanel.add( createButtonClearTransients(), bcon);
        return buttonPanel;
    }

    private JPanel buildParameterPane(){
        JPanel inputs = new JPanel();
        inputs.setLayout(new GridLayout(8,1,0,0));
        SetValue set_gamma = d -> segmentationController.setGamma(d);
        inputs.add(GuiTools.createInputField("gamma", createSavingValue(set_gamma), segmentationController.getGamma(), this));

        SetValue set_alpha = d -> segmentationController.setAlpha(d);
        inputs.add(GuiTools.createInputField("alpha", createSavingValue(set_alpha), segmentationController.getAlpha(), this));

        SetValue set_pressure = d -> segmentationController.setPressure(d);
        inputs.add(GuiTools.createInputField("pressure", createSavingValue(set_pressure), segmentationController.getPressure(), this));

        SetValue stericNeighbors = d -> segmentationController.setStericNeighborWeight(d);
        inputs.add(
                GuiTools.createInputField(
                        "steric neighbors",
                        createSavingValue(stericNeighbors) ,
                        segmentationController.getStericNeighborWeight(),
                        this
                )
        );

        SetValue set_image_weight = d -> segmentationController.setWeight(d);
        inputs.add(GuiTools.createInputField("image weight", createSavingValue(set_image_weight), segmentationController.getImageWeight(), this));

        SetValue set_divisions = d -> segmentationController.setDivisions((int)d);
        inputs.add(GuiTools.createComboControl("divisions", createSavingValue(set_divisions), segmentationController.getDivisions()));

        SetValue setBeta = d -> segmentationController.setBeta(d);
        inputs.add(GuiTools.createInputField("beta", createSavingValue(setBeta), segmentationController.getBeta(), this));

        for(Component comp: inputs.getComponents()){
            if(comp instanceof ParameterControl){
                ParameterControl pc = (ParameterControl)comp;
                parameterControls.put(pc.name, pc);
            }
        }
        return inputs;
    }
    private JPanel createContentPane(){
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        JPanel rightSide = new JPanel();


        BoxLayout layout = new BoxLayout(rightSide, BoxLayout.PAGE_AXIS);
        rightSide.setLayout(layout);

        rightSide.add(createButtonPanel());

        JPanel parameterPane = buildParameterPane();
        rightSide.add(new JScrollPane(parameterPane));



        content.add(rightSide, BorderLayout.EAST);
        mainDisplay = new JScrollPane(ringController.getSliceView().panel);
        content.add( mainDisplay, BorderLayout.CENTER);
        return content;
    }

    private JButton createButtonAdjustVolumeContrast() {

        JButton vc = new JButton("vol-contrast");
        vc.addActionListener(evt->{
            segmentationController.showVolumeClippingDialog();
        });
        buttons.add(vc);
        return vc;
    }

    private JCheckBox createRigidBoundaryCheckbox() {
        JCheckBox check = new JCheckBox("rigid boundaries");
        check.addActionListener(evt->{
            segmentationController.setRigidBoundaries(check.isSelected());
        });
        buttons.add(check);
        check.setOpaque(false);
        return check;
    }

    public void updateDisplayedParameters(){
        updateValue("gamma", segmentationController.getGamma());
        updateValue("alpha", segmentationController.getAlpha());
        updateValue("pressure", segmentationController.getPressure());
        updateValue("image weight", segmentationController.getImageWeight());
        updateValue("divisions", segmentationController.getDivisions());
        updateValue("beta", segmentationController.getBeta());
    }

    public void updateValue(String name, double value){
        parameterControls.get(name).updateValue(value);
    }

    SetValue createSavingValue(SetValue value){
        SetValue saving = d->{
            value.setValue(d);
            try{
                PropertySaver.saveProperties(segmentationController);
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
        return saving;
    }
    private void createButtonMeasureVolume(JPanel buttonPanel) {
        final JButton measureVolume = new JButton("measure volume");
        measureVolume.setToolTipText("Measures volume vs time");
        buttons.add(measureVolume);
        buttonPanel.add(measureVolume);
        measureVolume.addActionListener(evt-> segmentationController.measureVolume());

    }

    public JButton createButtonClearMesh(){
        final JButton clear_mesh = new JButton("clear mesh");
        buttons.add(clear_mesh);
        clear_mesh.addActionListener(actionEvent -> {
            setReady(false);
            segmentationController.clearSelectedMesh();
            finished();
        });
        return clear_mesh;
    }

    public JButton createButtonPrevious(){
        JButton previous = new JButton("previous");
        buttons.add(previous);
        previous.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setReady(false);
                segmentationController.previousFrame();
                finished();
            }
        });
        return previous;
    }

    public JButton createButtonNext(){
        JButton next = new JButton("next");
        buttons.add(next);
        next.addActionListener(evt -> {
            nextFrameAction();
        });
        return next;
    }
    public void initializeMeshAction(){
        if(!segmentationController.hasOriginalPlus()){
            return;
        }
        CircularMeshInitializationDialog dialog = new CircularMeshInitializationDialog(segmentationController);
        dialog.start();
        tabbedPane.add("Initializer", dialog.getContent());
        int i = tabbedPane.indexOfComponent(dialog.getContent());

        JPanel closableTab = GuiTools.getClosableTabComponent("initializer", evt->{
            dialog.afterClosing();
        });
        tabbedPane.setTabComponentAt(i, closableTab);

        tabbedPane.setSelectedComponent(dialog.getContent());
        dialog.setCloseCallback( () ->{
            tabbedPane.remove(dialog.getContent());
        });
    }
    public JButton createButtonInitializeMesh2(){
        final JButton prompt_mesh = new JButton("initialize mesh...");
        buttons.add(prompt_mesh);

        prompt_mesh.addActionListener((evt)->{
            initializeMeshAction();
        });
        return prompt_mesh;
    }

    public void createButtonSnapshot(JPanel buttonPanel){
        JButton snapshot = new JButton("snapshot");
        buttons.add(snapshot);
        buttonPanel.add(snapshot);
        snapshot.addActionListener((evt)->{
            setReady(false);
            segmentationController.takeSnapShot();
            finished();
        });
    }

    public JPanel createRemeshPanel(){
        JButton raycast = createButtonRemesh();
        JButton action = new JButton("connection remesh");
        buttons.add(action);

        JLabel scaledMinUnits = new JLabel("no units");
        JLabel scaledMaxUnits = new JLabel("no units");



        ParameterControl minValue = GuiTools.createInputField(
                "min length",
                v->{
                    MeshImageStack stack = segmentationController.getMeshImageStack();
                    if(stack != null){
                        double scale = stack.SCALE;
                        //TODO
                        String unit = stack.getUnits();

                        scaledMinUnits.setText(String.format(Locale.US, "%3.3f %s", scale*v, unit ));
                    } else{
                        scaledMinUnits.setText("");
                    }
                    segmentationController.setMinConnectionLength(v);
                },
                segmentationController.getMinConnectionLength(),
                this
        );

        ParameterControl maxValue = GuiTools.createInputField(
                "max length",
                v->{
                    MeshImageStack stack = segmentationController.getMeshImageStack();
                    if(stack != null){
                        double scale = stack.SCALE;
                        String unit = stack.getUnits();
                        scaledMaxUnits.setText(String.format(Locale.US, "%3.3f %s", scale*v, unit ));
                    } else{
                        scaledMaxUnits.setText("");
                    }
                    segmentationController.setMaxConnectionLength(v);
                },
                segmentationController.getMaxConnectionLength(),
                this
        );

        Runnable updateScaledUnitsText = ()->{
            double scale;
            MeshImageStack stack = segmentationController.getMeshImageStack();
            if(stack != null){
                scale = stack.SCALE;
                String unit = stack.getUnits();
                scaledMaxUnits.setText(String.format(Locale.US, "%3.3f %s",  scale*maxValue.getValue(), unit ));
                scaledMinUnits.setText(String.format(Locale.US, "%3.3f %s",  scale*minValue.getValue(), unit ));
            } else{
                scaledMinUnits.setText("");
                scaledMaxUnits.setText("");
            }
        };

        segmentationController.addFrameListener(frame->{
            updateScaledUnitsText.run();
        });


        JPanel host = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        host.add(action, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        host.add(raycast, gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 1;
        host.add(minValue, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        host.add(scaledMinUnits, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        host.add(maxValue, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        host.add(scaledMaxUnits, gbc);

        action.addActionListener(evt->{
            boolean reMeshAll = ( evt.getModifiers() & ActionEvent.CTRL_MASK ) > 0;
            connectionRemesh(reMeshAll);
        });
        host.setOpaque(false);
        return host;

    }

    public void connectionRemesh(boolean reMeshAll){
        double mn = segmentationController.getMinConnectionLength();
        double mx = segmentationController.getMaxConnectionLength();
        if(mn > mx ){
            throw new RuntimeException("minimum should be less than max");
        }else if( mx <= 0 ){
            throw new RuntimeException("maximum cannot be less than or equal to zero");
        }
        setReady(false);
        if(reMeshAll){
            segmentationController.reMeshConnectionsAllMeshes(mn, mx);
        }else{
            segmentationController.reMeshConnections(mn, mx);
        }
        finished();
    }

    public JButton createButtonRemesh(){
        JButton button = new JButton("raycast remesh");
        buttons.add(button);
        button.addActionListener((evt)->{
            setReady(false);
            segmentationController.reMesh();
            finished();
        });
        return button;
    }

    public JButton createButtonClearTransients(){
        JButton clearTransients = new JButton("X");
        clearTransients.setToolTipText("Clear all force vectors.");
        buttons.add(clearTransients);
        clearTransients.addActionListener((evt)->{
            setReady(false);
            segmentationController.clearTransientObjects();;
            finished();
        });
        return clearTransients;
    }

    public JButton createButtonShowVolume(){
        final String showing = "show volume";
        final String hide = "hide volume";
        JButton show_volume = new JButton(showing);
        buttons.add(show_volume);
        show_volume.addActionListener(e -> {
            if(showing.equals(show_volume.getText())){
                show_volume.setText(hide);
                showVolumeAction();
            } else{
                show_volume.setText(showing);
                hideVolumeAction();
            }
        });
        return show_volume;
    }

    public JButton createButtonShowForces(){
        JButton showForces = new JButton("show forces");
        buttons.add(showForces);
        showForces.addActionListener(e->{
            setReady(false);
            segmentationController.showForces();
            finished();
        });
        return showForces;
    }
    public void createButtonShowMeshVolume(JPanel buttonPanel){

        JButton showMeshVolume = new JButton("show mesh volume");
        buttons.add(showMeshVolume);
        buttonPanel.add(showMeshVolume);
        showMeshVolume.addActionListener(e->{
            setReady(false);
            segmentationController.showBinaryBlob();
            finished();
        });
    }


    public JPanel createEnergySelector(){

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        JLabel l = new JLabel("external energy");
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(l);

        JComboBox<ImageEnergyType> energySelector = new JComboBox<>(ImageEnergyType.values());
        energySelector.addActionListener(evt->{
            segmentationController.setImageEnergyType((ImageEnergyType)energySelector.getSelectedItem());
        });
        segmentationController.setImageEnergyType(ImageEnergyType.values()[0]);

        buttons.add(energySelector);
        panel.add(energySelector);
        return panel;
    }

    public JPanel createButtonAdjustMinimum(){
        Insets margin = new Insets(5, 0, 5, 0);
        JLabel m = new JLabel("min.");

        JButton decrease_min = new JButton("-");
        decrease_min.setPreferredSize(pm);
        decrease_min.setMargin(margin);
        buttons.add(decrease_min);
        decrease_min.addActionListener(e -> {
            setReady(false);
            segmentationController.changeVolumeClipping(-1,0);
            finished();
        });

        JButton increase_min = new JButton("+");
        increase_min.setPreferredSize(pm);
        increase_min.setMargin(margin);
        buttons.add(increase_min);
        increase_min.addActionListener(e -> {
            setReady(false);
            segmentationController.changeVolumeClipping(1,0);
            finished();
        });

        JPanel sub = new JPanel();
        sub.add(Box.createHorizontalStrut(5));
        sub.setLayout(new BoxLayout(sub, BoxLayout.LINE_AXIS));
        sub.add(m);
        sub.add(Box.createHorizontalGlue());
        sub.add(decrease_min);
        sub.add(increase_min);
        sub.add(Box.createHorizontalStrut(5));
        return sub;
    }

    public JPanel createButtonAdjustMaximum(){
        JLabel m = new JLabel("max.");
        Insets margin = new Insets(5, 0, 5, 0);
        JButton decrease_max = new JButton("-");
        decrease_max.setPreferredSize(pm);
        decrease_max.setMargin(margin);
        buttons.add(decrease_max);
        decrease_max.addActionListener(e -> {
            setReady(false);
            segmentationController.changeVolumeClipping(0,-1);
            finished();
        });

        JButton increase_max = new JButton("+");
        increase_max.setPreferredSize(pm);
        increase_max.setMargin(margin);
        buttons.add(increase_max);
        increase_max.addActionListener(e -> {
            setReady(false);
            segmentationController.changeVolumeClipping(0,1);
            finished();
        });

        JPanel sub = new JPanel();
        sub.setLayout(new BoxLayout(sub, BoxLayout.LINE_AXIS));
        sub.add(Box.createHorizontalStrut(5));
        sub.add(m);
        sub.add(Box.createHorizontalGlue());
        sub.add(decrease_max);
        sub.add(increase_max);
        sub.add(Box.createHorizontalStrut(5));
        return sub;
    }

    public void deformAction(boolean deformAll){
        if(ready){
            setReady(false);
            deformButton.setText("stop!");
            EventQueue.invokeLater(() -> deformButton.setEnabled(true));
            if(deformAll){
                segmentationController.deformAllMeshes();
            } else{
                segmentationController.deformMesh();
            }


        } else{
            deformButton.setEnabled(false);
            deformButton.setText("stopping");
            segmentationController.stopRunning();
            segmentationController.submit(() -> deformButton.setText("deform"));
            finished();
        }
    }

    public JButton createButtonDeform(){
        deformButton = new JButton("deform");
        buttons.add(deformButton);
        deformButton.addActionListener(actionEvent -> {
            deformAction( (actionEvent.getModifiers() & ActionEvent.CTRL_MASK) > 0);
        });
        return deformButton;
    }
    JMenuBar createMenu(final JFrame frame){
        JMenuBar menu = new JMenuBar();

        JMenu file = new JMenu("file");
        menu.add(file);

        JMenuItem original = new JMenuItem("Open Image");
        file.add(original);
        original.addActionListener(evt -> {
            openImage();
        });

        JMenuItem selectOpen = new JMenuItem("Select open image");
        file.add(selectOpen);
        selectOpen.addActionListener(evt->{

            selectOpenImage();

        });
        JMenuItem importH5Xml = new JMenuItem("import virtual");
        file.add(importH5Xml);
        importH5Xml.addActionListener( evt->{
            String id = IJ.getFilePath("select h5/xml or tiff file");
            if(id==null) return;
            setReady(false);

            segmentationController.submit( ()->{
                try {
                    if (id.endsWith(".tif") ) {
                        ImagePlus plus = FileInfoVirtualStack.openVirtual(id);
                        plus.show();
                        segmentationController.setOriginalPlus(plus);
                    } else{
                        ImporterOptions options = new ImporterOptions();
                        options.setVirtual(true);
                        options.setOpenAllSeries(true);

                        if(id == null) return;
                        options.setId(id);
                        ImagePlus[] pluses = BF.openImagePlus(options);
                        int w = -1;
                        int h = -1;
                        int z = -1;
                        for (ImagePlus plus : pluses) {
                            int nz = plus.getNSlices();
                            int ny = plus.getHeight();
                            int nx = plus.getWidth();
                            w = nx > w ? nx : w;
                            h = ny > h ? ny : h;
                            z = nz > z ? nz : z;

                            plus.show();
                        }
                        for (ImagePlus plus : pluses) {
                            int nz = plus.getNSlices();
                            int ny = plus.getHeight();
                            int nx = plus.getWidth();
                            Calibration cal = plus.getCalibration();
                            if(cal.scaled()){
                                if(nz < z){
                                    cal.pixelDepth = cal.pixelDepth*z / nz;
                                }
                                if(ny < h){
                                    cal.pixelHeight = cal.pixelHeight*h / ny;
                                }
                                if(nx < w){
                                    cal.pixelWidth = cal.pixelWidth*w / nx;
                                }
                            }
                        }
                        segmentationController.setOriginalPlus(pluses[0]);
                    }
                } catch( Exception e){
                    //oh well!
                } finally{
                    finished();
                }
            });
        });
        JMenuItem saveAs = new JMenuItem("save meshes as...");
        file.add(saveAs);
        saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        saveAs.addActionListener(actionEvent -> {
            saveAs();
        });
        JMenuItem saveMesh = new JMenuItem("save");
        file.add(saveMesh);
        saveMesh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        saveMesh.addActionListener(evt->{
            File f = segmentationController.getLastSavedFile();
            if(f==null){
                saveAs();
            } else{
                segmentationController.saveMeshes(f);
                finished();
            }
        });
        JMenuItem load = new JMenuItem("load meshes");
        file.add(load);
        load.addActionListener(actionEvent -> {
            setReady(false);

            FileDialog fd = new FileDialog(frame,"File to load mesh from");
            fd.setDirectory(OpenDialog.getDefaultDirectory());
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);

            if(fd.getFile()==null || fd.getDirectory()==null){
                finished();
                return;
            }

            File f = new File(fd.getDirectory(),fd.getFile());

            segmentationController.loadMeshes(f);
            finished();
        });

        JMenuItem saveParameters = new JMenuItem("Save parameters");
        file.add(saveParameters);
        saveParameters.addActionListener(evt->{
            setReady(false);

            FileDialog fd = new FileDialog(frame,"Select file to save parameters to.");
            fd.setDirectory(OpenDialog.getDefaultDirectory());
            fd.setMode(FileDialog.SAVE);
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                finished();
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.saveParameters(f);
            finished();
        });

        JMenuItem loadParameters = new JMenuItem("Load parameters");
        file.add(loadParameters);
        loadParameters.addActionListener(evt->{
            setReady(false);
            FileDialog fd = new FileDialog(frame,"Select file to load parameters from.");
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                finished();
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.loadParameters(f);
            finished();
            segmentationController.submit(this::updateDisplayedParameters);
        });

        JMenuItem newMeshes = new JMenuItem("Start new meshes.");
        newMeshes.setToolTipText("Clear the current meshes, for starting a new set.");
        file.add(newMeshes);
        newMeshes.addActionListener(evt->{
            segmentationController.restartMeshes();
        });
        //JMenuItem exportFor = new JMenuItem("export for:");
        //exportFor.setToolTipText("For exporting meshes to be used in a larger image.");
        //file.add(exportFor);
        //exportFor.addActionListener(evt->{
        //    exportFor();
        //});




        JMenu edit = new JMenu("edit");
        menu.add(edit);

        undo = new JMenuItem("undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        edit.add(undo);

        undo.addActionListener((evt)->{
            undoAction();
        });
        undo.setEnabled(false);



        redo = new JMenuItem("redo");
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK|ActionEvent.SHIFT_MASK));

        edit.add(redo);

        redo.addActionListener((evt)->{
            redoAction();
        });
        redo.setEnabled(false);

        JMenu mesh = new JMenu("mesh");
        menu.add(mesh);



        JMenuItem stlSave = new JMenuItem("Export as Stl");
        mesh.add(stlSave);
        stlSave.addActionListener((evt)->{
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File for Export");
            fd.setMode(FileDialog.SAVE);
            fd.setFile(segmentationController.getShortImageName() + ".stl");
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.exportAsStl(f);
            finished();
        });

        JMenuItem wireframeSave = new JMenuItem("Export as Wireframe Stl");
        mesh.add(wireframeSave);
        wireframeSave.addActionListener((evt)->{
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File for Export");
            fd.setMode(FileDialog.SAVE);
            fd.setFile(segmentationController.getShortImageName() + "-wf.stl");
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.exportAsWireframeStl(f);
            finished();
        });

        JMenuItem plySave = new JMenuItem("Export as PLY");
        mesh.add(plySave);
        plySave.addActionListener((evt)->{
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File for Export");
            fd.setMode(FileDialog.SAVE);
            fd.setFile(segmentationController.getShortImageName() + ".ply");
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.exportAsPly(f);
            finished();
        });

        JMenu trackMate = createTrackMateMenu();
        mesh.add(trackMate);

        JMenuItem load_3d_furrows = new JMenuItem("load furrows");
        mesh.add(load_3d_furrows);
        load_3d_furrows.addActionListener(event -> {
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File to load 3d furrow data from");
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.load3DFurrows(f);
            finished();
        });

        JMenuItem saveFurrows = new JMenuItem("save furrows");
        mesh.add(saveFurrows);
        saveFurrows.addActionListener(event -> {
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File to save furrow data to");
            fd.setMode(FileDialog.SAVE);
            fd.setFile(segmentationController.getShortImageName() + ".furrow");
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.saveFurrows(f);
            finished();
        });

        mesh.addSeparator();
        JMenuItem track = new JMenuItem("track selected");
        mesh.add(track);
        track.setAccelerator(KeyStroke.getKeyStroke('t'));
        track.addActionListener(evt->{
            trackMeshAction();
        });

        JMenuItem trackBack = new JMenuItem("track backwards");
        mesh.add(trackBack);
        trackBack.setAccelerator(KeyStroke.getKeyStroke('b'));
        trackBack.addActionListener(evt->{
            if(ready) {
                segmentationController.trackMeshBackwards();
                finished();
            }
        });


        JMenu tools = new JMenu("tools");
        menu.add(tools);
        JMenuItem bin = new JMenuItem("Create Binary Image");
        tools.add(bin);
        bin.addActionListener(evt->{
            segmentationController.createBinaryImage();
        });

        JMenuItem mosaic = new JMenuItem("Create Mosaic Image");
        tools.add(mosaic);
        mosaic.addActionListener(evt ->{
            segmentationController.createMosaicImage();
        });

        JMenuItem allVolumes = new JMenuItem("Measure All Volumes");
        tools.add(allVolumes);
        allVolumes.addActionListener(evt->{
            segmentationController.measureAllVolumes();
        });

        JMenuItem measureSelected = new JMenuItem("Measure Selected");
        tools.add(measureSelected);
        measureSelected.addActionListener(evt->{
            segmentationController.measureSelected();
        });

        JMenuItem measureObscured = new JMenuItem("Measure Obscured");
        tools.add(measureObscured);
        measureObscured.addActionListener(evt->{
            Double value = 0.1;
            setReady(false);
            Object update = JOptionPane.showInputDialog(frame, "Input maximum separation.", value);
            if(update==null){
                finished();
                return;
            }

            try{
                double v = Double.parseDouble(update.toString());
                segmentationController.calculateObscuringMeshes(v);

            } catch(Exception e){

            } finally{
                finished();
            }


        });

        JMenuItem showFurrowValues = new JMenuItem("Furrow Values");
        tools.add(showFurrowValues);
        showFurrowValues.addActionListener(evt->{
            segmentationController.showFurrowValues();
        });

        JMenuItem imprt = new JMenuItem("import meshes");
        tools.add(imprt);
        imprt.addActionListener(actionEvent -> {
            importMeshes();
        });

        JMenuItem imprtFrom = new JMenuItem("import from open image");
        tools.add(imprtFrom);
        imprtFrom.addActionListener(actionEvent -> {
            importFrom();
        });

        JMenuItem trackManager = new JMenuItem("Manage Tracks");
        tools.add(trackManager);
        trackManager.addActionListener(evt->{
            buildTrackManager();
        });

        JMenuItem recordSnapShots = new JMenuItem("Record Snapshots");
        tools.add(recordSnapShots);

        recordSnapShots.addActionListener(evt->{
            int i = segmentationController.getNFrames();
            if(i<=0) return;

            Object[] values = new Object[i];
            for(int j = 0; j<i; j++){
                values[j] = j+1;
            }

            Integer start = (Integer)JOptionPane.showInputDialog(
                    frame,
                    null,
                    "Select Start Frame",
                    JOptionPane.INFORMATION_MESSAGE, null,
                    values, values[0]);
            if(start!=null){
                int first = start - 1;
                int left = i - first;
                if(left<=0) return;

                values = new Object[left];

                for(int j = 0; j<left; j++){
                    values[j] = j + start;
                }

                Integer end = (Integer)JOptionPane.showInputDialog(
                        frame,
                        null,
                        "Select Last Frame",
                        JOptionPane.INFORMATION_MESSAGE, null,
                        values, values[values.length-1]);


                if(end != null){
                    segmentationController.recordSnapshots(first, end-1);
                }
            }
        });


        JMenuItem scripts = new JMenuItem("javascript console");
        tools.add(scripts);
        if(terminal != null) {
            scripts.addActionListener(evt -> {
                terminal.showTerminal();
            });

            terminal.addReadyObserver(this);
        }

        JMenuItem remotePrediction = new JMenuItem("remote prediction");
        tools.add(remotePrediction);
        remotePrediction.addActionListener(evt->{
            ImagePlus plus = segmentationController.getMeshImageStack().getOriginalPlus();
            RemotePrediction rep = new RemotePrediction();
            int v = rep.setup("process", plus);
            if(v == PlugInFilter.DOES_ALL){
                new Thread(()->{
                    rep.run(null);
                }).start();
            }
        });
        JMenuItem substituteImageData = new JMenuItem("substitute data");
        substituteImageData.addActionListener(evt->{
            ImagePlus plus = GuiTools.selectOpenImage(frame);
            if(plus != null){
                int index = plus.getCurrentSlice() - 1;
                //index goes 0, 1, 2, 3, 4, ... N-1
                //c0z0t0, c1z0t0, c0z1t0, ...
                int c = plus.getNChannels();
                int z = plus.getNSlices();
                int slicesPerFrame = c*z;
                int time = index / slicesPerFrame;
                int zi = (index - slicesPerFrame*time)/c;
                int ci = (index)%c;
                MeshImageStack stack = new MeshImageStack(plus, time, ci);
                segmentationController.submit(()->{
                    segmentationController.copyImageData( stack );
                });
            }
        });
        tools.add(substituteImageData);

        JMenu help = new JMenu("help");
        menu.add(help);
        JMenuItem about = new JMenuItem("about");
        help.add(about);
        about.addActionListener(evt->{
            GuiTools.showAboutWindow(frame);
        });

        JMenuItem faq = new JMenuItem("FAQ");
        faq.addActionListener(evt->{
            GuiTools.showFaqWindow(frame);
        });
        help.add(faq);



        return menu;
    }

    JMenu createTrackMateMenu(){
        JMenu trackMate = new JMenu("TrackMate");
        JMenuItem export = new JMenuItem("export as TmXml");
        export.addActionListener(evt ->{
            setReady(false);
            File f = getSaveFile(segmentationController.getShortImageName() + ".xml");
            if(f != null){
                segmentationController.submit(()->{
                    try{
                        TrackMateAdapter.saveAsTrackMateFile(segmentationController.getMeshImageStack(),
                                segmentationController.getAllTracks(),
                                f.toPath()
                        );
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                });
            }
            finished();
        });
        JMenuItem imp = new JMenuItem("import from TmXml");
        imp.addActionListener(evt->{
            setReady(false);
            File f = getOpenFile("Select Trackmate .xml");
            if(f!=null){
                segmentationController.submit( ()->{
                    List<Track> tracks = TrackMateAdapter.importTrackMateFile(
                            segmentationController.getMeshImageStack(),
                            f.toPath()
                    );
                    System.out.println("finished loading");
                    segmentationController.setMeshTracks(tracks);
                });
            }
            finished();
        });
        JMenuItem mapping = new JMenuItem("track from TmXml");
        mapping.addActionListener(evt->{
            setReady(false);
            File f = getOpenFile("Select Trackmate .xml");
            if(f != null){
                segmentationController.submit( () -> {
                    List<Track> tracks = TrackMateAdapter.applyTracking(
                            segmentationController.getAllTracks(),
                            segmentationController.getMeshImageStack(),
                            f.toPath()
                    );
                    segmentationController.setMeshTracks(tracks);
                });
            }
            finished();
        });
        mapping.setToolTipText("Use a trackmate file to track existing meshes.");

        trackMate.add(export);
        trackMate.add(imp);
        trackMate.add(mapping);
        return trackMate;
    }

    public void selectOpenImage(){
        GuiTools.selectOpenImage(frame, segmentationController);
    }

    public void nextFrameAction(){
        if(ready) {
            setReady(false);
            segmentationController.nextFrame();
            finished();
        }
    }

    public void previousFrameAction(){
        if(ready) {
            setReady(false);
            segmentationController.previousFrame();
            finished();
        }
    }

    public void trackMeshAction(){
        if(ready) {
            setReady(false);
            segmentationController.trackMesh();
            finished();
        }
    }

    public void trackMeshBackwardsAction(){
        if(ready) {
            setReady(false);
            segmentationController.trackMeshBackwards();
            finished();
        }

    }

    public void showVolumeAction(){
        if(ready){
            setReady(false);
            segmentationController.showVolume();
            finished();
        }
    }

    public void hideVolumeAction(){
        if(ready){
            setReady(false);
            segmentationController.hideVolume();
            finished();
        }
    }

    public void undoAction(){
        if(ready){
            setReady(false);
            segmentationController.undo();
            finished();
        }
    }

    public void redoAction(){
        if(ready){
            setReady(false);
            segmentationController.redo();
            finished();
        }
    }

    public void remeshAction(){
        if(ready){
            setReady(false);
            segmentationController.reMesh();
            finished();
        }
    }

    double[] getDimensionsFromTitle(String shortTitle){
        Pattern p = Pattern.compile("\\(([0-9.]+),([0-9.]+),([0-9.]+),([0-9.]+)\\)");
        Matcher m = p.matcher(shortTitle);
        if(m.find()){
            double ox = Double.parseDouble(m.group(1));
            double oy = Double.parseDouble(m.group(2));
            double w = Double.parseDouble(m.group(3));
            double z = Double.parseDouble(m.group(4));
        }
        return new double[]{0, 0, 0, 0};

    }

    File getSaveFile(String guess){
        FileDialog fd = new FileDialog(frame,"File to save mesh too");
        fd.setFile(guess);
        fd.setDirectory(OpenDialog.getDefaultDirectory());
        fd.setMode(FileDialog.SAVE);
        fd.setVisible(true);
        if(fd.getFile()==null || fd.getDirectory()==null){
            return null;
        }
        return new File(fd.getDirectory(), fd.getFile());
    }
    /*
    public void exportFor(){

        setReady(false);
        String shortTitle = segmentationController.getShortImageName();
        double[] viewBox = getDimensionsFromTitle(shortTitle);
        String titleGuess = getTitleFromTitle(shortTitle);
        FileDialog fd = new FileDialog(frame,"File to export mesh too");
        fd.setFile(titleGuess + ".bmf");
        fd.setDirectory(OpenDialog.getDefaultDirectory());
        fd.setMode(FileDialog.SAVE);
        fd.setVisible(true);
        if(fd.getFile()==null || fd.getDirectory()==null){
            finished();
            return;
        }
        File f = new File(fd.getDirectory(),fd.getFile());
        //segmentationController.exportTo(f, viewBox);
        finished();
    }*/
    public void saveAs(){
        setReady(false);
        FileDialog fd = new FileDialog(frame,"File to save mesh too");
        fd.setFile(segmentationController.getShortImageName() + ".bmf");
        fd.setDirectory(OpenDialog.getDefaultDirectory());
        fd.setMode(FileDialog.SAVE);
        fd.setVisible(true);
        if(fd.getFile()==null || fd.getDirectory()==null){
            finished();
            return;
        }
        File f = new File(fd.getDirectory(),fd.getFile());
        segmentationController.saveMeshes(f);
        finished();
    }

    public void openImage(){
        setReady(false);
        ImagePlus plus = IJ.openImage();
        if(plus == null){
            finished();
            return;
        }

        int channel = 0;
        if(plus.getNChannels()>1){
            Object[] values = IntStream.range(1, plus.getNChannels()+1).boxed().toArray();
            Object channelChoice = JOptionPane.showInputDialog(
                    frame,
                    "Select Channel:",
                    "Choose Channel",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    values,
                    values[0]
            );
            if(channelChoice == null) return;
            channel = (Integer)channelChoice - 1;
        }
        segmentationController.setOriginalPlus(plus, channel);
        plus.show();
        finished();
    }
    File getOpenFile(String title){
        FileDialog fd = new FileDialog(frame, title);
        fd.setDirectory(OpenDialog.getDefaultDirectory());
        fd.setMode(FileDialog.LOAD);
        fd.setVisible(true);
        if(fd.getFile()==null || fd.getDirectory()==null){
            return null;
        }
        return new File(fd.getDirectory(),fd.getFile());
    }
    public void importMeshes(){
        /**
         * "matching" the same frame.
         * "relative" the first import mesh frame is the aligned to the current select frame.
         *
         */


        setReady(false);
        FileDialog fd = new FileDialog(frame,"File to load mesh from");
        fd.setDirectory(OpenDialog.getDefaultDirectory());
        fd.setMode(FileDialog.LOAD);
        fd.setVisible(true);
        if(fd.getFile()==null || fd.getDirectory()==null){
            finished();
            return;
        }
        File f = new File(fd.getDirectory(),fd.getFile());
        Object[] types = ImportType.values();
        Object channelChoice = JOptionPane.showInputDialog(
                frame,
                "Select Channel:",
                "Choose Channel",
                JOptionPane.QUESTION_MESSAGE,
                null,
                types,
                types[0]
        );
        if(channelChoice != null){
            segmentationController.importMeshes(f, (ImportType)channelChoice);
        }
        finished();
    }

    public void importFrom(){
        setReady(false);
        ImagePlus plus = GuiTools.selectOpenImage(frame);
        if(plus == null) return;
        FileDialog fd = new FileDialog(frame,"File to load mesh from");
        fd.setDirectory(OpenDialog.getDefaultDirectory());
        fd.setMode(FileDialog.LOAD);
        fd.setVisible(true);
        if(fd.getFile()==null || fd.getDirectory()==null){
            finished();
            return;
        }
        File f = new File(fd.getDirectory(),fd.getFile());
        Object[] types = ImportType.values();
        Object channelChoice = JOptionPane.showInputDialog(
                frame,
                "Select import type:",
                "Choose import type",
                JOptionPane.QUESTION_MESSAGE,
                null,
                types,
                types[0]
        );
        if(channelChoice != null){
            MeshImageStack origin = new MeshImageStack(plus);
            BoundingBoxTransformer bbt = new BoundingBoxTransformer(origin, segmentationController.getMeshImageStack());
            List<Track> tracks = null;
            try {
                tracks = MeshReader.loadMeshes(f);
                tracks.forEach(bbt::transformTrack);
                segmentationController.importMeshes( tracks, (ImportType)channelChoice);

            } catch (IOException e) {
                return;
            }


        }
        finished();

    }
    private void buildTrackManager() {
        final String managerTitle = "manage tracks";
        int n = tabbedPane.getTabCount();
        for(int i = 0; i<n; i++){
            String title = tabbedPane.getTitleAt(i);
            if(managerTitle.equals(title)){
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }
        JPanel main = new JPanel();
        JPanel content = new JPanel();
        main.setLayout(new BorderLayout());
        JButton cancel = new JButton("close");
        JPanel row = new JPanel();
        row.setLayout(new GridLayout(1, 3));
        row.add(Box.createRigidArea(new Dimension(100,30)));

        row.add(cancel);

        main.add(row, BorderLayout.SOUTH);
        MeshTrackManager manager = new MeshTrackManager();
        manager.buildGui(frame, content);
        manager.manageSegmentationControllerTracks(segmentationController);

        main.add(content, BorderLayout.CENTER);

        tabbedPane.add(main, managerTitle);
        int dex = tabbedPane.indexOfComponent(main);
        ActionListener closeListener = evt->{
            tabbedPane.remove(main);
            segmentationController.removeUndoStateListener(manager);
        };
        JPanel closableTab = GuiTools.getClosableTabComponent(managerTitle, closeListener);
        tabbedPane.setTabComponentAt(dex, closableTab);
        cancel.addActionListener(closeListener);
        tabbedPane.setSelectedIndex(dex);
    }

    /**
     * Submits a 'finish' task to the segmentationController, when the segmentationController is finished it will run this task and enable the ui.
     *
     */
    public void finished(){
        segmentationController.submit(() -> {
            setReady(true);
            setFrameModified(segmentationController.getMeshModified());
        });
    }

    /**
     *
     * change the frame title when the meshes get modified.
     *
     */
    public void setFrameModified(boolean v){
        final String label;
        File file = segmentationController.getLastSavedFile();
        String title = file==null?segmentationController.getShortImageName():file.getName();

        if(v){
            label = "DM3D " + title + "[unsaved]";
        }  else{
            label = "DM3D " + title;
        }
        EventQueue.invokeLater(()->{
            frame.setTitle(label);
        });
    }



    @Override
    public boolean isReady() {
        return ready;
    }

    public void updateUndoRedo(long stateId){
        String ut = segmentationController.getUndoName();
        if(ut.isEmpty()){
            undo.setText("<html>undo</html>");
        } else{
            undo.setText("<html>undo <span style='font-size:smaller;color:gray;'>\"" + ut + "\"</span></html>");
        }

        String rt = segmentationController.getRedoName();
        if(rt.isEmpty()){
            redo.setText("<html>redo</html>");
        } else{
            redo.setText("<html>redo <span style='font-size:smaller;color:gray;'>\"" + rt + "\"</span></html>");
        }
    }
    @Override
    public void setReady(final boolean ready){
        this.ready=ready;
        EventQueue.invokeLater(() -> {

            for(JComponent b: buttons){
                b.setEnabled(ready);
            }

            if(ready==false){
                undo.setEnabled(false);
                redo.setEnabled(false);
            } else{
                undo.setEnabled(segmentationController.canUndo());
                redo.setEnabled(segmentationController.canRedo());
            }
        });
    }

    public void addTabbedPanel(Component panel, String label){
        tabbedPane.add(panel, label);
        tabbedPane.invalidate();
        frame.validate();
    }

    @Override
    public void frameChanged(int i) {

        frameIndicator.update();
    }

    class FrameIndicator{
        final static int MAX_LENGTH=45;
        JTextField field = new JTextField(5);
        JLabel max = new JLabel("/-");
        JLabel imageName = new JLabel("xxx");
        JPanel channelLabel = new JPanel();
        ButtonGroup csg = new ButtonGroup();
        List<JRadioButton> channelSelectors = new ArrayList<>();
        FrameIndicator() {
            //Dimension size = new Dimension(60, 30);
            //field.setMinimumSize(size);
            //field.setMaximumSize(size);
            //field.setPreferredSize(size);
            field.setEnabled(false);
            field.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    field.setEnabled(true);
                }
            });

            field.addActionListener( (actionEvent) -> {
                try {
                    int frame = Integer.parseInt(field.getText());
                    segmentationController.toFrame(frame-1);
                    field.setEnabled(false);
                } catch (NumberFormatException exc) {
                    //oh well
                }
            } );

            field.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent focusEvent) {
                }

                @Override
                public void focusLost(FocusEvent focusEvent) {
                    if (!field.isEnabled()) {
                        return;             //how?
                    }
                    try {
                        int frame = Integer.parseInt(field.getText());
                        segmentationController.toFrame(frame-1);
                        field.setEnabled(false);
                    } catch (NumberFormatException exc) {
                        //oh well
                    }
                }
            });
            channelLabel.setLayout(new BoxLayout(channelLabel, BoxLayout.LINE_AXIS));
            channelLabel.add(new JLabel("channel: "));
            channelLabel.setOpaque(false);
        }
        public JTextField getTextField() {
            return field;
        }
        public JLabel getMaxLabel(){
            return max;
        }

        public void update(){
            int frame = segmentationController.getCurrentFrame();
            int total = segmentationController.getNFrames();

            String title = segmentationController.getShortImageName();
            if(title.length() > MAX_LENGTH){
                title = title.substring(0, MAX_LENGTH) + "...";
            }
            imageName.setText(title);

            field.setEnabled(false);
            field.setText(String.format("%d", (frame+1)));
            max.setText("/ " + total);
            updateChannelSelectors();
        }

        void updateChannelSelectors(){
            int n = segmentationController.getNChannels();

            if(channelSelectors.size() != n){
                int s = channelSelectors.size();
                if(s < n){
                    for(int i = 0; i<(n - s); i++){
                        JRadioButton button = new JRadioButton();
                        button.setOpaque(false);
                        channelSelectors.add(button);
                        csg.add(button);

                        channelLabel.add(button);
                        button.addActionListener(evt->{
                            int channel = segmentationController.getCurrentChannel();
                            if(evt.getSource() == channelSelectors.get(channel)){
                                //Already selected skip!
                            } else{
                                int c = channelSelectors.indexOf(evt.getSource());
                                if(c >= 0) {
                                    segmentationController.selectChannel(c);
                                }
                            }
                        });
                    }
                } else{
                    for(int i = 0; i<(s-n); i++){
                        int dex = channelSelectors.size()-1;
                        JRadioButton b = channelSelectors.remove(dex);

                        csg.remove(b);
                        channelLabel.remove(b);
                    }
                }
                channelLabel.revalidate();

            }
            int channel = segmentationController.getCurrentChannel() + 1;
            channelSelectors.get(segmentationController.getCurrentChannel()).setSelected(true);

        }

    }

}




