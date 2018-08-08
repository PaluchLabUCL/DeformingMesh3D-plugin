package deformablemesh;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.Connection3D;
import deformablemesh.geometry.Intersection;

import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import deformablemesh.util.astar.AStarBasic;
import deformablemesh.util.astar.BasicCost;
import deformablemesh.util.astar.BasicHeuristic;
import deformablemesh.util.astar.Boundary;
import deformablemesh.util.astar.ChoiceGenerator;
import deformablemesh.util.astar.History;
import deformablemesh.util.astar.PossiblePath;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * User: msmith
 * Date: 8/1/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeformableMesh3DTools {

    /**
     * makes a rectangular prism mesh with triangular elements.
     * @param width size in x direction
     * @param height size in y direction
     * @param depth size in z direction
     *
     * @param segment_size size of each segment.
     * @return a mesh that will deform according to an image.
     */
    public static DeformableMesh3D createRectangleMesh(double width, double height, double depth, double segment_size){
        ArrayList<double[]> pts = new ArrayList<double[]>();
        ArrayList<int[]> connections = new ArrayList<int[]>();
        ArrayList<int[]> triangles = new ArrayList<int[]>();
        //For the complete length there will be n+1 nodes
        int nx = (int)(width/segment_size + 0.5) + 1;
        int ny = (int)(height/segment_size+0.5) + 1;
        int nz = (int)(depth/segment_size+0.5) + 1;
        double actual_w = (nx-1)*segment_size;
        double actual_h = (ny-1)*segment_size;
        double actual_z = (nz-1)*segment_size;

        int dex;

        //top face x-y @ actual_z/2
        int top_starting_dex = pts.size();
        for(int i = 0; i<nx; i++){
            for(int j = 0; j<ny; j++){
                dex = top_starting_dex + i*ny + j;
                pts.add(new double[]{
                        i*segment_size - actual_w/2,
                        j*segment_size - actual_h/2,
                        actual_z/2
                });

                //create a connection
                if(i>0){
                    connections.add(new int[]{
                            dex,
                            dex - ny
                    });
                }

                if(j>0){
                    connections.add(new int[]{
                            dex,
                            dex-1
                    });
                }
            }
        }

        for(int i = 1; i<nx; i++){
            for(int j = 1; j<ny; j++){
                dex = pts.size();
                //add center
                pts.add(new double[]{
                        (i-0.5)*segment_size - actual_w/2,
                        (j-0.5)*segment_size - actual_h/2,
                        actual_z/2
                });
                //first
                int a_dex = top_starting_dex + (i-1)*ny + (j-1);
                connections.add(new int[]{dex,a_dex});
                int b_dex = top_starting_dex + (i)*ny + (j-1);
                connections.add(new int[]{dex,b_dex});
                int c_dex = top_starting_dex + (i)*ny + (j);
                connections.add(new int[]{dex,c_dex});
                int d_dex = top_starting_dex + (i-1)*ny + (j);
                connections.add(new int[]{dex,d_dex});

                triangles.add(new int[]{dex, a_dex, b_dex});
                triangles.add(new int[]{dex, b_dex, c_dex});
                triangles.add(new int[]{dex, c_dex, d_dex});
                triangles.add(new int[]{dex, d_dex, a_dex});

            }
        }


        //bottom face x-y @ -actual_z/2
        int bottom_starting_dex = pts.size();
        for(int i = 0; i<nx; i++){
            for(int j = 0; j<ny; j++){

                dex = bottom_starting_dex + i*ny + j;
                pts.add(new double[]{
                        i*segment_size - actual_w/2,
                        j*segment_size - actual_h/2,
                        -actual_z/2
                });

                //create a connection
                if(i>0){
                    connections.add(new int[]{
                            dex,
                            dex - ny
                    });
                }

                if(j>0){
                    connections.add(new int[]{
                            dex,
                            dex - 1
                    });
                }


            }
        }
        //bottom face
        for(int i = 1; i<nx; i++){
            for(int j = 1; j<ny; j++){
                dex = pts.size();
                //add center
                pts.add(new double[]{
                        (i-0.5)*segment_size - actual_w/2,
                        (j-0.5)*segment_size - actual_h/2,
                        -actual_z/2
                });
                //first
                int a_dex = bottom_starting_dex + (i-1)*ny + (j-1);
                connections.add(new int[]{dex,a_dex});
                int b_dex = bottom_starting_dex + (i)*ny + (j-1);
                connections.add(new int[]{dex,b_dex});
                int c_dex = bottom_starting_dex + (i)*ny + (j);
                connections.add(new int[]{dex,c_dex});
                int d_dex = bottom_starting_dex + (i-1)*ny + (j);
                connections.add(new int[]{dex,d_dex});

                triangles.add(new int[]{dex, b_dex, a_dex});
                triangles.add(new int[]{dex, c_dex, b_dex});
                triangles.add(new int[]{dex, d_dex, c_dex});
                triangles.add(new int[]{dex, a_dex, d_dex});

            }
        }



        //left face  y-z @ -actual_x/2
        int left_starting_dex = pts.size();
        for(int i = 0; i<ny; i++){
            for(int j = 1; j<nz-1; j++){
                dex = left_starting_dex + i*(nz-2) + (j-1);
                pts.add(new double[]{
                        -actual_w/2,
                        i*segment_size - actual_h/2,
                        j*segment_size - actual_z/2
                });

                //creates a connection
                if(i>0){
                    //previous row
                    connections.add(new int[]{
                            dex,
                            dex - (nz-2)
                    });
                }

                if(j>1){
                    //previous column
                    connections.add(new int[]{
                            dex,
                            dex - 1
                    });
                }
            }
        }


        //left face connections
        for(int i = 0; i<ny; i++){
            connections.add(new int[]{
                    top_starting_dex + i,
                    left_starting_dex + i*(nz-2) + (nz-3)
            });

            connections.add(new int[]{
                    bottom_starting_dex + i,
                    left_starting_dex + i*(nz-2)
            });

        }

        //left face triangles
        for(int i = 1; i<ny; i++){
            for(int j = 2; j<nz-1; j++){
                dex = pts.size();
                //add center
                pts.add(new double[]{
                        -actual_w/2,
                        (i-0.5)*segment_size - actual_h/2,
                        (j-0.5)*segment_size - actual_z/2
                });
                //first
                int a_dex = left_starting_dex + (i-1)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,a_dex});
                int b_dex = left_starting_dex + (i)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,b_dex});
                int c_dex = left_starting_dex + (i)*(nz-2) + (j-1);
                connections.add(new int[]{dex,c_dex});
                int d_dex = left_starting_dex + (i-1)*(nz-2) + (j-1);
                connections.add(new int[]{dex,d_dex});

                triangles.add(new int[]{dex, b_dex, a_dex});
                triangles.add(new int[]{dex, c_dex, b_dex});
                triangles.add(new int[]{dex, d_dex, c_dex});
                triangles.add(new int[]{dex, a_dex, d_dex});

            }
        }

        //left face merging triangles
        for(int i = 1; i<ny; i++){

            dex = pts.size();

            pts.add(new double[]{
                    -actual_w/2,
                    (i-0.5)*segment_size - actual_h/2,
                    (0.5)*segment_size - actual_z/2
            });

            int a_dex = bottom_starting_dex + i-1;
            int b_dex = bottom_starting_dex + i;
            int c_dex = left_starting_dex + i*(nz-2);
            int d_dex = left_starting_dex + (i-1)*(nz-2);

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});

            dex = pts.size();

            pts.add(new double[]{
                    -actual_w/2,
                    (i-0.5)*segment_size - actual_h/2,
                    -(0.5)*segment_size + actual_z/2
            });

            a_dex = top_starting_dex + i;
            b_dex = top_starting_dex + i-1;
            c_dex = left_starting_dex + (i-1)*(nz-2) + (nz-3);
            d_dex = left_starting_dex + (i)*(nz-2) + (nz-3);

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});

        }

        //right face  y-z @ actual_x/2
        int right_starting_dex = pts.size();
        for(int i = 0; i<ny; i++){
            for(int j = 1; j<nz-1; j++){
                dex = right_starting_dex + i*(nz-2) + (j-1);
                pts.add(new double[]{
                        actual_w/2,
                        i*segment_size - actual_h/2,
                        j*segment_size - actual_z/2
                });

                //creates a connection
                if(i>0){
                    //previous row
                    connections.add(new int[]{
                            dex,
                            dex - (nz-2)
                    });
                }

                if(j>1){
                    //previous column
                    connections.add(new int[]{
                            dex,
                            dex - 1
                    });
                }
            }
        }

        for(int i = 0; i<ny; i++){
            connections.add(new int[]{
                    top_starting_dex + i + ny*(nx-1),
                    right_starting_dex + i*(nz-2) + (nz-3)
            });

            connections.add(new int[]{
                    bottom_starting_dex + i + ny*(nx-1),
                    right_starting_dex + i*(nz-2)
            });

        }

        //right face triangles
        for(int i = 1; i<ny; i++){
            for(int j = 2; j<nz-1; j++){
                dex = pts.size();
                //add center
                pts.add(new double[]{
                        actual_w/2,
                        (i-0.5)*segment_size - actual_h/2,
                        (j-0.5)*segment_size - actual_z/2
                });
                //first
                int a_dex = right_starting_dex + (i)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,a_dex});
                int b_dex = right_starting_dex + (i-1)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,b_dex});
                int c_dex = right_starting_dex + (i-1)*(nz-2) + (j-1);
                connections.add(new int[]{dex,c_dex});
                int d_dex = right_starting_dex + (i)*(nz-2) + (j-1);
                connections.add(new int[]{dex,d_dex});

                triangles.add(new int[]{dex, b_dex, a_dex});
                triangles.add(new int[]{dex, c_dex, b_dex});
                triangles.add(new int[]{dex, d_dex, c_dex});
                triangles.add(new int[]{dex, a_dex, d_dex});

            }
        }


        //right face merging triangles
        for(int i = 1; i<ny; i++){
            /*
            connections.add(new int[]{
                    top_starting_dex + i,
                    left_starting_dex + i*(nz-2) + (nz-3)
            });
            */
            dex = pts.size();

            pts.add(new double[]{
                    actual_w/2,
                    (i-0.5)*segment_size - actual_h/2,
                    (0.5)*segment_size - actual_z/2
            });

            int a_dex = bottom_starting_dex + i + ny*(nx-1);
            int b_dex = bottom_starting_dex + i-1 + ny*(nx-1);
            int c_dex = right_starting_dex + (i-1)*(nz-2);
            int d_dex = right_starting_dex + (i)*(nz-2);

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});

            dex = pts.size();

            pts.add(new double[]{
                    actual_w/2,
                    (i-0.5)*segment_size - actual_h/2,
                    -(0.5)*segment_size + actual_z/2
            });

            a_dex = top_starting_dex + i-1 + ny*(nx-1);
            b_dex = top_starting_dex + i + ny*(nx-1);
            c_dex = right_starting_dex + (i)*(nz-2) + (nz-3);
            d_dex = right_starting_dex + (i-1)*(nz-2) + (nz-3);

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});

        }


        //front face x-z @ -actual_y/2
        int front_starting_dex = pts.size();
        for(int i = 1; i<nx-1; i++){
            for(int j = 1; j<nz-1; j++){
                dex = front_starting_dex + (i-1)*(nz-2) + (j-1);
                pts.add(new double[]{
                        i*segment_size - actual_w/2,
                        -actual_h/2,
                        j*segment_size - actual_z/2
                });

                if(i>1){
                    connections.add(new int[]{
                            dex,
                            dex - (nz-2)
                    });
                }

                if(j>1){
                    connections.add(new int[]{
                            dex,
                            dex - 1
                    });
                }


            }
        }

        //connect to top and bottom.
        for(int i = 1; i<nx-1; i++){
            connections.add(new int[]{
                    front_starting_dex + (i-1)*(nz - 2),
                    bottom_starting_dex + i*ny
            });

            connections.add(new int[]{
                    front_starting_dex + (i-1)*(nz - 2) + (nz-3),
                    top_starting_dex + i*ny
            });

        }

        //connect to left and right
        for(int j = 1; j<nz-1; j++){
            connections.add(new int[]{
                    front_starting_dex + (j-1),
                    left_starting_dex + j - 1
            });

            connections.add(new int[]{
                    front_starting_dex + (j-1) + (nz-2)*(nx-3),
                    right_starting_dex + j - 1
            });
        }

        //front face triangles
        for(int i = 2; i<nx-1; i++){
            for(int j = 2; j<nz-1; j++){
                dex = pts.size();
                //add center
                pts.add(new double[]{
                        (i-0.5)*segment_size - actual_w/2,
                        -actual_h/2,
                        (j-0.5)*segment_size - actual_z/2
                });
                //first
                int a_dex = front_starting_dex + (i-1)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,a_dex});
                int b_dex = front_starting_dex + (i-2)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,b_dex});
                int c_dex = front_starting_dex + (i-2)*(nz-2) + (j-1);
                connections.add(new int[]{dex,c_dex});
                int d_dex = front_starting_dex + (i-1)*(nz-2) + (j-1);
                connections.add(new int[]{dex,d_dex});

                triangles.add(new int[]{dex, b_dex, a_dex});
                triangles.add(new int[]{dex, c_dex, b_dex});
                triangles.add(new int[]{dex, d_dex, c_dex});
                triangles.add(new int[]{dex, a_dex, d_dex});

            }
        }

        //front face triangles merging to top/bottom sans corners.
        for(int i = 2; i<nx-1; i++){
            dex = pts.size();

            pts.add(new double[]{
                    (i-0.5)*segment_size - actual_w/2,
                    -actual_h/2,
                    (0.5)*segment_size - actual_z/2
            });

            int a_dex = front_starting_dex + (i-2)*(nz - 2);
            int b_dex = front_starting_dex + (i-1)*(nz - 2);
            int c_dex = bottom_starting_dex + i*ny;
            int d_dex = bottom_starting_dex + (i-1)*ny;

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


            dex = pts.size();

            pts.add(new double[]{
                    (i-0.5)*segment_size - actual_w/2,
                    -actual_h/2,
                    -(0.5)*segment_size + actual_z/2
            });

            a_dex = front_starting_dex + (i-1)*(nz - 2) + (nz-3);
            b_dex = front_starting_dex + (i-2)*(nz - 2) + (nz-3);
            c_dex = top_starting_dex + (i-1)*ny;
            d_dex = top_starting_dex + i*ny;

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});

        }

        //front face triangles merging to left and right sans corners.
        for(int j = 2; j<nz-1; j++){

            dex = pts.size();

            pts.add(new double[]{
                    0.5*segment_size - actual_w/2,
                    -actual_h/2,
                    (j - 0.5)*segment_size - actual_z/2
            });

            int a_dex = front_starting_dex + (j-1);
            int b_dex = front_starting_dex + (j-2);
            int c_dex = left_starting_dex + j - 2;
            int d_dex = left_starting_dex + j - 1;

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


            dex = pts.size();

            pts.add(new double[]{
                    (-0.5)*segment_size + actual_w/2,
                    -actual_h/2,
                    (j-0.5)*segment_size - actual_z/2
            });

            a_dex = front_starting_dex + (j-2) + (nz-2)*(nx-3);
            b_dex = front_starting_dex + (j-1) + (nz-2)*(nx-3);
            c_dex = right_starting_dex + j - 1;
            d_dex = right_starting_dex + j - 2;

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


        }

        //front triangles four corners.
        for(int j = 1; j<nz; j+=nz-2){

            dex = pts.size();

            pts.add(new double[]{
                    0.5*segment_size - actual_w/2,
                    -actual_h/2,
                    (j - 0.5)*segment_size - actual_z/2
            });



            int a_dex, b_dex, c_dex, d_dex;

            if(j==1){
                a_dex = front_starting_dex;
                b_dex = bottom_starting_dex + ny;
                c_dex = bottom_starting_dex;
                d_dex = left_starting_dex;
            } else{
                a_dex = front_starting_dex + nz-3;
                b_dex = left_starting_dex + nz-3;
                c_dex = top_starting_dex;
                d_dex = top_starting_dex + ny;
            }
            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


            dex = pts.size();

            pts.add(new double[]{
                    (-0.5)*segment_size + actual_w/2,
                    -actual_h/2,
                    (j-0.5)*segment_size - actual_z/2
            });


            if(j==1){
                a_dex = front_starting_dex + (nx-3)*(nz - 2);
                b_dex = right_starting_dex;
                c_dex = bottom_starting_dex + (nx-1)*(ny);
                d_dex = bottom_starting_dex + (nx-2)*(ny);
            } else{
                a_dex = front_starting_dex + (nx-2)*(nz - 2) -1;
                b_dex = top_starting_dex + (nx-2)*(ny);
                c_dex = top_starting_dex + (nx-1)*(ny);
                d_dex = right_starting_dex + nz-3;
            }

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


        }


        //back plane
        int back_starting_dex = pts.size();
        for(int i = 1; i<nx-1; i++){
            for(int j = 1; j<nz-1; j++){
                dex = back_starting_dex + (i-1)*(nz-2) + (j-1);
                pts.add(new double[]{
                        i*segment_size - actual_w/2,
                        actual_h/2,
                        j*segment_size - actual_z/2
                });

                if(i>1){
                    connections.add(new int[]{
                            dex,
                            dex - (nz-2)
                    });
                }

                if(j>1){
                    connections.add(new int[]{
                            dex,
                            dex - 1
                    });
                }


            }
        }


        //connect to top and bottom.
        for(int i = 1; i<nx-1; i++){
            connections.add(new int[]{
                    back_starting_dex + (i-1)*(nz - 2),
                    bottom_starting_dex + i*ny + ny-1
            });

            connections.add(new int[]{
                    back_starting_dex + (i-1)*(nz - 2) + (nz-3),
                    top_starting_dex + i*ny + ny-1
            });

        }


        //connect to left and right
        for(int j = 1; j<nz-1; j++){
            connections.add(new int[]{
                    back_starting_dex + (j-1),
                    left_starting_dex + j - 1 + (ny-1)*(nz-2)
            });

            connections.add(new int[]{
                    back_starting_dex + (j-1) + (nz-2)*(nx-3),
                    right_starting_dex + j - 1 + (ny-1)*(nz-2)
            });
        }

        //back face triangles
        for(int i = 2; i<nx-1; i++){
            for(int j = 2; j<nz-1; j++){
                dex = pts.size();
                //add center
                pts.add(new double[]{
                        (i-0.5)*segment_size - actual_w/2,
                        actual_h/2,
                        (j-0.5)*segment_size - actual_z/2
                });
                //first
                int a_dex = back_starting_dex + (i-2)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,a_dex});
                int b_dex = back_starting_dex + (i-1)*(nz-2) + (j-2) ;
                connections.add(new int[]{dex,b_dex});
                int c_dex = back_starting_dex + (i-1)*(nz-2) + (j-1);
                connections.add(new int[]{dex,c_dex});
                int d_dex = back_starting_dex + (i-2)*(nz-2) + (j-1);
                connections.add(new int[]{dex,d_dex});

                triangles.add(new int[]{dex, b_dex, a_dex});
                triangles.add(new int[]{dex, c_dex, b_dex});
                triangles.add(new int[]{dex, d_dex, c_dex});
                triangles.add(new int[]{dex, a_dex, d_dex});

            }
        }


        //back face triangles merging to top/bottom sans corners.
        for(int i = 2; i<nx-1; i++){
            dex = pts.size();

            pts.add(new double[]{
                    (i-0.5)*segment_size - actual_w/2,
                    actual_h/2,
                    (0.5)*segment_size - actual_z/2
            });

            int a_dex = back_starting_dex + (i-1)*(nz - 2);
            int b_dex = back_starting_dex + (i-2)*(nz - 2);
            int c_dex = bottom_starting_dex + (i-1)*ny + ny - 1;
            int d_dex = bottom_starting_dex + (i)*ny + ny - 1;

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


            dex = pts.size();

            pts.add(new double[]{
                    (i-0.5)*segment_size - actual_w/2,
                    actual_h/2,
                    -(0.5)*segment_size + actual_z/2
            });

            a_dex = back_starting_dex + (i-2)*(nz - 2) + (nz-3);
            b_dex = back_starting_dex + (i-1)*(nz - 2) + (nz-3);
            c_dex = top_starting_dex + (i)*ny + ny - 1;
            d_dex = top_starting_dex + (i-1)*ny + ny - 1;

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});

        }

        //back face triangles merging to left and right sans corners.
        for(int j = 2; j<nz-1; j++){

            dex = pts.size();

            pts.add(new double[]{
                    0.5*segment_size - actual_w/2,
                    actual_h/2,
                    (j - 0.5)*segment_size - actual_z/2
            });

            int a_dex = back_starting_dex + (j-2);
            int b_dex = back_starting_dex + (j-1);
            int c_dex = left_starting_dex + j - 1 + (ny-1)*(nz-2);
            int d_dex = left_starting_dex + j - 2 + (ny-1)*(nz-2);

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


            dex = pts.size();

            pts.add(new double[]{
                    (-0.5)*segment_size + actual_w/2,
                    actual_h/2,
                    (j-0.5)*segment_size - actual_z/2
            });

            a_dex = back_starting_dex + (j-1) + (nz-2)*(nx-3);
            b_dex = back_starting_dex + (j-2) + (nz-2)*(nx-3);
            c_dex = right_starting_dex + j - 2+ (ny-1)*(nz-2);
            d_dex = right_starting_dex + j - 1+ (ny-1)*(nz-2);

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


        }

        //back triangles four corners.
        for(int j = 1; j<nz; j+=nz-2){

            dex = pts.size();

            pts.add(new double[]{
                    0.5*segment_size - actual_w/2,
                    actual_h/2,
                    (j - 0.5)*segment_size - actual_z/2
            });



            int a_dex, b_dex, c_dex, d_dex;

            if(j==1){
                a_dex = back_starting_dex;
                b_dex = left_starting_dex + (ny-1)*(nz-2);
                c_dex = bottom_starting_dex + ny -1;
                d_dex = bottom_starting_dex + 2*ny - 1;
            } else{
                a_dex = back_starting_dex + nz-3;
                b_dex = top_starting_dex + 2*ny - 1;
                c_dex = top_starting_dex + ny - 1;
                d_dex = left_starting_dex + (ny)*(nz-2) - 1 ;
            }
            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


            dex = pts.size();

            pts.add(new double[]{
                    (-0.5)*segment_size + actual_w/2,
                    actual_h/2,
                    (j-0.5)*segment_size - actual_z/2
            });


            if(j==1){
                a_dex = back_starting_dex + (nx-3)*(nz - 2);
                b_dex = bottom_starting_dex + (nx-1)*(ny) - 1;
                c_dex = bottom_starting_dex + (nx)*(ny) - 1;
                d_dex = right_starting_dex + (nz-2)*(ny-1);
            } else{
                a_dex = back_starting_dex + (nx-2)*(nz - 2) -1;
                b_dex = right_starting_dex + (nz-2)*(ny-1) + nz - 3;
                c_dex = top_starting_dex + (nx)*(ny) -1;
                d_dex = top_starting_dex + (nx-1)*(ny) -1;
            }

            connections.add(new int[]{dex,a_dex});
            connections.add(new int[]{dex,b_dex});
            connections.add(new int[]{dex,c_dex});
            connections.add(new int[]{dex,d_dex});

            triangles.add(new int[]{dex, b_dex, a_dex});
            triangles.add(new int[]{dex, c_dex, b_dex});
            triangles.add(new int[]{dex, d_dex, c_dex});
            triangles.add(new int[]{dex, a_dex, d_dex});


        }

        final DeformableMesh3D mesh = new DeformableMesh3D(pts, connections, triangles);

        return mesh;
    }

    public static DeformableMesh3D createRhombicDodecahedron(double l){
        List<double[]> points = new ArrayList<>(14);
        List<int[]> connections = new ArrayList<>(36); // 24 connections + 1 new connection per face
        List<int[]> triangles = new ArrayList<>(24);  //12 faces cut into 2 triangles

        for(int i = 0; i<4; i++){
            double theta = Math.PI*2/4*i;
            double[] a = {l*Math.sin(theta - Math.PI/4), 0, l*Math.cos(theta - Math.PI/4)};
            double[] b = {Math.sqrt(2)/2*l*Math.sin(theta), -l/2, Math.sqrt(2)/2*l*Math.cos(theta)};
            double[] c = {Math.sqrt(2)/2*l*Math.sin(theta), l/2 ,Math.sqrt(2)/2*l*Math.cos(theta)};
            points.add(a);
            points.add(b);
            points.add(c);

            //t1
            connections.add(new int[]{i*3 + 0, i*3 +1});
            connections.add(new int[]{i*3 + 1, i*3 + 2});
            connections.add(new int[]{i*3 + 2, i*3 + 0});

            //t2 remaining
            connections.add(new int[]{i*3 + 1, (i*3 + 3)%12});
            connections.add(new int[]{(i*3 + 3)%12, i*3 + 2});

            //t3
            connections.add(new int[]{i*3+1, 12});
            connections.add(new int[]{(i*3+4)%12, i*3 + 1});

            //t5
            connections.add(new int[]{(i*3+2), 13});
            connections.add(new int[]{(i*3+2)%12, (i*3 + 5)%12});

            triangles.add(new int[]{i*3 + 0, i*3 + 1, i*3 + 2});
            triangles.add(new int[]{i*3 + 1, (i*3 + 3)%12, i*3 + 2});

            triangles.add(new int[]{i*3 + 1, 12, (i*3 + 4)%12});
            triangles.add(new int[]{ (i*3 + 1), (i*3 + 4)%12, (i*3 + 3)%12});

            triangles.add(new int[]{i*3 + 2, (i*3 + 5)%12, 13});
            triangles.add(new int[]{ (i*3 + 2), (i*3 + 3)%12, (i*3 + 5)%12});

        }
        double[] d = {0, -l, 0};
        points.add(d);
        double[] e = {0, l, 0};
        points.add(e);

        return new DeformableMesh3D(points, connections, triangles);


    }

    public static DeformableMesh3D createTestBlock(){
        ArrayList<double[]> pts = new ArrayList<double[]>();
        ArrayList<int[]> connections = new ArrayList<int[]>();
        ArrayList<int[]> triangles = new ArrayList<int[]>();

        pts.add(new double[]{-1, -1, 1});
        pts.add(new double[]{-1, 1, 1});
        pts.add(new double[]{1, 1, 1});
        pts.add(new double[]{1, -1, 1});

        pts.add(new double[]{-1, -1, -1});
        pts.add(new double[]{-1, 1, -1});
        pts.add(new double[]{1, 1, -1});
        pts.add(new double[]{1, -1, -1});

        connections.add(new int[]{0, 4});
        connections.add(new int[]{0, 1});
        connections.add(new int[]{1, 5});
        connections.add(new int[]{1, 2});
        connections.add(new int[]{2, 6});
        connections.add(new int[]{2, 3});
        connections.add(new int[]{3, 7});
        connections.add(new int[]{3, 0});
        connections.add(new int[]{4, 5});
        connections.add(new int[]{5, 6});
        connections.add(new int[]{6, 7});
        connections.add(new int[]{7, 4});

        triangles.add(new int[]{0, 2, 1});
        triangles.add(new int[]{0,3,2});

        triangles.add(new int[]{0, 1, 5});
        triangles.add(new int[]{0,5,4});

        triangles.add(new int[]{1,2,5});
        triangles.add(new int[]{5,2,6});

        triangles.add(new int[]{2,3,6});
        triangles.add(new int[]{6,3,7});

        triangles.add(new int[]{3,0,4});
        triangles.add(new int[]{3,4,7});

        triangles.add(new int[]{4,5,6});
        triangles.add(new int[]{4,6,7});

        final DeformableMesh3D mesh = new DeformableMesh3D(pts, connections, triangles);
        mesh.create3DObject();

        return mesh;
    }

    /**
     * normal *dot* direction + |position|*direction
     *
     * @param direction
     * @param positions
     * @param triangles
     * @return
     */
    static public double calculateVolume(double[] direction, double[] positions, List<Triangle3D> triangles ){
        double sum = 0;

        for(Triangle3D triangle: triangles){
            triangle.update();
            sum += triangle.area*(triangle.normal[0]*direction[0] + triangle.normal[1]*direction[1] + triangle.normal[2]*direction[2])*
                   (triangle.center[0]*direction[0] + triangle.center[1]*direction[1] + triangle.center[2]*direction[2]);
        }

        return sum/2.0;
    }

    public static double calculateAverageIntensity(MeshImageStack stack, List<Triangle3D> triangles, double thickness) {
        int steps = (int)(thickness/stack.pixel_dimensions[0]) + 1;
        double ds = steps>1?thickness/stack.SCALE/(steps-1):thickness/stack.SCALE;
        double max = 0.0;
        double sum = 0;
        for(Triangle3D tri: triangles){
            tri.update();
            double s = 0;
            double mx = 0;
            for(int i = 0; i<steps; i++){
                double px = stack.getInterpolatedValue(tri.center[0]-ds*i*tri.normal[0], tri.center[1]-ds*i*tri.normal[1], tri.center[2]-ds*i*tri.normal[2]);
                s += px;
                if(px>mx)mx = px;
            }

            double intensity = s/steps;
            sum += intensity;

        }


        return sum/triangles.size();
    }

    /**
     * Calculates the center based on the position of the nodes. Finds the mean distance from the center.
     *
     * Since the nodes are not evenly distributed this method
     * can be biased.
     * @param nodes Nodes of the current mesh.
     * @return {x, y, z, r}
     */
    public static double[] centerAndRadius(List<Node3D> nodes){
        double[] sum = new double[4];

        for(Node3D node: nodes){
            double[] r = node.getCoordinates();
            sum[0] += r[0];
            sum[1] += r[1];
            sum[2] += r[2];
        }

        double size = nodes.size();

        sum[0] = sum[0]/size;
        sum[1] = sum[1]/size;
        sum[2] = sum[2]/size;


        for(Node3D node: nodes){
            double[] r = node.getCoordinates();
            sum[3] += Math.sqrt(
                    Math.pow(r[0] - sum[0], 2) +
                    Math.pow(r[1] - sum[1], 2) +
                    Math.pow(r[2] - sum[2], 2)
            );
        }

        sum[3] = sum[3]/size;



        return sum;



    }

    public static double calculateSurfaceArea(DeformableMesh3D mesh){
        double area = 0;
        for(Triangle3D triangle: mesh.triangles){
            triangle.update();
            area += triangle.area;
        }

        return area;

    }

    public static double calculateIntensity(Box3D bound, MeshImageStack stack) {
        double sum = 0;
        double[] start = stack.getImageCoordinates(bound.low);
        double[] end = stack.getImageCoordinates(bound.high);

        start[0] = start[0]<0? 0 : start[0];
        start[1] = start[1]<0? 0 : start[1];
        start[2] = start[2]<0? 0 : start[2];

        end[0] = end[0] > stack.getWidthPx() ? stack.getWidthPx() : end[0];
        end[1] = end[1] > stack.getHeightPx() ? stack.getWidthPx() : end[1];
        end[2] = end[2] > stack.getNSlices() ? stack.getNSlices() : end[2];
        double count = 0;
        for(int i = (int)start[0]; i<end[0]; i++){
            for(int j = (int)start[1]; j<end[1]; j++){
                for(int k = (int)start[2]; k<end[2]; k++){

                    sum += stack.getValue(i,j,k);
                    count++;
                }
            }
        }



        return sum/count;
    }

    public static List<List<Node3D>> generateConnectionMap(DeformableMesh3D mesh){
        List<List<Node3D>> connectionMap = mesh.nodes.stream().map(n -> new ArrayList<Node3D>()).collect(Collectors.toList());

        for(Connection3D connection: mesh.getConnections()){
            connectionMap.get(connection.A.index).add(connection.B);
            connectionMap.get(connection.B.index).add(connection.A);
        }

        return connectionMap;
    }

    public static AStarBasic<Node3D> pathFinder(List<List<Node3D>> connectionMap, Node3D destination){
        List<Node3D> best = new ArrayList<>();
        Boundary<Node3D> boundary = (p)->true;
        BasicHeuristic<Node3D> heuristic = (a1)->{
            double[] p1 = destination.getCoordinates();
            double[] b1 = a1.getCoordinates();
            return Vector3DOps.distance(p1,b1);
        };
        BasicCost<Node3D> cost = (a1,b1)-> Vector3DOps.distance(a1.getCoordinates(), b1.getCoordinates());



        ChoiceGenerator<Node3D> generator = n->connectionMap.get(n.index);
        History<Node3D> history = new History<Node3D>(){
            Map<Node3D, Double> visited = new HashMap<>();
            @Override
            public double visited(Node3D node3D) {
                return visited.getOrDefault(node3D, 0.);
            }

            @Override
            public void visit(Node3D node3D, double d) {
                visited.put(node3D, d);
            }
        };

        AStarBasic<Node3D> basic = new AStarBasic<>(boundary, heuristic, cost, generator, history);

        return basic;
    }

    public static List<Node3D> findPath(List<List<Node3D>> connectionMap, Node3D origin, Node3D goal){

        AStarBasic<Node3D> basic = pathFinder(connectionMap, goal);
        basic.setGoal(goal);
        PossiblePath<Node3D> starting = new Node3DPath(origin);

        return basic.findPath(starting).getPath();
    }

    static public ImagePlus createBinaryRepresentation(MeshImageStack stack, ImagePlus original, Map<Integer, DeformableMesh3D> meshes){
        int w = original.getWidth();
        int h = original.getHeight();

        ImageStack stacked = new ImageStack(original.getWidth(), original.getHeight());
        List<Integer> keys = new ArrayList<>(meshes.keySet());
        keys.sort(Integer::compareTo);
        double[] xdirection = {1,0,0};
        double[] center = {0,0,0};
        for(Integer i: keys){
            ImageStack substacked = new ImageStack(original.getWidth(), original.getHeight());
            DeformableMesh3D mesh = meshes.get(i);
            int slices = original.getNSlices();
            for(int slice = 0; slice<slices; slice++){
                int[] pixels = new int[w*h];

                ImageProcessor proc = new ColorProcessor(w, h, pixels);
                substacked.addSlice(proc);
            }
            mosaicBinary(stack, substacked, mesh, 255);

            for(int slice = 1; slice<=slices; slice++){
                stacked.addSlice(substacked.getProcessor(slice).convertToByteProcessor());
            }

        }


        ImagePlus ret = original.createImagePlus();
        ret.setStack(stacked);
        ret.setTitle("bined-" + original.getTitle());
        ret.setDimensions(original.getNChannels(), original.getNSlices(), original.getNFrames());
        int dims = 0;
        if(original.getNChannels()>1) dims++;
        if(original.getNSlices()>1) dims++;
        if(original.getNFrames()>1) dims++;

        return ret;
    }

    static public ImagePlus createBinaryRepresentation(MeshImageStack stack, DeformableMesh3D mesh){
        int w = stack.getWidthPx();
        int h = stack.getHeightPx();

        ImageStack binStack = new ImageStack(w, h);
        ImageStack colorStack = new ImageStack(w, h);
        double[] xdirection = {1,0,0};
        double[] center = {0,0,0};
        InterceptingMesh3D picker = new InterceptingMesh3D(mesh);
        int slices = stack.getNSlices();
        for(int slice = 0; slice<slices; slice++){
            int[] pixels = new int[w*h];
            ImageProcessor proc = new ColorProcessor(w, h, pixels);
            colorStack.addSlice(proc);
        }

        for(int slice = 0; slice<slices; slice++){

            binStack.addSlice(colorStack.getProcessor(slice+1).convertToByteProcessor());
        }
        mosaicBinary(stack, binStack, mesh, 255);


        ImagePlus ret = new ImagePlus();
        ret.setStack(binStack);
        ret.setDimensions(1, stack.getNSlices(), 1);
        Calibration cal = ret.getCalibration();
        cal.pixelWidth = stack.pixel_dimensions[0];
        cal.pixelHeight = stack.pixel_dimensions[1];
        cal.pixelDepth = stack.pixel_dimensions[2];

        return ret;
    }

    public static ImagePlus createMosaicRepresentation(MeshImageStack stack, ImagePlus original_plus, List<Track> allMeshTracks) {

        ImagePlus plus = original_plus.createImagePlus();

        Set<Integer> frames = new TreeSet<>();

        for(Track t: allMeshTracks){
            for(Integer i = 0; i<=stack.FRAMES; i++){

                if(t.containsKey(i)){
                    frames.add(i);
                }
            }
        }


        int w = original_plus.getWidth();
        int h = original_plus.getHeight();
        int n = original_plus.getNSlices();

        ImageStack timeStack = new ImageStack(w, h);

        for(Integer i: frames){
            ImageStack out = new ImageStack(w, h);
            for(int j = 0; j<n; j++){
                out.addSlice(new ColorProcessor(w, h));
            }
            for(Track t: allMeshTracks){
                if(t.containsKey(i)){
                    mosaicBinary(stack, out, t.getMesh(i), t.getColor().getRGB());
                }
            }

            for(int j = 1; j<= n; j++){

                timeStack.addSlice(out.getProcessor(j));
            }
        }
        plus.setStack(timeStack);
        plus.setDimensions(1, n, frames.size());
        return plus;
    }

    static void mosaicBinary(MeshImageStack stack, ImageStack out, DeformableMesh3D mesh, int rgb){
        InterceptingMesh3D picker = new InterceptingMesh3D(mesh);
        picker = new InterceptingMesh3D(RayCastMesh.rayCastMesh(picker, picker.getCenter(), 4));
        double[] xdirection = {1,0,0};

        int slices = out.getSize();
        int w = out.getWidth();
        int h = out.getHeight();
        double center[] = new double[3];
        for(int slice = 0; slice<slices; slice++){
            int[] pixels = (int[])(out.getProcessor(slice+1).getPixels());
            center[2] = slice;
            for(int j = 0; j<h; j++){
                int offset = j*w;
                center[1] = j;
                List<Intersection> sections = picker.getIntersections(stack.getNormalizedCoordinate(center), xdirection);
                sections.sort((a,b)->Double.compare(a.location[0], b.location[0]));

                boolean inside = false;
                double count = 0;
                double[] boundaries = new double[sections.size()+1];
                int valid = 0;
                boolean startInside = false;
                for(int k = 0; k<sections.size(); k++){

                    double bound = stack.getImageCoordinates(sections.get(k).location)[0];
                    boolean facingLeft = sections.get(k).surfaceNormal[0]<0;
                    boolean facingRight = !facingLeft;
                    boolean outside = !inside;
                    if(bound>0){
                        //check if it is actually a boundary

                        if(inside&&facingRight){
                            //going outside
                            inside = false;
                            boundaries[valid] = bound;
                            valid++;
                        } else if(outside&facingLeft){
                            //coming back in.
                            inside = true;
                            boundaries[valid] = bound;
                            valid++;
                        }

                    } else{
                        //check if entering or exiting.
                        if(inside && facingRight){
                            //entering.
                            inside = false;

                        } else if(outside&&facingLeft){
                            //exiting
                            inside = true;
                        }
                        startInside = inside;
                    }
                }
                inside = startInside;
                boundaries[valid] = w;

                int current = 0;

                for(int p = 0; p<w; p++){
                    if(p>boundaries[current]){
                        //switch.
                        current++;
                        inside = !inside;
                    }
                    if(inside){
                        pixels[p + offset] = rgb;
                    }
                }
            }

        }
    }

    public static DeformableMesh3D copyOf(DeformableMesh3D mesh) {
        double[] pos = Arrays.copyOf(mesh.positions, mesh.positions.length);
        int[] con = Arrays.copyOf(mesh.connection_index, mesh.connection_index.length);
        int[] tri = Arrays.copyOf(mesh.triangle_index, mesh.triangle_index.length);
        return new DeformableMesh3D(pos, con, tri);
    }

    /**
     * Finds the closest and furthest away nodes from the centroid.
     *
     * @param nodes
     * @param centroid
     * @return {min, max}
     */
    public static double[] findMinMax(List<Node3D> nodes, double[] centroid) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for(Node3D node: nodes){

            double[] r = node.getCoordinates();
            double distance = Math.sqrt(
                    Math.pow(r[0] - centroid[0], 2) +
                            Math.pow(r[1] - centroid[1], 2) +
                            Math.pow(r[2] - centroid[2], 2)
            );
            min = distance<min?distance:min;
            max = distance>max?distance:max;
        }
        return new double[]{min, max};

    }

    public static DeformableMesh3D mergeMeshes(List<DeformableMesh3D> meshes){
        int posCount = 0;
        int triCount = 0;
        int conCount = 0;
        for(DeformableMesh3D mesh: meshes){
            posCount += mesh.positions.length;
            triCount += mesh.triangle_index.length;
            conCount += mesh.connection_index.length;
        }
        double[] points = new double[posCount];
        int[] triangles = new int[triCount];
        int[] connections = new int[conCount];

        int offset = 0;
        int toff = 0;
        int coff = 0;
        for(DeformableMesh3D mesh: meshes){
            System.arraycopy(mesh.positions, 0, points, offset, mesh.positions.length);
            for(int i = 0; i<mesh.triangle_index.length; i++){
                triangles[i + toff] = mesh.triangle_index[i] + offset/3;
            }
            for(int i = 0; i<mesh.connection_index.length; i++){
                connections[i+coff] = mesh.connection_index[i] + offset/3;
            }


            offset += mesh.positions.length;
            toff += mesh.triangle_index.length;
            coff += mesh.connection_index.length;
        }

        return new DeformableMesh3D(points, connections, triangles);
    }
}

class Node3DPath implements PossiblePath<Node3D>{
    double distance;
    double heuristic;

    List<Node3D> path =  new ArrayList<>();
    public Node3DPath(Node3D start){
        path.add(start);
    }

    public Node3DPath(List<Node3D> path){
        path.forEach(this.path::add);
    }
    @Override
        public Node3D getEndPoint() {
        return path.get(path.size()-1);
    }


    @Override
    public void addPoint(Node3D node3D, double cost, double heuristic) {
        path.add(node3D);
        distance += cost;
        this.heuristic = heuristic;
    }

    @Override
    public double getDistance() {
        return distance;
    }

    @Override
    public double getHeuristic() {
        return heuristic;
    }

    @Override
    public List<Node3D> getPath() {
        return new ArrayList<>(path);
    }

    @Override
    public PossiblePath<Node3D> duplicate() {

        return new Node3DPath(path);
    }

    @Override
    public boolean sameDestination(Node3D destination) {
        Node3D n = getEndPoint();
        return n.index==destination.index;
    }

}