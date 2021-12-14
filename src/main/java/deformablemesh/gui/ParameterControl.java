package deformablemesh.gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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
        setOpaque(false);
    }

    public double getValue(){
        return Double.parseDouble(field.getText());
    }
    public void updateValue(double value){
        field.setText(GuiTools.displayFormat(value));
    }

}
