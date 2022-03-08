import struct, json

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
