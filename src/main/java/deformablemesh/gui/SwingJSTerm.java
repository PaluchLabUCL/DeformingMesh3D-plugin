package deformablemesh.gui;

import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Point;
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

import static java.awt.event.KeyEvent.VK_ENTER;

/**
 * Created by msmith on 4/14/14.
 */
public class SwingJSTerm {

    final ScriptEngine engine;
    JTextArea display, input;
    List<String> commandHistory = new ArrayList<>();
    List<ReadyObserver> observers = new ArrayList<>();
    JFrame frame;
    SegmentationController controls;
    int commandIndex;
    String[] historyTemp = new String[1];
    JButton previous;
    JButton next;

    public SwingJSTerm(SegmentationController controls){

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine();

        Bindings bindings = engine.createBindings();

        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        engine.put("controls", controls);
        engine.put("terminal", this);
        try {
            addClasses();
        } catch (Exception e) {
            //do without.
            e.printStackTrace();
        }
        this.controls = controls;
    }

    public void appendToDisplay(String text){
        int pos = display.getCaretPosition();

        display.append(text);
        //System.out.println("old: " + pos + " new: " + display.getCaretPosition());
        //display.setCaretPosition(display.getDocument().getLength());

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
        previous = new JButton("previous");
        next = new JButton("next");

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
        //root.setLayout(new BorderLayout());

        display = new JTextArea("**shift+enter will execute command immediately.**\n");
        display.setEditable(false);
        display.setCaretPosition(display.getDocument().getLength());

        display.setBorder(BorderFactory.createCompoundBorder(
                display.getBorder(),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JScrollPane display_pane = new JScrollPane(display);
        root.add(display_pane);



        TextBoxSelections tbs = new TextBoxSelections(engine);
        input = tbs.input;
        input.setRows(10);

        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_MASK);

        input.getInputMap().put(ks, "Submit");
        input.getActionMap().put("Submit", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent evt){
                submit();
            }
        });

        JScrollPane house = new JScrollPane(input);








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
            submit();
        });

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.add(eval);
        buttons.add(previous);
        buttons.add(next);
        root.add(house);
        root.add(buttons);
        Border b = BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );
        root.setBorder(b);
        frame = new JFrame();
        frame.setContentPane(root);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        return root;
    }

    public void echo(Object o){
        String echoed;
        if(o.getClass().isArray()){
            if(o instanceof double[]){
                echoed = Arrays.toString((double[])o);
            } else if(o instanceof int[]){
                echoed = Arrays.toString((int[])o);
            } else if(o instanceof byte[]){
                echoed = Arrays.toString((byte[])o);
            } else if(o instanceof Object[]){
                echoed = Arrays.toString((Object[])o);
            } else{
                echoed = o.toString();
            }
        }else{
            echoed = o.toString() + "\n";
        }
        EventQueue.invokeLater(()->{
            appendToDisplay(echoed);
        });
    }

    public void submit(){
        String s = input.getText();
        if(s.length() == 0){
            return;
        }
        input.setText("");
        historyTemp[0] = "";
        commandHistory.add(s);
        commandIndex = 0;
        next.setEnabled(false);
        previous.setEnabled(true);

        evaluateExpression(s);
    }

    public void showTerminal(){
        if(frame==null){
            buildUI();
        }
        frame.setVisible(true);
    }

    public void evaluateHeadless(String s){
        try{
            engine.eval(s);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    private void evaluateExpression(String s){
        String[] lines = s.split("\n");

        EventQueue.invokeLater(()->{
            for(String line: lines){
                appendToDisplay(line + '\n');
            }
        });

        try{
            controls.submit(()->{
                observers.forEach(o->o.setReady(false));
            });
            engine.eval(s);
        } catch (ScriptException e) {
            EventQueue.invokeLater(()->{
                StackTraceElement[] elements = e.getStackTrace();
                appendToDisplay(e.getMessage() + '\n');
                if(elements.length>0){
                    appendToDisplay(elements[0].toString() + '\n');
                }
            });

        } finally{
            if(controls!=null){
                controls.submit(()->{
                    observers.forEach(o->o.setReady(true));
                });
            }
        }
        StringBuilder build = new StringBuilder();


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


class TextBoxSelections{
    Popup lastPopUp;
    JScrollPane view;
    JList<String> listView;
    JTextArea input;
    ScriptEngine engine;
    TextBoxSelections(ScriptEngine engine){
        input = new JTextArea();
        input.addCaretListener(evt->{
            hidePopUp();
        });
        this.engine = engine;
        InputMap im = input.getInputMap();
        ActionMap actions = input.getActionMap();
        KeyStroke tab = KeyStroke.getKeyStroke("TAB");
        Caret caret = input.getCaret();
        KeyStroke up = KeyStroke.getKeyStroke("UP");
        KeyStroke down = KeyStroke.getKeyStroke("DOWN");
        KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(escape, "escape");

        actions.put(im.get(escape), new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if(lastPopUp!=null) {
                    hidePopUp();
                }
            }
        });
        Action oldEnter = actions.get(im.get(enter));

        input.getActionMap().put(im.get(enter), new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if(lastPopUp!=null) {
                    if(listView.getSelectedValue()==null){
                        listView.setSelectedIndex(0);
                    }
                    insertSuggestion();                }
                else{
                    oldEnter.actionPerformed(e);
                }
            }
        });

        Action oldUp = actions.get(im.get(up));
        Action oldDown = actions.get(im.get(down));


        input.getActionMap().put(im.get(up), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(lastPopUp!=null) {
                    int i = listView.getSelectedIndex();
                    if(i>0){
                        listView.setSelectedIndex(i-1);
                    }
                }
                else{
                    oldUp.actionPerformed(e);
                }
            }
        });
        input.getActionMap().put(im.get(down), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(lastPopUp!=null) {
                    int i = listView.getSelectedIndex();
                    if(i<listView.getModel().getSize()){
                        listView.setSelectedIndex(i+1);
                    }
                } else{
                    oldDown.actionPerformed(e);
                }
            }
        });

        input.getActionMap().put(im.get(tab), new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(lastPopUp!=null){
                    //a second tab is assumed to mean take the presented option.
                    if(listView.getSelectedValue()==null){
                        listView.setSelectedIndex(0);
                    }
                    insertSuggestion();
                    return;
                }

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
                    List<String> suggestions;
                    if(!partial.contains(".")){
                        suggestions = getTopSuggestions(partial);
                    } else{
                        if(partial.equals(".")){
                            starting = Utilities.getWordStart(input, loc-2);
                            partial = input.getText(starting, loc - starting);
                        }
                        suggestions = getSuggestions(partial);
                    }

                    if(suggestions.size()>0){
                        JList<String> list = new JList<>(new ListModel<String>(){

                            @Override
                            public int getSize() {
                                return suggestions.size();
                            }

                            @Override
                            public String getElementAt(int index) {
                                return suggestions.get(index);
                            }

                            @Override
                            public void addListDataListener(ListDataListener l) {
                            }

                            @Override
                            public void removeListDataListener(ListDataListener l) {

                            }
                        });


                        list.setFocusable(false);

                        Point pt = caret.getMagicCaretPosition();

                        Point pt2 = new Point(pt);
                        SwingUtilities.convertPointToScreen(pt2, input);

                        view = new JScrollPane();
                        view.setViewportView(list);

                        Popup pop = PopupFactory.getSharedInstance().getPopup(input, view, pt2.x, pt2.y);
                        pop.show();


                        listView = list;
                        lastPopUp = pop;


                    }

                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }


            }
        });
    }
    List<String> getTopSuggestions(String partial){
        final int l = partial.length();
        if(l>0){
            return engine.getBindings(ScriptContext.ENGINE_SCOPE).keySet().stream().filter(
                    s->s.startsWith(partial)
            ).map(
                    s->s.substring(l)
            ).collect(Collectors.toList());

        }
        return engine.getBindings(ScriptContext.ENGINE_SCOPE).keySet().stream().collect(Collectors.toList());

    }
    List<String> getSuggestions(String partial){
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        List<String> suggestions = new ArrayList<>();

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
                        s->suggestions.add(s.substring(filter.length()))
                );
                methods.stream().filter(
                        s->s.startsWith(filter)
                ).forEach(
                        s->suggestions.add(s.substring(filter.length()))
                );
            } else{
                //apply a filter.
                fields.forEach(
                        suggestions::add
                );
                methods.forEach(
                        suggestions::add
                );
            }
        }

        return suggestions;
    }

    void insertSuggestion(){
        String rep = listView.getSelectedValue();

        if(rep!=null) {
            Caret caret = input.getCaret();
            Document doc = input.getDocument();
            int loc = caret.getMark();

            input.replaceRange(rep, loc, loc);

        }
        hidePopUp();
    }
    List<String> getAvailableFields( Object obj){
        return Arrays.stream(obj.getClass().getFields()).map(Field::getName).collect(Collectors.toList());
    }
    List<String> getAvailableMethodNames(Object obj){
        return Arrays.stream(obj.getClass().getMethods()).map(Method::getName).collect(Collectors.toList());
    }
    void hidePopUp(){
        if(lastPopUp!=null){
            lastPopUp.hide();
            lastPopUp=null;
            input.requestFocus();
        }
    }
}