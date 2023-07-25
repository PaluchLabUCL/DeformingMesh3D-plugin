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

import org.scijava.java3d.*;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Created by msmith on 2/8/16.
 */
public class SphereDataObject implements DataObject {
    TransformGroup tg;
    BranchGroup BG;
    double radius;
    double[] position;

    Appearance appearance;
    /**
     *     Creates a new moveable sphere at the origin with radius size
     *     @param center location of sphere.
     *     @param size     determines the radius of your sphere
     **/


    public SphereDataObject(double[] center, double size ){

        Appearance a = createAppearance();
        Sphere sphere = new Sphere(1, Sphere.GENERATE_NORMALS, 50, a);
        appearance = a;
        sphere.setPickable(false);

        Transform3D tt = new Transform3D();
        //tt.setTranslation(new Vector3d(1.0,0.0,0.0));
        tt.setScale(size);
        tt.setTranslation(new Vector3d(center[0], center[1], center[2]));
        tg = new TransformGroup(tt);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg.addChild(sphere);
        position = center;
        radius = size;
    }

    public Appearance createAppearance(){
        Color3f eColor    = new Color3f(0.0f, 0.0f, 0.0f);
        Color3f sColor    = new Color3f(1.0f, 1.0f, 1.0f);
        Color3f objColor  = new Color3f(0.6f, 0.6f, 0.6f);

        Material m = new Material(objColor, eColor, objColor, sColor, 100.0f);
        Appearance a = new Appearance();


        m.setLightingEnable(true);
        a.setMaterial(m);
        a.setTransparencyAttributes(
                new TransparencyAttributes(TransparencyAttributes.FASTEST, 0.2f)
        );
        return a;
    }



    public void moveTo(Point3d p){
        moveTo(p.x, p.y, p.z);
    }

    public void setRadius(double r){
        radius = r;
        update();
    }

    public void moveTo(double x, double y, double z){
        position[0] = x; position[1]=y; position[2]=z;
        //get old transform
        update();
    }

    public void update(){
        Transform3D tt = new Transform3D();
        tg.getTransform(tt);

        Vector3d n = new Vector3d(position[0], position[1], position[2]);
        tt.setScale(radius);
        tt.setTranslation(n);

        tg.setTransform(tt);
    }




    public BranchGroup getBranchGroup(){
        if(BG==null){
            BG = new BranchGroup();
            BG.setCapability(BranchGroup.ALLOW_DETACH);
            BG.addChild(tg);
        }
        return BG;
    }

    public static void main(String[] args){
        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        SphereDataObject obj = new SphereDataObject(new double[]{0,0,0}, 1.0 );
        frame.addDataObject(obj);
        frame.addKeyListener(new KeyListener() {
            double radius = 1;
            double x = 0;
            double y = 0;
            double z = 0;

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_EQUALS:
                        radius *= 2;
                        obj.setRadius(radius);
                        break;
                    case KeyEvent.VK_MINUS:
                        radius /= 2;
                        obj.setRadius(radius);
                        break;
                    case KeyEvent.VK_LEFT:
                        x -= 0.1;
                        obj.moveTo(x,y,z);
                        break;
                    case KeyEvent.VK_RIGHT:
                        x += 0.1;
                        obj.moveTo(x,y,z);
                        break;
                    case KeyEvent.VK_UP:
                        y += 0.1;
                        obj.moveTo(x,y,z);
                        break;
                    case KeyEvent.VK_DOWN:
                        y -= 0.1;
                        obj.moveTo(x,y,z);
                        break;
                    default:
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
    }

    public void setColor(float r, float g, float b) {
        Material material = appearance.getMaterial();
        material.setEmissiveColor(r, g, b);
        //appearance.setColoringAttributes();
    }
}
