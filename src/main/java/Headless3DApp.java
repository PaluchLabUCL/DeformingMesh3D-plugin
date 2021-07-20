import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.gui.SwingJSTerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * For running remotely. This will open the corresponding image files. Create and deform a mesh through all frames
 * and save the mesh, never opening a gui.
 * User: msmith
 * Date: 7/31/13
 * Time: 3:42 PM
 */
public class Headless3DApp {

    public static void main(String[] args) throws IOException {
        SegmentationModel model = new SegmentationModel();
        SegmentationController controls = new SegmentationController(model);

        SwingJSTerm term = new SwingJSTerm(controls);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder builder = new StringBuilder("");
        System.out.print("\n>>");
        while(true){
            String line = reader.readLine();
            System.out.println(line);

            if(line.startsWith("#")){
                term.evaluateHeadless(builder.toString());
                builder = new StringBuilder("");
                System.out.print("\n\n>>");
            } else{
                builder.append(line);
                builder.append("\n");
            }
        }

    }

}