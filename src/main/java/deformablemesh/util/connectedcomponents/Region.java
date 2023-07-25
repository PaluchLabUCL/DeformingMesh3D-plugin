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
package deformablemesh.util.connectedcomponents;

import deformablemesh.MeshImageStack;
import deformablemesh.gui.Drawable;
import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.VolumeDataObject;
import deformablemesh.util.ColorSuggestions;
import ij.ImageStack;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

public class Region {
    public static Drawable misses = g2d->{};
    List<int[]> pts = new ArrayList<>();
    int lx = Integer.MAX_VALUE;
    int ly = lx;
    int lz = lx;
    int hx, hy, hz;

    double[] center = new double[3];
    Color c;
    VolumeDataObject dataObject;
    boolean selected = false;
    int label;
    Region(int label, List<int[]> pts) {
        this.label= label;
        c = ColorSuggestions.getSuggestion();
        for(int[] pt: pts){
            lx = pt[0]<lx?pt[0]:lx;
            ly = pt[1]<ly?pt[1]:ly;
            lz = pt[2]<lz?pt[2]:lz;

            hx = pt[0]>hx?pt[0]:hx;
            hy = pt[1]>hy?pt[1]:hy;
            hz = pt[2]>hz?pt[2]:hz;
            center[0] += pt[0];
            center[1] += pt[1];
            center[2] += pt[2];
        }
        center[0] = center[0]/pts.size();
        center[1] = center[1]/pts.size();
        center[2] = center[2]/pts.size();

        hx = hx+1;
        hy = hy+1;
        hz = hz+1;
        this.pts = pts;
    }

    public double[] getSize(){

        return new double[]{
                hx - lx + 1,
                hy - ly + 1,
                hz - lz + 1
        };

    }
    public double[] getLowCorner(){
        return new double[]{lx, ly, lz};
    }
    public double[] getHighCorner(){
        return new double[]{hx, hy, hz};
    }
    public DataObject getDataObject(MeshImageStack stack){
        if(dataObject==null){
            dataObject = new VolumeDataObject(c);
            dataObject.setTextureData(stack, pts);
            double[] corner = stack.getNormalizedCoordinate(new double[]{lx-stack.offsets[0]*0.5, ly-stack.offsets[0]*0.5, lz-stack.offsets[0]*0.5});
            dataObject.setPosition(corner[0], corner[1], corner[2]);
        }

        return dataObject;
    }

    public void translate(int dx, int dy, int dz){
        lx += dx;
        hx += dy;

        ly += dy;
        hy += dy;

        lz += dz;
        hz += dz;

        for(int[] pt: pts){
            pt[0] += dx;
            pt[1] += dy;
            pt[2] += dz;
        }
        center[0] += dx;
        center[1] += dy;
        center[2] += dz;

    }

    public Drawable getXYSlice(int[] pt){
        int z = pt[2];
        if(z<lz||z>=hz){
            return misses;
        }
        List<int[]> plane = new ArrayList<>();
        int lowx = hx;
        int highx = lx;

        int lowy = hy;
        int highy = ly;

        for(int[] p: pts){
            if(p[2]!=z){
                continue;
            }
            int x = p[0];
            int y = p[1];
            lowx = x<lowx?x: lowx;
            highx = x>highx?x: highx;
            lowy = y<lowy?y: lowy;
            highy = y>highy?y: highy;
            plane.add(p);
        }

        if(plane.size()==0){
            return misses;
        }

        int w = highx - lowx + 1;
        int h = highy - lowy + 1;
        BufferedImage img = new BufferedImage(w,h, BufferedImage.TYPE_4BYTE_ABGR );
        WritableRaster raster = (WritableRaster)img.getData();
        int[] color = getColor(c);
        for(int[] p: plane){
            int x = p[0] - lowx;
            int y = p[1] - lowy;
            if(x<w && y<h) {
                raster.setPixel(x, y, color);
            }
        }
        img.setData(raster);

        BufferedImage outline = getOutline(img, color);

        int ox = lowx - pt[0];
        int oy = lowy - pt[1];
        AffineTransform disp = AffineTransform.getTranslateInstance(ox, oy);
        return g2d->{
            if(selected){
                g2d.drawImage(img, disp, null);
            }
            g2d.drawImage(outline, disp, null);
        };
    }
    public String getName(){
        return "#" + label;
    }

    public Color getColor(){
        return c;
    }

    public Drawable getZYSlice(int[] pt){
        int x = pt[0];
        if(x<lx||x>=hx){
            return misses;
        }
        List<int[]> plane = new ArrayList<>();
        int lowz = hz;
        int highz = lz;

        int lowy = hy;
        int highy = ly;

        for(int[] p: pts){
            if(p[0]!=x){
                continue;
            }
            int z = p[2];
            int y = p[1];
            lowz = z<lowz?z: lowz;
            highz = z>highz?z: highz;
            lowy = y<lowy?y: lowy;
            highy = y>highy?y: highy;
            plane.add(p);
        }

        if(plane.size()==0){
            return misses;
        }

        int w = highz - lowz + 1;
        int h = highy - lowy + 1;
        BufferedImage img = new BufferedImage(w,h, BufferedImage.TYPE_4BYTE_ABGR );
        WritableRaster raster = (WritableRaster)img.getData();
        int[] color = getColor(c);
        for(int[] p: plane){
            int imgX = p[2] - lowz;
            int imgY = p[1] - lowy;
            if(imgX<w && imgY<h) {
                raster.setPixel(imgX, imgY, color);
            }
        }
        img.setData(raster);

        BufferedImage outline = getOutline(img, color);

        int ox = lowz - pt[2];
        int oy = lowy - pt[1];
        AffineTransform disp = AffineTransform.getTranslateInstance(ox, oy);
        return g2d->{
            if(selected){
                g2d.drawImage(img, disp, null);
            }
            g2d.drawImage(outline, disp, null);
        };
    }


    public Drawable getXZSlice(int[] pt){
        int y = pt[1];
        if(y<ly||y>=hy){
            return misses;
        }
        List<int[]> plane = new ArrayList<>();
        int lowx = hx;
        int highx = lx;

        int lowz = hz;
        int highz = lz;

        for(int[] p: pts){
            if(p[1]!=y){
                continue;
            }
            int x = p[0];
            int z = p[2];
            lowx = x<lowx?x: lowx;
            highx = x>highx?x: highx;
            lowz = z<lowz?z: lowz;
            highz = z>highz?z: highz;
            plane.add(p);
        }

        if(plane.size()==0){
            return misses;
        }

        int w = highx - lowx + 1;
        int h = highz - lowz + 1;
        BufferedImage img = new BufferedImage(w,h, BufferedImage.TYPE_4BYTE_ABGR );
        WritableRaster raster = (WritableRaster)img.getData();
        int[] color = getColor(c);
        for(int[] p: plane){
            int imgX = p[0] - lowx;
            int imgY = p[2] - lowz;
            if(imgX<w && imgY<h) {
                raster.setPixel(imgX, imgY, color);
            }
        }
        img.setData(raster);

        BufferedImage outline = getOutline(img, color);

        int ox = lowx - pt[0];
        int oy = lowz - pt[2];
        AffineTransform disp = AffineTransform.getTranslateInstance(ox, oy);
        return g2d->{
            if(selected){
                g2d.drawImage(img, disp, null);
            }
            g2d.drawImage(outline, disp, null);
        };
    }

    static int[] getColor(Color c){

        return new int[]{c.getRed(), c.getGreen(), c.getBlue(), 255};
    }

    BufferedImage getOutline(BufferedImage img, int[] color){
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage outline = new BufferedImage(w,h, BufferedImage.TYPE_4BYTE_ABGR );
        WritableRaster raster2 = (WritableRaster)outline.getData();

        for(int i = 0; i<w; i++){
            for(int j = 0; j<h; j++){
                if( isEdge(i,j,img) ){
                    raster2.setPixel(i,j,color);
                }
            }
        }

        outline.setData(raster2);

        return outline;
    }

    boolean isEdge(int x, int y, BufferedImage img){
        if(img.getRGB(x, y)==0){
            return false;
        }
        for(int i = 0; i<3; i++){
            for (int j = 0; j<3; j++){
                if(i==1&&j==1) continue;

                int x2 = x + 1 - i;
                int y2 = y + 1 - j;
                if(x2<0||x2>=img.getWidth()||y2<0||y2>=img.getHeight()){
                    return true;
                } else{
                    if(img.getRGB(x2, y2)==0) return true;
                }
            }
        }
        return false;
    }

    public boolean isSelected(){
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getLabel() {
        return label;
    }

    public int calculateVolume() {
        return pts.size();
    }

    public int calculateSlices(){
        return hz - lz;
    }

    public double[] getCenter() {
        return center;
    }
    public List<int[]> getPoints(){
        return pts;
    }

    ImageStack getLocalBinaryPixels(){
        double[] size = getSize();
        int[] sizes = {(int)size[0], (int)size[1], (int)size[2]};
        ImageStack stack = new ImageStack(sizes[0], sizes[1]);
        for(int i = 0; i<sizes[2]; i++){
            stack.addSlice(new ShortProcessor(sizes[0], sizes[1]));
        }
        for(int[] pt: pts){
            stack.getProcessor(pt[2] - lz + 1).set(pt[0]-lx, pt[1]-ly, 1);
        }
        return stack;
    }
    public List<Region> split(){
        ImageStack stack = getLocalBinaryPixels();
        return ConnectedComponents3D.getRegions(stack);
    }
}
