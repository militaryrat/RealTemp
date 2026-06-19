package com.tempmod.realtemp;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handler for applying temperature damage to players each tick.
 * Only runs on the client side and only applies damage to the local player.
 */
@EventBusSubscriber(modid = RealTemp.MODID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TemperatureDamageHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        
        // Only apply to client player in single-player
        if (player.level().isClientSide) {
            return;
        }
        
        TemperatureDamageManager.applyTemperatureDamage(player);
    }
}
