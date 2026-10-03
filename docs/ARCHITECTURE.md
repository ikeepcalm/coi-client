# Architecture

The annotated source tree, the shared seams that stop one decision being made twice, and the
conventions the whole client follows. `CLAUDE.md` carries the short version.

## The tree

```
dev.ua.ikeepcalm.coi
  ├── CoiClient                 — the client entrypoint, and **only** that: load what is on disk,
  │                               register what draws and what listens, wire the two connection
  │                               events. It holds no ability state any more (see domain/ability/)
  ├── DataGenerator             — datagen entrypoint; creates the pack and generates nothing,
  │                               since every asset this client ships is hand-authored
  ├── config/
  │   ├── AbilityConfig     — persists slot bindings → config/coi_abilities.json
  │   ├── HudConfig         — every HUD setting → config/coi_hud.json
  │   ├── HudConfigIo       — the on-disk key names three ways: `read` ("the value, else
  │   │                       the default"), `write` (the same keys back), and `copy` (the
  │   │                       field-by-field working copy the settings screens edit)
  │   ├── HudConfigMigrations — brings an older file up to `HudConfig.LAYOUT_VERSION`
  │   ├── ClientStateStore  — persistent state, not preferences → coi_client_state.json
  │   └── ClientDataLoader  — the mod's own resource-pack data (the pathway archive), and
  │                           the reload hook clearing IconModels / PlateSymbols / CoiIcons;
  │                           holds `IngredientInfo`, one archive entry
  ├── network/
  │   ├── CoiNetworking     — the wire: payload registration, every S→C receiver, `sendHello`
  │   ├── ClientFeatures    — feature ids + protocol version this client speaks
  │   ├── ServerCapabilities — what the connected server said it can feed; reset on disconnect
  │   └── payload/          — one record per channel, all built from `CoiPayloads`
  │       ├── CoiPayloads   — the shared type/codec shapes: the `coi-client` namespace, the
  │       │                   1 MiB / 32 KiB caps, the read/write pair — spelled out once
  │       ├── ServerboundPayloads — the seven C→S records, nested inside it:
  │       ├── AbilityUsePayload      C→S  coi-client:use
  │       ├── AbilityCategoryUsePayload C→S coi-client:use_category
  │       ├── AbilityRequestPayload  C→S  coi-client:request
  │       ├── HelloPayload           C→S  coi-client:hello   (capability handshake)
  │       ├── ActionPayload          C→S  coi-client:action  (open_menu, sheet lifecycle)
  │       ├── MenuActionPayload      C→S  coi-client:menu_action (a click in a menu document)
  │       ├── GlyphSubmitPayload     C→S  coi-client:glyph_submit (a drawn spell, 32 KiB cap)
  │       ├── ClientboundPayloads — the S→C records, nested inside it:
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
  │       ├── GlyphPayload           S→C  coi-client:glyph   (canvas open / submit result)
  │       └── VisualEffectPayload    S→C  coi-client:effect
  ├── domain/               — what the mod knows, as opposed to what it draws
  │   ├── ability/
  │   │   ├── model/
  │   │   │   ├── AbilityInfo       — ability metadata record + the `id - englishName` encoding
  │   │   │   ├── AbilityCategories — the per-ability category choice and its lock, as the
  │   │   │   │                       server last reported it
  │   │   │   └── Pathways          — **the** single home for a pathway as a pathway:
  │   │   │                           normalisation, the 25-colour `pathwayRgb` table, the
  │   │   │                           emblem glyph, the spelling
  │   │   └── service/
  │   │       ├── AbilityRegistry   — the server's ability catalogue: both list formats (v1
  │   │       │                       delimited, v2 JSON) land in the same two collections, so
  │   │       │                       nothing downstream has to know which protocol answered
  │   │       └── AbilityBindings   — which ability sits in which key slot / wheel slot /
  │   │                               gesture, and the persistence behind all three; owns
  │   │                               MAX_ABILITIES
  │   ├── beyonder/
  │   │   └── model/        — one holder per S→C channel; all parse through util/JsonRead
  │   │       ├── BeyonderState     — madness, spirituality, health ceiling, pathway + sequence
  │   │       ├── ConditionsParser  — the `key=value;…` body of coi-client:conditions
  │   │       ├── SpiritualityTracker — spirituality as the HUD needs it: the server's last
  │   │       │                       figure plus the regen rate `predictedSpirituality()`
  │   │       │                       extrapolates from
  │   │       ├── ActingState       — acting progress, method cooldown, last grant
  │   │       ├── ResourceState     — ability resource meters keyed by id, TTL-expired
  │   │       ├── ActionBarState    — action-bar entries + client-side TTL expiry
  │   │       ├── TargetState       — last ability hit (name, before/after, HP)
  │   │       ├── CogitationState   — cogitation session: prompt, streak, timeout, fail
  │   │       ├── NotificationState — toast queue (3 visible, rest promoted in turn)
  │   │       ├── SheetState        — character sheet snapshot (identity, vitals, mind, acting
  │   │       │                       ledger, sub-menu gates)
  │   │       ├── SheetParser       — coi-client:sheet JSON → SheetState.Snapshot, defensively
  │   │       └── AppearanceState   — appearance traits per player UUID
  │   ├── menu/             — the declarative menu document (server-authored screens)
  │   │   ├── model/
  │   │   │   ├── MenuDocument      — session / version / screen id, header, sections, footer
  │   │   │   ├── MenuComponent     — sealed: text, note, stat, kv, checklist, button(s),
  │   │   │   │                       toggle, list, grid, input, divider, spacer, plus the v2
  │   │   │   │                       five (hero, details, steps, chips, panels)
  │   │   │   ├── MenuIcon          — pathway emblem / item model / glyph / ability / player
  │   │   │   │                       head / none
  │   │   │   ├── MenuParts         — the structures that appear in more than one component
  │   │   │   ├── MenuStyles        — the wire's enum words, folded onto this client's enums
  │   │   │   └── MenuLimits        — how much of a document this client will read
  │   │   └── service/
  │   │       ├── MenuParser        — defensive Gson; unknown `type`s skipped, sizes clamped
  │   │       ├── MenuJson          — the defensive reads a document is parsed with
  │   │       ├── MenuState         — the current menu document + a revision the screen watches
  │   │       └── MenuSample        — the hand-written document behind F8 → *Menu*
  │   ├── ceremony/         — protocol batch 7: the Sequence 0 ascension's set pieces. Six
  │   │   │                   more ids on the **existing** `coi-client:effect` channel,
  │   │   │                   announced by the `ceremony` feature (docs/ASCENSION_CEREMONY.md)
  │   │   ├── CeremonyEffect    — all six, one `Kind` per id: parse, push to a state, report
  │   │   │                       finished. The handle holds nothing itself
  │   │   ├── CeremonyEffects   — the hard reset every exit path shares, and the one tick this
  │   │   │                       batch needs: a new world, and the player taking the camera
  │   │   │                       back. Holds `CeremonyParams`, the defensive `key=value` read
  │   │   │                       plus the two shapes a stop arrives in (`stop`, `stop,fade=600`)
  │   │   ├── PostFx            — which pass is over the world, with the ramp either side, and
  │   │   │                       the five pass names with the graded chain each resolves to
  │   │   ├── SkyTintState      — the sky and fog an aftermath walks the world toward
  │   │   ├── LetterboxState    — the bars, and the slide at both ends
  │   │   ├── CameraCinematics  — the two things a rite does to the camera, both **view only**
  │   │   │                       and both read by `CameraMixin`: `ScreenShakeState`
  │   │   │                       (amplitude / hz / decay) and `CameraOrbitState` (the arc at
  │   │   │                       a rite's peak, as one `Placement` per frame)
  │   │   └── AudioBedState     — the crossfaded score bed and the `CeremonyBedSound`
  │   │                           instance behind it; specified, and
  │   │                           unused until the 22 `mysterria:ascension.*` tracks exist
  │   ├── effect/
  │   │   ├── EffectManager     — registry + active list, renders via HudRenderCallback
  │   │   ├── VisualEffect      — interface (start/render/isFinished/stop)
  │   │   ├── EffectSounds      — the audio companions (loops and one-shots)
  │   │   ├── HallucinationManager — client-side madness hallucinations, on the client tick
  │   │   └── visual/           — the effects themselves
  │   │       ├── EffectParams  — the `key=value,key=value` param splitter
  │   │       ├── EffectPaint   — the 2D paint helpers vanilla does not expose
  │   │       ├── CracksEffect, EyesEffect, VignetteEffect, HeartbeatEffect, GlitchEffect,
  │   │       │                   BloodRainEffect, FrostEffect, WhispersEffect, TunnelEffect,
  │   │       │                   FlashEffect, HallucinationEffect
  │   │       ├── ImpactFrameEffect — the sakuga impact frame (`impact_frame`): a 24 fps plan of
  │   │       │                   negative → held two-tone plate → release, composed around a
  │   │       │                   screen point or a projected world one; ImpactFrameDrawing is the
  │   │       │                   seeded drawing itself (splash, spikes, ring, lines, debris)
  │   │       ├── SpellImpactEffect — the world-space spell impact (`impact`); a shell over the
  │   │       │                   four collaborators below
  │   │       └── ImpactStyle (the presets), ImpactGeometry (the randomised shapes),
  │   │                           ImpactRenderer (every mark it puts on the world or the
  │   │                           screen), WorldImpact (one impact playing out at a position)
  │   ├── form/             — mythical creature forms (docs/MYTHICAL_FORMS.md)
  │   │   ├── MythicalFormManager  — uuid → pathway map, fed by coi-client:mythical
  │   │   ├── MythicalCreatureForm — per-pathway form; pathway/ holds all 20
  │   │   ├── FormPrimitives    — the hand-authored cuboid geometry the full forms are drawn from
  │   │   ├── PartialFormSpec   — placement/scale of a baked lower-body model
  │   │   ├── PartialForms      — shared resolve + carrier-transform helpers
  │   │   ├── PartialFormLayer  — draws the baked model as a player render layer
  │   │   ├── FormModel         — the `carrierDelta()` contract a baked model can implement
  │   │   ├── FormModelLayers   — registration + placement of the baked models
  │   │   ├── pathway/          — the 20 pathway forms (FoolForm, SunForm, VisionaryForm, …)
  │   │   └── model/            — Blockbench exports (VisionaryLowerModel/Animations);
  │   │                           **generated — regenerate from Blockbench, never hand-edit**
  │   ├── appearance/
  │   │   ├── AppearanceTraitLayer    — render layer for the traits the server granted
  │   │   ├── AppearanceTraitRenderer — one additive trait
  │   │   ├── TraitGeometry           — smooth tubes/quads/triangles in block units
  │   │   └── trait/                  — HornsTraitRenderer, MushroomTraitRenderer,
  │   │                                 FemaleTraitsRenderer
  │   ├── glyph/            : drawing magic's data (the server reads the strokes)
  │   │   ├── GlyphSheet        : the parsed `coi-client:glyph` open: session, caps, limits,
  │   │   │                       reference glyphs; clamped like every other channel
  │   │   └── GlyphDrawing      : the strokes per layer, thinned as they finish; builds the submit
  │   └── gesture/
  │       ├── GestureType       — 9 shapes (circle, V, Z, line down, caret, triangle,
  │       │                       square, hook, arc): direction templates + preview
  │       │                       polylines. Declaration order breaks scoring ties
  │       ├── DirectionCodes    — resampled stroke → 8-way direction string
  │       └── GestureRecognizer — resample → DirectionCodes → Levenshtein match
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
  │   ├── render/           — the paint the overlays share: no state, no render gates
  │   │   ├── CoiBar            — stateless bar layers (frame/fill/shimmer/notches/label)
  │   │   ├── HealthStyle       — which shape the HP pool takes (hearts / bar / ornate / pips),
  │   │   │                       and the per-style default Y offset
  │   │   ├── HealthBarPaint    — every pixel of all four; the absorption rule lives here
  │   │   ├── MadnessPalette    — the bar's colours and status word, one set per stage
  │   │   ├── MadnessCorruption — the full-screen stage effects: vignette, VHS, static
  │   │   ├── PlateCard         — the character plate's card, header and gauge rows
  │   │   ├── PlateSymbols      — the 32×32 fillable brain / mask sprites
  │   │   ├── SpiritSprites     — the three first-party spirituality sprites + the luster
  │   │   ├── AbilitySlotWidget — single slot: icon, cooldown, keybind, glow, toggle
  │   │   │                       outline + "ON" tag, red strike when locked/blocked
  │   │   └── SlotAnimations    — the three moments a slot animates: the cast punch, the
  │   │                           ready flash, the cooldown sweep
  │   └── layout/           — the HUD layout editor's model
  │       ├── HudLayout     — `editing()` flag every overlay's render gate honours
  │       ├── HudElement    — id / label / group / visible / bounds / moveTo / preview / reset
  │       ├── HudElements   — the descriptor list in draw order, `soloSet`, `byId`
  │       ├── LayoutGeometry — the arithmetic every element shares, incl. the anchored-move rule
  │       ├── LayoutGroups  — everything the editor does to a group rather than an element
  │       ├── ElementIds    — the stable, never-localized ids
  │       ├── SlotElement   — one ability slot (slot_1 … slot_10, one group)
  │       └── BarElements   — every bar-and-overlay descriptor, nested: the two bases
  │                           (`AbstractElement`, and `BarElement` — a 182-wide bar with its
  │                           label 10px above it) and the ten over them,
  │                           CharacterPlateElement, BeyonderHealthElement, MadnessElement,
  │                           SpiritualityElement, ActingElement, ResourceElement,
  │                           ActionBarElement, TargetHealthElement, CogitationElement,
  │                           NotificationElement
  ├── screen/               — the chrome these draw with lives in ui/CoiStyle
  │   ├── ScreenInput       — the keyboard concerns the mod's own screens share
  │   ├── ScrollbarPainter  — the 3px track-and-thumb bar drawn *beside* a card
  │   ├── CoiTabButton      — hand-drawn tab widget used by binding + settings screens
  │   ├── GestureScreen     — hold Left Alt, draw with mouse, release to cast;
  │   │                       inert until a gesture has an ability bound
  │   ├── glyph/            : GlyphCanvasScreen (layer tabs, ring guide, undo/clear, name,
  │   │                       submit, the server's verdict) + GlyphCanvasPainter (its pixels
  │   │                       and the reference sheet drawn from the server's templates)
  │   ├── TourScreen        — first-join walkthrough: spotlight cutouts + text cards,
  │   │                       movement stays enabled; re-run via "Show Tour Again"
  │   ├── TitleScreenHaunt  — the main menu remembers the madness you left with
  │   ├── title/            — the Lord of the Mysteries main menu (docs/TITLE_SCREEN.md)
  │   │   ├── TitleTakeover — the gate, the time base, the geometry and the one accent
  │   │   ├── TitleScene    — the void: gradient, central bloom, drifting motes
  │   │   ├── PathwayWheel  — the two rings, the 24 emblems, the lit one, the corruption
  │   │   ├── TitleLogo     — the wordmark, the pathway caption, the relocated splash
  │   │   └── TitleButtons  — the plate drawn in place of a vanilla button sprite
  │   ├── InventoryHint     — a strip on the vanilla inventory saying the Mystery Arts
  │   │                       item is gone and naming the live "open menu" keybind;
  │   │                       dismissed once, remembered in coi_client_state.json
  │   ├── sheet/            — the Beyonder character sheet (opened with M)
  │   │   ├── CharacterSheetScreen — one `MenuTheme` card, drawn as a menu document would be
  │   │   ├── SheetContext / SheetMetrics / SheetPalette — what a section is handed to draw
  │   │   │                       itself with, the measurements + the one hit test its cards
  │   │   │                       use, and every colour the sheet decides for itself
  │   │   ├── SheetHero, SheetVitals, SheetActing, SheetConditions, SheetDestinations,
  │   │   │   SheetFooter, SheetRows, SheetChips — one section each, in draw order
  │   │   └── SheetGlyphs   — the sheet's 8×8 drawn marks (heart/flask/hourglass, the
  │   │                       nine destination glyphs, lock)
  │   ├── ability/
  │   │   ├── AbilityBindingScreen — bind abilities to slots (opened with K); tabbed
  │   │   │                       (hotkeys/wheel/gestures) with a per-tab how-to banner
  │   │   ├── BindingRowPainter — one row of that screen's slot list
  │   │   ├── AbilityWheelScreen — the radial picker, held open on G
  │   │   ├── AbilityPickerOverlay — the modal chooser: the screen, its input and its scroll
  │   │   ├── PickerModel   — the flat Row list: the search, the grouping, the row heights
  │   │   ├── PickerPainter — one row at a time, plus the tooltip after the scissor
  │   │   ├── PickerMetrics — ROW_H 26 / HEADER_H 13 / UNBIND_H 18 / MESSAGE_H 18
  │   │   └── PickerLabels  — every word a row or its tooltip puts on screen
  │   ├── settings/
  │   │   ├── HudSettingsScreen — HUD customization: 3 tabs (Ability HUD/Elements/General),
  │   │   │                       scrollable rows so it fits any gui scale. Holds **no**
  │   │   │                       position rows: "Arrange on screen…" plus the per-element
  │   │   │                       *Align* buttons are the only position UI
  │   │   ├── SettingsTabs  — the contents of those three tabs, one method each
  │   │   ├── SettingsRows  — the row builders the tabs compose
  │   │   ├── HudLayoutScreen — drag-to-position editor: sample previews of every
  │   │   │                       element over the live world — or one element / one group,
  │   │   │                       in solo mode — snapping, Ctrl group move, nudge keys
  │   │   ├── LayoutPainter — what that editor paints over the world
  │   │   └── LayoutSnap    — where a dragged element actually lands
  │   ├── menu/             — the server-authored menu renderer
  │   │   ├── MenuScreen    — one scrolling card: header + sections + footer,
  │   │   │                   hand-drawn buttons, live list search, confirm modal,
  │   │   │                   collapsible sections + details, draggable scrollbar
  │   │   ├── MenuTheme     — every colour and primitive the screen draws with
  │   │   ├── MenuContext   — everything a collaborator may ask the screen for, including
  │   │   │                   `approach` (the one easing chokepoint)
  │   │   ├── MenuPart / MenuPartFactory — one laid-out piece in content space, and the
  │   │   │                   document → flat part list the card scrolls
  │   │   ├── MenuTextParts / MenuValueParts / MenuControlParts / MenuCollectionParts —
  │   │   │                   the words, the values, the things the player operates, and
  │   │   │                   the parts that repeat a cell
  │   │   ├── MenuChrome, MenuGauges, MenuIcons, MenuScrollbar, MenuConfirmModal
  │   │   └── MenuMetrics   — the geometry a document is drawn to, and its one hit test
  │   └── debug/            — dev-only (F8)
  │       ├── EffectDebugScreen — test effects, forms, bars and menus without a server
  │       ├── DebugStates       — the state pokes behind its buttons
  │       └── AppearanceDebugScreen — preview the appearance traits
  ├── input/
  │   └── CoiKeyBindings    — every keymapping and what pressing one does; presses are
  │                           edge-triggered by hand, because the wheel and gesture screens
  │                           need to know the key is still *held*
  ├── mixin/                — every mixin and accessor
  │   ├── LivingEntityRendererMixin — the mythical form: cancels the vanilla player render
  │   │                       for a full form, pushes hipRaise + the carrier transform
  │   │                       for a partial one
  │   ├── AvatarRendererMixin / AvatarRenderStateMixin (with `util/duck/AvatarRenderStateAccessor`) —
  │   │                       attach the mod's layers, and carry a player UUID on a render
  │   │                       state vanilla gives no identity to
  │   ├── PlayerModelMixin / HumanoidArmorLayerMixin — hide legs, then leggings + boots,
  │   │                       under a partial form
  │   ├── TitleScreenMixin  — the title-screen haunting hook, and the takeover's scene
  │   │                       (`extractBackground`, in place of the panorama)
  │   ├── LogoRendererMixin / SplashRendererMixin — suppress the vanilla wordmark, and
  │   │                       redraw the splash off ours, while the takeover is on
  │   ├── AbstractButtonMixin — the title screen's button chrome (`extractDefaultSprite`)
  │   ├── GameRendererMixin — runs the ceremony's post chain over the finished world,
  │   │                       in the same gap vanilla uses for the creeper view
  │   ├── CameraMixin       — the ceremony's orbit and shake, at the end of
  │   │                       `alignWithEntity` so the cull frustum follows them
  │   ├── FogRendererMixin / SkyRendererMixin — the two halves of `coi-client:sky_tint`:
  │   │                       the fog's colour and planes, and the sky disc
  │   ├── LoadingOverlayMixin / JoinMultiplayerScreenMixin / OnlineServerEntryMixin — the
  │   │                       startup logo and the Mysterria server-list entry
  │   └── EntityRendererAccessor / LivingEntityRendererAccessor / SelectionListEntryInvoker
  ├── ui/
  │   ├── CoiStyle          — shared dark/gold palette + card chrome; the BACKDROP / SCRIM /
  │   │                       VEIL dimming ladder, `cardWidth` and `formWidth`
  │   ├── CoiIcons          — the mod's first-party GUI icons, and the one way to draw them
  │   ├── AbilityIcons      — shared icon renderer: pack item model, else category/tier
  │   ├── IconModels        — "is that item model loaded?", cached per reload
  │   └── IngredientTooltips — names the pathway an item belongs to, in its tooltip
  └── util/
      ├── CoiLog            — the mod's one logger ("COI Client"); no System.out.println remains
      ├── JsonRead          — the defensive scalar reads every state class shares
      ├── duck/             — duck interfaces the mixins implement. **Must live outside
      │   │                   `mixin/`**: the mixins.json `package` owns that whole subtree,
      │   │                   and Mixin refuses to let ordinary code load a class from it
      │   └── AvatarRenderStateAccessor — the player UUID `AvatarRenderStateMixin` adds
      └── hooks/
          └── DiscordPresenceManager — Discord Rich Presence via discord-game-sdk4j
                                       (pure-Java IPC, bundled jar-in-jar); lazy connect
                                       on first join, APP_ID = 0 disables it entirely
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

- **`util/JsonRead`** — the defensive scalar reads (`string`, `intOf`, `dbl`, `bool`, `object`, …)
  that every `domain/beyonder/model/` class shares. Each answers "absent" with the caller's default instead of
  throwing, so a `handle` method only guards the two things that genuinely are fatal: a body that is
  not an object, and a value of the wrong Java type. `domain/menu/service/MenuJson` is the same idea for menu
  documents, with the length clamps a document needs.

- **`ui/CoiStyle`** — besides the dark/gold palette and the card chrome, it now names the screen
  dimming ladder: **`BACKDROP`** for a modal that owns the screen, **`SCRIM`** for one the player is
  expected to glance past, **`VEIL`** for the layout editor, where the world has to stay readable
  while it is dragged on. Choosing between them is a judgement about the screen behind, not a taste
  in alpha, which is why they are named rather than typed as hex at the call site. It also carries **two** width rules:
  `cardWidth` for a scrolling content card (the sheet and the menus, which are
  the same object to a player) and `formWidth` for a settings-style column of labelled rows (the
  binding screen, HUD settings). `formWidth` is deliberately **not** `cardWidth` — a form stops
  being readable long before a document does, so it takes a wider gutter and caps far lower, and
  keeping the rules apart is what stops a later widening of the reading card from stretching the
  forms with it.

**The big screens are shells over collaborators.** `MenuScreen`, `CharacterSheetScreen`,
`AbilityPickerOverlay`, `HudSettingsScreen`, `HudLayoutScreen`, `SpellImpactEffect` and
`HudElements` each kept their public surface and their behaviour, and handed the drawing out:

| Shell                                    | Keeps                                                                             | Hands out                                                                                                                                                                     |
|------------------------------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `screen/menu/MenuScreen`                 | the vanilla `Screen` lifecycle, the session's `__close` guard, scroll, `approach` | `MenuPartFactory` → `MenuPart`s; `Menu{Text,Value,Control,Collection}Parts` draw them; `MenuChrome`/`MenuGauges`/`MenuIcons`/`MenuScrollbar`/`MenuConfirmModal`/`MenuMetrics` |
| `screen/sheet/CharacterSheetScreen`      | the `sheet_open`/`sheet_close` lifecycle, the card, the scroll                    | one `Sheet*` class per section, all handed a `SheetContext`; `SheetMetrics`/`SheetPalette`                                                                                    |
| `screen/ability/AbilityPickerOverlay`    | the modal, the search box, the keyboard                                           | `PickerModel` (rows), `PickerPainter` (paint), `PickerMetrics`, `PickerLabels`                                                                                                |
| `screen/settings/HudSettingsScreen`      | the tabs, the working copy, Done/Cancel                                           | `SettingsTabs` (which rows), `SettingsRows` (how a row is built)                                                                                                              |
| `screen/settings/HudLayoutScreen`        | selection, drag, keys                                                             | `LayoutPainter` (what is drawn over the world), `LayoutSnap` (where it lands)                                                                                                 |
| `domain/effect/visual/SpellImpactEffect` | the `VisualEffect` contract and the params                                        | `ImpactStyle`, `ImpactGeometry`, `ImpactRenderer`, `WorldImpact`                                                                                                              |
| `hud/layout/HudElements`                 | `all` / `byId` / `soloSet` / `moveGroupBy`                                        | one `BarElements.*Element` / `SlotElement` descriptor per element; `LayoutGeometry`, `LayoutGroups`                                                                           |

The seam is always the same: the shell owns *state and lifecycle*, the collaborators own *pixels and
arithmetic* and are handed everything they need. So a rendering change is a change in one
collaborator, and none of the invariants recorded below moved with the code.

## Key patterns

- **Static singletons, one per concern** — `CoiClient` is the initializer and nothing else. The
  ability catalogue is `domain/ability/service/AbilityRegistry`, the slot bindings
  `domain/ability/service/AbilityBindings`, the
  pathway identity `domain/ability/model/Pathways`, the keymappings `input/CoiKeyBindings`, the wire
  `network/CoiNetworking`, and each S→C channel's data one class under `domain/beyonder/model/`. Screens and widgets
  still reach all of them through static methods — what changed is which class answers, not how.
- **Real-time cooldowns** — tracked via `System.currentTimeMillis()`, not ticks, for smooth animation.
- **Lazy effect geometry** — `CracksEffect` generates crack segments on first render (needs screen dimensions); seeded
  by `startTime` for consistent patterns.
- **Dev-only keybindings** — `effectDebugMenu` (F8) is only registered when `FabricLoader.isDevelopmentEnvironment()`.

## Keybindings

| Key             | Action                                                                                                                                                                                                                                                                                                                                  |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Z–N (6 keys)    | Ability slots 1–6 (each slot is placed on its own in the layout editor)                                                                                                                                                                                                                                                                 |
| *(unbound)*     | Ability slots 7–10 — assign in vanilla Controls, activate via `activeAbilitySlots`                                                                                                                                                                                                                                                      |
| G (hold)        | Ability wheel — radial picker, open while the key is held, `wheelSlots` slots                                                                                                                                                                                                                                                           |
| K               | Open Ability Binding screen                                                                                                                                                                                                                                                                                                             |
| M               | With `useServerMenus`, the plugin's chest GUI straight away (`menu_action`, no longer excluded on `menu_archive` servers); else the character sheet (`character_sheet`), else the server Beyonder menu, else an unsupported message. The preference only *reorders* the first two — a server without `menu_action` still gets the sheet |
| *(unbound × 9)* | Open one of the sheet's destinations directly (`church`, `abilities`, `mythical`, `uniqueness`, `honorific`, `map`, `seat`, `throne`, `pantheon`) — `ActionPayload.ofOpen`, no sheet behind it, so the menu's back arrow means "close"                                                                                                  |
| Left Alt (hold) | Gesture casting — draw a shape, release to cast (only when a gesture is bound)                                                                                                                                                                                                                                                          |
| F8 *(dev only)* | Open Effect Debug screen                                                                                                                                                                                                                                                                                                                |

## Pathway colours

Extracted from first segment of ability ID (before first `-`):
`fool`=purple, `door`=blue, `sun`=yellow, `tyrant`=cyan, `demoness`=red, `priest`=orange

The table itself is `domain/ability/model/Pathways.pathwayRgb` — **one** map for all 25 pathways, shared by the
HUD slots, the picker, the acting bar, the plate, the sheet and `MythicalFormManager`.
`AbilityInfo.pathwayColor`/`pathwayColorByName` are thin ARGB wrappers over it, kept because callers
that only hold an ability id read better that way.
