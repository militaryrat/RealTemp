package com.tempmod.realtemp;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All the knobs for the temperature model. Every default is chosen to mirror a
 * real, citable physical quantity (see comments) - tweak them to taste in
 * .minecraft/config/realtemp-client.toml.
 */
public class Config {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue BASE_TEMP_OFFSET_C;
    public static final ModConfigSpec.DoubleValue BASE_TEMP_SCALE_C;

    public static final ModConfigSpec.IntValue SEA_LEVEL_OVERRIDE;
    public static final ModConfigSpec.DoubleValue LAPSE_RATE_C_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue GEOTHERMAL_RATE_C_PER_BLOCK;

    public static final ModConfigSpec.DoubleValue DIURNAL_AMPLITUDE_C;
    public static final ModConfigSpec.DoubleValue DIURNAL_HUMIDITY_DAMPING;
    public static final ModConfigSpec.DoubleValue DIURNAL_PEAK_HOUR;

    public static final ModConfigSpec.DoubleValue RAIN_COOLING_C;
    public static final ModConfigSpec.DoubleValue THUNDER_EXTRA_COOLING_C;

    public static final ModConfigSpec.BooleanValue USE_FAHRENHEIT;
    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;

    // Temperature damage settings
    public static final ModConfigSpec.BooleanValue ENABLE_TEMPERATURE_DAMAGE;
    public static final ModConfigSpec.DoubleValue COLD_DAMAGE_THRESHOLD_C;
    public static final ModConfigSpec.DoubleValue HEAT_DAMAGE_THRESHOLD_C;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue TEMPERATURE_DAMAGE_INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_DAMAGE_MAX_PER_TICK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Maps Minecraft's biome temperature value (roughly -0.5 to 2.0) onto a real-world",
                "Celsius baseline: realC = base_temp_offset_c + (biomeTemp * base_temp_scale_c)"
        ).push("baseline");

        BASE_TEMP_OFFSET_C = builder
                .comment("Offset added to the scaled biome temperature, in Celsius.")
                .defineInRange("base_temp_offset_c", -5.0, -50.0, 50.0);

        BASE_TEMP_SCALE_C = builder
                .comment("Multiplier applied to Minecraft's biome temperature value.")
                .defineInRange("base_temp_scale_c", 20.0, 1.0, 60.0);

        builder.pop();

        builder.comment(
                "Vertical temperature gradient. Assumes 1 block = 1 meter, as requested."
        ).push("altitude");

        SEA_LEVEL_OVERRIDE = builder
                .comment(
                        "The Y level treated as the climate baseline ('sea level') for the lapse rate.",
                        "Set to -2048 (default) to automatically use the world's real sea level instead."
                )
                .defineInRange("sea_level_override", -2048, -2048, 320);

        LAPSE_RATE_C_PER_BLOCK = builder
                .comment(
                        "Above sea level: degrees C lost per block of altitude.",
                        "Real-world average environmental lapse rate is ~6.5C per 1000m = 0.0065/block.",
                        "With this default, a 1000-block mountain top is about 6.5C colder than its base."
                )
                .defineInRange("lapse_rate_c_per_block", 0.0065, 0.0, 0.05);

        GEOTHERMAL_RATE_C_PER_BLOCK = builder
                .comment(
                        "Below sea level (mineshafts, caves): degrees C gained per block of depth.",
                        "Real-world average geothermal gradient is ~25-30C per 1000m = 0.025-0.03/block."
                )
                .defineInRange("geothermal_gradient_c_per_block", 0.025, 0.0, 0.05);

        builder.pop();

        builder.comment(
                "Day/night swing. Only applied where the sky is visible overhead - enclosed caves",
                "stay at a stable, annual-mean-like temperature, the same way real caves do."
        ).push("diurnal");

        DIURNAL_AMPLITUDE_C = builder
                .comment("Max swing (+/-) from the daily average in a dry/arid biome, in Celsius.")
                .defineInRange("diurnal_amplitude_c", 8.0, 0.0, 30.0);

        DIURNAL_HUMIDITY_DAMPING = builder
                .comment(
                        "How much a biome's humidity (downfall) flattens the swing.",
                        "0 = no effect, 1 = fully flattened in the wettest biomes (humid air holds its temperature)."
                )
                .defineInRange("diurnal_humidity_damping", 0.6, 0.0, 1.0);

        DIURNAL_PEAK_HOUR = builder
                .comment(
                        "Hour of day (24h clock, 0 = midnight) of peak temperature.",
                        "Real-world peak is mid-afternoon (not solar noon) due to thermal lag in the ground/air."
                )
                .defineInRange("diurnal_peak_hour", 15.0, 0.0, 24.0);

        builder.pop();

        builder.comment(
                "Precipitation effects. Only applied where the sky is visible overhead."
        ).push("weather");

        RAIN_COOLING_C = builder
                .comment("Temperature drop while rain/snow is actually falling on you, in Celsius.")
                .defineInRange("rain_cooling_c", 3.0, 0.0, 20.0);

        THUNDER_EXTRA_COOLING_C = builder
                .comment("Extra drop on top of rain cooling during a thunderstorm, in Celsius.")
                .defineInRange("thunder_extra_cooling_c", 2.0, 0.0, 20.0);

        builder.pop();

        builder.comment("Display settings").push("display");

        USE_FAHRENHEIT = builder
                .comment("Show Fahrenheit instead of Celsius.")
                .define("use_fahrenheit", false);

        HUD_X = builder.defineInRange("hud_x", 4, 0, 8000);
        HUD_Y = builder.defineInRange("hud_y", 4, 0, 8000);

        builder.pop();

        builder.comment(
                "Temperature damage settings.",
                "Player takes damage when temperature is <= cold_damage_threshold_c",
                "or >= heat_damage_threshold_c"
        ).push("temperature_damage");

        ENABLE_TEMPERATURE_DAMAGE = builder
                .comment("Enable player damage based on extreme temperatures.")
                .define("enable_temperature_damage", true);

        COLD_DAMAGE_THRESHOLD_C = builder
                .comment(
                        "Temperature in Celsius at which cold damage starts.",
                        "Default: 6°C (44°F). Below this, player takes damage."
                )
                .defineInRange("cold_damage_threshold_c", 6.0, -100.0, 50.0);

        HEAT_DAMAGE_THRESHOLD_C = builder
                .comment(
                        "Temperature in Celsius at which heat damage starts.",
                        "Default: 35°C (95°F). Above this, player takes damage."
                )
                .defineInRange("heat_damage_threshold_c", 35.0, 0.0, 100.0);

        TEMPERATURE_DAMAGE_MULTIPLIER = builder
                .comment(
                        "Damage multiplier per degree Celsius beyond the threshold.",
                        "Default: 0.05 = 0.05 half-hearts per degree over threshold."
                )
                .defineInRange("temperature_damage_multiplier", 0.05, 0.0, 1.0);

        TEMPERATURE_DAMAGE_INTERVAL_TICKS = builder
                .comment(
                        "How often to apply damage (in ticks). 20 ticks = 1 second.",
                        "Default: 20 ticks (once per second)."
                )
                .defineInRange("temperature_damage_interval_ticks", 20, 1, 100);

        TEMPERATURE_DAMAGE_MAX_PER_TICK = builder
                .comment(
                        "Maximum damage (in half-hearts) that can be applied per damage interval.",
                        "Default: 3.0 (1.5 hearts per second max)."
                )
                .defineInRange("temperature_damage_max_per_tick", 3.0, 0.5, 10.0);

        builder.pop();

        SPEC = builder.build();
    }
}
