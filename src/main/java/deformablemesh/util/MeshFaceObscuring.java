package deformablemesh.util;

import deformablemesh.DeformableMesh3DTools;
import deformablemesh.MeshImageStack;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.InterceptingMesh3D;
import deformablemesh.geometry.Intersection;
import deformablemesh.geometry.Triangle3D;
import deformablemesh.track.Track;
import snakeprogram.util.TextWindow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class for detecting how much of a mesh is obscured by other meshes.
 *
 */
public class MeshFaceObscuring {
    List<InterceptingMesh3D> meshes;
    double cutoff = 1.0;

    public void setNeighbors(List<DeformableMesh3D> meshes){
        this.meshes = meshes.stream().map(InterceptingMesh3D::new).collect(Collectors.toList());
    }
    public void setNeighbor(DeformableMesh3D mesh){
        this.meshes = new ArrayList<>(1);
        meshes.add(new InterceptingMesh3D(mesh));
    }

    public Set<Triangle3D> getOverlapArea(DeformableMesh3D mesh){
        Set<Triangle3D> touching = new HashSet<>();

        for(InterceptingMesh3D neigh: meshes){
            for(Triangle3D t: mesh.triangles){
                t.update();
                if(naiveCheck(t, neigh)){
                    touching.add(t);
                }


            }
        }

        return touching ;
    }

    /**
     *
     * @param t
     * @param mesh
     * @return
     */
    public boolean naiveCheck(Triangle3D t, InterceptingMesh3D mesh){
        if(mesh.contains(t.center)){
            return true;
        }
        List<Intersection> sections = mesh.getIntersections(t.center, t.normal);
        for(Intersection section: sections){
            double dot = Vector3DOps.dot(t.normal, Vector3DOps.difference(section.location, t.center));
            if(dot>0 && dot<cutoff){
                return true;
            }
        }
        return false;
    }

    static public void analyzeTracks(List<Track> tracks, MeshImageStack stack, int frame, double cutoff){
        List<DeformableMesh3D> meshes = tracks.stream().filter(
                                                t->t.containsKey(frame)
                                            ).map(
                                                t->t.getMesh(frame)
                                            ).collect(Collectors.toList());

        List<String> colorNames = tracks.stream().map(t->t.getName()).collect(Collectors.toList());
        double factor = stack.SCALE*stack.SCALE;
        StringBuilder build = new StringBuilder("#Overlap data v0.1\n");
        build.append("#color-name\ttotal-area(unit^2)\toverlap-area(unit^2)\n");
        for(int i = 0; i<meshes.size();i++){

            MeshFaceObscuring finder =  new MeshFaceObscuring();
            finder.cutoff = cutoff;
            List<DeformableMesh3D> minusOne = new ArrayList<>(meshes);

            DeformableMesh3D mesh = meshes.get(i);
            minusOne.remove(mesh);
            finder.setNeighbors(minusOne);
            Set<Triangle3D> triangles = finder.getOverlapArea(mesh);
            double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);
            double covered = 0;
            for(Triangle3D t: triangles){
                covered += t.area;
            }
            build.append(String.format("%s\t%s\t%s\n", colorNames.get(i), Double.toString(area*factor), Double.toString(covered*factor)));


        }

        TextWindow window = new TextWindow("covered meshes", build.toString());
        window.display();
    }

    static public List<double[]> analyzeTracks(Track target, List<Track> others, double cutoff){
        List<double[]> overlaps = new ArrayList<>();


        for(Integer frame: target.getTrack().keySet()){
            MeshFaceObscuring finder =  new MeshFaceObscuring();
            finder.cutoff = cutoff;
            //1 over lap  per track plus frame plus total area.
            double[] values = new double[others.size() + 2];
            overlaps.add(values);
            DeformableMesh3D mesh = target.getMesh(frame);
            double area = DeformableMesh3DTools.calculateSurfaceArea(mesh);
            values[0] = frame;
            values[1] = area;

            int dex = 0;
            for(Track ot: others){
                double ol = 0;
                if(ot.containsKey(frame)) {
                    finder.setNeighbor(ot.getMesh(frame));
                    Set<Triangle3D> triangles = finder.getOverlapArea(mesh);
                    for(Triangle3D t: triangles){
                        ol += t.area;
                    }
                }
                values[2 + dex] = ol;
                dex++;
            }

        }


        return overlaps;
    }


}
