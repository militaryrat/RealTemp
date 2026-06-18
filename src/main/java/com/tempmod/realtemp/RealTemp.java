package com.tempmod.realtemp;

import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * RealTemp - a client-side-only mod that estimates an in-world "real" temperature
 * from biome, altitude, time of day, and sky exposure, and shows it as a HUD overlay.
 *
 * Because this only reads world state the client already has (biome, light, weather,
 * day time) and only draws a HUD overlay, the whole mod can live on the client side.
 * That means it also works fine on servers that don't have it installed - you'll
 * just be the only one who can see the readout.
 */
@Mod(value = RealTemp.MODID, dist = Dist.CLIENT)
public class RealTemp {

    public static final String MODID = "realtemp";

    public RealTemp(IEventBus modEventBus, ModContainer container) {
        // Register the config (lapse rate, sea level, units, etc.) - editable in
        // .minecraft/config/realtemp-client.toml after the first run.
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // Register the HUD layer that draws the temperature readout.
        modEventBus.addListener(this::registerGuiLayers);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(MODID, "temperature_hud"),
                (LayeredDraw.Layer) new TemperatureHud()
        );
    }
}
