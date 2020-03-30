#!/usr/bin/env python3

from meshreader.readers import Track, Mesh, saveMeshTracks, loadMeshTracks


if __name__ == "__main__":
    tracka = Track("red")
    trackb = Track("blue")

    pos = [
    0, 0, 0,
    0, 1, 0,
    0, 1, 1,
    0, 0, 1 
    ]
    con = [
    0, 1,
    1, 2,
    2, 0,
    0, 2,
    2, 3,
    3, 0
    ]
    tri = [
    0, 1, 2,
    0, 2, 3
    ]

    mesh = Mesh(pos, con, tri)
    
    tracka.addMesh(0, mesh)
    tracka.addMesh(1, mesh)
    trackb.addMesh(2, mesh)
    trackb.addMesh(3, mesh)
    
    saveMeshTracks([tracka, trackb], "test.bmf")
    
    loaded = loadMeshTracks("test.bmf")
    
    for t in loaded:
        print(t.name, len(t.meshes))
    
