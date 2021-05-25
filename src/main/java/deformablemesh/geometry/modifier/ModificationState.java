package deformablemesh.geometry.modifier;

import deformablemesh.meshview.CanvasView;

public interface ModificationState extends CanvasView {

    public void register();
    public void deregister();
    default public String getName(){ return this.getClass().getName();}
}
