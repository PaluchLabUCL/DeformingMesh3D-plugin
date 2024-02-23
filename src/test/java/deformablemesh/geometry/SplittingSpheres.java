package deformablemesh.geometry;

import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.util.Vector3DOps;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplittingSpheres {
    public static void main(String[] args){
        MeshFrame3D mesh = new MeshFrame3D();
        mesh.showFrame(true);
        mesh.addLights();
        //create a spherical mesh
        double[] center = new double[]{0, 0, 0};
        DeformableMesh3D m = RayCastMesh.sphereRayCastMesh(2);
        m.translate(center);
        m.scale(0.025, center);
        //place the furrow at the center
        Furrow3D f = new Furrow3D(center, Vector3DOps.nzhat);
        List<DeformableMesh3D> meshes = f.sliceMesh(m);
        Furrow3D reversed = new Furrow3D(f.cm,
                new double[]{-f.normal[0], -f.normal[1], -f.normal[2]}
        );
        List<DeformableMesh3D> splits = new ArrayList<>();
        Color[] colors = {new Color(255, 0, 0, 79), new Color(0,0, 255, 79)};
        int ci = 0;

        InterceptingMesh3D im = new InterceptingMesh3D(m);


        List<List<Node3D>> nodes = f.splitNodes(m.nodes);

        for(List<Node3D> side: nodes){
            List<Interceptable> interceptables = Arrays.asList(reversed, f, im);
            if(side.size()==0){
                continue;
            }
            double[] c = new double[3];
            for(Node3D n: side){
                double[] xyz = n.getCoordinates();
                c[0] += xyz[0];
                c[1] += xyz[1];
                c[2] += xyz[2];
            }
            c[0] = c[0]/side.size();
            c[1] = c[1]/side.size();
            c[2] = c[2]/side.size();
            DeformableMesh3D a = RayCastMesh.rayCastMesh(interceptables, c, 2);
            splits.add(a);
        }

        //split it
        //BUG the split mesh has a point across the midline.
        DeformableMesh3D a = splits.get(0);
        DeformableMesh3D b = splits.get(1);
        System.out.println(a.getBoundingBox());
        System.out.println(b.getBoundingBox());

        a.setColor(Color.RED);
        a.setShowSurface(true);
        a.create3DObject();
        b.setColor(Color.BLUE);
        b.setShowSurface(true);
        b.create3DObject();
        mesh.addDataObject(b.data_object);
        mesh.addDataObject(a.data_object);
        f.create3DObject();
        mesh.addDataObject(f.getDataObject());


    }
}
