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
package deformablemesh.util.actions;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by msmith on 4/21/16.
 */
public class ActionStackTests {
    String current =  "none";

    @Test
    public void orderTesting(){

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
