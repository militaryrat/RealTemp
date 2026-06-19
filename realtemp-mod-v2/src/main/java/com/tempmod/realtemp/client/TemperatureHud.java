package com.tempmod.realtemp.client;

import com.tempmod.realtemp.TemperatureCalculator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * Draws the current felt temperature in the corner of the screen.
 *
 * Recomputation is throttled to once every few game ticks rather than every
 * render frame - the local heat-source scan added in computeFeltTemperatureC
 * checks a small cube of blocks around you, which is cheap a few times a
 * second but wasteful at 100+ FPS.
 */
public class TemperatureHud implements LayeredDraw.Layer {

    private static final long UPDATE_INTERVAL_TICKS = 5;

    private long lastUpdateTick = Long.MIN_VALUE;
    private String cachedText = "";

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (gameTime - lastUpdateTick >= UPDATE_INTERVAL_TICKS) {
            double tempC = TemperatureCalculator.computeFeltTemperatureC(mc.level, mc.player);
            cachedText = ClientConfig.USE_FAHRENHEIT.get()
                    ? String.format("%.1f\u00B0F", TemperatureCalculator.celsiusToFahrenheit(tempC))
                    : String.format("%.1f\u00B0C", tempC);
            lastUpdateTick = gameTime;
        }

        guiGraphics.drawString(
                mc.font,
                Component.literal(cachedText),
                ClientConfig.HUD_X.get(),
                ClientConfig.HUD_Y.get(),
                0xFFFFFF,
                true
        );
    }
}
