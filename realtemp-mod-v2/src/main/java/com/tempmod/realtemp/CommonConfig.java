package com.tempmod.realtemp;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every tunable physics/gameplay constant. This is a COMMON config (not
 * CLIENT) because the hypothermia/heatstroke logic runs server-side and
 * needs these same values there.
 */
public class CommonConfig {

    public static final ModConfigSpec SPEC;

    // --- Baseline biome -> Celsius mapping ---
    public static final ModConfigSpec.DoubleValue BASE_TEMP_OFFSET_C;
    public static final ModConfigSpec.DoubleValue BASE_TEMP_SCALE_C;

    // --- Altitude ---
    public static final ModConfigSpec.IntValue SEA_LEVEL_OVERRIDE;
    public static final ModConfigSpec.DoubleValue LAPSE_RATE_C_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue GEOTHERMAL_RATE_C_PER_BLOCK;

    // --- Diurnal cycle ---
    public static final ModConfigSpec.DoubleValue DIURNAL_AMPLITUDE_C;
    public static final ModConfigSpec.DoubleValue DIURNAL_HUMIDITY_DAMPING;
    public static final ModConfigSpec.DoubleValue DIURNAL_PEAK_HOUR;

    // --- Weather ---
    public static final ModConfigSpec.DoubleValue RAIN_COOLING_C;
    public static final ModConfigSpec.DoubleValue THUNDER_EXTRA_COOLING_C;

    // --- Local heat sources (fire, lava, furnaces, campfires, torches...) ---
    public static final ModConfigSpec.IntValue HEAT_SOURCE_SCAN_RADIUS;
    public static final ModConfigSpec.DoubleValue LOCAL_HEAT_SOURCE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MAX_LOCAL_HEAT_BONUS_C;
    public static final ModConfigSpec.DoubleValue FIRE_CONTACT_BONUS_C;
    public static final ModConfigSpec.DoubleValue LAVA_CONTACT_BONUS_C;
    public static final ModConfigSpec.DoubleValue WATER_IMMERSION_COOLING_C;

    // --- Health effects ---
    public static final ModConfigSpec.BooleanValue ENABLE_HEALTH_EFFECTS;
    public static final ModConfigSpec.DoubleValue EXPOSURE_RECOVERY_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_THRESHOLD_C;
    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_ONSET_SECONDS;
    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_SEVERE_SECONDS;
    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_DAMAGE_INTERVAL_SECONDS;
    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_DAMAGE_AMOUNT;

    public static final ModConfigSpec.DoubleValue HEATSTROKE_THRESHOLD_C;
    public static final ModConfigSpec.DoubleValue HEATSTROKE_ONSET_SECONDS;
    public static final ModConfigSpec.DoubleValue HEATSTROKE_SEVERE_SECONDS;
    public static final ModConfigSpec.DoubleValue HEATSTROKE_DAMAGE_INTERVAL_SECONDS;
    public static final ModConfigSpec.DoubleValue HEATSTROKE_DAMAGE_AMOUNT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Maps Minecraft's biome temperature value (roughly -0.5 to 2.0) onto a real-world",
                "Celsius baseline: realC = base_temp_offset_c + (biomeTemp * base_temp_scale_c)"
        ).push("baseline");
        BASE_TEMP_OFFSET_C = builder.comment("Offset added to the scaled biome temperature, in Celsius.")
                .defineInRange("base_temp_offset_c", -5.0, -50.0, 50.0);
        BASE_TEMP_SCALE_C = builder.comment("Multiplier applied to Minecraft's biome temperature value.")
                .defineInRange("base_temp_scale_c", 20.0, 1.0, 60.0);
        builder.pop();

        builder.comment("Vertical temperature gradient. Assumes 1 block = 1 meter.").push("altitude");
        SEA_LEVEL_OVERRIDE = builder.comment(
                        "The Y level treated as the climate baseline ('sea level').",
                        "Set to -2048 (default) to auto-use the world's real sea level.")
                .defineInRange("sea_level_override", -2048, -2048, 320);
        LAPSE_RATE_C_PER_BLOCK = builder.comment(
                        "Above sea level: degrees C lost per block of altitude.",
                        "Real-world average environmental lapse rate is ~6.5C/1000m = 0.0065/block.")
                .defineInRange("lapse_rate_c_per_block", 0.0065, 0.0, 0.05);
        GEOTHERMAL_RATE_C_PER_BLOCK = builder.comment(
                        "Below sea level: degrees C gained per block of depth.",
                        "Real-world average geothermal gradient is ~25-30C/1000m = 0.025-0.03/block.")
                .defineInRange("geothermal_gradient_c_per_block", 0.025, 0.0, 0.05);
        builder.pop();

        builder.comment("Day/night swing. Only applied where the sky is visible overhead.").push("diurnal");
        DIURNAL_AMPLITUDE_C = builder.comment("Max swing (+/-) from the daily average in a dry/arid biome, in Celsius.")
                .defineInRange("diurnal_amplitude_c", 8.0, 0.0, 30.0);
        DIURNAL_HUMIDITY_DAMPING = builder.comment("How much biome humidity (downfall) flattens the swing. 0=none, 1=full.")
                .defineInRange("diurnal_humidity_damping", 0.6, 0.0, 1.0);
        DIURNAL_PEAK_HOUR = builder.comment("Hour of day (24h, 0=midnight) of peak temperature. Real peak is mid-afternoon.")
                .defineInRange("diurnal_peak_hour", 15.0, 0.0, 24.0);
        builder.pop();

        builder.comment("Precipitation effects. Only applied where the sky is visible overhead.").push("weather");
        RAIN_COOLING_C = builder.comment("Temperature drop while rain/snow is actually falling on you, in Celsius.")
                .defineInRange("rain_cooling_c", 3.0, 0.0, 20.0);
        THUNDER_EXTRA_COOLING_C = builder.comment("Extra drop on top of rain cooling during a thunderstorm.")
                .defineInRange("thunder_extra_cooling_c", 2.0, 0.0, 20.0);
        builder.pop();

        builder.comment(
                "Local heat sources - fire, lava, lit furnaces/campfires, torches, etc.",
                "These apply regardless of sky visibility, which is why a furnace room can be warm indoors."
        ).push("heat_sources");
        HEAT_SOURCE_SCAN_RADIUS = builder.comment("Block radius scanned around you for heat sources. Higher = more expensive.")
                .defineInRange("heat_source_scan_radius", 4, 1, 8);
        LOCAL_HEAT_SOURCE_MULTIPLIER = builder.comment("Overall multiplier on the summed local heat source contribution.")
                .defineInRange("local_heat_source_multiplier", 1.0, 0.0, 10.0);
        MAX_LOCAL_HEAT_BONUS_C = builder.comment("Hard cap on how much nearby blocks can add, in Celsius (prevents e.g. a torch farm from being absurd).")
                .defineInRange("max_local_heat_bonus_c", 25.0, 0.0, 200.0);
        FIRE_CONTACT_BONUS_C = builder.comment("Bonus when you are directly on fire (e.g. stood in a fire block), in Celsius.")
                .defineInRange("fire_contact_bonus_c", 50.0, 0.0, 500.0);
        LAVA_CONTACT_BONUS_C = builder.comment("Bonus when you are touching/swimming in lava, in Celsius. Takes priority over the fire bonus.")
                .defineInRange("lava_contact_bonus_c", 70.0, 0.0, 1000.0);
        WATER_IMMERSION_COOLING_C = builder.comment(
                        "Penalty when submerged in water, in Celsius.",
                        "Real water conducts heat away from a body far faster than air at the same temperature,",
                        "which is why cold-water immersion is so much more dangerous than cold air.")
                .defineInRange("water_immersion_cooling_c", 10.0, 0.0, 50.0);
        builder.pop();

        builder.comment(
                "Hypothermia (sustained cold) and heatstroke (sustained heat in direct sun) effects.",
                "Exposure builds up while you're past the threshold and decays while you aren't."
        ).push("health");
        ENABLE_HEALTH_EFFECTS = builder.comment("Master on/off switch for all temperature-based health effects.")
                .define("enable_health_effects", true);
        EXPOSURE_RECOVERY_MULTIPLIER = builder.comment("How much faster exposure decays (recovers) compared to how it builds up.")
                .defineInRange("exposure_recovery_multiplier", 2.0, 0.1, 10.0);

        HYPOTHERMIA_THRESHOLD_C = builder.comment("Felt temperature below which cold exposure starts building up.")
                .defineInRange("hypothermia_threshold_c", -5.0, -100.0, 50.0);
        HYPOTHERMIA_ONSET_SECONDS = builder.comment("Seconds of sustained cold before Slowness/Mining Fatigue kick in.")
                .defineInRange("hypothermia_onset_seconds", 60.0, 1.0, 36000.0);
        HYPOTHERMIA_SEVERE_SECONDS = builder.comment("Seconds of sustained cold before damage starts.")
                .defineInRange("hypothermia_severe_seconds", 180.0, 1.0, 36000.0);
        HYPOTHERMIA_DAMAGE_INTERVAL_SECONDS = builder.comment("Seconds between each hypothermia damage tick once severe.")
                .defineInRange("hypothermia_damage_interval_seconds", 6.0, 1.0, 600.0);
        HYPOTHERMIA_DAMAGE_AMOUNT = builder.comment("Damage points per hit (2.0 = 1 heart).")
                .defineInRange("hypothermia_damage_amount", 1.0, 0.0, 20.0);

        HEATSTROKE_THRESHOLD_C = builder.comment("Felt temperature above which heat exposure starts building up (only while in direct sun).")
                .defineInRange("heatstroke_threshold_c", 35.0, -50.0, 100.0);
        HEATSTROKE_ONSET_SECONDS = builder.comment("Seconds of sustained heat-in-sun before Weakness/Nausea kick in.")
                .defineInRange("heatstroke_onset_seconds", 60.0, 1.0, 36000.0);
        HEATSTROKE_SEVERE_SECONDS = builder.comment("Seconds of sustained heat-in-sun before damage starts.")
                .defineInRange("heatstroke_severe_seconds", 180.0, 1.0, 36000.0);
        HEATSTROKE_DAMAGE_INTERVAL_SECONDS = builder.comment("Seconds between each heatstroke damage tick once severe.")
                .defineInRange("heatstroke_damage_interval_seconds", 6.0, 1.0, 600.0);
        HEATSTROKE_DAMAGE_AMOUNT = builder.comment("Damage points per hit (2.0 = 1 heart).")
                .defineInRange("heatstroke_damage_amount", 1.0, 0.0, 20.0);
        builder.pop();

        SPEC = builder.build();
    }
}
