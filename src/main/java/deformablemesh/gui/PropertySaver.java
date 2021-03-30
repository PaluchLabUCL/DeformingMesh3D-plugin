package deformablemesh.gui;

import deformablemesh.SegmentationController;
import deformablemesh.meshview.MeshFrame3D;

import javax.swing.JFrame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * For saving constants that the user has entered.
 *
 *
 * Created on 10/03/2017.
 */
public class PropertySaver {
    /* once this fails once, it will stop trying to save. */
    static boolean canSave=true;

    /**
     * Tries to find a user.home and .dmesh3d file for user preferences to reload the last used constants.
     *
     * @param control where the values go.
     * @throws IOException
     */
    static public void loadProperties(SegmentationController control) throws IOException {
        String home = System.getProperty("user.home");
        File props = new File(home, ".dmesh3d");
        if(props.exists()) {
            loadProperties(control, props);
        }
    }

    /**
     * Tries to find a user.home and .dmesh3d file for user preferences to reload the last used constants.
     *
     * @param control where the values go.
     * @param props file properties will be loaded from.
     * @throws IOException
     */
    static public void loadProperties(SegmentationController control, File props) throws IOException {
        //try to read them properties.
        List<String> lines = Files.readAllLines(props.toPath(), StandardCharsets.UTF_8);
        for(String line: lines){
            String[] pair = line.split("\\t");
            try {
                switch (pair[0]) {
                    case "gamma":
                        control.setGamma(Double.parseDouble(pair[1]));
                        break;
                    case "pressure":
                        control.setPressure(Double.parseDouble(pair[1]));
                        break;
                    case "image-weight":
                        control.setWeight(Double.parseDouble(pair[1]));
                        break;
                    case "alpha":
                        control.setAlpha(Double.parseDouble(pair[1]));
                        break;
                    case "divisions":
                        control.setDivisions(Integer.parseInt(pair[1]));
                        break;
                    case "beta":
                        control.setBeta(Double.parseDouble(pair[1]));
                        break;
                    case "steric-weight":
                        control.setStericNeighborWeight(Double.parseDouble(pair[1]));
                        break;
                    default:
                        System.out.println("skipping: " + pair[0]);

                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Tries to save the currently set properties/constants.
     *
     * @param control
     * @throws IOException
     */
    static public void saveProperties(SegmentationController control) throws IOException {
        if(!canSave) return;
        String home = System.getProperty("user.home");
        File props = new File(home, ".dmesh3d");
        saveProperties(control, props);
    }


    static public void saveProperties(SegmentationController control, File props) throws IOException {
        try(BufferedWriter writer = Files.newBufferedWriter(props.toPath(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)){
            writer.write(String.format("%s\t%s\n","gamma", Double.toHexString(control.getGamma())));
            writer.write(String.format("%s\t%s\n","pressure", Double.toHexString(control.getPressure())));
            writer.write(String.format("%s\t%s\n","image-weight", Double.toHexString(control.getImageWeight())));
            writer.write(String.format("%s\t%s\n","alpha", Double.toHexString(control.getAlpha())));
            writer.write(String.format("%s\t%s\n","steric-weight", Double.toHexString(control.getStericNeighborWeight())));
            writer.write(String.format("%s\t%d\n","divisions", control.getDivisions()));
            writer.write(String.format("%s\t%s\n","beta", Double.toHexString(control.getBeta())));
        } catch(IOException exc){
            canSave = false;
            throw new IOException(exc);
        }
    }


    /**
     * For setting up the position of the windows. Currently just tries to place them side by side.
     * 
     * @param control
     * @param mf3d
     */
    static public void positionFrames(ControlFrame control, MeshFrame3D mf3d){
        JFrame c = control.getFrame();
        JFrame m = mf3d.getJFrame();
        int x = m.getX();
        int y = m.getY();
        int w = m.getWidth();
        int h = m.getHeight();
        c.setLocation(x+w, y);

    }
}
