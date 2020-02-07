package deformablemesh.gui;

import deformablemesh.Deforming3DMesh_Plugin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;

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
    public static ParameterControl createInputField(String name, final SetValue action, double initial, ReadyObserver observer){

        JLabel label = new JLabel(name);


        final JTextField field = new JTextField();
        field.setText(displayFormat(initial));
        field.setMinimumSize(new Dimension(100, 20));
        field.setPreferredSize(new Dimension(100, 20));
        field.setMaximumSize(new Dimension(200, 20));
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
        JDialog log = new JDialog(owner, "about Mesh3D");
        log.setModal(false);
        JEditorPane svg = new JEditorPane("text/html", versionHTML);
        svg.setEditable(false);
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

    public static String displayFormat(double value){
        return Double.toString(value);
    }

    public static double parseDouble(String value) throws ParseException{
        return Double.parseDouble(value);
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
}
