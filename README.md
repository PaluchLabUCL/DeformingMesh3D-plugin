# DM3D
ThreeD image segmentation algorithm, for roundish cells.

The latest documentation can be found at:

[DM3D Github pages](https://franciscrickinstitute.github.io/dm3d-pages/)

## Roadmap 1.0 Draft

As of version 0.8.0 these features have been added. There are some details that need
to be worked out. Especially documentation.

This is the start of a draft for a 1.0 release version. Right now I have quite a few
components that are nearly ready but need to be added. The two major features that I
want included in a 1.0 release.

## installation

The easiest way to install this plugin is through the [Fiji](https://fiji.sc/) update site.

Run Fiji, choose the menu "help", select "update..." after updating there will be a dialog that has "Manage Update Sites".

https://sites.imagej.net/Odinsbane

Once the site has been added, updating fiji should cause it to download and install the deforming mesh plugin
which can be found in "plugins"->"PL_Mesh3D"->"Deforming Mesh 3D", and JFilament will also be installed.


## Documentation

[DM3D Github pages](https://franciscrickinstitute.github.io/dm3d-pages/)

This includes guides for using the plugin, and javadoc for using the plugin via scripting.

# Changes
0.9.7
- improved slice view synchronization
- added sculpt/select items to slice view
- Limited title length
- Changed the way errors are displayed

0.9.6
- updated scijava pom
- added flatlaf to pom
- created a way to substitute image data
- added a way to remove all tracking information
- added a remote prediction class (experimental)
- removed cursor radius controls, added javascript portal
- improved display for hidpi developing with flatlaf for consistency
- Added run file to javascript console
- Added faq to help menu.

0.9.4
- Improved autotracking accessiblity.
- - `linkPossibleTrack()` to link the selected track.
- - `autotrackAvailableTracks()` attempts to link all available in the first frame with the second frame.
- Import from open image has been created, for loading meshes from different image geometries.
- Connection remeshing calculates the expected number of connections differently/better.
- Orient the furrow from the 3D canvas.
- Flip the furrow.
- BUG FIX: empty tracks get removed from the manager.
0.9.2
- 3D viewer has been improved to handle viewing small sections of large images.
0.9.1
- BUG FIX track manager selects correct mesh.
0.9.0
- Selected mesh is now updated in track manager.
- Exposing averaged intensity vs time plot
- exposed frame to frame displacement simple auto tracker.
- Updated mesh file reading to include progress and range checking.

0.8.0
- Select channel from main display.
- Added 3D sculpting - feature preview version.
- mesh track manager and undo/redo follow the state of the ActionManager.
- deformAllMeshes has a limit, can be used in a script.
- restartOffScreenCanvas javascript function to address bug in offscreen canvas.
- scaled units are indicated for connection remesh.
- histogram display improved.
- 3D preview removed for now.
- Meshes can have transparency.

0.7.0
- Creating Binary images algorithm improvement.
- Visualization improvements.

0.6.0
- Feature preview modifier and limited controls to the furrow panel. Debugging for next release.
- created 2d svg/image view and exposed through javascript
- added normals to the data object, that need to be updated.
- light tracks the display.
- objects are lit by a single directional light, and an ambient light.
- lighting can be adjusted in the mesh viewer to find better lighting settings.
- moved the ring controller to be in the controlframe
- exposed mesh sculpting via the furrow3d view input
- BUG FIX slice view 3d scrolling
- made the modification cursor size adjustable.
- ui overhaul
- track manager has select all/ select all before/ and select all after
- track manager modifies names to avoid duplicates.

0.5.0
- moved initializer to a new frame.
- Created a non-3d mode
- BUG FIX names can break mesh loading.
- BUG FIX track manager changes show surface.
- Split mesh and deform partial available from java script.
- Some linear plots.
- Moved manage tracks to a tabbed pane.
- Improved non-3d version to run program for running over x11 forwarded.
- steric mesh uses shorted distance along normal.
- improved redo/undo
- "SegmentationController.selectTrack" was broken.
- Bug fix: BinaryInterceptible was off by 1 for z axis.
- Furrow3D shows the texture.
- Furrow input panel is changing design.
- Histogram input is smaller and adjustabble.
- show/hide furrow.
- Meshes are now selectable from the furrow screen.
- Worked on the import meshes. Created 4 types of import. Checked each on. do and undo seemed to work.
- Removed a bug where removing a track would empty all of it's meshes, breaking undo.
- Added labels to the histogram control.
- Implemented the curvature smoothing algorithm. Not sure if it does anything.
- Channels now sync to the frame when they're added.
- BinaryInterceptible considers edge of image points as edges.
- Added limited transparency control
- Added mesh projections to the Furrow tab.
- Restructuring the furrow tab layout.
- Remove the field 'original_stack'
- Adding a non-energy option.
- Added channel select when selecting an open image plus.

0.3.9
- Added connection remesher to control panel.
- Multi channel support 
  - Display multiple volumes as channels
  - Select a single channel from a multi-channel image
- Steric Energy improvements
- Exposed auto-detect meshes through javascript


0.3.8
- Contrast preview dialog matches the clamped values.
- Removed the normalizer weight from controls, added steric neighbors.
- Exposed connection remesher in javascript with min/max lengths.
- Contrast adjustment for displayed channels.

0.3.7
- Added key command actions to 3D display.
- Added help screen to hud to display key commands.
- Added mesh export options for wire-frame and shell meshes.
- Swing javascript terminal has a shortcut execution mode.
- Contrast adjustment tool.
- Selecting open image returns to the current frame for swapping energies.
- Improved 3D volume data handling, now is a DataObject.
- Created a 3D surface for volume textures.
- Improved 3D geometry display performance.
- python meshes have a reader and writer.
- Bug fix: color selection could cause infinite loop.
- Connected components refactoring.
- Connection based remeshing *
- Manual Mesh editing *
- Forces can be displayed as vectors *
- VolumeDataObject is accessible through MeshFrame3D api for displaying multi-channels. *

* only available through javascript.

0.3.6

- javascript available actions improved.
- surface plots available through javascript interface.
- steric forces improved.
- clear mesh from current frame.
- transformations to look at plane.


0.3.5

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
- Initialization sliders are finer controlled.
- Can now switch image by selecting open image.

0.3.4

- status panel/mesh color display
- ctrl z /ctrl-shift z undo/redo respectively
- js terminal has suggestions and completions
- track backwards
- frame number displays 1 off of index. (looks like imagej's 1 index scheme)
- exports to ply file.
- Mesh track manager for tracking arranging meshes through time.
- user preferences are saved

# Reference

[Chapter 19 - An active contour ImageJ plugin to monitor daughter cell size in 3D during cytokinesis](https://www.sciencedirect.com/science/article/pii/S0091679X16300607?via%3Dihub)

MB Smith, A Chaigne, EK Paluch

https://doi.org/10.1016/bs.mcb.2016.05.003
