package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.IndexedLineArray;
import org.scijava.java3d.IndexedTriangleArray;
import org.scijava.java3d.Material;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.TexCoordGeneration;
import org.scijava.java3d.Texture3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Quat4d;
import org.scijava.vecmath.Vector3d;

import java.awt.Color;


/**
 * Date: 8/5/13
 * To change this template use File | Settings | File Templates.
 */
public class FurrowPlaneDataObject implements DataObject {
    BranchGroup branchGroup;
    TransformGroup transformGroup;

    IndexedLineArray lines;
    IndexedTriangleArray surface_front;
    IndexedTriangleArray surface_back;
    double[] positions;


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


        int[] connection_index = new int[]{
                0,1,
                1,2,
                2,3,
                3,0
        };

        int[] triangle_index_front = new int[]{
                0, 2, 1,
                0, 3, 2
        };

        int[] triangle_index_back = new int[]{
                0, 1, 2,
                0, 2, 3
        };

        lines.setCoordinates(0,positions);
        lines.setCoordinateIndices(0, connection_index);
        lines.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

        surface_front = new IndexedTriangleArray(4,GeometryArray.COORDINATES, 6);
        surface_front.setCoordinates(0, positions);
        surface_front.setCoordinateIndices(0, triangle_index_front);

        surface_back = new IndexedTriangleArray(4,GeometryArray.COORDINATES, 6);
        surface_back.setCoordinates(0, positions);
        surface_back.setCoordinateIndices(0, triangle_index_back);

        Shape3D front_shape = new Shape3D(surface_front);
        front_shape.setAppearance(createFront());

        Shape3D back_shape = new Shape3D(surface_back);
        back_shape.setAppearance(createBack());

        branchGroup = new BranchGroup();
        branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
        branchGroup.setCapability(BranchGroup.ALLOW_PICKABLE_READ);
        branchGroup.setCapability(BranchGroup.ALLOW_PICKABLE_WRITE);
        transformGroup = new TransformGroup();
        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        transformGroup.addChild(new Shape3D(lines));
        transformGroup.addChild(front_shape);
        transformGroup.addChild(back_shape);

        branchGroup.addChild(transformGroup);

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
        Transform3D tt = new Transform3D();
        Vector3d  norm = new Vector3d(normal);
        Vector3d x = new Vector3d();
        x.cross(UP,norm);
        double cos = UP.dot(norm);
        double st2 = Math.sqrt(0.5 - cos*0.5);
        double ct2 = Math.sqrt(0.5 + cos*0.5);

        if (x.lengthSquared()!=0) {
            x.normalize();
        }else{
            x.cross(new Vector3d(new double[]{0,1,0}), norm);
        }



        Quat4d rot = new Quat4d(new double[]{x.x*st2, x.y*st2, x.z*st2, ct2});
        tt.setRotation(rot);
        tt.setTranslation(new Vector3d(cm));
        transformGroup.setTransform(tt);
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
