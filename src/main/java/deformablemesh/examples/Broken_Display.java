package deformablemesh.examples;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import org.scijava.java3d.*;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.java3d.utils.universe.SimpleUniverse;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;

import javax.swing.*;
import java.awt.*;

public class Broken_Display implements PlugIn{
    public static void main(String[] args){

        new Broken_Display().run("what");
    }

    @Override
    public void run(String s) {
        ImageJ ij = IJ.getInstance();
        JDialog log = new JDialog(ij);
        Canvas3D c3d = new Canvas3D(SimpleUniverse.getPreferredConfiguration());

        SimpleUniverse universe = new SimpleUniverse(c3d);
        Background background = new Background(new Color3f(0.4f, 0.1f, 0.05f));
        BranchGroup grp = new BranchGroup();
        //gb.addChild(new Sphere());

        universe.getViewingPlatform().setNominalViewingTransform();
        universe.getViewer().getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

        BoundingSphere bounds =  new BoundingSphere(new Point3d(0.0,0.0,0.0), 10000.0);

        background.setApplicationBounds(bounds);
        grp.addChild(background);

        Sphere sphere = new Sphere(1, Sphere.GENERATE_NORMALS, 50, new Appearance());
        grp.addChild(sphere);

        universe.addBranchGraph(grp);
        universe.getViewer().getView().setMinimumFrameCycleTime(5);




        log.setSize(640, 480);
        log.add(c3d);
        log.setVisible(true);
    }
}
