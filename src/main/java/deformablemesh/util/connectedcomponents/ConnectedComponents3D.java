package deformablemesh.util.connectedcomponents;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ConnectedComponents3D {
    final List<ConnectedComponents2D> pixels = new ArrayList<>();
    int width;
    int height;

    //Contains all of the points for an associated value
    TreeMap<Integer,List<int[]>> log = new TreeMap<>();

    //list of centroids, x,y,weight they are stored as doubles
    List<double[]> output;

    int last_added;

    /**
     * Convenience method for getting all of the regions. This will return
     *
     * @param short_threshed
     * @return
     */
    static public List<Region> getRegions(ImageStack short_threshed){
        ConnectedComponents3D cc3d = new ConnectedComponents3D();
        cc3d.firstPass(short_threshed);
        cc3d.secondPass(short_threshed);

        return cc3d.log.entrySet().stream().map(e -> new Region(e.getKey(), e.getValue())).collect(Collectors.toList());
    }
    /*
        Takes a masked image and performs a first pass connected regions filter on it.
        Goes through pixel by pixel and checks its top and left neighbors for values
        Then it marks its what value this pixel should be.
    */
    private void firstPass(ImageStack threshed){
        for(int i = 1; i<=threshed.size(); i++){
            ConnectedComponents2D cc2d = new ConnectedComponents2D(threshed.getProcessor(i));
            cc2d.process();
            add(cc2d, i);
        }
    }
    static class Mapping{
        final Integer A;
        final Integer B;
        final int hc;
        Mapping(Integer a, Integer b){
            A = a;
            B = b;
            hc = A + B<<16;
        }
        @Override
        public int hashCode(){
            return hc;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj==this){
                return true;
            }
            Mapping obj2 = (Mapping) obj;
            return obj2.A.equals(A) && obj2.B.equals(B);
        }
        @Override
        public String toString(){
            return "Mapping[" + A + "::" + B +"]";
        }
    }

    /**
     * Adds a cc2d and uses the backing data for tracking. The maps will be broken after
     * the values have been included. So the cc2d log data is from original mapping.
     *
     * @param cc2d
     * @param slice
     */
    private void add(ConnectedComponents2D cc2d, int slice){
        if(pixels.size() > 0){
            ConnectedComponents2D previous = pixels.get(pixels.size() - 1);
            Map<Integer, List<int[]>> regions2D = cc2d.getRegions();
            Set<Mapping> finishing = new HashSet<>();
            int free = log.lastKey() + 1;
            Set<Mapping> conjoined = new HashSet<>();
            Set<Integer> keys = regions2D.keySet();

            for(Integer key: keys){

                if(key==0){
                    continue;
                }
                TreeSet<Integer> linked = new TreeSet<>();

                for(int[] px2d: regions2D.get(key)){

                    int l = previous.get(px2d[0],px2d[1]);
                    if(l!=0) {
                        linked.add(l);
                    }
                }

                if(linked.size()==0){
                    //start a new region.
                    //get a valid key value, update cc2d
                    Integer open = free;
                    free++;
                    finishing.add(new Mapping(key, open));

                } else if(linked.size()==1){
                    //add it to appropriate region.
                    for(Integer i : linked){
                        finishing.add(new Mapping(key, i));
                    }
                } else{
                    //grabs and removes the first one.
                    Integer bottom = linked.pollFirst();
                    //The lowest value is used for the other keys.
                    finishing.add( new Mapping(key, bottom) );
                    for(Integer i : linked ){
                        //these will need to be updated in previous slices. All i's are going to the bottom.
                        conjoined.add( new Mapping(i, bottom));
                    }
                }
            }
            /*
                Now that we're done we have 2 maps.
                finishing: cc2d regions to existing regions.
                conjoined: disconnected mappings of existing regions.

                Goal: reduce conjoined.
                N^? version.
                  current: { from, to } & tomap: {}
                  search through every mapping for from/to if either appears. add to tomap.
                  move tomap to current and repeate.
            */
            Map<Integer, Integer> mapped = new HashMap<>();

            TreeSet<Integer> current = new TreeSet<>();
            Set<Integer> toMap = new HashSet<>();
            TreeSet<Integer> checked = new TreeSet<>();
            Deque<Mapping> stack = new ArrayDeque<>(conjoined);
            Deque<Mapping> next = new ArrayDeque<>();

            //stacked is all of the labels from the previous slice that need to be joined.
            while(stack.size()>0){

                Mapping first = stack.pollFirst();
                current.add(first.A);
                current.add(first.B);

                while(current.size() > 0) {
                    //go through the conjoined labels and find any more instances
                    //of the current mappings. This will create clusters of conjoined labels
                    //that all need to be mapped together.
                    for (Mapping map : stack) {
                        if (current.contains(map.A)) {
                            toMap.add(map.B);
                        } else if (current.contains(map.B)) {
                            toMap.add(map.A);
                        } else {
                            next.add(map);
                        }
                    }
                    //there are no more conjoined instance of current labels.
                    checked.addAll(current);
                    current.clear();

                    //More labels were added to be conjoined for mapping.
                    current.addAll(toMap);
                    toMap.clear();

                    //reduce the stack to only labels that haven't been checked.
                    stack.clear();
                    stack.addAll(next);
                    next.clear();
                }
                //found all of the mappings for the first map to merge the labels
                //to the lowest remaining label value.
                Integer bottom = checked.pollFirst();
                for(Integer i: checked){
                    mapped.put(i, bottom);
                }
                checked.clear();
            }

            //shift old pixels that
            for(Integer i: mapped.keySet()){
                Integer target = mapped.get(i);
                List<int[]> source = log.get(i);
                List<int[]> dest = log.get(target);
                for(int[] px: source){
                    dest.add(px);
                    set(px, target);
                }
                log.remove(i);
            }
            pixels.add(cc2d);
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       for(Mapping m : finishing){
            Integer label;
            if(mapped.containsKey(m.B)){
                label = mapped.get(m.B);
            } else{
                label = m.B;
            }

            List<int[]> px = regions2D.get(m.A);
            List<int[]> destination = log.computeIfAbsent(label, ArrayList::new);

            for(int[] p: px){
                destination.add(new int[]{p[0], p[1], slice});
                //regions no longer match labels!
                cc2d.set(p[0], p[1], label);
            }
            }


        } else{
            pixels.add(cc2d);
            Map<Integer, List<int[]>> regions2D = cc2d.getRegions();
            for(Integer key: regions2D.keySet()){
                List<int[]> px = log.computeIfAbsent( key, ArrayList::new );
                for(int[] px2d : regions2D.get(key)){
                    px.add(new int[]{px2d[0], px2d[1], slice});
                }
            }
        }

    }

    /**
     * This breaks the backing cc2d. Regions will no longer map labels.
     * @param xyz xy and slice value. z goes from 1 to N slices.
     * @param value
     */
    private void set(int[] xyz, int value){
        pixels.get(xyz[2]-1).set(xyz[0], xyz[1], value);
    }

    /**
     * Labels the stack with the regions from the first pass.
     *
     * @param stack
     */
    private void secondPass(ImageStack stack){
        for(Integer key: log.keySet()){
            List<int[]> px = log.get(key);
            for(int[] x: px){
                stack.getProcessor(x[2]).set(x[0], x[1], key);
            }
        }
    }



    public static void main(String[] args){
        new ImageJ();

        File output;
        File base;
        ImagePlus plus;

        base = args.length >= 1? new File(args[0]) : new File(ij.IJ.getFilePath("select mosaic image"));

        plus = ij.IJ.openImage(base.getAbsolutePath());
        ImageStack threshed = new ImageStack(plus.getWidth(), plus.getHeight());
        for(int i = 1; i<=plus.getNSlices(); i++){
            ImageProcessor proc = plus.getStack().getProcessor(i).convertToShort(false);
            threshed.addSlice( proc );
        }
        List<Region> regions = ConnectedComponents3D.getRegions(threshed);

        new ImagePlus("blobbed", threshed).show();

    }

}
