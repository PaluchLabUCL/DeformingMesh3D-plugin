package deformablemesh.util.astar;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A path finding routine.
 *
 * Created by msmith on 2/4/14.
 */
public class AStarBasic<T> {
    boolean debug = false;


    Boundary<T> bounds;
    BasicHeuristic<T> heuristic;
    BasicCost<T> cost;
    ChoiceGenerator<T> generator;
    History<T> history;
    T goal;


    Comparator<PossiblePath<T>> hc = new Comparator<PossiblePath<T>>() {
        @Override
        public int compare(PossiblePath<T> o1, PossiblePath<T> o2) {

            return Double.compare(o1.getDistance() + o1.getHeuristic(), o2.getDistance() + o2.getHeuristic());
        }

        @Override
        public boolean equals(Object obj) {
            return obj==this;
        }
    };

    PriorityQueue<PossiblePath<T>> open = new PriorityQueue<>(500, hc);

    public AStarBasic(Boundary<T> bounds, BasicHeuristic<T> heuristic, BasicCost<T> cost, ChoiceGenerator<T> generator, History<T> history){

        this.bounds = bounds;
        this.heuristic = heuristic;
        this.cost = cost;
        this.generator = generator;
        this.history = history;

    }
    public void setGoal(T dest){
        goal = dest;
    }
    public PossiblePath<T> findPath(PossiblePath<T> starting){

        boolean test = true;
        PossiblePath<T> best = starting;
        open.add(starting);
        while(test){
            expandBest();
            best = open.peek();
            if(best.sameDestination(goal)){
                test = false;
            }
        }

        return best;

    }

    void expandBest(){

        PossiblePath<T> p = open.poll();
        T last = p.getEndPoint();
        List<T> choices = generator.getChoices(last);
        for(T c: choices){

            if(bounds.contains(c)){

                double visited = history.visited(c);

                PossiblePath<T> q = p.duplicate();
                q.addPoint(c, cost.step(last, c), heuristic.getCost(c) );

                if(visited>0){

                    if(visited>(q.getHeuristic() + q.getDistance())){
                        //the new position is better

                        Iterator<PossiblePath<T>> iter = open.iterator();
                        while(iter.hasNext()){
                            PossiblePath<T> pp = iter.next();
                            if(pp.sameDestination(c)){
                                iter.remove();
                                break;
                            }
                        }

                        open.add(q);
                        history.visit(c, q.getHeuristic() + q.getDistance());

                    } else{
                        //That is a worse path to that point do not add it.
                    }

                } else{

                    //new point lets add it.
                    history.visit(c, (q.getHeuristic() + q.getDistance()));
                    open.add(q);

                }

            }

        }

    }

    public static void main(String[] args){
        ImageJ.main(args);
        final ImageProcessor proc = new ColorProcessor(200, 200);
        proc.setColor(255);
        proc.fillOval(10, 10, 75, 75);
        final int[] a = new int[]{0,0};
        final int[] b = new int[] {150, 150};



        AStarBasic<int[]> basic = AStarXY.createXYAStar(proc, a, b);
        basic.setGoal(b);
        List<int[]> path = basic.findPath(new PossiblePathXY(a)).getPath();

        proc.setColor(Color.WHITE);
        for(int i = 0; i<path.size()-1; i++){
            int[] p1 = path.get(i);
            int[] p2 = path.get(i+1);
            proc.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }


        new ImagePlus("image", proc).show();

    }

}
