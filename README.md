# DeformingMesh3D
ThreeD image segmentation algorithm, for roundish cells.

# Changes

0.35

- Initial window size checks monitor size and attempts to be <=80%
- Mesh initialization zoom keeps cursor location the same.
- ctrl + drag to move mesh initialization views.
- 3 and 1 panel views during initialization
- Create and show a binary representation of currently selected mesh.
- stl writing improved.
- Added a cursor to the initialization window.
- BUG FIX: load meshes now finishes when canceled.
- BUG FIX: restart meshes, undoable.
- load/save parameters
- Initialization sliders are finer controleld.
- Can now switch image by selecting open image.

0.34

- status panel/mesh color display
- ctrl z /ctrl-shift z undo/redo respectively
- js terminal has suggestions and completions
- track backwards
- frame number displays 1 off of index. (looks like imagej's 1 index scheme)
- exports to ply file.
- Mesh track manager for tracking arranging meshes through time.
- user preferences are saved
