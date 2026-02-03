# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

COI Client is a client-only Minecraft Fabric mod that implements a customizable ability system with HUD overlay. Players can bind up to 3 abilities, use them via keybindings (Z/X/C), and customize the visual interface. The mod communicates with a server-side component via hidden chat message prefixes.

**Environment:** Client-only (`"environment": "client"` in fabric.mod.json)
**Java Version:** 21
**Minecraft Version:** 1.21.11
**Fabric Loader:** 0.18.4
**Fabric API:** 0.141.3+1.21.11

## Build & Development Commands

### Building
```bash
./gradlew build
```
Output: `build/libs/coi-client-<version>.jar`

### Clean Build
```bash
./gradlew clean build
```

### Running in Development
```bash
./gradlew runClient
```

### Generate Sources
```bash
./gradlew genSources
```

## Architecture

### Core Components

**CircleOfImaginationClient** (`src/client/java/dev/ua/ikeepcalm/coi/client/CircleOfImaginationClient.java`)
- Main client orchestrator and singleton state manager
- Manages ability bindings, keybindings, and server communication
- Entry point: `onInitializeClient()`
- Static methods provide global access to ability state: `getAvailableAbilities()`, `getBoundAbility()`, `setBoundAbility()`

**AbilityHudOverlay** (`src/client/java/dev/ua/ikeepcalm/coi/client/hud/AbilityHudOverlay.java`)
- Coordinates rendering of the HUD overlay
- Manages 3 `AbilitySlotWidget` instances (one per ability slot)
- Registered via `HudRenderCallback.EVENT`

**AbilitySlotWidget** (`src/client/java/dev/ua/ikeepcalm/coi/client/hud/AbilitySlotWidget.java`)
- Renders individual ability slots with icons, cooldowns, keybinds, and effects
- Handles cooldown countdown based on elapsed real time (not ticks)
- Color-codes abilities by pathway (Fool=purple, Door=blue, Sun=yellow, Tyrant=cyan, Demoness=red, Priest=orange)

**ChatMessageMixin** (`src/client/java/dev/ua/ikeepcalm/coi/client/mixin/ChatMessageMixin.java`)
- Intercepts chat messages with special prefixes before they render
- Routes ability data and cooldown updates to handlers
- Mixin target: `net.minecraft.client.gui.hud.ChatHud.addMessage()`

### Configuration

**AbilityConfig** (`src/client/java/dev/ua/ikeepcalm/coi/client/config/AbilityConfig.java`)
- Persists ability bindings to `config/coi_abilities.json`
- Format: `{"ability1": "id - name", "ability2": null, "ability3": null}`

**HudConfig** (`src/client/java/dev/ua/ikeepcalm/coi/client/config/HudConfig.java`)
- Persists HUD settings to `config/coi_hud.json`
- 8 settings: position (hudX, hudYOffset), size (slotSize, slotSpacing), scale (hudScale), visual toggles (enabled, showKeybinds, showAbilityNames, showGlowEffect)

### Screen Components

**AbilityBindingScreen** - Main UI for binding abilities to slots
**AbilityDropdownWidget** - Custom dropdown with scroll support (max 5 visible items)
**HudSettingsScreen** - HUD customization with sliders, text fields, and 4 presets (Default/Compact/Large/Minimal)

## Client-Server Communication Protocol

Communication uses hidden chat message prefixes that are intercepted before rendering:

**Client → Server:**
- `_0_0_2_2_rUSE:abilityId` - Execute ability
- `_0_0_2_2_rREQUEST` - Request available abilities list

**Server → Client:**
- `_0_0_1_1_rABILITIES:id1|name1;id2|name2;...` - Ability data response
- `_0_0_3_3_rCOOLDOWN:abilityId:ticks` - Cooldown update (ticks = game ticks, 20 ticks = 1 second)

All messages are intercepted by `ChatMessageMixin.interceptMessage()` and routed to appropriate handlers in `CircleOfImaginationClient`.

## Data Flow

### Ability Execution Flow
1. User presses keybinding (Z/X/C) → detected in `ClientTickEvents.END_CLIENT_TICK` handler
2. `CircleOfImaginationClient.sendAbilityUse()` sends `_0_0_2_2_rUSE:abilityId` via chat
3. Server processes ability and sends cooldown: `_0_0_3_3_rCOOLDOWN:abilityId:ticks`
4. `ChatMessageMixin` intercepts → `handleCooldownData()` → `AbilityHudOverlay.setCooldown()`
5. `AbilitySlotWidget` updates cooldown display based on elapsed real time

### Ability Binding Flow
1. User opens binding screen (K key) → `requestAbilitiesFromServer()` sends `_0_0_2_2_rREQUEST`
2. Server responds with ability list → `handleAbilityData()` parses and stores in `availableAbilities`
3. User selects ability from dropdown → `setBoundAbility()` updates state
4. `AbilityConfig.saveBindings()` persists to disk
5. `AbilityHudOverlay.updateAbilitySlot()` updates HUD display

### Binding Validation
When ability data is received, `validateBoundAbilities()` cross-references bound abilities with the current available abilities list. Stale bindings (abilities no longer available) are automatically cleared and config is saved.

## Key Patterns

### Static Singleton Pattern
`CircleOfImaginationClient` uses static fields and methods to provide global access to ability state. This simplifies access from screens, widgets, and event handlers without complex dependency injection.

### Functional Interfaces
`AbilityDropdownWidget` uses method references for data access and callbacks:
```java
new AbilityDropdownWidget(
    x, y, width, height,
    CircleOfImaginationClient::getAvailableAbilities,  // Supplier
    currentSelection,
    selected -> CircleOfImaginationClient.setBoundAbility(slot, selected)  // Consumer
);
```

### Chat-Based RPC
Uses Minecraft's existing chat infrastructure for client-server communication. Mixin intercepts messages before rendering to implement transparent RPC without custom networking packets.

### Real-Time Cooldown Tracking
Cooldowns are tracked using `System.currentTimeMillis()` and decremented based on elapsed real time during each render frame, not game ticks. This provides smooth countdown animations even if the game lags.

## Important Code Locations

### Entry Point
`CircleOfImaginationClient.onInitializeClient()` - Loads configs, registers keybindings, initializes HUD, sets up tick handler

### Keybindings
Registered in `CircleOfImaginationClient.onInitializeClient()`:
- Ability 1: Z (GLFW_KEY_Z = 90)
- Ability 2: X (GLFW_KEY_X = 88)
- Ability 3: C (GLFW_KEY_C = 67)
- Ability Menu: K (GLFW_KEY_K = 75)

### Tick Handler
`ClientTickEvents.END_CLIENT_TICK.register()` - Polls keybindings and sends ability use messages

### Render Callback
`HudRenderCallback.EVENT.register(AbilityHudOverlay::renderAbilities)` - Renders HUD overlay each frame

### Mixin Configuration
- `coi-client.client.mixins.json` - Client-only mixins (ChatMessageMixin)
- `coi-client.mixins.json` - Shared mixins (currently empty)

## Ability Format

**Wire Format (from server):** `id|displayName;id|displayName;...`
**In-Memory Format:** `"id - displayName"`
**Example:** `"sun-9-0 - bard-song"`

Pathway is extracted from the first segment of the ID (before first hyphen).

## Configuration Files

Located in Minecraft's `config/` directory:
- `coi_abilities.json` - Ability slot bindings (3 slots)
- `coi_hud.json` - HUD display settings (8 properties)

Both use Gson for JSON serialization with pretty printing enabled.

## HUD Presets

**Default:** x=10, yOffset=60, size=40, spacing=50, scale=1.0
**Compact:** x=5, yOffset=40, size=30, spacing=35, scale=0.8
**Large:** x=15, yOffset=80, size=55, spacing=65, scale=1.2
**Minimal:** x=3, yOffset=30, size=25, spacing=30, scale=0.7

## Localization

Language files located in `src/client/resources/assets/coi-client/lang/`:
- `en_us.json` - English (US)
- `uk_ua.json` - Ukrainian

Translation keys follow format: `key.coi.<category>.<name>`, `screen.coi.<name>`, `notification.coi.<name>`

## Known Implementation Details

### Test Abilities Fallback
If server doesn't respond to `REQUEST` message, the mod falls back to test abilities defined in `CircleOfImaginationClient.getTestAbilities()`. This is useful for debugging UI without a server.

### Cooldown Calculation
Cooldowns are converted from game ticks (from server) to milliseconds (client-side). The widget tracks `maxCooldownTicks` (for percentage calculation) and `lastUseTime` (for elapsed time calculation).

### Pathway Colors
Colors are hardcoded in `AbilitySlotWidget.getPathwayColor()` based on the first segment of the ability ID. If the pathway is unknown, defaults to gray (0xFF777777).

### Screen Navigation
Ability Binding Screen → HUD Settings Screen. The HUD Settings button in the binding screen opens the settings screen, passing itself as the parent for proper back navigation.
