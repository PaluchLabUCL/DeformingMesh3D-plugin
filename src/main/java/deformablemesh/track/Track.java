package deformablemesh.track;

import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.util.ColorSuggestions;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by msmith on 3/30/16.
 */
public class Track {
    String name;
    Color color;
    boolean showSurface = false;
    private Map<Integer, DeformableMesh3D> track = new TreeMap<>();
    private boolean selected;

    public Track(List<Color> usedColors){
        this.color = ColorSuggestions.getSuggestion(usedColors);
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

    public void addMesh(Integer i, DeformableMesh3D mesh){
        mesh.setColor(color);
        mesh.setShowSurface(showSurface);
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
    public Integer getFirstFrame(){
        Integer min = Integer.MAX_VALUE;
        for(Integer i: track.keySet()){
            if(i<min){
                min = i;
            }
        }
        return min;
    }

    public Integer getLastFrame(){
        Integer max = -1;
        for(Integer i: track.keySet()){
            if(i>max){
                max = i;
            }
        }
        return max;
    }

    public int getFrame(DeformableMesh3D mesh){
        for(Integer i: track.keySet()){
            if(track.get(i)==mesh){
                return i;
            };
        }
        return -1;
    }

    public int size(){
        return track.size();
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
        if (!color.equals(c)) {
            for(DeformableMesh3D mesh: track.values()){
                mesh.setColor(c);
            }
        }
        color = c;
    }

    public void setSelected(boolean selected) {
        for(DeformableMesh3D mesh: track.values()){
            mesh.setSelected(selected);
        }

        this.selected = selected;
    }

    public void setShowSurface(boolean show){
        showSurface = show;
        for(DeformableMesh3D mesh: track.values()){
            mesh.setShowSurface(show);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean getShowSurface() {
        return showSurface;
    }
}
