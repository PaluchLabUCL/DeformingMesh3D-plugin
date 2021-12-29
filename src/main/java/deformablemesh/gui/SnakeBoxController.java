package deformablemesh.gui;


import deformablemesh.geometry.SnakeBox;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controls for adding and removing curves from the snakebox.
 * Created by msmith on 1/12/16.
 */
public class SnakeBoxController implements ListModel<CurveItem>, FrameListener{
    JList<CurveItem> list;
    JButton activate;
    JButton remove;

    List<CurveItem> contents = new ArrayList<>();
    List<ListDataListener> dataListeners = new ArrayList<>();
    TreeMap<Integer, List<CurveItem>> curves = new TreeMap<>();
    SnakeBox box;
    public SnakeBoxController(SnakeBox box){
        this.box = box;
        curves.put(box.frame, contents);
    }

    /**
     * Adds a new curve to the current frame based on the points and add the name to the list.
     *
     * @param baseName
     * @param points
     */
    public void addCurve(String baseName, List<double[]> points){

        boolean set = false;
        String goodName = baseName;
        int i = 0;
        while(!set) {
            set=true;
            for (CurveItem item : contents) {
                if(item.name.equals(goodName)){
                    goodName = baseName + "-" + i;
                    set=false;
                    i++;
                    break;
                }
            }
        }
        CurveItem item = new CurveItem(goodName, points);
        contents.add(item);
        dataChanged(contents.size()-1, contents.size());
        checkSelection();
    }



    /**
     * For keeping the display updated with the the current frame.
     * @param frame
     */
    public void setFrame(int frame){
        if(!curves.keySet().contains(frame)){
            curves.put(frame, new ArrayList<>());
        }
        contents = curves.get(frame);
        dataChanged(0, contents.size());

    }

    public List<List<double[]>> getCurves(int frame){
        if(!curves.keySet().contains(frame)){
            return Collections.emptyList();
        }
        return curves.get(frame).stream().map(c->c.points).collect(Collectors.toList());

    }
    public void removeCurve(int index){
        CurveItem item = contents.get(index);
        if (item.isActive()){
            box.removeCurve(item.points);
        }
        contents.remove(index);

        dataChanged(index, index);
        checkSelection();

    }

    private void dataChanged(int lower, int upper){
        for(ListDataListener l: dataListeners){
            l.contentsChanged(new ListDataEvent(list, ListDataEvent.CONTENTS_CHANGED, lower, upper));
        }
    }

    public JPanel getControls() {
        list = new JList<>();
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.add(new JScrollPane(list));
        list.setCellRenderer(new CurveItemRenderer());
        list.setModel(new ListModel<CurveItem>() {

            @Override
            public int getSize() {
                return contents.size();
            }

            @Override
            public CurveItem getElementAt(int index) {
                return contents.get(index);
            }

            @Override
            public void addListDataListener(ListDataListener l) {
                dataListeners.add(l);
            }

            @Override
            public void removeListDataListener(ListDataListener l) {
                dataListeners.remove(l);
            }
        });


        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        remove = new JButton("X");


        activate = new JButton("Activate");
        buttons.add(remove);
        buttons.add(activate);
        container.add(buttons, BorderLayout.SOUTH);
        list.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);


        remove.addActionListener((evt) -> {
            int first = list.getSelectedIndex();
            removeCurve(first);
        });

        activate.addActionListener(evt ->
        {
            CurveItem item = list.getSelectedValue();
            activateItem(item);
        });

        checkSelection();

        list.addListSelectionListener((evt) -> checkSelection());

        return container;

    }

    public TreeMap<Integer, List<List<double[]>>> getAllCurves(){
        TreeMap<Integer, List<List<double[]>>> ret = new TreeMap<>();

        for(Integer i: curves.keySet()){
            ret.put(i, curves.get(i).stream().filter(CurveItem::isActive).map(c->c.points).collect(Collectors.toList()));
        }
        return ret;
    }

    public void loadCurves(Map<Integer, List<List<double[]>>> values){
        boolean refresh = false;
        for(Integer i: values.keySet()){
            if(!curves.containsKey(i)){
                curves.put(i, new ArrayList<>());
            }
            List<CurveItem> items = curves.get(i);
            int count = 0;
            for(List<double[]> list: values.get(i)){
                CurveItem item = new CurveItem(String.format("%d-%d-loaded", i, count), list);
                count++;
                items.add(item);
                item.active = true;
            }
            if(items==contents){
                refresh=true;
            }
        }
        if(refresh){
            dataChanged(0, contents.size());
        }
    }
    public void activateItem(CurveItem item) {
        item.active = !item.active;
        if(item.active){
            box.addCurve(item.points);
        } else{
            box.removeCurve(item.points);
        }
        dataChanged(list.getSelectedIndex(), list.getSelectedIndex());
        checkSelection();
    }

    public void checkSelection(){
        int dex = list.getSelectedIndex();
        if (dex >= 0 && dex < contents.size()) {
            remove.setEnabled(true);
            activate.setEnabled(true);
            if (contents.get(dex).active) {
                activate.setText("deactivate");
            } else {
                activate.setText("activate");
            }
        } else {
            remove.setEnabled(false);
            activate.setEnabled(false);
        }
    }

    @Override
    public void frameChanged(int i) {
        setFrame(i);
    }

    public static void main(String[] args){
        SnakeBoxController sbc = new SnakeBoxController(new SnakeBox());

        JFrame frame = new JFrame();
        frame.add(sbc.getControls());
        JButton add = new JButton("add");
        frame.add(add, BorderLayout.SOUTH);
        add.addActionListener(evt->{
            sbc.addCurve("test", new ArrayList<>());
        });
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public CurveItem getElementAt(int index) {
        return null;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        dataListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        dataListeners.remove(l);
    }
}

class CurveItem{
    String name = "none";
    boolean active = false;
    boolean selected = false;
    List<double[]> points;

    /**
     * Creates a new
     * @param name
     * @param points
     */
    public CurveItem(String name, List<double[]> points){
        this.name = name;
        this.points = points;
    }
    @Override
    public String toString(){
        return name;
    }

    public boolean isActive() {
        return active;
    }
}

class CurveItemRenderer implements ListCellRenderer<CurveItem>{
    Color bg = Color.BLACK;
    Color selectBg = Color.BLUE;
    Color fg = Color.WHITE;
    String text;
    boolean selected;
    JPanel renders;
    boolean active;
    CurveItem current;
    boolean roundTop=false;
    boolean roundBottom=false;
    public CurveItemRenderer(){
        renders = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){

                if(current.selected){
                    g.setColor(selectBg);
                } else{
                    g.setColor(bg);
                }

                if(roundTop&&roundBottom){
                    g.fillRoundRect(0, 0, 250, 30, 10, 10);
                } else if(roundTop){
                    g.fillRoundRect(0, 0, 250, 40, 10, 10);
                }else if(roundBottom){
                    g.fillRoundRect(0, -10, 250, 40, 10, 10);
                }else {
                    g.fillRect(0, 0, 250, 30);
                }

                g.setColor(fg);
                g.drawOval(10, 8, 14, 14 );
                g.drawString(current.toString(), 40, 20);
                if(current.active){
                    g.setColor(Color.RED);
                } else{
                    g.setColor(Color.DARK_GRAY);
                }
                g.fillOval(11, 9, 12, 12);
                g.setColor(new Color(255, 255, 255, 200));
                g.fillOval(14, 12, 2, 2);

            }
        };

        renders.setPreferredSize(new Dimension(250, 30));
        renders.setMinimumSize(new Dimension(250, 30));
        renders.setMaximumSize(new Dimension(250, 30));

    }
    @Override
    public Component getListCellRendererComponent(JList<? extends CurveItem> list, CurveItem value, int index, boolean isSelected, boolean cellHasFocus) {
        value.selected = isSelected;
        current =value;
        roundTop=index==0;
        roundBottom=index==list.getModel().getSize()-1;
        return renders;
    }
}
class SnakeBoxView{

}
