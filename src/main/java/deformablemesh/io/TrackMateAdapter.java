package deformablemesh.io;

import deformablemesh.MeshImageStack;
import deformablemesh.geometry.BinaryMomentsOfInertia;
import deformablemesh.geometry.Box3D;
import deformablemesh.geometry.DeformableMesh3D;
import deformablemesh.geometry.RayCastMesh;
import deformablemesh.geometry.Sphere;
import deformablemesh.track.Track;
import deformablemesh.util.Vector3DOps;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlWriter;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A utility class for using TrackMate files.
 *
 * @Author
 */
public class TrackMateAdapter {
    /**
     * For mapping a mesh from normalized coordinates to "real unit" coordinates.
     *
     */
    static class ProxySpot{

        final double cx, cy, cz;
        final double radius;

        ProxySpot(DeformableMesh3D mesh, MeshImageStack mis){
            Box3D box = mesh.getBoundingBox();
            double[] center = mis.getImageCoordinates(box.getCenter());
            //gets the image in pixels needs to be scaled to real units.

            cx = center[0] * mis.pixel_dimensions[0];
            cy = center[1] * mis.pixel_dimensions[1];
            cz = center[2] * mis.pixel_dimensions[2];
            //volume is going to be a normalized weight.
            radius = Math.cbrt(3* box.getVolume()/2/4/Math.PI)*mis.SCALE;
        }

        static double[] getNormalizedCoordinates(Spot spot, MeshImageStack mis) {
            double f = 1. / mis.SCALE;
            return new double[] {
                    spot.getDoublePosition(0) * f - mis.offsets[0],
                    spot.getDoublePosition(1) * f - mis.offsets[1],
                    spot.getDoublePosition(2) * f - mis.offsets[2]
            };
        }

        public static double getNormalizedRadius(Spot spot, MeshImageStack mis) {
            return spot.getFeature("RADIUS")/mis.SCALE;
        }
    }

    /**
     * Takes the provided Tracks for the provided image file and attempts to link them based on associated positions.
     *
     * First all of the spots are loaded from the trackmate file, each spot is associated with a mesh based on the
     * center of mass positions. If a mesh is associated to a spat, it is added to a track related to the spots track id.
     *
     * This will return an intersection of mesh tracks and trackmate spots.
     *
     * @param tracks The collection of tracks that will be mapped using the trackmate file tracks.
     * @param mis Image that tracks geometry is based on.
     * @param trackMateFile xml file that tracking information is loaded from.
     * @return A new set of tracks with meshs from the original tracks link based on information
     * from the trackmate file.
     */
    public static List<Track> applyTracking(List<Track> tracks, MeshImageStack mis, Path trackMateFile){

        double CUTOFF = 1.0;
        TmXmlReader reader = new TmXmlReader(trackMateFile.toFile());
        Model model = reader.getModel();
        TrackModel trackModel = model.getTrackModel();
        trackModel.nTracks(false);
        SpotCollection spots = model.getSpots();
        Map<String, Track> mapper = new HashMap<>();
        for (int i = 0; i < mis.getNFrames(); i++) {
            final int frame = i;
            List<DeformableMesh3D> meshes = tracks.stream().filter(
                    t-> t.containsKey(frame)
                ).map(
                    t->t.getMesh(frame)
                ).collect(Collectors.toList());
            int start = meshes.size();
            List<double[]> centers = new ArrayList<>();
            for(int j = 0; j<meshes.size(); j++){
                //do the same conversion used to create the trackmate file. If the mesh
                //shape hasn't been changed, and trackmate isnt' changing the CM this should be
                //result in 0's.
                //BinaryMomentsOfInertia bmi = new BinaryMomentsOfInertia(meshes.get(j), mis);
                //double[] center = bmi.getCenterOfMass();
                ProxySpot ps = new ProxySpot(meshes.get(j), mis);

                centers.add( new double[] { ps.cx, ps.cy, ps.cz});
            }

            Iterable<Spot> si = spots.iterable(frame, false);
            if(si != null){
                for (Spot spot : si) {

                    double min = Double.MAX_VALUE;
                    int closest = -1;
                    for(int j = 0; j<centers.size(); j++){

                        double[] mcm = centers.get(j);
                        double[] scm = {
                                spot.getDoublePosition(0),
                                spot.getDoublePosition(1),
                                spot.getDoublePosition(2)
                        };
                        double d = Vector3DOps.distance(scm, mcm);
                        if(d < min){
                            min = d;
                            closest = j;
                            if( d == 0 ) break;
                        }
                    }
                    if( min < CUTOFF ){
                        Object id = trackModel.trackIDOf(spot);
                        if(id == null){
                            id = "N" + mapper.size();
                        }

                        Track track = mapper.computeIfAbsent(id.toString(), j -> new Track("" + j));
                        if (track.containsKey(i)) {
                            System.out.println("Track has multiple spots");
                        } else {
                            track.addMesh(i, meshes.get(closest));
                            centers.remove(closest);
                            meshes.remove(closest);
                        }
                    } else{
                        System.out.println("couldn't map spot: " + min + " more than " + CUTOFF);
                    }


                }
            }
            if(meshes.size() > 0){
                System.out.println(meshes.size() + " of " + start + " meshes untracked on frame: " + frame);
            }
        }
        return new ArrayList<>(mapper.values());
    }

    /**
     * Overload in case the image hasn't been loaded yet.
     *
     * @param image path to the associated image.
     * @param trackMateFile path to a trackmate xml file.
     * @return Spherical meshes based on the position and radius of the spots found in the trackmate file. The
     * meshes are linked into tracks based on the TrackModel#trackIDOf(Spot spot)
     */
    public static List<Track> importTrackMateFile(Path image, Path trackMateFile) {
        return importTrackMateFile(new MeshImageStack(image), trackMateFile);
    }

    public static List<Track> importTrackMateFile(MeshImageStack mis, Path trackMateFile){

        List<Track> tracks = new ArrayList<>();
        TmXmlReader reader = new TmXmlReader(trackMateFile.toFile());
        Model model = reader.getModel();
        TrackModel trackModel = model.getTrackModel();
        Map<String, Track> mapper = new HashMap<>();
        trackModel.nTracks(false);
        SpotCollection spots = model.getSpots();
        long start = System.currentTimeMillis();
        for (int i = 0; i < mis.getNFrames(); i++) {
            int count = 0;
            Iterable<Spot> si = spots.iterable(i, false);
            if(si != null) {
                for (Spot spot : spots.iterable(i, false)) {
                    double [] xyz = ProxySpot.getNormalizedCoordinates( spot, mis);

                    Object id = trackModel.trackIDOf(spot);
                    if(id == null){
                        id = "N" + mapper.size();
                    }
                    Track track = mapper.computeIfAbsent(id.toString(), j -> new Track(j));
                    if (track.containsKey(i)) {
                        System.out.println("Track " + id + " has multiple spots same frame.");

                    } else {
                        double[] center = xyz;
                        double radius = spot.getFeature("RADIUS");
                        Sphere s = new Sphere(center, ProxySpot.getNormalizedRadius(spot, mis) );
                        DeformableMesh3D mesh = RayCastMesh.rayCastMesh(s, s.getCenter(), 1);
                        track.addMesh(i, mesh);

                    }
                    count++;
                }
            }
            System.out.println( "finished: " + i + " after " + ( System.currentTimeMillis() - start ) / 1000 );
            start = System.currentTimeMillis();
        }
        tracks.addAll(mapper.values());
        return tracks;
    }
    public static void saveAsTrackMateFile(MeshImageStack stack, List<Track> tracks, Path destination) throws IOException {
        Model trackMateModel = new Model();
        ImagePlus plus = stack.getOriginalPlus();
        // Set you physical units here.
        String spaceUnits = plus.getFileInfo().unit;
		String timeUnits = "seconds";
		trackMateModel.setPhysicalUnits( spaceUnits, timeUnits );

        trackMateModel.beginUpdate();
        double quality = 1.0;
        double weight = 1.0;
        
        // Set the frame interval here (convert from frame to seconds).
        double dt = plus.getFileInfo().frameInterval; // in timeUnits.
        double imgToUnit = stack.scale_values[0];
        double imgZToUnit = stack.scale_values[2];

        long start = System.currentTimeMillis();
        int tenPercent = tracks.size()/100;
        tenPercent = tenPercent == 0 ? 1: tenPercent;
        int count = 0;
        for(Track t: tracks){
            Spot last = null;
            for(Map.Entry<Integer, DeformableMesh3D> entry: t.getTrack().entrySet()){
                DeformableMesh3D mesh = entry.getValue();
                ProxySpot ps = new ProxySpot(mesh, stack);

                Spot s = new Spot(ps.cx, ps.cy, ps.cz, ps.radius, quality);
                s.putFeature( Spot.POSITION_T, Double.valueOf( dt * entry.getKey() ) );

                trackMateModel.addSpotTo(s, entry.getKey());

                if(last != null){
                    trackMateModel.addEdge(s, last, weight);
                }
                last = s;
            }
            count ++;
            if(count%tenPercent == 0){
                System.out.println( "finished: " + count + " after " + ( System.currentTimeMillis() - start ) / 1000 );
                start = System.currentTimeMillis();
            }

        }
        TrackModel tm = trackMateModel.getTrackModel();

        trackMateModel.endUpdate();

        Settings s = new Settings(stack.getOriginalPlus());
        s.addAllAnalyzers();
        
        // Compute all features.

        TmXmlWriter writer = new TmXmlWriter(destination.toFile());
        writer.appendSettings(s);
        writer.appendModel(trackMateModel);
        writer.appendGUIState( "ChooseTracker" );
        writer.writeToFile();

    }

    public static void main(String[] args) throws IOException {
        Path img = Paths.get("sample.tif");
        Path mesh = Paths.get("sample.bmf");
        Path xml = Paths.get("exported.xml");
        Path imported = Paths.get("sample-imported.bmf");
        Path applied = Paths.get("sample-applied.bmf");

        MeshImageStack mis = new MeshImageStack( img );
        List<Track> tracks = MeshReader.loadMeshes(mesh.toFile());
        saveAsTrackMateFile(mis, tracks, xml);

        List<Track> tracks2 = importTrackMateFile(img, xml);
        MeshWriter.saveMeshes(imported.toFile(), tracks2);

        applyTracking(tracks, mis, xml);
        MeshWriter.saveMeshes(applied.toFile(), tracks);
    }
}
