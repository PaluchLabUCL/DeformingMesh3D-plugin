# Binary Mesh Format

This is a small python library implementation for reading and writing triangulated meshes.
## Format
A .bmf file contains mesh tracks, where a track is a collection of meshes over time, a mesh 
is a set positions, connections, and triangles.

### Header
The first four bytes are a version string. The current version string should be -1.

The next four bytes are an integer that says how many tracks are in the file.

### Track

The first data in a track is a name, which can be a variable size. The first two bytes indicate the length of the name. The string encoding is a subset of UTF8.

Then each mesh is written.

### Mesh
First a 4 byte int to represent the timepoint the mesh exists at. 

Then a 4 byte integer representing the number of positions values, 8 byte float.
Then a 4 byte integer is use to represent the number of connections indexes, each connection index is a 4 byte integers.
Finally a 4 byte integer is used to represent the number of triangle indexes, each index is a 4 byte integer 

## Usage
```
import binarymeshformat

meshTracks = binarymeshformat.loadMeshes(filename)
```

That will return a list of mesh tracks.
##
