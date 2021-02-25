package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.gui.FrameListener;
import java.awt.Color;

/**
 * For adding a second channel to display.
 */
public class ChannelVolume implements FrameListener {
    VolumeDataObject vdo;
    MeshImageStack stack;
    String name;
    public ChannelVolume(MeshImageStack stack, Color c){
        vdo = new VolumeDataObject(c);

        this.stack = stack;
        vdo.setTextureData(stack);
        name = "ChannelVolume(" + stack + ", " + c + ")";
    }

    @Override
    public void frameChanged(int i) {
        stack.setFrame(i);
        vdo.setTextureData(stack);
    }
    @Override
    public String toString(){
        return name;
    }
}
