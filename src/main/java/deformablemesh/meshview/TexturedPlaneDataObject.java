package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Connection3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Triangle3D;
import org.scijava.java3d.*;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;
import org.scijava.vecmath.Vector4f;
import snakeprogram3d.display3d.DataObject;
import snakeprogram3d.display3d.VolumeTexture;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TexturedPlaneDataObject implements DataObject {
    IndexedTriangleArray surfaces;
    Shape3D surface_object;
    BranchGroup branch_group;
    private boolean showSurface = false;
    private Color volumeColor = Color.WHITE;
    Appearance texturedAppearance;
    MeshImageStack stack;
    double[][][] texture_data;
    int[] sizes;
    double[] lengths;
    double[] offsets;
    VolumeTexture volume;
    float min = 0;
    float max = 1;
    public TexturedPlaneDataObject(DeformableMesh3D mesh, MeshImageStack stack){
        double[] positions = mesh.positions;

        offsets = new double[]{ stack.offsets[0], stack.offsets[1], stack.offsets[2]};

        surfaces = new IndexedTriangleArray(mesh.nodes.size(), GeometryArray.COORDINATES|GeometryArray.NORMALS, 3*mesh.triangles.size());
        surfaces.setCoordinates(0, positions);
        surfaces.setCoordinateIndices(0, mesh.triangle_index);
        surfaces.setNormals(0, generateNormals(mesh.positions, mesh.triangle_index));
        surfaces.setNormalIndices(0, mesh.triangle_index);

        surfaces.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        surfaces.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);

        surface_object = new Shape3D(surfaces);
        surface_object.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

        setTextureData(stack);

        surface_object.setAppearance(createTexturedSurface());


        branch_group = new BranchGroup();
        branch_group.setCapability(BranchGroup.ALLOW_DETACH);
        branch_group.addChild(surface_object);
    }

    public void setTextureData(MeshImageStack stack){
        this.stack = stack;
        int w = stack.getWidthPx();
        int h = stack.getHeightPx();
        int d = stack.getNSlices();

        int lowx = 0;
        int highx = w;
        int lowy = 0;
        int highy = h;
        int lowz = 0;
        int highz = d;





        //create a new one if there isn't one, or if the dimensions do not match.
        if(texture_data==null||d!=texture_data[0][0].length||h!=texture_data[0].length||w!=texture_data.length){
            texture_data = new double[w][h][d];
        }
        sizes = new int[]{w, h, d};

        for(int z = 0; z<d; z++){
            for(int y = 0; y<h; y++){
                for(int x = 0; x<w; x++){
                    texture_data[x][y][z] = stack.getValue(x, y, z);
                }
            }
        }

        updateVolume();

    }





    private Appearance hiddenSurface() {
        Appearance a = new Appearance();
        a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.SCREEN_DOOR, 1f));
        return a;
    }


    private Appearance createTexturedSurface(){
        VolumeTexture texture = new VolumeTexture(texture_data, min, max, new Color3f(Color.WHITE));

        TexCoordGeneration texCGen = new TexCoordGeneration();
        texCGen.setFormat(TexCoordGeneration.TEXTURE_COORDINATE_3);

        double xf = stack.getWidthPx()*stack.pixel_dimensions[0];
        double yf = stack.getHeightPx()*stack.pixel_dimensions[1];
        double zf = stack.getNSlices()*stack.pixel_dimensions[2];
        double longest = xf > yf ?
                zf > xf ? zf : xf :
                zf > yf ? zf : yf;
        System.out.println(xf + ", " + yf + ", " + zf);

        xf = longest/xf;
        yf = longest/yf;
        zf = longest/zf;
        System.out.println( "scaled " + xf + ", " + yf + ", " + zf);
        System.out.println( "offset: " + (offsets[0]*xf) + ", " + (offsets[1]*yf) + ", " + (offsets[2]*zf));
        Vector4f xPlane = new Vector4f((float)xf, 0, 0, (float)(offsets[0]*xf));
        Vector4f yPlane = new Vector4f(0, -(float)yf, 0, (float)(offsets[1]*yf));
        Vector4f zPlane = new Vector4f(0, 0, (float) zf, (float)(offsets[2]*zf));

        texCGen.setPlaneS(xPlane);
        texCGen.setPlaneT(yPlane);
        texCGen.setPlaneR(zPlane);
        texCGen.setPlaneQ(new Vector4f(0, 0, 1, 0));

        Appearance appear = new Appearance();

        appear.setCapability(Appearance.ALLOW_TEXTURE_WRITE);

        appear.setTexCoordGeneration(texCGen);

        appear.setTexture(texture);

        PolygonAttributes p = new PolygonAttributes();
        p.setCullFace(PolygonAttributes.CULL_NONE);

        Material material = new Material();
        //material.setAmbientColor(new Color3f(0f,0.3f,0.3f));
        material.setLightingEnable(false);
        appear.setMaterial(material);
        appear.setPolygonAttributes(p);

        return appear;
    }


    public void updateVolume(){
        volume = new VolumeTexture(texture_data, min, max, new Color3f(volumeColor));

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


        }

        return normals;
    }

    @Override
    public BranchGroup getBranchGroup() {
        return branch_group;
    }


    public void setShowSurface(boolean showSurface) {
        if(showSurface){
            surface_object.setAppearance(texturedAppearance);
        } else{
            surface_object.setAppearance(hiddenSurface());
        }

        this.showSurface = showSurface;
    }


}
