package deformablemesh.geometry.modifier;

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
