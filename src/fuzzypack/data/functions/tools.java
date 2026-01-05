package fuzzypack.data.functions;


import java.awt.*;

import static javax.swing.Action.DEFAULT;

public class tools {

    public static Color ColorAlphaChange(Color inColor, float alpha) {
        /** Changes the alpha value of the color to a new value **/
        int r = inColor.getRed();
        int g = inColor.getGreen();
        int b = inColor.getBlue();

        if ((r >= 0 && r <= 255) && (g >= 0 && g <= 255) && (b >= 0 && b <= 255)) {
            return new Color(inColor.getRed(), inColor.getGreen(), inColor.getBlue(), (int) alpha);
        }  else {
            return Color.white;
        }
    }

    public static Color ColorChangeRGB (Color inColor, String rgb, int value) {
        /**Change rgb value of a color, return white if u done goofed **/
        if (value > 255) value = 255;
        if (value < 0) value = 0;
        int r = inColor.getRed();
        int g = inColor.getGreen();
        int b = inColor.getBlue();
        int a = inColor.getAlpha();

        switch (rgb) {
            case "r":
                return new Color(value, g, b, a);
            case "g":
                return new Color(r, value, b, a);
            case "b":
                return new Color(r, g, value, a);

            case DEFAULT:
                return Color.white;
        }
        return Color.white; //java moment
    }


}
