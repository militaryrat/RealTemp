package com.tempmod.realtemp;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Event handler for applying temperature damage to players each tick.
 * Runs on the server side for single-player worlds.
 */
@EventBusSubscriber(modid = RealTemp.MODID)
public class TemperatureDamageHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Get all players in the level and apply damage
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player != null && player.level() != null && !player.level().isClientSide) {
                TemperatureDamageManager.applyTemperatureDamage(player);
            }
        });
    }
}
