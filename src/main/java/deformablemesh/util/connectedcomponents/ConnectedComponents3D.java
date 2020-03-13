package deformablemesh.util.connectedcomponents;

import ij.ImageStack;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectedComponents3D {
    final List<short[]> pixels = new ArrayList<>();
    int width;
    int height;
    //filled after first pass, this has all of the mappings
    ArrayList<int[]> premap;

    //This is the actual map that has all of the mappings after they are reduced
    HashMap<Integer,Integer> final_map;

    //Contains all of the points for an associated value
    HashMap<Integer,ArrayList<int[]>> log;

    //list of centroids, x,y,weight they are stored as doubles
    ArrayList<double[]> output;

    int last_added;

    static public List<Region> getRegions(ImageStack short_threshed){
        ConnectedComponents3D cc3d = new ConnectedComponents3D();
        cc3d.width = short_threshed.getWidth();
        cc3d.height = short_threshed.getHeight();
        for(int i = 1; i<=short_threshed.size(); i++){
            cc3d.pixels.add((short[])short_threshed.getProcessor(i).getPixels());
        }
        System.out.println("first pass");
        cc3d.firstPass(short_threshed);
        System.out.println("second pass");
        cc3d.secondPass(short_threshed);
        System.out.println("calculating centroids");

        System.out.println(cc3d.log.size());
        return cc3d.log.entrySet().stream().map(e -> new Region(e.getKey(), e.getValue())).collect(Collectors.toList());
    }
    /*
        Takes a masked image and performs a first pass connected regions filter on it.
        Goes through pixel by pixel and checks its top and left neighbors for values
        Then it marks its what value this pixel should be.
    */
    private void firstPass(ImageStack threshed){
        premap = new ArrayList<int[]>();
        last_added = 0;

        final_map = new HashMap<Integer,Integer>();
        final_map.put(0,0);

        int h = threshed.getHeight();
        int w = threshed.getWidth();
        int d = threshed.getSize();
        for(int k = 1; k<=d; k++){
            System.out.println(k + " / " + d);
            short[] px = pixels.get(k-1);

            for(int i = 0; i<h; i++){
                for(int j = 0; j<w; j++){

                    int x = rowBy(j,i,k);


                    px[j + i*w] = (short)x;

                }
            }
        }
        reduceMap();
    }
    /*
        This is the function that filters each pixel, if the current pixel
        has a value then it takes the number above, to the left or behind.
        If there is both a number above and a number to the left then a map values
        is add.
    */
    private int rowBy(int j, int i, int k){
        int above,left,behind,now;
        above = (i-1)<0?0:getPixel(j,i-1,k);
        left = (j-1)<0?0:getPixel(j-1,i,k);
        behind = k>1?getPixel(j,i,k-1):0;

        now = getPixel(j,i,k);

        if(now>0){
            //valid point
            if(above>0){
                if(left>0) map(above,left);
                if(behind>0) map(above,behind);

                return above;
            } else if(left>0){
                if(behind>0) map(left,behind);


                return left;
            }else if(behind>0){

                return behind;


            } else{

                last_added += 1;

                int[] a = {last_added,last_added};
                premap.add(a);
                return last_added;

            }
        } else
            return 0;
    }
    public void map(int a, int b){
        int[] m = {a,b};
        premap.add(m);
    }
    public int getPixel(int j, int i, int k){
        //return threshed.getProcessor(k).getPixel(j,i);
        return pixels.get(k-1)[j + i*width];

    }
    /**
     * Eliminates redundant points in the 'pre-map' and leaves final_map with the correct mappings
     **/
    private void reduceMap(){
        System.out.println("reducing map " + premap.size());
        while(premap.size()>0){
            //Set for looping
            int[] next = premap.get(0);
            premap.remove(0);
            HashSet<Integer> next_set = new HashSet<Integer>();
            int source = next[0];
            next_set.add(next[0]);
            next_set.add(next[1]);
            ArrayList<Integer> trying = new ArrayList<Integer>();
            for(Integer e: next_set)
                trying.add(e);
            while(trying.size()>0){
                Integer cur = trying.get(0);
                trying.remove(0);
                ArrayList<int[]> replacement = new ArrayList<int[]>();
                for(int i=0;i<premap.size(); i++ ){
                    int[] test = premap.get(i);
                    if(cur.equals(test[0])||cur.equals(test[1])){
                        int size = next_set.size();
                        next_set.add(test[0]);
                        if(next_set.size()>size){
                            size += 1;
                            trying.add(test[0]);
                        }
                        next_set.add(test[1]);
                        if(next_set.size()>size)
                            trying.add(test[1]);
                    }
                    else
                        replacement.add(test);
                }
                premap=replacement;
            }
            //place value into hashmaps values
            for(int e: next_set)
                final_map.put(e,source);
        }

    }


    private void secondPass(ImageStack stack){
    /*This two jobs, it goes through and does the map so that all of the connected regions
      have their associated points, and it sets the values of the pixels to 255
    */

        log = new HashMap<Integer,ArrayList<int[]>>();

        for(Integer v: final_map.values()){
            ArrayList<int[]> points = new ArrayList<int[]>();
            log.put(v,points);
        }
        int h = stack.getHeight();
        int w = stack.getWidth();
        int d = stack.getSize();
        for(int k = 1; k<=d; k++){
            ImageProcessor separate = stack.getProcessor(k);
            for(int i = 0; i<h; i++){
                for(int j = 0; j<w; j++){
                    int cur = separate.getPixel(j,i);
                    int rep = final_map.get(cur);
                    int[] point = {j,i,k};

                    if(rep!=0){
                        log.get(rep).add(point);
                    }

                }
            }
        }
    }

}
