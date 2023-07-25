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
package deformablemesh.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by melkor on 3/9/16.
 */
public class CompositeInterceptables implements Interceptable{
    List<Interceptable> objects= new ArrayList<>();

    public CompositeInterceptables(Collection<? extends Interceptable> collection){
        objects.addAll(collection);
    }

    public CompositeInterceptables(Interceptable... items){
        for(Interceptable item: items){
            objects.add(item);
        }
    }

    public void addInterceptable(Interceptable i){
        objects.add(i);
    }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        List<Intersection> ret = new ArrayList<>();
        for(Interceptable i: objects){
            ret.addAll(i.getIntersections(origin, direction));
        }
        return ret;
    }

    @Override
    public boolean contains(double[] point){
        for(Interceptable i: objects){
            if(i.contains(point)){
                return true;
            }
        }
        return false;
    }
}
