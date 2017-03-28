package deformablemesh.track;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Furrow3D;
import deformablemesh.geometry.ProjectableMesh;
import deformablemesh.io.MeshWriter;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.ColorSuggestions;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A gui/class for organizing a sorting the linking of meshes.
 *
 * Created on 22/03/2017.
 */
public class MeshTrackManager {
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


    public MeshTrackManager(){
        model = new MeshListModel();

    }
    public void buildJFrameGui(){
        JFrame frame;
        frame = new JFrame();
        JPanel content = new JPanel();
        buildGui(frame, content);
        frame.setContentPane(content);
        frame.setSize(new Dimension(600, 480));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void buildGui(Component parent, JPanel content){
        trackTable = new JTable();
        trackTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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
        content.add(
                new JScrollPane(
                        trackTable,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                ),
                BorderLayout.CENTER
        );


        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 3));
        JButton shiftTrack = new JButton("to existing track");

        shiftTrack.addActionListener(evt->{
            String[] destinations = getPossibleDestinations();
            if(destinations.length==0){
                return;
            }
            String example = (String)JOptionPane.showInputDialog(
                    parent,
                    "Select Destination Track.",
                    "Tracks",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    destinations,
                    destinations[0]);
            if(example==null) {
                //cancelled?
                return;
            }
            moveToTrack(example);
        });

        buttons.add(shiftTrack);

        JButton toNewTrack = new JButton("to new track");
        toNewTrack.addActionListener(evt->toNewTrack());
        buttons.add(toNewTrack);

        JButton shiftFrame = new JButton("shift frame");
        shiftFrame.addActionListener(evt->shiftFrame());
        buttons.add(shiftFrame);

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

    private void shiftFrame() {
        int c = trackTable.getSelectedColumn();

        if(c>0){
            int[] rows = trackTable.getSelectedRows();
            if(rows.length==0){
                return;
            }

            Track t = tracks.get(c-1);
            int count = rows.length;
            for(int i = 0; i<rows.length; i++){
                if(!t.containsKey(rows[i])){
                    rows[i] = -1;
                    count--;
                }
            }
            //no good meshes.
            if(count==0){
                return;
            }

            if(count<rows.length) {
                //remove some.
                int[] rows2 = new int[count];
                int j = 0;
                for (int i = 0; i < rows.length; i++) {
                    if(rows[i]>=0){
                        rows2[j] = rows[i];
                        j++;
                    }
                }
                rows = rows2;
            }
            String shift = (String)JOptionPane.showInputDialog("Enter Number of Frames to Shift: ", "0");
            if(shift==null) return;
            int offset = 0;
            try{
                offset = Integer.parseInt(shift);

            } catch(Exception e){
                return;
            }


            for(int i=0; i<rows.length; i++){
                int going = rows[i] + offset;
                boolean safe = false;
                for(int j = 0; j<rows.length; j++){
                    if(going==rows[j]){
                        //good stop checking.
                        safe = true;
                        break;
                    }
                }
                if(!safe){
                    if(t.containsKey(going)){
                        //occupied destination.
                        return;
                    }
                }
            }

            List<DeformableMesh3D> moving = new ArrayList<>(rows.length);
            for(int i=0; i<rows.length; i++){
                moving.add(t.getMesh(rows[i]));
                //remove the mesh from the old location.
                t.remove(moving.get(i));
                //store the destination position.
                rows[i] = rows[i] + offset;
            }

            for(int i=0; i<rows.length; i++){
                t.addMesh(rows[i], moving.get(i));
            }

            shapeTable();
        }
    }

    private void toNewTrack(){
        int c = trackTable.getSelectedColumn();

        if(c>0){
            int[] rows = trackTable.getSelectedRows();
            if(rows.length==0){
                return;
            }

            Track t = tracks.get(c-1);
            int count = rows.length;
            for(int i = 0; i<rows.length; i++){
                if(!t.containsKey(rows[i])){
                    rows[i] = -1;
                    count--;
                }
            }
            //no good meshes.
            if(count==0){
                return;
            }

            if(count<rows.length) {
                //remove some.
                int[] rows2 = new int[count];
                int j = 0;
                for (int i = 0; i < rows.length; i++) {
                    if(rows[i]>=0){
                        rows2[j] = rows[i];
                        j++;
                    }
                }
                rows = rows2;
            }

            List<Color> colors = tracks.stream().map(tr->tr.color).collect(Collectors.toList());
            Color color = ColorSuggestions.getSuggestion(colors);
            Track n = new Track(ColorSuggestions.getColorName(color), color);

            for(int i: rows){
                DeformableMesh3D mesh = t.getMesh(i);
                t.remove(mesh);
                n.addMesh(i, mesh);
            }

            tracks.add(n);
            shapeTable();
        }
    }

    private void moveToTrack(String example) {
        Track destination = null;
        for(Track t: tracks){
            if(t.name.equals(example)){
                destination=t;
                break;
            }
        }

        if(destination==null){
            throw new RuntimeException("Destination not found!");
        }
        int c = trackTable.getSelectedColumn();

        if(c>0) {
            int[] rows = trackTable.getSelectedRows();
            if(rows.length==0){
                throw new RuntimeException("No meshes selected!");
            }
            Track t = tracks.get(c-1);
            int count = rows.length;
            for(int i = 0; i<rows.length; i++){
                if(!t.containsKey(rows[i])){
                    rows[i] = -1;
                    count--;
                }
            }
            //no good meshes.
            if(count==0){
                throw new RuntimeException("None of the selected rows have meshes!");
            }

            if(count<rows.length) {
                //remove some.
                int[] rows2 = new int[count];
                int j = 0;
                for (int i = 0; i < rows.length; i++) {
                    if(rows[i]>=0){
                        rows2[j] = rows[i];
                        j++;
                    }
                }
                rows = rows2;
            }
            Track one = tracks.get(c-1);
            for(Integer index: rows){
                DeformableMesh3D mesh = one.getMesh(index);
                one.remove(mesh);
                destination.addMesh(index, mesh);
            }


        } else{
            throw new RuntimeException("Valid column not selected!");
        }

        //why on earth?

    }

    private String[] getPossibleDestinations() {
        int c = trackTable.getSelectedColumn();

        if(c>0){
            int[] rows = trackTable.getSelectedRows();
            if(rows.length==0){
                return new String[0];
            }
            Track t = tracks.get(c-1);
            int count = rows.length;
            for(int i = 0; i<rows.length; i++){
                if(!t.containsKey(rows[i])){
                    rows[i] = -1;
                    count--;
                }
            }
            //no good meshes.
            if(count==0){
                return new String[0];
            }

            if(count<rows.length) {
                //remove some.
                int[] rows2 = new int[count];
                int j = 0;
                for (int i = 0; i < rows.length; i++) {
                    if(rows[i]>=0){
                        rows2[j] = rows[i];
                        j++;
                    }
                }
                rows = rows2;
            }

            List<String> acceptable = new ArrayList<>(tracks.size());

            for(Track otra: tracks){
                if(otra==t) continue;
                boolean available = true;
                for(int i = 0; i<rows.length; i++){
                    if(otra.containsKey(rows[i])){
                        available = false;
                        break;
                    }
                }
                if(!available){
                    continue;
                }
                acceptable.add(otra.getName());
            }

            return acceptable.toArray(new String[acceptable.size()]);

        }


        return new String[0];
    }

    public List<Track> getTracks(){
        List<Track> verified = new ArrayList<>(tracks.size());
        for(Track track: tracks){
            if(track.isEmpty()){
                continue;
            }
            verified.add(track);
        }
        return verified;
    }

    public void manageMeshTrackes(List<Track> tracks){
        this.tracks.clear();
        labels.clear();

        int rows = 0;
        for(Track track: tracks){
            Track replacement = new Track(track.name, track.color);
            Set<Integer> ints = track.getTrack().keySet();
            for(Integer i: ints){

                DeformableMesh3D mesh = track.getMesh(i);
                labels.put(mesh, createLabel(mesh));
                if(i>rows){
                    rows = i;
                }
                replacement.addMesh(i, mesh);
            }
            this.tracks.add(replacement);
        }
        model.rows = rows;

        shapeTable();

    }

    public void shapeTable(){

        model.fireTableStructureChanged();
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

    }

    private JLabel createLabel(DeformableMesh3D mesh) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);
        double[] center = DeformableMesh3DTools.centerAndRadius(mesh.nodes);
        Furrow3D furrow = new Furrow3D(center, new double[]{0, 0, 1});
        FurrowTransformer transformer = new FurrowTransformer(furrow, new MeshImageStack());
        ProjectableMesh pmesh = new ProjectableMesh(mesh);
        Shape path = pmesh.continuousPaths(transformer);
        AffineTransform transform = AffineTransform.getScaleInstance(64, 64);
        transform.translate(0.5, 0.5);
        path = transform.createTransformedShape(path);

        Graphics2D g2d = (Graphics2D)img.getGraphics();
        g2d.setColor(Color.BLUE);
        g2d.drawLine(32, 34, 32, 0);
        g2d.drawLine(30, 32, 64, 32);
        g2d.setColor(new Color(200, 200, 200, 40));
        g2d.fillRect(0, 0, 64, 64);
        g2d.setColor(Color.BLACK);
        g2d.draw(path);
        g2d.setColor(mesh.getColor());
        g2d.fill(path);
        g2d.dispose();
        return new JLabel(new ImageIcon(img));
    }



    public static void main(String[] args) throws IOException, InvocationTargetException, InterruptedException {
        MeshTrackManager manager = new MeshTrackManager();
        List<Track> tracks = MeshWriter.loadMeshes(new File("sample.bmf"));
        manager.manageMeshTrackes(tracks);
        EventQueue.invokeAndWait(()->{
            manager.buildJFrameGui();
        });



    }
}
