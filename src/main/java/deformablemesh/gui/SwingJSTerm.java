package deformablemesh.gui;

import deformablemesh.SegmentationController;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by msmith on 4/14/14.
 */
public class SwingJSTerm {

    final ScriptEngine engine;
    JTextArea display, input;
    List<String> history = new LinkedList<String>();
    List<String> commandHistory = new ArrayList<>();
    JFrame frame;
    int commandIndex;
    SwingJSTerm(SegmentationController controls){

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine();

        Bindings bindings = engine.createBindings();

        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        engine.put("controls", controls);
        addClasses();
    }

    public void addClasses(){

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/load-bindings.js"), StandardCharsets.UTF_8
                )
        );
        try {
            engine.eval(reader);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    JPanel buildUI(){
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
        //root.setLayout(new BorderLayout());

        display = new JTextArea();
        display.setEditable(false);
        //display.setPreferredSize(new Dimension(600, 100));
        JScrollPane display_pane = new JScrollPane(display);
        display_pane.setPreferredSize(new Dimension(600, 100));
        root.add(display_pane);

        input = new JTextArea();
        input.setRows(10);
        JScrollPane house = new JScrollPane(input);

        String[] historyTemp = new String[1];




        JButton previous = new JButton("previous");
        JButton next = new JButton("next");

        previous.addActionListener((evt)->{
            commandIndex++;
            //index we will be at.
            int eIndex = commandHistory.size() - commandIndex;
            if(eIndex>=0 && eIndex<commandHistory.size()){
                //valid history.
                if(commandIndex==1){
                    //removing user entered data.
                    historyTemp[0] = input.getText();
                }
                input.setText(commandHistory.get(eIndex));
                next.setEnabled(true);
                if(commandIndex>=commandHistory.size()){
                    previous.setEnabled(false);
                }
            }


        });
        previous.setEnabled(false);

        next.addActionListener((evt)->{
            //requesting index.
            commandIndex--;
            int eIndex = commandHistory.size() - commandIndex;
            System.out.println("getting comand: " + eIndex);
            if(eIndex>=0 && eIndex<commandHistory.size()){
                //valid history.
                input.setText(commandHistory.get(eIndex));
                previous.setEnabled(true);
            } else if(eIndex>=commandHistory.size()){
                commandIndex = 0;
                next.setEnabled(false);
                input.setText(historyTemp[0]);
                previous.setEnabled(true);
            }
        });

        next.setEnabled(false);

        JButton eval = new JButton("eval");
        eval.addActionListener((event) -> {

            String s = input.getText();
            input.setText("");
            historyTemp[0] = "";
            commandHistory.add(s);
            commandIndex = 0;
            next.setEnabled(false);
            previous.setEnabled(true);

            evaluateExpression(s);

        });
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.add(eval);
        buttons.add(previous);
        buttons.add(next);
        root.add(house);
        root.add(buttons);

        frame = new JFrame();
        frame.setContentPane(root);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        return root;
    }
    public void showTerminal(){
        if(frame==null){
            buildUI();
        }
        frame.setVisible(true);
    }
    private void evaluateExpression(String s){
        String[] lines = s.split("\n");
        for(String line: lines){
            history.add(line + '\n');
        }
        EventQueue.invokeLater(() -> display.setText(""));

        try{
            engine.eval(s);
        } catch (ScriptException e) {

            StackTraceElement[] elements = e.getStackTrace();
            history.add(e.getMessage() + '\n');
            if(elements.length>0){

                history.add(elements[0].toString() + '\n');

            }

        }
        StringBuilder build = new StringBuilder();
        history.stream().forEach((w)->build.append(w));

        EventQueue.invokeLater(() -> display.setText(build.toString()));

    }

    public static void main(String[] args){
        SwingJSTerm term = new SwingJSTerm(null);
        term.showTerminal();
        term.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
