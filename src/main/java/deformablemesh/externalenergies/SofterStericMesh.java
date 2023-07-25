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
package deformablemesh.externalenergies;

import deformablemesh.geometry.CurvatureCalculator;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.geometry.Intersection;
import deformablemesh.util.Vector3DOps;

import java.util.List;
import java.util.stream.Collectors;

public class SofterStericMesh extends StericMesh {
    CurvatureCalculator curve;

    public SofterStericMesh(DeformableMesh3D id, DeformableMesh3D neighbor, double weight) {
        super(id, neighbor, weight);
        curve = new CurvatureCalculator(id);
    }

    public double[] getNormal(Integer i) {
        double[] n = curve.getNormal(i);

        return n;
    }

    static class RotatedIntersection {
        double[] surface;
        double dot;

        /**
         * Creates an intersection that doesn't need a location because it is assumed to be along the normal.
         *
         * @param origin
         * @param normal
         * @param s
         */
        RotatedIntersection(double[] origin, double[] normal, Intersection s) {
            double dx = s.location[0] - origin[0];
            double dy = s.location[1] - origin[1];
            double dz = s.location[2] - origin[2];

            dot = Vector3DOps.dot(new double[]{dx, dy, dz}, normal);
            surface = s.surfaceNormal;
        }
    }

    @Override
    public void updateForces(double[] positions, double[] fx, double[] fy, double[] fz) {
        if (!staticShape || mesh == null) {
            mesh = new InterceptingMesh3D(deformableMesh);
        }


        double[] pt = new double[3];
        //double[] center = mesh.getCenter();

        for (int i = 0; i < fx.length; i++) {
            pt[0] = positions[3 * i];
            pt[1] = positions[3 * i + 1];
            pt[2] = positions[3 * i + 2];

            if (mesh.boundsContains(pt)) {
                double[] normal = getNormal(i);
                List<RotatedIntersection> intersections = mesh.getIntersections(pt, normal).stream().map(
                        is -> new RotatedIntersection(pt, normal, is)
                ).collect(Collectors.toList());

                //sort the intersections by their distance from pt along the normal axis.
                intersections.sort((a, b) -> Double.compare(a.dot, b.dot));

                //start outside
                boolean outside = true;

                //cross intersections until we pass our point.
                double penetration = 0;

                //we're coming from negative infinity (outside) to the origin.
                boolean borked = false;
                for (RotatedIntersection inter : intersections) {
                    if (inter.dot > 0) {

                        //We've crossed the origin, so the last intersection determines
                        // the penetration.
                        if(inter.dot < -penetration){
                            penetration = inter.dot;
                            //It's closer to leave out this way. 2 possibilties:
                            //  - The mesh is inverted.
                            //  - The mesh crosses the intersected mesh.
                        }
                        break;
                    } else {

                        boolean facingUp = Vector3DOps.dot(inter.surface, normal) > 0;
                        if (facingUp) {
                            //surface is facing up. Should only happen when we just left.
                            if (outside) {
                                borked = true;
                            } else if (borked) {
                                //this is a very rare condition.
                                borked = false;
                            }

                            outside = !outside;

                        } else {
                            if (!outside) {
                                borked = true;
                            } else if (borked) {
                                //Gets better?
                                borked = false;
                            }
                            outside = !outside;
                            penetration = inter.dot;
                        }
                    }
                }

                if (borked) {
                    continue;
                }

                if (!outside) {
                    fx[i] += penetration * weight * normal[0] * 100;
                    fy[i] += penetration * weight * normal[1] * 100;
                    fz[i] += penetration * weight * normal[2] * 100;
                }
            }

        }
    }

    /**
     * Finds the normal of the neighbor node nearest to the provided point.
     *
     * @param pt
     * @return
     */
    public double[] getEffectiveNormal(double[] pt) {
        int best = -1;
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < deformableMesh.nodes.size(); i++) {
            double l = Vector3DOps.distance(pt, deformableMesh.nodes.get(i).getCoordinates());
            if(l<closest){
                best = i;
                closest = l;
            }
            if(l == 0){
                break;
            }
        }

        return getNormal(best);
    }


    @Override
    public double getEnergy(double[] pt) {
        if (!staticShape || mesh == null) {
            mesh = new InterceptingMesh3D(deformableMesh);
        }

        if (mesh.boundsContains(pt)) {
            double[] actual = getEffectiveNormal(pt);
            double[] normal = {-actual[0], -actual[1], -actual[2]};
            List<RotatedIntersection> intersections = mesh.getIntersections(pt, normal).stream().map(
                    is -> new RotatedIntersection(pt, normal, is)
            ).collect(Collectors.toList());

            //sort the intersections by their distance from pt along the normal axis.
            intersections.sort((a, b) -> Double.compare(a.dot, b.dot));

            //start outside
            boolean outside = true;

            //cross intersections until we pass our point.
            double penetration = 0;

            //we're coming from negative infinity (outside) to the origin.
            boolean borked = false;
            for (RotatedIntersection inter : intersections) {
                if (inter.dot > 0) {
                    //We've crossed the origin, so the last intersection determines
                    // the penetration.
                    break;
                } else {

                    boolean facingUp = Vector3DOps.dot(inter.surface, normal) > 0;
                    if (facingUp) {
                        //surface is facing up. Should only happen when we just left.
                        if (outside) {
                            borked = true;
                        } else if (borked) {
                            //this is a very rare condition.
                            borked = false;
                        }

                        outside = !outside;

                    } else {
                        if (!outside) {
                            borked = true;
                        } else if (borked) {
                            //Gets better?
                            borked = false;
                        }
                        outside = !outside;
                        penetration = inter.dot;
                    }
                }
            }

            if (!outside) {
                return penetration * penetration;
            }
        }
        return 0;
    }
}
