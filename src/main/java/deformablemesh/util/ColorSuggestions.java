package deformablemesh.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * Created by msmith on 6/21/16.
 */
public class ColorSuggestions {
    static int next = 0;
    static List<Color> colors = Arrays.asList(
            new Color(0xff0000),
            new Color(0x0000ff),
            new Color(0x005500),
            new Color(0x8F9779),
            new Color(0xA52A2A),
            new Color(0xE0218A),
            new Color(0x7C0A02),
            new Color(0x0D98BA),
            new Color(0xCD7F32),
            new Color(0x4B3621),
            new Color(0x062A78),
            new Color(0xB87333),
            new Color(0x28589C),
            new Color(0xFF8C00),
            new Color(0x9400D3),
            new Color(0x614051),
            new Color(0xCC6666));

    final static List<String> names = Arrays.asList(
                                "red",
                                "blue",
                                "green",
                                "artichoke",
                                "auburn",
                                "pink",
                                "barn red",
                                "blue green",
                                "bronze",
                                "cafe noir",
                                "catalina blue",
                                "copper",
                                "cyan cobalt blue",
                                "dark orange",
                                "dark violet",
                                "eggplant",
                                "fuzzy wuzzy"
    );


    public static Color getSuggestion(){
        Color c = colors.get(next++);
        next = next%colors.size();
        return c;
    }

    public static String getColorName(Color c){
        int dex = colors.indexOf(c);
        if(dex<0){
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        } else{
            return names.get(dex);
        }
    }

    public static Color fromNameOrSuggestion(String name){
        int dex = names.indexOf(name);
        if(dex<0){
            return getSuggestion();
        } else{
            return colors.get(dex);
        }
    }

    public static void main(String[] args){

        for(int i = 0; i<100; i++){
            int r = (int)(Math.random()*255);
            int g = (int)(Math.random()*255);
            int b = (int)(Math.random()*255);
            System.out.println(getColorName(new Color(r, g, b)));
        }

    }
}
