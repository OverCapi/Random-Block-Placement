# Random Block Placement — Codebase Overview

A Minecraft Fabric mod that replaces block placement with a weighted random selection from a user-defined pool. When you right-click to place a block, the mod automatically swaps to a randomly chosen block from your inventory based on configured weights, then restores your original slot.

---

## Architecture

The mod is split into **two sides** following Fabric conventions:

```
src/
├── main/java/       # Common & server-side (ModInitializer)
└── client/java/     # Client-only (ClientModInitializer, rendering, mixins)
```

### Data flow (placement cycle)

```
User right-clicks
    │
    ▼
MultiPlayerGameModeMixin.rbp$onUseItemOnHead()
    │  ┌─ Check mod enabled & blocks selected
    │  ├─ rbp$isBlocksMissing() → warn & disable if missing
    │  ├─ rbp$getEligibleSlots() → scan inventory for selected blocks
    │  ├─ rbp$getRandomSlotEntry() → weighted random pick
    │  └─ Swap to target slot (hotbar or inventory swap)
    ▼
Minecraft places the block
    │
    ▼
MultiPlayerGameModeMixin.rbp$onUseItemOnReturn()
    │  └─ Restore original slot & sync with server
```

### Data flow (configuration)

```
BlockSelectionScreen (GUI)
    │  User selects blocks & sets weights
    ├─ BlockSelectionScreenState (working copy)
    ▼
Save button → BlockPlacerConfig.save() → rnd-block-placer.json
                                ▲
BlockPlacerClient.onInitializeClient() → BlockPlacerConfig.load()
```

---

## Package: `capi.rnd_block_placer` (main)

### `RandomBlockPlacer.java`

- **Role:** Mod entry point (`ModInitializer`)
- **Fields:**
  - `MOD_ID` — `"rnd-block-placer"`, used for all registrations
  - `LOGGER` — SLF4J logger
- **Methods:**
  - `onInitialize()` — Logs mod startup (Fabric lifecycle)
  - `id(String path)` — Convenience factory for namespaced `Identifier`

### `BlockPlacerHandler.java`

- **Role:** Placeholder for future server-side event handling. Currently empty.

---

## Package: `capi.rnd_block_placer.client`

### `RandomBlockPlacerClient.java`

- **Role:** Client-side entry point (`ClientModInitializer`)
- **Methods:**
  - `onInitializeClient()` — Chains three startup calls:
    1. `BlockPlacerConfig.INSTANCE.load()` — load persisted selections
    2. `Bind.load()` — register keybindings
    3. `HudRndBlockPlacer.init()` — register HUD element

---

## Package: `capi.rnd_block_placer.client.blockPlacer`

### `BlockPlacer.java`

- **Role:** Singleton service controlling the random placement mode
- **Pattern:** Singleton via `INSTANCE` field
- **State:**
  - `isEnabled` — whether random placement is active
  - `blockPlacerConfig` — reference to the shared config
- **Methods:**
  - `isEnabled()` / `enable()` / `disable()` / `toggleBlockPlacement()` — state management

---

## Package: `capi.rnd_block_placer.client.config`

### `BlockPlacerConfig.java`

- **Role:** Persistence layer for block selection and weights. Reads/writes a JSON file at `<config-dir>/rnd-block-placer.json`.
- **Fields:**
  - `DEFAULT_WEIGHT = 100` — default weight assigned to newly selected blocks
  - `selectedBlocks` — `HashMap<Identifier, Integer>` mapping block IDs to weights
- **Methods:**
  - `getSelectedBlocksKey()` / `getSelectedBlocks()` / `getSelectedBlockWeight()` — read access
  - `setSelectedBlocks(...)` / `resetSelectedBlocks()` — write access
  - `save()` — serializes `selectedBlocks` to JSON and writes to disk
  - `load()` — parses JSON, reconstructs the map (handles `namespace:path` keys)
  - `getConfigPath()` — resolves the config file path via FabricLoader

**JSON format:**
```json
{
  "selectedBlocks": {
    "minecraft:stone": 100,
    "minecraft:dirt": 50
  }
}
```

---

## Package: `capi.rnd_block_placer.client.hud`

### `HudRndBlockPlacer.java`

- **Role:** Registers a HUD element that shows "§aENABLE" in the top-right corner when random placement is active.
- **Methods:**
  - `init()` — Registers via `HudElementRegistry.addFirst()`. The lambda checks `BlockPlacer.INSTANCE.isEnabled()` before rendering, making it a lightweight passive indicator.

---

## Package: `capi.rnd_block_placer.client.keymapping`

### `Bind.java`

- **Role:** Singleton managing all keybindings via Fabric API's `KeyMappingHelper`.
- **Default keys:**
  - **B** — Opens the `BlockSelectionScreen` GUI
  - **J** — Toggles random placement on/off
- **Architecture:** Registers an `END_CLIENT_TICK` handler. Each tick it consumes any clicks since the last tick, avoiding event-driven issues.
- **Methods:**
  - `load()` — static no-op (registration happens in the private constructor via `register()`)
  - `register()` — hooks `ClientTickEvents.END_CLIENT_TICK`
  - `onClientTick(Minecraft)` — checks both keybindings each tick
  - `toggleSelectionScreen(Minecraft)` — opens the screen via `setScreenAndShow()`

---

## Package: `capi.rnd_block_placer.client.screen`

### `BlockSelectionScreen.java`

- **Role:** Main GUI for selecting blocks and configuring weights.
- **Extends:** `Screen`
- **Architecture:** Follows a **State / Renderer separation** — the screen owns an `EditBox` for weight input but delegates inventory rendering to `BlockSelectionScreenRenderer`.
- **Key behaviors:**
  - **Left-click** on a block item → toggles selection (adds with `DEFAULT_WEIGHT` or removes)
  - **Shift+click** on a block item → opens the weight editor (`EditBox`)
  - **Save** → writes working state to `BlockPlacerConfig` and persists to disk
  - **Reset** → clears working state
  - **Enter** in the weight editor → confirms the weight (values ≤ 0 remove the block)
  - **Escape** in the weight editor → cancels editing

### `BlockSelectionScreenRenderer.java`

- **Role:** Renders the custom screen elements on top of the vanilla inventory background.
- **Layers drawn (in order):**
  1. `drawBackground()` — vanilla inventory container texture
  2. `drawSelectedList()` — left-side panel showing selected blocks sorted by weight descending, with percentage. Caps display to fit available space ("+ N more")
  3. `drawInventorySlots()` — inventory grid with:
     - **Green highlight** for selected blocks
     - **Weight overlay** (top-right of slot) for selected blocks
     - **Gray overlay** for non-block items (can't be selected)
     - **White hover** effect
- **Helper:** `findStackFor(Identifier, LocalPlayer)` — searches first 36 inventory slots for a matching block stack

### `BlockSelectionScreenState.java`

- **Role:** Working copy of the block selection, decoupled from the persisted config until the user hits "Save". This prevents accidental modification on cancel/close.
- **Fields:**
  - `workingWeights` — `HashMap<Identifier, Integer>` (the mutable working copy)
  - `workingEnableRndPlacement` — working toggled state
  - `isOpen` — whether the screen is active
- **Methods:**
  - `init()` — copies from `BlockPlacerConfig.getSelectedBlocks()` and `BlockPlacer.isEnabled()`
  - `reset()` — clears all state
  - `addWeight()` / `toggleRndPlacement()` / `enableRndPlacement()` / `disableRndPlacement()` — mutations

### `BlockSelectionScreenConstant.java`

- **Role:** Compile-time constants for layout, textures, and colors.
- **Notable values:**
  - `IMAGE_W = 176`, `IMAGE_H = 166` — standard vanilla inventory container size
  - `INVENTORY_ROW = 4`, `INVENTORY_COL = 9` — 3 main rows + 1 hotbar
  - `SLOT_SIZE = 18` — standard slot pixel size
  - Colors: `GREEN_SEL`, `GRAY_OVERLAY`, `HOVER`

---

## Package: `capi.rnd_block_placer.client.mixin`

### `MultiPlayerGameModeAccessor.java`

- **Role:** Mixin accessor interface that exposes `MultiPlayerGameMode.ensureHasSentCarriedItem()` (a private method).
- **Used by:** `MultiPlayerGameModeMixin` to sync the held item with the server after restoring the original slot.

### `MultiPlayerGameModeMixin.java`

- **Role:** Core mechanic — this is where the random block swapping happens. It mixes into `MultiPlayerGameMode.useItemOn()` at `HEAD` and `RETURN`.
- **State fields (`@Unique`):**
  - `rbp$swapped` — whether a swap is in progress
  - `rbp$originalSlot` — the player's hotbar slot before the swap
  - `rbp$invSwapped` / `rbp$invSlot` — tracks whether a main-inventory swap was performed and which slot was involved
- **Internal record:** `SlotEntry(int slot, ItemStack stack, int weight)`
- **Private methods:**
  - `rbp$getEligibleSlots(Inventory)` — scans all inventory slots for selected blocks, returns a map of block ID → SlotEntry
  - `rbp$getRandomSlotEntry(HashMap, RandomSource)` — weighted random selection using cumulative weight algorithm
  - `rbp$isBlocksMissing(BlockPlacerConfig, LocalPlayer)` — checks all selected blocks are present in inventory; sends a chat warning with names of missing blocks
- **HEAD inject (`rbp$onUseItemOnHead`):**
  1. Guards: main hand only, mod enabled, blocks selected
  2. Check for missing blocks → disable mod if any are missing
  3. Collect eligible slots, pick one randomly by weight
  4. If the block is in the hotbar → just switch selected slot
  5. If the block is in the main inventory → perform a `ContainerInput.SWAP` to bring it to the current hotbar slot
- **RETURN inject (`rbp$onUseItemOnReturn`):**
  1. Swap back any inventory slot that was moved
  2. Restore the original hotbar slot
  3. Call `ensureHasSentCarriedItem()` to sync with the server

---

## Dependency graph

```
RandomBlockPlacerClient
  ├── BlockPlacerConfig (load/save JSON)
  ├── Bind → BlockPlacer, BlockSelectionScreen
  │              │              │
  │              ▼              ▼
  │        BlockPlacer    BlockSelectionScreen
  │              │              ├── BlockSelectionScreenState
  │              │              └── BlockSelectionScreenRenderer
  │              │
  ▼              ▼
MultiPlayerGameModeMixin
  ├── BlockPlacer (isEnabled, getBlockPlacerConfig)
  ├── BlockPlacerConfig (getSelectedBlocksKey, getSelectedBlockWeight)
  └── MultiPlayerGameModeAccessor (sync carried item)
```

---

## Design decisions

1. **Weighted random over uniform random** — Users assign weights to blocks (e.g., stone: 100, dirt: 50). Stone will be placed twice as often. This allows fine-grained control without removing/add blocks.

2. **Working state before save** — `BlockSelectionScreenState` acts as a scratchpad. Changes don't affect the live config until "Save" is pressed, preventing accidental disruption during placement.

3. **Inventory swap for non-hotbar blocks** — The mod handles blocks outside the hotbar by using Minecraft's `ContainerInput.SWAP` packet, which temporarily brings the item to the hotbar slot. This avoids server desync.

4. **Singleton pattern for core services** — `BlockPlacer`, `BlockPlacerConfig`, and `Bind` all use singletons since they represent global game state.
