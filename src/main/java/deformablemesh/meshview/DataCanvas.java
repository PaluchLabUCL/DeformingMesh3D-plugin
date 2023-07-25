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
package deformablemesh.meshview;

import deformablemesh.util.Vector3DOps;
import org.scijava.java3d.*;
import org.scijava.java3d.utils.picking.PickCanvas;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.java3d.utils.picking.PickTool;
import org.scijava.java3d.utils.universe.SimpleUniverse;
import org.scijava.java3d.utils.universe.Viewer;
import org.scijava.java3d.utils.universe.ViewingPlatform;
import org.scijava.vecmath.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 * Shows the 3d scenes
 */
public class DataCanvas extends Canvas3D {
    static class Camera{
        double ox, oy, oz;
        double radius = 1;
        AxisAngle4d aa;
        public void zoomIn(){
            radius = radius*0.9875;
        }
        public void zoomOut(){
            radius = radius*1.0125;
        }
    }
    Camera camera = new Camera();
    public static interface ViewListener{
        public void viewUpdated();
    }
    List<ViewListener> viewListeners = new ArrayList<>();

    SimpleUniverse universe;

    BoundingSphere bounds;
    BranchGroup group;

    private PickCanvas pickCanvas;
    
    //double ZOOM = 1;
    //AxisAngle4d aa = new AxisAngle4d(0, 0, 1, 0);

    //double DX = 0;
    //double DY = 0;

    List<CanvasView> viewers = new ArrayList<>();
    
    Color3f backgroundColor = new Color3f(0f,0f,0f);
    Background background;
    CanvasController controller;
    final GraphicsConfiguration gc;
    private OffScreenCanvas3D offscreen;

    public DataCanvas(GraphicsConfiguration gc,Color3f back){
        super(gc,false);
        backgroundColor = back;
        this.gc = gc;
        createUniverse();
    }


    public DataCanvas(GraphicsConfiguration gc){
        super(gc,false);
        this.gc = gc;
        createUniverse();
    }

    void createUniverse(){
        universe = new SimpleUniverse(this);

        universe.getViewingPlatform().setNominalViewingTransform();
        universe.getViewer().getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

        group = new BranchGroup();
        
        group.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        group.setCapability(Group.ALLOW_CHILDREN_WRITE);
        bounds =  new BoundingSphere(new Point3d(0.0,0.0,0.0), 10000.0);

        background = new Background();
        background.setCapability(Background.ALLOW_COLOR_WRITE);
        background.setColor(backgroundColor);
        background.setApplicationBounds(bounds);
        group.addChild(background);

        universe.addBranchGraph(group);
        universe.getViewer().getView().setMinimumFrameCycleTime(5);
        controller = new CanvasController(this);
        
        pickCanvas = new PickCanvas(this, group);
        pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
        setView(StationaryViews.THREEQUARTER);
        Rectangle r = getBounds();
        setBounds(0, 0, (int)r.getWidth()*2, (int)r.getHeight()*2);
    }

    public void setDefaultControllerEnabled(boolean enabled){
        controller.setEnabled(enabled);
    }

    /**
     * Adds a "DataObject" which is just an interface for adding a branch group.
     *
     * @param a the data object.
     */
    public void addObject(DataObject a){
        group.addChild(a.getBranchGroup());
    }
    
    
    public void zoomIn(){
        double before = camera.radius;
        camera.zoomIn();

        if(before >= 0.5 && camera.radius < 0.5){
            Viewer viewer = universe.getViewer();
            View view = viewer.getView();
            view.setFrontClipDistance(0.01);
            view.setBackClipDistance(1.0);
        }

        updateView();
    }
    
    public void zoomOut(){
        double before = camera.radius;
        camera.zoomOut();
        if(before <= 1.0 && camera.radius > 1.0){
            Viewer viewer = universe.getViewer();
            View view = viewer.getView();
            view.setFrontClipDistance(0.1);
            view.setBackClipDistance(10.0);
        }
        updateView();
    }

    public void twistView(int dz){
        double rate = 0.005;
        Vector4d q1 = axisAngleToQuarternion(camera.aa);
        Vector4d q2 = axisAngleToQuarternion(new AxisAngle4d(0, 0, 1, -rate*dz));
        camera.aa = new AxisAngle4d(quarternionToAxisAngle(multiplyQuarternions(q1,q2)));

        updateView();
    }

    /**
     *  Rotates the view using an axis angle. The rotation is in the camera cooridnates. Eg. {0, 0, 1, 0.01} would be
     *  about the axis forward.
     *
     * @param axisAngle
     */
    public void rotateView(double[] axisAngle){
        double[] four = new double[4];
        if(axisAngle.length == 3){
            double m = Vector3DOps.normalize(axisAngle);
            four[0] = axisAngle[0];
            four[1] = axisAngle[1];
            four[2] = axisAngle[2];
            four[3] = m;
        } else{
            four[0] = axisAngle[0];
            four[1] = axisAngle[1];
            four[2] = axisAngle[2];
            four[3] = axisAngle[3];
        }
        //view coordinates left x up = forward.

        double[] n = new double[]{four[0], four[1], four[2]};
        double[] up = getUp();
        double[] forward = getForward();
        double[] left = Vector3DOps.cross(up, forward);

        four[0] = Vector3DOps.dot(left, n);
        four[1] = Vector3DOps.dot(up, n);
        four[2] = Vector3DOps.dot(forward, n);

        Vector4d q1 = axisAngleToQuarternion(camera.aa);
        AxisAngle4d rotation = new AxisAngle4d(four);
        Vector4d q2 = axisAngleToQuarternion(rotation);
        camera.aa = new AxisAngle4d(quarternionToAxisAngle(multiplyQuarternions(q1,q2)));
        updateView();
    }

    public void rotateView(int dx,int dy){

        int mx = dx<0?-dx:dx;
        int my = dy<0?-dy:dy;

        if(mx > 3*my){
            dy = 0;
        }
        if(my>3*mx){
            dx = 0;
        }

        Vector4d q1 = axisAngleToQuarternion(camera.aa);
        double rate = 0.005;
        Vector4d q2;
        if(dx!=0 && dy!=0){
            q2 = axisAngleToQuarternion(new AxisAngle4d(0, 1, 0, -rate*dx));
            q2 = multiplyQuarternions(q2, axisAngleToQuarternion(new AxisAngle4d(1, 0, 0, -rate*dy)));
        } else if(dx == 0){
            q2 = axisAngleToQuarternion(new AxisAngle4d(1, 0, 0, -rate*dy));
        } else{
            q2 = axisAngleToQuarternion(new AxisAngle4d(0, 1, 0, -rate*dx));
        }

        camera.aa = new AxisAngle4d(quarternionToAxisAngle(multiplyQuarternions(q1,q2)));


        updateView();

    }

    public void centerCamera( double[] pt){
        camera.ox = pt[0];
        camera.oy = pt[1];
        camera.oz = pt[2];
        updateView();
    }

    public void translateView(int dx, int dy){
        Transform3D rot = new Transform3D();
        rot.setRotation(camera.aa);
        double rate = 1.0/64.0;
        Vector3d v3d = new Vector3d(-dx*rate, dy*rate, 0);

        rot.transform(v3d);

        camera.ox += v3d.x;
        camera.oy += v3d.y;
        camera.oz += v3d.z;

        updateView();

    }

    /**
     * Removes an object if it exists.
     *
     * @param obj object of interest 
     */
    synchronized public void removeObject(DataObject obj){
    
        group.removeChild(obj.getBranchGroup());
        obj.getBranchGroup().detach();
    
    }

    public void addViewListener(ViewListener l){
        viewListeners.add(l);
    }

    public void removeViewListener(ViewListener l){
        viewListeners.remove(l);
    }

    private void updateView(){
        TransformGroup ctg = universe.getViewingPlatform().getViewPlatformTransform();



        Vector3d displace = new Vector3d(0,0,camera.radius);
        Transform3D rot = new Transform3D();
        rot.setRotation(camera.aa);
        rot.transform(displace);
        displace.add(new Vector3d(camera.ox, camera.oy, camera.oz));
        rot.setTranslation(displace);
        ctg.setTransform(rot);
        viewListeners.forEach(ViewListener::viewUpdated);
    }

    public void debugOrientation(){
        TransformGroup ctg = universe.getViewingPlatform().getViewPlatformTransform();
        Transform3D transform = new Transform3D();
        ctg.getTransform(transform);
        Vector3d z = new Vector3d(0,0,1);
        Vector3d y = new Vector3d(0,1,0);
        Vector3d x = new Vector3d(1,0,0);
        transform.transform(z);
        transform.transform(y);
        transform.transform(x);
        System.out.println("Towards user: " + z);
        System.out.println("Up: " + y);
        System.out.println("Right: " + x);
    }

    /**
     * Adding a snake listener sets 'picking' events where using the mouse on the 3d view can
     * cause interactions.
     *
     * @param cv the displayed view that will be interacted with.
     */
    public void addSnakeListener(CanvasView cv){
        viewers.add(cv);
    }

    public void removeSnakeListener(CanvasView cv){
        viewers.remove(cv);
    }

    /**
     *  Gets the 'results' a pick result and send the results on down
     *  the line
     *
     **/
    public void clicked(MouseEvent evt){
        if(viewers.size()>0){
            pickCanvas.setShapeLocation(evt);
            PickResult[] results = pickCanvas.pickAllSorted();
            if(results == null){
                results = new PickResult[0];
            }
            for(CanvasView viewer: viewers){
                viewer.updateClicked(results, evt);
            }
        }
    }

    public void dragged(MouseEvent evt){
        if(viewers.size()>0){
            pickCanvas.setShapeLocation(evt);


            PickResult[] results = pickCanvas.pickAllSorted();
            if(results == null){
                results = new PickResult[0];
            }
            for(CanvasView viewer: viewers){
                viewer.updateDragged(results, evt);
            }
        }
    }

    public void pressed(MouseEvent evt){
        if(viewers.size()>0){
            pickCanvas.setShapeLocation(evt);


            PickResult[] results = pickCanvas.pickAllSorted();
            if(results == null){
                results = new PickResult[0];
            }
            for(CanvasView viewer: viewers){
                viewer.updatePressed(results, evt);
            }
        }
    }

    public void released(MouseEvent evt){
        if(viewers.size()>0){
            pickCanvas.setShapeLocation(evt);


            PickResult[] results = pickCanvas.pickAllSorted();
            if(results == null){
                results = new PickResult[0];
            }
            for(CanvasView viewer: viewers){
                viewer.updateReleased(results, evt);
            }
        }
    }

    public void moved(MouseEvent evt){
        if(viewers.size()>0){
            pickCanvas.setShapeLocation(evt);


            PickResult[] results = pickCanvas.pickAllSorted();
            if(results == null){
                results = new PickResult[0];
            }
            for(CanvasView viewer: viewers){
                viewer.updateMoved(results, evt);
            }
        }
    }

        
        /**
         * Gets the best graphics configuration to display on the current device.
         * 
         * @param frame frame that you want to add a Canvas3d to
         * @return a graphics configuration on the current display.
         */
        public static GraphicsConfiguration getBestConfigurationOnSameDevice(Window frame){
            
            GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            GraphicsConfiguration good = null;

            GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();

            for(GraphicsDevice gd: gs){

                if(gd==gc.getDevice()){
                    good = gct.getBestConfiguration(gd.getConfigurations());
                    if(good!=null)
                        break;

                }
            }



        return good;
    }

    public void createOffscreenCanvas(){
        offscreen = new OffScreenCanvas3D(gc, true);
        Screen3D screen = getScreen3D();
        Screen3D off = offscreen.getScreen3D();
        Dimension dim = screen.getSize();
        off.setSize(dim);
        off.setPhysicalScreenWidth(screen.getPhysicalScreenWidth());
        off.setPhysicalScreenHeight(screen.getPhysicalScreenHeight());
        universe.getViewer().getView().addCanvas3D(offscreen);
    }

    public void destroyOffscreenCanvas(){
        universe.getViewer().getView().removeCanvas3D(offscreen);
    }

    public BufferedImage snapShot(){
            if(offscreen == null){
                createOffscreenCanvas();
            }
        BufferedImage img = offscreen.doRender(getWidth(), getHeight());
        return img;
    }



    public void changeBackgroundColor(Color color){

        backgroundColor = new Color3f(color);
        background.setColor(backgroundColor);

    }

    public Color getCanvasBackgroundColor(){
            return new Color(backgroundColor.x, backgroundColor.y, backgroundColor.z);
    }

    public void setView(StationaryViews view){
        switch(view){
            case XY:
                camera.aa = new AxisAngle4d(0, 0, 1, 0);
                break;
            case XZ:
                camera.aa = new AxisAngle4d(1, 0, 0, Math.PI/2);
                break;
            case YZ:
                double l = Math.sqrt(3)/3;
                camera.aa = new AxisAngle4d(l, l, l, 2*Math.PI/3);
                break;
            case THREEQUARTER:
                camera.aa = new AxisAngle4d(0.20032220429878106, 0.5947418035684883, 0.7785584124219587, 2.6296664043138676);
                break;
        }

        camera.radius = 3;
        camera.ox = 0;
        camera.oy = 0;
        camera.oz = 0;

        updateView();
    }
    double[] quarternionToAxisAngle(Vector4d q){
            double theta = Math.acos(q.w)*2;
            double x, y, z;
            if(theta<0.0001){
                x = 0;
                y = 0;
                z = 1;
                theta = 0;
            } else{
                double s = Math.sqrt(1 - q.w*q.w);
                x = q.x/s;
                y = q.y/s;
                z = q.z/s;

            }

            return new double[]{x, y, z, theta};


    }

    /**
     * All of the current view values.
     *
     * @return DX, DY, ZOOM, aa.x, aa.y, aa.z, aa.angle
     *
     */
    public double[] getViewParameters(){
        return new double[] {
                camera.ox, camera.oy, camera.oz, camera.radius,
                camera.aa.x, camera.aa.y, camera.aa.z, camera.aa.angle
        };
    }

    /**
     * Sets the view based on the parameters.
     *
     * @param view ox, oy, oz, ZOOM, aa.x, aa.y, aa.z, aa.angle
     */
    public void setViewParameters(double[] view){
        camera.ox  = view[0];
        camera.oy  = view[1];
        camera.oz = view[2];
        camera.radius = view[3];
        camera.aa = new AxisAngle4d( view[4], view[5], view[6], view[7]);
        updateView();
    }

    Vector4d axisAngleToQuarternion(AxisAngle4d aa){
        double s = Math.sin(aa.angle/2);
        double c = Math.cos(aa.angle/2);

        return new Vector4d(aa.x*s, aa.y*s, aa.z*s, c);

    }
    Vector4d multiplyQuarternions(Vector4d q1, Vector4d q2){
        double qw = q1.w*q2.w - q1.x*q2.x - q1.y*q2.y - q1.z*q2.z;
        double qx = q1.x*q2.w + q1.w*q2.x + q1.y*q2.z - q1.z*q2.y;
        double qy = q1.w*q2.y - q1.x*q2.z + q1.y*q2.w + q1.z*q2.x;
        double qz = q1.w*q2.z + q1.x*q2.y - q1.y*q2.x + q1.z*q2.w;

        return new Vector4d(qx, qy, qz, qw);
    }
    /**
     * Rotates the view such that the new view will be facing towards the normal.
     *
     * @param normal
     */
    public void lookTowards(double[] normal, double[] up){
        TransformGroup ctg = universe.getViewingPlatform().getViewPlatformTransform();
        //ctg.getTransform(transform);

        Vector3d n = new Vector3d(normal);
        Vector3d vup = new Vector3d(up);
        vup.normalize();

        double dot = n.dot(vup);

        Vector3d v = new Vector3d(
                vup.x - dot*n.x,
                vup.y - dot*n.y,
                vup.z - dot*n.z
        );

        v.normalize();
        Vector3d u = new Vector3d();
        u.cross(v, n);

        Matrix3d matrix = new Matrix3d();
        matrix.setColumn(0, u);
        matrix.setColumn(1, v);
        matrix.setColumn(2, n);


        //we want to rotate our view such that the normal is back towards us.
        camera.aa = new AxisAngle4d();
        camera.aa.set(matrix);

        updateView();


    }

    /**
     * Use the original rotation and change the view
     *
     * @param dx
     * @param dy
     */
    public void pivotAboutCenterOfView(int dx,int dy){

    }

    /**
     * The up vector is a vector that would move in the y direction on the current display.
     *
     * @return {x, y, z} representing the current up vector.
     */
    public double[] getUp(){
        TransformGroup ctg = universe.getViewingPlatform().getViewPlatformTransform();
        Transform3D transform = new Transform3D();
        ctg.getTransform(transform);
        Vector3d up = new Vector3d(0, 1, 0);
        transform.transform(up);
        return new double[]{up.x, up.y, up.z};
    }

    public double[] getForward(){
        TransformGroup ctg = universe.getViewingPlatform().getViewPlatformTransform();
        Transform3D transform = new Transform3D();
        ctg.getTransform(transform);
        Vector3d forward = new Vector3d(0, 0, -1);
        transform.transform(forward);
        return new double[]{forward.x, forward.y, forward.z};
    }

}


class OffScreenCanvas3D extends Canvas3D {
    ImageComponent2D buffer;
    OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
                      boolean offScreen) {

        super(graphicsConfiguration, offScreen);

    }

    BufferedImage doRender(int width, int height) {
        if(buffer == null) {
            //This means, the size of the buffer will only be set the first time.
            //due to a bug.
            BufferedImage bImage = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            buffer = new ImageComponent2D(
                    ImageComponent.FORMAT_RGBA, bImage);
            setOffScreenBuffer(buffer);
        }

        renderOffScreenBuffer();
        waitForOffScreenRendering();
        BufferedImage r = getOffScreenBuffer().getImage();
        return r;
    }

    public void postSwap() {
        // No-op since we always wait for off-screen rendering to complete
    }


}
