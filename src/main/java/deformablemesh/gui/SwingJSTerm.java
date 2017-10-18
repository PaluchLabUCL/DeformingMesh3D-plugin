package deformablemesh.gui;

import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.rmi.CORBA.Util;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by msmith on 4/14/14.
 */
public class SwingJSTerm {

    final ScriptEngine engine;
    JTextArea display, input;
    List<String> history = new LinkedList<String>();
    List<String> commandHistory = new ArrayList<>();
    List<ReadyObserver> observers = new ArrayList<>();
    JFrame frame;
    SegmentationController controls;
    int commandIndex;
    SwingJSTerm(SegmentationController controls){

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine();

        Bindings bindings = engine.createBindings();

        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        engine.put("controls", controls);
        try {
            addClasses();
        } catch (Exception e) {
            //do without.
            e.printStackTrace();
        }
        this.controls = controls;
    }


    public void addClasses() throws ScriptException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/load-bindings.js"), StandardCharsets.UTF_8
                )
        );
        engine.eval(reader);

    }

    JPanel buildUI(){
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
        //root.setLayout(new BorderLayout());

        display = new JTextArea();
        display.setEditable(false);
        JScrollPane display_pane = new JScrollPane(display);
        display_pane.setPreferredSize(new Dimension(600, 100));
        root.add(display_pane);

        input = new JTextArea();
        InputMap im = input.getInputMap();
        KeyStroke tab = KeyStroke.getKeyStroke("TAB");

        input.getActionMap().put(im.get(tab), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Caret caret = input.getCaret();
                Document doc = input.getDocument();
                int loc = caret.getMark();
                int len = doc.getLength();


                try {
                    int end = Utilities.getWordEnd(input, loc);
                    int starting = Utilities.getWordStart(input, loc);
                    int l = loc - starting;
                    if(l==0){
                        if(len - loc != 0){

                            end = Utilities.getWordEnd(input, loc - 1);
                            starting = Utilities.getWordStart(input, loc - 1);
                            if(end==loc){
                                l = loc - starting;
                            }
                        } else{
                            return;
                        }
                    }
                    String partial = input.getText(starting, l);

                    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                    if(!partial.contains(".")){
                        for(String key: bindings.keySet()){
                            if(key.startsWith(partial)){
                                System.out.println("... " + key);
                            }
                        }
                    } else{
                        if(partial.equals(".")){
                            starting = Utilities.getWordStart(input, loc-2);
                            partial = input.getText(starting, loc - starting);
                        }
                        String[] orders = partial.split(Pattern.quote("."));

                        Object obj = bindings.get(orders[0]);

                        if(obj!=null){
                            List<String> fields = getAvailableFields(obj);
                            List<String> methods = getAvailableMethodNames(obj);

                            final String filter;
                            if(orders.length==1){
                                filter = "";
                            } else {
                                filter = orders[1];
                            }
                                if(filter.length()>0){
                                    //apply a filter.
                                    fields.stream().filter(
                                            s->s.startsWith(filter)
                                    ).forEach(
                                            System.out::println
                                    );
                                    methods.stream().filter(
                                            s->s.startsWith(filter)
                                    ).forEach(
                                            System.out::println
                                    );
                                } else{
                                    //apply a filter.
                                    fields.forEach(
                                            System.out::println
                                    );
                                    methods.forEach(
                                            System.out::println
                                    );
                                }
                            }

                    }


                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });

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
    List<String> getAvailableFields( Object obj){
        return Arrays.stream(obj.getClass().getFields()).map(Field::getName).collect(Collectors.toList());
    }
    List<String> getAvailableMethodNames(Object obj){
        return Arrays.stream(obj.getClass().getMethods()).map(Method::getName).collect(Collectors.toList());
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
            controls.submit(()->{
                observers.forEach(o->o.setReady(false));
            });
            engine.eval(s);
        } catch (ScriptException e) {

            StackTraceElement[] elements = e.getStackTrace();
            history.add(e.getMessage() + '\n');
            if(elements.length>0){

                history.add(elements[0].toString() + '\n');

            }

        } finally{
            if(controls!=null){
                controls.submit(()->{
                    observers.forEach(o->o.setReady(true));
                });
            }
        }
        StringBuilder build = new StringBuilder();
        history.stream().forEach((w)->build.append(w));

        EventQueue.invokeLater(() -> display.setText(build.toString()));

    }

    public static void main(String[] args){
        SwingJSTerm term = new SwingJSTerm(new SegmentationController(new SegmentationModel()));
        term.showTerminal();
        term.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void addReadyObserver(ReadyObserver observer) {
        observers.add(observer);
    }
}
