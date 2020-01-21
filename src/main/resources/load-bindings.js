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

var ImageStack = Java.type("ij.ImageStack");
var ColorProcessor = Java.type("ij.process.ColorProcessor");
var ImageStack = Java.type("ij.ImageStack");

function snapshotsThreeSixty(steps){
    mf3d = controls.getMeshFrame3D();
    stack = 0;
    dt = 2*3.141592/steps;

    for(var i = 0; i<steps; i++){

        c = Math.cos(i*dt)*Math.cos(0.2);
        s = Math.sin(i*dt)*Math.cos(0.2);
        z = Math.sin(0.2)
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
    plus.setTitle("rotating snapshot");
    plus.show();
}