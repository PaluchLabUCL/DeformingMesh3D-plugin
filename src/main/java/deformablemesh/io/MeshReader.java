package deformablemesh.io;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.Track;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshReader {
    final long limit;
    File input;
    long current = 0;
    DataInputStream dis;
    long pos = 0;
    private class CountingInputStream extends InputStream {

        final InputStream src;

        public CountingInputStream(InputStream src){
            this.src = src;
        }

        @Override
        public int read() throws IOException {
            pos++;
            return src.read();
        }
        @Override
        public void close() throws IOException {
            src.close();
        }

    }
    public MeshReader(File meshFile){
        input = meshFile;
        this.limit = meshFile.length();
    }

    private DataInputStream startReading() throws IOException{

        dis = new DataInputStream(
                new CountingInputStream(
                        new BufferedInputStream(
                                Files.newInputStream(input.toPath(), StandardOpenOption.READ)
                        )
                )
        );

        return dis;
    }

    private Track readMeshLegacy(int frames) throws IOException{
        Track track = new Track("legacy");

        Map<Integer, DeformableMesh3D> map = new HashMap<>();

        for(int i = 0; i<frames; i++) {
            readMesh(dis, map);
        }

        track.setData(map);
        return track;
    }

    public List<Track> loadMeshes() throws IOException{
        List<Track> tracks = new ArrayList<>();
        try(
                DataInputStream stream = startReading()
        ) {


            int version = dis.readInt();
            current += Integer.BYTES;

            if (version > 0) {
                //instead of a version number there was a number of frames.
                tracks.add(readMeshLegacy(version));
            } else if (version == -1) {
                int trackCount = dis.readInt();
                for (int i = 0; i < trackCount; i++) {
                    Track t = loadTrack();
                    tracks.add(t);
                }
            } else {
                throw new IOException("Unsupported Version");
            }
        }
        return tracks;
    }

    /**
     * Reads a mesh from the provided DataInputStream and places it into the mapped data.
     *
     * @param dis input stream data will be read from.
     * @param map mapping of frames to meshes.
     * @throws IOException
     */
    private void readMesh(DataInputStream dis, Map<Integer, DeformableMesh3D> map) throws IOException {
        int current = dis.readInt();

        int pos_count = checkCount(dis.readInt(), Double.BYTES);
        double[] positions = new double[pos_count];
        for (int j = 0; j < pos_count; j++) {
            positions[j] = dis.readDouble();
        }

        int con_count = checkCount(dis.readInt(), Integer.BYTES);
        int[] connection_indices = new int[con_count];
        for (int j = 0; j < con_count; j++) {
            connection_indices[j] = dis.readInt();
        }

        int tri_count = checkCount(dis.readInt(), Integer.BYTES);
        int[] triangle_indices = new int[tri_count];

        for (int j = 0; j < tri_count; j++) {
            triangle_indices[j] = dis.readInt();
        }

        DeformableMesh3D mesh = DeformableMesh3D.loadMesh(positions, connection_indices, triangle_indices);
        map.put(current, mesh);

    }

    private Track loadTrack() throws IOException {
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

    public double progress(){
        return 1.0*pos/limit;
    }

    /**
     * When a counting variable is read this check if it will go out of bounds or if it
     * @param count number of bytes being requested.
     * @param dataWidth width of data in bytes
     * @return count if it is not broken.
     */
    private int checkCount(int count, int dataWidth) throws IOException {
        if(count < 0 || count*dataWidth > (limit - pos)){
            throw new IOException("invalid count variable: " + count + ", file size " + limit + " read " + pos);
        }

        return count;
    }


    static public List<Track> loadMeshes(File input) throws IOException {
        MeshReader reader = new MeshReader(input);
        return reader.loadMeshes();
    }
}
