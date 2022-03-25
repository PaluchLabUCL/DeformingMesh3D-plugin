package deformablemesh.util.actions;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by msmith on 12/8/15.
 */
public class ActionStack {
    final int MAX_HISTORY = 25;
    final Deque<ActionState> history = new ConcurrentLinkedDeque<>();
    final Deque<ActionState> undone = new ConcurrentLinkedDeque<>();
    AtomicLong cumulativeStates = new AtomicLong(0);
    AtomicLong currentState = new AtomicLong(0);
    List<StateListener> listeners = new ArrayList<>();
    public long getCurrentState() {
        return currentState.get();
    }



    private class ActionState{
        final UndoableActions action;
        final long nextState;
        final long prevState;
        ActionState(UndoableActions action){
            this.action = action;
            nextState = newState();
            prevState = currentState.get();
        }
    }

    private long newState(){
        return cumulativeStates.incrementAndGet();
    }

    public boolean hasUndo(){
        return history.size()>0;
    }

    public boolean hasRedo(){
        return undone.size()>0;
    }

    public String getUndoableActionName(){
        if(history.size()>0)
            return history.getLast().action.getName();
        else
            return "";
    }

    public String getRedoableActionName(){
        if(undone.size()>0)
            return undone.getLast().action.getName();
        else
            return "";
    }
    void setCurrentState( long id){
        currentState.set(id);
        listeners.forEach(l -> l.stateUpdated(id));
    }
    public void undo(){
        ActionState state = history.pollLast();
        undone.add(state);
        state.action.undo();
        setCurrentState(state.prevState);
    }

    public void redo(){
        ActionState state = undone.pollLast();
        state.action.redo();
        history.add(state);
        setCurrentState(state.nextState);
    }

    public void clearHistory(){
        history.clear();
    }

    public void postAction(UndoableActions action){
        ActionState state = new ActionState(action);
        history.add(state);
        if(history.size()>MAX_HISTORY){
            history.removeFirst();
        }
        if(undone.size()>0){
            undone.clear();
        }
        action.perform();
        setCurrentState(state.nextState);
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

    public void addStateListener( StateListener sl ){
        listeners.add(sl);
    }

    public void removeStateListener(StateListener sl){
        int before = listeners.size();
        listeners.remove(sl);
        if(before == listeners.size()){
            System.out.println("warning: state listener not removed!");
        }
    }



}
