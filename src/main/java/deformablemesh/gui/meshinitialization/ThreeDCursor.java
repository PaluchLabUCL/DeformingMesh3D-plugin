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
package deformablemesh.gui.meshinitialization;

import deformablemesh.gui.Drawable;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.Vector3DOps;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by smithm3 on 24/05/18.
 */
public class ThreeDCursor{
    double[] xyz = new double[3];
    double lx, ly, lz;
    List<Runnable> notify = new ArrayList<>();
    boolean drawing = true;
    public ThreeDCursor(double lx, double ly, double lz){
        this.lx = lx;
        this.ly = ly;
        this.lz = lz;
    }

    public void setPosition(double x, double y, double z){
        xyz[0] = x;
        xyz[1] = y;
        xyz[2] = z;
    }


    public Shape getProjection(FurrowTransformer transformer) {
        double[] A = {-lx/2, xyz[1], xyz[2]};
        double[] B = {lx/2, xyz[1], xyz[2]};
        double[] C = {xyz[0], -ly/2, xyz[2]};
        double[] D = {xyz[0], ly/2, xyz[2]};
        double[] E = {xyz[0], xyz[1], -lz/2};
        double[] F = {xyz[0], xyz[1], lz/2};

        Path2D path = new Path2D.Double();
        double[] pt = transformer.getPlaneCoordinates(A);
        path.moveTo(pt[0], pt[1]);
        pt = transformer.getPlaneCoordinates(B);
        path.lineTo(pt[0], pt[1]);
        pt = transformer.getPlaneCoordinates(C);
        path.moveTo(pt[0], pt[1]);
        pt = transformer.getPlaneCoordinates(D);
        path.lineTo(pt[0], pt[1]);
        pt = transformer.getPlaneCoordinates(E);
        path.moveTo(pt[0], pt[1]);
        pt = transformer.getPlaneCoordinates(F);
        path.lineTo(pt[0], pt[1]);
        return path;
    }

    public void setVisible(boolean visible){
        drawing = visible;
    }

    Drawable getDrawable(FurrowTransformer transformer){
        return g2d-> {
            if(drawing) {
                g2d.setColor(Color.WHITE);
                g2d.draw(getProjection(transformer));
            }
        };
    }

    void addNotification(Runnable r){
        notify.add(r);
    }

    /**
     * Moves to the new normalized coordinates only changing the direction along the normal.
     *
     * @param f
     * @param normal
     */
    public void toPosition(double f, double[] normal) {
        double dot = Vector3DOps.dot(normal, xyz);
        double[] scaled = {normal[0]*dot, normal[1]*dot, normal[2]*dot};
        double[] p = Vector3DOps.difference(xyz, scaled);
        xyz[0] = p[0] + f*normal[0];
        xyz[1] = p[1] + f*normal[1];
        xyz[2] = p[2] + f*normal[2];
        notify.forEach(Runnable::run);
    }
}
