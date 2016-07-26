package deformablemesh.util.actions;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by msmith on 4/21/16.
 */
public class ActionStackTests {
    String current =  "none";

    @Test
    public void orderTesting(String[] args){

        ActionStack stack = new ActionStack();

        stack.postAction(new TestAction("one"));
        stack.postAction(new TestAction("two"));
        stack.postAction(new TestAction("three"));
        stack.postAction(new TestAction("four"));
        stack.postAction(new TestAction("five"));
        stack.postAction(new TestAction("six"));
        Assert.assertEquals("six", current);
        stack.undo();
        Assert.assertEquals("five", current);
        stack.undo();
        Assert.assertEquals("four", current);
        stack.redo();
        Assert.assertEquals("five", current);
        stack.postAction(new TestAction("seven"));
        Assert.assertEquals("seven", current);

        Assert.assertFalse(stack.hasRedo());

        while(stack.hasUndo()){
            stack.undo();
        }
        Assert.assertEquals("none", current);


    }

    class TestAction implements UndoableActions{
        final String label;
        final String before;
        TestAction(String l){
            before = current;
            label = l;
        }
        @Override
        public void perform() {
            current = label;
        }

        @Override
        public void undo() {
            current = before;
        }

        @Override
        public void redo() {
            current = label;
        }
    }
}
