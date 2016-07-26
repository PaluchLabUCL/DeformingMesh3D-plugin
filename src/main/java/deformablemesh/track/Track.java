package deformablemesh.track;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.util.ColorSuggestions;

import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by msmith on 3/30/16.
 */
public class Track {
    String name;
    Color color;

    private Map<Integer, DeformableMesh3D> track = new TreeMap<>();
    private boolean selected;

    public Track(){
        this.color = ColorSuggestions.getSuggestion();
        this.name = ColorSuggestions.getColorName(this.color);
    }

    public Track(String name, Color c){
        this.name = name;
        this.color = c;
    }

    public Track(String name) {
        this.name = name;
        this.color = ColorSuggestions.fromNameOrSuggestion(name);
    }

    void addMesh(Integer i, DeformableMesh3D mesh){
        mesh.setColor(color);
        track.put(i, mesh);
        mesh.setSelected(selected);
    }

    public boolean containsKey(Integer i){
        return track.containsKey(i);
    }

    void removeMesh(int i, DeformableMesh3D mesh){
        track.remove(i, mesh);
    }

    /**
     * Legacy method that returns a duplicate map that represents the time vs. track.
     * @return TreeMap
     */
    public Map<Integer, DeformableMesh3D> getTrack(){
        return new TreeMap<>(track);
    }

    public DeformableMesh3D getMesh(Integer i){
        return track.get(i);
    }

    public boolean isEmpty(){
        return track.isEmpty();
    }

    public void remove(DeformableMesh3D mesh) {
        Integer found = -1;
        for(Integer i: track.keySet()){
            if(track.get(i)==mesh){
                found = i;
                break;
            }
        }
        if(found.compareTo(0)>=0){
            track.remove(found);
        }
    }

    public boolean containsMesh(DeformableMesh3D mesh) {
        return track.values().contains(mesh);
    }


    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    /**
     * For use when reading data from a file.
     *
     * @param data
     */
    public void setData(Map<Integer, DeformableMesh3D> data){
        track = data;
        for(DeformableMesh3D mesh: track.values()){
            mesh.setSelected(selected);
            mesh.setColor(color);
        }
    }

    public Color getColor() {
        return color;
    }
    public void setColor(Color c){
        color = c;
    }

    public void setSelected(boolean selected) {
        for(DeformableMesh3D mesh: track.values()){
            mesh.setSelected(selected);
        }

        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }
}
