package deformablemesh.util.connectedcomponents;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

  /*
    For performing connected components on 2d images.
  */

public class ConnectedComponents2D{


  //filled after first pass, this has all of the mappings
  ArrayList<int[]> premap;
  
  //This is the actual map that has all of the mappings after they are reduced
  HashMap<Integer,Integer> final_map;
  
  //Contains all of the points for an associated value
  HashMap<Integer,List<int[]>> log;
  
  //list of centroids, x,y,weight
  ArrayList<double[]> output;
  
  int last_added;
  
  public ArrayList<double[]> getCentroids(ImageProcessor threshed){
    firstPass(threshed);
    secondPass(threshed);
    calculateCentroids();
    return output;
  
  }

    /**
     * Takes a binary and performs a first pass connected regions filter on it.
     * Goes through pixel by pixel and checks its top and left neighbors for values
     * Then marks what value this pixel should be.
     * @param threshed binary image
     */
  private void firstPass(ImageProcessor threshed){

    premap = new ArrayList<int[]>();
    last_added = 0;
    
    final_map = new HashMap<>();
    final_map.put(0, 0);
    
    int h = threshed.getHeight();
    int w = threshed.getWidth();
    
    for(int i = 0; i<h; i++){
        for(int j = 0; j<w; j++){
            int x = rowBy(threshed,j,i);
            threshed.putPixel(j,i,x);
        }
    }
    reduceMap();
  }

    /**
     * Essential the Kernel for the firstPast.  Filters pixel by checking
     * for a value.  If yes it takes the number above or the number to the left.
     *
     * If there is both a number above and a number to the left then a map values
     * is added.
     *
     * If the pixel is zero, then there is now change
     *
     * @param threshed image data
     * @param j - x coordinate
     * @param i - y coordinate
     * @return the index for the last added.
     */
  private int rowBy(ImageProcessor threshed, int j, int i){

    int above,left,now;
    above = threshed.getPixel(j,i-1);
    left = threshed.getPixel(j-1,i);
    now = threshed.getPixel(j,i);
    if(now>0){
        if(above>0 && left>0){
            if(above != left){
                    int[] a = {above,left};
                    premap.add(a);
                }
            return above;
        } else if(above>0 || left>0) {
            return above>0?above:left;
        } else{
            last_added += 1;
            int[] a = {last_added,last_added};
            premap.add(a);
            return last_added;
        }
    } else
        return 0;
  }

    /**
     * Goes through the pre-map and groups all of the linking numbers together.
     *
     */
  private void reduceMap(){
    while(premap.size()>0){
        //Set for looping
        int[] next = premap.get(0);
        premap.remove(0);
        HashSet<Integer> next_set = new HashSet<>();
        int source = next[0];
        next_set.add(next[0]);
        next_set.add(next[1]);
        ArrayList<Integer> trying = new ArrayList<>();
        for(Integer e: next_set)
            trying.add(e);
        while(trying.size()>0){
            int cur = trying.get(0);
            trying.remove(0);
            ArrayList<int[]> replacement = new ArrayList<>();
            for(int i=0;i<premap.size(); i++ ){
                int[] test = premap.get(i);
                if(cur==test[0]||cur==test[1]){
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
        for(Integer e: next_set)
            final_map.put(e,source);
    }
    
  }

    /**
     * Uses the map created from the numbered image processor, creates the log, which contains all
     * of the coordinates for each point in the connected region.
     *
     * @param separate image processor with numbers that have been mapped, modifies map to be a binary image 0's or 255's.
     */
  private void secondPass(ImageProcessor separate){

    log = new HashMap<>();
    
    for(Integer v: final_map.values()){
        ArrayList<int[]> points = new ArrayList<>();
        log.put(v,points);
    }
    int h = separate.getHeight();
    int w = separate.getWidth();
    for(int i = 0; i<h; i++){
        for(int j = 0; j<w; j++){
            int cur = separate.getPixel(j,i);
            int rep = final_map.get(cur);
            int[] point = {j, i};
            if(rep!=0){
                log.get(rep).add(point);
                separate.putPixel(j, i, 255);
            }
        }
    }
  }
  
  private void calculateCentroids(){
    output = new ArrayList<>();

    for(Integer key: log.keySet()){
        //each key represents a region
        if(!key.equals(0)){
            List<int[]> pts = log.get(key);
            double sumx = 0;
            double sumy = 0;
            double weight = pts.size();
            for(int[] pt: pts){
                sumx += pt[0];
                sumy += pt[1];
            }
            double[] next = {sumx/weight,sumy/weight,weight};
            output.add(next);
        }
    }
  }

    /**
     *
     *
     * @return Current list of centroids each double is {x,y,size}
     */
  public ArrayList<double[]> getOutput(){
  /*
        Gets the current arraylist output
  */
    return output;
  }

  public HashMap<Integer, List<int[]>> getPoints(){
      return log;
  }

}
