package deformablemesh.externalenergies;

/**
 * Created by msmith on 2/10/16.
 */
public enum ImageEnergyType {
    Intensity("Max Intensity"), Gradient("Max Gradient"), PerpendicularIntensity("⟂ Max Intensity"), PerpendicularGradient("⟂ Max Gradient");

    String title;
    ImageEnergyType(String t){
        title = t;
    }

    @Override
    public String toString(){
        return title;
    }

}
