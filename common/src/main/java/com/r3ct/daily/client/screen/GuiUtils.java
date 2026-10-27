package com.r3ct.daily.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiUtils {
    public static void drawRewardStyleBar(GuiGraphicsExtractor g, Font font, int x, int y, float currentValue, int maxValue, String label, String valueText, int color, int barWidth, int tickStep, int[] milestones) {
        g.text(font, "§0" + label + ": " + valueText, x, y - 12, 0xFF000000, false);
        g.fill(x, y, x + barWidth, y + 8, 0xFF373737);

        int fillWidth = (int)((Math.min(currentValue, maxValue) / (float)maxValue) * (barWidth - 2));
        if (fillWidth > 0) g.fill(x + 1, y + 1, x + 1 + fillWidth, y + 7, color | 0xFF000000);

        for (int tick = tickStep; tick < maxValue; tick += tickStep) {
            int tickX = x + (int)((tick / (float)maxValue) * (barWidth - 2));
            g.fill(tickX, y, tickX + 1, y + 8, 0xFF000000);
        }

        for (int milestone : milestones) {
            int milestoneX = x + (int)((milestone / (float)maxValue) * (barWidth - 2));
            g.fill(milestoneX, y - 2, milestoneX + 1, y + 10, 0xFFFFFFFF);
            String milestoneText = String.valueOf(milestone);
            g.text(font, "§0" + milestoneText, milestoneX - (font.width(milestoneText) / 2), y + 13, 0xFF000000, false);
        }
    }
}