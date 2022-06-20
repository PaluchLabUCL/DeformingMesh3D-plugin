package deformablemesh.track;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.util.ColorSuggestions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * For keeping track of mesh linking. A mesh only exists in one frame, but it can be linked to meshes in other frames.
 * A track is a mesh that persists in multiple frames.
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

    /**
     * All of the current tracks.
     *
     * @return a new list containing of the current tracks.
     */
    public List<Track> getAllMeshTracks(){
        return new ArrayList<>(tracks);
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


    /**
     * Returns n tracks with a name that is not contained in the current set of tracks names.
     * Probably the same color.
     *
     * int n number of tracks to be created.
     *
     * @return list with n tracks.
     */
    public List<Track> prepareEmptyTracks(int n){

        Set<String> names = tracks.stream().map(Track::getName).collect(Collectors.toCollection(HashSet::new));
        List<Color> colors = tracks.stream().map(Track::getColor).collect(Collectors.toCollection(ArrayList::new));
        List<Track> fresh = new ArrayList<>();
        while(fresh.size() < n){
            Track track = new Track(colors);
            String name = track.name;
            int id = 0;

            while(names.contains(name)){
                id++;
                name = track.name + "-" + id;
            }
            track.setName(name);
            colors.add(track.color);
            names.add(track.name);
            fresh.add(track);
        }
        return fresh;
    }

    public Track prepareEmptyTrack(){
        return prepareEmptyTracks(1).get(0);
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
        selectedTrack = prepareEmptyTrack();

        selectedTrack.setSelected(true);
        tracks.add(selectedTrack);
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

        int i = tracks.indexOf(track);
        if(i == -1){
            //doesn't contain track.
            i = 0;
        } else {
            tracks.remove(i);
        }
        if(selectedTrack==track) {
            selectedTrack.setSelected(false);
            //if the selected track is the 0th track or not contained.
            int next = i - 1;

            if(next < 0 ){
                next = 0;
            }

            if(track.size() <= next){
                next = tracks.size() - 1;
            }

            if (tracks.size() > 0) {
                selectedTrack = tracks.get(next);
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

    public void selectPreviousTrack() {
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
            i = (i-1);
            i = i < 0? tracks.size() - 1 : i;
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


    public void selectMeshTrack(Track track) {
        if(selectedTrack!=null){
            selectedTrack.setSelected(false);
        }
        if(tracks.contains(track)){
            selectedTrack = track;
            track.setSelected(true);
        } else{
            selectedTrack = null;
        }
    }


}
