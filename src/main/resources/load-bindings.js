var CompositeInterceptables = Java.type("deformablemesh.geometry.CompositeInterceptables");
var InterceptingMesh3D = Java.type("deformablemesh.geometry.InterceptingMesh3D");
var DoubleArray = Java.type("double[]");
var DeformableMesh3D = Java.type("deformablemesh.geometry.DeformableMesh3D");
var RayCastMesh = Java.type("deformablemesh.geometry.RayCastMesh");
var ImagePlus = Java.type("ij.ImagePlus");
var MeshImageStack = Java.type("deformablemesh.MeshImageStack");
var WireframeMesh = Java.type("deformablemesh.geometry.WireframeMesh");
var Color = Java.type("java.awt.Color");
var ArrayList = Java.type("java.util.ArrayList");
var Track = Java.type("deformablemesh.track.Track");
var File = Java.type("java.io.File");
var ImageEnergyType = Java.type("deformablemesh.externalenergies.ImageEnergyType");
//var Graph = Java.type("lightgraph.Graph");
var ImageStack = Java.type("ij.ImageStack");
var ColorProcessor = Java.type("ij.process.ColorProcessor");
var ImageStack = Java.type("ij.ImageStack");
var MeshAnalysis = Java.type("deformablemesh.util.MeshAnalysis");

function echo(obj){
    terminal.echo(obj);
}


function snapshotsThreeSixty(steps){
    total = 1260
    mf3d = controls.getMeshFrame3D();
    stack = 0;
    perStep = 1260/steps;
    for(var i = 0; i<steps; i++){

        mf3d.rotateView(perStep, 0);
        img = mf3d.snapShot();
        proc = new ColorProcessor(img);
        if(stack==0){
            stack = new ImageStack(proc.getWidth(), proc.getHeight());
        }
        stack.addSlice(proc);
    }
    plus = new ImagePlus();
    plus.setStack(stack);
    plus.setTitle("rotating snapshot");
    plus.show();
}

function meshToNewTrack(){
  track = controls.getSelectedMeshTrack();
  mesh = controls.getSelectedMesh();
  frame = controls.getCurrentFrame();
  controls.clearMeshFromTrack(track, frame);
  controls.startNewMeshTrack(frame, mesh);
  controls.selectMesh(mesh);
}


function restartOffscreenCanvas(){
  mf3d = controls.getMeshFrame3D();
  can = mf3d.getCanvas();
  can.destroyOffscreenCanvas();
  can.createOffscreenCanvas();
}

function normalizeColors(){
  tracks = controls.getAllTracks();
  for(i = 0; i<tracks.size(); i++){
      track = tracks.get(i);
      track.setColor( new Color(i+1));
  }
}