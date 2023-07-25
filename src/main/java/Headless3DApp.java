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
