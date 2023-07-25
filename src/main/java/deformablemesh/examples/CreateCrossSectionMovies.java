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
package deformablemesh.examples;

import deformablemesh.MeshImageStack;
import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Projectable;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.gui.Drawable;
import deformablemesh.gui.GuiTools;
import deformablemesh.gui.Slice3DView;
import deformablemesh.gui.meshinitialization.CircularMeshInitializationDialog;
import deformablemesh.io.MeshReader;
import deformablemesh.meshview.DeformableMeshDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.track.Track;
import deformablemesh.util.GroupDynamics;
import deformablemesh.util.Vector3DOps;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;




public class CreateCrossSectionMovies {

    CreateCrossSectionMovies(MeshImageStack stack){
        this.mis = stack;
    }

    static BufferedImage process(BufferedImage img, int cmask, int min, int max){
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i<img.getWidth(); i++){
            for(int j = 0; j<img.getHeight(); j++){
                int rgb = img.getRGB(i, j);
                int b = ( rgb & 0xff );
                int g = ( rgb & 0xff00 ) >> 8;
                int r = ( rgb & 0xff0000 ) >> 16;
                int gray = (r + g + b)/3;
                gray = 255*(gray - min)/(max - min);
                gray = gray < 0 ? 0 : gray;
                gray = gray > 255 ? 255 : gray;

                int a = gray*3/4;
                copy.setRGB(i, j, (a << 24 ) + ( ( ( gray << 16 ) + (gray << 8) +  gray ) & cmask) );
            }
        }

        return copy;
    }
    MeshImageStack mis;
    final static Color CLEAR = new Color(255, 255, 255, 0);
    public ImagePlus threeDVersion(List<Track> solid, List<Track> outlines, int dw, int dh){
        MeshFrame3D mf3d = new MeshFrame3D();
        mf3d.showFrame(true);
        mf3d.addLights();
        mf3d.hideAxis();
        mf3d.setBackgroundColor(Color.WHITE);
        double[] vp = mf3d.getViewParameters();
        vp[3] = 62.5/mis.SCALE;
        mf3d.setViewParameters(vp);
        JFrame jframe = mf3d.getJFrame();
        Component canvas = mf3d.getCanvas();

        int cw = jframe.getWidth();
        int pw = canvas.getWidth();

        int ch = jframe.getHeight();
        int ph = canvas.getHeight();

        jframe.setSize(cw + dw - pw, ch + dh - ph);
        ImageStack out = null;
        int min, max;
        if (solid.size() > 0) {
            min = solid.stream().mapToInt(Track::getFirstFrame).min().getAsInt();
            max = solid.stream().mapToInt(Track::getLastFrame).max().getAsInt();
        } else{
            min = outlines.stream().mapToInt(Track::getFirstFrame).min().getAsInt();
            max = outlines.stream().mapToInt(Track::getLastFrame).max().getAsInt();
        }
        System.out.println(min + ", " + max);
        for(int i = min; i<=max; i++){
            int cf = i;
            List<DeformableMesh3D> solidMeshes = solid.stream().filter(
                    t->t.containsKey(cf)
            ).map(
                    t->t.getMesh(cf)
            ).collect(
                    Collectors.toList()
            );
            List<DeformableMesh3D> outlinedMeshes = outlines.stream().filter(
                    t->t.containsKey(cf)
            ).map(
                    t->t.getMesh(cf)
            ).collect(
                    Collectors.toList()
            );

            double[] cm;
            if(solidMeshes.size() > 0) {
                cm = GroupDynamics.getCenterOfMass(solidMeshes);
            } else if(outlinedMeshes.size() > 0){
                cm = GroupDynamics.getCenterOfMass(outlinedMeshes);
            } else{
                cm = new double[] {0, 0, 0};
            }

            vp[0] = cm[0];
            vp[1] = cm[1];
            vp[2] = cm[2];
            mf3d.setViewParameters(vp);
            solidMeshes.forEach(sm ->{
                sm.setShowSurface(true);
                sm.create3DObject();
                DeformableMeshDataObject dao = sm.data_object;
                dao.setColor(sm.getColor());
                dao.setWireColor(CLEAR);

                mf3d.addDataObject(dao);
            });

            outlinedMeshes.forEach(sm ->{
                sm.setShowSurface(true);
                sm.create3DObject();
                DeformableMeshDataObject dao = sm.data_object;
                Color a = sm.getColor();
                Color t = new Color(a.getRed(), a.getGreen(), a.getBlue(), 64);
                dao.setColor(t);
                dao.setWireColor(t);

                mf3d.addDataObject(dao);
            });


            BufferedImage img = mf3d.snapShot();
            if(out == null){
                out = new ImageStack(img.getWidth(), img.getHeight());
            }
            out.addSlice(new ColorProcessor(img));
            solidMeshes.stream().map(m -> m.data_object).forEach(mf3d::removeDataObject);
            outlinedMeshes.stream().map(m -> m.data_object).forEach(mf3d::removeDataObject);

        }

        return new ImagePlus("3D rendered", out);
    }
    static ImagePlus createProjectionStack(List<Track> solid, List<Track> outlines, MeshImageStack stack, double[] direction){
        ImageStack out = null;
        int zoom = 2;
        for(int i = 0; i<stack.getNFrames(); i++) {
            int frame = i;
            stack.setFrame(frame);
            List<DeformableMesh3D> meshes = solid.stream().filter(t -> t.containsKey(frame)).map(t -> t.getMesh(frame)).collect(Collectors.toList());
            List<DeformableMesh3D> outlined = outlines.stream().filter(t -> t.containsKey(frame)).map(t -> t.getMesh(frame)).collect(Collectors.toList());
            double[] center = meshes.size() > 0 ?
                    GroupDynamics.getCenterOfMass(meshes) :
                    GroupDynamics.getCenterOfMass(outlined);

            FurrowTransformer t = stack.createFurrowTransform(center, direction);
            stack.setChannel(0);
            BufferedImage c1 = process(stack.createSlice(t), 0xff0000, 50, 255);
            stack.setChannel(1);
            BufferedImage c2 = process(stack.createSlice(t), 0xffffff, 100, 255);


            List<Drawable> solidDrawables = meshes.stream().map(mesh -> {
                ProjectableMesh pm = new ProjectableMesh(mesh);
                Shape s = pm.continuousPaths(t);
                Drawable d = g2d -> {
                    Color c = mesh.getColor();
                    int r = c.getRed();
                    int g = c.getGreen();
                    int b = c.getBlue();
                    Color fill = new Color(r, g, b, 64);
                    g2d.setColor(fill);
                    //g2d.setColor(Color.WHITE);
                    g2d.fill(s);
                    //g2d.draw(s);
                };
                return d;
            }).collect(Collectors.toList());
            List<Drawable> outlineDrawables = outlined.stream().map(mesh -> {
                ProjectableMesh pm = new ProjectableMesh(mesh);
                Shape s = pm.continuousPaths(t);
                Drawable d = g2d -> {
                    Color c = mesh.getColor();
                    int r = c.getRed();
                    int g = c.getGreen();
                    int b = c.getBlue();
                    Color line = new Color(r, g, b, 255);
                    g2d.setColor(line);
                    g2d.draw(s);

                };
                return d;
            }).collect(Collectors.toList());

            //Cretae and draw a zoomed version of the image.
            Image buff = new BufferedImage(zoom * c1.getWidth(), zoom * c1.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) buff.getGraphics();
            GuiTools.applyRenderingHints(g2d);
            AffineTransform transform = g2d.getTransform();
            AffineTransform scale = AffineTransform.getScaleInstance(zoom, zoom);
            transform.concatenate(scale);
            g2d.setTransform(transform);

            g2d.drawImage(c1, 0, 0, null);
            g2d.drawImage(c2, 0, 0, null);
            solidDrawables.forEach(d -> d.draw(g2d));
            outlineDrawables.forEach(d->d.draw(g2d));

            ImageProcessor proc = new ColorProcessor(buff);
            if(out == null){
                out = new ImageStack(proc.getWidth(), proc.getHeight());
            }
            out.addSlice("time frame: " + frame, proc);
        }
        return new ImagePlus("projection: " + Arrays.toString(direction), out);
    }

    public static void main(String[] args) throws IOException {
        File img;
        File solidFile;
        File outlinesFile;

        if(args.length == 0){
            img = new File(IJ.getFilePath("Select Image"));
            solidFile = new File(IJ.getFilePath("Select solid mesh file"));
            outlinesFile = new File(IJ.getFilePath("Select outline mesh file"));
        } else{
            img = new File(args[0]);
            solidFile = new File(args[1]);
            outlinesFile = new File(args[2]);
        }

        List<Track> solid = MeshReader.loadMeshes(solidFile);
        List<Track> outlines = MeshReader.loadMeshes(outlinesFile);

        ImagePlus plus = new ImagePlus(img.getAbsolutePath());
        MeshImageStack stack = new MeshImageStack(plus, 0, 1);

        new ImageJ();
        ImagePlus xz = createProjectionStack(solid, outlines, stack, Vector3DOps.zhat);
        ImagePlus yz = createProjectionStack(solid, outlines, stack, Vector3DOps.xhat);
        ImagePlus mxz = createProjectionStack(solid, outlines, stack, Vector3DOps.yhat);
        int w = yz.getHeight();
        int h = mxz.getHeight();
        CreateCrossSectionMovies ccsm = new CreateCrossSectionMovies(stack);
        ImagePlus rendered = ccsm.threeDVersion(solid, outlines, w, h);
        xz.show();
        yz.show();
        mxz.show();
        rendered.show();
    }
}
