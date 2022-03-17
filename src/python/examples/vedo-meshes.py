#!/usr/bin/env python

import binarymeshformat as bmf
import vedo.mesh
import vedo

import sys
import time

def getVedoMesh( bmfmesh ):
	points = [];
	triangles = []
	p = bmfmesh.positions
	t = bmfmesh.triangles
	
	for i in range( len(bmfmesh.positions)//3):
		point = [ p[3*i], p[3*i+1], p[3*i+2] ]
		points.append(point)
	for i in range( len(bmfmesh.triangles) // 3 ):
		triangle = [ t[i*3] , t[i*3+1], t[i*3 + 2] ]
		triangles.append( triangle );
	
	return vedo.mesh.Mesh( [ points, triangles ] )
		
def showMeshes(plotter,  bmf_meshes , colors=None):
	x = []
	if colors is None:
		colors = [i for i in range(len(bmf_meshes))]
	for i, bmesh in enumerate(bmf_meshes):
		vmesh = getVedoMesh( bmesh )
		vmesh.color(colors[i]).lineColor('black')
		x.append(vmesh)
	plotter += x
	return x

used_colors = []
def getColor( track_name ):
	if track_name in used_colors:
		return used_colors.index(track_name) + 1
	used_colors.append(track_name)
	return len(used_colors)

if __name__ == "__main__":
	tracks = bmf.loadMeshTracks(sys.argv[1])
	print("loaded %s tracks"%len(tracks) )
	
	frames = set()
	
	for track in tracks:
		for k in track.meshes:
			frames.add(k)
	
	plotter = vedo.Plotter(interactive=False)
	plotter.show()
	save = False
	while True:
		for frame in frames:
			to_see = []
			colors = []
			for track in tracks: 
				if frame in track.meshes.keys():
					to_see.append( track.meshes[frame] )
					colors.append( getColor(track.name) )
			added = showMeshes(plotter, to_see, colors)
			plotter.show()
			if save:
				plotter.screenshot("snapshot-%04d.png"%frame)
			
			for item in added:
				plotter.remove(item)
		break;
