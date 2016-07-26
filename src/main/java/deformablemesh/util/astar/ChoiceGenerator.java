package deformablemesh.util.astar;

import java.util.List;

public interface ChoiceGenerator<T>{
    List<T> getChoices(T last);
}
