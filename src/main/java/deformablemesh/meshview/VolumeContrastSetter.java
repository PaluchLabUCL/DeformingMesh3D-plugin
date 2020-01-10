package deformablemesh.meshview;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.Box3D;
import deformablemesh.gui.IntensityRanges;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VolumeContrastSetter{
    VolumeSamplerPanel preview;
    IntensityRanges range;
    VolumeDataObject vdo;
    JDialog dialog;
    Color previewBackgroundColor = Color.BLACK;
    Color volumeColor = Color.WHITE;

    public VolumeContrastSetter(VolumeDataObject vdo){
        this.vdo = vdo;
    }

    public void setPreviewBackgroundColor(Color c){
        previewBackgroundColor = c;
        if(preview != null){
            preview.mf3d.setBackgroundColor(previewBackgroundColor);
        }
    }

    public void showDialog(Frame parent){
        dialog = new JDialog(parent, "adjust volume contrast");
        dialog.setModal(true);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.BLACK);
        content.setOpaque(true);
        range = new IntensityRanges(vdo.texture_data);
        JPanel flow = new JPanel();
        flow.setOpaque(false);
        flow.add(range.getPanel());

        content.add(flow, BorderLayout.NORTH);
        Component comp = create3DPreviewer(dialog);

        content.add(comp, BorderLayout.CENTER);
        content.add(createButtons(), BorderLayout.SOUTH);

        range.addContrastableListener(preview::setMinMaxClipping);
        range.setClipValues(vdo.min, vdo.max);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setVisible(true);
    }

    public JPanel createButtons(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        JButton accept = new JButton("accept");
        accept.addActionListener(evt->{
            double[] clips = range.getClipValues();
            System.out.println("setting: " + clips[0] + ", " + clips[1]);
            vdo.setMinMaxRange(clips[0], clips[1]);
            dialog.dispose();
        });
        JButton cancel = new JButton("cancel");
        cancel.addActionListener(evt->{
            dialog.dispose();
        });

        JCheckBox oscillateView = new JCheckBox("oscillate preview");
        oscillateView.setBackground(Color.BLACK);
        oscillateView.setForeground(Color.WHITE);
        oscillateView.setSelected(true);

        oscillateView.addActionListener(evt->{
            if(preview != null){
                if(oscillateView.isSelected()){
                    preview.oscillate();
                } else{
                    preview.stopOscillation();
                }
            }
        });

        panel.add(oscillateView);
        panel.add(Box.createHorizontalGlue());
        panel.add(accept);
        panel.add(cancel);
        panel.setOpaque(false);
        return panel;
    }

    public Component create3DPreviewer(Window frame) {
        System.out.println("to here!");
        preview = new VolumeSamplerPanel(frame);
        preview.showSubSample(vdo);
        return preview.panel;
    }

    class VolumeSamplerPanel{
        MeshFrame3D mf3d;
        Component panel;
        VolumeDataObject previewVdo;
        int preview_time = 0;
        ScheduledFuture<?> future;
        ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(1);
        public VolumeSamplerPanel(Window parent){
            mf3d = new MeshFrame3D();

            panel = mf3d.asJPanel(parent);
            mf3d.setBackgroundColor(previewBackgroundColor);
            mf3d.showAxis();

            WindowListener l = new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent evt){
                    ses.shutdown();
                }

                @Override
                public void windowOpened(WindowEvent e) {
                    oscillate();
                }
            };
            parent.addWindowListener(l);

        }



        public void oscillate(){
            if(future != null){
                future.cancel(false);
                future = null;
            }
            future = ses.scheduleAtFixedRate(()->{
                double t = Math.sin(preview_time*3.14/100);
                mf3d.rotateView((int)(10*t), 0);
                preview_time++;
            }, 0, 20, TimeUnit.MILLISECONDS);
        }

        public void stopOscillation(){
            future.cancel(false);
            future = null;
        }
        int[] getShape(double[][][] arr){

            return new int[]{arr.length, arr[0].length, arr[0][0].length};

        }
        void showSubSample(VolumeDataObject full){
            previewVdo = new VolumeDataObject(volumeColor);
            int[] whd = {64, 64, 64};
            int[] shape = getShape(full.texture_data);


            int[] low = new int[3];
            int[] high = new int[3];

            for(int i = 0; i<3; i++){

                if(whd[i]>=shape[i]){
                    whd[i] = shape[i]/2;
                }

                int remain = shape[i] - whd[i];
                low[i] = remain/2;
                high[i] = low[i] + whd[i];

            }
            previewVdo.setColor(full.color);
            previewVdo.setTextureData(full, low, high);
            previewVdo.setMinMaxRange(full.min, full.max);
            mf3d.addDataObject(previewVdo);
        }


        void showSubSample(MeshImageStack stack){
            vdo = new VolumeDataObject(volumeColor);
            double[] l = stack.scaleToNormalizedLength(new double[]{64, 0, 0});

            vdo.setTextureData(

                    stack.createSubStack(
                            new Box3D(new double[]{0, 0, 0}, l[0], l[0], l[0] )
                    )

            );

            mf3d.addDataObject(vdo);
        }

        void setMinMaxClipping(double min, double max){
            previewVdo.setMinMaxRange(min, max);
        }

    }

}
