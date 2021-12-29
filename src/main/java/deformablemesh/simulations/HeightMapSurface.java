package deformablemesh.simulations;

import deformablemesh.MeshImageStack;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.io.MeshWriter;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HeightMapSurface implements ExternalEnergy {
    double[][] heightMap;
    int width, height;
    double minx = -0.5;
    double miny = -0.5;
    double maxx = 0.5;
    double maxy = 0.5;

    double xfactor = 0.5;
    double yfactor = 0.5;
    double pixel;
    double mag;
    DeformableMesh3D surfaceGeometry;

    public HeightMapSurface(double[][] heights, double mag){
        this.heightMap = heights;
        width = heightMap[0].length;
        height = heightMap.length;

        this.mag = mag;
        resetScale();
    }

    void setField(double xmin, double xmax, double ymin, double ymax){
        this.minx = xmin;
        this.maxx = xmax;
        this.miny = ymin;
        this.maxy = ymax;
        resetScale();
    }

    void resetScale(){
        xfactor = width/(maxx - minx);
        yfactor = height/(maxy - miny);
        pixel = 1/xfactor;
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        double[] proxy = new double[3];
        for(int i = 0; i<fx.length; i++){
            double x = positions[3*i+0];
            double y = positions[3*i+1];
            double z = positions[3*i+2];
            getForce(x, y, z, proxy);
            fx[i] += proxy[0];
            fy[i] += proxy[1];
            fz[i] += proxy[2];

        }
    }

    /**
     * Gets the force in normalized coordinates for a point at x,y,z
     *
     * @param x
     * @param y
     * @param z
     * @param target destination for force.
     * @return target.
     */
    double[] getForce(double x, double y, double z, double[] target){

        //dx = 1
        double hx0 = getHeight(x - 0.5*pixel, y);
        double hx1 = getHeight(x + 0.5*pixel, y);

        //dy = 1
        double hy0 = getHeight(x, y - 0.5*pixel);
        double hy1 = getHeight(x, y + 0.5*pixel);

        double[] du = {1*pixel, 0, hx1 - hx0};
        double[] dv = {0, 1*pixel, hy1 - hy0};

        double h = 0.25*(hx0 + hx1 + hy0 + hy1);

        double[] n = Vector3DOps.cross(du, dv);
        Vector3DOps.normalize(n);

        double delta = h - z;
        if(delta>0){
            target[0] = delta*n[0]*mag;
            target[1] = delta*n[1]*mag;
            target[2] = delta*n[2]*mag;
        } else{
            target[0] = 0;
            target[1] = 0;
            target[2] = 0;
        }

        return target;
    }

    /**
     * Gets the height for the x,y position on the map.
     *
     * @param x
     * @param y
     * @return actual value from height map.
     */
    double getHeight(double x, double y){
        double u = (x - minx)*xfactor;
        double v = (y - miny)*yfactor;

        int i0 = (int)u;
        int j0 = (int)v;
        if(i0<0){
            if(j0<0){
                return heightMap[0][0];
            } else if(j0>=height-1){
                return heightMap[height-1][0];
            } else{
                double f = v - j0;
                return heightMap[j0][0]*(1-f) + heightMap[j0+1][0];
            }
        } else if(i0>=width-1){
            if(j0<0){
                return heightMap[0][width-1];
            } else if(j0>=height-1){
                return heightMap[height-1][width-1];
            } else{
                double f = v - j0;
                return heightMap[j0][width-1]*(1-f) + heightMap[j0+1][width-1];
            }
        } else {
            if (j0 < 0) {

                double f = u - i0;
                return heightMap[0][i0] * (1 - f) + heightMap[0][i0 + 1] * f;

            } else if (j0 >= height - 1) {
                double f = u - i0;
                return heightMap[height - 1][i0] * (1 - f) + heightMap[height - 1][i0 + 1] * f;
            } else {
                double fx = u - i0;
                double fy = v - j0;
                double h00 = heightMap[j0][i0];
                double h01 = heightMap[j0][i0 + 1];

                double h0 = h00 * (1 - fx) + h01 * fx;

                double h10 = heightMap[j0 + 1][i0];
                double h11 = heightMap[j0 + 1][i0 + 1];

                double h1 = h10 * (1 - fx) + h11 * fx;
                return h0 * (1 - fy) + h1 * fy;
            }
        }
        // should be returned else where.
    }

    @Override
    public double getEnergy(double[] pos) {

        double dz = pos[2] - getHeight(pos[0], pos[1]);
        if(dz<0){
            return -dz*mag;
        }
        return 0;
    }

    public void createHeighMapDataObject() {
        int nPoints = 50;

        double dx = width*1.0/(nPoints - 1);
        double dy = height*1.0/(nPoints - 1);

        int xsteps = nPoints;
        int ysteps = nPoints;

        dx = width * 1.0 / (xsteps - 1);
        dy = height * 1.0 / (ysteps - 1);

        System.out.println(width + ": " + xsteps + ", " + height + ": "  + ysteps);

        double[] points = new double[3 * xsteps * ysteps];

        for (int j = 0; j < xsteps; j++) {
            for (int k = 0; k < ysteps; k++) {

                double x = j * dx * pixel + minx;
                double y = k * dy * pixel + miny;
                double z = getHeight(x, y);

                points[3 * (j + k * xsteps)] = x;
                points[3 * (j + k * xsteps) + 1] = y;
                points[3 * (j + k * xsteps) + 2] = z;

            }
        }

        int boxes = (xsteps - 1) * (ysteps - 1);
        int triangles = 2 * boxes * 3;
        int connections = ( 3 * boxes  + (xsteps - 1) + (ysteps - 1) )*2;
        int[] triangle_indexes = new int[triangles];
        int[] connection_indexes = new int[connections];

        int t = 0;
        int c = 0;
        for (int j = 0; j < xsteps - 1; j++) {
            for (int k = 0; k < ysteps - 1; k++) {
                int n = j + k * xsteps;
                int right = j + 1 + k * xsteps;
                int down = j + (k + 1) * xsteps;
                int diag = j + 1 + (k + 1) * xsteps;
                triangle_indexes[t++] = diag;
                triangle_indexes[t++] = n;
                triangle_indexes[t++] = right;

                triangle_indexes[t++] = n;
                triangle_indexes[t++] = diag;
                triangle_indexes[t++] = down;

                connection_indexes[c++] = n;
                connection_indexes[c++] = right;

                connection_indexes[c++] = n;
                connection_indexes[c++] = diag;

                connection_indexes[c++] = n;
                connection_indexes[c++] = down;

            }
            //at the end of the column connect down to dialgonal
            connection_indexes[c++] = j + (ysteps - 1)*xsteps;
            connection_indexes[c++] = j + 1 + (ysteps-1)*xsteps;

        }
        //right to diagonal for last column
        for (int k = 0; k < ysteps - 1; k++) {
            connection_indexes[c++] = (xsteps-1) + k*xsteps;
            connection_indexes[c++] = (xsteps - 1) + (k+1)*xsteps;
        }


        surfaceGeometry = new DeformableMesh3D(points, connection_indexes, triangle_indexes);
        surfaceGeometry.create3DObject();
        surfaceGeometry.ALPHA = 1;
        surfaceGeometry.BETA = 0.1;
        surfaceGeometry.GAMMA = 1;
        surfaceGeometry.reshape();
    }

    static class FixedPosition{
        int i;
        double x, y, z;
        public FixedPosition(int index, double x, double y, double z){
            this.i = index;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    ExternalEnergy getEdgeForce(DeformableMesh3D mesh){
        List<FixedPosition> locations = new ArrayList<>();
        for(int i = 0; i<mesh.nodes.size(); i++){
            double[] pt = mesh.nodes.get(i).getCoordinates();
            if(pt[0] == minx || pt[0] == maxx || pt[1] == miny || pt[1] == maxy){
                locations.add(new FixedPosition(i, pt[0], pt[1], 0  ));
            }
        }

        return new ExternalEnergy() {
            @Override
            public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
                for(FixedPosition fp: locations){
                    double x = positions[fp.i*3];
                    double y = positions[fp.i*3 + 1];
                    double z = positions[fp.i*3 + 2];

                    fx[fp.i] += fp.x - x;
                    fy[fp.i] = fp.y - y;
                    fz[fp.i] = fp.z - z;

                }
            }

            @Override
            public double getEnergy(double[] pos) {
                return 0;
            }
        };
    }

    public static void main(String[] args) throws IOException {

        ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        MeshImageStack reference = new MeshImageStack(
                new ImagePlus(Paths.get(args[1]).toAbsolutePath().toString())
        );

        double[][] points = new double[plus.getHeight()][plus.getWidth()];

        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.addLights();
        Track track = new Track("blue-height-map");
        for(int n = 1; n<=plus.getNFrames(); n++){
            ImageProcessor proc = plus.getStack().getProcessor(n);
            for(int i = 0; i<points.length; i++){
                for(int j = 0; j<points[0].length; j++){
                    points[i][j] = reference.getNormalizedCoordinate(new double[]{i, j, proc.getPixelValue(i, j)-1})[2];
                }
            }

            HeightMapSurface hms = new HeightMapSurface(points, 1);
            hms.createHeighMapDataObject();
            //double[] f = hms.getForce(0, 0, 0, new double[3]);
            //System.out.println(Arrays.toString(f));
            //hms.surfaceGeometry.addExternalEnergy(hms.getEdgeForce(hms.surfaceGeometry));

            hms.surfaceGeometry.setShowSurface(true);
            frame.addDataObject(hms.surfaceGeometry.data_object);
            track.addMesh(n-1, hms.surfaceGeometry);
            System.out.println("frame: " + n);
        }
        MeshWriter.saveMeshes(new File("height-maps.bmf"), Arrays.asList(track));
        //while(true){
        //    hms.surfaceGeometry.update();
        //}
    }

}
