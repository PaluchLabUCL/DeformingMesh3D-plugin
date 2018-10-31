package deformablemesh.gui;

import deformablemesh.SegmentationController;
import deformablemesh.externalenergies.ImageEnergyType;
import deformablemesh.gui.meshinitialization.CircularMeshInitializationDialog;
import deformablemesh.track.MeshTrackManager;
import deformablemesh.track.Track;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.OpenDialog;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: msmith
 * Date: 7/16/13
 * Time: 10:35 AM
 */
public class ControlFrame implements ReadyObserver, FrameListener {
    SegmentationController segmentationController;
    Map<String, ParameterControl> parameterControls = new HashMap<>();
    boolean ready = true;
    ArrayList<JComponent> buttons = new ArrayList<>();

    FrameIndicator frameIndicator = new FrameIndicator();
    private JFrame frame;
    JTabbedPane tabbedPane;

    JMenuItem undo, redo;
    public static Component instance;

    SwingJSTerm terminal;
    Dimension pm = new Dimension(29, 29);
    JLabel message = new JLabel("");

    public ControlFrame( SegmentationController model){
        this.segmentationController = model;

        terminal = new SwingJSTerm(model);

    }
    public void showFrame(){
        frame = new JFrame("Deformable Mesh Control Panel");
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

        addTabbedPanel(createContentPane(), "main");

        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        contentPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        frame.setContentPane(contentPanel);
        frame.setJMenuBar(createMenu(frame));


        frame.setSize(600, 500);
        frame.setVisible(true);
        instance=frame;
        segmentationController.addFrameListener(this);
    }
    public JFrame getFrame(){
        return frame;
    }

    private JPanel createStatusPanel(){
        JPanel status = new JPanel();
        status.setLayout(new BoxLayout(status, BoxLayout.PAGE_AXIS));

        JPanel colorStatusRow = new JPanel();
        colorStatusRow.add(new JLabel("selected color: " ));
        JLabel selectedColorLabel = new JLabel("");
        colorStatusRow.add(selectedColorLabel);
        status.add(colorStatusRow);
        status.add(message);
        segmentationController.addMeshListener(i->{
            selectedColorLabel.setText(segmentationController.getSelectedMeshName());
        });
        return status;
    }
    private JPanel createContentPane(){
        JPanel content = new JPanel();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 4));

        BoxLayout layout = new BoxLayout(content, BoxLayout.PAGE_AXIS);
        content.setLayout(layout);

        content.add(buttonPanel);
        createButtonDeform(buttonPanel);
        createButtonClearMesh(buttonPanel);
        createButtonInitializeMesh2(buttonPanel);
        createButtonRemesh(buttonPanel);

        createFrameIndicator(buttonPanel);
        createButtonPrevious(buttonPanel);
        createButtonNext(buttonPanel);
        buttonPanel.add(Box.createGlue());

        createButtonShowVolume(buttonPanel);
        createEnergySelector(buttonPanel);

        createButtonShowEnergy(buttonPanel);
        createButtonHideVolume(buttonPanel);

        buttonPanel.add(new JLabel("contrast: "));
        createButtonAdjustMinimum(buttonPanel);
        createButtonAdjustMaximum(buttonPanel);

        createButtonMeasureVolume(buttonPanel);

        JPanel inputs = new JPanel();
        inputs.setLayout(new GridLayout(4,2,0,0));
        content.add(inputs);
        SetValue set_gamma = d -> segmentationController.setGamma(d);
        inputs.add(GuiTools.createInputField("gamma", createSavingValue(set_gamma), segmentationController.getGamma(), this));

        SetValue set_alpha = d -> segmentationController.setAlpha(d);
        inputs.add(GuiTools.createInputField("alpha", createSavingValue(set_alpha), segmentationController.getAlpha(), this));

        SetValue set_pressure = d -> segmentationController.setPressure(d);
        inputs.add(GuiTools.createInputField("pressure", createSavingValue(set_pressure), segmentationController.getPressure(), this));

        SetValue normalizer = d -> segmentationController.setNormalizerWeight(d);
        inputs.add(GuiTools.createInputField("normalize", createSavingValue(normalizer), segmentationController.getNormalizeWeight(), this));

        SetValue set_image_weight = d -> segmentationController.setWeight(d);
        inputs.add(GuiTools.createInputField("image weight", createSavingValue(set_image_weight), segmentationController.getImageWeight(), this));

        SetValue set_divisions = d -> segmentationController.setDivisions((int)d);
        inputs.add(GuiTools.createInputField("divisions", createSavingValue(set_divisions), segmentationController.getDivisions(), this));

        SetValue set_curve_weight = d -> segmentationController.setCurveWeight(d);
        inputs.add(GuiTools.createInputField("curve weight", createSavingValue(set_curve_weight), segmentationController.getCurveWeight(), this));

        SetValue setBeta = d -> segmentationController.setBeta(d);
        inputs.add(GuiTools.createInputField("beta", createSavingValue(setBeta), segmentationController.getBeta(), this));

        for(Component comp: inputs.getComponents()){
            if(comp instanceof ParameterControl){
                ParameterControl pc = (ParameterControl)comp;
                parameterControls.put(pc.name, pc);
            }
        }

        return content;
    }

    public void updateDisplayedParameters(){
        updateValue("gamma", segmentationController.getGamma());
        updateValue("alpha", segmentationController.getAlpha());
        updateValue("pressure", segmentationController.getPressure());
        updateValue("normalize", segmentationController.getNormalizeWeight());
        updateValue("image weight", segmentationController.getImageWeight());
        updateValue("divisions", segmentationController.getDivisions());
        updateValue("curve weight", segmentationController.getCurveWeight());
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

    public void createButtonClearMesh(JPanel buttonPanel){
        final JButton clear_mesh = new JButton("clear mesh");
        buttons.add(clear_mesh);
        buttonPanel.add(clear_mesh);
        clear_mesh.addActionListener(actionEvent -> {
            setReady(false);
            segmentationController.clearSelectedMesh();
            finished();
        });
    }

    public void createButtonPrevious(JPanel buttonPanel){
        JButton previous = new JButton("previous");
        buttons.add(previous);
        buttonPanel.add(previous);
        previous.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setReady(false);
                segmentationController.previousFrame();
                finished();
            }
        });

    }

    public void createButtonNext(JPanel buttonPanel){
        JButton next = new JButton("next");
        buttons.add(next);
        buttonPanel.add(next);
        next.addActionListener(evt -> {
            setReady(false);
            segmentationController.nextFrame();
            finished();
        });


    }

    public void createButtonInitializeMesh2(JPanel panel){
        final JButton prompt_mesh = new JButton("initialize mesh...");
        buttons.add(prompt_mesh);
        panel.add(prompt_mesh);
        prompt_mesh.addActionListener((evt)->{
            if(!segmentationController.hasOriginalPlus()){
                return;
            }
            setReady(false);
            new CircularMeshInitializationDialog(frame, segmentationController, this::finished).start();
        });
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

    public void createButtonRemesh(JPanel buttonPanel){
        JButton button = new JButton("remesh");
        buttons.add(button);
        buttonPanel.add(button);
        button.addActionListener((evt)->{
            setReady(false);
            segmentationController.reMesh();
            finished();
        });
    }


    public void createButtonOutput(JPanel buttonPanel){
        JButton output = new JButton("output");
        buttons.add(output);
        buttonPanel.add(output);
        output.addActionListener(event -> {
            setReady(false);
            segmentationController.createOutput();
            finished();
        });
    }

    public void createButtonIntensty(JPanel buttonPanel){
        JButton intensity = new JButton("intensity");
        buttons.add(intensity);
        buttonPanel.add(intensity);
        intensity.addActionListener(event -> {
            setReady(false);
            segmentationController.calculateActinIntensity();
            finished();
        });
    }

    public void createButtonLineScan(JPanel buttonPanel){
        JButton line_scan = new JButton("line scan");
        buttons.add(line_scan);
        buttonPanel.add(line_scan);
        line_scan.addActionListener(e -> {
            setReady(false);
            segmentationController.calculateLineScans();
            finished();
        });
    }

    public void createButtonShowStress(JPanel buttonPanel){
        JButton show_stress = new JButton("show stress");
        buttons.add(show_stress);
        buttonPanel.add(show_stress);
        show_stress.addActionListener(e -> {
            setReady(false);
            segmentationController.showStress();
            finished();
        });
    }

    public void createButtonShowCurvature(JPanel buttonPanel){
        JButton show_curvature = new JButton("show curvature");
        buttons.add(show_curvature);
        buttonPanel.add(show_curvature);
        show_curvature.addActionListener(e -> {
            setReady(false);
            segmentationController.showCurvature();
            finished();
        });
    }

    public void createButtonClearTransients(JPanel buttonPanel){
        JButton clearTransients = new JButton("clear transients");
        buttons.add(clearTransients);
        buttonPanel.add(clearTransients);
        clearTransients.addActionListener((evt)->{
            setReady(false);
            segmentationController.notifyMeshListeners();
            finished();
        });
    }

    public void createButtonShowVolume(JPanel buttonPanel){
        JButton show_volume = new JButton("show volume");
        buttons.add(show_volume);
        buttonPanel.add(show_volume);
        show_volume.addActionListener(e -> {
            setReady(false);
            segmentationController.showVolume();
            finished();
        });
    }

    public void createButtonShowEnergy(JPanel buttonPanel){
        JButton show_energy = new JButton("show energy");
        buttons.add(show_energy);
        buttonPanel.add(show_energy);
        show_energy.addActionListener(e->{
            setReady(false);
            segmentationController.showEnergy();
            finished();
        });
    }
    public void createFrameIndicator(JPanel panel){
        JLabel l = new JLabel("frame: ");




        JPanel sub = new JPanel();
        sub.add(Box.createHorizontalStrut(15));
        sub.setLayout(new BoxLayout(sub, BoxLayout.LINE_AXIS));
        sub.add(l);
        sub.add(Box.createHorizontalGlue());
        sub.add(frameIndicator.getTextField());
        sub.add(frameIndicator.getMaxLabel());
        sub.add(Box.createHorizontalStrut(15));
        panel.add(sub);
    }

    public void createEnergySelector(JPanel buttonPanel){

        JPanel panel = new JPanel();
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
        buttonPanel.add(panel);
    }
    public void createButtonHideVolume(JPanel buttonPanel){
        JButton hide_volume = new JButton("hide volume");
        buttons.add(hide_volume);
        buttonPanel.add(hide_volume);
        hide_volume.addActionListener(actionEvent -> {
            setReady(false);
            segmentationController.hideVolume();
            finished();
        });
    }


    public void createButtonAdjustMinimum(JPanel buttonPanel){
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
        buttonPanel.add(sub);
    }

    public void createButtonAdjustMaximum(JPanel buttonPanel){
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
        buttonPanel.add(sub);
    }

    public void createButtonDeform(JPanel buttonPanel){
        final JButton deform = new JButton("deform");
        buttons.add(deform);
        buttonPanel.add(deform);

        deform.addActionListener(actionEvent -> {
            if(ready){
                setReady(false);
                deform.setText("stop!");
                EventQueue.invokeLater(() -> deform.setEnabled(true));
                if((actionEvent.getModifiers()&ActionEvent.CTRL_MASK)>0){
                    segmentationController.deformAllMeshes();
                } else{
                    segmentationController.deformMesh();
                }


            } else{
                deform.setEnabled(false);
                deform.setText("stopping");
                segmentationController.stopRunning();
                segmentationController.submit(() -> deform.setText("deform"));
                finished();
            }
        });

    }
    JMenuBar createMenu(final JFrame frame){
        JMenuBar menu = new JMenuBar();

        JMenu file = new JMenu("file");
        menu.add(file);

        JMenuItem original = new JMenuItem("Open Image");
        file.add(original);
        original.addActionListener(evt -> {
            setReady(false);
            ImagePlus original1 = IJ.openImage();
            if(original1 ==null){
                finished();
                return;
            }
            segmentationController.setOriginalPlus(original1);
            original1.show();
            finished();
        });

        JMenuItem selectOpen = new JMenuItem("Select open image");
        file.add(selectOpen);
        selectOpen.addActionListener(evt->{

            String[] imageLabels = WindowManager.getImageTitles();

            if(imageLabels.length==0) return;

            Object[] choices = new Object[imageLabels.length];
            for(int i = 0; i<choices.length; i++){
                choices[i] = imageLabels[i];
            }

            Object option = JOptionPane.showInputDialog(
                    frame,
                    "Choose from open images:",
                    "Choose Open Image",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    choices,
                    choices[0]
            );
            if(option instanceof String) {
                ImagePlus plus = WindowManager.getImage((String) option);
                if (plus != null) {

                    segmentationController.setOriginalPlus(plus);

                }
            }

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




        JMenu edit = new JMenu("edit");
        menu.add(edit);

        undo = new JMenuItem("undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));

        edit.add(undo);

        undo.addActionListener((evt)->{
            segmentationController.undo();
            finished();
        });
        undo.setEnabled(false);



        redo = new JMenuItem("redo");
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK|ActionEvent.SHIFT_MASK));

        edit.add(redo);

        redo.addActionListener((evt)->{
            segmentationController.redo();
            finished();
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

        JMenuItem saveSnakes = new JMenuItem("save curves");
        mesh.add(saveSnakes);
        saveSnakes.addActionListener((evt)->{
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File to save curve data to");
            fd.setMode(FileDialog.SAVE);
            fd.setFile(segmentationController.getShortImageName() + ".snake");
            fd.setVisible(true);
            if(fd.getFile()!=null){
                File f = new File(fd.getDirectory(),fd.getFile());
                segmentationController.saveCurvesAsSnakes(f);
            }
            finished();
        });

        JMenuItem loadSnakes = new JMenuItem("load curves");
        mesh.add(loadSnakes);
        loadSnakes.addActionListener((evt)->{
            setReady(false);
            FileDialog fd = new FileDialog(frame,"Load curve data from snake file");
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            if(fd.getFile()==null){
                finished();
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.loadCurvesFromSnakes(f);
            finished();
        });
        mesh.addSeparator();
        JMenuItem track = new JMenuItem("track selected");
        mesh.add(track);
        track.setAccelerator(KeyStroke.getKeyStroke('t'));
        track.addActionListener(evt->{
            if(ready) {
                segmentationController.trackMesh();
                finished();
            }
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
            setReady(false);
            FileDialog fd = new FileDialog(frame,"File to load mesh from");
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                finished();
                return;
            }
            File f = new File(fd.getDirectory(),fd.getFile());
            segmentationController.importMeshes(f);
            finished();
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
        scripts.addActionListener(evt->{
            terminal.showTerminal();
        });
        terminal.addReadyObserver(this);

        JMenu help = new JMenu("help");
        menu.add(help);
        JMenuItem about = new JMenuItem("about");
        help.add(about);
        about.addActionListener(evt->{
            GuiTools.showAboutWindow(frame);
        });



        return menu;
    }

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

    private void buildTrackManager() {

        JDialog dialog = new JDialog(frame, true);
        JPanel main = new JPanel();
        JPanel content = new JPanel();
        main.setLayout(new BorderLayout());
        JButton accept = new JButton("accept");
        JButton cancel = new JButton("cancel");
        JPanel row = new JPanel();
        row.setLayout(new GridLayout(1, 3));
        row.add(Box.createRigidArea(new Dimension(100,30)));
        row.add(accept);

        row.add(cancel);

        main.add(row, BorderLayout.SOUTH);
        MeshTrackManager manager = new MeshTrackManager();
        manager.manageMeshTrackes(segmentationController.getAllTracks());
        manager.buildGui(dialog, content);
        cancel.addActionListener(evt->{
            dialog.dispose();

        });
        accept.addActionListener(evt->{
            List<Track> tracks = manager.getTracks();
            segmentationController.setMeshTracks(tracks);
            dialog.dispose();
        });

        main.add(content, BorderLayout.CENTER);
        dialog.setContentPane(main);
        dialog.pack();
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Submits a 'finish' task to the segmentationController, when the segmentationController is finished it will run this task and enable the ui.
     *
     */
    public void finished(){
        segmentationController.submit(() -> {
            setReady(true);
            EventQueue.invokeLater(this::displayErrors);
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

    public void displayErrors(){
        List<Exception> exceptions = segmentationController.getExecutionErrors();
        for(Exception exc: exceptions){
            GuiTools.errorMessage(exc.toString() + ": " + exc.getMessage());
            exc.printStackTrace();
        }
    }



    @Override
    public boolean isReady() {
        return ready;
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

    public void addTabbedPanel(JPanel panel, String label){
        tabbedPane.add(panel, label);
        tabbedPane.invalidate();
        frame.validate();
    }

    @Override
    public void frameChanged(int i) {
        frameIndicator.setFrame(segmentationController.getCurrentFrame(), segmentationController.getNFrames());
    }

    class FrameIndicator{
        JTextField field = new JTextField("-");
        JLabel max = new JLabel("/-");
        FrameIndicator() {
            Dimension size = new Dimension(60, 30);
            field.setMinimumSize(size);
            field.setMaximumSize(size);
            field.setPreferredSize(size);
            field.setEnabled(false);
            field.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    field.setEnabled(true);
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
            });

            field.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        int frame = Integer.parseInt(field.getText());
                        segmentationController.toFrame(frame-1);
                        field.setEnabled(false);
                    } catch (NumberFormatException exc) {
                        //oh well
                    }
                }
            });

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
        }
        public JTextField getTextField() {
            return field;
        }
        public JLabel getMaxLabel(){
            return max;
        }
        public void setFrame(int frame, int total) {
            field.setEnabled(false);
            field.setText(String.format("%d", (frame+1)));
            max.setText("/ " + total);
        }

    }

}




