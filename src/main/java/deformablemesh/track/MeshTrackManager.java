package deformablemesh.track;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.io.MeshWriter;
import deformablemesh.ringdetection.FurrowTransformer;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A gui/class for organizing a sorting the linking of meshes.
 *
 * Created on 22/03/2017.
 */
public class MeshTrackManager {
    JFrame frame;
    JTable trackTable;
    List<Track> tracks = new ArrayList<>();
    MeshListModel model;
    MeshSelectionModel selectionModel = new MeshSelectionModel();
    Map<DeformableMesh3D, JLabel> labels = new HashMap<>();
    JLabel nullLabel = getNullLabel();

    private JLabel getNullLabel() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = (Graphics2D)img.getGraphics();
        g2d.setColor(Color.BLUE);
        g2d.setColor(new Color(200, 200, 200, 40));
        g2d.fillRect(0, 0, 64, 64);
        return new JLabel(new ImageIcon(img));
    }

    class MeshListModel extends AbstractTableModel {
        int rows;
        @Override public String getColumnName(int col){
            if(col>0)
                return tracks.get(col-1).getName();
            else
                return "frame";
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return tracks.size()+1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {

            return columnIndex>0?DeformableMesh3D.class:String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(columnIndex>0)
                return tracks.get(columnIndex-1).getMesh(rowIndex);
            else
                return Integer.toString(rowIndex+1);
        }
    }

    class MeshSelectionModel extends DefaultListSelectionModel {

    }

    class MeshCellRenderer implements TableCellRenderer{
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if(value instanceof DeformableMesh3D){
                JLabel label = labels.get(value);
                if(isSelected){
                    label.setOpaque(true);
                    label.setBackground(Color.BLACK);
                } else{
                    label.setOpaque(false);
                    label.setBackground(Color.WHITE);
                }
                return label;
            }

            return nullLabel;
        }
    }


    MeshTrackManager(){
        model = new MeshListModel();

    }


    public void buildGui(){
        frame = new JFrame();
        JPanel content = new JPanel();
        trackTable = new JTable();
        trackTable.setCellSelectionEnabled(true);
        trackTable.setSelectionModel(selectionModel);
        trackTable.setRowHeight(64);
        trackTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        DefaultListSelectionModel lsm =
                (DefaultListSelectionModel)trackTable.getColumnModel().getSelectionModel();
        lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        trackTable.setModel(model);
        trackTable.setDefaultRenderer(DeformableMesh3D.class, new MeshCellRenderer());

        content.setLayout(new BorderLayout());
        content.add(new JScrollPane(trackTable,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        frame.setContentPane(content);
        frame.setSize(new Dimension(680, 400));
        frame.setVisible(true);
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("file");
        bar.add(menu);

        frame.setJMenuBar(bar);

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 3));
        

        content.add(buttons, BorderLayout.SOUTH);


        TableColumnModel tcm = trackTable.getColumnModel();
        if(tcm!=null){
            int c = tcm.getColumnCount();
            for (int i = 0; i < c; i++) {

                TableColumn column = tcm.getColumn(i);
                if (i == 0) {
                    column.setPreferredWidth(40);
                    column.setMinWidth(40);
                    column.setMaxWidth(40);
                } else {
                    column.setPreferredWidth(64);
                    column.setMinWidth(64);
                    column.setMaxWidth(64);
                }
            }
        }
    }

    public void manageMeshTrackes(List<Track> tracks){
        int rows = 0;
        for(Track track: tracks){

            Set<Integer> ints = track.getTrack().keySet();
            for(Integer i: ints){
                DeformableMesh3D mesh = track.getMesh(i);
                labels.put(mesh, createLabel(mesh));
                if(i>rows){
                    rows = i;
                }
            }
        }
        model.rows = rows;
        this.tracks.addAll(tracks);

        if(trackTable!=null) {
            TableColumnModel tcm = trackTable.getColumnModel();
            if (tcm != null) {
                int c = tcm.getColumnCount();
                for (int i = 0; i < c; i++) {

                    TableColumn column = tcm.getColumn(i);
                    if (i == 0) {
                        column.setPreferredWidth(40);
                        column.setMinWidth(40);
                        column.setMaxWidth(40);
                    } else {
                        column.setPreferredWidth(64);
                        column.setMinWidth(64);
                        column.setMaxWidth(64);
                    }
                }
            }
        }
        model.fireTableStructureChanged();

    }

    private JLabel createLabel(DeformableMesh3D mesh) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);
        double[] center = DeformableMesh3DTools.centerAndRadius(mesh.nodes);
        Furrow3D furrow = new Furrow3D(center, new double[]{0, 0, 1});
        FurrowTransformer transformer = new FurrowTransformer(furrow, new MeshImageStack());
        ProjectableMesh pmesh = new ProjectableMesh(mesh);
        List<double[]> lines = pmesh.getSlicedTriangles(transformer);
        Path2D path = new Path2D.Double();

        for(int i = 0; i<lines.size()/2; i++){
            double[] a = lines.get(i*2);
            double[] b = lines.get(i*2 + 1);
            double x1 = (a[0] + 0.5)*64;
            double y1 = (a[1] + 0.5)*64;
            double x2 = (b[0] + 0.5)*64;
            double y2 = (b[1] + 0.5)*64;
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
        }

        Graphics2D g2d = (Graphics2D)img.getGraphics();
        g2d.setColor(Color.BLUE);
        g2d.drawLine(32, 34, 32, 0);
        g2d.drawLine(30, 32, 64, 32);
        g2d.setColor(new Color(200, 200, 200, 40));
        g2d.fillRect(0, 0, 64, 64);
        g2d.setColor(Color.RED);
        g2d.draw(path);
        g2d.dispose();
        return new JLabel(new ImageIcon(img));
    }



    public static void main(String[] args) throws IOException, InvocationTargetException, InterruptedException {
        MeshTrackManager manager = new MeshTrackManager();
        List<Track> tracks = MeshWriter.loadMeshes(new File("sample.bmf"));
        manager.manageMeshTrackes(tracks);
        EventQueue.invokeAndWait(()->{
            manager.buildGui();
            manager.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });



    }
}
