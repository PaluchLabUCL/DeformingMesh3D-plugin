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
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Quat4d;
import org.scijava.vecmath.Vector3d;


/**
 * Date: 8/5/13
 */
public class FurrowPlaneDataObject implements DataObject {
    final static int[] triangle_index_front = new int[]{
            0, 2, 1,
            0, 3, 2
    };
    final static int[] triangle_index_back = new int[]{
            0, 1, 2,
            0, 2, 3
    };
    final static int[] connection_index = new int[]{
            0,1,
            1,2,
            2,3,
            3,0
    };

    BranchGroup branchGroup;

    IndexedLineArray lines;
    IndexedTriangleArray surface_front;
    IndexedTriangleArray surface_back;
    double[] positions;

    Shape3D front_shape, back_shape, line_shape;

    public FurrowPlaneDataObject(double[] cm, double[] normal){
        this(cm, normal, 0.125);
    }
    public FurrowPlaneDataObject(double[] cm, double[] normal, double length) {
        lines = new IndexedLineArray(4, GeometryArray.COORDINATES, 8 );
        positions = new double[]{
                -length,-length,0,
                length,-length,0,
                length,length,0,
                -length,length,0

        };

        lines.setCoordinates(0,positions);
        lines.setCoordinateIndices(0, connection_index);
        lines.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

        surface_front = new IndexedTriangleArray(4,GeometryArray.COORDINATES, 6);
        surface_front.setCoordinates(0, positions);
        surface_front.setCoordinateIndices(0, triangle_index_front);
        surface_front.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

        surface_back = new IndexedTriangleArray(4,GeometryArray.COORDINATES, 6);
        surface_back.setCoordinates(0, positions);
        surface_back.setCoordinateIndices(0, triangle_index_back);
        surface_back.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

        front_shape = new Shape3D(surface_front);
        front_shape.setAppearance(createFront());
        front_shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);

        line_shape = new Shape3D(lines);
        line_shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);

        back_shape = new Shape3D(surface_back);
        back_shape.setAppearance(createBack());
        back_shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);

        branchGroup = new BranchGroup();
        branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
        branchGroup.setCapability(BranchGroup.ALLOW_PICKABLE_READ);
        branchGroup.setCapability(BranchGroup.ALLOW_PICKABLE_WRITE);

        branchGroup.addChild(line_shape);
        branchGroup.addChild(front_shape);
        branchGroup.addChild(back_shape);

        updatePosition(cm, normal);

    }

    /**
     *  Moves the position of the visible plane.
     *
     * @param cm
     * @param normal
     */
    private final static Vector3d UP = new Vector3d(new double[]{0,0,1});
    public void updatePosition(double[] cm, double[] normal){
        synchronized(positions) {
            Transform3D tt = new Transform3D();
            Vector3d norm = new Vector3d(normal);
            Vector3d x = new Vector3d();
            x.cross(UP, norm);
            double cos = UP.dot(norm);
            double st2 = Math.sqrt(0.5 - cos * 0.5);
            double ct2 = Math.sqrt(0.5 + cos * 0.5);

            if (x.lengthSquared() != 0) {
                x.normalize();
            } else {
                x.cross(new Vector3d(new double[]{0, 1, 0}), norm);
            }


            Quat4d rot = new Quat4d(new double[]{x.x * st2, x.y * st2, x.z * st2, ct2});
            tt.setRotation(rot);
            tt.setTranslation(new Vector3d(cm));

            double[] tty = new double[positions.length];
            for (int i = 0; i < 4; i++) {
                Point3d point = new Point3d(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]);
                tt.transform(point);
                tty[3 * i] = point.x;
                tty[3 * i + 1] = point.y;
                tty[3 * i + 2] = point.z;
            }


            surface_front.setCoordinates(0, tty);
            front_shape.setGeometry(surface_front);
            surface_back.setCoordinates(0, tty);
            back_shape.setGeometry(surface_back);
            lines.setCoordinates(0, tty);
            line_shape.setGeometry(lines);
        }
    }

    public Shape3D getFrontShape(){
        return front_shape;
    }

    private Appearance createFront(){
        Appearance a = new Appearance();
        ColoringAttributes c_at = new ColoringAttributes(1f, 0f, 0f, ColoringAttributes.NICEST);
        a.setColoringAttributes(c_at);
        return a;

    }

    private Appearance createBack(){
        Appearance a = new Appearance();
        ColoringAttributes c_at = new ColoringAttributes(0f, 0f, 1f, ColoringAttributes.NICEST);
        a.setColoringAttributes(c_at);
        return a;

    }
    @Override
    public BranchGroup getBranchGroup() {
        return branchGroup;
    }
}
