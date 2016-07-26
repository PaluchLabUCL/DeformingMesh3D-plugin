package deformablemesh.geometry;

/**
 *
 * An ordered collection of 3 nodes that will be used for calculating geometric properties.
 * The winding of the triangle should be such that CCW points outwards from the volume
 * contained within.
 *
 * User: msmith
 * Date: 7/2/13
 * Time: 7:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class Triangle3D {
    final Node3D A,B,C;

    public double area;
    public double[] normal;
    public double[] center;

    public Triangle3D(Node3D a, Node3D b, Node3D c){
        A = a;
        B = b;
        C = c;
        normal = new double[3];
        center = new double[3];
    }

    //cacluates the area the normal and the center.
    final static double one_third = 1.0/3.0;
    public void update(){
        //calculate area
        double[] a = A.getCoordinates();
        double[] b = B.getCoordinates();
        double[] c = C.getCoordinates();

        double[] ab = new double[3];
        double[] ac = new double[3];
        for(int i = 0; i<3; i++){
            ab[i] = b[i] - a[i];
            ac[i] = c[i] - a[i];
        }

        normal[0] = (ab[1]*ac[2] - ab[2]*ac[1]);
        normal[1] = (ab[2]*ac[0] - ab[0]*ac[2]);
        normal[2] = (ab[0]*ac[1] - ab[1]*ac[0]);

        area = 0.5*Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if(area>0){
            for(int i = 0; i<3; i++){
                normal[i] = normal[i]/area;
                center[i] = (a[i] + b[i] + c[i])*one_third;
            }
        }



    }

    public int[] getIndices(){
        return new int[]{A.index, B.index, C.index};
    }

    public boolean hasNode(Node3D other){

        return A==other||B==other||C==other;

    }

    public double[] getNormal() {
        return normal;
    }

    @Override
    public int hashCode(){
        return A.index + B.index + C.index;
    }

    @Override
    public boolean equals(Object o){
        if(o==null) return false;


        if(o instanceof Triangle3D ){
            //check for cyclic equality.
            Triangle3D ot = (Triangle3D)o;
            if(A.index==ot.A.index){
                return B.index==ot.B.index&&C.index==ot.C.index;
            } else if(A.index==ot.B.index){
                return B.index==ot.C.index&&C.index==ot.A.index;
            } else if(A.index==ot.C.index){
                return B.index==ot.A.index&&C.index==ot.B.index;
            }
        }

        return false;
    }

    public void getIndices(int[] indexes) {
        indexes[0] = A.index;
        indexes[1] = B.index;
        indexes[2] = C.index;
    }
}
