package deformablemesh.geometry;

import deformablemesh.MeshImageStack;
import deformablemesh.externalenergies.CurveBasedEnergy;
import deformablemesh.gui.FrameListener;
import deformablemesh.meshview.LineDataObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * For keeping track of the curve data.
 *
 * Created by msmith on 10/20/15.
 */
public class SnakeBox implements FrameListener {
    public int frame;
    TreeMap<Integer, List<List<double[]>>> curves = new TreeMap<>();

    int height, width, depth;
    double principle = 1;
    MeshImageStack stack;
    double weight = 0;
    List<FrameListener> listeners = new ArrayList<>();
    public SnakeBox(){
        frame = -1;
    }


    public void setScale(MeshImageStack stack){
        this.stack = stack;
        frameChanged(stack.CURRENT);
    }
    @Override
    public void frameChanged(int i) {
        frame=i;
        listeners.forEach(l->l.frameChanged(i));
    }


    public void setCurveWeight(double v){
        weight=v;
    }

    public double getCurveWeight(){
        return weight;
    }

    public void addRingEnergy(int frame, DeformableMesh3D mesh){

        if(weight==0||!curves.containsKey(frame)) return;

        for(List<double[]> pts: curves.get(frame)){
            ContractileRing ring = new ContractileRing(pts);
            List<Node3D> connected = ring.createMappings(mesh);
            if(connected.size()==0){
                continue;
            }
            LineDataObject obj = new LineDataObject(connected);
            obj.setColor(0,0,1);

            mesh.addExternalEnergy(new CurveBasedEnergy(ring, weight));
        }

    }


    public void addCurve(List<double[]> pts) {
        if(pts.size()==0){
            return;
        }else if(pts.get(0).length!=3){
            throw new RuntimeException("not tranformed points");
        }
        List<List<double[]>> c = curves.get(frame);
        if(c==null) {
            c = new ArrayList<>();
            curves.put(frame,c);
        }

        c.add(pts);
        frameChanged(frame);
    }

    public void removeCurve(List<double[]> points) {
        List<List<double[]>> c = curves.get(frame);
        c.remove(points);
        frameChanged(frame);
    }

    public List<List<double[]>> getCurves(){
        return getCurves(frame);
    }
    public List<List<double[]>> getCurves(int frame){
        if(curves.containsKey(frame)){
            return curves.get(frame);
        }
        else{
            List<List<double[]>> list = new ArrayList<>();
            curves.put(frame, list);
            return list;
        }
    }

    public void addFrameListener(FrameListener l) {
        listeners.add(l);
    }
}
