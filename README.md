# DeformingMesh3D
ThreeD image segmentation algorithm, for roundish cells.

# Javascript console examples

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
