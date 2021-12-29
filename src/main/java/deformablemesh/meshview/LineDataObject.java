package deformablemesh.meshview;

import deformablemesh.geometry.Node3D;
import org.scijava.java3d.*;
import org.scijava.vecmath.Vector3d;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    double[] positions;
    int[] indexes;
    IndexedLineArray line;
    public LineDataObject(List<Node3D> points){
        this(points, 3f);
    }
    public LineDataObject(List<Node3D> points, float width){

        createNewLineArray(
                points.stream().map(
                        Node3D::getCoordinates
                ).collect(
                        Collectors.toList()
                )
        );

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
     * Creates a copy of the provided array to keep as a backing and updates the LineArray.
     *
     * @param positions an array containing coordinates: { x0, y0, z0, x1, y1, z1, ...}
     */
    public void updateGeometry(double[] positions){
        if( positions.length == this.positions.length ){
            System.arraycopy(positions, 0, this.positions, 0, this.positions.length);
            line.setCoordinates(0, positions);
        } else{
            int n = positions.length/3;
            this.positions = Arrays.copyOf(positions, 0);
            //each consecutive point is connected by one connection.
            indexes = new int[n*2 - 2];
            line = new IndexedLineArray(n,GeometryArray.COORDINATES, 2*n-2);
            for(int i=0; i<n-1; i++){
                indexes[2 * i] = i;
                indexes[2 * i + 1] = i + 1;
            }

            line.setCoordinates(0, positions);
            line.setCoordinateIndices(0, indexes);
            line.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
            line3d.setGeometry(line);
        }
    }

    private void createNewLineArray(List<double[]> points){
        positions = new double[points.size()*3];
        //each consecutive point is connected by one connection.
        indexes = new int[points.size()*2 - 2];
        line = new IndexedLineArray(points.size(),GeometryArray.COORDINATES, 2*points.size()-2);
        for(int i=0; i<points.size(); i++){
            if(i<points.size() -1) {
                indexes[2 * i] = i;
                indexes[2 * i + 1] = i + 1;
            }
            double[] pt = points.get(i);
            positions[3*i] = pt[0];
            positions[3*i + 1] = pt[1];
            positions[3*i + 2] = pt[2];
        }

        line.setCoordinates(0, positions);
        line.setCoordinateIndices(0, indexes);
        line.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
    }
    /**
     * updates the geometry of snakes, not safe if there are zero points.
     */
    public void updateGeometry(List<double[]> points){

        if(points.size() == this.positions.length/3){
            for(int i=0; i<points.size(); i++){
                double[] pt = points.get(i);
                positions[3*i] = pt[0];
                positions[3*i + 1] = pt[1];
                positions[3*i + 2] = pt[2];
            }
            line.setCoordinates(0, positions);
        } else{
            createNewLineArray(points);
            line3d.setGeometry(line);
        }


    }
    public Node getNode(){
        return line3d;
    }

    public void setColor(float r, float g, float v){
        c_at.setColor(r, g, v);
    }


}
