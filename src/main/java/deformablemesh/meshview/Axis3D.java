package deformablemesh.meshview;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3f;
import snakeprogram3d.display3d.DataObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * User: msmith
 * Date: 8/8/13
 * Time: 7:52 AM
 */
public class Axis3D implements DataObject {
    BranchGroup bg;
    Color wireColor = Color.LIGHT_GRAY;
    double s = 1;
    List<LineDataObject> wires = new ArrayList<>();
    public Axis3D(){
        bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_DETACH);

        Transform3D transformx = new Transform3D();
        transformx.setTranslation(new Vector3f((float)s,0f,0f));

        Appearance a = new Appearance();

        a.setColoringAttributes(new ColoringAttributes(new Color3f(1f,0f,0f),ColoringAttributes.FASTEST));
        Sphere spherex = new Sphere((float)0.01, Sphere.GENERATE_NORMALS, 50, a);
        TransformGroup tgx = new TransformGroup(transformx);
        tgx.addChild(spherex);

        a = new Appearance();
        a.setColoringAttributes(new ColoringAttributes(new Color3f(0f,1f,0f),ColoringAttributes.FASTEST));
        Sphere spherey = new Sphere((float)0.01, Sphere.GENERATE_NORMALS, 50, a);
        Transform3D transformy = new Transform3D();
        transformy.setTranslation(new Vector3f(0f,(float)s,0f));
        TransformGroup tgy = new TransformGroup(transformy);
        tgy.addChild(spherey);

        a = new Appearance();
        a.setColoringAttributes(new ColoringAttributes(new Color3f(0f, 0f, 1f), ColoringAttributes.FASTEST));
        Sphere spherez = new Sphere((float)0.01, Sphere.GENERATE_NORMALS, 50, a);

        Transform3D transformz = new Transform3D();
        transformz.setTranslation(new Vector3f(0f, 0f, (float)s));
        TransformGroup tgz = new TransformGroup(transformz);
        tgz.addChild(spherez);

        bg.addChild(tgx);
        bg.addChild(tgy);
        bg.addChild(tgz);

        double[] positions = {
                -s, -s, -s,
                -s,  s, -s,
                 s,  s, -s,
                 s, -s, -s,
                -s, -s, s,
                -s,  s,  s,
                 s,  s, s,
                 s, -s, s,
                0, 0, 0,
                s, 0, 0,
                0, s, 0,
                0, 0, s
            };
        List<Node3D> nodes = new ArrayList<>();
        for(int i = 0; i<positions.length/3; i++){
            nodes.add(new Node3D(positions, i));
        }

        LineDataObject obj = new LineDataObject(Arrays.asList(nodes.get(0), nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(0)), 0.5f);
        float[] c = wireColor.getRGBComponents(new float[4]);
        obj.setColor(c[0], c[1], c[2]);
        bg.addChild(obj.getBranchGroup());
        obj = new LineDataObject(Arrays.asList(nodes.get(4), nodes.get(5), nodes.get(6), nodes.get(7), nodes.get(4)), 0.5f);
        obj.setColor(c[0], c[1], c[2]);
        bg.addChild(obj.getBranchGroup());
        for(int i = 0; i<4; i++){
            obj = new LineDataObject(Arrays.asList(nodes.get(i), nodes.get(i+4)), 0.5f);
            obj.setColor(c[0], c[1], c[2]);
            bg.addChild(obj.getBranchGroup());
        }

        obj = new LineDataObject(Arrays.asList(nodes.get(8), nodes.get(9)), 0.5f);
        obj.setColor(c[0], c[1], c[2]);
        bg.addChild(obj.getBranchGroup());
        obj = new LineDataObject(Arrays.asList(nodes.get(8), nodes.get(10)), 0.5f);
        obj.setColor(c[0], c[1], c[2]);
        bg.addChild(obj.getBranchGroup());
        obj = new LineDataObject(Arrays.asList(nodes.get(8), nodes.get(11)), 0.5f);
        obj.setColor(c[0], c[1], c[2]);
        bg.addChild(obj.getBranchGroup());

    }

    public void setWireColor(Color c){
        wireColor = c;
        float[] comps = wireColor.getRGBComponents(new float[4]);
        for(LineDataObject ldo: wires){
            ldo.setColor(comps[0], comps[1], comps[2]);
        }
    }

    @Override
    public BranchGroup getBranchGroup() {
        return bg;
    }
}
