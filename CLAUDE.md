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
- `BlockPlacer.INSTANCE`: the enabled/disabled toggle. It is in memory only and is not persisted. `canContinueWithout(missing, player)` applies the `MissingBlockMode` setting: in CONTINUE mode it warns once per missing block and keeps placing with the rest (CONTINUE_QUIET skips the warning), and it turns placement off when nothing is left.
- `BlockPlacerConfig.INSTANCE`: selected blocks (`Identifier → weight`, default weight 100), named presets, and the active preset. It is persisted as JSON by Gson to `<configDir>/rnd-block-placer.json`. It is loaded in `RandomBlockPlacerClient.onInitializeClient`, and saved explicitly with `save()`.
- `KeyBindings`: B opens `BlockSelectionScreen`, and J toggles `BlockPlacer`. Both are polled on `ClientTickEvents.END_CLIENT_TICK`.
- `HudRndBlockPlacer` shows the enabled indicator with the loaded preset name. Its position comes from the `HudPosition` setting.

### Selection screen (`client/screen`)
`BlockSelectionScreen` is built from vanilla widgets (`Button`, `CycleButton`, `EditBox`, `Tooltip`). It shows three columns side by side: the Selection panel, the clickable inventory, and the Presets panel.
- `SelectionLayout.compute(width, height)` holds all the geometry. When the three columns cannot fit, even with 18 px slots (`split == false`), it falls back to the inventory plus a single right-hand panel with Selection/Presets tab buttons. Slot size (18 to 28) and panel width shrink so the screen fits down to Minecraft's minimum GUI size. `slotX`, `slotY` and `slotAt` are shared by rendering and click handling.
- `init()` runs again on every resize and tab switch through `rebuildWidgets()`, so it only rebuilds widgets. Anything that must survive a rebuild (working state, current tab, text being typed, scroll position) is stored in fields created in the constructor.
- `BlockSelectionScreenState` is the **working copy** of the selection and the loaded preset. `apply()` writes it to `BlockPlacerConfig`. Whether that happens on close or only through the Apply button depends on `BlockPlacerConfig.saveMode` (`SaveMode.ON_CLOSE` or `MANUAL`), which is set in `SettingsScreen`.
- The UI shows and edits **percentages**, but config and presets still store integer weights. `BlockSelectionScreenState.setPercent` rescales the other blocks to keep their proportions, storing weights in hundredths of a percent. `addBlock` gives a new block the average weight so it gets an even share.
- Preset operations in `PresetsTab` (create, overwrite, delete) write to the config **immediately**. Only the selection and the loaded preset belong to the working state. Overwrite and delete need a second click to confirm (the `Armed` state).
- `tab/SidebarTab` is the base class for `SelectionTab` and `PresetsTab`. It provides the scrollable fixed-height row list. Each tab creates its widgets in `init(area, addWidget)` and refreshes their labels and enabled state every frame in `updateWidgets()`. The screen loops over `visibleTabs()` (both panels when split, the active tab otherwise) for rendering and input.
- `InventoryGrid` draws the slots and item tooltips. Panels are drawn in `extractBackground`, which runs in an earlier render stratum, so they stay behind the widgets.
- The placement toggle acts directly on `BlockPlacer.INSTANCE`, like the J key. It is not part of the working state.
- Input is routed in this order: vanilla widgets first, then the visible panels' lists, then inventory slot clicks.
- `BlockSelectionScreenConstants`: all layout sizes and colors. Put new layout numbers here.
- `PanelStyle` draws the shared look (panels, panel titles, header title). Every screen of the mod uses it.
- `SettingsScreen` uses the same header, panel and footer structure. Enum settings implement `config/TranslatableOption`: labels and tooltips come from `<prefix>.<value>` and `<prefix>.<value>.tooltip`, and the row description from `<prefix>.description`. To add one, add the field to `BlockPlacerConfig` (read it with `parseEnum`) and call `addOptionRow(values, current, setter)` in `init()`. The panel height and control positions are computed from the rows.

### Preset sharing (`client/share`)
A vanilla server relays nothing but chat, so sharing goes through `/msg`, or public chat (`sendChat`) from the "Send to public chat" button.
- `PresetCodec` is pure. It encodes `name\nid=weight…` (with the `minecraft:` prefix stripped) as Deflate, then URL-safe Base64. It splits the result into lines of the form `[rbp1 <msgId> <i>/<n>] <data>`, sized to fit the 256-char command limit. `main()` runs a round-trip self-check: `java -ea PresetCodec.java`.
- `PresetShare` queues the `/msg` commands, sending one per second so the server does not kick for spam.
  - It hides incoming chunks through `ClientReceiveMessageEvents` ALLOW_CHAT/ALLOW_GAME, then reassembles them.
  - When a share is complete, it posts a chat line with an `[Import]` button. The button runs the client command `/rbp import <token>`. A name conflict gets a ` (n)` suffix.
  - It ignores its own echoes using the msgIds it sent.
- Clipboard: "Copy code" in `SharePlayersScreen` copies the unsplit `PresetCodec.encodeData` code. "Import from clipboard" in `PresetsTab` imports it through `PresetShare.importFromClipboard`, which also accepts a single-chunk chat line. Results are shown as toasts, because chat is hidden while a screen is open.
- `SharePlayersScreen` lets the player pick online players. The Share buttons in `SelectionTab` and `PresetsTab` open it through the `openShare` callback of `BlockSelectionScreen`.

### Resources
User-facing strings are translation keys in `src/client/resources/assets/rnd-block-placer/lang/en_us.json`, for example `button.rnd-block-placer.*`, `tooltip.rnd-block-placer.*`, and `label.rnd-block-placer.*`. The screen uses no custom textures.

## Conventions
- Code comments are short `//` lines above classes and methods, describing intent. Follow that style.
- `net/minecraft/client/player/LocalPlayer.class` at the repo root is a stray committed file. It is not part of the build.
