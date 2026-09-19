# Dossier and specimen presentation

The character sheet uses a fixed portrait leaf and three index pages: Status, Acting record,
and Destinations. Left/right arrows switch pages; Tab and Enter operate the index buttons.
Each page remembers its scroll position. At small GUI widths the portrait becomes an inline
identity block. Status shows acting progress; its full source ledger remains on Acting record.
No facts are generated from the portrait or its decoration.

Pages use spare height automatically: acting source rows spread vertically, destination tiles
grow with centered labels and matching hit areas, and Status distributes space between its rows
and sections. Wrapped text is measured before expanding Status. Expansion stops when content
needs scrolling; resizing and GUI scale changes recalculate the available space.

Acting source rows have alternating surfaces and bottom rules, with content centered vertically.
Menu pathway icons use the original-resolution transparent art in `textures/pathways/quality`,
without multiplying its colors by the pathway accent. Texture metadata enables smooth filtering.
Missing quality variants fall back to the bundled emblem, then the inline glyph/crest. Resource
reload clears availability caches; inline text continues to use the compact pathway font.

## Server contract

Protocol remains 2. Clients advertise `menu_specimen` in addition to `menu_ui`.
The mythical-form adapter includes this optional member only for supporting clients:

```json
"presentation": {
  "template": "specimen",
  "subject": "giant",
  "caption": "Form study"
}
```

The subject identifies an installed mythical-form renderer; the caption is localized by the server.
The client never branches on the menu screen ID. Unknown templates use the ordinary document
renderer. Unknown models show the server's icon and an unavailable message. Every description,
state, warning and action remains in ordinary `sections` and `footer`, including future fields.
Older clients therefore retain the full menu. The server still validates session, document version,
permissions, cooldowns and transformations using the existing action handlers.

The specimen camera supports dragging to rotate and scrolling to zoom. On wide windows it is a
separate leaf; on narrow windows it scrolls above the ordinary document. Reduced-effects mode freezes
the specimen's animation clock. Camera input is deliberate and remains available.

## Render isolation

`EntityStudy` creates a fresh avatar render state through the vanilla inventory rendering pipeline.
An optional `previewForm` on that state overrides the form lookup for this draw alone: null means
world state, empty means human portrait, a pathway means the specimen. Extraction always resets
the override. Neither the entity nor `MythicalFormManager`'s authoritative transformation map is
modified. Full procedural forms and partial baked forms use the same lookup.

## Development verification

`gradlew.bat test --offline` covers presentation parsing, malformed/unknown hints, legacy documents,
closure and preservation of server action IDs and disabled states. The server's `MenuDocumentTest`
covers serialization, complete fallback content and unchanged handler registration.

F8 opens the existing development panel. **Sheet** seeds the dossier; **Specimen preview** opens a
disabled-action sample. Neither requires a plugin server.

For repeatable runtime screenshots, put a disposable singleplayer world at
`build/archive-preview/saves/Preview`, then run:

```powershell
.\gradlew.bat runClient -ParchivePreview --offline
```

This uses a separate game directory, opens forty views at three GUI scales (including partial
and unknown forms, spacious Status, and the compact tabs at their scroll limits), writes screenshots under
`build/archive-preview/screenshots`, and exits. The capture hook requires both development mode
and the explicit `coi.archivePreview` JVM property; it also refuses multiplayer worlds.

The presentation covers the dossier, fixed mythical forms, the [ability field manual](ABILITY_MANUAL.md),
church ledgers, uniqueness reliquaries, honorific inscriptions, map visibility and seat challenges.
`menu_archive` negotiates the five additional templates: `ledger`, `relic`, `inscription`, `atlas`,
and `challenge`. A wide folio includes a section index; narrow windows use a compact index strip.
Section labels and offsets come from the actual document. Page Up/Down and Home/End navigate
the reading area while a text field is not focused. Server sections, gates and confirmations stay
authoritative. The native pathway chooser preserves access to secondary pathways. Visionary's
conception/form chooser remains its existing server-owned flow.

## Pathway portraits

The portrait uses the server sheet's pathway and sequence to select a dark scene and an isolated
model pose. Every recognized pathway has its own scene and animated gesture: Priest flames,
Fool masks, Door stars, Sun orbiting suns, Hermit scrolls, Tyrant rain and water currents,
Chained foreground restraints, Paragon gears, Tower rising masonry, Fortune dice, Aeon hourglasses,
Patriarch birds, Sublunary orbital moons, Abyss fissures, Death shades, Demoness mirrors,
Darkness eclipse veils, Moon lunar phases, Mother blossoms, Hanged sacrificial roots,
Giant twilight blades, Emperor crown and columns, Justiciar scales, Visionary dream eyes,
and Error clocks and worms. Unknown pathways retain a neutral constellation fallback.
Effects increase toward Sequence 0; Priest and Giant carry a cosmetic sword
at Sequences 2–0. These decorations confer no equipment or abilities.

The engraved halo rotates slowly. Reduced-effects mode freezes every portrait animation.
The dossier head follows the cursor with a smoothed additive gaze, limited to 6 degrees horizontally
and 4 degrees vertically. Body and limb poses remain unchanged, keeping props and restraints aligned.
Reduced-effects mode also disables this gaze offset.
Pose and item overrides exist only on the GUI avatar render state, reset during normal extraction;
the live player's position, equipment and animation are never changed. Portrait samples at
Sequences 9 and 0, compact layouts and frozen/animated modes are included in the capture run.

## Verification performed

The client offline build and 13 protocol/localization/projection tests passed. The Minecraft 26.2
development run captured all 40 views, including the five additional menu families and Priest,
Fool and Door portraits. Binding persistence, passive state confirmation and normal render-state
extraction after closing a portrait were asserted in game. These captures use sample data in a
copied singleplayer world; a live plugin connection has not been exercised by this smoke run.
The separate `gradlew.bat build runClient -PportraitPreview --offline` run captures all 25 pathways
at Sequences 9 and 0, frozen and compact Chained portraits, and the honorific emblem alignment.
