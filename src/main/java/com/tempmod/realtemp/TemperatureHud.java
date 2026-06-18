package com.tempmod.realtemp;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Draws the current temperature in the top-left corner every frame.
 * Cheap enough to compute live - no caching/tick-throttling needed.
 */
public class TemperatureHud implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        BlockPos pos = mc.player.blockPosition();
        double tempC = TemperatureCalculator.computeTemperatureC(mc.level, pos);

        String text = Config.USE_FAHRENHEIT.get()
                ? String.format("%.1f\u00B0F", TemperatureCalculator.celsiusToFahrenheit(tempC))
                : String.format("%.1f\u00B0C", tempC);

        guiGraphics.drawString(
                mc.font,
                Component.literal(text),
                Config.HUD_X.get(),
                Config.HUD_Y.get(),
                0xFFFFFF,
                true
        );
    }
}
