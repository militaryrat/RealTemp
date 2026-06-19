package com.tempmod.realtemp;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side (logical server only - guarded via Level#isClientSide) tracker
 * for sustained cold/heat exposure. Lives entirely in memory: a relog or
 * server restart resets your exposure meter back to zero. That's a deliberate
 * simplification - tracking it persistently would need NeoForge's data
 * attachment system, which felt like overkill for a single-player mod.
 *
 * Two independent meters per player:
 *  - coldTicks  builds up while felt temp < hypothermia_threshold_c
 *  - heatTicks  builds up while felt temp > heatstroke_threshold_c AND you're
 *               standing in direct, unobstructed, non-rainy sunlight
 * Both decay (recover) whenever their condition isn't met.
 *
 * Each meter has two stages once past "onset": status effects only, then
 * (past "severe") periodic real damage.
 */
public class TemperatureHealthHandler {

    private static final int CHECK_INTERVAL_TICKS = 20; // once per second

    private final Map<UUID, Exposure> exposures = new ConcurrentHashMap<>();

    private static final class Exposure {
        double coldTicks;
        double heatTicks;
        long lastColdDamageTick = Long.MIN_VALUE;
        long lastHeatDamageTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide() || !CommonConfig.ENABLE_HEALTH_EFFECTS.get()) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (player.isSpectator() || player.isCreative()) {
            return;
        }

        Exposure exposure = exposures.computeIfAbsent(player.getUUID(), id -> new Exposure());
        double feltC = TemperatureCalculator.computeFeltTemperatureC(level, player);

        updateCold(player, level, exposure, feltC);
        updateHeat(player, level, exposure, feltC);
    }

    private void updateCold(Player player, Level level, Exposure exposure, double feltC) {
        double recovery = CommonConfig.EXPOSURE_RECOVERY_MULTIPLIER.get();
        if (feltC < CommonConfig.HYPOTHERMIA_THRESHOLD_C.get()) {
            exposure.coldTicks += CHECK_INTERVAL_TICKS;
        } else {
            exposure.coldTicks = Math.max(0.0, exposure.coldTicks - CHECK_INTERVAL_TICKS * recovery);
        }

        double onsetTicks = CommonConfig.HYPOTHERMIA_ONSET_SECONDS.get() * 20.0;
        double severeTicks = CommonConfig.HYPOTHERMIA_SEVERE_SECONDS.get() * 20.0;

        if (exposure.coldTicks >= onsetTicks) {
            applyRefreshingEffect(player, MobEffects.MOVEMENT_SLOWDOWN);
            applyRefreshingEffect(player, MobEffects.DIG_SLOWDOWN);
        }

        if (exposure.coldTicks >= severeTicks) {
            long intervalTicks = (long) (CommonConfig.HYPOTHERMIA_DAMAGE_INTERVAL_SECONDS.get() * 20.0);
            if (player.tickCount - exposure.lastColdDamageTick >= intervalTicks) {
                player.hurt(level.damageSources().freeze(), (float) CommonConfig.HYPOTHERMIA_DAMAGE_AMOUNT.get());
                exposure.lastColdDamageTick = player.tickCount;
            }
        }
    }

    private void updateHeat(Player player, Level level, Exposure exposure, double feltC) {
        double recovery = CommonConfig.EXPOSURE_RECOVERY_MULTIPLIER.get();
        boolean inSun = TemperatureCalculator.isInDirectSunlight(level, player.blockPosition());

        if (feltC > CommonConfig.HEATSTROKE_THRESHOLD_C.get() && inSun) {
            exposure.heatTicks += CHECK_INTERVAL_TICKS;
        } else {
            exposure.heatTicks = Math.max(0.0, exposure.heatTicks - CHECK_INTERVAL_TICKS * recovery);
        }

        double onsetTicks = CommonConfig.HEATSTROKE_ONSET_SECONDS.get() * 20.0;
        double severeTicks = CommonConfig.HEATSTROKE_SEVERE_SECONDS.get() * 20.0;

        if (exposure.heatTicks >= onsetTicks) {
            applyRefreshingEffect(player, MobEffects.WEAKNESS);
            applyRefreshingEffect(player, MobEffects.CONFUSION);
        }

        if (exposure.heatTicks >= severeTicks) {
            long intervalTicks = (long) (CommonConfig.HEATSTROKE_DAMAGE_INTERVAL_SECONDS.get() * 20.0);
            if (player.tickCount - exposure.lastHeatDamageTick >= intervalTicks) {
                player.hurt(level.damageSources().generic(), (float) CommonConfig.HEATSTROKE_DAMAGE_AMOUNT.get());
                exposure.lastHeatDamageTick = player.tickCount;
            }
        }
    }

    /** Re-applies a short effect every check so it never expires between checks, without stacking. */
    private void applyRefreshingEffect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        player.addEffect(new MobEffectInstance(effect, CHECK_INTERVAL_TICKS + 5, 0, false, true, true));
    }
}
