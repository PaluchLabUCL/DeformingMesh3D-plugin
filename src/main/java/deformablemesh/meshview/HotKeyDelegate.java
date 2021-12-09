package deformablemesh.meshview;

import deformablemesh.SegmentationController;
import deformablemesh.gui.ControlFrame;
import deformablemesh.gui.GuiTools;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotKeyDelegate {
    JComponent comp;
    ControlFrame accessControl;
    MeshFrame3D display3D;
    boolean hudShowing = false;
    List<ActionMapKey> actions;

    Map<Integer, String> specialCharacters = buildSpecialCharacterMap();
    static private Map<Integer, String> buildSpecialCharacterMap(){
        Map<Integer, String> map = new HashMap<>();
        map.put(KeyEvent.VK_LEFT, "\u2190");
        map.put(KeyEvent.VK_UP, "\u2191");
        map.put(KeyEvent.VK_DOWN, "\u2193");
        map.put(KeyEvent.VK_RIGHT, "\u2192");
        map.put(KeyEvent.VK_BACK_SPACE, "BS");
        map.put(KeyEvent.VK_DELETE, "DEL");


        return map;
    }


    class ActionMapKey{
        KeyStroke k;
        String name;
        String description;
        Action action;
        String activation;
        ActionMapKey(KeyStroke k, String name, String description, Action action){
            this.k = k;
            this.name = name;
            this.description = description;
            this.action = action;
            comp.getInputMap().put(k, name);
            comp.getActionMap().put(name, action);

            activation = createActivationString();
        }


        String createActivationString(){
            String activate = specialCharacters.computeIfAbsent(k.getKeyCode(), k-> "'" + (char)k.intValue() + "'");
            int m = k.getModifiers();
            int c = KeyEvent.CTRL_DOWN_MASK;
            int s = KeyEvent.SHIFT_MASK;

            if( (s & m) != 0){
                activate = "SHIFT+" + activate;
            }

            if( (c & m) != 0){
                activate = "CTRL+" + activate;
            }
            return activate;
        }

        void draw(int x, int y, Graphics2D g){


            g.drawString(activation, x, y);
            g.drawString(description, x+100, y);

        }
    }
    Runnable ifEnabled(Runnable r){
        return () -> {
            if (accessControl.isReady()) {
                r.run();
            }
        };
    }

    public HotKeyDelegate(MeshFrame3D frame, SegmentationController controller, ControlFrame gui){
        this.comp = (JPanel) frame.frame.getContentPane();
        accessControl = gui;
        display3D = frame;
        actions = new ArrayList<>();
        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_N, 0, true),
                "NEXT_MESH",
                "Selects next mesh",
                ifEnabled(controller::selectNextMeshTrack)
        );
        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_O, 0, true),
                "TOGGLE_SURFACE",
                "Show/Hide current mesh surface",
                controller::toggleSurface
            );
        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true),
                "SNAPSHOT",
                "Snapshot of current scene.",
                ifEnabled(controller::takeSnapShot)
            );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true),
                "DEFORM_MESH",
                "Deform/Stop deforming current mesh.",
                ()->accessControl.deformAction(false)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK, true),
                "DEFORM_MESHES",
                "Deform/Stop all meshes.",
                ()->accessControl.deformAction(true)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_T, 0, true),
                "TRACK_FORWARD",
                "Track selected mesh forward.",
                ifEnabled(accessControl::trackMeshAction)
        );


        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, true),
                "TRACK_BACKWARD",
                "Track selected mesh backwards.",
                ifEnabled(accessControl::trackMeshBackwardsAction)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_I, 0, true),
                "INITIALIZE_MESHES",
                "Initialize new meshes",
                ifEnabled(accessControl::initializeMeshAction)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, true),
                "REMESH",
                "Raycast selected mesh",
                ifEnabled(accessControl::remeshAction)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true),
                "CONNECTION_REMESH",
                "Remesh Connections of selected mesh.",
                ifEnabled(()->accessControl.connectionRemesh(false))
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK, true),
                "CONNECTION_REMESH_ALL",
                "Remesh Connections of all meshes.",
                ifEnabled(()->accessControl.connectionRemesh(true))
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK, true),
                "UNDO",
                "Undo most recent action.",
                ifEnabled(accessControl::undoAction)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true),
                "REDO",
                "Redo most recently undone action.",
                ifEnabled(accessControl::redoAction)
        );


        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true),
                "CLEAR_MESH",
                "Clear selected mesh from frame. (backspace)",
                ifEnabled(controller::clearSelectedMesh)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true),
                "CLEAR_MESH2",
                "Clear selected mesh from frame. (delete)",
                ifEnabled(controller::clearSelectedMesh)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true),
                "TOGGLE_AXIS",
                "Toggle axis display.",
                frame::toggleAxis
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true),
                "PREVIOUS_FRAME",
                "Previous frame.",
                ifEnabled(accessControl::previousFrameAction)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true),
                "NEXT_FRAME",
                "Next frame.",
                ifEnabled(accessControl::nextFrameAction)
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, true),
                "SHOW_VOLUME",
                "Show image as 3D volume.",
                accessControl::showVolumeAction
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.SHIFT_MASK, true),
                "HIDE_VOLUME",
                "Hide 3D volume representation.",
                accessControl::hideVolumeAction
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true),
                "ADD_VOLUME",
                "add volume channel",
                frame::createNewChannelVolume
        );
        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_X, 0, true),
                "REMOVE_VOLUME",
                "remove volume channel",
                frame::chooseToremoveChannelVolume
        );
        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true),
                "CONTRAST_VOLUME",
                "contrast volume channel",
                frame::chooseToContrastChannelVolume
        );

        createActionMapKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_H, 0, true),
                "SHOW_HELP_HUD",
                "Show/Hide cheat sheet.",
                this::toggleHud
        );



    }
    public void toggleHud(){
        if(hudShowing){
            display3D.setNoHud();
            hudShowing = false;
        } else {
            display3D.setHud(this::draw);
            hudShowing = true;
        }
    }
    int padding = 15;
    int margin = 25;
    int width = 420;
    int actionHeight = 20;
    public void draw(Graphics2D graphics){
        GuiTools.applyRenderingHints(graphics);
        int w = comp.getWidth();
        int h = comp.getHeight();

        int ox = padding;
        int oy = padding;

        int contentHeight = actionHeight*(actions.size()) + 2*margin;


        graphics.setColor(Color.BLACK);
        graphics.drawRect(ox, oy, width, contentHeight);

        graphics.setColor(new Color(255, 255, 255, 75));
        graphics.fillRect(ox, oy, width, contentHeight);
        int x = margin;
        int y = margin + actionHeight;
        graphics.setColor(Color.BLACK);
        for(ActionMapKey amk: actions){
            amk.draw(ox + x, oy + y, graphics);
            y += actionHeight;
        }

    }

    ActionMapKey createActionMapKey(KeyStroke k, String name, String description, Runnable run){
        ActionMapKey key = new ActionMapKey(k, name, description, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
            }
        });
        actions.add(key);
        return key;
    }

    /**
     *  addKeyListener(new KeyListener(){

    @Override
    public void keyTyped(KeyEvent e) {
    char c = e.getKeyChar();
    switch(c){
    case ' ':
    toggleAxis();
    break;
    case 's':
    segmentationController.takeSnapShot();
    break;
    case 'n':
    segmentationController.selectNextMeshTrack();
    break;
    case 'o':
    segmentationController.toggleSurface();
    break;
    default:
    break;
    }
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
    });
     */


}
