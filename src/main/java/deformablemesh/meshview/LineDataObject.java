package deformablemesh.meshview;

import deformablemesh.geometry.Node3D;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.LineArray;
import org.scijava.java3d.LineAttributes;
import org.scijava.java3d.Node;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;
import snakeprogram3d.display3d.DataObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msmith on 10/30/15.
 */
public class LineDataObject implements DataObject {
    /*
     * TODO: Updated such that there is a single double array backing the data.
     *  During update, check if the number of points changes, if not just update the positions.
     *
     */

    final float LINEWIDTH;

    Shape3D line3d;
    Transform3D scale;
    int FRAME;
    private BranchGroup BG;
    ColoringAttributes c_at;
    List<double[]> positions = new ArrayList<>();
    public LineDataObject(List<Node3D> points){
        this(points, 3f);
    }
    public LineDataObject(List<Node3D> points, float width){
        points.forEach(p->positions.add(p.getCoordinates()));
        LineArray line = new LineArray(2*(positions.size()-1), GeometryArray.COORDINATES);
        for(int i=0; i<positions.size()-1; i++){
            line.setCoordinate(2*i,new Point3d(positions.get(i)));
            line.setCoordinate(2*i+1,new Point3d(positions.get(i+1)));

        }
        line3d = new Shape3D(line);
        line3d.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        LINEWIDTH=width;

        line3d.setAppearance(createAppearance());

    }

    public void dispose(){
        line3d.removeAllGeometries();
    }


    public BranchGroup getBranchGroup(){
        if(BG==null){
            Transform3D scale = new Transform3D();
            scale.setTranslation(new Vector3d(0,0,0));
            Vector3d saxis = new Vector3d(new double[]{1.,1.,1.});
            scale.setScale( saxis);
            TransformGroup tg = new TransformGroup();
            tg.setTransform(scale);
            tg.addChild(line3d);
            BG = new BranchGroup();
            BG.setCapability(BranchGroup.ALLOW_DETACH);
            BG.addChild(tg);

        }
        return BG;

    }

    public Appearance createAppearance(){
        Appearance a = new Appearance();

        c_at = new ColoringAttributes(1f, 0f, 0f, ColoringAttributes.NICEST);
        c_at.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);

        LineAttributes la = new LineAttributes();
        la.setLineWidth(LINEWIDTH);
        a.setColoringAttributes(c_at);
        a.setLineAttributes(la);

        return a;

    }

    /**
     * updates the geometry of snakes, not safe if there are zero points.
     */
    public void updateGeometry(List<double[]> positions){

        LineArray line = new LineArray(2*(positions.size()-1), GeometryArray.COORDINATES);
        for(int i=0; i<positions.size()-1; i++){
            line.setCoordinate(2*i,new Point3d(positions.get(i)));
            line.setCoordinate(2*i+1,new Point3d(positions.get(i+1)));

        }
        line3d.setGeometry(line);

    }
    public Node getNode(){
        return line3d;
    }

    public void setColor(float r, float g, float v){
        c_at.setColor(r, g, v);
    }


}
