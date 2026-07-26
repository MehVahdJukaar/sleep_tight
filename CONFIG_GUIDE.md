# Moonlight Config Builder — quick hookup guide

How to wire up a config using Moonlight's `ConfigBuilder` (the loader-independent config + native config screen). Everything lives in `net.mehvahdjukaar.moonlight.api.platform.configs`.

## 1. The holder pattern

Configs are declared once in a `static {}` block and exposed as `public static final Supplier<T>` fields. `define(...)` returns the value handle (a `Supplier`); read it later with `.get()`.

```java
public class MyConfigs {
    public static final ModConfigHolder SPEC;
    public static final Supplier<Integer> SOME_VALUE;

    static {
        ConfigBuilder builder = ConfigBuilder.create("mymod", ConfigType.CLIENT); // or COMMON_SYNCED

        builder.push("category");
        SOME_VALUE = builder
                .comment("What this does. Shows as the tooltip/description in the screen.")
                .define("some_value", 8, 1, 32); // default, min, max
        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    // call from your mod init so the class loads and the static block runs
    public static void init() {}
}
```

`ConfigType.CLIENT` = per-client, not synced. `ConfigType.COMMON_SYNCED` = server-authoritative, synced to clients.

## 2. Categories & subcategories

`push("name")` opens a category, `pop()` closes it. Nest freely; each level is a section in the file and a sub-screen in the UI.

```java
builder.push("mirror");
  // ... options ...
  builder.push("recursion");   // subcategory: mirror.recursion.*
    // ... options ...
  builder.pop();
builder.pop();
```

## 3. Value definers

`define(...)` is overloaded. Each returns a `Supplier` of the matching type:

```java
builder.define("flag", true);                       // boolean
builder.define("count", 8, 1, 32);                  // int (default, min, max)
builder.define("ratio", 2.0, 1.0, 16.0);            // double
builder.define("mode", MyEnum.DEFAULT);             // enum
builder.define("list", List.of("a", "b"));          // string list
```

There are many more (`defineColor`, `defineSlider`, `defineItem`, `defineBlock`, `defineRange`, `defineVec3`, `defineObject(codec)`, `defineRegex`, ...). `comment(...)` can go before *or* after the `define`.

## 4. Icons

**Icons belong on categories and features only, never on leaf options.** A row like `outline` or `limit_palette` is a plain setting - it reads fine as a label, and dressing every one of them up just adds noise and makes the rows that *do* navigate somewhere harder to spot. `icon(...)` technically decorates the next `push` **or** `define`, but the `define` case is there for the rare hero option, not for general use.

Most of the time you don't call it at all, because features infer their own:

| what | icon |
|---|---|
| `feature("creeper_drop")` | auto, `modid:creeper_drop` |
| `pushFeature("mirror")` / `push` + `mainFeature()` | auto, `modid:mirror` — the gate row and the category row share it |
| plain `push("color_settings")` | **none** — this is the one place you actually need `icon(...)` |
| plain `define("outline", ...)` | none, and leave it that way |

A bare string uses your mod namespace; it resolves lazily client-side as an item/block, so a name that isn't real simply shows no icon. That's also why the inferred `modid:<name>` is safe: a feature whose name isn't an item just renders bare.

```java
builder.icon("minecraft:red_dye").push("color_settings"); // plain category: needs one
builder.icon("cassette").feature("creeper_drop", true);   // only to override the inferred modid:creeper_drop
```

## 5. Feature toggles (the checkmark switches)

A "feature" is a boolean that renders as a ✓/✗ switch with an icon, and **gates** things. The icon auto-infers from the name (`modid:name`) unless you set one with `icon(...)`, and `mainFeature()` mirrors it onto the category row too - so a gated category never needs an explicit icon.

```java
// leaf feature: a single ✓/✗ toggle
CREEPER_DROP = builder.icon("cassette").comment("...").feature("creeper_drop", true);

// category master toggle: gates every option under the category (they gray out when off)
builder.push("mirror");
builder.comment("...");
MIRROR_ENABLED = builder.mainFeature();     // creates the "enabled" gate for this category
  MAX_SIZE = builder.comment("...").define("max_connected_size", 8, 1, 24);
builder.pop();

// shortcut for "open a gated category": push(name) + mainFeature()
SCREEN_EFFECTS = builder.pushFeature("screen_effects");
```

Feature suppliers are **effective** = own value AND every ancestor gate, composed at read time (toggling a parent never rewrites stored child values). Non-feature children keep returning their raw `.get()` — the gate only grays them in the UI, so still guard them in code (`if (SCREEN_EFFECTS.get()) ...`).

### When is something actually a feature?

A feature is a switch that **disables a meaningful chunk of the mod** — a whole system of content or behavior that a user would deliberately turn off. It is not just "any boolean". Plumbing toggles and preference settings stay plain `define(...)` booleans even though they're on/off.

- **Not features** (plain `define`): the creative tab toggle, server/client asset generation mode, tooltip toggles, tag generation, the version-check packet. These tweak how the mod behaves; they don't switch off a body of content.
- **Features** (`feature`/`mainFeature`): per-entry / per-type enables (the `entries` config that disables a whole wood type or a specific generated block). Turning one off removes real content, recipes, and tab items — that's what the ✓/✗ switch is for.

Rule of thumb: if flipping it makes blocks/items/recipes disappear, it's a feature; if it just changes a setting, it's a `define`.

## 6. Reload markers

Tell the user (and the screen badge) that a change needs more than a hot reload. Call **immediately before the `define`** it applies to; it's a single-shot flag consumed by the next value.

```java
builder.gameRestart().comment("...").define("update_fps", 10.0, 1, 60);  // needs full restart
builder.worldReload().comment("...").define("max_connected_size", 8, 1, 24); // needs world reload
builder.affectsDynamicPacks().worldReload().comment("...").define(...);      // also invalidates pack cache
```

Pick the tier honestly:
- **GAME_RESTART** — value is read once and cached at startup (e.g. baked into a `Suppliers.memoize`'d object). Nothing re-reads it.
- **WORLD_RELOAD** — value is baked at world/data load (recipe conditions, loot, multiblock structure cached in world data).
- **NONE** (default) — read live each use, applies immediately.

### Values a dynamic pack reads

`affectsDynamicPacks()` is the same kind of one-shot flag, and it's **orthogonal** to the reload tier — it says nothing about *when* the value applies, only that the generated pack is stale once it changes. Mark every value your `DynamicResourcePack`/`DynamicDataPack` generator reads:

```java
builder.affectsDynamicPacks().worldReload().comment("...").define("fancy_recipes", true);
```

Without it, a `GlobalCachedStrategy` pack (`GlobalCachedFolderStrategy` / `GlobalCachedZipStrategy`) hands back the previously generated files: those strategies fingerprint the *loaded resource packs* to decide whether to regenerate, and a config edit doesn't move that fingerprint. The symptom is the classic "I changed the config, reloaded, nothing happened, but it works after deleting the cache folder". With the flag, Moonlight deletes the cache fingerprint and forces a regen as soon as the value actually changes — on the config-reload event, and on `manuallySetValue`. Non-cached (regenerate-every-launch) strategies don't care either way, so the flag is harmless there.

**Put the value in the config type that matches the pack it feeds.** The invalidated pack type is derived from the config, not from the value: `ConfigType.CLIENT` → `CLIENT_RESOURCES`, anything else → `SERVER_DATA`. A value driving generated *assets* declared in the common config invalidates the data cache and leaves the stale assets in place, which looks exactly like the flag not working.

## 7. Gating recipes / loot / creative tabs by a config

Moonlight exposes a simple named recipe condition. Register one flag handler, then add the condition to any recipe/advancement JSON.

```java
RegHelper.registerSimpleRecipeCondition(res("flag"), s -> switch (s) {
    case "mirror"       -> MyConfigs.isMirrorEnabled();
    case "picture_tape" -> MyConfigs.isPictureTapeEnabled();
    default             -> true;
});
```

```json
{
  "fabric:load_conditions": [ { "condition": "mymod:flag", "flag": "picture_tape" } ],
  "neoforge:conditions":    [ { "type": "mymod:flag",      "flag": "picture_tape" } ],
  "type": "minecraft:crafting_shaped",
  "...": "..."
}
```

Recipe conditions run at data load, so the backing config should be **WORLD_RELOAD + affectsDynamicPacks**. Gate creative-tab entries and loot in code with the same helper.

## 8. Wiring up the config screen

**You never write a config screen class.** The screen is built from the `ConfigBuilder` tree at runtime, so a `MyModConfigScreen extends Screen` in your source tree is always a mistake - delete it. Options, categories, icons, tooltips, feature switches and reload badges all come from the `define`/`push`/`icon`/`comment` calls. `SPEC.makeScreen(parent)` hands you the finished screen; `MoonlightConfigSelectScreen.create(modId, parent, background)` hands you the picker over *every* config a mod registered (client + common + ...), and returns null if the mod has none.

### NeoForge — nothing to do

Moonlight walks every tracked `ModConfigHolder` on `FMLLoadCompleteEvent` and registers an `IConfigScreenFactory` for the owning mod container, so the Config button in the mod list already opens the native screen (falling back to NeoForge's `ConfigurationScreen`). Don't register your own extension point; you'd just fight it. The only requirement is that your config class is actually loaded by then - a `MyConfigs.init()` call from the mod constructor is enough.

If the user turns Moonlight's own `custom_config_screen` off, Moonlight registers nothing and the loader's screen (or Configured, if installed) takes over. That's the intended escape hatch.

### Fabric — one small ModMenu class

ModMenu only reads a per-mod entrypoint, so Moonlight cannot register this for you. Add the entrypoint to `fabric.mod.json`:

```json
"entrypoints": {
  "modmenu": [ "net.mehvahdjukaar.mymod.integration.platform.ModMenuCompat" ]
}
```

and, for a mod with a single config:

```java
public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClientConfigs.CONFIG_SPEC::makeScreen;
    }
}
```

With more than one config, return the picker instead so the user can reach all of them:

```java
return parent -> MoonlightConfigSelectScreen.create(MyMod.MOD_ID, parent, ModTextures.CONFIG_BACKGROUND);
```

ModMenu goes in as `modCompileOnly` and stays out of `depends` - the entrypoint is simply ignored when it isn't installed. Keep the version in step with the MC version (11.x for 1.21.1); the ancient 4.x coordinate still compiles because the API barely moved, which makes a stale value easy to miss.

## Gotchas (learned the hard way)

- **`worldReload()`/`gameRestart()` must precede a `define`, never a `push`.** On NeoForge the flag is forwarded straight into Forge's spec, which throws `Dangling restart value` if the next call is a `push`. So for a gated category, use `push(name)` → set the flag → `mainFeature()` rather than putting the flag before `pushFeature(name)`.
- **`SPEC.manuallySetValue(handle, value)` needs the raw `define` handle**, not a feature's effective supplier (that's a wrapped lambda and will throw). Keep a raw `Supplier` reference if you need to set a value programmatically.
- **Changing keys/category paths resets that value** to default on next load (old key is ignored). Fine in dev; be deliberate for released mods.
- **No manual lang needed** — readable names and comment descriptions are auto-registered. Hand-written `modid.configuration.*` keys are orphaned if you rename.
