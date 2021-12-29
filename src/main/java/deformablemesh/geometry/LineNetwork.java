package deformablemesh.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LineNetwork {

    List<DeformableLine3D> snakes;
    Map<DeformableLine3D, Integer> offsets;


    static class SharedNode{
        int global;
        List<DeformableLine3D> attached;
        List<Integer> locations;
        SharedNode(){
            attached = new ArrayList<>();
            locations = new ArrayList<>();
        }
        public void attach(DeformableLine3D line, int loc){
            attached.add(line);
            locations.add(loc);
        }
    }

    public void update(){
        int n = 0;
        for(DeformableLine3D line: snakes){
            offsets.put(line, n);
            n += line.nodes.size();
        }
        //total number of points, minus the point that is shared.




    }


}
