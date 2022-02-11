package deformablemesh.io;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.BinaryMomentsOfInertia;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.track.Track;
import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.action.ExportTracksToXML;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackMateAdapter {
    public static List<Track> importTrackMateFile(Path image, Path trackMateFile) {
        List<Track> tracks = new ArrayList<>();
        MeshImageStack mis = new MeshImageStack(image);
        TmXmlReader reader = new TmXmlReader(trackMateFile.toFile());
        Model model = reader.getModel();
        TrackModel trackModel = model.getTrackModel();
        double f = 1 / mis.SCALE;
        Map<Integer, Track> mapper = new HashMap<>();
        trackModel.nTracks(false);
        SpotCollection spots = model.getSpots();
        for (int i = 0; i < mis.getNFrames(); i++) {

            for (Spot spot : spots.iterable(i, false)) {
                double x = spot.getDoublePosition(0) * f;
                double y = spot.getDoublePosition(1) * f;
                double z = spot.getDoublePosition(2) * f;
                Integer id = trackModel.trackIDOf(spot);
                for(Map.Entry<String, Double> feature : spot.getFeatures().entrySet()){
                    System.out.println(feature.getKey() + ", " + feature.getValue());
                }
                Track track = mapper.computeIfAbsent(trackModel.trackIDOf(spot), j -> new Track("" + j));
                if (track.containsKey(i)) {
                    System.out.println("Track has multiple spots");
                } else {
                    double radius = spot.getFeature("RADIUS");
                    Sphere s = new Sphere(new double[]{x, y, z}, radius*f);
                    DeformableMesh3D mesh = RayCastMesh.rayCastMesh(s, s.getCenter(), 2);
                    track.addMesh(i, mesh);

                }
                System.out.println(trackModel.trackIDOf(spot));

            }
        }
        tracks.addAll(mapper.values());
        return tracks;
    }
    public static void saveAsTrackMateFile(MeshImageStack stack, List<Track> tracks, Path destination) throws IOException {
        Model trackMateModel = new Model();
        trackMateModel.beginUpdate();
        double quality = 1.0;
        double weight = 1.0;
        for(Track t: tracks){
            Spot last = null;
            for(Map.Entry<Integer, DeformableMesh3D> entry: t.getTrack().entrySet()){
                DeformableMesh3D mesh = entry.getValue();
                BinaryMomentsOfInertia moments = new BinaryMomentsOfInertia(mesh, stack);
                double[] center = moments.getCenterOfMass();
                double[] pxNSlices = stack.getImageCoordinates(center);


                double x = center[0]*stack.SCALE;
                double y = center[1]*stack.SCALE;
                double z = center[2]*stack.SCALE;

                double radius = Math.cbrt(3*moments.volume()/4/Math.PI)*stack.SCALE;

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
        Path img = Paths.get(args[0]);
        Path xml = Paths.get("texting.xml");
        List<Track> tracks = importTrackMateFile(img, xml);
        MeshWriter.saveMeshes(new File("texting.bmf"), tracks);
        //MeshImageStack mis = new MeshImageStack( Paths.get(args[0]) );
        //List<Track> tracks = MeshWriter.loadMeshes(new File(args[1]));
        //saveAsTrackMateFile(mis, tracks, Paths.get("texting.xml"));
    }
}
