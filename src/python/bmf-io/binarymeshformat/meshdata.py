class Track:
    """
        Represents a mesh track, which has a name and an indexed collection
        of meshes.
    """
    def __init__(self, name):
        self.name = name;
        self.meshes = {}
    def addMesh(self, frame, mesh):
        self.meshes[frame] = mesh
        
class Mesh:
    """
        Data 
    """
    def __init__(self, positions, connections, triangles):
        self.positions = positions
        self.connections = connections
        self.triangles = triangles
