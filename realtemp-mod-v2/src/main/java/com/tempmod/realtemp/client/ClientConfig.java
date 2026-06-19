package com.tempmod.realtemp.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Pure display settings. Everything that affects the physics lives in CommonConfig. */
public class ClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue USE_FAHRENHEIT;
    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("display");
        USE_FAHRENHEIT = builder.comment("Show Fahrenheit instead of Celsius.")
                .define("use_fahrenheit", false);
        HUD_X = builder.defineInRange("hud_x", 4, 0, 8000);
        HUD_Y = builder.defineInRange("hud_y", 4, 0, 8000);
        builder.pop();

        SPEC = builder.build();
    }
}
