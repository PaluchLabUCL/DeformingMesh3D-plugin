package deformablemesh.util.actions;

/**
 * Created by msmith on 12/8/15.
 */
public interface UndoableActions {
    void perform();
    void undo();
    void redo();
    default String getName(){return "";}
}
