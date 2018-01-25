#!/usr/bin/env python3

import struct


class MeshReader:
	def __init__(self, bytes):
		self.bytes = bytes
		self.pos = 0
		self.tracks = []
	def load(self):
		self.version = self.readInt()
		self.track_count = self.readInt()
		for i in range(self.track_count):
			name = self.readUTFString();
			track = Track(name)
			mesh_count = self.readInt()
			for j in range(mesh_count):
				frame = self.readInt()
				position_count = self.readInt()
				positions = self.readDoubles(position_count)
				connection_count = self.readInt()
				connections = self.readInts(connection_count)
				triangle_count = self.readInt()
				triangles = self.readInts(triangle_count)
				track.addMesh(frame, Mesh(positions, connections, triangles))
			self.tracks.append(track)
				
	def readInt(self):
		i = struct.unpack_from(">i", self.bytes[self.pos:])[0]
		self.pos += 4
		return i
	
	def readInts(self, n):
		ints = struct.unpack_from(">%si"%n, bytes[self.pos:])
		self.pos += 4*n
		return ints
		
	def readDouble(self):
		d = struct.unpack_from("d", self.bytes[self.pos:])[0]
		self.pos+=8
		return d
	
	def readDoubles(self, n):
		doubles = struct.unpack_from("%sd"%n, bytes[self.pos:])
		self.pos += n*8
		return doubles
		
	def readUTFString(self):
		s = struct.unpack_from(">h", self.bytes[self.pos:])[0]
		self.pos += 2
		name = struct.unpack_from("%ss"%s, self.bytes[self.pos:])[0]
		self.pos += s
		return name.decode("utf-8")
	
	def getTracks(self):
		return self.tracks
class Track:
	def __init__(self, name):
		self.name = name;
		self.meshes = {}
	def addMesh(self, frame, mesh):
		self.meshes[frame] = mesh
		
class Mesh:
	def __init__(self, positions, connections, triangles):
		self.positions = positions
		self.connections = connections
		self.triangles = triangles
		
if __name__=="__main__":
        import sys
	print("testing purposes only")
	bytes = open(sys.argv[1], 'rb+').read()
	print(len(bytes))
	readr = MeshReader(bytes)
	readr.load()
	
