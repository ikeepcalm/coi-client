# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

COI Client is a client-only Minecraft Fabric mod implementing a customizable ability system with HUD overlay. Players bind up to **6 abilities** to keybindings (Z/X/C/V/B/N), use them in-game, and can customize the HUD visually. The mod communicates with a server-side Paper plugin via **Fabric custom payloads** (plugin messaging).

**Environment:** Client-only
**Java:** 21 | **MC:** 1.21.11 | **Fabric Loader:** 0.18.4 | **Fabric API:** 0.141.3+1.21.11

## Build Commands

```bash
./gradlew build          # → build/libs/coi-client-<version>.jar
./gradlew clean build
./gradlew runClient      # dev client
./gradlew genSources
```

## Architecture

```
CircleOfImaginationClient   — entry point, singleton state, payload registration
  ├── hud/
  │   ├── AbilityHudOverlay    — renders all ability slots via HudRenderCallback
  │   └── AbilitySlotWidget    — single slot: icon, cooldown, keybind label, glow
  ├── effects/
  │   ├── EffectManager        — registry + active list, renders via HudRenderCallback
  │   ├── VisualEffect         — interface (start/render/isFinished/stop)
  │   └── impl/                — CracksEffect, EyesEffect, VignetteEffect,
  │                               HeartbeatEffect, GlitchEffect
  ├── network/
  │   ├── AbilityUsePayload    C→S  coi-client:use
  │   ├── AbilityRequestPayload C→S  coi-client:request
  │   ├── AbilitiesPayload     S→C  coi-client:abilities
  │   ├── CooldownPayload      S→C  coi-client:cooldown
  │   └── VisualEffectPayload  S→C  coi-client:effect
  ├── config/
  │   ├── AbilityConfig        — persists slot bindings → config/coi_abilities.json
  │   ├── HudConfig            — persists HUD settings  → config/coi_hud.json
  │   └── AbilityInfo          — in-memory ability metadata record
  └── screen/
      ├── AbilityBindingScreen — bind abilities to slots (opened with K)
      ├── AbilityDropdownWidget — scrollable dropdown, uses method references
      ├── HudSettingsScreen    — HUD customization (sliders, 4 presets)
      └── EffectDebugScreen    — dev-only (F8), test visual effects without server
```

## Network Protocol

Full reference: **[docs/NETWORK_PROTOCOL.md](docs/NETWORK_PROTOCOL.md)**

Summary:
- C→S `coi-client:use` — activate ability by id
- C→S `coi-client:request` — request available abilities list
- S→C `coi-client:abilities` — pipe/semicolon delimited ability data
- S→C `coi-client:cooldown` — ability id + ticks
- S→C `coi-client:effect` — trigger/stop a visual effect

Ability wire format: `id|localizedName|englishName|category` per entry, `;` separated.
In-memory format: `"id - englishName"`.

## Visual Effects System

Full reference + server integration guide: **[docs/VISUAL_EFFECTS.md](docs/VISUAL_EFFECTS.md)**

Effects are triggered server-side via `VisualEffectPayload(effectId, params)`.
`EffectManager` maintains the active list and renders all effects via `HudRenderCallback`.
Effects support `params = "stop"` to remove, `effectId = "all"` to clear all.

Available effects: `vignette`, `heartbeat`, `cracks`, `eyes`, `glitch`, `bloodrain`, `frost`, `whispers`, `tunnel`, `flash`

**Debug screen** (dev environment only, F8): lists all registered effects with Test/Stop buttons and a params input field. `shouldPause()` returns false so effects are visible while the screen is open.

## Key Patterns

- **Static singleton** — `CircleOfImaginationClient` holds all ability state; screens and widgets access it via static methods.
- **Real-time cooldowns** — tracked via `System.currentTimeMillis()`, not ticks, for smooth animation.
- **Lazy effect geometry** — `CracksEffect` generates crack segments on first render (needs screen dimensions); seeded by `startTime` for consistent patterns.
- **Dev-only keybindings** — `effectDebugMenu` (F8) is only registered when `FabricLoader.isDevelopmentEnvironment()`.

## Keybindings

| Key | Action |
|-----|--------|
| Z–N (6 keys) | Ability slots 1–6 |
| K | Open Ability Binding screen |
| F8 *(dev only)* | Open Effect Debug screen |

## Ability Pathway Colors

Extracted from first segment of ability ID (before first `-`):
`fool`=purple, `door`=blue, `sun`=yellow, `tyrant`=cyan, `demoness`=red, `priest`=orange

## Config Files

`config/coi_abilities.json` — bound ability ids per slot
`config/coi_hud.json` — 8 HUD settings (x, yOffset, slotSize, slotSpacing, scale, enabled, showKeybinds, showAbilityNames, showGlowEffect)

## Localization

`src/client/resources/assets/coi-client/lang/en_us.json` + `uk_ua.json`
Key format: `key.coi.*`, `screen.coi.*`, `notification.coi.*`
