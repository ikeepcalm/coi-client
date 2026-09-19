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

This uses a separate game directory, opens thirteen views at three GUI scales (including partial
and unknown forms, spacious Status, and the compact tabs at their scroll limits), writes screenshots under
`build/archive-preview/screenshots`, and exits. The capture hook requires both development mode
and the explicit `coi.archivePreview` JVM property; it also refuses multiplayer worlds.

The first release covers the dossier and fixed mythical forms. Visionary's form selection remains
its existing server-owned flow. Ability manuals, church ledgers, relic presentation and milestone
sequences are subsequent work; they are not implemented by this change.

## Verification performed

Both offline builds passed. Client protocol/localization tests and all 159 server tests passed.
Minecraft 26.2 development runs captured the three dossier pages, two GUI scales, full and partial
creature models, and the missing-model fallback. English and Ukrainian dossier labels were checked
in game. These captures use sample data in a copied singleplayer world; a live plugin connection
and every pathway's model have not been exercised by this smoke run.
