#!/usr/bin/env python3

import binarymeshformat as bmf

if __name__ == "__main__":
    tracka = bmf.Track("red")
    trackb = bmf.Track("blue")

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

    mesh = bmf.Mesh(pos, con, tri)
    
    tracka.addMesh(0, mesh)
    tracka.addMesh(1, mesh)
    trackb.addMesh(2, mesh)
    trackb.addMesh(3, mesh)
    
    bmf.saveMeshTracks([tracka, trackb], "test.bmf")
    
    loaded = bmf.loadMeshTracks("test.bmf")
    
    for t in loaded:
        print(t.name, len(t.meshes))
    
