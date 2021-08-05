package deformablemesh.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by msmith on 6/21/16.
 */
public class ColorSuggestions {
    static int next = 0;
    static Random ng = new Random();

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
            new Color(0xCC6666),
            new Color(0xFFEBCD),
            new Color(0x7fff00),
            new Color(0xd2691e),
            new Color(0x00ffff),
            new Color(0xbdb76b),
            new Color(0x9932cc),
            new Color(0x8fbc8f),
            new Color(0x9400d3),
            new Color(0x696969),
            new Color(0xff00ff),
            new Color(0xffd700),
            new Color(0x228b22),
            new Color(0xdaa520),
            new Color(0x4b0082),
            new Color(0x008080),
            new Color(0xd2b48c),
            new Color(0x191970));

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
                                "fuzzy wuzzy",
                                "antique white",
                                "blanched almond",
                                "chartruese",
                                "chocolate",
                                "cyan",
                                "dark khaki",
                                "dark orchid",
                                "dark sea green",
                                "dark violet",
                                "dim grey",
                                "fuchsia",
                                "gold",
                                "forest green",
                                "golden rod",
                                "indigo",
                                "teal",
                                "tan",
                                "midnight blue"
    );


    public static Color getSuggestion(){
        Color c = colors.get(next++);
        next = next%colors.size();
        return c;
    }

    /**
     * Tries to get a color that doesn't exist in the current list.
     *
     * @param existing
     * @return
     */
    public static Color getSuggestion(List<Color> existing){
        List<Color> possible = colors.stream().filter(c->!existing.contains(c)).collect(Collectors.toList());

        if(possible.size()>0) {
            Color c = possible.get(0);
            return c;
        } else {
            Color c;
            do {
                c = new Color(ng.nextInt()&(0xffffff));
                next = next % colors.size();
            } while (existing.contains(c));
            return c;
        }

    }


    public static String getColorName(Color c){
        int dex = colors.indexOf(c);
        if(dex<0){
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        } else{
            return names.get(dex);
        }
    }

    public static Color fromNameOrSuggestion(String fullName){
        String name;
        if(fullName.contains("-")){
            name = fullName.split("-")[0];

        } else{
            name = fullName;
        }
        //name is just the html code.
        if('#'==name.charAt(0)){
            try{
                Color c = new Color(Integer.parseInt(name.substring(1), 16));
                return c;
            } catch(Exception e){
                //fail through.
            }

        }

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
