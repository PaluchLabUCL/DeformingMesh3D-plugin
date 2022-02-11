package deformablemesh.io;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.BinaryMomentsOfInertia;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.track.Track;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.ExportTracksToXML;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class TrackMateAdapter {

    public static void saveAsTrackMateFile(MeshImageStack stack, List<Track> tracks, Path destination) throws IOException {
        Model trackMateModel = new Model();
        // Set you physical units here.
        String spaceUnits = "spaceUnits";
		String timeUnits = "timeUnits";
		trackMateModel.setPhysicalUnits( spaceUnits, timeUnits );
        trackMateModel.beginUpdate();
        double quality = 1.0;
        double weight = 1.0;
        for(Track t: tracks){
            Spot last = null;
            for(Map.Entry<Integer, DeformableMesh3D> entry: t.getTrack().entrySet()){
                DeformableMesh3D mesh = entry.getValue();
                BinaryMomentsOfInertia moments = new BinaryMomentsOfInertia(mesh, stack);
                double[] center = moments.getCenterOfMass();

                double x = center[0];
                double y = center[1];
                double z = center[2];

                double radius = Math.cbrt(3*moments.volume()/4/Math.PI);

                Spot s = new Spot(x, y, z, radius, quality);
                trackMateModel.addSpotTo(s, entry.getKey());
                if(last != null){
                    trackMateModel.addEdge(s, last, radius);
                }
                last = s;
            }

        }

        trackMateModel.endUpdate();

        Settings s = new Settings(stack.getOriginalPlus());
        TmXmlWriter writer = new TmXmlWriter(destination.toFile());
        writer.appendSettings(s);
        writer.appendModel(trackMateModel);
        writer.writeToFile();

        //ExportTracksToXML.export(trackMateModel, s, destination.toFile());
    }

    public static void main(String[] args) throws IOException {

        MeshImageStack mis = new MeshImageStack( Paths.get(args[0]) );
        List<Track> tracks = MeshWriter.loadMeshes(new File(args[1]));
        saveAsTrackMateFile(mis, tracks, Paths.get("texting.xml"));
    }
}
