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
package deformablemesh.io;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.ringdetection.ContractileRingDetector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by msmith on 4/5/16.
 */
public class FurrowWriter {
    public static void writeFurrows(File f, MeshImageStack stack, ContractileRingDetector detector) {
        try( BufferedWriter br = Files.newBufferedWriter(Paths.get(f.toString()), Charset.forName("utf-8"))  ) {
            br.write("#furrow3d - ver 0.2\n");
            br.write("#center of mass in image coordinates, normal - normalized vector\n");
            br.write("#frame\tcenter:x (px) \ty (px)\tz (slice) \tnormal:x\ty\tz\n");
            for(Integer i: detector) {
                Furrow3D furrow = detector.getFurrow(i);
                double[] cm = stack.getImageCoordinates(furrow.cm);
                double[] dir= furrow.normal;
                String s = String.format("%d\t%s\t%s\t%s\t%s\t%s\t%s\n",
                        i, hex(cm[0]), hex(cm[1]), hex(cm[2]), hex(dir[0]), hex(dir[1]), hex(dir[2]) );
                br.write(s);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static String hex(double f){
        return Double.toHexString(f);
    }



    public static Map<Integer, Furrow3D> loadFurrows(File f, MeshImageStack stack) {
        Map<Integer, Furrow3D> loaded = new HashMap<>();
        try(BufferedReader br = Files.newBufferedReader(f.toPath(), Charset.forName("UTF8"))) {

            String s = br.readLine();
            while(s!=null){
                if(s.charAt(0)=='#'){
                    s = br.readLine();
                    continue;
                }
                String[] values = s.split("\t");
                int frame = Integer.parseInt(values[0]);
                double x1 = Double.parseDouble(values[1]);
                double y1 = Double.parseDouble(values[2]);
                double z1 = Double.parseDouble(values[3]);
                double nx = Double.parseDouble(values[4]);
                double ny = Double.parseDouble(values[5]);
                double nz = Double.parseDouble(values[6]);

                Furrow3D furrow = new Furrow3D(
                        stack.getNormalizedCoordinate(new double[]{x1,y1,z1}),
                        new double[]{nx, ny,nz}
                );
                loaded.put(frame, furrow);
                s=br.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return loaded;

    }
}
