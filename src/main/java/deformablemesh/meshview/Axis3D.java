package deformablemesh.meshview;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.DeformableMesh3D;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3f;
import snakeprogram3d.display3d.DataObject;


/**
 * User: msmith
 * Date: 8/8/13
 * Time: 7:52 AM
 */
public class Axis3D implements DataObject {
    BranchGroup bg;

    public Axis3D(){
        bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_DETACH);

        Transform3D transformx = new Transform3D();
        transformx.setTranslation(new Vector3f(1f,0f,0f));

        Appearance a = new Appearance();

        a.setColoringAttributes(new ColoringAttributes(new Color3f(1f,0f,0f),ColoringAttributes.FASTEST));
        Sphere spherex = new Sphere((float)0.01, Sphere.GENERATE_NORMALS, 50, a);
        TransformGroup tgx = new TransformGroup(transformx);
        tgx.addChild(spherex);

        a = new Appearance();
        a.setColoringAttributes(new ColoringAttributes(new Color3f(0f,1f,0f),ColoringAttributes.FASTEST));
        Sphere spherey = new Sphere((float)0.01, Sphere.GENERATE_NORMALS, 50, a);
        Transform3D transformy = new Transform3D();
        transformy.setTranslation(new Vector3f(0f,1f,0f));
        TransformGroup tgy = new TransformGroup(transformy);
        tgy.addChild(spherey);

        a = new Appearance();
        a.setColoringAttributes(new ColoringAttributes(new Color3f(0f, 0f, 1f), ColoringAttributes.FASTEST));
        Sphere spherez = new Sphere((float)0.01, Sphere.GENERATE_NORMALS, 50, a);

        Transform3D transformz = new Transform3D();
        transformz.setTranslation(new Vector3f(0f, 0f, 1f));
        TransformGroup tgz = new TransformGroup(transformz);
        tgz.addChild(spherez);

        bg.addChild(tgx);
        bg.addChild(tgy);
        bg.addChild(tgz);

        DeformableMesh3D block = DeformableMesh3DTools.createRectangleMesh(0.02, 0.02, 0.02, 0.01);
        block.translate(new double[]{0.01, 0.01, 0.01});
        block.create3DObject();
        bg.addChild(block.data_object.getBranchGroup());

    }

    @Override
    public BranchGroup getBranchGroup() {
        return bg;
    }
}
