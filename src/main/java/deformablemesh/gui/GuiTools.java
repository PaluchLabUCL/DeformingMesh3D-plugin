package deformablemesh.gui;

import deformablemesh.Deforming3DMesh_Plugin;
import deformablemesh.SegmentationController;
import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.stream.IntStream;

import static deformablemesh.gui.ControlFrame.instance;

/**
 * User: msmith
 * Date: 8/6/13
 * Time: 9:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class GuiTools {
    final static String versionHTML = getVersionHTML();
    public static void createTextOuputPane(String s){
        final JFrame frame = new JFrame();
        final JTextComponent pane = new JTextArea();
        pane.setText(s);
        frame.setContentPane(new JScrollPane(pane));

        JMenuBar menu_bar = new JMenuBar();
        frame.setJMenuBar(menu_bar);

        JMenu file = new JMenu("file");
        menu_bar.add(file);

        JMenuItem save = new JMenuItem("save");
        file.add(save); 

        save.addActionListener(new ActionListener(){


            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FileDialog fd = new FileDialog(frame,"File to save too");
                fd.setMode(FileDialog.SAVE);
                fd.setVisible(true);
                if(fd.getFile()==null) return;
                File f = new File(fd.getDirectory(),fd.getFile());

                try {
                    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), Charset.forName("utf-8")));
                    br.write(pane.getText());
                    br.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
        //frame.pack();
        frame.setSize(600,800);
        frame.setVisible(true);

    }

    public static ParameterCombo createComboControl(String name, final SetValue action, int initial){
        JLabel label = new JLabel(name);

        Integer[] values = {0, 1, 2, 3, 4, 5};
        int dex = -1;
        for(int i = 0; i< values.length; i++){
            if(values[i] == initial){
                dex = i;
            }
        }
        final JComboBox<Integer> box = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5});

        box.setSelectedIndex(dex);

        box.addActionListener(evt->{
            action.setValue(box.getItemAt(box.getSelectedIndex()));
        });


        ParameterCombo control = new ParameterCombo();
        control.prepareValue(label, box);
        return control;
    }
    public static ParameterControl createInputField(String name, final SetValue action, double initial, ReadyObserver observer){

        JLabel label = new JLabel(name);


        final JTextField field = new JTextField(10);
        field.setText(displayFormat(initial));
        //field.setMinimumSize(new Dimension(100, 20));
        //field.setPreferredSize(new Dimension(100, 20));
        //field.setMaximumSize(new Dimension(200, 20));
        field.setEnabled(false);
        field.setHorizontalAlignment(JTextField.RIGHT);

        field.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent evt){
                if(observer.isReady() && !field.isEnabled()){
                    field.setEnabled(true);
                    observer.setReady(false);
                }
            }
        });

        field.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    try {
                        action.setValue(parseDouble(field.getText()));
                        observer.setReady(true);
                        field.setEnabled(false);

                    } catch (ParseException e) {
                        e.printStackTrace();


                    }
                } catch(NumberFormatException exc){
                    //oh well
                }
            }
        });

        field.addFocusListener(new FocusListener(){

            @Override
            public void focusGained(FocusEvent focusEvent) {

            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if(!field.isEnabled()){
                    return;             //how?
                }
                try{

                    action.setValue(parseDouble(field.getText()));
                    observer.setReady(true);
                    field.setEnabled(false);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        ParameterControl control = new ParameterControl();
        control.prepareValue(label, field);
        return control;
    }

    public static void errorMessage(String s) {
        JOptionPane.showMessageDialog(instance, s);
    }

    public static void showAboutWindow(JFrame owner){
        JDialog log = new JDialog(owner, "about DM3D");
        log.setModal(false);
        JEditorPane svg = new JEditorPane("text/html", versionHTML);
        svg.setEditable(false);
        svg.addHyperlinkListener(hyperlinkEvent ->{
            try {
                if(hyperlinkEvent.getEventType()== HyperlinkEvent.EventType.ACTIVATED){
                    Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        });
        log.setContentPane(svg);
        log.pack();
        log.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        log.setVisible(true);
    }

    private static String getVersionHTML(){

        try {
            String versionTag = "%%VERSION%%";
            BufferedReader r = new BufferedReader(new InputStreamReader(Thread.currentThread().getClass().getResourceAsStream("/about.html"), Charset.forName("UTF8")));
            StringBuilder b = new StringBuilder();
            String s;

            while((s=r.readLine())!=null){
                b.append(s);
            }

            int i = b.indexOf(versionTag);
            b.replace(i, i + versionTag.length(), Deforming3DMesh_Plugin.version);

            return b.toString();

        } catch (Exception e) {
            return String.format("<html><body style=\"background-color: black; color: green;\">Version %s</bod></html>", Deforming3DMesh_Plugin.version);
        }


    }

    private static String getFaqHTML(){
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(Thread.currentThread().getClass().getResourceAsStream("/help.html"), Charset.forName("UTF8")));
            StringBuilder b = new StringBuilder();
            String s;

            while((s=r.readLine())!=null){
                b.append(s);
            }

            return b.toString();

        } catch (Exception e) {
            return "<html><body style=\"background-color: black; color: green;\">FAQ not available!</bod></html>";
        }
    }

    public static String displayFormat(double value){
        return Double.toString(value);
    }

    public static double parseDouble(String value) throws ParseException{
        return Double.parseDouble(value);
    }

    static public JPanel getClosableTabComponent(String title, ActionListener closeListener){
        JPanel closableTab = new JPanel();
        closableTab.setOpaque(false);
        JLabel label = new JLabel(title);
        int f = label.getFont().getSize();
        label.setOpaque(false);
        JButton button = GuiTools.getXButton(f);
        button.addActionListener(closeListener);
        closableTab.add(label);
        closableTab.add(button);
        return closableTab;
    }
    public static JButton getXButton(int fontSize){
        int size = fontSize;

        JButton b = new JButton();
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(null);
        b.setIcon( getXIcon( size, Color.BLACK ) );
        b.setRolloverIcon( getXIcon(size, Color.GREEN.darker()) );
        b.setPressedIcon( getXIcon(size, Color.RED));
        b.setPreferredSize(new Dimension(size, size));
        return b;
    }

    public static void highQualityRenderingHints(Graphics2D g2d){
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static Icon getXIcon(int size, Color c) {
        int L = size*2/3;
        int b = (size - L)/2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = img.createGraphics();
        applyRenderingHints(g2d);

        g2d.setColor(c);
        g2d.drawLine(b, b, L + b, L+b);
        g2d.drawLine(b, L+b, L+b, b);
        g2d.dispose();
        return new ImageIcon(img);
    }

    /**
     * For making a g2d that draws nicely.
     * @param g2d
     */
    public static void applyRenderingHints(Graphics2D g2d){
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static void showFaqWindow(JFrame frame) {
        JDialog log = new JDialog(frame, "DM3D: FAQ");
        log.setModal(false);
        JEditorPane svg = new JEditorPane("text/html", getFaqHTML());
        svg.setEditable(false);
        svg.addHyperlinkListener(hyperlinkEvent ->{
            try {
                if(hyperlinkEvent.getEventType()== HyperlinkEvent.EventType.ACTIVATED){
                    Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        });
        log.setContentPane(svg);
        log.setSize(frame.getWidth(), frame.getHeight());
        log.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        log.setVisible(true);
    }

    public static class LocaleNumericTextField{
        final JTextField field;

        public LocaleNumericTextField(JTextField ret, double initial) {
            this.field = ret;
            setValue(initial);
        }

        public void setValue(double value){
            field.setText(displayFormat(value));
        }
        public JTextField getTextField(){
            return field;
        }
        public double getValue(){
            try{
                return parseDouble(field.getText());
            } catch (ParseException e) {
                return 0;
            }
        }


    }

    static public ImagePlus selectOpenImage(JFrame parent){
        String[] imageLabels = WindowManager.getImageTitles();

        if(imageLabels.length==0) return null;

        Object[] choices = new Object[imageLabels.length];
        for(int i = 0; i<choices.length; i++){
            choices[i] = imageLabels[i];
        }

        Object option = JOptionPane.showInputDialog(
                parent,
                "Choose from open images:",
                "Choose Open Image",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if(option instanceof String) {
            ImagePlus plus = WindowManager.getImage((String) option);
            return plus;
        }
        return null;
    }

    private static Image icon;

    public static Image getIcon(){
        if(icon==null){
            icon = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);

            Graphics2D g = (Graphics2D)icon.getGraphics();

            g.setPaint(Color.BLACK);
            g.fillRoundRect(3, 3, 58, 58, 16, 16);

            g.setColor(Color.LIGHT_GRAY);
            g.setStroke(new BasicStroke(3));
            g.drawLine(32, 3, 32, 32);
            g.drawLine(32, 32, 3, 61);
            g.drawLine(32, 32, 58, 32);

            g.setColor(Color.WHITE);
            g.fillOval(26, 24, 22, 22);




        }

        return icon;
    }
    static public void selectOpenImage(Window parent, SegmentationController segmentationController){

        String[] imageLabels = WindowManager.getImageTitles();

        if(imageLabels.length==0) return;

        Object[] choices = new Object[imageLabels.length];
        for(int i = 0; i<choices.length; i++){
            choices[i] = imageLabels[i];
        }

        Object option = JOptionPane.showInputDialog(
                parent,
                "Choose from open images:",
                "Choose Open Image",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if(option instanceof String) {
            ImagePlus plus = WindowManager.getImage((String) option);
            if (plus != null) {
                int channel = 0;
                if(plus.getNChannels()>1){
                    Object[] values = IntStream.range(1, plus.getNChannels()+1).boxed().toArray();
                    Object channelChoice = JOptionPane.showInputDialog(
                            parent,
                            "Select Channel:",
                            "Choose Channel",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            values,
                            values[0]
                    );
                    if(channelChoice == null) return;
                    channel = (Integer)channelChoice - 1;
                }

                segmentationController.setOriginalPlus(plus, channel);
            }
        }
    }
}
