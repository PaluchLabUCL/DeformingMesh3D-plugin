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
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 *      updated 5/9/2014: Uses the correct dimensions for the image and preserves slice labels.
 *
 *      updated 5/8/2014: When the output image is created it loops through all of the images in the
 *      image stack, not just all of the slices.
 *
  *     A plugin for collecting a rectangular region by cropping 
  *     and rotating the region.  For RGB images bilinear interpolation
  *     is used, for Anything else, the default interpolation is used.
  *
  *
  **/
public class Mouse_X extends MouseAdapter implements PlugInFilter {

    ImagePlus imp,drawplus;
    ImageCanvas drawcanvas;
    ImageProcessor drawproc,bufferproc;
    
    ArrayList<int[]> points;
    int state;


    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL;
    }

    /**
      *     This will create a new image plus to draw on so that you can 
      *     click on the canvas to select the four points necessary
      * 
      **/
    public void run(ImageProcessor ip) {
        ImagePlus drawplus = new ImagePlus("Click to select mid-points",ip.convertToRGB().duplicate());
        
        drawplus.show();
        drawcanvas = drawplus.getCanvas();
        
        drawcanvas.addMouseListener(this);
        drawcanvas.addMouseMotionListener(this);
        
        drawproc = drawplus.getProcessor();
        state = 0;
        
        points = new ArrayList<int[]>();
	}
    
    public void mouseClicked(MouseEvent evt){
        
        
        int x = drawcanvas.offScreenX(evt.getX());
        int y = drawcanvas.offScreenY(evt.getY());
        
        //gets the various points
        switch(state){
            case 0:
                drawproc.setColor(Color.BLUE);
                drawproc.drawOval(x -10, y - 10, 20, 20);
                addPoint(x,y);
                break;
            case 1:
                addPoint(x,y);
                drawproc.setColor(Color.BLUE);
                drawproc.drawOval(x -10, y - 10, 20, 20);
                drawproc.setColor(Color.GREEN);
                drawproc.drawLine(points.get(0)[0],points.get(0)[1],points.get(1)[0],points.get(1)[1]);
                break;
            case 2:
                addPoint(x,y);
                drawproc.setColor(Color.BLUE);
                drawproc.drawOval(x -10, y - 10, 20, 20);
                bufferproc = drawproc.duplicate();
                break;
            case 3:
                addPoint(x,y);
                drawproc.setColor(Color.BLUE);
                drawproc.drawOval(x -10, y - 10, 20, 20);
                drawproc.setColor(Color.GREEN);
                drawproc.drawLine(points.get(2)[0],points.get(2)[1],points.get(3)[0],points.get(3)[1]);
                break;
            default:
            }
                
        state = points.size();
        
        if(state==4)
            createOvula();
            
        drawcanvas.update(drawcanvas.getGraphics());

    }
    
    private void addPoint(int x, int y){
        int[] p = new int[2];
        p[0] = x;
        p[1] = y;
        points.add(p);
    }
    
    
    /**
      *     During 'state 3' we draw the bounding rectangle
      *     to show the area that will be cropped and rotated.
      *
      **/
    public void mouseMoved(MouseEvent evt){
        
        if(state==3){
            int x = drawcanvas.offScreenX(evt.getX());
            int y = drawcanvas.offScreenY(evt.getY());
            drawproc.copyBits(bufferproc,0,0,Blitter.COPY);
            
            int[] p1,p2,p3,p4;
            
            p1 = points.get(0);
            p2 = points.get(1);
            p3 = points.get(2);
            p4 = new int[] {x, y};
            //principle axis
            int lx = p2[0] - p1[0];
            int ly = p2[1] - p1[1];
            
            double lx_sq = Math.pow(lx,2);
            double ly_sq = Math.pow(ly,2);
            double length = Math.sqrt(lx_sq+ly_sq);
            
            int width = (int)length;        //integer value for creating new image dimensions
            
            //secondary axis
            double hx = p4[0] - p3[0];
            double hy = p4[1] - p3[1];
            
            double hx_sq = Math.pow(hx,2);
            double hy_sq = Math.pow(hy,2);
            
            //Finds the length of height_d by finding the projection of the chosen line along the image r1xr2 = |r1||r2|sin(theta)
            double height_d = Math.abs((hx*ly - hy*lx)/length);
            
            
            int height = (int)height_d;         //integer value for new image dimensions
            
            //angle that the principle axis makes with the horizontal positive is ccw trig functions only
            
            double sintheta = ly/length;
            double costheta = lx/length;
            
            int startx = (int)(p1[0] - sintheta*height_d/2);
            int starty = (int)(p1[1] + costheta*height_d/2);
            
            int endx = (int)(p1[0] + sintheta*height_d/2);
            int endy = (int)(p1[1] - costheta*height_d/2);
            
            
            drawproc.drawLine(startx,starty, endx, endy);
            drawproc.drawLine(endx,endy,endx + lx, endy + ly);
            drawproc.drawLine(endx + lx, endy + ly, startx + lx, starty + ly);
            drawproc.drawLine(startx + lx, starty + ly, startx, starty);
            
            drawcanvas.update(drawcanvas.getGraphics());
            
            
        }
    
    }
    
    
    
    /**
      *     This sizes out the rectangle and then gets the pixel values in the image then plots them onto a new image stack
      **/
    public void createOvula(){
    
        int[] p1,p2,p3,p4;
        
        p1 = points.get(0);
        p2 = points.get(1);
        p3 = points.get(2);
        p4 = points.get(3);
        //principle axis
        double lx = p2[0] - p1[0];
        double ly = p2[1] - p1[1];
        
        double lx_sq = Math.pow(lx,2);
        double ly_sq = Math.pow(ly,2);
        double length = Math.sqrt(lx_sq+ly_sq);
        
        int width = (int)length;        //integer value for creating new image dimensions
        
        //secondary axis
        double hx = p4[0] - p3[0];
        double hy = p4[1] - p3[1];
        
        double hx_sq = Math.pow(hx,2);
        double hy_sq = Math.pow(hy,2);
        
        //Finds the length of height_d by finding the projection of the chosen line along the image r1xr2 = |r1||r2|sin(theta)
        double height_d = Math.abs((hx*ly - hy*lx)/length);
        
        
        int height = (int)height_d;         //integer value for new image dimensions
        
        //angle that the principle axis makes with the horizontal positive is ccw trig functions only
        
        double sintheta = ly/length;
        double costheta = lx/length;
        
        double startx = p1[0] - sintheta*height_d/2;
        double starty = p1[1] + costheta*height_d/2;
        
        double[][] cnet_map = new double[height*width][2];
        
        
        
        //int c = Color.RED.getRGB();
        int c = 150<<16;
        for(int i = 0; i<height; i++){
            for(int j = 0; j<width; j++){
            
                //creates a map
                cnet_map[i*width + j][0] = startx + j*costheta + i*sintheta;
                cnet_map[i*width + j][1] = starty + j*sintheta - i*costheta;
                
                //Creates a tinted box of the pixels used some pixelation occurs here because the points are drawn as ints
                int x = (int)cnet_map[i*width + j][0];
                int y = (int)cnet_map[i*width + j][1];
                drawproc.putPixel(x,y,(drawproc.getPixel(x,y)|c));
            }        
        }
        if(imp.getType()==ImagePlus.COLOR_RGB)
            createCroppedImageRGB(width,height, cnet_map);
        else
            createCroppedImage(width,height, cnet_map);
        
        
        
    }
    
    /**
     *      Creates the rotated image by mapping the values located in 
     *      cnet_map to a new rectangular image.
     *      @param width width of new image
     *      @param height of new image
     *      @param cnet_map coordinate map.
     **/
    public void createCroppedImage(int width, int height, double[][] cnet_map){
        //old stack for getting processors
        ImageStack AB = imp.getStack();

        //new stack for creating new imageplus
        ImageStack outstack = new ImageStack(width,height);
        ImageProcessor cp;

        
        for(int k = 1; k<=AB.getSize(); k++){
            
            cp = AB.getProcessor(k);
            ImageProcessor np = cp.createProcessor(width,height);
            
            //This is the rotation algorithm, it maps values from the rotated rectangle to a non-rotated.
            for(int i = 0; i < height; i++){
                for(int j = 0; j < width; j++){
                    np.putPixelValue(j,height - 1 - i,cp.getInterpolatedPixel(cnet_map[i*width + j][0],cnet_map[i*width + j][1]));
                } 
            
            }
            
            String label = AB.getSliceLabel(k);
            outstack.addSlice(label,np);
        }
        

        createOutputPlus(outstack);
        points = new ArrayList<int[]>();
        
    }

    void createOutputPlus(ImageStack outstack){
        ImagePlus showme = imp.createImagePlus();
        showme.setStack(outstack);
        showme.setTitle("x1-" + imp.getTitle());
        showme.setDimensions(imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
        int dims = 0;
        if(imp.getNChannels()>1) dims++;
        if(imp.getNSlices()>1) dims++;
        if(imp.getNFrames()>1) dims++;

        if(dims>1) showme.setOpenAsHyperStack(true);
        showme.show();
    }
    
    /**
     *  
     *  RGB version that interpolates each pixel of the int array 
     *  separately, this uses bi-linear interpolation...I hope
     *  @param width width of new image
     *  @param height of new image
     *  @param cnet_map coordinate map.
     * */
    public void createCroppedImageRGB(int width, int height, double[][] cnet_map){
        //old stack for getting processors
        ImageStack AB = imp.getStack();

        //new stack for creating new imageplus
        ImageStack outstack = new ImageStack(width,height);
        ImageProcessor cp;

        
        for(int k = 1; k<=AB.getSize(); k++){
            
            cp = AB.getProcessor(k);
            ImageProcessor np = cp.createProcessor(width,height);
            
            int[] px = new int[3];
            int[] pxi = new int[3];
            int[] pxj = new int[3];
            int[] pxij = new int[3];
            
            int[] npx = new int[3];
            double tx, ty;
            //This is the rotation algorithm, it maps values from the rotated rectangle to a non-rotated.
            for(int i = 0; i < height; i++){
                for(int j = 0; j < width; j++){
                    
                    /** begin interpolating pixel, need to supply bound check */
                    int x = (int)cnet_map[i*width + j][0];
                    int y = (int)cnet_map[i*width + j][1];
                    
                    tx = cnet_map[i*width + j][0] - x;
                    ty = cnet_map[i*width + j][1] - y;
                    
                    px = cp.getPixel(x,y,px);
                    pxi = cp.getPixel(x+1,y,pxi);
                    pxj = cp.getPixel(x,y+1,pxj);
                    pxij = cp.getPixel(x+1,y+1,pxij);
                    
                    for(int l = 0; l < 3; l++)
                        npx[l] = (int)(
                            
                            (px[l] + tx*(pxi[l] - px[l]))*(1-ty) + (pxj[l] + (pxij[l] - pxj[l])*tx)*ty
                        );
                        
                    
                    np.putPixel(j,height - 1 - i,npx);
                } 
            
            }

            String label = AB.getSliceLabel(k);
            outstack.addSlice(label,np);
        }
        
        createOutputPlus(outstack);
        
        points = new ArrayList<int[]>();
        
    }
        
}
