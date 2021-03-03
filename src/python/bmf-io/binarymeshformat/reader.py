#!/usr/bin/env python3

import struct, json


class MeshReader:
    """
        Class for reading meshes from a collection of bytes. This class
        could be considered private. It is used by the function
        'loadMeshTracks.
        
        The readXyz methods are intented to read the corresponding 
        java DataOutputStream written values.
    """
    def __init__(self, data):
        """
            Starts a reader over the provided bytes.
            Args:
                data: bytes-like object with mesh tracks. Will be used 
                      with struct.unpack_from.
        """
        self.bytes = data
        self.pos = 0
        self.tracks = []
    def load(self):
        """
            Populates and the list of tracks for this reader. 
        """
        self.version = self.readInt()
        self.track_count = self.readInt()
        for i in range(self.track_count):
            name = self.readUTFString();
            track = Track(name)
            mesh_count = self.readInt()
            for j in range(mesh_count):
                frame = self.readInt()
                position_count = self.readInt()
                positions = self.readDoubles(position_count)
                connection_count = self.readInt()
                connections = self.readInts(connection_count)
                triangle_count = self.readInt()
                triangles = self.readInts(triangle_count)
                track.addMesh(frame, Mesh(positions, connections, triangles))
            self.tracks.append(track)
            
                
    def readInt(self):
        """
            Reads a single 4 byte int big endian.
        """
        i = struct.unpack_from(">i", self.bytes, self.pos)[0]
        self.pos += 4
        return i
    
    def readInts(self, n):
        """
            Reads n 4 byte big-endian ints.
        """
        ints = struct.unpack_from(">%si"%n, self.bytes, self.pos)
        self.pos += 4*n
        return ints
        
    def readDouble(self):
        """
            Resuts a single 8 byte floating point value, big endian.
        """
        d = struct.unpack_from(">d", self.bytes, self.pos)[0]
        self.pos+=8
        return d
    
    def readDoubles(self, n):
        """
            Reads n 8 byte floating point values, big endian.
            Args:
                n: number of floats to read.
        """
        doubles = struct.unpack_from(">%sd"%n, self.bytes, self.pos)
        self.pos += n*8
        return doubles
        
    def readUTFString(self):
        """
            Reads a utf string, the first 2 bytes are a big endian
            short that represents the number of characters. Then it 
            reads that many bytes/characters.
        """
        s = struct.unpack_from(">h", self.bytes, self.pos)[0]
        self.pos += 2
        name = struct.unpack_from("%ss"%s, self.bytes, self.pos)[0]
        self.pos += s
        return name.decode("utf-8")
    def getTracks(self):
        """
            returns the generated tracks, should be called after load.
        """
        return self.tracks


class MeshWriter:
    """
        Class for writing meshes. 
        
        version -1
    """
    def __init__(self, tracks):
        """
            Starts a reader over the provided bytes.
            Args:
                data: bytes-like object with mesh tracks. Will be used 
                      with struct.unpack_from.
        """
        self.tracks = tracks
        self.version = -1

    def save(self, filename):
        """
            Opens the file and writes the bytes. 
        """
        with open(filename, 'wb') as self._out:
            self.writeInt( self.version )
            self.writeInt( len(self.tracks) )
            for track in self.tracks:
                self.writeUTFString( track.name )
                mesh_count = len(track.meshes)
                self.writeInt(mesh_count)
                for key in track.meshes:
                    mesh = track.meshes[key]
                    self.writeInt(key)
                    self.writeInt( len(mesh.positions) )
                    self.writeDoubles( mesh.positions )
                    self.writeInt( len(mesh.connections) )
                    self.writeInts(mesh.connections)
                    self.writeInt( len(mesh.triangles) )
                    self.writeInts(mesh.triangles)
            
                
    def writeInt(self, n):
        """
            Writes a single 4 byte int big endian.
        """
        bs = struct.pack(">i", n)
        return self._out.write(bs)
    
    def writeInts(self, ns):
        """
            Writes n 4 byte big-endian ints.
        """
        bs = struct.pack(">%si"%len(ns), *ns)
        return self._out.write(bs)
    
    def writeDoubles(self, ds):
        """
            Writes n 8 byte floating point values, big endian.
            Args:
                ds: iterable of doubles.
        """
        bs = struct.pack(">%sd"%len(ds), *ds)
        return self._out.write(bs)
        
    def writeUTFString(self, strng):
        """
            Writes a utf string, the first 2 bytes are a big endian
            short that represents the number of characters. Then it 
            writes that many bytes/characters.
            The full range of utf8 is not supported, it should be compatible with java's
            java/io/DataOutputStream.html#writeUTF(java.lang.String)
        """
        enc = strng.encode("utf-8")
        s = len(enc)
        bs = struct.pack(">h", s)
        c = self._out.write(bs)
        bs = struct.pack("%ss"%s, enc)
        c += self._out.write(bs)
        return c

class Track:
    """
        Represents a mesh track, which has a name and an indexed collection
        of meshes.
    """
    def __init__(self, name):
        self.name = name;
        self.meshes = {}
    def addMesh(self, frame, mesh):
        self.meshes[frame] = mesh
        
class Mesh:
    """
        Data 
    """
    def __init__(self, positions, connections, triangles):
        self.positions = positions
        self.connections = connections
        self.triangles = triangles

def loadMeshTracks(filename):
    """
      For opening mesh files generated by our Deformable Mesh plugin.
      Args:
        filename: a str to be used with open.
    """
    with open(filename, 'rb') as f:
        data = f.read()
        reader = MeshReader(data)
        reader.load()
        return reader.tracks
    return None

def saveMeshTracks(tracks, filename):
    """
        Saves the tracks to the file provided.
        Args:
            tracks: a list of tracks.
            filename: path to the file to be saved to.
    """
    filename = str(filename) #in case a pathlib.Path is used.
    writer = MeshWriter(tracks)
    writer.save(filename)
    
def createJsonOutput(tracks, output_file):
    """
        Turns provide tracks into a dictionary, then writes that to the
        provided file.
        
        Args:
            output_file: filename that json will be written to.
    """
    track_dictionary = {}
    id_num = 0
    for track in tracks:
        single = {"name" : track.name, "meshes": {}}
        for k in track.meshes:
            mesh = track.meshes[k]
            packed = {}
            
            packed["points"] = [ (mesh.positions[i], mesh.positions[i+1], mesh.positions[i+2]) for i in range(0, len(mesh.positions), 3)]
            packed["triangles"] = [ (mesh.triangles[i], mesh.triangles[i+1], mesh.triangles[i+2]) for i in range(0, len(mesh.triangles), 3) ]
            single["meshes"][k] = packed
            
        track_dictionary["id_%s"%id_num] = single
        id_num += 1
        
    with open(output_file, 'w') as out:
        json.dump(track_dictionary, out, indent="  ")

if __name__=="__main__":
    import sys
    print("testing purposes only")
    tracks = loadMeshTracks(sys.argv[1])
    print("%d tracks loaded"%len(tracks))
    for track in tracks:
        print("%s:"%track.name)
        meshes = track.meshes
        for mesh in meshes:
            print("\tframe: %s with %d positions, %d connections, %d triangles"%(mesh, len(meshes[mesh].positions), len(meshes[mesh].connections), len(meshes[mesh].triangles)))

    
