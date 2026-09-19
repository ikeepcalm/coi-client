# Network Protocol

> The menu channels (`coi-client:menu` / `coi-client:menu_action`) are specified in full,
> together with the plugin half, in [MENU_SYSTEM.md](MENU_SYSTEM.md).

Canonical wire reference for COI Client ⟷ the Circle of Imagination Paper plugin. Both repos
implement this document; the plugin's `docs/CLIENT_PROTOCOL.md` links here rather than restating it.

Transport is **Fabric custom payloads** (Minecraft plugin messaging). Every channel is in the
`coi-client` namespace, and every field is written with the vanilla buffer methods:

| Type | Write / read | Bytes |
|------|--------------|-------|
| String | `buf.writeUtf(s)` / `buf.readUtf()` | VarInt length + UTF-8 |
| int | `buf.writeInt(i)` / `buf.readInt()` | 4 bytes, big-endian |

On the Paper side the same layout is produced by writing a VarInt length followed by the UTF-8 bytes
(`PluginMessageCodec.writeString`) and, for `cooldown`, a big-endian `int`.

Names below are Mojang mappings (Mojmap), as used by the client sources.

---

## Channels

| Dir | Channel | Payload class | Fields |
|-----|---------|---------------|--------|
| C→S | `coi-client:use` | `AbilityUsePayload` | `String abilityId`, `String action` |
| C→S | `coi-client:request` | `AbilityRequestPayload` | *(empty)* |
| C→S | `coi-client:hello` | `HelloPayload` | `String json` |
| C→S | `coi-client:action` | `ActionPayload` | `String json` |
| C→S | `coi-client:menu_action` | `MenuActionPayload` | `String json` (32 KiB cap) |
| S→C | `coi-client:abilities` | `AbilitiesPayload` | `String data` |
| S→C | `coi-client:cooldown` | `CooldownPayload` | `String abilityId`, `int ticks` |
| S→C | `coi-client:effect` | `VisualEffectPayload` | `String effectId`, `String params` |
| S→C | `coi-client:mythical` | `MythicalFormPayload` | `String targetUuid`, `String params` |
| S→C | `coi-client:conditions` | `ConditionsPayload` | `String data` |
| S→C | `coi-client:appearance` | `AppearancePayload` | `String targetUuid`, `String traits` |
| S→C | `coi-client:server` | `ServerInfoPayload` | `String json` |
| S→C | `coi-client:abilities_v2` | `AbilitiesV2Payload` | `String json` (1 MiB cap) |
| S→C | `coi-client:state` | `AbilityStatePayload` | `String json` |
| S→C | `coi-client:acting` | `ActingPayload` | `String json` |
| S→C | `coi-client:resource` | `ResourcePayload` | `String json` |
| S→C | `coi-client:actionbar` | `ActionBarPayload` | `String json` |
| S→C | `coi-client:target` | `TargetHealthPayload` | `String json` |
| S→C | `coi-client:cogitation` | `CogitationPayload` | `String json` |
| S→C | `coi-client:notify` | `NotifyPayload` | `String json` |
| S→C | `coi-client:sheet` | `SheetPayload` | `String json` (1 MiB cap) |
| S→C | `coi-client:menu` | `MenuPayload` | `String json` (1 MiB cap) |

All payload records live in `client/network/payload/`, built from the shared type/codec shapes in
`CoiPayloads` (the namespace, the 1 MiB / 32 KiB caps and the read/write pair are spelled out once
there, not 22 times). They are registered — and the S→C receivers attached — in
`CoiNetworking.registerPayloads`.

---

## Handshake

### `coi-client:hello` (C→S)

Sent once from `ClientPlayConnectionEvents.JOIN`, **before** `coi-client:request`, so the server
knows which surfaces the client can render before it starts feeding any of them.

```json
{"modVersion":"1.2.0","protocol":2,"features":["ability_hud","hotkeys","effects","appearance","mythical","conditions","spirituality_hud","menu_action"]}
```

- `modVersion` — the mod's own version from `fabric.mod.json`, read via `FabricLoader`.
- `protocol` — currently `2` (see **Protocol history**).
- `features` — see **Feature ids**. Built by `ClientFeatures.helloJson()`.

### `coi-client:server` (S→C)

The server's reply. Stored in `ServerCapabilities`; every field is optional and a malformed body is
ignored. Reset on disconnect.

```json
{"pluginVersion":"1.3.2-SNAPSHOT","protocol":2,"features":["spirituality","menu_action"]}
```

A server that never sends this leaves `ServerCapabilities.known()` false and every
`ServerCapabilities.has(...)` false — which is how the client hides UI the server cannot feed.

### Feature ids

Client-advertised (`ClientFeatures.SUPPORTED`):

| Id | Meaning |
|----|---------|
| `ability_hud` | renders the ability slot HUD |
| `hotkeys` | casts abilities from keybindings |
| `effects` | renders `coi-client:effect` |
| `appearance` | renders `coi-client:appearance` traits |
| `mythical` | renders `coi-client:mythical` forms |
| `conditions` | consumes `coi-client:conditions` |
| `spirituality_hud` | draws spirituality itself — the server may skip its boss bars |
| `menu_action` | can send `coi-client:action open_menu` — the server may drop the slot-9 shortcut item |
| `ability_meta` | understands `coi-client:abilities_v2` — the server may send the rich list instead of v1 |
| `ability_state` | renders `coi-client:state` — the server may skip the STATUS action-bar publishes |
| `acting_hud` | draws the acting bar — the server may skip the `acting:*` action-bar entries |
| `action_bar` | draws COI's action-bar channels itself — the server may skip `sendActionBar` |
| `target_health` | draws the target health bar — the server may skip the 60-publish damage animation |
| `cogitation` | draws cogitation prompts — the server may skip the titles |
| `notify` | draws notification toasts — the server may skip the matching titles |
| `character_sheet` | renders the Beyonder character sheet itself — `M` opens it instead of the InvUI menu |
| `resource_bar` | draws ability resource meters itself — the server may skip the RESERVE glyph bars and per-ability reserve boss bars |
| `menu_ui` | renders `coi-client:menu` documents — the server may send one instead of opening an InvUI chest GUI |

Server-advertised:

| Id | Meaning |
|----|---------|
| `spirituality` | `conditions` carries the spirituality keys |
| `menu_action` | `coi-client:action` with `open_menu` is handled |
| `ability_meta` | ability lists go out on `coi-client:abilities_v2` |
| `ability_state` | toggle and category changes go out on `coi-client:state` |
| `acting` | acting progress goes out on `coi-client:acting` |
| `action_bar` | action-bar entries go out on `coi-client:actionbar` |
| `target_health` | ability hits go out on `coi-client:target` |
| `cogitation` | cogitation events go out on `coi-client:cogitation` |
| `notify` | toast-worthy events go out on `coi-client:notify` |
| `character_sheet` | `coi-client:sheet` is pushed and the `sheet_open`/`sheet_close`/`open`/`toggle_terrain` actions are handled |
| `resource_bar` | resource meters go out on `coi-client:resource` |
| `menu_ui` | server menus go out as `coi-client:menu` documents and `coi-client:menu_action` clicks are handled |

### Legacy fallback

A client that has `coi-client:appearance` registered but never sent `hello` is a pre-v2 client. The
server must treat it as `{ability_hud, hotkeys, effects, appearance, mythical, conditions}` —
**never** `spirituality_hud` or `menu_action`. Such players keep the boss bars and the shortcut item
exactly as before.

---

## C→S Payloads

### `coi-client:use`

Sent when a bound key, wheel slot or gesture fires.

```
String abilityId   — e.g. "sun-9-0"
String action      — "execute" (default) or "left_click"
```

`left_click` is only sent for abilities the server flagged with `hasLeftClick` in the abilities list.
Constants: `AbilityInfo.ACTION_EXECUTE` / `ACTION_LEFT_CLICK`.

### `coi-client:request`

Empty payload. Sent on join (after `hello`) and whenever the client wants the ability list refreshed.
The server responds with `coi-client:abilities`, and usually an immediate `coi-client:conditions`.

### `coi-client:action`

One JSON string, always with an `action` key. Servers should rate-limit per player — except the
two sheet lifecycle actions, which must be exempt or the sheet can get stuck open.

| Body | Sent when | Server does |
|------|-----------|-------------|
| `{"action":"open_menu"}` | the `M` key on a `menu_action` server without `character_sheet`, or the sheet's *Server Menu* button | opens the InvUI Beyonder menu |
| `{"action":"sheet_open"}` | `CharacterSheetScreen.init()` | marks the sheet open (5-minute TTL) and pushes `coi-client:sheet` |
| `{"action":"sheet_close"}` | `CharacterSheetScreen.onClose()`/`removed()`, exactly once | marks the sheet closed, stopping the 60-tick pushes |
| `{"action":"open","target":"church\|abilities\|mythical\|uniqueness\|honorific\|map\|seat"}` | one of the sheet's seven sub-menu buttons (the sheet then closes itself) | re-checks the gate and opens that InvUI GUI |
| `{"action":"toggle_terrain"}` | the sheet's terrain-damage button (the sheet stays open) | flips the flag, sends the usual chat feedback, pushes a fresh sheet |

The `open` action also carries `"ui":"client"|"server"` — the player's own `useServerMenus`
setting (`ActionPayload.ofOpen`). `client` (the default) asks for a `coi-client:menu` document,
`server` for the original InvUI chest GUI. A server that predates the menu protocol ignores the
field and opens its chest GUI either way. When both peers support `menu_archive`, registered
native menus take precedence over this legacy preference. `open_menu` opens a native pathway
chooser, including secondary pathways; unported flows retain their existing fallback.

When neither `character_sheet` nor `menu_action` is advertised, `M` shows
`notification.coi.menu_unsupported` on the action bar instead of sending anything.

### `coi-client:menu_action`

One JSON string — a click inside a `coi-client:menu` document.

```json
{"session":"7f3a…","version":4,"action":"kick_3","value":"optional text"}
```

- `session` and `version` are echoed from the **newest** document the client holds, so the server can
  drop a click aimed at a screen it has already replaced.
- `action` is an opaque token the server minted for that document version; the client never invents
  one, except for the two reserved lifecycle actions.
- `value` is present only for an `input` component's submit.
- `__back` — the back chevron (or Backspace); the server pops its own stack.
- `__close` — the screen went away. Sent exactly once, whichever way it closed (Esc, the X, or being
  replaced), and `MenuScreen` guards it the way `CharacterSheetScreen` guards `sheet_close`.
  **Not** sent when the server itself closed the screen with `{"closed":true}`.
- Nothing is sent unless `ClientPlayNetworking.canSend(MenuActionPayload.ID)`.

---

## S→C Payloads

### `coi-client:abilities`

The full list of abilities available to this player, `;`-separated, fields `|`-separated:

```
id|localizedName|englishName|category|hasLeftClick;...
```

| Field | Notes |
|-------|-------|
| `id` | internal ability id; the pathway prefix before the first `-` picks the HUD colour |
| `localizedName` | shown to the player in their locale |
| `englishName` | used internally and in cast notifications |
| `category` | icon category (`textures/icons/<category>/<tier>.png`); defaults to `uncategorized` |
| `hasLeftClick` | `true`/`false`; when true the picker offers a second "(Left Click)" entry |

Fields 4 and 5 are optional — entries with only `id|localizedName` still parse. Example:

```
sun-9-0|Пісня барда|bard-song|music|false;fool-1-0|Жарт|jest|trickery|true
```

### `coi-client:cooldown`

```
String abilityId   — must match an id from the abilities list
int    ticks       — cooldown length in game ticks (20 = 1 second)
```

The client converts this to wall-clock time immediately, so the HUD animates smoothly.

### `coi-client:effect`

```
String effectId   — a registered effect name, or "all"
String params     — comma-separated key=value pairs, or "stop"
```

See **[VISUAL_EFFECTS.md](VISUAL_EFFECTS.md)** for the effect list and each effect's parameters.

### `coi-client:mythical`

```
String targetUuid  — the player being transformed (string form of the UUID)
String params      — "<pathway>:<complete>:start" | "<pathway>:<complete>:stop"
```

Only fields 1 and 3 are read: the pathway name (lower-cased) and the action. The middle field is
carried for the server's own bookkeeping. An empty `params`, or fewer than three `:`-separated
fields, clears the form. See **Mythical Creature Forms** in `CLAUDE.md`.

### `coi-client:conditions`

Beyonder state for the **local** player, as `key=value` pairs separated by `;`. Unknown keys are
ignored, so the format is safely extensible in both directions.

| Key | Type | Notes |
|-----|------|-------|
| `madness` | double | 0–100; drives the madness bar, hallucinations and title-screen haunting |
| `permanentMadness` | double | 0–100; the floor marker on the bar |
| `freezeStacks` | int | shown under the bar |
| `mentalPressure` | int | shown under the bar |
| `tiredness` | double | shown under the bar |
| `spirituality` | int | **protocol 2**; its presence sets `hasSpiritualityData()` |
| `maxSpirituality` | int | **protocol 2** |
| `spiritualityRegen` | boolean | **protocol 2**; true while below max — drives the shimmer and the hide-when-full fade |
| `maxHealth` | double | **protocol 2**; the Beyonder's *max* HP in pool units. Its presence sets `hasHealthData()` and swaps the vanilla hearts for the health bar |
| `pathway` | string | **protocol 2**; primary pathway name (`eternalaeon` folds onto `aeon`) |
| `sequence` | int | **protocol 2**; lowest sequence level — shown in Discord presence |

Pairs are split on the **first** `=` only, so string values may contain one.

Example:

```
madness=42.50;permanentMadness=5.00;freezeStacks=0;mentalPressure=0;tiredness=0.00;spirituality=430;maxSpirituality=1000;spiritualityRegen=true;maxHealth=1750.0
```

A payload without `spirituality` never enables the spirituality bar, and one without `maxHealth`
leaves the vanilla hearts untouched, so old servers simply keep their boss bars and hearts. Parsed
by `state/ConditionsParser`, into `state/BeyonderState` (`parseAndUpdate` is the entry point).

**Why only the ceiling.** The server never sends current HP. Vanilla health is a proportional
mirror of the pool, and the plugin recomputes the pool *from* vanilla health after every hit, so the
client already holds the authoritative current value and derives
`poolCurrent = maxHealth * (vanillaHealth / vanillaMaxHealth)` every frame. That is *more* accurate
than a pushed value, which would be stale between the server's 200-tick health syncs after any
non-ability damage. The ceiling still has to be pushed because it is volatile — True Form doubles
it, Strata and Death marks cut it — so it cannot be derived from the sequence table alone.

### `coi-client:abilities_v2`

One JSON string — the protocol-2 replacement for `coi-client:abilities`, sent only to clients that
advertised `ability_meta`, and never alongside v1. The client codec reads with
`readUtf(1_048_576)`, because a full list with descriptions blows past the default 32767-char cap.

```json
{"abilities":[{
  "id":"sun-9-0","name":"<player-locale name>","englishName":"<english name>",
  "category":"attack","hasLeftClick":false,
  "kind":"active|activated|passive",
  "description":"<player-locale description or empty>",
  "cost":20,"drainPerSecond":0.0,"cooldownSeconds":30,"cooldownRemainingTicks":0,
  "pathway":"sun","sequence":9,
  "active":false,"locked":false,"blocked":false,"blockedBy":"",
  "icon":"circleofimagination:sun/holylight"
}]}
```

| Field | Notes |
|-------|-------|
| `kind` | `active` (fire and forget), `activated` (toggled), `passive` (enabled/disabled) |
| `description` | wrapped at 200px in the picker tooltip; may be empty |
| `cost` | one-shot spirituality cost; shown when `drainPerSecond` is 0 |
| `drainPerSecond` | sustained drain for toggled abilities; takes precedence over `cost` in the tooltip |
| `cooldownSeconds` | full cooldown — makes the max known before the first cast |
| `cooldownRemainingTicks` | applied to the slot on arrival, so a reconnect resumes mid-cooldown |
| `active` | `isActivated()` / `isEnabled()`; always false for `kind=active` |
| `locked` / `blocked` | madness lock / grazing block or suppressed contract — the slot goes red and struck through |
| `blockedBy` | `""`, `"hanged"`, `"devouring"` or `"contract"`; picks the tooltip's reason line |
| `icon` | full item-model id (the same one the plugin's shortcut item uses), or empty |

Passives are included in this list (v1 excludes them). `icon` is only honoured when the loaded
resource pack actually defines `assets/<namespace>/items/<path>.json`; otherwise the client falls
back to its bundled category/tier icon. `ui/IconModels` caches that answer per icon string and
clears it on resource reload and on disconnect.

Parsed by `AbilityRegistry.handleAbilityDataV2`.

### `coi-client:state`

One JSON string, a single-ability update. Either a toggle:

```json
{"id":"sun-9-0","active":true}
```

or a category switch:

```json
{"id":"sun-9-0","category":"fire","categoryName":"<player-locale>"}
```

An active ability outlines its HUD slot in cyan with an `ON` tag; a category label is appended to
the slot's name line. Parsed by `AbilityRegistry.handleAbilityState`.

### `coi-client:acting`

One JSON string — acting progress for the local player.

```json
{"pathway":"sun","sequence":9,"acting":1234,"needed":5000,"percent":24.68,
 "cooldownRemaining":1200,"cooldownTotal":1800,
 "overflow":0,"overflowEligible":false,"limited":false,"outer":false,
 "granted":0,"source":""}
```

| Field | Notes |
|-------|-------|
| `percent` | 0–100; drives the bar fill and its label |
| `cooldownRemaining` / `cooldownTotal` | **seconds**; the client counts the remainder down locally between pushes |
| `outer` | true for an `OuterPathway` — no acting progression, so the bar stays hidden |
| `granted` / `source` | non-zero only on the push that follows a grant; drives the `+N` popup |

An empty object (`{}`) means the player has no pathway and clears the state. Sent after hello, on
`coi-client:request`, every 60 ticks, and immediately after any grant. Parsed by
`ActingState`.

### `coi-client:resource`

One JSON string per resource meter — an ability's own reserve, charge count, stack pool or
whatever else it wants a bar for. Each packet is a full update of one bar.

```json
{"id":"tyrant:rage-meter","label":"Rage Meter","current":45.0,"max":100.0,
 "color":"FF5555","ttlMs":1500,"format":"percent"}
```

| Field | Notes |
|-------|-------|
| `id` | stable key per resource; a later packet with the same id **replaces** that bar in place, keeping its position in the stack |
| `label` | already player-localised plain text — the client does not translate it |
| `current` / `max` | doubles, `max > 0`; `current` is clamped into `[0, max]` client-side |
| `color` | 6 hex digits, no `#` (RRGGBB); anything unparseable falls back to white |
| `ttlMs` | the bar expires locally at `receivedAt + ttlMs` unless refreshed. `0` (or missing) = persistent until an explicit remove — a self-refreshing passive just re-sends every few hundred ms |
| `format` | `percent` → `label: 45.0%`; `value` → `label: 45 / 100` (integers when both sides are whole, else one decimal). Missing reads as `percent` |

Removal is its own form:

```json
{"id":"tyrant:rage-meter","remove":true}
```

Bars stack in arrival order, capped by the player's `resourceMaxBars` setting. Parsed by
`ResourceState`, drawn by `ResourceOverlay`.

### `coi-client:actionbar`

One JSON string — the whole action-bar list, replacing the vanilla single-line action bar for
capable players.

```json
{"maxVisible":2,"entries":[
  {"channel":"COOLDOWN","key":"artifact:sword","priority":60,"ttlMs":1850,"text":{"text":"Ready in 3s"}}
]}
```

| Field | Notes |
|-------|-------|
| `maxVisible` | how many lines the server thinks should show; the client caps it further with `actionBarLines` |
| `channel` | one of `RESERVE`, `SENSORY`, `COOLDOWN`, `STATUS`, `CATEGORY`, `NOTIFICATION`, `MINIGAME`, `SYSTEM` — picks the 2 px tick colour |
| `key` | the plugin's per-channel source key (dedupe identity), unused by the client for now |
| `ttlMs` | remaining lifetime; the client expires the entry at `receivedAt + ttlMs` |
| `text` | a **vanilla text-component object** (Adventure's Gson serializer is wire-compatible), deserialized through `ComponentSerialization.CODEC` |

Each payload replaces the whole list; an empty `entries` array clears it. Only COI's own entries
move — vanilla action bars from other plugins still render normally. Parsed by
`ActionBarState`, drawn by `hud/overlay/ActionBarOverlay`.

### `coi-client:target`

One JSON string per ability hit, replacing the server's 60 one-tick action-bar publishes.

```json
{"uuid":"…","name":"Steve","before":0.82,"after":0.75,
 "health":150.0,"max":200.0,"damage":12.5,"kind":"beyonder"}
```

`before` / `after` are 0..1 fractions. The bar sits under the crosshair for 3 s; the chunk between
`before` and `after` flashes white for 600 ms before settling to dark red. `kind` is reserved for a
later `creature` variant. Parsed by `TargetState`, drawn by `hud/overlay/TargetHealthOverlay`.

### `coi-client:cogitation`

One JSON string per session event, replacing the vanilla titles.

```json
{"state":"start"}
{"state":"prompt","action":"TURN_360","label":"<player-locale>","streak":3,"timeoutMs":5000}
{"state":"fail","streak":0}
{"state":"stop","interrupted":false}
```

`stop` clears everything. The server only sweeps for timeouts every 40 ticks, so the client's time
bar can hold at 0 for up to two seconds before a `fail` arrives. Parsed by
`CogitationState`, drawn by `hud/overlay/CogitationOverlay`.

### `coi-client:notify`

One JSON string per toast.

```json
{"kind":"advancement|acting|bounty|madness|info","title":"…","body":"…",
 "color":"FFD870","durationMs":4000}
```

`title` and `body` are already resolved in the player's locale server-side; `color` is `RRGGBB`
(the accent bar and title colour). At most three toasts show at once in the top-right corner; the
rest queue. Parsed by `NotificationState`, drawn by `hud/overlay/NotificationOverlay`.

### `coi-client:sheet`

One JSON string — the whole character sheet. Pushed on `sheet_open`, after `toggle_terrain`, and
every 60 ticks while the sheet is open.

```json
{"pathway":"fool","pathwayName":"Fool","pathwayColor":"B347CC","sequence":7,
 "sequenceName":"Magician","outer":false,
 "health":80.0,"maxHealth":120.0,
 "spirituality":300,"maxSpirituality":500,
 "madness":35.2,"permanentFloor":10.0,"godhoodFloor":5.0,"madnessStage":1,
 "tiredness":22.0,"tirednessStage":0,
 "acting":{"acting":1234,"needed":5000,"percent":24.68,"limited":false,
   "cooldownRemaining":1200,"cooldownTotal":1800,
   "sources":[{"source":"ABILITY_GAMEPLAY","label":"Ability gameplay","contributed":600,"cap":2500,"unlimited":false}],
   "overflow":{"eligible":true,"banked":300,"ceiling":17500,"uncapped":false,"foreignThrottlePercent":0}},
 "lifeAndDeath":{"present":true,"meter":0.42},
 "pressure":{"present":true,"stacks":3,"cap":10},
 "anomaly":false,
 "actions":{"church":true,"abilities":true,"mythical":false,"uniqueness":true,
   "honorific":false,"map":true,"seat":false,"terrainDamage":true}}
```

- `pathwayColor` is six hex characters with no `#`; the client falls back to `Pathways.pathwayRgb`
  when it is missing or unparseable.
- `madnessStage` and `tirednessStage` are 0–4 and pick the `screen.coi.sheet_stage_*` /
  `screen.coi.sheet_tired_*` names.
- `acting` is **omitted entirely** for outer pathways; the sheet then shows `screen.coi.sheet_outer`
  where the ledger would be. `cooldownRemaining`/`cooldownTotal` are seconds, and the client counts
  the remaining one down locally between pushes.
- A source with `unlimited` renders its cap as `∞`; a source at or over its cap renders red.
- `actions.*` are availability gates for the seven sub-menu buttons — except `terrainDamage`, which
  is the **current toggle state** and drives the `sheet_btn_terrain_on`/`_off` label.
- All strings arrive already resolved in the player's locale. Spirituality here uses the GUI's
  three-way fallback (Influence → Concealment → real), which can legitimately differ from the
  spirituality HUD bar fed by `conditions`.
- Every field may be missing: the parse is defensive throughout and every absent value defaults.
  Parsed by `state/SheetParser` into a `SheetState.Snapshot`, held by `state/SheetState`, drawn by
  `screen/sheet/CharacterSheetScreen` and its `Sheet*` sections, reset on disconnect.

### `coi-client:menu`

One JSON string (1 MiB cap) describing a whole screen. The client renders it and nothing else: it
never branches on `screen`, which exists only so a push can be recognised as "the same screen
refreshed" — scroll position and typed text survive that, a different `screen` id resets them.
Navigation, gating and side effects all stay on the server, which answers every click with the next
document.

```json
{"session":"7f3a…","version":4,"screen":"church.main",
 "title":"Church of the Fool","subtitle":"Sanctum of Dreams · 12 members",
 "accent":"B347CC","icon":{"kind":"pathway","value":"fool"},
 "back":true,"closable":true,
 "toast":{"style":"error","text":"You are not a bishop any more."},
 "sections":[{"title":"Standing","components":[…]}],
 "footer":[{"type":"button","id":"done","label":"Done","style":"primary"}]}
```

- `session` — opaque token minted per menu session; `version` increments on every push. Both are
  echoed back on every click.
- `icon.kind` ∈ `pathway` (a pathway key → `CoiIcons.drawPathwayEmblem`), `item` (a
  `namespace:path` item model id → `AbilityIcons.drawItemModel`), `glyph` (one of the 25 first-party
  16×16 icons in `CoiIcons.GLYPHS` — the only kind whose art does not depend on the player's
  resource pack), `ability` (an ability id, through the same `AbilityIcons` route the HUD uses),
  `head` (a player uuid, resolved through the tab list), `none`.
- `accent` — 6 hex digits, no `#`; drives the card rule, the headings, the primary buttons and the
  scrollbar. Absent means the mod's gold.
- `toast` — one line shown under the header for the document's life, in `info|success|warn|error`
  colours. Only its arrival highlight is transient, so the card never reflows under the cursor.
- `{"session":…,"closed":true}` closes the screen **without** a `__close` reply.
- All strings arrive already localized. The client adds no lang keys for document content.

**Components** (`sections[].components[]`):

| `type` | Fields | Renders as |
|--------|--------|-----------|
| `text` | `text`, `style` (`body\|muted\|heading\|warn\|danger\|success`), `align` | a wrapped paragraph |
| `note` | `style`, `title?`, `text` | callout box with a 2px accent edge |
| `stat` | `label`, `value`, `fraction?` (0..1), `color?`, `hint?` | label + right-aligned value, optional gauge |
| `kv` | `rows[{label,value,color?,hint?}]` | two-column key/value table |
| `checklist` | `items[{ok,label,detail?}]` | drawn tick/cross list, red on fail |
| `button` | `id`,`label`,`desc?`,`style` (`primary\|secondary\|danger\|success\|ghost`),`enabled`,`disabledReason?`,`confirm?{title,body,confirmLabel}`,`icon?` | full-width button; `desc` is a muted second line |
| `buttons` | `buttons[<button>]`, `columns?` (1–4) | a row/grid of the above |
| `toggle` | `id`,`label`,`on`,`enabled`,`desc?`,`onText?`,`offText?`,`disabledReason?` | switch row |
| `list` | `id`,`rows[{id,title,subtitle?,icon?,badge?,badgeColor?,tooltip[]?,action?,enabled,color?}]`,`searchable?`,`maxVisible?`,`empty?` | card rows with hover, badge and tooltip |
| `grid` | `cells[<row>]`,`columns?` (1–12) | icon tiles with hover tooltips |
| `input` | `id`,`label?`,`placeholder?`,`value?`,`maxLength?`,`submit` (action id),`submitLabel?`,`hint?` | text field + submit button |
| `divider` | — | hairline |
| `spacer` | `size?` | vertical gap |

- Unknown `type` values are **skipped silently** and unknown fields ignored — a newer plugin must
  degrade to a screen missing one row, never to no screen.
- `enabled` defaults to **true** when absent. A disabled control is drawn, not hidden, and its
  `disabledReason` is the tooltip.
- A button with `confirm` raises a client-side modal and sends nothing until the player agrees —
  this is what replaces the plugin's two-step chest confirms.
- `maxVisible` is **advisory**: the client scrolls the whole card, so a list is never paged.

Parsed by `client/menu/MenuParser` into `MenuDocument`/`MenuComponent`, held by `MenuState`,
drawn by `screen/menu/MenuScreen` (+ `MenuTheme`), reset on disconnect.

### `coi-client:appearance`

```
String targetUuid  — the player the traits belong to
String traits      — comma-separated trait ids, or empty to clear
```

Rendered as extra geometry on that player by `AppearanceTraitLayer`.

### `coi-client:server`

See **Handshake**.

---

## Ability ID Format

`<pathway>-<tier>-<index>` — e.g. `sun-9-0`, `fool-1-2`, `tyrant-3-1`.

The client takes the segment before the first `-` as the pathway and colours the HUD slot with it:

| Pathway | Color |
|---------|-------|
`Pathways.pathwayRgb` is the single colour table for all 25 pathways — the HUD slots, the
picker, the acting bar and the mythical-form burst all read from it. `eternalaeon` resolves to
`aeon`; anything unrecognised falls back to a pale lavender.

| Pathway | Color | Pathway | Color |
|---------|-------|---------|-------|
| fool | Purple | fortune | Gold |
| door | Blue | chained | Slate |
| sun | Yellow | abyss | Dark magenta |
| tyrant | Cyan | justiciar | Pale gold |
| demoness | Red | emperor | Amber |
| priest | Orange | moon | Pale violet |
| error | Gray | mother | Green |
| tower | Blue-gray | patriarch | Sea blue |
| visionary | Teal | sublunary | Silver-blue |
| hanged | Green | aeon | Pale gold |
| darkness | Violet | giant | Bronze |
| death | Pale blue | paragon | White |
| hermit | Purple | *(unknown)* | Lavender |

---

## Protocol history

| Version | Change |
|---------|--------|
| 1 | Pre-handshake. Channels `use`, `request`, `abilities`, `cooldown`, `effect`, `mythical`, `conditions`, `appearance`. The server could only detect the mod by checking whether `coi-client:appearance` was a registered channel — no version, no per-feature gating. |
| 2 | Adds `hello`, `server` and `action`; adds `spirituality`, `maxSpirituality` and `spiritualityRegen` to `conditions`. Per-feature gating replaces the boolean mod check; clients that skip the handshake fall back to the legacy feature set above. |
| 2 (batch 2) | Still protocol 2 — every addition is gated on a feature id. Adds `abilities_v2`, `state` and `acting`; adds `pathway` and `sequence` to `conditions`; adds the `ability_meta`, `ability_state`, `acting_hud` client features and the `ability_meta`, `ability_state`, `acting` server features. |
| 2 (batch 3) | Still protocol 2. Adds `actionbar`, `target`, `cogitation` and `notify`, and the matching `action_bar`, `target_health`, `cogitation`, `notify` feature ids on both sides. No change to `conditions`. |
| 2 (batch 4) | Still protocol 2. Adds `sheet` and the `character_sheet` feature id on both sides, plus the `sheet_open`, `sheet_close`, `open` and `toggle_terrain` actions on `coi-client:action`. No change to `conditions`. |
| 2 (batch 5) | Still protocol 2. Adds `resource` and the `resource_bar` feature id on both sides — client-rendered resource meters replace the server's RESERVE glyph bars and per-ability reserve boss bars. No change to `conditions`. |
| 2 (batch 6) | Still protocol 2. Adds `menu` and `menu_action` and the `menu_ui` feature id on both sides — declarative client-rendered menus replace the InvUI chest GUIs — plus the `ui` field on `coi-client:action`'s `open`. No change to `conditions`. |

## Category casts and manual presentation

Protocol 2 peers may advertise ability_categories and ability_manual. See
[ABILITY_MANUAL.md](ABILITY_MANUAL.md#protocol-additions-protocol-2) for the use_category
channel, category metadata/state fields, compatibility behavior and binding storage contract.
