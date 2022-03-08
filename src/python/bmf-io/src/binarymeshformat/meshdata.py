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
