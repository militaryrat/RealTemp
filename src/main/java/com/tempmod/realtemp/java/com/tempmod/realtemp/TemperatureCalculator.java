package com.tempmod.realtemp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Pure-function temperature model. No rendering, no side effects - just
 * world state in, a Celsius estimate out. Kept separate from the HUD class
 * so it's easy to reuse (e.g. from a command, a thermometer item, etc.)
 * later if you want to expand the mod.
 *
 * Model, in order of application:
 *  1. Biome baseline       - Minecraft's biome temperature value, rescaled to Celsius.
 *  2. Altitude              - real environmental lapse rate above sea level,
 *                              real geothermal gradient below it.
 *  3. Diurnal cycle          - a cosine wave peaking in mid-afternoon, damped by
 *                              biome humidity, only where the sky is visible.
 *  4. Weather                - rain/thunder cooling, only where the sky is visible.
 */
public final class TemperatureCalculator {

    private TemperatureCalculator() {
    }

    public static double computeTemperatureC(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();

        double biomeTemp = climate.temperature(); // vanilla scale, roughly -0.5 .. 2.0
        double downfall = climate.downfall();      // 0.0 (arid) .. 1.0 (humid)

        double baseTempC = Config.BASE_TEMP_OFFSET_C.get() + biomeTemp * Config.BASE_TEMP_SCALE_C.get();
        double altitudeEffectC = computeAltitudeEffectC(level, pos.getY());

        double diurnalC = 0.0;
        double weatherC = 0.0;
        if (level.canSeeSky(pos)) {
            diurnalC = computeDiurnalC(level, downfall);
            weatherC = computeWeatherCoolingC(level, pos);
        }

        return baseTempC + altitudeEffectC + diurnalC - weatherC;
    }

    private static double computeAltitudeEffectC(Level level, int y) {
        int seaLevel = resolveSeaLevel(level);
        int relativeHeight = y - seaLevel;
        if (relativeHeight > 0) {
            return -relativeHeight * Config.LAPSE_RATE_C_PER_BLOCK.get();
        }
        return -relativeHeight * Config.GEOTHERMAL_RATE_C_PER_BLOCK.get();
    }

    private static int resolveSeaLevel(Level level) {
        int override = Config.SEA_LEVEL_OVERRIDE.get();
        if (override > -2048) {
            return override;
        }
        return level.getSeaLevel();
    }

    private static double computeDiurnalC(Level level, double downfall) {
        double amplitude = Config.DIURNAL_AMPLITUDE_C.get()
                * (1.0 - downfall * Config.DIURNAL_HUMIDITY_DAMPING.get());
        double hourOfDay = hourOfDay(level);
        double peakHour = Config.DIURNAL_PEAK_HOUR.get();
        return amplitude * Math.cos(2.0 * Math.PI * (hourOfDay - peakHour) / 24.0);
    }

    private static double hourOfDay(Level level) {
        long dayTime = level.getDayTime() % 24000L;
        // Tick 0 is 6:00 AM in vanilla Minecraft; tick 18000 is midnight.
        double hour = (dayTime / 24000.0) * 24.0 + 6.0;
        return hour % 24.0;
    }

    private static double computeWeatherCoolingC(Level level, BlockPos pos) {
        if (!level.isRainingAt(pos)) {
            return 0.0;
        }
        double cooling = Config.RAIN_COOLING_C.get();
        if (level.isThundering()) {
            cooling += Config.THUNDER_EXTRA_COOLING_C.get();
        }
        return cooling;
    }

    public static double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }
}
