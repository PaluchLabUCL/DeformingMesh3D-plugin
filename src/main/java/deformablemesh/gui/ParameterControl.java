package deformablemesh.gui;

import javax.swing.*;

/**
 * Created on 31/07/18.
 */
public class ParameterControl extends JPanel {
    String name;
    JTextField field;

    public void prepareValue(JLabel label, JTextField field){
        this.name = label.getText();
        this.field = field;

        BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
        setLayout(layout);
        add(label);
        add(Box.createHorizontalGlue());
        add(field);
    }

    public void updateValue(double value){
        field.setText(GuiTools.displayFormat(value));
    }

}
