package deformablemesh.io;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.track.MeshTracker;
import deformablemesh.track.Track;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param output
     * @param mesh
     * @param offset
     */
    static public void saveStlMesh(File output, DeformableMesh3D mesh, double[] offset, double SCALE) throws IOException{
        List<Triangle3D> triangles = mesh.triangles;
        BufferedWriter writes = Files.newBufferedWriter(output.toPath(), Charset.forName("UTF8"));
        writes.write("solid mesh\n");
        for(Triangle3D triangle: triangles){
            triangle.update();
            double[] n = triangle.getNormal();
            writes.write(String.format("facet normal %e %e %e\n", n[0], n[1], n[2]));
            writes.write("  outer loop\n");
            int[] indexes = triangle.getIndices();
            for(int dex: indexes){
                double[] p = mesh.getCoordinates(dex);
                writes.write(
                        String.format(
                                "    vertex %e %e %e\n",
                                (p[0] + offset[0])*SCALE,
                                (p[1] + offset[1])*SCALE,
                                (p[2] + offset[2])*SCALE
                        )
                );
            }
            writes.write("  endloop\n");
            writes.write("endfacet\n");
        }
        writes.write("endsolid mesh\n");
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

    static public Track readMeshLegacy(int frames, DataInputStream dis) throws IOException{
        Track track = new Track("legacy");

        Map<Integer, DeformableMesh3D> map = new HashMap<>();

        for(int i = 0; i<frames; i++) {
            readMesh(dis, map);
        }

        track.setData(map);
        return track;
    }

    static public void readMesh(DataInputStream dis, Map<Integer, DeformableMesh3D> map) throws IOException {

        int current = dis.readInt();
        int pos_count = dis.readInt();
        double[] positions = new double[pos_count];

        for (int j = 0; j < pos_count; j++) {
            positions[j] = dis.readDouble();
        }

        int con_count = dis.readInt();
        int[] connection_indices = new int[con_count];

        for (int j = 0; j < con_count; j++) {
            connection_indices[j] = dis.readInt();
        }

        int tri_count = dis.readInt();
        int[] triangle_indices = new int[tri_count];

        for (int j = 0; j < tri_count; j++) {
            triangle_indices[j] = dis.readInt();
        }
        DeformableMesh3D mesh = DeformableMesh3D.loadMesh(positions, connection_indices, triangle_indices);
        map.put(current, mesh);
    }

    static public List<Track> loadMeshes(File input) throws IOException {
        List<Track> tracks = new ArrayList<Track>();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(input.toPath(), StandardOpenOption.READ)));

        int version = dis.readInt();
        if(version>0){
            tracks.add(readMeshLegacy(version, dis));
        } else if(version==-1){
            int trackCount = dis.readInt();
            for(int i = 0; i<trackCount; i++){
                Track t = loadTrack(dis);
                tracks.add(t);
            }
        } else{
            throw new IOException("Unsupported Version");
        }

        return tracks;
    }

    private static Track loadTrack(DataInputStream dis) throws IOException {

        String name = dis.readUTF();
        int timePoints = dis.readInt();
        Map<Integer, DeformableMesh3D> map = new HashMap<>();

        for(int i = 0; i<timePoints; i++){
            readMesh(dis, map);
        }

        Track t = new Track(name);
        t.setData(map);
        return t;

    }




}
