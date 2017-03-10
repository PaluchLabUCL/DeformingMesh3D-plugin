package deformablemesh.gui;

import deformablemesh.SegmentationController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Created on 10/03/2017.
 */
public class PropertySaver {
    static boolean canSave=true;
    static public void loadProperties(SegmentationController control) throws IOException {
        String home = System.getProperty("user.home");
        File props = new File(home, ".dmesh3d");
        if(props.exists()){
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
                        case "curve-weight":
                            control.setCurveWeight(Double.parseDouble(pair[1]));
                            break;
                        case "alpha":
                            control.setAlpha(Double.parseDouble(pair[1]));
                            break;
                        case "normalize":
                            control.setNormalizerWeight(Double.parseDouble(pair[1]));
                            break;
                        case "divisions":
                            control.setDivisions(Integer.parseInt(pair[1]));
                            break;
                        case "beta":
                            control.setBeta(Double.parseDouble(pair[1]));
                            break;
                        default:
                            System.out.println("skipping: " + pair[0]);

                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    static public void saveProperties(SegmentationController control) throws IOException {
        if(!canSave) return;
        String home = System.getProperty("user.home");
        File props = new File(home, ".dmesh3d");
        try(BufferedWriter writer = Files.newBufferedWriter(props.toPath(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)){
            writer.write(String.format("%s\t%f\n","gamma", control.getGamma()));
            writer.write(String.format("%s\t%f\n","pressure", control.getPressure()));
            writer.write(String.format("%s\t%f\n","image-weight", control.getImageWeight()));
            writer.write(String.format("%s\t%f\n","curve-weight", control.getCurveWeight()));
            writer.write(String.format("%s\t%f\n","alpha", control.getAlpha()));
            writer.write(String.format("%s\t%f\n","normalize", control.getNormalizeWeight()));
            writer.write(String.format("%s\t%d\n","divisions", control.getDivisions()));
            writer.write(String.format("%s\t%f\n","beta", control.getBeta()));
        } catch(IOException exc){
            canSave = false;
            throw new IOException(exc);
        }
    }
}
