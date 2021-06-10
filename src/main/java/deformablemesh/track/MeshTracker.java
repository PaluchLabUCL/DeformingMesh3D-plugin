package deformablemesh.track;

import deformablemesh.geometry.DeformableMesh3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * For keeping track of mesh linking. A mesh only exists in one frame, but it can be linked to meshes in other frames.
 * A track would be a mesh that persists in multiple frames.
 *
 * Created by msmith on 3/22/16.
 */
public class MeshTracker {

    List<Track> tracks = new ArrayList<>();
    volatile Track selectedTrack;

    public DeformableMesh3D getSelectedMesh(int frame){
        if(selectedTrack!=null){
            return selectedTrack.getMesh(frame);
        }
        return null;
    }

    public List<DeformableMesh3D> getCurrent(int frame){
        return tracks.stream().filter(
                map->map.containsKey(frame)
        ).map(
                map->map.getMesh(frame)
        ).collect(Collectors.toList());
    }

    public Track getSelectedTrack() {
        return selectedTrack;
    }

    public void addMeshTracks(List<Track> meshes) {
        tracks.addAll(meshes);
    }

    public void clearMeshes() {
        tracks.clear();
       if(selectedTrack!=null){
            selectedTrack.setSelected(false);
            selectedTrack = null;
        }

    }

    public List<Track> getAllMeshTracks(){
        return Collections.unmodifiableList(tracks);
    }

    public boolean hasSelectedTrack() {

        return selectedTrack!=null;

    }

    /**
     * Adds a mesh to the currently selected. If no tracks are selected, a new track is created and set as selected.
     *
     * @param frame
     * @param replacementMesh
     */
    public void addMesh(int frame, DeformableMesh3D replacementMesh) {

        if(selectedTrack==null){
            selectedTrack = new Track(tracks.stream().map(Track::getColor).collect(Collectors.toList()));
            selectedTrack.setSelected(true);
            tracks.add(selectedTrack);
        }

        selectedTrack.addMesh(frame, replacementMesh);

    }

    public Track createTrack(){
        Track track = new Track(tracks.stream().map(Track::getColor).collect(Collectors.toList()));
        tracks.add(track);
        return track;
    }

    /**
     * Creates a new track, adds the provided mesh, adds it to the tracked tracks,
     * and selects it.
     *
     * @param frame
     * @param freshMesh
     */
    public Track createNewMeshTrack(int frame, DeformableMesh3D freshMesh) {
        if(selectedTrack!=null){
            selectedTrack.setSelected(false);
        }
        System.out.println("getting color: ");
        selectedTrack = new Track(tracks.stream().map(Track::getColor).collect(Collectors.toList()));

        System.out.println("selecting");
        selectedTrack.setSelected(true);

        System.out.println("adding track");
        tracks.add(selectedTrack);
        System.out.println("adding mesh");
        selectedTrack.addMesh(frame, freshMesh);
        return selectedTrack;

    }

    public void removeMeshFromTrack(int frame, DeformableMesh3D mesh, Track track) {
        track.removeMesh(frame, mesh);
        if(track.isEmpty()){
            removeTrack(track);
        }
    }

    public void removeTrack(Track track){
        tracks.remove(track);
        if(selectedTrack==track) {
            selectedTrack.setSelected(false);
            if (tracks.size() > 0) {
                selectedTrack = tracks.get(0);
                selectedTrack.setSelected(true);
            } else{
                selectedTrack=null;
            }
        }
    }

    public void addTrack(Track track){
        tracks.add(track);
    }

    public void addMeshToTrack(int f, DeformableMesh3D mesh, Track track) {
        track.addMesh(f, mesh);
        if(!tracks.contains(track)){
            tracks.add(track);
        }
        if(selectedTrack==null){
            selectedTrack=track;
            selectedTrack.setSelected(true);
        }
    }

    public void selectNextTrack() {
        if(tracks.size()==0){
            if(selectedTrack!=null){
                selectedTrack.setSelected(false);
            }
            selectedTrack = null; //just in  case.
            return; //no tracks to be selected!
        }

        if(selectedTrack==null){
            selectedTrack = tracks.get(0);
        } else{
            selectedTrack.setSelected(false);
            int i = tracks.indexOf(selectedTrack);
            i = (i+1)%tracks.size();
            selectedTrack = tracks.get(i);
        }
        selectedTrack.setSelected(true);

    }

    /**
     * changes the selected track to the track containing the mesh.
     *
     * @param mesh
     */
    public void selectTrackContainingMesh(DeformableMesh3D mesh){
        if(selectedTrack!=null){
            selectedTrack.setSelected(false);
        }
        for(Track track: tracks){
            if(track.containsMesh(mesh)){
                selectedTrack = track;
                selectedTrack.setSelected(true);
                return;
            }
        }
    }


}
