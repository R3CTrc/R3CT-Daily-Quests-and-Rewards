package com.r3ct.daily.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiUtils {
    public static void drawRewardStyleBar(GuiGraphicsExtractor g, Font font, int x, int y, float val, int max, String label, String valueText, int color, int bWidth, int tickStep, int[] labelsToDraw) {
        g.text(font, "§0" + label + ": " + valueText, x, y - 12, 0xFF000000, false);
        g.fill(x, y, x + bWidth, y + 8, 0xFF373737);
        int w = (int)((Math.min(val, max) / (float)max) * (bWidth - 2));
        if (w > 0) g.fill(x + 1, y + 1, x + 1 + w, y + 7, color | 0xFF000000);

        for (int p = tickStep; p < max; p += tickStep) {
            int tickX = x + (int)((p / (float)max) * (bWidth - 2));
            g.fill(tickX, y, tickX + 1, y + 8, 0xFF000000);
        }

        for (int p : labelsToDraw) {
            int tickX = x + (int)((p / (float)max) * (bWidth - 2));
            g.fill(tickX, y - 2, tickX + 1, y + 10, 0xFFFFFFFF);
            String pStr = String.valueOf(p);
            g.text(font, "§0" + pStr, tickX - (font.width(pStr) / 2), y + 13, 0xFF000000, false);
        }
    }
}