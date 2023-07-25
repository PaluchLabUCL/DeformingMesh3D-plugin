/*-
 * #%L
 * Triangulated surface for deforming in 3D.
 * %%
 * Copyright (C) 2013 - 2023 University College London
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package deformablemesh.util.astar;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by msmith on 10/28/15.
 */
public class AStarXY{
    final static int[][] dir = {
            {0, -1},
            {1, -1},
            {1, 0},
            {1, 1},
            {0, 1},
            {-1, 1},
            {-1, 0},
            {-1, -1}
    };
    public static PossiblePath<int[]> createXYPossiblePath(int[] start){
        return new PossiblePathXY(start);
    }
    public static AStarBasic<int[]> createXYAStar(ImageProcessor ip, int[] start, int[] end){
        Rectangle2D bound = new Rectangle2D.Double(0,0,ip.getWidth(), ip.getHeight());
        Boundary<int[]> bb = pt->bound.contains(pt[0], pt[1]);
        BasicHeuristic<int[]> bh = a1 -> (10*Math.sqrt(Math.pow(a1[0] - end[0], 2) + Math.pow(a1[1] - end[1], 2)));

        BasicCost<int[]> cost = (int[] a, int[] b)->{
                int s = 0;
                int dx = (b[0]-a[0]);
                dx = dx*dx;
                int dy = (b[1] - b[0]);
                dy = dy*dy;

                if(dy>0&&dx>0){
                    s = 14;
                } else{
                    s = 10;
                }

                int p = ip.get(b[0], b[1]);
                if(p>0)
                    s += 100;
                return s;
            };

        History<int[]> history = new History<int[]>(){
            ImageProcessor proc = new FloatProcessor(ip.getWidth(), ip.getHeight());
            @Override
            public double visited(int[] o) {
                return proc.getf(o[0], o[1]);
            }

            @Override
            public void visit(int[] o, double d) {
                proc.setf(o[0], o[1], (float)d);
            }
        };

        ChoiceGenerator<int[]> generator = (pt)-> Arrays.stream(dir).map(a -> new int[]{a[0] + pt[0], a[1] + pt[1]}).collect(Collectors.toList());

        return new AStarBasic<>(bb, bh, cost, generator, history);
    }

}


class PossiblePathXY implements PossiblePath<int[]>{
    double h;
    double distance;
    List<int[]> path = new ArrayList<>();
    public PossiblePathXY(int[] start){
        h = 0;
        distance = 0;
        path.add(start);
    }

    public PossiblePathXY(PossiblePathXY path){
        this.path.addAll(path.path);
        distance = path.distance;
    }
    public int[] getEndPoint(){
        return path.get(path.size()-1);
    }

    public void addPoint(int[] p, double cost, double heuristic){
        h = heuristic;
        distance += cost;
        path.add(p);
    }

    @Override
    public double getDistance() {
        return distance;
    }

    @Override
    public double getHeuristic() {
        return h;
    }

    @Override
    public List<int[]> getPath() {
        return path;
    }

    public PossiblePath<int[]> duplicate(){
        return new PossiblePathXY(this);
    }

    @Override
    public boolean sameDestination(int[] d) {
        int[] a = getEndPoint();
        return a[0]==d[0]&&a[1]==d[1];
    }
}
