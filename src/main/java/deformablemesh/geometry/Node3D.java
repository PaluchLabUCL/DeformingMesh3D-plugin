package deformablemesh.geometry;

/**
 * Essentially a pointer. It keeps a reference to the backing array of data, and the
 * starting position.
 *
 * User: msmith
 * Date: 7/2/13
 * Time: 7:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class Node3D {
    private double[] positions;
    public final int index;
    public Node3D(double[] data, int index){
        positions = data;
        this.index = index;
    }

    public int getIndex(){
        return index;
    }


    void update(){

    }

    /**
     * Creates a double[] with the position of this node.
     *
     * @return a new double[] that contains the x,y,z coordinates of this array.
     */
    public double[] getCoordinates(){
        return new double[]{positions[3*index], positions[3*index+1], positions[3*index+2]};
    }

    /**
     * Updates the array backing this position.
     *
     * @param pos
     */
    public void setPosition(double[] pos){
        for(int i = 0; i<3; i++){
            positions[3*index + i] = pos[i];
        }
    }

    /**
     * Sets the position array that this node uses to back its data. This method is for use when updating a mesh by
     * by adding or removing points.
     *
     * @param data the array containing the position information. This array should contain all of the positions for
     *             all of the Node3D in the same mesh.
     */
    public void setBackingData(double[] data){
        positions = data;
    }

    @Override
    public boolean equals(Object o){
        if(o==null)return false;
        if(o instanceof Node3D){
            return ((Node3D)o).positions==positions && ((Node3D)o).index==index;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return index;
    }


}
