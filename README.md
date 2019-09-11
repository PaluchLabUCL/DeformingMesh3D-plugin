# DeformingMesh3D
ThreeD image segmentation algorithm, for roundish cells.

## installation

The easiest way to install this plugin is through the [Fiji](https://fiji.sc/) update site.

Run Fiji, choose the menu "help", select "update..." after updating there will be a dialog that has "Manage Update Sites".

https://sites.imagej.net/Odinsbane

Once the site has been added, updating fiji should cause it to download and install the deforming mesh plugin
which can be found in "plugins"->"PL_Mesh3D"->"Deforming Mesh 3D", and JFilament will also be installed.

## Documentation

Original Documentation can be found at, [LMCB UCL](https://www.ucl.ac.uk/lmcb/meshplugin) website.


# Javascript console examples

The best feature of the javascript console is that it has auto complete. Start by typing a command, especially by
using the 'controls' object, and press tab and a selection of options will be shown.


## Changing the color scheme (>=0.36)

```javascript
mf = controls.getMeshFrame3D();
mf.setBackgroundColor(Color.BLACK);
controls.setVolumeColor(Color.GREEN);
```
set it back.
```javascript
mf.setBackgroundColor(Color.WHITE);
controls.setVolumeColor(Color.GREEN);
```

## Surface Plots

There are two surface plots available. Intensity or Curvature.

```javascript

sp = controls.intensitySurfacePlot();
sp.processAndShow();

```

We can set a clipping range, and change the colors used. We also can access the MeshFrame and  set the background color.

```javascript

  cp = controls.curvatureSurfacePlot();
  cp.process();
  cp.setMax(cp.getMax()*0.8);
  cp.setHighColor(Color.YELLOW);
  cp.setLow(Color.BLUE);
  cp.show();
```

The surface plots have quite a few controls.

## Iterating through all of the frames and deforming each existing mesh 100 steps.

```javascript
n = controls.getNFrames();
tracks = controls.getAllTracks();
for(var i = 0; i<n; i++){
    controls.toFrame(i)
    ts = tracks.size();
    for( var j = 0; j<ts; j++){
        track = tracks.get(j);
        if(track.containsKey(i)){
            mesh = track.getMesh(i);
            print(i + ", " + j + ", " + mesh);
            controls.selectMesh(mesh);
            controls.deformMesh(100);
        }
    }
}
```

Creates a movie by taking a snapshot and rotating the viewing platform each frame.

```javascript
ImageStack = Java.type("ij.ImageStack");
ColorProcessor = Java.type("ij.process.ColorProcessor");
ImageStack = Java.type("ij.ImageStack");

controls.submit( function(){
    mf3d = controls.getMeshFrame3D();
    stack = 0;
    for(var i = 0; i<360; i++){
        c = Math.cos(i*3.14/180)*Math.cos(0.1);
        s = Math.sin(i*3.14/180)*Math.cos(0.1);
        z = Math.sin(0.1)
        controls.nextFrame();
        mf3d.lookTowards([c, s, z], [0, 0, 1]);
        img = mf3d.snapShot();
        proc = new ColorProcessor(img);
        if(stack==0){
            stack = new ImageStack(proc.getWidth(), proc.getHeight());
        }
        stack.addSlice(proc);
    }
    plus = new ImagePlus();
    plus.setStack(stack);
    plus.show();

});
```
# Changes
0.36

- javascript available actions improved.
- surface plots available through javascript interface.
- steric forces improved.
- clear mesh from current frame.
- transformations to look at plane.


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
- Initialization sliders are finer controlled.
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

# Reference

[Chapter 19 - An active contour ImageJ plugin to monitor daughter cell size in 3D during cytokinesis](https://www.sciencedirect.com/science/article/pii/S0091679X16300607?via%3Dihub)

MB Smith, A Chaigne, EK Paluch

https://doi.org/10.1016/bs.mcb.2016.05.003