package deformablemesh.util.actions;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by msmith on 12/8/15.
 */
public class ActionStack {
    final int MAX_HISTORY = 25;
    final Deque<UndoableActions> history = new ConcurrentLinkedDeque<>();
    final Deque<UndoableActions> undone = new ConcurrentLinkedDeque<>();
    public boolean hasUndo(){
        return history.size()>0;
    }

    public boolean hasRedo(){
        return undone.size()>0;
    }

    public String getUndoableActionName(){
        if(history.size()>0)
            return history.getLast().getName();
        else
            return "";
    }

    public String getRedoableActionName(){
        if(undone.size()>0)
            return undone.getLast().getName();
        else
            return "";
    }

    public void undo(){
        UndoableActions action = history.pollLast();
        undone.add(action);
        action.undo();
    }

    public void redo(){
        UndoableActions action = undone.pollLast();
        action.redo();
        history.add(action);
    }

    public void postAction(UndoableActions action){

        history.add(action);
        if(history.size()>MAX_HISTORY){
            history.removeFirst();
        }
        if(undone.size()>0){
            undone.clear();
        }
        action.perform();

    }

    /**
     * For performing multiple actions such that they are undone, and redone together and in sequence.
     * @param a
     * @param b
     * @return
     */
    public static UndoableActions chainActions(UndoableActions a, UndoableActions b){

        return new UndoableActions(){

            @Override
            public void perform() {
                a.perform();
                b.perform();
            }

            @Override
            public void undo() {
                b.undo();
                a.undo();
            }

            @Override
            public void redo() {
                a.redo();
                b.redo();
            }

            @Override
            public String getName(){
                return a.getName() + b.getName();
            }
        };
    }




}
