# Binary Mesh Format

This is a small python library implementation for reading and writing triangulated meshes.

## Usage
```
import binarymeshformat as bmf

meshTracks = bmf.loadMeshes(filename)
```

That will return a list of mesh tracks.

## Track structure

A track has a name and a map. The name is just a string and the map maps integers to meshes.

## Mesh structure

The mesh contains points in 3D space and their connections. It uses 3 aspects:

- positions which is an array of x,y,z coordinates. 
- connections which is an array of indexes indicating two points are connected. Two indexes per connection.
- triangles an array of indexes, three indexes per triangle. 

## File Format
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

