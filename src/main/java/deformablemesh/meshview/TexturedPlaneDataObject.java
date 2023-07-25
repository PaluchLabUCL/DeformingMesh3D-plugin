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

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import org.scijava.java3d.*;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector4f;

import java.awt.Color;

/**
 * A volume
 */
public class TexturedPlaneDataObject extends DeformableMeshDataObject {
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
        super(mesh.nodes, mesh.connections, mesh.triangles, mesh.positions, mesh.connection_index, mesh.triangle_index);
        offsets = new double[]{ stack.offsets[0], stack.offsets[1], stack.offsets[2]};

        setTextureData(stack);
        surface_object.setAppearance(createTexturedSurface());
        branch_group.removeChild(mesh_object);
    }
    public MeshImageStack getMeshImageStack(){
        return stack;
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

        xf = longest/xf;
        yf = longest/yf;
        zf = longest/zf;
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
        //appear.setTransparencyAttributes(
        //        new TransparencyAttributes(TransparencyAttributes.NICEST, 1.0f)
        //);

        return appear;
    }

    public void setMinMaxRange(double min, double max){
        this.min = (float)min;
        this.max = (float)max;
        updateVolume();
    }

    public void updateVolume(){
        volume = new VolumeTexture(texture_data, min, max, new Color3f(volumeColor));
        texturedAppearance = createTexturedSurface();
        surface_object.setAppearance(texturedAppearance);
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


    public  Shape3D getShape() {
        return surface_object;
    }
}
