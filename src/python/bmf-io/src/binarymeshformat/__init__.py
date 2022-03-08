"""
 First iteration of a simple binary mesh format. It saves collections of
 mesh tracks, where a mesh is a triangulated and connected series of points,
 and a track links multiple meshes.
  
 It loads and saves meshes in a very simple format consistent with
  our Fiji plugin, DM3D.
 
 To load a set of mesh tracks use. 
 
 ===
 Classes:
 
     Track : data structure for keeping track of meshes over time.
     
     Mesh : mesh data, points, connections and triangles that represent a triangulated mesh.
 
 ===
 Methods:
    
    saveMeshTracks( tracks, filename ) for saving meshes to a file. tracks
    is a list of Track, and filename is a string path to the file to save.
    
    loadMeshTracks( filename ); Returns a list of tracks.
 
 ===
 Modules
 
   writer classes for writing meshes.
   reader classes for reading meshes.
   meshdata contains the Track and Mesh.
"""


from .writer import saveMeshTracks
from .reader import loadMeshTracks

from .meshdata import Track, Mesh 

