package deformablemesh.meshview;

import deformablemesh.externalenergies.ExternalEnergy;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.Node3D;
import deformablemesh.util.Vector3DOps;
import org.scijava.java3d.BranchGroup;
import snakeprogram3d.display3d.DataObject;

import java.util.ArrayList;
import java.util.List;

public class VectorField implements DataObject {
    DeformableMesh3D mesh;
    List<Arrow> vectors = new ArrayList<>();

    double scale = 0.1;
    BranchGroup group;
    private interface EnergyProvider{
        List<ExternalEnergy> getEnergies();
    }

    EnergyProvider provider;

    public VectorField(DeformableMesh3D mesh){
        this.mesh = mesh;
        provider = mesh::getExternalEnergies;
    }

    public VectorField(DeformableMesh3D mesh, List<ExternalEnergy> energies){
        this.mesh = mesh;
        final List<ExternalEnergy> def = new ArrayList<>(energies);
        provider = ()->def;
    }
    public void initialize(){
        int n = mesh.nodes.size();
        if(mesh!=null){
            List<ExternalEnergy> energies = provider.getEnergies();
            final double[] fx = new double[n];
            final double[] fy = new double[n];
            final double[] fz = new double[n];



            for(ExternalEnergy external: energies) {
                external.updateForces(mesh.positions, fx, fy, fz);
            }

            for(int i = 0; i<n; i++){
                Node3D node = mesh.nodes.get(i);
                double[] pt = node.getCoordinates();
                double[] f = {fx[i], fy[i], fz[i]};
                double m = Vector3DOps.normalize(f);
                if(m<1e-4){
                    f[0] = 1;
                    f[1] = 0;
                    f[2] = 0;
                    m = 1e-4;
                }
                Arrow a = new Arrow(1, 0.0625);
                a.pointAlong(f);
                m = m*scale;
                a.moveTo(pt[0] + f[0]*m*0.5, pt[1] + f[1]*m*0.5, pt[2] + f[2]*m*0.5);
                a.setScale(m);
                vectors.add(a);
            }

        }

    }
    public void setGamma(double g){
        scale = 100/g;
    }

    public List<Arrow> getVectors(){
        return new ArrayList<>(vectors);
    }
    public void update(){

        int n = mesh.nodes.size();
        if(mesh!=null){
            List<ExternalEnergy> energies = provider.getEnergies();
            final double[] fx = new double[n];
            final double[] fy = new double[n];
            final double[] fz = new double[n];



            for(ExternalEnergy external: energies) {
                external.updateForces(mesh.positions, fx, fy, fz);
            }

            for(int i = 0; i<n; i++){
                Node3D node = mesh.nodes.get(i);
                double[] pt = node.getCoordinates();
                double[] f = {fx[i], fy[i], fz[i]};
                double m = Vector3DOps.normalize(f);
                if(m<1e-4){
                    continue;
                }
                m = m*scale;
                Arrow a = vectors.get(i);
                a.moveTo(pt[0] + f[0]*m*0.5, pt[1] + f[1]*m*0.5, pt[2] + f[2]*m*0.5);
                a.pointAlong(f);
                a.setScale(m);
            }

        }
    }

    @Override
    public BranchGroup getBranchGroup() {
        if(group==null){
            group = new BranchGroup();
            group.setCapability(BranchGroup.ALLOW_DETACH);
            vectors.forEach(v -> group.addChild(v.getBranchGroup()));
        }
        return group;
    }
}
