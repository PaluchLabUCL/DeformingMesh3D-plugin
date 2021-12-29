package deformablemesh.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.nio.file.Paths;
import java.util.*;

public class DistanceTransformMosaicImage {

    Map<Integer, Blob> blobs = new HashMap<>();
    Blob background;
    ImagePlus mosaic;
    boolean fullTransform = false;
    public DistanceTransformMosaicImage(ImagePlus mosaic){
        this.mosaic =mosaic;
    }

    public void createCascades(){
        for(Blob blob: blobs.values()){
            blob.createCascade();
        }

        if(fullTransform){
            background = getBackgroundBlob();
            background.createGrowingCascade(1);
        }
    }

    public void createGrowingCascades(){
        for(Blob blob: blobs.values()){
            blob.createGrowingCascade(1);
        }
    }

    public void createScaledCascades(int scale){
        for(Blob blob: blobs.values()){

            blob.createScaledCascade(scale);

        }

        if(fullTransform){
            System.out.println("backgrounding");
            background = getBackgroundBlob();
            background.createGrowingCascade(scale);
        }

    }


    public Blob getBackgroundBlob(){
        ImageStack stack = mosaic.getStack();
        int w = stack.getWidth();
        int h = stack.getHeight();
        Blob background = new Blob(0);
        for(int i = 1; i<=stack.size(); i++){
            ImageProcessor proc = stack.getProcessor(i);
            for(int x = 0; x<w; x++){
                for(int y = 0; y<h; y++){
                    int v = proc.get(x, y);
                    if(v==0){
                        background.addPoint(new int[]{x, y, i});
                    }
                }
            }
        }
        return background;

    }

    public void findBlobs(){

        ImageStack stack = mosaic.getStack();
        int w = stack.getWidth();
        int h = stack.getHeight();
        for(int i = 1; i<=stack.size(); i++){
            ImageProcessor proc = stack.getProcessor(i);
            for(int x = 0; x<w; x++){
                for(int y = 0; y<h; y++){
                    int v = proc.get(x, y);
                    if(v!=0){
                        blobs.computeIfAbsent(v, Blob::new).addPoint(new int[]{x,y,i});
                    }
                }
            }
        }
        blobs.values().forEach(blob -> blob.setImageLimits(w, h, stack.size()+1));
        System.out.println(blobs.size() + " blobs found.");

    }

    public ImagePlus createLabeledImage(){
        ImageStack stack = new ImageStack(mosaic.getWidth(), mosaic.getHeight());
        ImageStack mosStack = mosaic.getStack();
        for(int i = 1; i<=mosStack.size(); i++){
            stack.addSlice(new ShortProcessor(stack.getWidth(), stack.getHeight()));
        }

        for(Blob b: blobs.values()){
            b.labelImage(stack);
        }
        if(fullTransform){
            System.out.println(background.cascade.size() + " background levels to label");
            long start = System.currentTimeMillis();
            background.labelImage(stack);
            System.out.println("labelled background in: " + (System.currentTimeMillis() - start) + "ms");
        }


        ImagePlus plus = mosaic.createImagePlus();
        plus.setStack(stack, 1, mosaic.getNSlices(), mosaic.getNFrames());
        plus.setOpenAsHyperStack(true);
        return plus;


    }

    static class Blob{
        List<int[]> points = new ArrayList<>();
        int lx = 0;
        int ly = 0;
        int lz = 0;
        int hx = 0;
        int hy = 0;
        int hz = 0;
        int w, h, d;
        final int label;
        byte[] stack;
        boolean fill = false;
        int maxLevel = 32;
        int labels = 32;
        int imageWidth, imageHeight, imageDepth;
        Map<Integer, List<int[]>> cascade = new TreeMap<>();

        Blob(int label){

            imageWidth = Integer.MAX_VALUE;
            imageHeight = Integer.MAX_VALUE;
            imageDepth = Integer.MAX_VALUE;

            this.label = label;

        }

        //TODO configure labels min - max transition.

        void labelImage(ImageStack stack){
            int gradient = maxLevel/labels;
            for(Integer i: cascade.keySet()){
                int a = i/gradient;

                if(a>labels) a = labels;

                int distanceLabel;

                //distanceLabel = 1 << (a);
                distanceLabel = a;
                List<int[]> level = cascade.get(i);
                Collections.sort(level, (p1, p2)->Integer.compare(p1[2], p2[2]));
                int lastSlice = -1;
                ImageProcessor proc = null;
                for(int[] pt: level){
                    if(pt[2] != lastSlice){
                        proc = stack.getProcessor(pt[2]);
                        lastSlice=pt[2];
                    }
                    proc.set(pt[0], pt[1], distanceLabel);
                }

            }


        }

        public void createGrowingCascade(int scale){
            List<int[]> full = new ArrayList<>(points);

            w = hx - lx + 1;
            h = hy - ly + 1;
            d = hz - lz + 1;
            System.out.println("creating stack");
            stack = new byte[w*h*d];
            for(int[] f: full){
                set(f, (byte)1);
            }

            int level = 0;
            List<int[]> current = new ArrayList<>();
            List<int[]> replacements = new ArrayList<>();
            for(int[] pt: full) {
                if (!touchesBoundary(pt) && edgePoint(pt)){
                    current.add(pt);
                } else{
                    replacements.add(pt);
                }
            }
            System.out.println("separated out pixels.");

            for(int[] pt: current){
                set(pt, (byte)0);
            }
            full = replacements;
            level++;

            while(level<maxLevel) {
                List<int[]> next = new ArrayList<>();
                if((level +1)%scale == 0){
                    for(int[] pt: current) {
                        List<int[]> pts = neighbors(pt);
                        for(int[] p: pts){
                            set(p, (byte)0);
                            next.add(p);
                        }
                    }
                } else{
                    for(int[] pt: current) {
                        List<int[]> pts = neighbors2D(pt);
                        for(int[] p: pts){
                            set(p, (byte)0);
                            next.add(p);
                        }
                    }
                }
                cascade.put(level, next);
                level++;
                current = next;
            }
            List<int[]> maxed = new ArrayList<>(full.size());
            for(int[] pt: full){
                if(get(pt) == 1){
                    maxed.add(pt);
                }
            }
            cascade.put(level, maxed);

        }


        List<int[]> neighbors(int[] pt){
            List<int[]> neighs = new ArrayList<>(8);
            for(int i = -1; i<=1; i++){
                for(int j = -1; j<=1; j++){
                    for(int k = -1; k<=1; k++){
                        if(get(pt[0] + i, pt[1] + j, pt[2] +k)!=0){
                            neighs.add(new int[]{pt[0] + i, pt[1] + j, pt[2] +k});
                        }
                    }
                }
            }
            return neighs;

        }

        List<int[]> neighbors2D(int[] pt){
            List<int[]> neighs = new ArrayList<>(8);
            for(int i = -1; i<=1; i++){
                for(int j = -1; j<=1; j++){
                    if(get(pt[0] + i, pt[1] + j, pt[2])!=0){
                        neighs.add(new int[]{pt[0] + i, pt[1] + j, pt[2]});
                    }
                }
            }
            return neighs;

        }


        public void createScaledCascade(int scale){
            List<int[]> full = new ArrayList<>(points);

            w = hx - lx + 1;
            h = hy - ly + 1;
            d = hz - lz + 1;
            stack = new byte[w*h*d];
            for(int[] f: full){
                set(f, (byte)1);
            }

            int level = 0;
            while(full.size()>0){

                List<int[]> region = new ArrayList<>();

                if(level==maxLevel){
                    region.addAll(full);
                    cascade.put(level, region);
                    break;
                }

                if((level +1)%scale == 0){
                    for(int[] pt: full){
                        if (edgePoint(pt)){
                            region.add(pt);
                        }
                    }
                } else{
                    for(int[] pt: full){
                        if (edgePoint2D(pt)){
                            region.add(pt);
                        }
                    }
                }
                for(int[] pt: region){
                    set(pt, (byte)0);
                    full.remove(pt);
                }
                cascade.put(level, region);
                level++;
            }
        }

        void createCascade(){
            List<int[]> full = new ArrayList<>(points);

            w = hx - lx + 1;
            h = hy - ly + 1;
            d = hz - lz + 1;
            stack = new byte[w*h*d];
            double start = points.size();

            long time = System.currentTimeMillis();

            for(int[] f: full){
                set(f, (byte)1);
            }

            System.out.println( ((System.currentTimeMillis() - time)/1000.0) + " seconds");
            time = System.currentTimeMillis();
            int level = 0;
            while(full.size()>0){
                if(level > maxLevel){
                    cascade.get(maxLevel).addAll(full);
                    break;
                }
                List<int[]> region = new ArrayList<>();
                List<int[]> next = new ArrayList<>(full.size());
                for(int[] pt: full){
                    if (edgePoint(pt)){
                        region.add(pt);
                    } else{
                        next.add(pt);
                    }
                }
                System.out.println( ((System.currentTimeMillis() - time)/1000.0) + " to find " + region.size() + " px of " + full.size());
                time = System.currentTimeMillis();
                if(region.size() == 0){
                    //no edge points
                    region.addAll(full);
                }

                for(int[] pt: region){
                    set(pt, (byte)0);
                }

                cascade.put(level, region);
                level++;
                System.out.println( ((System.currentTimeMillis() - time)/1000.0) + " to remove " + region.size() + " from " + full.size());
                time = System.currentTimeMillis();
                full = next;

            }


        }
        public void setImageLimits( int width, int height, int depth){
            this.imageWidth = width;
            this.imageHeight = height;
            this.imageDepth = depth;
        }

        void set(int[] xyz, byte v){
            stack[
                                ( xyz[0] - lx )
                          + w * ( xyz[1] - ly )
                  + ( w * h ) * ( xyz[2] - lz )
            ] = v;
        }

        byte get(int[] xyz){
            return get(xyz[0], xyz[1], xyz[2]);
        }

        byte get(int x, int y, int z){

            if( (x<lx) || (x>hx) || (y<ly)|| (y>hy) || z<lz||z>hz){
                return 0;
            }

            return stack[
                                    ( x - lx )
                              + w * ( y - ly )
                      + ( w * h ) * ( z - lz )
                    ];
        }

        boolean touchesBoundary(int[] pt){
            return touchesBoundary(pt[0], pt[1], pt[2]);
        }

        boolean touchesBoundary(int x, int y, int z){
            return ( (x==lx) || (x==hx) || (y==ly)|| (y==hy) || z==lz ||z==hz);
        }

        boolean edgePoint(int[] pt){
            int xi, yi, zi;
            for(int i = -1; i<=1; i++){
                for(int j = -1; j<=1; j++){
                    for(int k = -1; k<=1; k++){
                        xi = pt[0] + i;
                        yi = pt[1] + j;
                        zi = pt[2] + k;
                        if(xi < 0 || yi < 0 || zi < 1 || xi == imageWidth || yi == imageHeight || zi==imageDepth){
                            continue;
                        }
                        if(get(pt[0] + i, pt[1] + j, pt[2] +k)==0){
                            return true;
                        }
                    }
                }
            }

            return false;

        }

        boolean edgePoint2D(int[] pt){

            for(int i = -1; i<=1; i++){
                for(int j = -1; j<=1; j++){
                    if(get(pt[0] + i, pt[1] + j, pt[2])==0) {
                        return true;
                    }
                }
            }

            return false;

        }

        void addPoint(int[] pt){

            if(points.size()==0){
                lx = pt[0];
                ly = pt[1];
                lz = pt[2];
                hx = pt[0];
                hy = pt[1];
                hz = pt[2];
            } else{
                lx = pt[0]<lx?pt[0]:lx;
                hx = pt[0]>hx?pt[0]:hx;
                ly = pt[1]<ly?pt[1]:ly;
                hy = pt[1]>hy?pt[1]:hy;
                lz = pt[2]<lz?pt[2]:lz;
                hz = pt[2]>hz?pt[2]:hz;
            }
            points.add(pt);
        }

    }



    public static void main(String[] args){
        ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        DistanceTransformMosaicImage dtm = new DistanceTransformMosaicImage(plus);

        dtm.findBlobs();
        dtm.createCascades();
        ImagePlus out = dtm.createLabeledImage();
        out.show();
        //IJ.save(out, Paths.get(args[1]).toAbsolutePath().toString());


    }


}
