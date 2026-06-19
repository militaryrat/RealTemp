package com.tempmod.realtemp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Pure-function temperature model, now with three layers:
 *
 *  1. AMBIENT  - biome baseline + altitude + diurnal cycle + weather (unchanged
 *                from before; still the "what's the air like here" number).
 *  2. LOCAL HEAT SOURCES - fire/lava/lit furnaces/campfires/torches near you.
 *     Applied regardless of sky visibility - this is why an indoor furnace
 *     room registers as warm even though it can't see the sky.
 *  3. ENTITY STATUS - are you literally on fire, in lava, or submerged in
 *     water right now. Water conducts heat away from a body much faster than
 *     air, so submersion is modeled as a flat "felt" temperature penalty.
 *
 * computeFeltTemperatureC() is the one you actually want for display or for
 * health effects - it folds all three together.
 */
public final class TemperatureCalculator {

    private TemperatureCalculator() {
    }

    private record BlockHeatSource(Block block, double baseHeatC) {
    }

    private static final List<BlockHeatSource> HEAT_SOURCES = List.of(
            new BlockHeatSource(Blocks.LAVA, 35.0),
            new BlockHeatSource(Blocks.FIRE, 30.0),
            new BlockHeatSource(Blocks.SOUL_FIRE, 25.0),
            new BlockHeatSource(Blocks.MAGMA_BLOCK, 18.0),
            new BlockHeatSource(Blocks.CAMPFIRE, 22.0),
            new BlockHeatSource(Blocks.SOUL_CAMPFIRE, 18.0),
            new BlockHeatSource(Blocks.FURNACE, 16.0),
            new BlockHeatSource(Blocks.BLAST_FURNACE, 18.0),
            new BlockHeatSource(Blocks.SMOKER, 16.0),
            new BlockHeatSource(Blocks.LANTERN, 4.0),
            new BlockHeatSource(Blocks.SOUL_LANTERN, 3.0),
            new BlockHeatSource(Blocks.TORCH, 3.0),
            new BlockHeatSource(Blocks.WALL_TORCH, 3.0),
            new BlockHeatSource(Blocks.SOUL_TORCH, 2.0),
            new BlockHeatSource(Blocks.SOUL_WALL_TORCH, 2.0)
    );

    // ---------------------------------------------------------------
    // Top-level entry points
    // ---------------------------------------------------------------

    /** Ambient air temperature at a position: biome + altitude + diurnal + weather. */
    public static double computeAmbientTemperatureC(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();

        double biomeTemp = climate.temperature();
        double downfall = climate.downfall();

        double baseTempC = CommonConfig.BASE_TEMP_OFFSET_C.get() + biomeTemp * CommonConfig.BASE_TEMP_SCALE_C.get();
        double altitudeEffectC = computeAltitudeEffectC(level, pos.getY());

        double diurnalC = 0.0;
        double weatherC = 0.0;
        if (level.canSeeSky(pos)) {
            diurnalC = computeDiurnalC(level, downfall);
            weatherC = computeWeatherCoolingC(level, pos);
        }

        return baseTempC + altitudeEffectC + diurnalC - weatherC;
    }

    /** Sum of nearby fire/lava/furnace/campfire/torch heat, distance-weighted and capped. */
    public static double computeLocalHeatSourceC(Level level, BlockPos center) {
        int radius = CommonConfig.HEAT_SOURCE_SCAN_RADIUS.get();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        double total = 0.0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.equals(center)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            for (BlockHeatSource source : HEAT_SOURCES) {
                if (state.is(source.block())) {
                    if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                        break; // unlit furnace/campfire - no heat
                    }
                    double distance = Math.sqrt(pos.distSqr(center));
                    total += source.baseHeatC() / (1.0 + distance);
                    break;
                }
            }
        }

        double scaled = total * CommonConfig.LOCAL_HEAT_SOURCE_MULTIPLIER.get();
        return Math.min(scaled, CommonConfig.MAX_LOCAL_HEAT_BONUS_C.get());
    }

    /** Ambient + local heat sources + your own fire/lava/water status. This is "what you feel". */
    public static double computeFeltTemperatureC(Level level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        double felt = computeAmbientTemperatureC(level, pos) + computeLocalHeatSourceC(level, pos);

        if (entity.isInLava()) {
            felt += CommonConfig.LAVA_CONTACT_BONUS_C.get();
        } else if (entity.isOnFire()) {
            felt += CommonConfig.FIRE_CONTACT_BONUS_C.get();
        }

        if (entity.isInWater()) {
            felt -= CommonConfig.WATER_IMMERSION_COOLING_C.get();
        }

        return felt;
    }

    /** True if the position can see a clear, non-rainy sky during daylight hours. */
    public static boolean isInDirectSunlight(Level level, BlockPos pos) {
        if (!level.canSeeSky(pos) || level.isRainingAt(pos)) {
            return false;
        }
        double hour = hourOfDay(level);
        return hour >= 6.0 && hour <= 18.0;
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    private static double computeAltitudeEffectC(Level level, int y) {
        int seaLevel = resolveSeaLevel(level);
        int relativeHeight = y - seaLevel;
        if (relativeHeight > 0) {
            return -relativeHeight * CommonConfig.LAPSE_RATE_C_PER_BLOCK.get();
        }
        return -relativeHeight * CommonConfig.GEOTHERMAL_RATE_C_PER_BLOCK.get();
    }

    private static int resolveSeaLevel(Level level) {
        int override = CommonConfig.SEA_LEVEL_OVERRIDE.get();
        if (override > -2048) {
            return override;
        }
        return level.getSeaLevel();
    }

    private static double computeDiurnalC(Level level, double downfall) {
        double amplitude = CommonConfig.DIURNAL_AMPLITUDE_C.get()
                * (1.0 - downfall * CommonConfig.DIURNAL_HUMIDITY_DAMPING.get());
        double hourOfDay = hourOfDay(level);
        double peakHour = CommonConfig.DIURNAL_PEAK_HOUR.get();
        return amplitude * Math.cos(2.0 * Math.PI * (hourOfDay - peakHour) / 24.0);
    }

    private static double hourOfDay(Level level) {
        long dayTime = level.getDayTime() % 24000L;
        double hour = (dayTime / 24000.0) * 24.0 + 6.0;
        return hour % 24.0;
    }

    private static double computeWeatherCoolingC(Level level, BlockPos pos) {
        if (!level.isRainingAt(pos)) {
            return 0.0;
        }
        double cooling = CommonConfig.RAIN_COOLING_C.get();
        if (level.isThundering()) {
            cooling += CommonConfig.THUNDER_EXTRA_COOLING_C.get();
        }
        return cooling;
    }

    public static double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }
}
