package deformablemesh.externalenergies;

/**
 * Created by msmith on 2/10/16.
 */
public enum ImageEnergyType {
    PerpendicularIntensity("⟂ Max Intensity"), PerpendicularGradient("⟂ Max Gradient"),
    None("No Energy");

    String title;
    ImageEnergyType(String t){
        title = t;
    }

    @Override
    public String toString(){
        return title;
    }

}
