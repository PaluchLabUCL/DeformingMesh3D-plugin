###
# #%L
# Triangulated surface for deforming in 3D.
# %%
# Copyright (C) 2013 - 2023 University College London
# %%
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
# #L%
###
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

