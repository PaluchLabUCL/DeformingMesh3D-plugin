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
class Track:
    """
        Represents a mesh track, which has a name and an indexed collection
        of meshes. It has two members. 
        
        name : a string to identify the track.
        meshes : A dictionary that contains meshes. The keys are the framenumber.
    """
    def __init__(self, name):
        self.name = name;
        self.meshes = {}
    def addMesh(self, frame, mesh):
        self.meshes[frame] = mesh
        
class Mesh:
    """
        Contains the data to represent a mesh. It contains three fields.
        
           positions : Each N point has x,y,z positions. So this is a list
           of 3N floats.
           
           connections : Represents a connection between points i and j.
           Each connection is represented by 2 consecutive integers.
           
           triangles : Represents triangles, or collections of 3 points. i, j, k.
           Represent by 3 consecutive integers.
           
    """
    def __init__(self, positions, connections, triangles):
        self.positions = positions
        self.connections = connections
        self.triangles = triangles
