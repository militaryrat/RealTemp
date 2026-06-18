# RealTemp — a real-physics temperature HUD for NeoForge 1.21.1

Shows a live temperature reading in the corner of your screen, calculated from:

- **Altitude** — real atmospheric lapse rate / geothermal gradient (1 block = 1 m)
- **Biome** — Minecraft's own biome temperature value, rescaled to °C
- **Time of day** — a day/night cycle, damped in humid biomes
- **Sunlight exposure** — caves and roofed areas skip the day/night swing entirely,
  the same way real caves stay at a stable temperature year-round

It's a client-only mod (just a HUD overlay), so it works in singleplayer or on
any server, even one that doesn't have it installed.

## The physics model

```
temperature(°C) = biomeBaseline + altitudeEffect + diurnalSwing − weatherCooling
```

**1. Biome baseline.** Minecraft assigns every biome a unitless "temperature"
value, roughly -0.5 (frozen ocean) to 2.0 (desert). We rescale that onto a
Celsius range that roughly matches each biome's real-world counterpart:

```
biomeBaseline = -5 + (biomeTemp × 20)
```

(desert ≈ 35°C, plains ≈ 11°C, snowy plains ≈ -5°C, frozen ocean ≈ -15°C)

**2. Altitude.** This is the part that matters most for your 1000-block
mountains. Above your world's sea level, we apply the real **environmental
lapse rate** — the average rate the atmosphere cools with height, about
6.5°C per 1000 m:

```
altitudeEffect = -0.0065 °C/block × (Y - seaLevel)      [above sea level]
```

So a 1000-block peak comes out about 6.5°C colder than the base of the
mountain — exactly the real-world rule of thumb. Below sea level (caves,
mineshafts), we instead apply the real **geothermal gradient**, about
25–30°C per 1000 m of depth, so deep mines run slightly warmer:

```
altitudeEffect = +0.025 °C/block × (seaLevel - Y)        [below sea level]
```

**3. Diurnal (day/night) swing.** A cosine wave peaking in the
mid-afternoon (real weather stations see peak temperature a few hours after
solar noon, not at noon itself, because the ground keeps radiating heat it
absorbed earlier). The swing's amplitude shrinks in humid biomes, since
humid air and cloud cover resist temperature swings in reality:

```
diurnalSwing = amplitude × cos(2π × (hour - 15) / 24)
amplitude    = 8°C × (1 - downfall × 0.6)
```

**4. Sky exposure.** All of step 3, plus rain/thunder cooling, is *only*
applied where `canSeeSky()` is true. Enclosed caves get neither — they sit
at a flat, stable temperature, just like real caves do (a real cave's
temperature tracks the local annual average, basically unaffected by the
day/night cycle on the surface).

**5. Weather.** While rain or snow is actually falling on your block, you
lose another 3°C; thunderstorms take off 2°C more.

Every one of these constants lives in the config file and can be retuned —
see "Configuration" below.

### Caveats

This is a stylized model, not a fluid-dynamics simulation — there's no wind,
no humidity transport, no pressure systems, and the lapse rate is a flat
constant rather than the real atmosphere's variable profile. The point was to
use *real, citable physical constants* (lapse rate, geothermal gradient,
diurnal phase lag) rather than made-up numbers, while staying simple enough
to run every frame.

## Setup

You'll need the official NeoForge template, then drop these files in —
this keeps you on a known-good Gradle/wrapper setup rather than one I hand
assembled blind.

1. **Get the template.** Download/clone the official MDK for 1.21.1:
   `https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle`
   (click "Use this template" on GitHub, or download the ZIP).

2. **Copy these files into the template, overwriting where they already
   exist:**
   - `src/main/resources/META-INF/neoforge.mods.toml` → replace the
     template's copy with the one in this package.
   - `src/main/java/com/tempmod/realtemp/*.java` → copy the whole folder
     into the template's `src/main/java/`.

3. **Edit `gradle.properties`** (in the template) and change just these
   lines — leave `minecraft_version` / `neo_version` alone, since those
   need to match whatever the template ships with:
   ```
   mod_id=realtemp
   mod_name=RealTemp
   mod_license=MIT
   mod_version=1.0.0
   mod_group_id=com.tempmod
   mod_authors=YourNameHere
   mod_description=Real-physics temperature HUD based on altitude, biome, time of day, and sunlight.
   ```

4. **Open the project in IntelliJ IDEA or Eclipse** (both have built-in
   Gradle support) and let it sync — first sync downloads NeoForge and
   decompiles Minecraft, which can take a while.

5. **Run it**: use the `runClient` Gradle task (or the generated "Client"
   run configuration in your IDE). You should see a temperature readout
   appear in the top-left corner once you're in a world.

6. **Build a distributable jar** with `gradlew build` — the output lands in
   `build/libs/`.

## Configuration

After the first run, edit `.minecraft/config/realtemp-client.toml` (every
constant from the model above is there, with comments) and use `/reload`
or just restart to pick up changes — no rebuild needed.

Common tweaks:
- `use_fahrenheit=true` — show °F instead of °C
- `lapse_rate_c_per_block` — make mountains feel colder/milder
- `hud_x` / `hud_y` — reposition the readout if it overlaps another mod's HUD
