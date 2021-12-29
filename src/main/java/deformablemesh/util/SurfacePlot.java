package deformablemesh.util;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.gui.GuiTools;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.PlotSurface;
import ij.io.OpenDialog;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.Color;
import java.awt.FileDialog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * For plotting values on the surface of a mesh.
 */
abstract public class SurfacePlot{
    Color high = new Color(255, 255, 0);
    Color low = new Color(0, 0, 255);
    int range = 10;


    double min = Double.MAX_VALUE;
    double max = -min;

    DeformableMesh3D mesh;
    //MeshImageStack stack;


    double delta;
    MeshFrame3D viewer;
    float[] colors;
    double[] values;
    public void setHighColor(Color high) {
        this.high = high;
    }
    public Color getHighColor(){
        return high;
    }

    public Color getLowColor() {
        return low;
    }

    public void setLowColor(Color low) {
        this.low = low;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min){
        this.min = min;
    }

    public void setMax(double max){
        this.max = max;
    }

    public double getMax(){
        return max;
    }



    /**
     * Calculates all of the local values at each node. Sets the min and max values. These can be changed before showing
     * the plots.
     */
    public void process(){
        values = new double[mesh.nodes.size()];


        for(int i = 0;  i<mesh.nodes.size(); i++){
            Node3D node = mesh.nodes.get(i);
            double v = sample(node);
            max = v>max?v:max;
            min = v<min?v:min;
            values[i] = v;
        }


    }


    public void showValuesWindow(){
        StringBuilder builder = new StringBuilder("#n\tx\ty\tz\ti\tc\n");
        float[] comps = new float[4];
        for(int i = 0;  i<mesh.nodes.size(); i++){
            double[] pt = mesh.nodes.get(i).getCoordinates();
            double v = values[i];
            System.arraycopy(colors, 3*i, comps, 0, 3);
            int c = new Color(comps[0], comps[1], comps[2]).getRGB();
            builder.append(String.format(Locale.US, "%d\t%f\t%f\t%f\t%f\t%d\n", i, pt[0], pt[1], pt[2], v, c ));

        }

        GuiTools.createTextOuputPane(builder.toString());
    }

    public void saveAsPLyFile(Path outputPath) throws IOException{

        try(BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)){
            writer.write("ply\n");
            writer.write("format ascii 1.0\n");
            writer.write("comment meshes created by deformable mesh plugin.\n");
            int verts = mesh.positions.length/3;
            int faces = mesh.triangles.size();
            writer.write(String.format("element vertex %d\n", verts));
            writer.write("property float x\n");
            writer.write("property float y\n");
            writer.write("property float z\n");
            writer.write("property uchar red\n");
            writer.write("property uchar green\n");
            writer.write("property uchar blue\n");
            writer.write(String.format("element face %d\n", faces));
            writer.write("property list uchar int vertex_index\n");
            writer.write("end_header\n");
            //write out the vertexes.

            for(int j = 0; j<mesh.positions.length/3; j++){
                int dex = j*3;

                Color c = new Color(colors[dex], colors[dex+1], colors[dex+2]);

                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();

                writer.write(String.format(Locale.US, "%f %f %f %d %d %d\n",
                    (mesh.positions[dex]),
                    (mesh.positions[dex+1]),
                    (mesh.positions[dex+2]),
                        r,g,b
                    )
                );
                }
            for(Triangle3D triangle: mesh.triangles){
                int[] indices = triangle.getIndices();
                writer.write(String.format(Locale.US, "%d %d %d %d\n",3, indices[0], indices[1], indices[2]));
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    public void show(){
        show(false);
    }



    public void show(boolean exitOnClose){
        viewer = new MeshFrame3D();

        viewer.showFrame(exitOnClose);
        viewer.addLights();
        viewer.setBackgroundColor(Color.BLACK);

        JFrame frame = viewer.getJFrame();
        JMenuBar bar = new JMenuBar();
        JMenu data = new JMenu("data");


        JMenuItem showValues = new JMenuItem("show values");
        showValues.addActionListener(e->{
            showValuesWindow();
        });
        data.add(showValues);

        JMenuItem saveAsPly = new JMenuItem("save as ply");
        saveAsPly.addActionListener(e ->{
            FileDialog fd = new FileDialog(frame,"File to save ply file");
            fd.setDirectory(OpenDialog.getDefaultDirectory());
            fd.setMode(FileDialog.SAVE);
            fd.setVisible(true);
            if(fd.getFile()==null || fd.getDirectory()==null){
                return;
            }
            Path f = new File(fd.getDirectory(),fd.getFile()).toPath();
            try {
                saveAsPLyFile(f);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        });

        data.add(saveAsPly);

        bar.add(data);
        frame.setJMenuBar(bar);

        colors = new float[mesh.positions.length];
        HotAndCold ci = new HotAndCold(high, low);
        ci.setMinMax(min, max);
        for(int i = 0; i<mesh.nodes.size(); i++){
            float[] f = ci.getColor(values[i]);
            System.arraycopy(f, 0, colors, 3*i, 3);
        }

        PlotSurface surface = new PlotSurface(mesh.positions, mesh.triangle_index, colors);
        viewer.addDataObject(surface);
    }

    public MeshFrame3D processAndShow(boolean exitOnClose){

        process();
        show(exitOnClose);

        return viewer;

    }

    public MeshFrame3D processAndShow(){
        return processAndShow(false);
    }

    abstract public double sample(Node3D node);


}
