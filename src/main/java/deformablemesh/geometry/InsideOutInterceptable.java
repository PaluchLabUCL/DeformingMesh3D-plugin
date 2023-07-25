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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Object should be created with the surface facing outwards. This class is a convenience to create a surface facing
 * inwards. For example a mesh that stops ray-casting from passing through it the normals would face in.
 *
 * Essentially the outside is now the inside.
 * Created by msmith on 4/8/16.
 */
public class InsideOutInterceptable implements Interceptable {
    Interceptable regular;
    public InsideOutInterceptable(Interceptable interceptable){
        regular = interceptable;
    }
    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        return regular.getIntersections(origin, direction).stream().map(InsideOutInterceptable::invert).collect(Collectors.toList());
    }
    static Intersection invert(Intersection section){
        return new Intersection(section.location, invert(section.surfaceNormal));
    }

    static double[] invert(double[] d){
        return new double[]{-d[0], -d[1], -d[2]};
    }
}
