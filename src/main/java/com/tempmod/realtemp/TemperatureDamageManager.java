package com.tempmod.realtemp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Handles player damage based on temperature extremes.
 * Player takes damage when temperature is too cold (<=6°C) or too hot (>=35°C).
 * 
 * Damage is applied based on:
 * - How far from the danger zone the temperature is
 * - Configurable damage intervals and amounts
 */
public class TemperatureDamageManager {

    private TemperatureDamageManager() {
    }

    /**
     * Apply damage to player based on current temperature.
     * Called every tick when the player is in single-player mode.
     */
    public static void applyTemperatureDamage(Player player) {
        if (player == null || player.level() == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = player.blockPosition();
        double tempC = TemperatureCalculator.computeTemperatureC(level, pos);

        // Check if damage should be applied
        if (!Config.ENABLE_TEMPERATURE_DAMAGE.get()) {
            return;
        }

        // Don't apply damage in creative mode
        if (player.isCreative()) {
            return;
        }

        double coldThreshold = Config.COLD_DAMAGE_THRESHOLD_C.get();
        double heatThreshold = Config.HEAT_DAMAGE_THRESHOLD_C.get();
        
        // Check for cold damage
        if (tempC <= coldThreshold) {
            applyDamage(player, tempC, coldThreshold, true);
        }
        // Check for heat damage
        else if (tempC >= heatThreshold) {
            applyDamage(player, tempC, heatThreshold, false);
        }
    }

    /**
     * Apply damage based on temperature extremes.
     * 
     * @param player The player to damage
     * @param currentTemp Current temperature in Celsius
     * @param threshold The danger threshold (cold or heat)
     * @param isCold True for cold damage, false for heat damage
     */
    private static void applyDamage(Player player, double currentTemp, double threshold, boolean isCold) {
        if (!Config.ENABLE_TEMPERATURE_DAMAGE.get()) {
            return;
        }

        // Calculate how far past the threshold we are
        double degreesOverThreshold;
        if (isCold) {
            degreesOverThreshold = threshold - currentTemp;  // e.g., 6 - (-10) = 16
        } else {
            degreesOverThreshold = currentTemp - threshold;  // e.g., 40 - 35 = 5
        }

        // Damage scales with temperature severity
        double damageMultiplier = Config.TEMPERATURE_DAMAGE_MULTIPLIER.get();
        double baseDamage = damageMultiplier * degreesOverThreshold;

        // Apply damage at a configured interval (in ticks)
        int damageInterval = Config.TEMPERATURE_DAMAGE_INTERVAL_TICKS.get();
        if (player.tickCount % damageInterval == 0) {
            float damageAmount = (float) baseDamage;
            
            // Minimum damage of 0.5 half-hearts when in danger zone
            if (damageAmount < 0.5f) {
                damageAmount = 0.5f;
            }

            // Maximum damage cap to prevent one-shots
            float maxDamage = Config.TEMPERATURE_DAMAGE_MAX_PER_TICK.get();
            if (damageAmount > maxDamage) {
                damageAmount = maxDamage;
            }

            // Apply the damage
            player.hurt(player.damageSources().generic(), damageAmount);
        }
    }
}
