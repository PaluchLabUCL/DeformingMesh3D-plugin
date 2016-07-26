package deformablemesh.geometry;

/**
 * User: msmith
 * Date: 7/2/13
 * Time: 7:56 AM
 */
public class Connection3D {
    final public Node3D A, B;
    double length;

    Connection3D(Node3D a, Node3D b){
        A=a;
        B=b;
    }

    void update(){
        double[] p1 = A.getCoordinates();
        double[] p2 = B.getCoordinates();
        double x = p1[0] - p2[0];
        double y = p1[1] - p2[1];
        double z = p1[2] - p2[2];
        length = Math.sqrt(x*x + y*y + z*z);
    }
    int[] getIndices(){
        return new int[]{A.index, B.index};
    }

    @Override
    public int hashCode(){
        return A.index+B.index;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Connection3D){
            Connection3D oc = (Connection3D)o;
            return A.index==oc.A.index?
                    B.index==oc.B.index :
                    A.index==oc.B.index && B.index==oc.A.index;
        }
        return false;
    }

}
