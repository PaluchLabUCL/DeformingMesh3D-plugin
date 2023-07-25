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
package deformablemesh.simulations;

import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.MeshFrame3D;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import org.scijava.java3d.*;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3f;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Test class for improving surface rendering.
 *
 *
 */
public class WavesOnASheet {
    double time = 0;
    double velocity = 1;
    double[] xModes;
    double[] yModes;
    double dt = 1e-6;
    int nPoints = 128;

    double ids = (nPoints - 1.0)/2.0;
    double[] positions;
    int[] triangle_indexes;
    float[] normals;

    IndexedTriangleArray surfaces;
    Shape3D shape;
    int[] side_indexes;
    double[] side_positions;
    IndexedTriangleArray sides;
    Shape3D side_shape;

    double[][] c;
    double[][] px, py;
    double[][] c2;
    double[][] px2, py2;

    public WavesOnASheet(int N){
        xModes = new double[N];
        yModes = new double[N];

        double xs = 0;
        double ys = 0;
        double w = 0.5;
        double noise = 0.0;
        for(int i = 0; i<N-1; i++){
            xModes[i] = - 0 * Math.cos(Math.PI*(i+1))/(Math.PI*(i+1)) + noise * (0.5 - Math.random());
            yModes[i] = - 0 * Math.cos(Math.PI*(i+1))/(Math.PI*(i+1)) + noise * (0.5 - Math.random());
        }
        //xModes[0] = 1;
        //yModes[0] = 1;
    }

    void prepareFiniteDifference(){
        c = new double[nPoints][nPoints];
        c2 = new double[nPoints][nPoints];
        px = new double[nPoints][nPoints];
        py = new double[nPoints][nPoints];
        px2 = new double[nPoints][nPoints];
        py2 = new double[nPoints][nPoints];

        double ds = 2.0/(nPoints-1);

        double r = 0.03;
        double h = 0.4;
        for(int i = 0; i<nPoints; i++){
            for(int j = 0; j<nPoints; j++){
                double x = (i - 2*nPoints/3)*ds;
                double y = (j - 2*nPoints/3)*ds;
                double l = Math.sqrt(x*x + y*y);
                c[i][j] = h*Math.exp( - (l*l*l*l)/(2*r*r*r*r));
                positions[3*(i + j*nPoints) + 2] = c[i][j];
            }
        }
    }

    void dCdr(int i, int j, double[] dcdr){
        if(i == 0 || i==nPoints-1){
            if(i == 0){
                dcdr[0] = (c[i+1][j] - c[i][j])*2*ids;
            } else{
                dcdr[0] = (c[i][j] - c[i-1][j])*2*ids;
            }
        } else{
            dcdr[0] = (c[i+1][j] - c[i-1][j])*2*ids;
        }
        if( j == 0 || j == nPoints-1){
            if(j == 0){
                dcdr[1] = (c[i][j+1] - c[i][j])*ids*2;
            } else{
                dcdr[1] = (c[i][j] - c[i][j-1])*ids*2;
            }
        } else{
            dcdr[1] = (c[i][j+1] - c[i][j-1])*2*ids;
        }

    }
    void dPdr(int i, int j, double[] dpdr){
        if(i == 0 || i==nPoints-1){
            if( i == 0){
                dpdr[0] = px[i+1][j]*ids;
            } else{
                dpdr[0] = -px[i-1][j]*ids;
            }
        } else{
            dpdr[0] = (px[i+1][j] - px[i-1][j])*2*ids;
        }
        if( j == 0 || j == nPoints-1){
            if(j == 0){
                dpdr[1] = py[i][j+1]*ids;
            } else{
                dpdr[1] = -py[i][j-1]*ids;
            }
        } else{
            dpdr[1] = (py[i][j+1] - py[i][j-1])*2*ids;
        }
    }
    int regions = 4;
    ExecutorService service = Executors.newFixedThreadPool(regions);
    void step(){
        CountDownLatch latch = new CountDownLatch(regions);
        int block = nPoints/regions;
        for(int region = 0; region< regions; region++) {
            final int start = region*block;
            final int finish = (region+1)*block;
            service.submit(()->{
                double[] dcdr = new double[2];
                double[] dpdr = new double[2];
                for (int i = start; i < finish; i++) {
                    for (int j = 0; j < nPoints; j++) {
                        dCdr(i, j, dcdr);
                        dPdr(i, j, dpdr);
                        c2[i][j] = c[i][j] + (dpdr[0] + dpdr[1]) * dt;
                        px2[i][j] = px[i][j] + velocity * velocity * (dcdr[0]) * dt;
                        py2[i][j] = py[i][j] + velocity * velocity * (dcdr[1]) * dt;
                    }
                }
                latch.countDown();
            });
        }
        try{
            latch.await();
        } catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
        //swap buffers.
        double[][] temp = c;
        c = c2;
        c2 = temp;
        temp = px;
        px = px2;
        px2 = temp;
        temp = py;
        py = py2;
        py2 = temp;

    }
    public void update3DPDE(){
        for (int i = 0; i < nPoints; i++) {
            for (int j = 0; j < nPoints; j++) {
                positions[3 * (i + j * nPoints) + 2] = c[i][j];
            }
        }
        update3DGeometry();
    }

    Appearance createAppearance(){
        Appearance a = new Appearance();
        float[] rgb = new Color(0.3f, 0.3f, 1f).getRGBComponents(new float[4]);
        Color3f ambient = new Color3f(0*rgb[0]/2, 0*rgb[1]/2, 0*rgb[2]/2);
        Color3f emmisive = new Color3f(rgb[0]/2, rgb[1]/2, rgb[2]/2);
        Color3f difuse = new Color3f(rgb[0], rgb[1], rgb[2]);

        Color3f specular = new Color3f(1f, 1f, 1f);
        Material mat = new Material(
                ambient,
                emmisive,
                difuse,
                specular,
                100f);
        a.setMaterial(mat);
        a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 0.1f));
        return a;
    }

    Appearance createSideAppearance(){
        Appearance a = new Appearance();
        Color3f lightBlue = new Color3f(0.4f, 0.4f, 1.f);
        a.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f));
        a.setColoringAttributes(new ColoringAttributes(lightBlue, ColoringAttributes.SHADE_FLAT));
        a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 0.5f));
        return a;
    }

    void generateNormals(){
        System.out.println("called");
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

            float nx = (float)(rby*rcz - rbz*rcy);
            float ny = (float)(rbz*rcx - rbx*rcz);
            float nz = (float)(rbx*rcy - rby*rcx);


            normals[a*3] += nx;
            normals[a*3 + 1] += ny;
            normals[a*3 + 2] += nz;

            normals[b*3] += nx;
            normals[b*3 + 1] += ny;
            normals[b*3 + 2] += nz;

            normals[c*3] += nx;
            normals[c*3 + 1] += ny;
            normals[c*3 + 2] += nz;

        }

        for(int i = 0; i<normals.length/3; i++){
            float nx = normals[ i * 3 ];
            float ny = normals[ i * 3 + 1 ];
            float nz = normals[ i * 3 + 2];

            float mag = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);

            if(mag==0){
                continue;
            }

            normals[ i*3 ] /= mag;
            normals[ i*3 + 1 ] /= mag;
            normals[i*3 + 2] /= mag;

        }

    }
    void syncSides(){

        for (int j = 0; j < nPoints; j++) {
            //xaxis 2 - top and bottome 3 - coordinates
            side_positions[2*3 * j + 2] = positions[3*(j + 0*nPoints) + 2];

            int i0 = 2*3*nPoints;
            side_positions[i0 + 2*3 * j + 2] = positions[3*(j + (nPoints-1)*nPoints) + 2];

            i0 = 2*2*3*nPoints;
            side_positions[i0 + 2*3 * j + 2] = positions[3*(0 + (j)*nPoints) + 2];

            i0 = 3*2*3*nPoints;
            side_positions[i0 + 2*3 * j + 2] = positions[3*( (nPoints -1) + (j)*nPoints) + 2];

        }

        for (int j = 0; j < nPoints; j++) {
            side_positions[2*3 * j + 2] = positions[3*(j + 0*nPoints) + 2];
        }
        sides.setCoordinates(0, side_positions);
    }
    void update3DGeometry(){
        syncSides();
        generateNormals();
        surfaces.setNormals(0, normals);
        surfaces.setCoordinates(0, positions);

    }
    void create3DGeometry(){
        surfaces = new IndexedTriangleArray(nPoints*nPoints, GeometryArray.COORDINATES|GeometryArray.NORMALS, triangle_indexes.length);
        surfaces.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        surfaces.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
        surfaces.setCoordinates(0,positions);
        surfaces.setCoordinateIndices(0,triangle_indexes);
        surfaces.setNormals(0, normals);
        surfaces.setNormalIndices(0, triangle_indexes);


        shape = new Shape3D(surfaces);
        shape.setAppearance(createAppearance());

        sides = new IndexedTriangleArray(side_positions.length/3, GeometryArray.COORDINATES, side_indexes.length);
        sides.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        sides.setCoordinates(0, side_positions);
        sides.setCoordinateIndices(0, side_indexes);
        side_shape = new Shape3D(sides);

        side_shape.setAppearance(createSideAppearance());

    }

    public void createSurface() {

        double dx = 2.0/(nPoints - 1);
        double dy = 2.0/(nPoints - 1);

        int boxes = (nPoints - 1) * (nPoints - 1);
        int triangles = 2 * boxes * 3;
        triangle_indexes = new int[triangles];
        positions = new double[nPoints*nPoints*3];
        normals = new float[positions.length];
        System.out.println(normals.length + ", " + positions.length + ", " + triangles/3);

        for (int j = 0; j < nPoints; j++) {
            for (int k = 0; k < nPoints; k++) {

                double x = j * dx - 1;
                double y = k * dy - 1;
                double z = getZ(x, y);

                positions[3 * (j + k * nPoints)] = x;
                positions[3 * (j + k * nPoints) + 1] = y;
                positions[3 * (j + k * nPoints) + 2] = z;

            }
        }

        int t = 0;
        int c = 0;
        for (int j = 0; j < nPoints - 1; j++) {
            for (int k = 0; k < nPoints - 1; k++) {
                int n = j + k * nPoints;
                int right = j + 1 + k * nPoints;
                int down = j + (k + 1) * nPoints;
                int diag = j + 1 + (k + 1) * nPoints;
                triangle_indexes[t++] = diag;
                triangle_indexes[t++] = n;
                triangle_indexes[t++] = right;

                triangle_indexes[t++] = n;
                triangle_indexes[t++] = diag;
                triangle_indexes[t++] = down;
            }
        }
    }

    void createSides(){
        double dx = 2.0/(nPoints - 1);

        int triangles = 4 * 2 * nPoints;
        side_indexes = new int[ triangles * 3 ];
        side_positions = new double[ 4 * 2 * nPoints * 3 ];

        double bottom = -0.1;
        for (int j = 0; j < nPoints; j++) {
            double s = j * dx - 1;
            //xaxis 2 - top and bottome 3 - coordinates
            side_positions[2*3 * j] = s;
            side_positions[2*3 * j + 1] = -1;
            side_positions[2*3 * j + 2] = positions[3*(j + 0*nPoints) + 2];
            side_positions[2*3 * j + 3] = s;
            side_positions[2*3 * j + 4] = -1;
            side_positions[2*3 * j + 5] = bottom;

            int i0 = 2*3*nPoints;
            side_positions[i0 + 2*3 * j] = s;
            side_positions[i0 + 2*3 * j + 1] = 1;
            side_positions[i0 + 2*3 * j + 2] = positions[3*(j + (nPoints-1)*nPoints) + 2];
            side_positions[i0 + 2*3 * j + 3] = s;
            side_positions[i0 + 2*3 * j + 4] = 1;
            side_positions[i0 + 2*3 * j + 5] = bottom;

            i0 = 2*2*3*nPoints;
            side_positions[i0 + 2*3 * j] = -1;
            side_positions[i0 + 2*3 * j + 1] = s;
            side_positions[i0 + 2*3 * j + 2] = positions[3*(0 + (j)*nPoints) + 2];
            side_positions[i0 + 2*3 * j + 3] = -1;
            side_positions[i0 + 2*3 * j + 4] = s;
            side_positions[i0 + 2*3 * j + 5] = bottom;

            i0 = 3*2*3*nPoints;
            side_positions[i0 + 2*3 * j] = 1;
            side_positions[i0 + 2*3 * j + 1] = s;
            side_positions[i0 + 2*3 * j + 2] = positions[3*( (nPoints -1) + (j)*nPoints) + 2];
            side_positions[i0 + 2*3 * j + 3] = 1;
            side_positions[i0 + 2*3 * j + 4] = s;
            side_positions[i0 + 2*3 * j + 5] = bottom;
        }

        int t = 0;
        for(int k = 0; k<4; k++) {
            for (int j = nPoints*k; j < nPoints*(k+1) - 1; j++) {
                int n = 2 * j;
                int right = 2 * (j + 1);
                int down = 2 * j + 1;
                int diagonal = 2 * (j + 1) + 1;

                side_indexes[t++] = n;
                side_indexes[t++] = down;
                side_indexes[t++] = diagonal;

                side_indexes[t++] = n;
                side_indexes[t++] = diagonal;
                side_indexes[t++] = right;
            }
        }
    }

    double fourier(double position, int mode){

        double kj = Math.PI *( mode + 1 );
        double wj = kj*velocity;
        return Math.sin(position*kj)*Math.cos( wj*time );
    }

    double getZ(double x, double y){
        double z = 0;
        double fx = 0 ;
        double fy = 0;
        for(int i = 0; i<xModes.length; i++){
            fx += xModes[i] * fourier(x, i);
        }
        for(int j = 0; j<yModes.length; j++){
            fy += yModes[j] * fourier(y, j);
        }

        return fx*fy;
    }

    public void update(){
        time+= dt;

        for (int j = 0; j < nPoints; j++) {
            for (int k = 0; k < nPoints; k++) {

                double x = positions[3*(j + k*nPoints)];
                double y = positions[3 * (j + k * nPoints) + 1];
                positions[3 * (j + k * nPoints) + 2] = getZ(x, y);

            }
        }
        update3DGeometry();

    }

    DataObject getLights(){
        BoundingSphere bounds =	new BoundingSphere (new Point3d(0, 0.0, 0.0), 25.0);
        float directional = 0.5f;

        DirectionalLight light1 = new DirectionalLight(new Color3f(directional, directional, directional), new Vector3f(-(float)Math.sqrt(3)/3f, -(float)Math.sqrt(3)/3f, (float)Math.sqrt(3)/3f));
        DirectionalLight light2 = new DirectionalLight(new Color3f(directional, directional, directional), new Vector3f(-(float)Math.sqrt(3)/3f, (float)Math.sqrt(3)/3f, (float)Math.sqrt(3)/3f));

        light1.setInfluencingBounds(bounds);
        light2.setInfluencingBounds(bounds);
        BranchGroup bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_DETACH);
        //bg.addChild(amber);
        bg.addChild(light1);
        bg.addChild(light2);
        return () -> bg;

    }


    public static void main(String[] args) throws Exception{
        WavesOnASheet sheet = new WavesOnASheet(100);
        sheet.createSurface();
        sheet.createSides();

        sheet.prepareFiniteDifference();
        sheet.create3DGeometry();
        sheet.update3DPDE();

        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.hideAxis();
        frame.setBackgroundColor(Color.BLACK);
        //frame.addDataObject(sheet.getLights());
        frame.addLights();

        frame.addDataObject(()->{
            BranchGroup bg = new BranchGroup();
            bg.addChild(sheet.shape);
            bg.addChild(sheet.side_shape);
            return bg;
        });
        long steps = 0;
        long start = System.currentTimeMillis();

        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if(semaphore.hasQueuedThreads()){
                    semaphore.release();
                } else{
                    try {
                        semaphore.acquire();
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {

            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });


        ImageStack stack = null;
        while(true){
            semaphore.acquire();
            semaphore.release();
            sheet.step();
            steps++;
            if(steps%2000 == 0 ){
                sheet.update3DPDE();
                System.out.println( (System.currentTimeMillis() - start)/1000.0 + "s for " + steps + " step." );
                if(stack == null || stack.size() < 600){
                    BufferedImage img = frame.snapShot();
                    if(stack == null){
                        stack = new ImageStack(img.getWidth(), img.getHeight());
                    }
                    stack.addSlice(new ColorProcessor(img));
                    if(stack.size() == 600){
                        new ImageJ();
                        new ImagePlus("animation", stack).show();
                    }
                }

                steps = 0;
                start = System.currentTimeMillis();
            }
        }
    }

}
