package deformablemesh.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public class ParameterCombo extends ParameterControl{
    JComboBox<Integer> values;

    public void prepareValue(JLabel label, JComboBox<Integer> values){
        this.name = label.getText();
        this.values =values;
        BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
        setLayout(layout);
        add(label);
        add(Box.createHorizontalGlue());
        add(values);
    }

    public void updateValue(double value){
        double min = Double.MAX_VALUE;
        int s = -1;
        for(int i = 0; i<values.getItemCount(); i++){

            double delta = Math.abs(value - values.getItemAt(i));
            if(delta<min){
                min = delta;
                s = i;
            }
        }

        values.setSelectedIndex(s);
    }

}
