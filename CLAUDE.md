# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

COI Client is a client-only Minecraft Fabric mod implementing a customizable ability system with HUD overlay. Players bind up to **10 abilities** to keybindings (slots 1–6 default Z/X/C/V/B/N, slots 7–10 default unbound), use them in-game, and can customize the HUD visually. The player-facing slot count is the `activeAbilitySlots` HUD setting (1–10, default 6); `MAX_ABILITIES = 10` is a hard ceiling because keymappings can only be registered once at init. Lowering the count hides bindings without deleting them. The mod communicates with a server-side Paper plugin via **Fabric custom payloads** (plugin messaging).

**Environment:** Client-only
**Java:** 25 | **MC:** 26.2 | **Fabric Loader:** 0.19.5 | **Fabric API:** 0.159.0+26.2

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
  ├── gesture/
  │   ├── GestureType          — 5 shapes (circle, V, Z, line down, triangle):
  │   │                          direction templates + preview polylines
  │   ├── GestureRecognizer    — resample → 8-way direction string → Levenshtein match
  │   └── GestureScreen        — hold Left Alt, draw with mouse, release to cast;
  │                              inert until a gesture has an ability bound
  ├── presence/
  │   └── DiscordPresenceManager — Discord Rich Presence via discord-game-sdk4j
  │                                (pure-Java IPC, bundled jar-in-jar); lazy connect
  │                                on first join, APP_ID = 0 disables it entirely
  ├── mcf/                     — mythical creature forms (see below)
  │   ├── MythicalFormManager  — uuid → pathway map, fed by coi-client:form
  │   ├── MythicalCreatureForm — per-pathway form; forms/ holds all 20
  │   ├── PartialFormSpec      — placement/scale of a baked lower-body model
  │   ├── PartialForms         — shared resolve + carrier-transform helpers
  │   ├── PartialFormLayer     — draws the baked model as a player render layer
  │   └── model/               — Blockbench exports (VisionaryLowerModel/Animations)
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
      ├── AbilityBindingScreen — bind abilities to slots (opened with K); tabbed
      │                          (hotkeys/wheel/gestures) with per-tab how-to banner
      ├── AbilityPickerOverlay — modal ability chooser: search box, icon rows, Unbind
      ├── AbilityIcons         — shared category/tier icon renderer (pathway-tinted)
      ├── CoiStyle             — shared dark/gold palette + card chrome (from TourScreen)
      ├── CoiTabButton         — hand-drawn tab widget used by binding + settings screens
      ├── HudSettingsScreen    — HUD customization: 3 tabs (Ability HUD/Madness/Display),
      │                          scrollable rows so it fits any gui scale, 4 presets
      ├── TourScreen           — first-join walkthrough: spotlight cutouts + text cards,
      │                          movement stays enabled; re-run via "Show Tour Again"
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
- S→C `coi-client:mythical` — transform a player into a pathway form (see Mythical Creature Forms)
- S→C `coi-client:conditions` — beyonder state (madness etc.) for the local player
- S→C `coi-client:appearance` — appearance traits per player UUID

Ability wire format: `id|localizedName|englishName|category` per entry, `;` separated.
In-memory format: `"id - englishName"`.

## Visual Effects System

Full reference + server integration guide: **[docs/VISUAL_EFFECTS.md](docs/VISUAL_EFFECTS.md)**

Effects are triggered server-side via `VisualEffectPayload(effectId, params)`.
`EffectManager` maintains the active list and renders all effects via `HudRenderCallback`.
Effects support `params = "stop"` to remove, `effectId = "all"` to clear all.

Available effects: `vignette`, `heartbeat`, `cracks`, `eyes`, `glitch`, `bloodrain`, `frost`, `whispers`, `tunnel`, `flash`, `impact`, `hallucination`

**Sound layer** — `EffectSounds` plays audio companions for effects (loops for `heartbeat`/`whispers`/`tunnel`, one-shots for `cracks`/`frost`/`glitch`); assets in `assets/coi-client/sounds/` + `sounds.json`. Volume via `effectSoundVolume` HUD setting.

**Madness hallucinations** — `HallucinationManager` (client tick) fires phantom positional sounds and visual flickers once `ClientBeyonderState` madness ≥ 25, scaling with stages 25/50/75; darkness/night makes events up to ~2.5x more frequent. Server can force one via the `hallucination` pseudo-effect (`event=footsteps|whisper|cave|block|flicker|random`). Toggle: `enableHallucinations` HUD setting, which also gates:
- **HUD gaslighting** (`hud/HudGaslight`) — at madness ≥ 75 the HUD briefly lies: wrong cooldown numbers, glitched keybind glyphs, two slots trading places.
- **Title screen haunting** (`screen/TitleScreenHaunt` + `TitleScreenMixin`) — corruption (max of madness at disconnect and permanent madness, incl. debug-screen values) is persisted to `config/coi_client_state.json` (`ClientStateStore`); the main menu shows a scaled vignette, occasional eye apparitions, and whisper splash lines (`title.coi.haunt_splash.*`). Clean players always get LOTM flavor splashes (`title.coi.splash.*`) — not gated by the hallucinations toggle.

**Debug screen** (dev environment only, F8): lists all registered effects with Test/Stop buttons and a params input field. `shouldPause()` returns false so effects are visible while the screen is open.

## Mythical Creature Forms (`mcf/`)

S→C `coi-client:mythical` (`MythicalFormPayload`, `targetUuid` + `pathway:<unused>:start|stop`) marks
a player's UUID as transformed into a pathway's form. Two kinds:

- **Full forms** — the vanilla player render is cancelled outright (`PlayerRendererMixin` at HEAD)
  and replaced with procedural geometry drawn from `Coi3dPrimitives`. 19 of the 20 pathways.
- **Partial forms** — a baked Blockbench model stands in for the *lower body* while the player's own
  head/torso/arms keep rendering. Currently Visionary only (`CoiModelLayers.VISIONARY_LOWER_SPEC`).

Partial forms are assembled from four pieces that all have to agree:

| Piece | Job |
|-------|-----|
| `PlayerModelMixin` (`setupAnim` TAIL) | hides leg parts; must run in `setupAnim`, since submission is deferred |
| `HumanoidArmorLayerMixin` | hides leggings/boots, which draw from their own model set |
| `PlayerRendererMixin` | `hipRaise` push (world space, at HEAD) + **carrier transform** push (model space) |
| `PartialFormLayer` | draws the baked model, undoing the carrier transform it inherits |

**The carrier transform** is what makes the halves read as one body. The rig's torso bone (its
"carrier") both rotates and translates during the walk cycle, around a pivot that is over a block
away from the player's waist. `CoiFormModel#carrierDelta` hands out that bone's full rigid motion,
`PartialForms#carrierTransform` converts it into player space, and the renderer mixin pushes it onto
the pose stack just before the model is submitted — so the player *and* every layer above it (armor,
held items, cape, appearance traits) ride the torso exactly. Copying the rotation angle alone is not
enough and looks like shearing: same tilt, wrong pivot, no translation.

Placement knobs live in `CoiModelLayers`; read the comment there before touching one. `hipRaise` and
the carrier push sit in different coordinate spaces on purpose — see `PlayerRendererMixin`.

**Dev testing** (no server needed): F8 → *Form: None (Click to cycle)* applies a form to yourself.

## Key Patterns

- **Static singleton** — `CircleOfImaginationClient` holds all ability state; screens and widgets access it via static methods.
- **Real-time cooldowns** — tracked via `System.currentTimeMillis()`, not ticks, for smooth animation.
- **Lazy effect geometry** — `CracksEffect` generates crack segments on first render (needs screen dimensions); seeded by `startTime` for consistent patterns.
- **Dev-only keybindings** — `effectDebugMenu` (F8) is only registered when `FabricLoader.isDevelopmentEnvironment()`.

## Keybindings

| Key | Action |
|-----|--------|
| Z–N (6 keys) | Ability slots 1–6 |
| *(unbound)* | Ability slots 7–10 — assign in vanilla Controls, activate via `activeAbilitySlots` |
| K | Open Ability Binding screen |
| Left Alt (hold) | Gesture casting — draw a shape, release to cast (only when a gesture is bound) |
| F8 *(dev only)* | Open Effect Debug screen |

## Ability Pathway Colors

Extracted from first segment of ability ID (before first `-`):
`fool`=purple, `door`=blue, `sun`=yellow, `tyrant`=cyan, `demoness`=red, `priest`=orange

## Config Files

`config/coi_abilities.json` — bound ability ids per key slot (`abilityN`), wheel slot (`wheelN`), and gesture (`gesture_<id>`)
`config/coi_hud.json` — HUD settings (position/size/scale, display toggles, epilepsy mode, madness bar, `effectSoundVolume`, `enableHallucinations`, `activeAbilitySlots`, `wheelSlots`, `enableDiscordPresence`, `presenceShowMadness`)
`config/coi_client_state.json` — persistent state, not preferences (`ClientStateStore`): last madness values for title haunting, `tourCompleted` for the first-join tour (survives HUD config resets)

## Localization

`src/client/resources/assets/coi-client/lang/en_us.json` + `uk_ua.json`
Key format: `key.coi.*`, `screen.coi.*`, `notification.coi.*`
