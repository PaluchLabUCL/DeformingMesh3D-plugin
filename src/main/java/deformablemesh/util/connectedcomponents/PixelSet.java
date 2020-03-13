package deformablemesh.util.connectedcomponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PixelSet{
    Set<Pixel> pixels = new HashSet<>();
    List<int[]> original = new ArrayList<>();
    PixelSet(){

    }
    boolean add(int[] xyz){
        if(pixels.add(new Pixel(xyz))){
            original.add(xyz);
            return true;
        }
        return false;
    }
    static class Pixel{
        final int x, y, z;
        public Pixel(int[] xyz){
            x = xyz[0];
            y = xyz[1];
            z = xyz[2];
        }
        @Override
        public int hashCode(){
            return x + y + z;
        }
        @Override
        public boolean equals(Object o){
            if(o instanceof Pixel){
                Pixel op = (Pixel)o;
                return op.x==x && op.y==y && op.z == z;
            }
            return false;
        }
    }

}
