# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

COI Client is a client-only Minecraft Fabric mod implementing a customizable ability system with HUD overlay. Players bind up to **10 abilities** to keybindings (slots 1–6 default Z/X/C/V/B/N, slots 7–10 default unbound), use them in-game, and can customize the HUD visually. The player-facing slot count is the `activeAbilitySlots` HUD setting (1–10, default 6); `AbilityBindings.MAX_ABILITIES = 10` is a hard ceiling because keymappings can only be registered once at init. Lowering the count hides bindings without deleting them. The mod communicates with a server-side Paper plugin via **Fabric custom payloads** (plugin messaging).

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
dev.ua.ikeepcalm.coi
  ├── CoiClient                 — common-side entrypoint; exists only because Fabric wants one
  ├── CoiLog                    — the mod's one logger ("COI Client"); no System.out.println remains
  └── client/
      ├── CoiClientMod          — the client entrypoint, and **only** that: load what is on disk,
      │                           register what draws and what listens, wire the two connection
      │                           events. It holds no ability state any more (see ability/)
      ├── DataGenerator         — datagen entrypoint; creates the pack and generates nothing,
      │                           since every asset this client ships is hand-authored
      ├── ability/
      │   ├── AbilityRegistry   — the server's ability catalogue: both list formats (v1 delimited,
      │   │                       v2 JSON) land in the same two collections, so nothing downstream
      │   │                       has to know which protocol answered
      │   ├── AbilityBindings   — which ability sits in which key slot / wheel slot / gesture, and
      │   │                       the persistence behind all three; owns MAX_ABILITIES
      │   ├── AbilityInfo       — ability metadata record + the `id - englishName` encoding
      │   └── Pathways          — **the** single home for a pathway as a pathway: normalisation,
      │                           the 25-colour `pathwayRgb` table, the emblem glyph, the spelling
      ├── input/
      │   └── CoiKeyBindings    — every keymapping and what pressing one does; presses are
      │                           edge-triggered by hand, because the wheel and gesture screens
      │                           need to know the key is still *held*
      ├── network/
      │   ├── CoiNetworking     — the wire: payload registration, every S→C receiver, `sendHello`
      │   ├── ClientFeatures    — feature ids + protocol version this client speaks
      │   ├── ServerCapabilities — what the connected server said it can feed; reset on disconnect
      │   └── payload/          — one record per channel, all built from `CoiPayloads`
      │       ├── CoiPayloads   — the shared type/codec shapes: the `coi-client` namespace, the
      │       │                   1 MiB / 32 KiB caps, the read/write pair — spelled out once
      │       ├── AbilityUsePayload      C→S  coi-client:use
      │       ├── AbilityRequestPayload  C→S  coi-client:request
      │       ├── HelloPayload           C→S  coi-client:hello   (capability handshake)
      │       ├── ActionPayload          C→S  coi-client:action  (open_menu, sheet lifecycle)
      │       ├── MenuActionPayload      C→S  coi-client:menu_action (a click in a menu document)
      │       ├── AbilitiesPayload       S→C  coi-client:abilities
      │       ├── AbilitiesV2Payload     S→C  coi-client:abilities_v2 (JSON, 1 MiB cap)
      │       ├── AbilityStatePayload    S→C  coi-client:state   (toggle / category)
      │       ├── ActingPayload          S→C  coi-client:acting
      │       ├── ResourcePayload        S→C  coi-client:resource (JSON, one meter per packet)
      │       ├── ActionBarPayload       S→C  coi-client:actionbar (JSON, text components)
      │       ├── TargetHealthPayload    S→C  coi-client:target
      │       ├── CogitationPayload      S→C  coi-client:cogitation
      │       ├── NotifyPayload          S→C  coi-client:notify
      │       ├── CooldownPayload        S→C  coi-client:cooldown
      │       ├── ConditionsPayload      S→C  coi-client:conditions
      │       ├── AppearancePayload      S→C  coi-client:appearance
      │       ├── MythicalFormPayload    S→C  coi-client:mythical
      │       ├── SheetPayload           S→C  coi-client:sheet   (JSON, 1 MiB cap)
      │       ├── ServerInfoPayload      S→C  coi-client:server
      │       ├── MenuPayload            S→C  coi-client:menu    (JSON document, 1 MiB cap)
      │       └── VisualEffectPayload    S→C  coi-client:effect
      ├── state/                — one holder per S→C channel; all parse through json/JsonRead
      │   ├── BeyonderState     — madness, spirituality, health ceiling, pathway + sequence
      │   ├── ConditionsParser  — the `key=value;…` body of coi-client:conditions
      │   ├── SpiritualityTracker — spirituality as the HUD needs it: the server's last figure
      │   │                       plus the regen rate `predictedSpirituality()` extrapolates from
      │   ├── ActingState       — acting progress, method cooldown, last grant
      │   ├── ResourceState     — ability resource meters keyed by id, TTL-expired
      │   ├── ActionBarState    — action-bar entries + client-side TTL expiry
      │   ├── TargetState       — last ability hit (name, before/after, HP)
      │   ├── CogitationState   — cogitation session: prompt, streak, timeout, fail
      │   ├── NotificationState — toast queue (3 visible, rest promoted in turn)
      │   ├── SheetState        — character sheet snapshot (identity, vitals, mind, acting
      │   │                       ledger, sub-menu gates)
      │   ├── SheetParser       — coi-client:sheet JSON → SheetState.Snapshot, defensively
      │   ├── AppearanceState   — appearance traits per player UUID
      │   ├── MenuState         — the current menu document + a revision the screen watches
      │   └── MenuSample        — the hand-written document behind F8 → *Menu*
      ├── hud/
      │   ├── HudGate           — `blocked(client, settings)`: the four refusals every overlay
      │   │                       opens with. Calling it is how an overlay cannot forget the
      │   │                       `HudLayout.editing()` term
      │   ├── HudAnchor         — TOP/BOTTOM × LEFT/CENTER/RIGHT corner math for bars
      │   ├── HudScale          — per-element scaling about the element's own fill origin
      │   ├── HudOpacity        — HudScale's other half: ambient per-element alpha, pushed
      │   │                       around one element's draw and read by `apply(argb)`
      │   ├── HudGaslight       — the HUD's madness lies (wrong cooldowns, swapped slots)
      │   ├── overlay/          — everything that draws on the HUD; each gates on HudGate
      │   │   ├── AbilityOverlay        — all ability slots; owns `slotOrigin` / `boxSize` /
      │   │   │                           `rowStep` / `shiftRow`, the answer to "where is slot N"
      │   │   ├── BeyonderHealthOverlay — replaces the vanilla hearts with the real HP pool,
      │   │   │                           numbers inside the bar
      │   │   ├── CharacterPlateOverlay — one card: head + pathway crest, sanity (brain) and
      │   │   │                           acting (mask) symbol gauges, reserve rows; supersedes
      │   │   │                           the madness / acting / resource bars
      │   │   ├── MadnessOverlay        — madness bar + the stage screen effects
      │   │   ├── SpiritualityOverlay   — spirituality bar (protocol 2 only)
      │   │   ├── ActingOverlay         — acting bar + `+N` gain popup (protocol 2 only)
      │   │   ├── ResourceOverlay       — stack of server-pushed ability resource meters
      │   │   ├── ActionBarOverlay      — COI's own action-bar channel lines above the hotbar
      │   │   ├── TargetHealthOverlay   — hit target's HP bar under the crosshair
      │   │   ├── CogitationOverlay     — centred cogitation prompt card + streak + timer
      │   │   └── NotificationOverlay   — top-right toast stack for `coi-client:notify`
      │   ├── widget/
      │   │   ├── AbilitySlotWidget — single slot: icon, cooldown, keybind, glow, toggle
      │   │   │                       outline + "ON" tag, red strike when locked/blocked
      │   │   └── SlotAnimations    — the three moments a slot animates: the cast punch, the
      │   │                           ready flash, the cooldown sweep
      │   ├── render/           — the paint the overlays share: no state, no render gates
      │   │   ├── CoiBar            — stateless bar layers (frame/fill/shimmer/notches/label)
      │   │   ├── HealthStyle       — which shape the HP pool takes (hearts / bar / ornate / pips),
      │   │   │                       and the per-style default Y offset
      │   │   ├── HealthBarPaint    — every pixel of all four; the absorption rule lives here
      │   │   ├── MadnessPalette    — the bar's colours and status word, one set per stage
      │   │   ├── MadnessCorruption — the full-screen stage effects: vignette, VHS, static
      │   │   ├── PlateCard         — the character plate's card, header and gauge rows
      │   │   ├── PlateSymbols      — the 32×32 fillable brain / mask sprites
      │   │   └── SpiritSprites     — the three first-party spirituality sprites + the luster
      │   └── layout/           — the HUD layout editor's model
      │       ├── HudLayout     — `editing()` flag every overlay's render gate honours
      │       ├── HudElement    — id / label / group / visible / bounds / moveTo / preview / reset
      │       ├── HudElements   — the descriptor list in draw order, `soloSet`, `byId`
      │       ├── LayoutGeometry — the arithmetic every element shares, incl. the anchored-move rule
      │       ├── LayoutGroups  — everything the editor does to a group rather than an element
      │       └── element/      — one descriptor per positionable element
      │           ├── ElementIds      — the stable, never-localized ids
      │           ├── AbstractElement — shared id/label plumbing
      │           ├── BarElement      — a 182-wide bar with its label 10px above it
      │           ├── SlotElement     — one ability slot (slot_1 … slot_10, one group)
      │           └── CharacterPlateElement, BeyonderHealthElement, MadnessElement,
      │                               SpiritualityElement, ActingElement, ResourceElement,
      │                               ActionBarElement, CogitationElement, TargetHealthElement,
      │                               NotificationElement
      ├── effect/
      │   ├── EffectManager     — registry + active list, renders via HudRenderCallback
      │   ├── VisualEffect      — interface (start/render/isFinished/stop)
      │   ├── EffectSounds      — the audio companions (loops and one-shots)
      │   ├── HallucinationManager — client-side madness hallucinations, on the client tick
      │   └── visual/           — the effects themselves
      │       ├── EffectParams  — the `key=value,key=value` param splitter
      │       ├── EffectPaint   — the 2D paint helpers vanilla does not expose
      │       ├── CracksEffect, EyesEffect, VignetteEffect, HeartbeatEffect, GlitchEffect,
      │       │                   BloodRainEffect, FrostEffect, WhispersEffect, TunnelEffect,
      │       │                   FlashEffect, HallucinationEffect
      │       ├── ImpactFrameEffect — the world-space spell impact; a shell over impact/
      │       └── impact/       — ImpactStyle (the presets), ImpactGeometry (the randomised
      │                           shapes), ImpactRenderer (every mark it puts on the world or
      │                           the screen), WorldImpact (one impact playing out at a position)
      ├── gesture/
      │   ├── GestureType       — 5 shapes (circle, V, Z, line down, triangle):
      │   │                       direction templates + preview polylines
      │   ├── DirectionCodes    — resampled stroke → 8-way direction string
      │   └── GestureRecognizer — resample → DirectionCodes → Levenshtein match
      ├── form/                 — mythical creature forms (see below)
      │   ├── MythicalFormManager  — uuid → pathway map, fed by coi-client:mythical
      │   ├── MythicalCreatureForm — per-pathway form; pathway/ holds all 20
      │   ├── FormPrimitives    — the hand-authored cuboid geometry the full forms are drawn from
      │   ├── PartialFormSpec   — placement/scale of a baked lower-body model
      │   ├── PartialForms      — shared resolve + carrier-transform helpers
      │   ├── PartialFormLayer  — draws the baked model as a player render layer
      │   ├── FormModel         — the `carrierDelta()` contract a baked model can implement
      │   ├── FormModelLayers   — registration + placement of the baked models
      │   ├── pathway/          — the 20 pathway forms (FoolForm, SunForm, VisionaryForm, …)
      │   └── model/            — Blockbench exports (VisionaryLowerModel/Animations);
      │                           **generated — regenerate from Blockbench, never hand-edit**
      ├── appearance/
      │   ├── AppearanceTraitLayer    — render layer for the traits the server granted
      │   ├── AppearanceTraitRenderer — one additive trait
      │   ├── TraitGeometry           — smooth tubes/quads/triangles in block units
      │   └── trait/                  — HornsTraitRenderer, MushroomTraitRenderer,
      │                                 FemaleTraitsRenderer
      ├── menu/                 — the declarative menu document (server-authored screens)
      │   ├── MenuDocument      — session / version / screen id, header, sections, footer
      │   ├── MenuComponent     — sealed: text, note, stat, kv, checklist, button(s), toggle,
      │   │                       list, grid, input, divider, spacer, plus the v2 five
      │   │                       (hero, details, steps, chips, panels)
      │   ├── MenuIcon          — pathway emblem / item model / glyph / ability / player head / none
      │   ├── MenuParser        — defensive Gson; unknown `type`s skipped, sizes clamped
      │   ├── MenuParts         — the structures that appear in more than one component
      │   ├── MenuJson          — the defensive reads a document is parsed with
      │   ├── MenuStyles        — the wire's enum words, folded onto this client's enums
      │   └── MenuLimits        — how much of a document this client will read
      ├── config/
      │   ├── AbilityConfig     — persists slot bindings → config/coi_abilities.json
      │   ├── HudConfig         — every HUD setting → config/coi_hud.json
      │   ├── HudConfigReader   — the on-disk key names, read as "the value, else the default"
      │   ├── HudConfigWriter   — the same key names, written back
      │   ├── HudConfigMigrations — brings an older file up to `HudConfig.LAYOUT_VERSION`
      │   ├── HudSettingsCopy   — the field-by-field copy behind the settings screens'
      │   │                       working copies
      │   └── ClientStateStore  — persistent state, not preferences → coi_client_state.json
      ├── data/
      │   ├── ClientDataLoader  — the mod's own resource-pack data (the pathway archive), and
      │   │                       the reload hook clearing IconModels / PlateSymbols / CoiIcons
      │   └── IngredientInfo    — one archive entry: which pathway and sequence wants an item
      ├── json/
      │   └── JsonRead          — the defensive scalar reads every state class shares
      ├── ui/
      │   ├── CoiStyle          — shared dark/gold palette + card chrome; the BACKDROP / SCRIM /
      │   │                       VEIL dimming ladder, `cardWidth` and `formWidth`
      │   ├── CoiIcons          — the mod's first-party GUI icons, and the one way to draw them
      │   ├── AbilityIcons      — shared icon renderer: pack item model, else category/tier
      │   ├── IconModels        — "is that item model loaded?", cached per reload
      │   └── IngredientTooltips — names the pathway an item belongs to, in its tooltip
      ├── presence/
      │   └── DiscordPresenceManager — Discord Rich Presence via discord-game-sdk4j
      │                                (pure-Java IPC, bundled jar-in-jar); lazy connect
      │                                on first join, APP_ID = 0 disables it entirely
      ├── duck/                 — duck interfaces the mixins implement. **Must live outside
      │   │                       `mixin/`**: the mixins.json `package` owns that whole subtree,
      │   │                       and Mixin refuses to let ordinary code load a class from it
      │   └── AvatarRenderStateAccessor — the player UUID `AvatarRenderStateMixin` adds
      ├── mixin/                — every mixin and accessor
      │   ├── LivingEntityRendererMixin — the mythical form: cancels the vanilla player render
      │   │                       for a full form, pushes hipRaise + the carrier transform
      │   │                       for a partial one
      │   ├── AvatarRendererMixin / AvatarRenderStateMixin (with `duck/AvatarRenderStateAccessor`) —
      │   │                       attach the mod's layers, and carry a player UUID on a render
      │   │                       state vanilla gives no identity to
      │   ├── PlayerModelMixin / HumanoidArmorLayerMixin — hide legs, then leggings + boots,
      │   │                       under a partial form
      │   ├── TitleScreenMixin  — the title-screen haunting hook, and the takeover's scene
      │   │                       (`extractBackground`, in place of the panorama)
      │   ├── LogoRendererMixin / SplashRendererMixin — suppress the vanilla wordmark, and
      │   │                       redraw the splash off ours, while the takeover is on
      │   ├── AbstractButtonMixin — the title screen's button chrome (`extractDefaultSprite`)
      │   ├── LoadingOverlayMixin / JoinMultiplayerScreenMixin / OnlineServerEntryMixin — the
      │   │                       startup logo and the Mysterria server-list entry
      │   └── EntityRendererAccessor / LivingEntityRendererAccessor / SelectionListEntryInvoker
      └── screen/               — the chrome these draw with lives in ui/CoiStyle
          ├── ScreenInput       — the keyboard concerns the mod's own screens share
          ├── ScrollbarPainter  — the 3px track-and-thumb bar drawn *beside* a card
          ├── GestureScreen     — hold Left Alt, draw with mouse, release to cast;
          │                       inert until a gesture has an ability bound
          ├── TourScreen        — first-join walkthrough: spotlight cutouts + text cards,
          │                       movement stays enabled; re-run via "Show Tour Again"
          ├── TitleScreenHaunt  — the main menu remembers the madness you left with
          ├── title/            — the Lord of the Mysteries main menu (see below)
          │   ├── TitleTakeover — the gate, the time base, the geometry and the one accent
          │   ├── TitleScene    — the void: gradient, central bloom, drifting motes
          │   ├── PathwayWheel  — the two rings, the 24 emblems, the lit one, the corruption
          │   ├── TitleLogo     — the wordmark, the pathway caption, the relocated splash
          │   └── TitleButtons  — the plate drawn in place of a vanilla button sprite
          ├── InventoryHint     — a strip on the vanilla inventory saying the Mystery Arts
          │                       item is gone and naming the live "open menu" keybind;
          │                       dismissed once, remembered in coi_client_state.json
          ├── sheet/            — the Beyonder character sheet (opened with M)
          │   ├── CharacterSheetScreen — one `MenuTheme` card, drawn as a menu document would be
          │   ├── SheetContext / SheetMetrics / SheetPalette — what a section is handed to draw
          │   │                       itself with, the measurements + the one hit test its cards
          │   │                       use, and every colour the sheet decides for itself
          │   ├── SheetHero, SheetVitals, SheetActing, SheetConditions, SheetDestinations,
          │   │   SheetFooter, SheetRows, SheetChips — one section each, in draw order
          │   └── SheetGlyphs   — the sheet's 8×8 drawn marks (heart/flask/hourglass, the
          │                       seven destination glyphs, lock)
          ├── ability/
          │   ├── AbilityBindingScreen — bind abilities to slots (opened with K); tabbed
          │   │                       (hotkeys/wheel/gestures) with a per-tab how-to banner
          │   ├── BindingRowPainter — one row of that screen's slot list
          │   ├── AbilityWheelScreen — the radial picker, held open on G
          │   ├── AbilityPickerOverlay — the modal chooser: the screen, its input and its scroll
          │   ├── PickerModel   — the flat Row list: the search, the grouping, the row heights
          │   ├── PickerPainter — one row at a time, plus the tooltip after the scissor
          │   ├── PickerMetrics — ROW_H 26 / HEADER_H 13 / UNBIND_H 18 / MESSAGE_H 18
          │   └── PickerLabels  — every word a row or its tooltip puts on screen
          ├── settings/
          │   ├── HudSettingsScreen — HUD customization: 3 tabs (Ability HUD/Elements/General),
          │   │                       scrollable rows so it fits any gui scale. Holds **no**
          │   │                       position rows: "Arrange on screen…" plus the per-element
          │   │                       *Align* buttons are the only position UI
          │   ├── SettingsTabs  — the contents of those three tabs, one method each
          │   ├── SettingsRows  — the row builders the tabs compose
          │   ├── HudLayoutScreen — drag-to-position editor: sample previews of every
          │   │                       element over the live world — or one element / one group,
          │   │                       in solo mode — snapping, Ctrl group move, nudge keys
          │   ├── LayoutPainter — what that editor paints over the world
          │   └── LayoutSnap    — where a dragged element actually lands
          ├── menu/             — the server-authored menu renderer
          │   ├── MenuScreen    — one scrolling card: header + sections + footer,
          │   │                   hand-drawn buttons, live list search, confirm modal,
          │   │                   collapsible sections + details, draggable scrollbar
          │   ├── MenuTheme     — every colour and primitive the screen draws with
          │   ├── MenuContext   — everything a collaborator may ask the screen for, including
          │   │                   `approach` (the one easing chokepoint)
          │   ├── MenuPart / MenuPartFactory — one laid-out piece in content space, and the
          │   │                   document → flat part list the card scrolls
          │   ├── MenuTextParts / MenuValueParts / MenuControlParts / MenuCollectionParts —
          │   │                   the words, the values, the things the player operates, and
          │   │                   the parts that repeat a cell
          │   ├── MenuChrome, MenuGauges, MenuIcons, MenuScrollbar, MenuConfirmModal
          │   └── MenuMetrics   — the geometry a document is drawn to, and its one hit test
          ├── widget/
          │   └── CoiTabButton  — hand-drawn tab widget used by binding + settings screens
          └── debug/            — dev-only (F8)
              ├── EffectDebugScreen — test effects, forms, bars and menus without a server
              ├── DebugStates       — the state pokes behind its buttons
              └── AppearanceDebugScreen — preview the appearance traits
```

## Shared seams

Five small classes exist only so the same decision cannot be made twice. Reach for them before
retyping what they hold.

- **`hud/HudGate.blocked(client, settings)`** — the four refusals every overlay's render gate opens
  with: no player, `hud.isHidden()`, `HudLayout.editing()`, master switch off. The editing term is
  the one that matters and the easy one to leave out — an overlay that forgets it draws its live
  self underneath the editor's preview, which is the one way to break the layout editor. A new
  overlay calls `HudGate.blocked` rather than retyping the chain; whatever else it needs (its own
  `show*` toggle, whether the server has sent data, the plate superseding it) stays in the overlay,
  because no two of those agree.

  Two overlays deliberately do more with the gate than return early:
  `overlay/BeyonderHealthOverlay` turns it into a **fall-through** — it replaced a vanilla element,
  so `blocked` means "draw the hearts", not "draw nothing", and its `render` returns a boolean the
  wrapper uses to call the original (it also hands the frame back in creative and spectator rather
  than re-deriving vanilla's rule). `overlay/MadnessOverlay` passes the gate and *then* draws
  `MadnessCorruption.screenEffects` **before** the `showCharacterPlate` check, because the stage
  vignette is the world reacting to the player, not a readout — the plate supersedes the bar, never
  the effects.

- **`dev.ua.ikeepcalm.coi.CoiLog`** — the mod's one logger (`CoiLog.LOG`, named "COI Client"). Every
  hand-written `System.out.println("COI Client: …")` is gone; the prefix is now the logger's name,
  so it cannot drift between call sites, and the lines land with a level and a timestamp like every
  other mod's.

- **`network/payload/CoiPayloads`** — the shape every payload record is built from: the `coi-client`
  namespace, the `MAX_DOCUMENT` (1 MiB) and `MAX_ACTION` (32 KiB) caps, and the read/write pair.
  Both ends have to agree on a cap or the packet is rejected mid-flight, so it is stated once here
  rather than 22 times. A payload class is then its record components, its id and its codec — which
  is what makes a mismatch with the plugin visible at a glance.

- **`json/JsonRead`** — the defensive scalar reads (`string`, `intOf`, `dbl`, `bool`, `object`, …)
  that every `state/` class shares. Each answers "absent" with the caller's default instead of
  throwing, so a `handle` method only guards the two things that genuinely are fatal: a body that is
  not an object, and a value of the wrong Java type. `menu/MenuJson` is the same idea for menu
  documents, with the length clamps a document needs.

- **`ui/CoiStyle`** — besides the dark/gold palette and the card chrome, it now names the screen
  dimming ladder: **`BACKDROP`** for a modal that owns the screen, **`SCRIM`** for one the player is
  expected to glance past, **`VEIL`** for the layout editor, where the world has to stay readable
  while it is dragged on. Choosing between them is a judgement about the screen behind, not a taste
  in alpha, which is why they are named rather than typed as hex at the call site. It also carries
  **two** width rules: `cardWidth` for a scrolling content card (the sheet and the menus, which are
  the same object to a player) and `formWidth` for a settings-style column of labelled rows (the
  binding screen, HUD settings). `formWidth` is deliberately **not** `cardWidth` — a form stops
  being readable long before a document does, so it takes a wider gutter and caps far lower, and
  keeping the rules apart is what stops a later widening of the reading card from stretching the
  forms with it.

**The big screens are shells over collaborators.** `MenuScreen`, `CharacterSheetScreen`,
`AbilityPickerOverlay`, `HudSettingsScreen`, `HudLayoutScreen`, `ImpactFrameEffect` and
`HudElements` each kept their public surface and their behaviour, and handed the drawing out:

| Shell | Keeps | Hands out |
|-------|-------|-----------|
| `screen/menu/MenuScreen` | the vanilla `Screen` lifecycle, the session's `__close` guard, scroll, `approach` | `MenuPartFactory` → `MenuPart`s; `Menu{Text,Value,Control,Collection}Parts` draw them; `MenuChrome`/`MenuGauges`/`MenuIcons`/`MenuScrollbar`/`MenuConfirmModal`/`MenuMetrics` |
| `screen/sheet/CharacterSheetScreen` | the `sheet_open`/`sheet_close` lifecycle, the card, the scroll | one `Sheet*` class per section, all handed a `SheetContext`; `SheetMetrics`/`SheetPalette` |
| `screen/ability/AbilityPickerOverlay` | the modal, the search box, the keyboard | `PickerModel` (rows), `PickerPainter` (paint), `PickerMetrics`, `PickerLabels` |
| `screen/settings/HudSettingsScreen` | the tabs, the working copy, Done/Cancel | `SettingsTabs` (which rows), `SettingsRows` (how a row is built) |
| `screen/settings/HudLayoutScreen` | selection, drag, keys | `LayoutPainter` (what is drawn over the world), `LayoutSnap` (where it lands) |
| `effect/visual/ImpactFrameEffect` | the `VisualEffect` contract and the params | `ImpactStyle`, `ImpactGeometry`, `ImpactRenderer`, `WorldImpact` |
| `hud/layout/HudElements` | `all` / `byId` / `soloSet` / `moveGroupBy` | one `element/*Element` descriptor per element; `LayoutGeometry`, `LayoutGroups` |

The seam is always the same: the shell owns *state and lifecycle*, the collaborators own *pixels and
arithmetic* and are handed everything they need. So a rendering change is a change in one
collaborator, and none of the invariants recorded below moved with the code.

## Network Protocol

Full reference: **[docs/NETWORK_PROTOCOL.md](docs/NETWORK_PROTOCOL.md)**

Summary (protocol 2):

- C→S `coi-client:use` — activate ability by id + action (`execute`/`left_click`)
- C→S `coi-client:request` — request available abilities list
- C→S `coi-client:hello` — capability handshake, sent on JOIN **before** `request`
- C→S `coi-client:action` — `{"action":"open_menu"}` (gated on `menu_action`), plus the character
  sheet's `sheet_open`, `sheet_close`, `{"action":"open","target":…,"ui":…}` and `toggle_terrain`
- C→S `coi-client:menu_action` — a click inside a menu document: `{session, version, action, value?}`
- S→C `coi-client:abilities` — pipe/semicolon delimited ability data
- S→C `coi-client:cooldown` — ability id + ticks
- S→C `coi-client:effect` — trigger/stop a visual effect
- S→C `coi-client:mythical` — transform a player into a pathway form (see Mythical Creature Forms)
- S→C `coi-client:conditions` — beyonder state (madness, spirituality, …) for the local player
- S→C `coi-client:appearance` — appearance traits per player UUID
- S→C `coi-client:server` — plugin version + server feature list (reply to `hello`)
- S→C `coi-client:abilities_v2` — rich ability list as JSON (description, cost, cooldown, lock
  state, icon model, toggle state, passives included); replaces `abilities` for clients that
  advertise `ability_meta`
- S→C `coi-client:state` — one ability's toggle or category change
- S→C `coi-client:acting` — acting progress, method cooldown, overflow, last grant
- S→C `coi-client:resource` — one ability resource meter (id, label, current/max, colour, ttl,
  percent/value format); `{"id":…,"remove":true}` takes it off the HUD
- S→C `coi-client:actionbar` — COI's sorted action-bar entries (channel, key, priority, ttl,
  text component); replaces the vanilla action bar for clients that advertise `action_bar`
- S→C `coi-client:target` — one packet per ability hit (name, before/after fraction, HP, damage)
- S→C `coi-client:cogitation` — start / prompt / fail / stop for a cogitation session
- S→C `coi-client:notify` — a toast (kind, title, body, colour, duration)
- S→C `coi-client:sheet` — the whole character sheet as JSON; pushed on `sheet_open`, after
  `toggle_terrain`, and every 60 ticks while the sheet is open
- S→C `coi-client:menu` — a whole server-authored screen as a JSON document; `{"closed":true}`
  takes it away again

Ability wire format (v1): `id|localizedName|englishName|category|hasLeftClick` per entry, `;`
separated. In-memory format: `"id - englishName"`.

Client feature ids (`ClientFeatures.SUPPORTED`): `ability_hud`, `hotkeys`, `effects`, `appearance`,
`mythical`, `conditions`, `spirituality_hud`, `menu_action`, `ability_meta`, `ability_state`,
`acting_hud`, `action_bar`, `target_health`, `cogitation`, `notify`, `character_sheet`,
`resource_bar`, `menu_ui`.

**Ability icons** — `ui/AbilityIcons.draw` prefers the resource pack's per-ability item model (`AbilityInfo.icon()`,
rendered as a glowstone-dust stack with `DataComponents.ITEM_MODEL` set, posed
to the box size) and falls back to the bundled `textures/icons/<category>/<tier>.png`, with the
category whitelisted against the 14 shipped folders. `ui/IconModels` decides which, by looking for
`<ns>:items/<path>.json` in the resource manager — cached per icon id, cleared on resource reload
and on disconnect.

**Capability gating** — `ClientFeatures` lists what this client renders; `ServerCapabilities` holds
the server's reply and is reset on disconnect. A server that never replies leaves every
`ServerCapabilities.has(...)` false, so client-only UI stays hidden and the legacy server paths (boss bars, slot-9
shortcut item) keep working unchanged.

## Visual Effects System

Full reference + server integration guide: **[docs/VISUAL_EFFECTS.md](docs/VISUAL_EFFECTS.md)**

Effects are triggered server-side via `VisualEffectPayload(effectId, params)`.
`EffectManager` maintains the active list and renders all effects via `HudRenderCallback`.
Effects support `params = "stop"` to remove, `effectId = "all"` to clear all.

Available effects: `vignette`, `heartbeat`, `cracks`, `eyes`, `glitch`, `bloodrain`, `frost`, `whispers`, `tunnel`, `flash`, `impact`, `hallucination`

**Sound layer** — `EffectSounds` plays audio companions for effects (loops for `heartbeat`/`whispers`/`tunnel`, one-shots for `cracks`/`frost`/`glitch`); assets in `assets/coi-client/sounds/` + `sounds.json`. Volume via `effectSoundVolume` HUD setting.

**Madness hallucinations** — `HallucinationManager` (client tick) fires phantom positional sounds and visual flickers once `BeyonderState` madness ≥ 25, scaling with stages 25/50/75; darkness/night makes events up to ~2.5x more frequent. Server can force one via the `hallucination` pseudo-effect (`event=footsteps|whisper|cave|block|flicker|random`). Toggle: `enableHallucinations` HUD setting, which also gates:
- **HUD gaslighting** (`hud/HudGaslight`) — at madness ≥ 75 the HUD briefly lies: wrong cooldown numbers, glitched keybind glyphs, two slots trading places.
- **Title screen haunting** (`screen/TitleScreenHaunt` + `TitleScreenMixin`) — corruption (max of madness at disconnect and permanent madness, incl. debug-screen values) is persisted to `config/coi_client_state.json` (`ClientStateStore`); the main menu shows a scaled vignette, occasional eye apparitions, and whisper splash lines (`title.coi.haunt_splash.*`). The same figure drives the title screen takeover's corruption (see *The Title Screen Takeover*), and the haunt still draws last, over whichever menu is underneath. Clean players always get LOTM flavor splashes (`title.coi.splash.*`) — not gated by the hallucinations toggle.

**Debug screen** (dev environment only, F8): lists all registered effects with Test/Stop buttons and a params input field. `shouldPause()` returns false so effects are visible while the screen is open.

## Mythical Creature Forms (`form/`)

S→C `coi-client:mythical` (`MythicalFormPayload`, `targetUuid` + `pathway:<unused>:start|stop`) marks
a player's UUID as transformed into a pathway's form. Two kinds:

- **Full forms** — the vanilla player render is cancelled outright (`LivingEntityRendererMixin` at HEAD)
  and replaced with procedural geometry drawn from `FormPrimitives`. 19 of the 20 pathways.
- **Partial forms** — a baked Blockbench model stands in for the *lower body* while the player's own
  head/torso/arms keep rendering. Currently Visionary only (`FormModelLayers.VISIONARY_LOWER_SPEC`).

Partial forms are assembled from four pieces that all have to agree:

| Piece | Job |
|-------|-----|
| `PlayerModelMixin` (`setupAnim` TAIL) | hides leg parts; must run in `setupAnim`, since submission is deferred |
| `HumanoidArmorLayerMixin` | hides leggings/boots, which draw from their own model set |
| `LivingEntityRendererMixin` | `hipRaise` push (world space, at HEAD) + **carrier transform** push (model space) |
| `PartialFormLayer` | draws the baked model, undoing the carrier transform it inherits |

**The carrier transform** is what makes the halves read as one body. The rig's torso bone (its
"carrier") both rotates and translates during the walk cycle, around a pivot that is over a block
away from the player's waist. `FormModel#carrierDelta` hands out that bone's full rigid motion,
`PartialForms#carrierTransform` converts it into player space, and the renderer mixin pushes it onto
the pose stack just before the model is submitted — so the player *and* every layer above it (armor,
held items, cape, appearance traits) ride the torso exactly. Copying the rotation angle alone is not
enough and looks like shearing: same tilt, wrong pivot, no translation.

Placement knobs live in `FormModelLayers`; read the comment there before touching one. `hipRaise` and
the carrier push sit in different coordinate spaces on purpose — see `LivingEntityRendererMixin`.

`form/model/VisionaryLowerModel` and `VisionaryLowerAnimations` are **Blockbench exports**. They are
generated files: change the rig in the modelling tool and re-export, never hand-edit the Java. A
hand edit is invisible until the next export silently reverts it.

**Dev testing** (no server needed): F8 → *Form: None (Click to cycle)* applies a form to yourself.

## HUD Bars

`MadnessOverlay` and `SpiritualityOverlay` both draw a 182×6 bar and share two helpers:

- `hud/HudAnchor` — `parse(String)` + `resolve(screenW, screenH, barW, xOffset, topY, bottomYOffset)`
  returns `{x, y}`. Six anchors: TOP/BOTTOM × LEFT/CENTER/RIGHT, where LEFT means `x = 10 + xOffset`
  and RIGHT its mirror, `x = screenW - barW - 10 + xOffset` (`HudAnchor.MARGIN`); `isTop()`,
  `isCenter()` and `isRight()` answer which. Every bar now passes its own Y offset for both arguments — the madness bar
  included, since `layoutVersion` 2 (`MadnessOverlay.DEFAULT_TOP_Y = 20` is only the default) —
  so any bar can be dragged whichever edge it is anchored to. `TourScreen` spotlights call the same method,
  which is why they can't drift from the overlays.
- `hud/render/CoiBar` — stateless layers: `frame`, `fill` (gradient + bevel), `shimmer`, `notches`,
  `label` (centred 10px above the bar), `lerpWidth`, `withAlpha`. Anything madness-specific (glitch
  slices, cracks, static, permanent-madness marker, the extras line) stays in its own overlay.

The spirituality bar only draws once a protocol-2 server has actually sent `spirituality` on
`conditions` (`BeyonderState.hasSpiritualityData()`); it goes red below 30% of max, its label
goes red below 25%, and with `spiritualityHideWhenFull` it eases out at full instead of sitting
there. Dev testing: F8 → the *Spirit* buttons.

The spirituality bar is no longer a `CoiBar` composite: it blits three first-party sprites from
`textures/gui/hud/` (`spirituality_fill.png` 182×5 clipped to progress, `spirituality_frame.png`
184×25 drawn at `(fillX-1, fillY-9)`, and `spirituality_frame_critical.png` crossfaded in below
30%), with the luster — edge bloom, motes, drain trail — drawn as plain `fill`s in
`SpiritualityOverlay` and the geometry in `hud/render/SpiritSprites`. Numbers only, right-aligned above
the fill; the flask ornament is the label. `BeyonderState.predictedSpirituality()` extrapolates
between the server's once-a-second regen steps from a rate measured off the last two increases (discarded on any
decrease, capped at max/2 per second and 1.5 s ahead), and hide-when-full now
appears instantly and only retreats after a 1.5 s hold plus a 900 ms fade. The
`hud.coi.spirituality_label` lang key is now unused — leave it in place.

`ActingOverlay` is a third, thinner (182×4) bar using the same two helpers, coloured by
`Pathways.pathwayRgb`. It draws only while `ActingState.hasData()` and the pathway is not
an `OuterPathway`, shows the method cooldown as `mm:ss` on the label, and floats a pathway-coloured
`+N` above the label for 1.2s after a grant. Dev testing: F8 → the *Acting* buttons.

`ResourceOverlay` draws the server's ability resource meters as a stack of 182×4 bars in the
colour each packet names, 18 px apart, growing downwards from a `TOP_*` anchor and upwards from a
`BOTTOM_*` one (`HudAnchor.isTop()`), capped at `resourceMaxBars`. Bars are keyed by id so a
refresh never reorders the stack, and each one expires on its own `ttlMs` unless the server keeps
re-sending it. Dev testing: F8 → the *Resource* / *Res clear* buttons.

## HUD Layout Editor

`screen/settings/HudLayoutScreen` (HUD Settings → *Arrange on screen…*, or *Align* on any element's row)
drags every positionable element into place over the live world. It is **the only position UI**:
the settings tabs carry no anchor cycles and no X/Y offset rows any more — only the show toggles,
sizes and an *Align* button on each element's header. The fields themselves are untouched, still
written to `config/coi_hud.json`, and the presets still assign them. The screen is a thin shell over
`hud/layout/` — it owns selection, the drag and the keys, `LayoutPainter` owns what is drawn over
the world and `LayoutSnap` owns where a dragged element lands:

- `HudElement` — `id` / `label` / `group` / `visible` / `bounds` / `moveTo` / `renderPreview` /
  `resetPosition` / `resetGroup`. `bounds` and `moveTo` are inverses in gui-scaled pixels: after
  `moveTo(x, y, …)`, `bounds` reports `(x, y)` again. Previews draw **sample data** and must never
  touch a class under `state/`. One implementation per element lives in `hud/layout/element/`
  (`SlotElement`, `BarElement` and the nine descriptors over it), with the ids in `ElementIds` —
  never localized, since they go into `coi_hud.json` and into an *Align* button's argument.
- `HudElements.all(settings)` — the descriptors in draw order: `slot_1` … `slot_N` (only the first
  `activeAbilitySlots` of the ten), then `character_plate`, `beyonder_health`, `madness`,
  `spirituality`, `acting`, `resources`, `action_bar`, `target_health`, `cogitation`,
  `notifications`. The list depends on the settings, so the screen builds it once in its
  constructor, and renders forwards / hit-tests backwards so you grab what you see.
- `LayoutGeometry.applyAnchoredMove(...)` — the one rule for anchored bars: the anchor follows the half
  of the screen the bar landed in (`TOP_*` above the middle, `BOTTOM_*` below), and horizontally
  `CENTER` within 12 px of the screen's centre line (and then a near-zero X offset snaps to a true
  0), else `LEFT` (`x = 10 + xOffset`) or `RIGHT` (`x = screenW - barW - 10 + xOffset`) depending on
  which side of the centre line the element's own centre lands. Stacks taller than one bar pass
  their own top/bottom decision.
- **Ability slots are one element each.** `slot_1` … `slot_10` all return `group() ==
  "ability_slots"`; `HudElements.soloSet(id, s)` resolves an *Align* id to a whole group or to a
  single element, and `HudElements.moveGroupBy(…)` is the Ctrl-drag (both delegate to
  `hud/layout/LayoutGroups`, which is where everything group-shaped lives): slots still in the shared row
  ride `hudX`/`hudYOffset` (shifted once, through `AbilityOverlay.shiftRow`), the rest take the
  same delta through their own `moveTo`. `resetGroup` puts the row origin back; `resetPosition` on a
  slot only drops that slot's own placement.
- `HudLayout.editing()` — **every overlay's render gate must include it**, right after the
  `client.gui.hud.isHidden()` check, or the editor will draw its preview on top of the real thing.
  A new overlay that forgets this is the one way to break the editor — which is exactly why the
  whole chain now lives in `hud/HudGate.blocked`, and why an overlay calls that rather than
  retyping it.

**Solo mode** — opened with a preselected id (an *Align* button), the screen resolves it in its
constructor to a one-element list, or — for `ability_slots` — to the whole slot group, and every
loop iterates that `elements` list: nothing else is drawn, outlined, chipped or hit-tested. The
title line above the toolbar reads `screen.coi.layout_title_one` ("Align: <element>", carrying the
group's own `layout_el_ability_slots` label for the slots), `Reset all` becomes
`screen.coi.layout_reset_one` acting on that set alone (plus one `resetGroup` per group), `Tab` is
inert only when the set holds a single element, and a click on empty space keeps the selection.
Cancel / Done / Esc are unchanged. Opened with no id ("Arrange on screen…") the screen lists
everything.

**Group move** — hold **Ctrl** while dragging (or nudging with the arrows) any element that has a
`group()` and the whole group moves by the same delta, each member clamped on screen. The hint
`screen.coi.layout_hint_group` shows above the toolbar only while such an element is selected.
Modifier state comes off the `MouseButtonEvent` / `KeyEvent` (`InputWithModifiers.hasControlDown()`).

Drag snaps to a 4 px grid (Shift for free placement), to the screen centre and to a 10 px margin
within 6 px; arrows nudge 1 px (Shift 8), `R` resets the selection, right-click resets the element
under the cursor, `Tab` cycles (all-elements mode only), `T` flips the toolbar to the other edge,
`Esc` is Done.

**The toolbar gets out of the way.** It is opaque chrome over a live HUD and `inToolbar` turns every
click inside it into a no-op, so an element parked underneath is not merely hard to see — it cannot
be selected, dragged or right-click reset at all. Parked at the bottom it lands squarely on anything
bottom-centre, which is what the Beyonder health element is by definition, and that is what players
reported as the bar they could not move. `chooseToolbarSide` counts how many of the screen's own
elements each candidate band would cover and takes the emptier edge; ties keep the bottom, where the
toolbar has always been. The count is taken **once per `init`**, never per frame — re-deciding while
the player drags would pull the toolbar out from under the cursor mid-gesture — and `T` overrides it
for good, so a resize cannot park it back on what was just uncovered. The hint lines outside the
card are **stacked** rather than placed at fixed offsets (`outsideHintY`), since two of the three are
conditional and the card can be on either edge. Opened from HUD Settings it edits **that screen's working
copy**; opened otherwise it edits the live settings.

**Done commits, whichever settings object the screen was handed.** `persist()` writes a working copy
through to the live settings before saving. Leaving the commit to the settings screen's own Done
meant a player could arrange the whole HUD, press Done here, then press Cancel or Esc on the screen
*behind* this one and lose every drag — which is not something "Done" can mean, and is what players
reported as the layout resetting itself. The write-through carries whatever rows the settings screen
had already changed in that copy too: the two screens edit one object, and a rule that committed
half of it would be harder to predict than one that commits it. Cancel is unaffected — it restores
its snapshot into `settings` and never reaches `persist`.

**`slotSize` is the ability HUD's only size knob.** The slot is drawn at
`HudConfig.BASE_SLOT_SIZE` (40) under a pose scale of `slotSize / 40`, so the keybind chip, the
ability name and the cooldown readout grow with the box instead of staying stuck at one font size —
which is what a plain "draw the box bigger" setting could never do. `MIN_SLOT_SIZE`/`MAX_SLOT_SIZE`
are exactly `MIN/MAX_ELEMENT_SCALE × 40`, so the slider's range maps onto the supported scale band
with nothing to clamp away. Three helpers keep the relationship in one place:
`AbilityOverlay.slotScale(s)`, `boxSize(s)` (`== slotSize`) and `rowStep(s)`
(`boxSize + 10×scale`).

**Per-slot placement** — `AbilityOverlay.slotOrigin(index, w, h, settings)` is *the* answer to
"where does slot N go": the shared row (`hudX + index * rowStep(s)`, `h - hudYOffset` — `rowOrigin`
is plain screen pixels, since the scale grows the slots rather than displacing the row) while
`settings.slotPlacements[index]` is null, otherwise that placement's own `HudAnchor.resolve`, fed
`boxSize(s)`. Either way the box is **clamped** into `[0, w - boxSize(s)]²`, so no slot can be drawn
off screen. `slotBounds` adds the scaled name line under the box, `rowBounds` unions every active
slot (the tour spotlight reads it), and the render loop, the editor previews and `HudGaslight`'s
slot swap (two indices trade origins) all go through `slotOrigin`. Slots sit at their index, not at
their rank among the bound ones.

Positions the editor writes are the same fields the old rows write, so only two things migrate. The
madness bar used to ignore `madnessYOffset` while top-anchored (`layoutVersion` 2: a pre-v2 file gets
`madnessYOffset = 20` back if its anchor is a TOP one). `layoutVersion` **3** retired `hudScale` and
`slotSpacing`: `hudScale` *divided* `hudX`/`hudYOffset` instead of scaling anything, and
`slotSpacing` was a second size knob free to disagree with `slotSize`. Neither has a
position-preserving conversion (the old scale formula depended on the screen height), so a pre-v3
file keeps its offsets verbatim and the two keys are simply dropped on the next save — slots 2..N in
the shared row shift to the derived step. `layoutVersion` **5** sets `beyonderHealthStyle` to `HEARTS` and re-places the
health element: a pre-v5 file predates the setting, so its stored placement was calibrated for a bar
sitting *on* the hearts' row, where the new default's readout would land on the hearts it has just
handed back. `migrate` guards each step by the version that introduced it, so a file at version 0
gets all of them.

## Character Plate

`hud/overlay/CharacterPlateOverlay` replaces three of the four look-alike bars with **one card**. Madness,
acting and the reserve stack were the same 182px `CoiBar` recipe stacked down the top-left corner,
which read as clutter; the plate gives them a shared home and each gauge its own *shape*. The
overlay is the gate and the data; `hud/render/PlateCard` is the card, the header and the rows.

| Row     | Source                              | Notes                                                                                                                     |
|---------|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| header  | player skin + `BeyonderState` | 16×16 head (face `u=8,v=8`, hat `u=40,v=8`), name, the pathway emblem through `CoiIcons.drawPathwayEmblem` tinted `Pathways.pathwayRgb` + `· SEQUENCE n` |
| sanity  | `100 - madness`                     | brain symbol; permanent madness is a **ceiling** the fill can't reach, drawn as a dark capped band on both symbol and bar |
| acting  | `ActingState`                 | mask symbol, `mm:ss` method cooldown, the `+N` grant popup                                                                |
| reserve | `ResourceState`               | under a divider, one compact row each, capped by `resourceMaxBars`                                                        |

**`characterPlateOpacity`** (Elements tab, default `1.0`, clamped
`HudConfig.MIN_ELEMENT_OPACITY`…`MAX_ELEMENT_OPACITY` = 0.15–1.0) fades the whole card so the world
shows through it. The floor is **not zero**: below roughly `alpha < 4/255` the font renderer stops
honouring the alpha channel, so a slider reaching zero would make the numbers behave differently
from the card they sit on — and an element faded to nothing is indistinguishable from one the
show/hide checkbox turned off. It needs no migration and no `layoutVersion` bump, since 1.0 is
exactly the old behaviour.

**Every colour the plate draws goes through `HudOpacity.apply`** — fills, outlines, text colours and
the ARGB tints handed to `ctx.blit` (the player head, the brain/mask sprites). One colour that skips
it is a piece of the card that stays solid while the rest fades, which reads as a bug rather than as
a setting. Two consequences worth knowing: `CoiBar` owns colours the caller cannot reach by dimming
what it passes in — the frame's background gradient and the fill's two bevel hairlines — so `fill`
gained the faded overload `frame` already had, both fed `HudOpacity.current()`; and the card chrome
moved from `CoiStyle.drawCard` to `PlateCard.drawChrome`, which restates the three draw calls but
still reads its three colours **from `CoiStyle`**, because `ui/` seeing the ambient alpha would mean
a `ui → hud` import. The one known gap is `CoiIcons.drawCrest`'s centre pip, on the fallback emblem
for a pathway `Pathways` has no art for — it lives in `ui/` for the same reason.

**Spirituality is deliberately not on the plate** — it keeps its own sprite bar and its own position;
that bar is the one the user is happy with. Rows with no data are **omitted, not blanked**, so the
card's height follows the server, and the whole plate hides when there is no pathway and no gauge
has data (a vanilla server never shows a card containing just a head).

**Symbols** (`hud/render/PlateSymbols`) are `textures/gui/hud/symbol_brain.png` and `symbol_mask.png`, each
a single **32×32 full-colour** sprite — ordinary artwork, not a mask/ink frame pair. `PlateSymbols.SIZE`
is both the sheet edge and the drawn size, so every blit is 1:1 and the art never resamples; 32 is
chosen because the source brain is natively 32×32 and the source clown 64×64, both whole-pixel
ratios. Three draws per symbol:

1. the whole sprite under `EMPTY_RGB`, a **multiply** that dims the art toward its own shadow rather
   than flattening it to grey;
2. its bottom `fill` rows again under the caller's **wash** — `PlateSymbols.NO_WASH` keeps the art's
   own colours, and sanity only bleeds toward the stage colour from stage 2, so the brain shouting
   red means something;
3. the rows a ceiling puts out of reach, under `CAPPED_RGB`.

The partial draws are **sub-rect blits, not scissors** (`v = SIZE - fillH`): a scissor resolves in
window pixels and would cut the wrong rows once the plate sits inside a `HudScale` push. A missing
texture degrades to a filled rounded rect, so a pack that drops the sprites still gets a gauge.
Swapping in better art is a PNG swap with no code change — which is exactly how the shipped pair got
there, replacing a generated placeholder set.

**`showCharacterPlate` (default true) is an either/or.** While it is on, `MadnessOverlay`,
`ActingOverlay` and `ResourceOverlay` draw no bar, `HudElements.all()` drops those three
outright (a superseded element is not the same as one the player switched off, so it is not left in
the editor as a hidden ghost), and the settings screen hides their rows. The madness **screen
effects** are not a bar and keep running either way — that is the easy mistake here. Turning the
plate off restores all three exactly as before.

## The Character Sheet

`screen/sheet/CharacterSheetScreen` (**M**, on a `character_sheet` server) is the plate's full-size
sibling: a **single scrolling column of sections**, not a grid of look-alike bars.

**It is drawn as a menu document, and that is the point.** The sheet opens every server-authored
menu the player will ever see, so the two have to be the same object: `CoiStyle.cardWidth` for the
card, `CoiStyle.BACKDROP` behind it, a header carrying the pathway emblem and a close cross, the
faint pathway watermark, `MenuTheme.headingCaption` small-caps sections on the card's own surface, a
draggable accent scrollbar *inside* the card, and `MenuTheme.button` in the footer. Every primitive —
chip, gauge, panel, toggle, badge, hairline — comes from `MenuTheme`, so the sheet cannot drift from
the menus again without somebody changing `MenuTheme` itself. The screen owns **no paint of its
own**. Before this it floated separately bordered cards over a differently dim backdrop with
vanilla-ish buttons in the corner, drew HUD `CoiBar` bars inside a GUI, and used a 12px chip where
the menus use 16 — stepping from it into a menu read as stepping into another program.

| Section     | What it is                                                                                                                                                                                      |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| hero        | 32px player head, the pathway emblem + name in `SheetState.pathwayArgb()`, `Sequence N — <title>`, and the sequence again as a 2×-scaled numeral in a badge                               |
| vitals      | four 32px rows — health, spirituality, madness, tiredness — each with its **own symbol**, its own colour, the number first and the bar second, and a plain-language line for the stage          |
| acting      | the mask gauge + `mm:ss` method cooldown, then the **whole** source ledger (label · bar · `n / cap`, `∞` when unlimited, red when capped), with chips for limited / overflow / foreign throttle |
| conditions  | Line of Life and Death, Frenzied Mage's Presence, the anomaly — chips, **only when present**                                                                                                    |
| where to go | the seven sub-menus as cards: glyph, name, one line of what is on the other side                                                                                                                |
| preferences | terrain damage as a switch row, so a preference cannot be mistaken for a doorway                                                                                                                |
| footer      | *Server menu* · *Done*, inside the card under a hairline, in `MenuTheme.button` chrome                                                                                                          |

Three things a change here must keep:

- **The lifecycle is unchanged.** `sheet_open` on init, `sheet_close` exactly once on the way out (both `onClose` and
  `removed`), the `send` guard that never talks into the void, and every value
  re-read from `SheetState` each frame so the 60-tick pushes land live.
- **The draw *is* the layout.** Each section is its own class — `SheetHero`, `SheetVitals`,
  `SheetActing`, `SheetConditions`, `SheetDestinations`, `SheetFooter` over the shared `SheetRows` /
  `SheetChips` — and each is handed a `SheetContext` and returns `int`. Section heights follow the
  data, so a section's `draw` returns the y it reached rather than being measured first — measure and draw are the same pass rather than two
  that can drift. Two consequences: mouse events reuse the last frame's hit boxes (`SheetContext.Hit`), the same
  bargain `AbilityPickerOverlay` makes; and the **card's height trails the content by one frame**, so
  `contentHeight` is seeded full (`Integer.MAX_VALUE / 4`) — the card opens at full size and settles
  down onto a short document instead of opening as a sliver and snapping out.
- **A locked destination explains itself.** The card stays legible, dims, gains a lock glyph and
  answers the hover with `screen.coi.sheet_lock_<target>` — a dead grey rectangle that never says
  why is what the old button row did.

Cards send **`ActionPayload.ofOpen(target)`** (which carries the player's `useServerMenus`
preference) and close the sheet; the server answers with its own GUI or a `coi-client:menu` document.

Symbols split by who owns the art: madness and acting reuse `PlateSymbols`' 32×32 brain and mask, and
everything else is drawn from `screen/sheet/SheetGlyphs`' 8×8 grid — blown up by whole pixels, so a mark is
exactly as sharp at 16px as at 32 and needs no PNG. Section headings and condition chips carry the
bundled 16px `CoiIcons` glyphs instead, by the same names a server-authored document uses. The one
deliberate disagreement with the plate is
that **the sheet's brain fills with madness, not sanity**: the row is named Madness and the bar beside
it splits into permanent / godhood / temporary, so a symbol filling the other way would contradict it.

## First-party GUI icons

`ui/CoiIcons` is the one way to draw the mod's small icons, in
`textures/gui/icons/`. **Each icon ships at exactly the size it is drawn at**, so every blit is 1:1
and the artwork never resamples — that is why the sizes are constants in `CoiIcons` rather than a
caller's argument, and why the cog exists twice. Anything that wants a new size gets a new file, not
a scaled blit.

| Icon            | Size | Used by                                     |
|-----------------|------|---------------------------------------------|
| `cog`           | 16   | the badge beside the HUD Settings title     |
| 25 named glyphs | 16   | the menu system's `glyph` icon kind (below) |

**The 25 glyphs** (`alliance`, `authority`, `cooldown`, `cost`, `damage`, `defense`, `divination`,
`flame`, `growth`, `health`, `magic`, `power`, `regen`, `resist`, `restore`, `rites`, `saturation`,
`sequence`, `slot_empty`, `slot_filled`, `soul`, `spirit`, `spirituality`, `uniqueness`, `ward`) are
what a server-authored menu names through `MenuIcon.Kind.GLYPH`. They ship **authored at 16×16**, so
every blit is 1:1 with no reduction at all — unlike the cog, which is a 64→16 whole-pixel reduction.
`CoiIcons.GLYPHS` is the list; `CoiIcons.glyph(name)` resolves one.

`glyph` is the only icon kind whose art lives in this jar, which makes it the only one a server can
name without knowing the player's resource pack — so it is the right kind for anything *conceptual*
(a cost, a cooldown, a sequence) and `item` stays for things that really are an item. **`CoiIcons.glyph` sanitises the
name to `[a-z0-9_]`**, so a document cannot walk out of the icon
folder or reach another namespace; a name that survives sanitising but matches no file just fails
`draw` like any other absent icon.

**16px is the floor for this artwork, and that is a measured fact, not a preference.** The sources
are detailed 64×64 images, not pixel art upscaled from a small grid — only ~34% of the cog's 4×4
blocks are a flat colour. Any reduction below 16px averages the detail away and reads as a blur, so
the slots narrower than that (tab marks, button gutters, the picker's meta-line badges, the plate's
reserve rows) keep their **drawn glyphs and primitives**, which stay sharp at any size. An 8px set
was tried and reverted. Before adding an icon to a new slot, check the slot is at least 16px.

`draw` returns **false** when no loaded pack defines the icon, and every caller falls back to what it
drew before. Presence is cached per identifier and cleared on resource reload by `ClientDataLoader`,
alongside `IconModels` and `PlateSymbols`.

`CoiIcons.drawPathwayEmblem` is the one way to draw a pathway's symbol and returns the width it
drew. The mod ships **real art for all 25 pathways** — 9px bitmaps behind the
`coi-client:pathway_icons` font (`textures/pathways/*.png`), keyed by PUA codepoints in
`Pathways`' own emblem map — so the character sheet and the character plate both use
it and can never show two different symbols for the same pathway. The diamond crest is only a
fallback for a pathway the map does not know; the plate used to draw that crest unconditionally,
which was simply wrong about what art existed.

Source artwork lives in **`art-sources/gui-icons/`** at the repo root — deliberately *outside*
`src/client/resources/`, so the full-size originals are kept for re-cropping without shipping in the
jar. The shipped icon is a whole-pixel reduction (64→16 is 4:1); the plate's symbols are the same (32→32 and 64→32).

## The Title Screen Takeover

`screen/title/` replaces the vanilla main menu with **the Pathway Wheel**: a dark void, a slowly
turning ring of the mod's 24 pathway emblems, a gold bloom, the Lord of the Mysteries wordmark at
the centre, and the vanilla buttons re-chromed in `MenuTheme`'s dark/gold language. It is the first
thing a player sees, and before this it was somebody else's game with a mod installed.

The scene is state-aware without asking the server anything: `ClientStateStore` persists
`lastPathway` beside the madness values at disconnect, so **the player's own emblem is lit** on the
wheel and named under the wordmark, and **their persisted madness corrupts the scene**.

`TitleTakeover` is the shell and owns exactly four things — the gate, the wall-clock time base, the
`Geometry` record (`cx`, `cy = 0.46h`, `radius`) and **`accent()`**. That last one is the reason the
class exists: corruption drags a single colour from `CoiStyle.ACCENT` toward a desaturated crimson
and every collaborator reads it, so the void, the ring, the wordmark and the button rails sicken
together instead of four classes each holding an opinion about how red things have got.

| Class | Draws |
|-------|-------|
| `TitleScene` | two gradient halves split at `cy`, a radial bloom, ~140 motes seeded from a fixed `long` — the same trick `CracksEffect` uses, to the opposite end: a crack pattern is seeded by its trigger time so every shatter differs, the menu's dust by a constant so the menu looks like itself on every launch |
| `PathwayWheel` | the stroked outer ring, the counter-rotating dashed inner one, the emblems, the travelling crest and the cursor's repulsion |
| `TitleLogo` | `lom_logo.png` (512×339, ratio preserved at every size), the pathway caption, the splash |
| `TitleButtons` | the button plate: near-black fill, accent border, and the **2px accent rail** the character sheet's destination cards carry — the rhyme that ties the menu to the rest of the mod |

Four things a change here must keep:

- **The ring turns; the emblems do not.** Rotating each emblem with its own angle is the obvious
  reading of "a turning wheel" and the wrong one — half the wheel ends up upside down, and these are
  symbols the player is meant to recognise. One revolution per 240 s, off `System.currentTimeMillis()`.
- **The emblems are blitted, not glyphed.** `Pathways.emblemTexture` gives the 64×64 art, drawn at
  the whole-pixel 32 (40 for the lit one). `CoiIcons.drawPathwayEmblem` renders a *9px bitmap font*
  glyph and is mush at this size — it stays right for the sheet and the plate and wrong for here.
  `emblemTexture` is also the one place the `aeon` / `eternalaeon.png` spelling fold is undone.
- **The logo is laid out against vanilla's first button row**, `height / 4 + 48` in
  `TitleScreen.init`, never against a fraction of the screen height: the mod's wordmark is far
  larger than vanilla's, so a percentage that clears the buttons at 16:9 puts them through the
  middle of it in a short window. The band runs from just under the wheel's apex (so the emblem
  riding that point does not land on the lettering) down to that row, and the logo is fitted by
  *both* its width cap and the band's height. On a window too short for both, clearing the buttons
  wins and the apex emblem is allowed to graze.
- **Off is a real path.** `coiTitleScreen` false makes every hook fall through to vanilla —
  panorama, logo, splash placement, button sprites — and `TitleScreenHaunt`, which predates the
  takeover, keeps drawing over the vanilla menu exactly as it did. All three shared hooks
  (`LogoRenderer`, `SplashRenderer`, `AbstractButton`) also check the screen, since the rest of the
  game uses them too.

**The wheel is alive in two ways**, and they share one number. A **travelling crest** circulates the
ring once per 18 s, counter to the wheel and thirteen times its speed — the same sense the dashed
inner ring turns, so the ring reads as the mechanism and the light as driven by it. It is a *pure
function of the clock and the emblem's ring index*, falling off over `CREST_SPREAD` (1.2) emblem
spacings, so one emblem is at full strength and its neighbours are warmed: no state, nothing to pop
when it changes target, and nothing to decide if `Pathways.RING` ever changes size. It raises an
emblem from its resting `REST_ALPHA` (77) toward full and adds two faint rings at the peak, and it
**skips the lit pathway entirely** — that emblem is already at 255 with a halo of its own, so a
travelling light could only make a permanent mark blink. Second, the cursor **pushes emblems away**:
64 px of influence, 16 px of displacement, smoothstepped to zero at the radius (a linear ramp has a
visible crease where it ends), eased toward its target over 120 ms through `TitleTakeover.approach`
— the same contract as `SheetContext.approach`, so `epilepsyMode` returns the target outright at one
chokepoint. Two invariants:

- **The push is measured from the emblem's undisturbed position**, never from its displaced one. A
  displacement fed back into its own input oscillates — the pushed emblem is further away, so it is
  pushed less, so it returns — and an emblem the cursor follows runs away for as long as it is
  followed. Only the easing carries state; the target is a pure function of the cursor.
- **One proximity value drives both the push and the brightening** (and the 32 → 36 px growth).
  Deciding brightness by hit-testing where the emblem *is* would dim it as it slides out from under
  the cursor it is fleeing, which reads as a bug. That shared value is why the two effects are one
  feature.

`TitleTakeover` owns the frame delta as well as the clock, advanced **only** in `drawScene` —
`drawSplash` also runs once a frame, and counting the gap twice would halve every tween — and
clamped to 100 ms, since the menu can sit unrendered for minutes and an unclamped delta finishes
every tween in one step. `PathwayWheel`'s two `float[]` offset arrays are static, in the same shape
as `TitleScene`'s mote field, and are **render thread only**.

**Corruption** is `TitleScreenHaunt.intensity()` — so the hallucinations toggle gates the takeover's
horror for free and a clean player never sees any of it. As it rises: the accent turns, the ring
picks up brief judders, one to three emblems drift toward `error`'s grey on their own slow cycles,
and above 0.6 hairline cracks strike **across** the ring as chords. The haunt's own vignette and eye
apparitions still draw last, above everything. `epilepsyMode` freezes the mote twinkle, the splash
pulse, the judder and the emblem flicker **at their steady state** rather than removing them.

The splash is drawn from `SplashRendererMixin` rather than alongside the logo, so it keeps vanilla's
place in the layer order and only its position moves. The line itself comes from
`TitleScreenHaunt.splashText()`, which decides it once per launch — re-rolling it here would give a
different line every frame, and a haunted line the haunt never chose.

## Beyonder Health Bar

`hud/overlay/BeyonderHealthOverlay` is the **only element in the mod that replaces a vanilla HUD element**
rather than attaching beside one: it takes `VanillaHudElements.HEALTH_BAR` through
`HudElementRegistry.replaceElement`, keeps the `original`, and calls it whenever our bar is not
drawing — so a vanilla server, a non-Beyonder, creative/spectator, `hud.isHidden()`,
`HudLayout.editing()` or `showBeyonderHealth = false` all get the real hearts back, untouched.

The reason it exists: a Beyonder's HP pool runs 50 (S9) → 1750 (S0) and past 2500 under True Form,
but vanilla `max_health` stays **20** and ten hearts cannot say that. The bar draws `1,234 / 1,750`
inside its own fill, in the hearts' exact footprint.

**The client derives current HP; the server only sends the ceiling.** Vanilla health is a
proportional mirror of the pool, and the plugin recomputes the pool *from* vanilla health after
every hit, so:

```
poolCurrent = maxHealth * (player.getHealth() / player.getMaxHealth())
```

This is not just cheaper than a pushed value — it is **more accurate**, because the plugin's own
`beyonder.getHealth()` is only re-derived every 200 ticks and is stale after non-ability damage. Use
`player.getMaxHealth()`, never a hardcoded 20: some abilities apply a negative max-health modifier,
and the mirror is a percentage either way. `maxHealth` still has to come over the wire (on
`conditions`, see the protocol doc) because it is volatile — True Form doubles it, Strata and Death
marks cut it.

**Four styles, and `HEARTS` is the default.** Replacing the hearts outright took vanilla's
*absorption* hearts with it, and players said so; `hud/render/HealthStyle` is the answer. The
overlay stays the shell — the render gate, the vanilla replacement, the derivation, the flash/pulse
state and the anchor — and `hud/render/HealthBarPaint` owns the pixels of all four:

| Style | Draws |
|-------|-------|
| `HEARTS` *(default)* | nothing but the readout. Vanilla keeps its heart row, absorption hearts included, and the mod adds the `1,234 / 1,750` those ten hearts cannot carry, plus a gold `+175` for a shield |
| `BAR` | the 1.2.0 bar, unchanged |
| `ORNATE` | the same geometry under carved chrome: near-black border, a bevel cut into the box, gold corner brackets |
| `PIPS` | ten notched segments draining the way a heart row does, absorption as gold pips growing inward from the right |

Three things a change here must keep:

- **All four occupy the same `BAR_WIDTH × BAR_HEIGHT` box**, so switching style never moves the
  element and `BeyonderHealthElement`'s `bounds`/`moveTo` stay exact inverses without ever asking
  which style is on. For `HEARTS` the box is where the *numbers* go — vanilla owns the hearts'
  position and that style deliberately does not move them.
- **The replacement hook has three answers, not two.** It used to be
  `if (!render(ctx)) original.extractRenderState(…)`. `HEARTS` needs *both*, and the hearts have to
  go down **first** or they land on the readout, so the displaced element is handed in as a
  `Runnable` the gate calls at the moment it decides. A boolean could express neither the third case
  nor the ordering.
- **Absorption is never invisible.** The gold continues past the main fill while the box has room
  and is drawn *over* the fill's right-hand end when it has not. The original clamped it to
  `BAR_WIDTH - fillW`, which is zero at full pool — so the one state a shield most needs to announce
  itself in was the one state that showed nothing.

`HealthStyle.defaultYOffset()` replaces the old single `DEFAULT_Y_OFFSET`: 39 for the three
replacing styles (the hearts' own row, free precisely because they took the hearts off it) and
**59** for `HEARTS`, which has to clear both the hearts *and* vanilla's armour bar at `h - 49`. The
obvious "one line up" at 49 drops the readout onto the armour of every armoured player. Switching
style **carries the placement across only if the player never moved it**
(`BeyonderHealthOverlay.applyStyle` / `atDefaults`): a bar somebody dragged somewhere must not be
snatched back, and one nobody touched must not end up printed over the hearts it just restored.

`BAR_WIDTH` is **96** — the widest the box can be on the hearts' row. It starts at the hotbar's
left edge (`screenW / 2 - 91`) and ends at `screenW / 2 + 5`; the hearts only reached
`screenW / 2 - 10`, so replacing them freed the 20px gap in the middle, and the food bar at
`screenW / 2 + 10` is the hard stop. Staying on this row is what lets the armour bar keep sitting
directly above it, as it did above the hearts.

`DEFAULT_X_OFFSET` is **derived**, `BAR_WIDTH / 2 - 91`, never typed. `HudAnchor.resolve` centres on
the element's width, so a stored X offset is only meaningful against the width it was calibrated
for — hard-coding it once already went wrong: widening the bar from 82 to 100 left the old `-50`
behind and pushed every existing config 9px to the left. `layoutVersion` **4** resets the bar's
placement for that reason, and **5** does it again for the styles. The identity only holds for an
**even** `BAR_WIDTH`, which is also what keeps the centring exact at odd window widths, so the width
must stay even — 95 shipped in 1.2.0 and broke exactly that, which is the other reason it is now 96.

`BAR_HEIGHT` is **9**, and the constraint is easy to miss: `CoiBar.frame` draws its border *outside*
the box, so the drawn footprint is `BAR_HEIGHT + 2`. The heart row only has `h-40 … h-30` before the
experience bar at `h-29`, and the XP bar draws *after* us — at 11 the bar was 13 rows tall and had
its bottom silently clipped.

The fill colour is **driven by how much pool is left**, not fixed: a deep crimson down to 60%, then
blending to the alarm scarlet, reaching it exactly at `LOW_FRACTION` (30%) where the pulse also
begins — so the colour shift and the pulse are one cue, not two thresholds. A bar that was bright
scarlet at full health spent its loudest colour on the state that needs no attention and had nothing
left to escalate to.

Absorption is drawn as a gold segment past the main fill, converted into pool units — vanilla's
absorption hearts disappear along with the element we replaced, so not drawing it would lose real
information. Damage flashes the bar and low HP pulses it, both suppressed by `epilepsyMode`.

**Known gap:** vanilla's `HEALTH_BAR` height provider still reports heart rows, so with absorption
active the armour/air bars are pushed up and leave a gap above our one-row bar. Fixing it means
replacing the left-side height provider too, which would have to duplicate the whole render gate.

## HUD Settings & per-element scale

`screen/settings/HudSettingsScreen` has three tabs, split by what a setting *is* rather than by which overlay
draws it:

| Tab             | Holds                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Ability HUD** | `showAbilityHud` first — off, and the rest of the tab is **not built**, since there is nothing left for a size, a count or a decoration to apply to. Then `slotSize` (the only size knob), key + wheel slot counts, the three slot-decoration toggles, one *Align* for the whole slot group                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **Elements**    | one uniform block per bar/overlay: a show/hide checkbox **labelled with the element's own name** (`screen.coi.layout_el_<id>`) and an *Align* button, then — **only while the element is switched on** — its *Scale* slider and its own extras (the health element's four-way `Style` cycle, spirituality hide-when-full, resource max bars, action-bar lines). `SettingsRows.elementRow` returns the checked state and re-runs the screen's `init()` on toggle, so switching an element off collapses its block instead of leaving dead controls behind. The Character Plate leads the tab; while it is on, the madness / acting / resources blocks are absent entirely and only `resourceMaxBars` survives, since the plate still draws those rows |
| **General**     | the master `enabled` switch, `useServerMenus` (open the plugin's chest GUIs instead of the mod's menus), `coiTitleScreen` (the Pathway Wheel main menu), accessibility (epilepsy mode, hallucinations, effect volume), Discord presence, "Show Tour Again"                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

The four presets are gone — the layout editor plus per-element scale cover what they used to
approximate, so the bottom row is just *Reset · Cancel · Done*. Removing them orphaned
`screen.coi.preset*`, `settings_tab_bars`/`_display`, the four `*_section` headers and the eight
`show_*` keys; the element checkboxes now reuse the `layout_el_*` labels the editor already owns.

**Per-element scale** — eight `float` fields (`madnessScale`, `spiritualityScale`, `actingScale`,
`resourceScale`, `actionBarScale`, `targetHealthScale`, `cogitationScale`, `notificationScale`),
plus `characterPlateScale`, default `1.0`, clamped to `HudConfig.MIN_ELEMENT_SCALE`…`MAX_ELEMENT_SCALE` (0.5–2.0). The
ability
slots have no scale field of their own — theirs is derived from `slotSize` (see above) and is the
only one that also scales a *step*, so scaled-up slots never overlap.

**Per-element opacity** is so far the character plate's alone (`characterPlateOpacity`), through the
`hud/HudOpacity` seam. It is deliberately shaped as `HudScale`'s twin — ambient render state pushed
around one element's draw, popped after, render-thread only, so the draw code inside needs no new
argument. The asymmetry is forced rather than chosen: `ctx.pose()` is a 2D matrix stack with no
colour channel, so there is no pose-level alpha to push and opacity has to be applied per colour.
Nested pushes multiply, so a nested piece can never come out more solid than what encloses it.

The one rule: **every element scales about its own fill origin** — the point its `anchor()` returns.
`hud/HudScale` implements it as translate (origin) → scale → translate (−origin) on the pose, so the
draw code inside keeps using absolute screen coordinates unchanged, and a scale of 1 skips the matrix
work entirely. Two consequences anything new must honour:

- anchor resolution passes the **scaled** width (`HudScale.size(BAR_WIDTH, scale)`) to
  `HudAnchor.resolve`, or `*_RIGHT`/`*_CENTER` anchors and the on-screen clamp drift;
- `HudElements`' `bounds` and `moveTo` stay exact inverses only if both scale the label/frame padding
  (`HudScale.size(BAR_LABEL_PAD, scale)`, …) — an unscaled pad there makes elements jump when dragged.

Full-screen effects drawn by an overlay (the madness stage vignette) stay **outside** the push.

## Server-driven Overlays (batch 3)

Four overlays render structured UI events the plugin used to push through vanilla surfaces. All
four attach before `VanillaHudElements.CHAT` and are gated on `settings.enabled` plus their own
`show*` toggle (DISPLAY tab → *Overlays*):

| Overlay               | Position                                                                                                                                       | Source                  |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| `ActionBarOverlay` | centred, stacked upward from `h - actionBarYOffset` (default 72), 10 px apart, 2 px channel-coloured tick left of each line                    | `coi-client:actionbar`  |
| `TargetHealthOverlay` | 100×5 bar at `(w/2 - 50, h/2 + 18)`, name above, `hp / max (pct%)` below                                                                       | `coi-client:target`     |
| `CogitationOverlay`   | 220×54 `CoiStyle.drawCard` at `(w/2 - 110, h/2 - 70)`; label at 1.5× scale, streak, draining 200×3 timer                                       | `coi-client:cogitation` |
| `NotificationOverlay` | top-right toasts, `x = w - 12 - 180`, 180 px cards with a 2 px accent bar; slide in 250 ms, hold, fade 300 ms (`epilepsyMode` drops the slide) | `coi-client:notify`     |

Action-bar text arrives as a **vanilla text-component object** and is deserialized through the
registry-free `ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)`; anything the codec
rejects degrades to its plain string form rather than dropping the line. Entries expire locally at
`receivedAt + ttlMs`, and `actionBarLines` (0 = follow the server's `maxVisible`) caps how many
draw. Dev testing: F8 → the *ActionBar / Target / Cogitate / Toast* row.

## Server-authored Menus (batch 6)

Full reference, both repos in one place: **[docs/MENU_SYSTEM.md](docs/MENU_SYSTEM.md)** — the wire
contract, the server's builder DSL, every ported screen, everything that still opens a chest GUI,
how to add a menu, and the constraints the visual redesign kept.

**Vocabulary v2** added five component types — `hero`, `details`, `steps`, `chips`, `panels` — plus
an `icon` field on `kv` rows / `checklist` items / `stat` / `note` / `toggle` / section headings, a
tri-state checklist (`ok` / `no` / **`pending`**), `stat` `style`/`cap`/`delta`, section
`badge`/`collapsed`, list-row `fraction`/`meta`, `grid` `size`, and a labelled `divider`. Purely
additive: **the protocol stays 2 and the feature id stays `menu_ui`** — the capability list
negotiates this, not a version bump.

The reason it exists: the renderer could attach an icon to exactly three things (document header,
button, list/grid row), so an adapter that wanted to *explain* something had one tool — a muted grey
paragraph. Five of the seven menu families therefore carried **zero** component-level icons and
~28,000 characters of prose, 52 blocks of it over 140 characters. After the rewrite there are **zero standalone `text`
components in any adapter**; every paragraph lives inside a `details`
disclosure, collapsed by default. The information was never the problem — showing it unasked was.

Two client-side rules the renderer must keep: **disclosure state (`details` open/closed, collapsed
sections) is keyed `screenId + "/" + id` and survives a rebuild**, resetting only when the screen id
changes — a 60-tick server refresh slamming shut what the player just opened is the bug to avoid;
and **all easing funnels through `MenuContext.approach`**, which returns the target outright under
`epilepsyMode`, so one chokepoint honours the setting.

**The back arrow returns to the character sheet.** Every root document sets `back(true)` but is
opened with a reset stack, so `__back` at the root used to reach `session.pop() == null` and close
the menu outright. The server now answers that case with `{"closed":true,"back":true}`; the client
reopens `CharacterSheetScreen` when the flag is set **and** the session came from the sheet **and**
the server advertises `character_sheet`. Esc and the X still close outright — only the back arrow
goes back. The flag is read off the raw JSON in `handleMenu`, deliberately not through `MenuParser`,
because it describes the *transition* and a closed document has no screen to describe.

The plugin's ~217 InvUI chest GUIs cannot each become a Java class here — `ChurchGUI` alone is
2833 lines and ~30 screens, and every server-side menu change would need a client release. So the
server ships a **document** describing one screen and the client renders it: `client/menu/` parses
it, `screen/menu/MenuScreen` draws it, and every click goes back on `coi-client:menu_action` for the
server to answer with the next document. The plugin keeps owning navigation, gating and side
effects — exactly where that logic already lives. A document is rendered, never interpreted: the
`screen` id is only used to tell "the same screen refreshed" (scroll and typed text survive) from a
new one.

- **Forward compatibility is the parser's whole job.** `MenuParser` never throws: an unknown
  component `type` is skipped, a wrong JSON type reads as absent, sizes are clamped. A newer plugin
  must degrade to a screen missing one row, never to no screen — which is why `MenuComponent` is
  sealed on *this* side but the wire is not.
- **`enabled` defaults to true, and a disabled control is drawn with its `disabledReason` as a
  tooltip.** A dead button that will not say why is the thing the chest GUIs did worst.
- **`confirm` is client-side.** A button carrying one raises a modal and sends nothing until the
  player agrees, replacing the plugin's two-step chest confirms.
- **One card, one scrollbar.** A `list` flattens its rows (and its search box) into the outer scroll
  rather than nesting one, so `maxVisible` is advisory. Rows are laid out in pixels because no two
  component types are the same height — the same reason `AbilityPickerOverlay` works that way.
- `__close` is sent **exactly once**, however the screen goes away, and never in answer to the
  server's own `{"closed":true}`. Every send is guarded by `canSend`, like the character sheet's.
- Text fields outlive a rebuild (`fields` is keyed by component id): the search box re-lays out the
  whole card on every keystroke, and recreating the `EditBox` would drop the caret mid-word.
- **`useServerMenus`** remains a compatibility preference for older servers. When the server
  advertises `menu_archive`, the checkbox is hidden and registered native replacements take
  precedence. The sheet's Pathways button opens the native multi-pathway chooser; unsupported
  flows still retain their server fallback.
- **Archive templates** (`ledger`, `relic`, `inscription`, `atlas`, `challenge`) keep all document
  sections and actions, adding a section index and adaptive folio layout. `ability_manual` moves
  represented ability rows into its details; Other actions preserves every remaining control.
  See `docs/ARCHIVE_PRESENTATION.md` and `docs/ABILITY_MANUAL.md` for the current presentation contract.
- **Portrait scenes** use the authoritative sheet pathway and sequence. `PortraitPose` is attached
  to an isolated avatar render state and reset during ordinary extraction; never mutate the live
  player to pose a menu model. Reduced-effects mode freezes scene motion.
- Dev testing: F8 → *Menu* feeds a sample document covering every component type through the real
  parser, so the renderer can be judged with no server attached.

## The Ability Picker

`screen/ability/AbilityPickerOverlay` is the modal shell — the search box, the keyboard and the
scroll; `PickerModel` is the list itself, `PickerPainter` draws one row at a time, `PickerMetrics`
holds the heights and `PickerLabels` every word. The model is a flat `List<Row>` of `UNBIND` /
`HEADER` / `ABILITY` / `MESSAGE` records, so scrolling, hit-testing and keyboard arithmetic all walk
one structure. `Row.clickable()` is what makes headers and the no-results line inert. **Rows are not
uniform height** (`PickerMetrics.ROW_H 26`, `HEADER_H 13`, `UNBIND_H 18`, `MESSAGE_H 18`), so every
scroll calculation is in *pixels*: `PickerModel.contentHeight()`, `maxScroll()` (walks backwards for
the first index whose tail still fits `listH`) and the scrollbar handle. `filtered` stays a plain
ability list, so Enter-picks-first-match is unchanged.

- **Grouping** — sorted pathway → sequence → name, then one header per **pathway *and* sequence**
  (`FOOL · SEQ 5`, then `FOOL · SEQ 4`, …): the sequence is the bracket players actually think in.
  Since the list is already sorted, a group break is just "this row's key differs from the last
  one's". Pathway/sequence prefer `AbilityInfo.pathway()`/`sequence()` and fall back to the id
  segments; a sequence of `-1` prints the pathway alone.
- **Two-line rows** — line 1 is icon + clipped name + right-aligned kind tag (`ACTIVE` / `TOGGLE` /
  `PASSIVE`, or red `LOCKED` / `BLOCKED`); line 2 is cost, cooldown and category. **The cost and
  cooldown badges always draw**, as `◈ <n>` / `◈ <n>/s` and `⏱ <n>s`, with an em-dash for genuinely
  free/instant abilities — a blank where a number belongs is what made the old list read as
  interchangeable. The two glyphs live in the lang strings, so a font that lacks them is a
  translation fix, not a code change.
- **`metaAvailable`**, computed once in `open()` via `PickerModel.detectMeta()`, is `ServerCapabilities.has("ability_meta")` *or*
  any listed ability carrying a richer field. False (a protocol-1 server) degrades line 2 to the
  category alone rather than a column of em-dashes. The OR keeps badges alive in the dev
  environment, where nothing answers the hello.
- Categories translate through `screen.coi.ability_cat_*` (14 keys), resolved from the raw
  `info.category()` so a server-invented category falls back to the raw word instead of
  `uncategorized`. Pathway captions stay English and upper-cased — no pathway-name lang keys exist.
- Search matches display name, pathway key **and** category.
- The tooltip keeps only what the row can't show: description, sequence, block reason.

## Ability State on the HUD

`AbilityOverlay.setActive/setCategoryLabel/setCooldown` fan out to **every** slot whose
`AbilityInfo.extractId(stored)` equals the id — exact equality, not `contains`, and no early
`break`. An active (toggled) ability gets a pulsing cyan outline and an `ON` tag; a locked or
blocked one gets a red icon tint and a struck-through red name. State arrives either inline on the
v2 ability list (`active`, `cooldownRemainingTicks`) or live on `coi-client:state`.

## Key Patterns

- **Static singletons, one per concern** — `CoiClientMod` is the initializer and nothing else. The
  ability catalogue is `ability/AbilityRegistry`, the slot bindings `ability/AbilityBindings`, the
  pathway identity `ability/Pathways`, the keymappings `input/CoiKeyBindings`, the wire
  `network/CoiNetworking`, and each S→C channel's data one class under `state/`. Screens and widgets
  still reach all of them through static methods — what changed is which class answers, not how.
- **Real-time cooldowns** — tracked via `System.currentTimeMillis()`, not ticks, for smooth animation.
- **Lazy effect geometry** — `CracksEffect` generates crack segments on first render (needs screen dimensions); seeded by `startTime` for consistent patterns.
- **Dev-only keybindings** — `effectDebugMenu` (F8) is only registered when `FabricLoader.isDevelopmentEnvironment()`.

## Keybindings

| Key             | Action                                                                                                                   |
|-----------------|--------------------------------------------------------------------------------------------------------------------------|
| Z–N (6 keys)    | Ability slots 1–6 (each slot is placed on its own in the layout editor)                                                  |
| *(unbound)*     | Ability slots 7–10 — assign in vanilla Controls, activate via `activeAbilitySlots`                                       |
| G (hold)        | Ability wheel — radial picker, open while the key is held, `wheelSlots` slots                                             |
| K               | Open Ability Binding screen                                                                                              |
| M               | With `useServerMenus`, the plugin's chest GUI straight away (`menu_action`); else the character sheet (`character_sheet`), else the server Beyonder menu, else an unsupported message. The preference only *reorders* the first two — a server without `menu_action` still gets the sheet |
| *(unbound × 7)* | Open one of the sheet's destinations directly (`church`, `abilities`, `mythical`, `uniqueness`, `honorific`, `map`, `seat`) — `ActionPayload.ofOpen`, no sheet behind it, so the menu's back arrow means "close" |
| Left Alt (hold) | Gesture casting — draw a shape, release to cast (only when a gesture is bound)                                           |
| F8 *(dev only)* | Open Effect Debug screen                                                                                                 |

## Ability Pathway Colors

Extracted from first segment of ability ID (before first `-`):
`fool`=purple, `door`=blue, `sun`=yellow, `tyrant`=cyan, `demoness`=red, `priest`=orange

The table itself is `ability/Pathways.pathwayRgb` — **one** map for all 25 pathways, shared by the
HUD slots, the picker, the acting bar, the plate, the sheet and `MythicalFormManager`.
`AbilityInfo.pathwayColor`/`pathwayColorByName` are thin ARGB wrappers over it, kept because callers
that only hold an ability id read better that way.

## Config Files

`config/coi_abilities.json` — bound ability ids per key slot (`abilityN`), wheel slot (`wheelN`), and gesture (`gesture_<id>`)
`config/coi_hud.json` — HUD settings (position/size/scale, display toggles, epilepsy mode, madness bar, spirituality bar
(`showSpiritualityBar`, `spiritualityAnchor`, `spiritualityXOffset`, `spiritualityYOffset`, `spiritualityHideWhenFull`),
acting bar (`showActingBar`, `actingAnchor`, `actingXOffset`, `actingYOffset`), resource bars (`showResourceBars`,
`resourceAnchor`, `resourceXOffset`, `resourceYOffset`, `resourceMaxBars`), overlays (`showActionBar`,
`actionBarXOffset`, `actionBarYOffset`, `actionBarLines`, `showTargetHealth`, `targetHealthXOffset`,
`targetHealthYOffset`, `showCogitationOverlay`, `cogitationXOffset`, `cogitationYOffset`, `showNotifications`,
`notificationXOffset`, `notificationYOffset`), `slotPlacements` (a 10-entry array, each entry `null` for "stay in the
row" or
`{"anchor":"TOP_LEFT","x":0,"y":0}`), the character plate (`showCharacterPlate`, `characterPlateAnchor`,
`characterPlateXOffset`, `characterPlateYOffset`, `characterPlateScale`, `characterPlateOpacity`), the health element (`showBeyonderHealth`,
`beyonderHealthStyle` — `HEARTS` | `BAR` | `ORNATE` | `PIPS`, default `HEARTS` —
`beyonderHealthAnchor`, `beyonderHealthXOffset`, `beyonderHealthYOffset`, `beyonderHealthScale`),
`showAbilityHud`, `slotSize` (the
ability slots' only size knob, 20–80) plus the nine per-element scales (`madnessScale`, `spiritualityScale`,
`actingScale`, `resourceScale`, `actionBarScale`, `targetHealthScale`, `cogitationScale`, `notificationScale` — all
`1.0`, clamped 0.5–2.0), `layoutVersion`, `effectSoundVolume`, `enableHallucinations`, `activeAbilitySlots`,
`wheelSlots`, `enableDiscordPresence`, `presenceShowMadness`, `useServerMenus`, `coiTitleScreen`)
`config/coi_client_state.json` — persistent state, not preferences (`ClientStateStore`): last madness values for title
haunting, `lastPathway` for the emblem the title wheel lights, `tourCompleted` for the first-join tour,
`inventoryHintDismissed` for the shortcut-item hint (all survive HUD config resets)

## Localization

`src/client/resources/assets/coi-client/lang/en_us.json` + `uk_ua.json`
Key format: `key.coi.*`, `screen.coi.*`, `notification.coi.*`

## Known gaps

Things that are wrong on purpose, or wrong and known. Written down so the next reader does not have
to rediscover them.

- **`HudConfig.load()` still NPEs on a truncated or empty `coi_hud.json`.** `GSON.fromJson` returns
  `null` for an empty or whitespace-only document, `HudConfigReader.read` calls `json.has(...)`
  straight away, and the `try` only catches `IOException` — so a config file cut short by a crash
  during a save takes the mod's init down instead of falling back to defaults. Pre-existing;
  deliberately **not** fixed during a no-behaviour-change refactor, because the fix is a behaviour
  change (silently resetting a corrupt config) that wants its own decision.
- **`form/model/VisionaryLowerModel` and `VisionaryLowerAnimations` are generated.** They are
  Blockbench exports and must be regenerated from the modelling tool, never hand-edited — see
  *Mythical Creature Forms*.
- **The vanilla `HEALTH_BAR` height provider still reports heart rows**, so absorption pushes the
  armour/air bars up and leaves a gap above our one-row bar — see *Beyonder Health Bar*.
