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

import Jama.LUDecomposition;
import Jama.Matrix;
import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.LineDataObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeformableLine3D {

    public double GAMMA;
    public double ALPHA;
    public double BETA;
    LUDecomposition decomp;

    List<ExternalEnergy> energies = new ArrayList<>();

    public List<Node3D> nodes;
    public List<Connection3D> connections;
    public double[] positions;
    public int[] connection_index;

    public LineDataObject data_object;
    private final static ExecutorService pool = Executors.newFixedThreadPool(3);

    private Color color = Color.BLUE;
    private boolean selected;
    public DeformableLine3D(List<double[]> positions, List<int[]> connections){
        this.positions = new double[positions.size()*3];
        this.connection_index = new int[connections.size()*2];

        nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
        for(double[] pt: positions){
            Node3D n = new Node3D(this.positions, nodes.size());
            n.setPosition(pt);
            nodes.add(n);
        }
        //Create connections and index array.
        for(int i = 0; i<connections.size(); i++){
            int[] dices = connections.get(i);

            Connection3D c = new Connection3D(nodes.get(dices[0]), nodes.get(dices[1]));
            this.connections.add(c);
            connection_index[2*i] = dices[0];
            connection_index[2*i+1] = dices[1];
        }

    }
    public void addExternalEnergy(ExternalEnergy e){
        energies.add(e);
    }
    public void reshape(){
        double[][] data = new double[nodes.size()][nodes.size()];
        if(BETA>0){
            //this has a simpler beta matrix.
        }

        for(Connection3D c: connections){

            c.update();
            int[] dex = c.getIndices();
            data[dex[0]][dex[0]] += ALPHA;
            data[dex[0]][dex[1]] += -ALPHA;
            data[dex[1]][dex[0]] += -ALPHA;
            data[dex[1]][dex[1]] += ALPHA;

        }


        for(Node3D n: nodes){
            data[n.index][n.index] += GAMMA;
        }

        Matrix M = new Matrix(data);

        decomp = M.lu();
    }
    public void update(){
        if(decomp==null){
            reshape();
        }

        final double[] fx = new double[nodes.size()];
        final double[] fy = new double[nodes.size()];
        final double[] fz = new double[nodes.size()];

        double[] pt;
        for(Node3D n: nodes){
            n.update();
            pt = n.getCoordinates();
            fx[n.index] += GAMMA*pt[0];
            fy[n.index] += GAMMA*pt[1];
            fz[n.index] += GAMMA*pt[2];
        }

        for(ExternalEnergy external: energies) {
            external.updateForces(positions, fx, fy, fz);
        }

        Future<double[]> xfuture = pool.submit(() -> {
            final Matrix FX = new Matrix(fx,nodes.size());
            Matrix deltax = decomp.solve(FX);
            return deltax.getRowPackedCopy();
        });

        Future<double[]> yfuture = pool.submit(() -> {
            final Matrix FY = new Matrix(fy,nodes.size());
            Matrix deltay = decomp.solve(FY);
            return deltay.getRowPackedCopy();
        });

        Future<double[]> zfuture = pool.submit(() -> {
            final Matrix FZ = new Matrix(fz,nodes.size());
            Matrix deltaz = decomp.solve(FZ);
            return deltaz.getRowPackedCopy();
        });


        try {
            double[] nx = xfuture.get();
            double[] ny = yfuture.get();
            double[] nz = zfuture.get();
            for(int i = 1; i<nodes.size()-1; i++){
                positions[3*i] = nx[i];
                positions[3*i+1] = ny[i];
                positions[3*i+2] = nz[i];

            }

        } catch (InterruptedException e) {
            System.err.println("Program was interrupted during calculations!");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.err.println("Exception Occurred During update");
            e.printStackTrace();
        }

        if(data_object!=null){
            data_object.updateGeometry(positions);
        }
    }

    public void createDataObject(){
        data_object = new LineDataObject(nodes);
    }
    public DataObject getDataObject(){
        if(data_object == null) createDataObject();

        return data_object;
    }

    public void clearEnergies() {
        energies.clear();
    }
}
