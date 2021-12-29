package deformablemesh.meshview;

import deformablemesh.geometry.Connection3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Triangle3D;
import org.scijava.java3d.*;
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
    int[] triangle_indexes;
    float[] normals;

    public DeformableMeshDataObject(List<Node3D> nodes, List<Connection3D> connections, List<Triangle3D> triangles, double[] positions, int[] connection_index, int[] triangle_index){

        lines = new IndexedLineArray(nodes.size(), GeometryArray.COORDINATES, 2*connections.size() );
        lines.setCoordinates(0,positions);
        lines.setCoordinateIndices(0, connection_index);
        lines.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

        mesh_object = new Shape3D(lines);
        mesh_object.setAppearance(createLineAppearance());
        mesh_object.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        surfaces = new IndexedTriangleArray(nodes.size(), GeometryArray.COORDINATES|GeometryArray.NORMALS, 3*triangles.size());
        surfaces.setCoordinates(0,positions);
        surfaces.setCoordinateIndices(0,triangle_index);
        normals = new float[positions.length];
        triangle_indexes = triangle_index;
        generateNormals(positions);
        surfaces.setNormals(0, normals);
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
    public void setSurfaceAppearance(Appearance a){
        if(surface_object != null){
            surface_object.setAppearance(a);
        }
    }

    float[] adjust(float[] c, float v){
        if(v<0) return new float[]{0, 0, 0};
        if(v<1){
            return new float[]{c[0]*v, c[1]*v, c[2]*v};
        }
        return new float[]{
                clamp(c[0] + (v-1)),
                clamp(c[1] + (v-1)),
                clamp(c[2] + (v-1))
        };
    }

    private Appearance createSurfaceAppearance() {
        Appearance a = new Appearance();
        float[] rgb = color.getRGBComponents(new float[4]);
        Color3f ambient = new Color3f(adjust(rgb, 1));
        Color3f emmisive = new Color3f(adjust(rgb, 0));
        Color3f diffuse = new Color3f(adjust(rgb, 0));

        Color3f specular = new Color3f(adjust(rgb, 1.66f));
        Material mat = new Material(
                ambient,
                emmisive,
                diffuse,
                specular,
                0f);
        a.setMaterial(mat);
        if(rgb[3] != 1f){
            a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 1-rgb[3]));
        }
        return a;

    }

    void generateNormals(double[] positions){
        int t = triangle_indexes.length/3;

        for(int i = 0; i<normals.length; i++){
            normals[i] = 0;
        }

        for(int i = 0; i<t; i++){
            int dex = i*3;
            int a = triangle_indexes[dex];
            int b = triangle_indexes[dex+1];
            int c = triangle_indexes[dex+2];

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
            float nx = normals[i*3];
            float ny = normals[i*3+1];
            float nz = normals[i+3+2];

            float mag = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
            if(mag==0){
                continue;
            }
            normals[i*3]/=mag;
            normals[i*3+1] /= mag;
            normals[i*3 + 2] /= mag;

        }

    }


    public void  updateGeometry(double[] positions){
        lines.setCoordinates(0, positions);
        surfaces.setCoordinates(0, positions);
        generateNormals(positions);
        surfaces.setNormals(0, normals);

    }

    public Appearance createLineAppearance(){
        Appearance a = new Appearance();
        float[] r = new float[4];
        r = wires.getRGBComponents(r);
        ColoringAttributes c_at = new ColoringAttributes(r[0], r[1], r[2], ColoringAttributes.NICEST);
        c_at.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
        LineAttributes la = new LineAttributes();
        la.setLineWidth(1);
        a.setColoringAttributes(c_at);
        a.setLineAttributes(la);
        if(r[3] != 1f){
            a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, (1f - r[3])));
        }
        return a;

    }



    @Override
    public BranchGroup getBranchGroup() {
        return branch_group;
    }


    public void setWireColor(Color color) {
        wires = color;
        //float[] r = new float[4];
        //r = wires.getColorComponents(r);

        mesh_object.setAppearance(createLineAppearance());

        //mesh_object.getAppearance().getColoringAttributes().setColor(r[0], r[1], r[2]);

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
    }
}
