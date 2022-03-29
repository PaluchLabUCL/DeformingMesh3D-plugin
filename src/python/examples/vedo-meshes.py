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
		vmesh.color(colors[i]).alpha(0.5).lineColor("black")
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
	save = False
	
	tracks = bmf.loadMeshTracks(sys.argv[1])
	print("loaded %s tracks"%len(tracks) )
	
	frames = set()
	
	for track in tracks:
		for k in track.meshes:
			frames.add(k)
	
	frames = list(frames)
	nFrames = len(frames)
	
	plotter = vedo.Plotter(interactive= not save)
	plotter.index = 0;
	plotter.meshes = []
	plotter.running = True
	
	#def addCallback(self, eventName, func, priority=0.0) 
	def finishPlotter( evt ):
		print( "plotter clossing")
		plotter.running = False
		plotter.index = nFrames
		plotter.closeWindow()
		
	def changeFrame(evt):
		kp = evt['keyPressed']
		if kp == 'comma':
			plotter.index -= 1
			if plotter.index < 0:
				plotter.index  = nFrames - 1
			
			
		elif kp == 'period':
			plotter.index += 1
			if plotter.index == nFrames:
				plotter.index = 0
		else:
			if kp == 'b':
				finishPlotter(evt)
			return
		plotter.remove(plotter.meshes)
		plotter.meshes.clear()
		frame = frames[plotter.index]
		print("frame: %s index: %s"%(frame, plotter.index) )
		to_see = []
		colors = []
		for track in tracks: 
			if frame in track.meshes.keys():
				to_see.append( track.meshes[frame] )
				colors.append( getColor(track.name) )
		plotter.meshes += showMeshes(plotter, to_see, colors)
		plotter.render()
	
		
	if not save:
		plotter.addCallback("KeyPress", changeFrame)
	while plotter.index < nFrames:
		frame = frames[plotter.index]
		print("frame: %s index: %s %s"%(frame, plotter.index, plotter) )
		to_see = []
		colors = []
		for track in tracks: 
			if frame in track.meshes.keys():
				to_see.append( track.meshes[frame] )
				colors.append( getColor(track.name) )
		plotter.meshes += showMeshes(plotter, to_see, colors)
		plotter.show()
		
		if save:
			plotter.screenshot("snapshot-%04d.png"%frame)
			plotter.index+=1
		else:
			break
	
		plotter.remove(plotter.meshes)
		plotter.meshes.clear()
	
