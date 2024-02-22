/*-
 * #%L
 * Triangulated surface for deforming in 3D.
 * %%
 * Copyright (C) 2013 - 2023 University College London
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package deformablemesh.gui;

import deformablemesh.SegmentationController;
import deformablemesh.SegmentationModel;
import ij.IJ;

import javax.script.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    JTextField scriptFile;
    JButton runScriptFile;

    public SwingJSTerm(SegmentationController controls){
        ScriptEngineManager manager = new ScriptEngineManager();

        //NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        //engine = factory.getScriptEngine();
        engine = manager.getEngineByName("nashorn");
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

    public void runFile(Path path){
        try(BufferedReader reader  = Files.newBufferedReader(path)){
            engine.eval(reader);
        } catch (IOException e) {
            echo("Error reading file!");
            PrintWriter writer = new PrintWriter(new Writer() {
                StringBuilder builder = new StringBuilder();
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    for(int i = off; i<off + len; i++){
                        builder.append(cbuf[i]);
                    }
                }

                @Override
                public void flush() throws IOException {
                    echo(builder);
                    builder = new StringBuilder();
                }

                @Override
                public void close() throws IOException {
                    echo(builder);
                }
            });

            e.printStackTrace(writer);
            writer.close();
        } catch (ScriptException e) {
            echo("Error running script!");
            PrintWriter writer = new PrintWriter(new Writer() {
                StringBuilder builder = new StringBuilder();
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    for(int i = off; i<off + len; i++){
                        builder.append(cbuf[i]);
                    }
                }

                @Override
                public void flush() throws IOException {
                    echo(builder);
                    builder = new StringBuilder();
                }

                @Override
                public void close() throws IOException {
                    echo(builder);
                }
            });

            e.printStackTrace(writer);
            writer.close();
        }
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
        scriptFile = new JTextField(65);
        runScriptFile = new JButton("run file");
        runScriptFile.addActionListener(evt->{

            Path p = null;
            if(scriptFile.getText().length() != 0) {
                p = Paths.get(scriptFile.getText());
                if( !Files.exists(p)){
                    p = null;
                }
            }
            if(p == null){
                String s = IJ.getFilePath("select script to run");
                p = Paths.get(s).toAbsolutePath();
                scriptFile.setText(p.toString());
            }
            if(p != null){
                runFile(p);
            }
        });
        JSplitPane root = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        display = new JTextArea("**shift+enter will execute command immediately.**\n");
        display.setEditable(false);
        display.setCaretPosition(display.getDocument().getLength());

        display.setBorder(BorderFactory.createCompoundBorder(
                display.getBorder(),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JScrollPane display_pane = new JScrollPane(display);
        root.add(display_pane, JSplitPane.TOP);



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
        buttons.add(scriptFile);
        buttons.add(runScriptFile);
        root.add(house, JSplitPane.BOTTOM);
        Border b = BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );
        root.setBorder(b);
        frame = new JFrame();
        JPanel content = new JPanel(new BorderLayout());
        content.add(root, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        frame.setContentPane(content);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        return content;
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

            controls.submit(()->{
                observers.forEach(o->o.setReady(false));
                try{
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
                    observers.forEach(o->o.setReady(true));
                }

            });

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
