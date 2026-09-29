# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Random Block Placement (mod id `rnd-block-placer`) is a **client-side** Fabric mod for Minecraft 26.3. When enabled, each block placement swaps in a weighted-random block from the player's inventory. It must work on vanilla servers, so all behavior is done through normal client→server actions (hotbar slot changes and inventory swap clicks). It never uses custom packets or server-side logic.

Toolchain: Java 25, Fabric Loom (`net.fabricmc.fabric-loom`), Fabric API. Versions live in `gradle.properties`. There is no test source set.

## Commands

```bash
./gradlew build          # compile + jar (what CI runs); output in build/libs/
./gradlew runClient      # launch a dev Minecraft client (game dir: run/, gitignored)
./gradlew genSources     # decompile Minecraft sources for browsing MC APIs
```

CI (`.github/workflows/build.yml`) runs `./gradlew build` on pushes and PRs to `main`. Pushing a `v*` tag triggers `release.yml`, which builds and attaches `build/libs/*.jar` to a GitHub release. Bump `mod_version` in `gradle.properties` before tagging.

## Architecture

Loom `splitEnvironmentSourceSets()` is on, so there are two source sets:
- `src/main`: only the common `ModInitializer` (`RandomBlockPlacer`, which holds `MOD_ID` and `LOGGER`), plus `fabric.mod.json` and an empty common mixin config.
- `src/client`: all real functionality. Client code may reference `main`, but not the reverse.

Minecraft 26.x ships unobfuscated with Mojang names, so APIs are used directly (for example `MultiPlayerGameMode`, `GuiGraphicsExtractor`, `Screen.extractRenderState`). There is no mappings layer.

### Placement flow (the core mechanic)
`client/mixin/MultiPlayerGameModeMixin` injects at HEAD and RETURN of `MultiPlayerGameMode.useItemOn`:
1. **HEAD**: if `BlockPlacer.INSTANCE` is enabled and the hand is the main hand, check for missing selected blocks. If any are missing, send a chat warning and auto-disable. Otherwise pick a slot through `RandomBlockSelector`.
   - If the chosen slot is in the hotbar, it calls `setSelectedSlot`.
   - If it is in the main inventory, it runs a `ContainerInput.SWAP` through `handleContainerInput` into the current hotbar slot.
2. **RETURN**: undo the inventory swap if one happened, restore the original selected slot, and call `ensureHasSentCarriedItem` (exposed by `MultiPlayerGameModeAccessor`) to resync with the server.

State between HEAD and RETURN is kept in `@Unique rbp$*` fields. Mixin members use the `rbp$` prefix.

`RandomBlockSelector` holds the selection logic and does not touch game state. It scans inventory slots `0..INVENTORY_SIZE`, keeps one slot per selected item id, and picks one with a cumulative-weight roll.

### Singletons / state
- `BlockPlacer.INSTANCE`: the enabled/disabled toggle. It is in memory only and is not persisted.
- `BlockPlacerConfig.INSTANCE`: selected blocks (`Identifier → weight`, default weight 100), named presets, and the active preset. It is persisted as JSON by Gson to `<configDir>/rnd-block-placer.json`. It is loaded in `RandomBlockPlacerClient.onInitializeClient`, and saved explicitly with `save()`.
- `KeyBindings`: B opens `BlockSelectionScreen`, and J toggles `BlockPlacer`. Both are polled on `ClientTickEvents.END_CLIENT_TICK`.
- `HudRndBlockPlacer` shows the enabled indicator.

### Selection screen (`client/screen`)
`BlockSelectionScreen` is built from vanilla widgets (`Button`, `CycleButton`, `EditBox`, `Tooltip`). It shows the clickable inventory on the left, and a sidebar with two tabs on the right.
- `SelectionLayout.compute(width, height)` holds all the geometry. Slot size (18 to 28) and sidebar width shrink so the screen fits down to Minecraft's minimum GUI size. `slotX`, `slotY` and `slotAt` are shared by rendering and click handling.
- `init()` runs again on every resize and tab switch through `rebuildWidgets()`, so it only rebuilds widgets. Anything that must survive a rebuild (working state, current tab, text being typed, scroll position) is stored in fields created in the constructor.
- `BlockSelectionScreenState` is the **working copy** of the selection and the loaded preset. `apply()` writes it to `BlockPlacerConfig`. Whether that happens on close or only through the Apply button depends on `BlockPlacerConfig.saveMode` (`SaveMode.ON_CLOSE` or `MANUAL`), which is set in `SettingsScreen`.
- Preset operations in `PresetsTab` (create, overwrite, delete) write to the config **immediately**. Only the selection and the loaded preset belong to the working state. Overwrite and delete need a second click to confirm (the `Armed` state).
- `tab/SidebarTab` is the base class for `SelectionTab` and `PresetsTab`. It provides the scrollable fixed-height row list. Each tab creates its widgets in `init(area, addWidget)` and refreshes their labels and enabled state every frame in `updateWidgets()`.
- `InventoryGrid` draws the slots and item tooltips. Panels are drawn in `extractBackground`, which runs in an earlier render stratum, so they stay behind the widgets.
- The placement toggle acts directly on `BlockPlacer.INSTANCE`, like the J key. It is not part of the working state.
- Input is routed in this order: vanilla widgets first, then the active tab's list, then inventory slot clicks.
- `BlockSelectionScreenConstants`: all layout sizes and colors. Put new layout numbers here.

### Resources
User-facing strings are translation keys in `src/client/resources/assets/rnd-block-placer/lang/en_us.json`, for example `button.rnd-block-placer.*`, `tooltip.rnd-block-placer.*`, and `label.rnd-block-placer.*`. The screen uses no custom textures.

## Conventions
- Code comments are short `//` lines above classes and methods, describing intent. Follow that style.
- `net/minecraft/client/player/LocalPlayer.class` at the repo root is a stray committed file. It is not part of the build.
