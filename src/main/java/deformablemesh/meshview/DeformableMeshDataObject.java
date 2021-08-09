package deformablemesh.meshview;

import deformablemesh.geometry.Connection3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Triangle3D;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.IndexedLineArray;
import org.scijava.java3d.IndexedTriangleArray;
import org.scijava.java3d.LineAttributes;
import org.scijava.java3d.Material;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.vecmath.Color3f;

import java.awt.Color;
import java.util.List;

/**
 * A wrapper for all of the java3d stuff. In case it isn't available.
 *
 * User: msmith
 * Date: 8/2/13
 * Time: 10:25 AM
 */
public class DeformableMeshDataObject implements DataObject {
    IndexedLineArray lines;
    IndexedTriangleArray surfaces;
    Shape3D mesh_object,surface_object;
    BranchGroup branch_group;
    Color wires = Color.YELLOW;
    private boolean showSurface = false;
    private Color color;
    public DeformableMeshDataObject(List<Node3D> nodes, List<Connection3D> connections, List<Triangle3D> triangles, double[] positions, int[] connection_index, int[] triangle_index){

        lines = new IndexedLineArray(nodes.size(), GeometryArray.COORDINATES, 2*connections.size() );
        lines.setCoordinates(0,positions);
        lines.setCoordinateIndices(0, connection_index);
        lines.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

        mesh_object = new Shape3D(lines);
        mesh_object.setAppearance(createLineAppearance());

        surfaces = new IndexedTriangleArray(nodes.size(), GeometryArray.COORDINATES|GeometryArray.NORMALS, 3*triangles.size());
        surfaces.setCoordinates(0,positions);
        surfaces.setCoordinateIndices(0,triangle_index);
        surfaces.setNormals(0, generateNormals(positions, triangle_index));
        surfaces.setNormalIndices(0, triangle_index);

        surfaces.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        surfaces.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);

        surface_object = new Shape3D(surfaces);
        surface_object.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        surface_object.setAppearance(hiddenSurface());


        branch_group = new BranchGroup();
        branch_group.setCapability(BranchGroup.ALLOW_DETACH);
        branch_group.setCapability(BranchGroup.ALLOW_PICKABLE_WRITE);
        branch_group.addChild(mesh_object);
        branch_group.addChild(surface_object);
    }

    private Appearance hiddenSurface() {
        Appearance a = new Appearance();
        a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.SCREEN_DOOR, 1f));
        return a;
    }
    float clamp(float f){
        if(f<0){
            return 0;
        } else if(f>1){
            return 1f;
        } else{
            return f;
        }

    }

    private Appearance createSurfaceAppearance() {
        Appearance a = new Appearance();
        float[] rgb = color.getRGBComponents(new float[4]);
        Color3f ambient = new Color3f(rgb[0], rgb[1], rgb[2]);
        Color3f emmisive = new Color3f(Color.BLACK);
        Color3f difuse = new Color3f(rgb[0], rgb[1], rgb[2]);

        Color3f specular = new Color3f(1f, 1f, 1f);
        Material mat = new Material(
                ambient,
                emmisive,
                difuse,
                specular,
                0.1f);
        a.setMaterial(mat);

        float alpha = (1f - rgb[3]);
        TransparencyAttributes tat = new TransparencyAttributes(
                TransparencyAttributes.NICEST,
                alpha);
        tat.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        a.setTransparencyAttributes(tat);

        return a;
    }

    float[] generateNormals(double[] positions, int[] triangleIndexes){
        float[] normals = new float[positions.length];
        int t = triangleIndexes.length/3;


        for(int i = 0; i<t; i++){
            int dex = i*3;
            int a = triangleIndexes[dex];
            int b = triangleIndexes[dex+1];
            int c = triangleIndexes[dex+2];
            double ax = positions[3*a];
            double ay = positions[3*a + 1];
            double az = positions[3*a + 2];
            double bx = positions[3*b];
            double by = positions[3*b + 1];
            double bz = positions[3*b + 2];
            double cx = positions[3*c];
            double cy = positions[3*c + 1];
            double cz = positions[3*c + 2];

            double rbx = bx -ax;
            double rby = by - ay;
            double rbz = bz - az;
            double rcx = cx -ax;
            double rcy = cy -ay;
            double rcz = cz - az;

            double nx = rby*rcz - rbz*rcy;
            double ny = rbz*rcx - rbx*rcz;
            double nz = rbx*rcy - rby*rcx;

            double mag = Math.sqrt(nx*nx + ny*ny + nz*nz);
            if(mag>0){
                nx = nx/mag;
                ny = ny/mag;
                nz = nz/mag;
            }
            normals[3*a] += nx;
            normals[3*a + 1] += ny;
            normals[3*a + 2] += nz;

            normals[3*b] += nx;
            normals[3*b + 1] += ny;
            normals[3*b + 2] += nz;

            normals[3*c] += nx;
            normals[3*c + 1] += ny;
            normals[3*c + 2] += nz;



        }

        for(int i = 0; i<normals.length/3; i++){
            double nx = normals[i*3];
            double ny = normals[i*3+1];
            double nz = normals[i+3+2];

            double mag = Math.sqrt(nx*nx + ny*ny + nz*nz);
            if(mag==0){
                continue;
            }
            normals[i*3]/=mag;
            normals[i*3+1] /= mag;
            normals[i*3 + 2] /= mag;


            double px  = (float)positions[3*i + 0];
            double py = (float)positions[3*i + 1];
            double pz = (float)positions[3*i + 2];

        }

        return normals;
    }


    public void updateGeometry(double[] positions){
        lines.setCoordinates(0, positions);
        surfaces.setCoordinates(0, positions);
    }

    public Appearance createLineAppearance(){
        Appearance a = new Appearance();
        float[] r = new float[3];
        r = wires.getColorComponents(r);
        ColoringAttributes c_at = new ColoringAttributes(r[0], r[1], r[2], ColoringAttributes.NICEST);
        c_at.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
        a.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        a.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        LineAttributes la = new LineAttributes();
        la.setLineWidth(1);
        a.setColoringAttributes(c_at);
        a.setLineAttributes(la);
        TransparencyAttributes tat = new TransparencyAttributes(
                TransparencyAttributes.NICEST,
                0f);
        tat.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        a.setTransparencyAttributes(tat);
        return a;

    }



    @Override
    public BranchGroup getBranchGroup() {
        return branch_group;
    }


    public void setWireColor(Color color) {
        wires = color;
        float[] r = new float[3];
        r = wires.getColorComponents(r);
        float alpha = color.getAlpha() / 255.f;
        mesh_object.getAppearance().getColoringAttributes().setColor(r[0], r[1], r[2]);
        mesh_object.getAppearance().getTransparencyAttributes().setTransparency(1 - alpha);
    }

    public void setShowSurface(boolean showSurface) {
        if(showSurface){
            surface_object.setAppearance(createSurfaceAppearance());
        } else{
            surface_object.setAppearance(hiddenSurface());
        }
        this.showSurface = showSurface;
    }

    public void setColor(Color color) {
        if(color.equals(this.color)){
            return;
        }
        this.color = color;
        if(showSurface){
            surface_object.setAppearance(createSurfaceAppearance());
        }
        //copySolidColor();
        //updateColors(colors);

    }
}
