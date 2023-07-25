#!/usr/bin/env python3

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
    
