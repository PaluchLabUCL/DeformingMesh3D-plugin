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
package deformablemesh.io;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.geometry.WireframeMesh;
import deformablemesh.track.MeshTracker;
import deformablemesh.track.Track;

import java.awt.Color;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * For writing meshes incrementally. Opens file writes a mesh and then closes it.
 *
 * User: msmith
 * Date: 8/5/13
 * Time: 8:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class MeshWriter {
    File output;

    public MeshWriter(File output){
        this.output = output;
    }

    public void open(){
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(output));
            //just uses too many frames
            int frames = 0;

            dos.writeInt(frames);
            dos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized public void write(DeformableMesh3D mesh, Integer frame){

        try {
            RandomAccessFile ras = new RandomAccessFile(output, "rw");
            ras.seek(0);
            int frames = ras.readInt();
            ras.seek(0);

            ras.writeInt(frames+1);
            ras.seek(ras.length());

            writeMesh(ras,mesh,frame);
            ras.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Saves the current mesh to an ascii
     *
     * @param output
     * @param tracks
     * @param offset
     * @param SCALE
     * @param frame
     * @throws IOException
     */
    static public void saveStlMesh(File output, List<Track> tracks, double[] offset, double SCALE, Integer frame) throws IOException{
        BufferedWriter writes = Files.newBufferedWriter(output.toPath(), Charset.forName("UTF8"));
        double ox = offset[0];
        double oy = offset[1];
        double oz = offset[2];
        for(Track track: tracks) {
            /*
                For increasing the offset if some points are negative.
             */
            if (!track.containsKey(frame)) {
                continue;
            }
            DeformableMesh3D mesh = track.getMesh(frame);
            for(int i = 0; i<mesh.positions.length/3; i++){
                double[] p = mesh.getCoordinates(i);
                if(p[0]+ox < 0){
                    ox = -p[0];
                }
                if(p[1]+oy < 0){
                    oy = -p[1];
                }
                if(p[2]+oz < 0){
                    oz = -p[2];
                }

            }
        }
        System.out.println(ox + ", " + oy + ", " + oz);
        for(Track track: tracks) {
            if(!track.containsKey(frame)){
                continue;
            }
            DeformableMesh3D mesh = track.getMesh(frame);
            List<Triangle3D> triangles = mesh.triangles;
            writes.write("solid mesh\n");
            for (Triangle3D triangle : triangles) {
                triangle.update();
                double[] n = triangle.getNormal();
                writes.write(String.format(Locale.US, "facet normal %e %e %e\n", n[0], n[1], n[2]));
                writes.write("  outer loop\n");
                int[] indexes = triangle.getIndices();
                for (int dex : indexes) {
                    double[] p = mesh.getCoordinates(dex);
                    writes.write(
                            String.format(Locale.US,
                                    "    vertex %e %e %e\n",
                                    (p[0] + ox) * SCALE,
                                    (p[1] + oy) * SCALE,
                                    (p[2] + oz) * SCALE
                            )
                    );
                }



                writes.write("  endloop\n");
                writes.write("endfacet\n");
            }
            writes.write("endsolid mesh\n");

        }
        writes.close();
    }

    static public void legacySaveMeshes(File output, Map<Integer, DeformableMesh3D> meshes){
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(output));
            int frames = meshes.size();

            dos.writeInt(frames);

            for(Integer i: meshes.keySet()){

                DeformableMesh3D mesh = meshes.get(i);
                writeMesh(dos,mesh,i);

            }
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static public void saveMesh(String filename, DeformableMesh3D mesh) throws IOException{
        File output = new File(filename);
        MeshTracker tracker = new MeshTracker();
        Track t = tracker.createNewMeshTrack(0, mesh);
        saveMeshes(output, tracker);

    }

    static public void saveMeshes(File output, List<Track> tracks) throws IOException {
        MeshTracker tracker = new MeshTracker();
        tracker.addMeshTracks(tracks);
        saveMeshes(output, tracker);
    }

    static public void saveMeshes(File output, MeshTracker tracker) throws IOException {
        try(DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(
                        Files.newOutputStream( output.toPath(), StandardOpenOption.CREATE
                    )
                )
        )){
            dos.writeInt(-1); //new version of the mesh writing.

            List<Track> tracks = tracker.getAllMeshTracks();
            dos.writeInt(tracks.size());
            for(Track track: tracks){

                dos.writeUTF(track.getName());
                Map<Integer, DeformableMesh3D> map = track.getTrack();

                dos.writeInt(map.size());
                for(Integer i: map.keySet()){
                    writeMesh(dos, map.get(i), i);
                }

            }

        }catch(IOException exc){
            throw new IOException(exc);
        }

    }
    static public void writeMesh(DataOutput dos, DeformableMesh3D mesh, int frame) throws IOException {
        dos.writeInt(frame);
        int pos_count = mesh.positions.length;

        dos.writeInt(pos_count);
        for(int j = 0; j<pos_count; j++){
            dos.writeDouble(mesh.positions[j]);
        }

        int con_count = mesh.connection_index.length;
        dos.writeInt(con_count);
        for(int j = 0; j<con_count;j++){
            dos.writeInt(mesh.connection_index[j]);
        }

        int tri_count = mesh.triangle_index.length;
        dos.writeInt(tri_count);
        for(int j = 0; j<tri_count; j++){
            dos.writeInt(mesh.triangle_index[j]);
        }
    }










    /**
     * Save all of the tracks contained in the current frame.
     *
     * @param output
     * @param tracks
     * @param frame
     */
    public static void exportToPly(File output, List<Track> tracks, int frame, double[] offsets, double scale) throws IOException {
        List<DeformableMesh3D> meshes = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        int vertices = 0;
        int faces = 0;

        for(Track t: tracks){
            if(t.containsKey(frame)){
                Color c = t.getColor();
                DeformableMesh3D mesh = t.getMesh(frame);
                int verts = mesh.positions.length/3;
                int triangles = mesh.triangles.size();
                vertices += verts;
                faces += triangles;

                meshes.add(mesh);
                colors.add(c);
            }
        }

        if(meshes.size()==0){
            return;
        }

        try(BufferedWriter writer = Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)){
            writer.write("ply\n");
            writer.write("format ascii 1.0\n");
            writer.write("comment meshes created by deformable mesh plugin.\n");
            writer.write(String.format("element vertex %d\n", vertices));
            writer.write("property float x\n");
            writer.write("property float y\n");
            writer.write("property float z\n");
            writer.write("property uchar red\n");
            writer.write("property uchar green\n");
            writer.write("property uchar blue\n");
            writer.write(String.format("element face %d\n", faces));
            writer.write("property list uchar int vertex_index\n");
            writer.write("end_header\n");
            //write out the vertexes.
            for(int i = 0; i<meshes.size(); i++){
                DeformableMesh3D mesh = meshes.get(i);
                Color c = colors.get(i);
                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();
                for(int j = 0; j<mesh.positions.length/3; j++){
                    int dex = j*3;
                    writer.write(String.format(Locale.US, "%f %f %f %d %d %d\n",
                            (mesh.positions[dex])*scale,
                            (mesh.positions[dex+1])*scale,
                            (mesh.positions[dex+2])*scale,
                            r,g,b
                        )
                    );
                }
            }

            int offset = 0;
            for(DeformableMesh3D mesh: meshes){
                for(Triangle3D triangle: mesh.triangles){
                    int[] indices = triangle.getIndices();
                    writer.write(String.format(Locale.US, "%d %d %d %d\n",3, indices[0]+offset, indices[1]+offset, indices[2]+offset ));
                }
                offset += mesh.nodes.size();
            }
        } catch (IOException e) {
            throw new IOException(e);
        }

    }

    public static void exportToStlWireframe(File output, List<Track> tracks, double[] offset, double SCALE, Integer frame) throws IOException {
        List<Track> wireFrameTracks = new ArrayList<>();

        for(Track track: tracks){
            if(track.containsKey(frame)){
                DeformableMesh3D wireframe = new WireframeMesh(track.getMesh(frame)).getWireFrameMesh();
                Track t = new Track("wire-" + track.getName(), track.getColor());
                t.addMesh(frame, wireframe);
                wireFrameTracks.add(t);
            }

        }

        saveStlMesh(output, wireFrameTracks, offset, SCALE, frame);

    }
}
