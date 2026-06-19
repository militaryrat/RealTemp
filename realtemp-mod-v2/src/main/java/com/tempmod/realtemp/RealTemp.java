package com.tempmod.realtemp;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Common entrypoint - loads on BOTH the client and a dedicated server, so the
 * hypothermia/heatstroke logic (which has to be server-authoritative) works
 * even if you ever play on a real dedicated server that has this mod installed.
 *
 * The HUD overlay is registered separately in client.RealTempClient, which is
 * its own @Mod class restricted to Dist.CLIENT - this keeps every client-only
 * class (GuiGraphics, LayeredDraw, Minecraft...) out of this class entirely,
 * so a dedicated server never has to load them.
 */
@Mod(RealTemp.MODID)
public class RealTemp {

    public static final String MODID = "realtemp";

    public RealTemp(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        NeoForge.EVENT_BUS.register(new TemperatureHealthHandler());
    }
}
