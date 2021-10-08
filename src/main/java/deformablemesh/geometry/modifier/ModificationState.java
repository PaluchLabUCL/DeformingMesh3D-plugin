package deformablemesh.geometry.modifier;

import deformablemesh.meshview.CanvasView;
import org.scijava.java3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;

public interface ModificationState {

    public void register();
    public void deregister();
    default public String getName(){ return this.getClass().getName();}
    public void updatePressed(double[] point, MouseEvent evt);
    public void updateReleased(double[] point, MouseEvent evt);
    public void updateClicked(double[] point, MouseEvent evt);
    public void updateMoved(double[] point, MouseEvent evt);
    public void updateDragged(double[] point, MouseEvent evt);

}
