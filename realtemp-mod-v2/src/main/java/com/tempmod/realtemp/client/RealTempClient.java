package com.tempmod.realtemp.client;

import com.tempmod.realtemp.RealTemp;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Second @Mod class for the SAME mod id, restricted to Dist.CLIENT - this is
 * the NeoForge-recommended way to split off client-only registration code so
 * it never gets loaded (and never has to resolve client-only classes like
 * GuiGraphics/LayeredDraw) on a dedicated server.
 */
@Mod(value = RealTemp.MODID, dist = Dist.CLIENT)
public class RealTempClient {

    public RealTempClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modEventBus.addListener(this::registerGuiLayers);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(RealTemp.MODID, "temperature_hud"),
                (LayeredDraw.Layer) new TemperatureHud()
        );
    }
}
