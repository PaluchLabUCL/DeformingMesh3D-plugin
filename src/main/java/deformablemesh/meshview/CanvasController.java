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
package deformablemesh.meshview;

import java.awt.event.*;

/**
 * Mouse listener class for handling the basic interactions with the DataCanvas interactions.
 *
 * */
public class CanvasController extends MouseAdapter {
    DataCanvas dc;
    int start_dragx, start_dragy;
    int click_type;
    boolean disabled=false;

    CanvasController(DataCanvas c){
        dc = c;
        dc.addMouseMotionListener(this);
        dc.addMouseListener(this);
        dc.addMouseWheelListener(this);
        this.disabled= false;

        dc.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped(KeyEvent e) {

                if(e.getKeyChar()=='1'){
                    dc.setView(StationaryViews.XY);
                } else if(e.getKeyChar()=='2'){
                    dc.setView(StationaryViews.XZ);
                } else if(e.getKeyChar()=='3'){
                    dc.setView(StationaryViews.YZ);
                } else if(e.getKeyChar()=='4'){
                    dc.setView(StationaryViews.THREEQUARTER);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
    }
    public void mouseMoved(MouseEvent e){
        dc.moved(e);
    }

    public void mouseReleased(MouseEvent evt){
        dc.released(evt);
    }
    public void mousePressed(MouseEvent e){
        click_type = e.getButton();
        start_dragx = e.getX();
        start_dragy = e.getY();

        dc.pressed(e);

    }

    public void mouseClicked(MouseEvent e){
        dc.clicked(e);
    }

    /**
     * For dragging, when disabled the state of the controller is updated, but the state of the canvas is not modified.
     *
     * @param e
     */
    public void mouseDragged(MouseEvent e){
        int dx = e.getX() - start_dragx;
        start_dragx = e.getX();
        int dy = e.getY() - start_dragy;
        start_dragy = e.getY();

        if(disabled){
            dc.dragged(e);
            return;
        }
        if(click_type==MouseEvent.BUTTON1)
            dc.rotateView(dx,dy);
        else
            dc.translateView(dx,dy);
    }
    public void mouseWheelMoved(MouseWheelEvent e){
        if(disabled){
            //TODO delegate to Canvas for propagation.
            return;
        }

        if(e.getWheelRotation()<0){
            dc.zoomIn();
        } else if(e.getWheelRotation()>0){
            dc.zoomOut();
        }
    }

    /**
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled){
        this.disabled = !enabled;
    }


}
