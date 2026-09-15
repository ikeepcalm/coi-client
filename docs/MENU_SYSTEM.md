# The COI menu system — native server menus

**Status:** implemented on both sides, both builds green. The original renderer was runtime-tested by
the user ("it works"); **the v2 vocabulary and the seven adapter rewrites are built but not yet
runtime-tested.**

**The visual redesign has happened** — see [Vocabulary v2](#vocabulary-v2) below. The section
[Redesigning the visuals](#redesigning-the-visuals) is kept because its constraints still bind.

---

## Vocabulary v2

The v1 renderer could attach an icon to exactly three things: the document header, a button, and a
list/grid row. `kv` rows, `stat`, `checklist` items, `note`, `toggle` and section headings had no
icon field at all, in the parser or the drawing code. An adapter that wanted to *explain* something
therefore had one tool — a muted grey paragraph.

Four surveys of all 22 screens measured the result: **zero** component-level icons across Map,
Mythical, Seat, Uniqueness and Honorific; ~28,000 characters of `menu.*` prose over 349 keys, **52
blocks of it over 140 characters**; 14 `text` components on `seat.challenge` alone, two of its
sections pure prose before the player reached any control; and `uniqueness` showing six stat bars
each trailed by a 150–220 character paragraph. Grid cells parsed `title`, `subtitle`, `badge` and
`color` and drew **none** of them. The whole system had one animation.

### What was added

Five component types — `hero`, `details`, `steps`, `chips`, `panels` — plus an `icon` field on `kv`
rows / `checklist` items / `stat` / `note` / `toggle` / section headings, a tri-state checklist
(`ok` / `no` / **`pending`**), `stat` `style` (`bar`/`ring`/`segments`) / `cap` / `delta`, section
`icon`/`badge`/`collapsed`, list-row `fraction`/`meta`, `grid` `size` (`small`/`medium`/`large`, and
cells now actually draw their caption, badge and colour edge), a labelled `divider`, and a fourth
`MenuIcon` kind, **`glyph`**, naming one of 25 icons that ship in the client jar.

**Purely additive. The protocol stays 2 and the feature id stays `menu_ui`** — the capability list
negotiates this, not a version bump. An older client skips an unknown `type` and ignores unknown
fields, exactly as it already did.

### The result

**Zero standalone `text` components remain in any of the eleven adapter files.** Every paragraph now
lives inside a `details` disclosure, collapsed by default — the information was never the problem,
showing it unasked was.

| Screen | `text` before | after |
|---|---|---|
| `seat.challenge` | 14 | 0 |
| `uniqueness` | 12 | 0 |
| `abilities.catalogue` | 9 | 0 |
| `honorific` | 5 | 0 |
| `mythical.form` | 3 | 0 |
| `map.visibility` | 2 | 0 |
| church (15 screens) | 9 | 0 |

Things that were being said twice were cut, not moved: `menu.mythical.warn.incomplete` (190 chars
restating the Stability hint), `gui.church.site.aura-saturation.factors` (restating in numbers the
gauge directly above it), the two `church.regional` lines restating their own checklist lore, the
`church.site.staff.confirm` action sentence that appeared **three times** on one screen, and the
divine-actions line that repeated its own button's `disabledReason`.

### Two client rules a change here must keep

- **Disclosure state is keyed `screenId + "/" + id` and survives a rebuild**, resetting only when
  the screen id actually changes. A 60-tick refresh slamming shut what the player just opened is the
  bug this prevents — the same bargain renderer decision 6 makes for `EditBox` identity.
- **All easing funnels through `MenuContext.approach`**, which returns the target outright under
  `epilepsyMode`. One chokepoint, so a future animation cannot forget the setting.

### The back arrow

Every root document sets `back(true)`, but `MenuRouter.tryOpen` opens it with `open()`, which resets
the stack — so `__back` at the root reached `session.pop() == null` and fell through to
`close(player)`. The menu vanished instead of going back, which read as "quit". Deeper screens (the
church tree, which uses `ChurchMenus.push`) were always fine.

`MenuChannelUtil.back` now answers the empty-stack case with `{"closed":true,"back":true}`; every
other close path still sends `{"closed":true}` with no flag. The client reopens
`CharacterSheetScreen` when the flag is set **and** `MenuState.openedFromSheet()` **and** the
server advertises `character_sheet`. **Esc and the X still close outright** — only the back arrow
goes back. The flag is read off the raw JSON in `handleMenu` rather than through `MenuParser`,
because it describes the *transition*: a closed document has no screen to describe.

This document is the whole system in one place: why it exists, the wire contract, both halves'
architecture, every screen that was ported, everything that still opens a chest GUI, and the recipe
for adding the next one. It spans two repositories:

|                       | Path                                                                               | Build                                                                                                                                         |
|-----------------------|------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Client (Fabric mod)   | `F:\Mysterria\coi-client`                                                          | `./gradlew build` → `build/libs/coi-client-1.8.0.jar`                                                                                         |
| Server (Paper plugin) | `C:\Users\ikeepcalm\Documents\Mysterria\CircleOfImagination`, branch `development` | `./gradlew build --offline` — the plain build fails on an unreachable snapshot repo, which is a network fact about this machine, not the code |

Companion docs: `docs/NETWORK_PROTOCOL.md` (client, channel table), `docs/CLIENT_PROTOCOL.md`
(plugin, the same contract from the server's side), `CLAUDE.md` in both repos.

---

## 1. Why this exists

The plugin rendered every player-facing menu as an InvUI chest GUI: items whose names are labels and
whose lore is the body text, dyes as buttons, glass panes as borders, paged selectors for lists, and
click-twice-in-ten-seconds for confirmation. A vanilla client cannot be told to draw anything else.

The character sheet (`M`) was already native, but its seven buttons handed the player straight back
to those chest screens — which is the complaint that started this work.

**The rejected design** was a bespoke payload and a bespoke `Screen` per menu. `ChurchGUI` alone is
2833 lines and roughly thirty screens (six role views, people, sites, wars, artifacts, blessings,
diplomacy, watcher settings, gifts, selectors, confirms). That design needs a client release for
every server-side menu change, and it moves navigation and gating — which already live in the
plugin, correctly — into the mod.

**The chosen design:** the server ships a *document* describing one screen; the client renders it;
every click goes back and is answered with the next document. One renderer covers every menu that
exists today and every menu added later, and the plugin keeps owning navigation, permissions and
side effects.

---

## 2. Wire contract

Feature id (both directions): **`menu_ui`**. **The protocol version stays 2** — the capability list
is what negotiates this, not the version number. (Decided explicitly; do not bump it for menus.)

### 2.1 Channels

| Direction | Channel                        | Payload                                                | Cap    |
|-----------|--------------------------------|--------------------------------------------------------|--------|
| S→C       | `coi-client:menu`              | one JSON string: a whole screen                        | 1 MiB  |
| C→S       | `coi-client:menu_action`       | one JSON string: one click                             | 32 KiB |
| C→S       | `coi-client:action` (existing) | `{"action":"open","target":…,"ui":"client"\|"server"}` | —      |

`ui` carries the player's own preference (`useServerMenus`). `client` — which is also what an absent
field means — asks for a document; `server` asks for the original chest GUI. A server that predates
this ignores the field and opens its chest GUI either way.

### 2.2 The document

```json
{
  "session": "7f3a…",
  "version": 4,
  "screen": "church.main",
  "title": "Church of the Fool",
  "subtitle": "Sanctum of Dreams · 12 members",
  "accent": "B347CC",
  "icon": {
    "kind": "pathway",
    "value": "fool"
  },
  "back": true,
  "closable": true,
  "toast": {
    "style": "error",
    "text": "You are not a bishop any more."
  },
  "sections": [
    {
      "title": "Standing",
      "components": []
    }
  ],
  "footer": [
    {
      "type": "button"
    }
  ]
}
```

- `session` — opaque token minted per menu session; `version` increments on every push.
- `screen` — a stable id used for **scroll/search retention** and logging. The client never branches
  on it beyond that: the same id on a push keeps the scroll offset and typed search text, a
  different id resets both.
- `accent` — 6 hex digits, no `#`. Drives the card rule, headings and primary buttons.
- `icon.kind` is one of `pathway` (a pathway key → the client's emblem font), `item`
  (`namespace:path` item model → the client's item route), `head` (player UUID),
  **`glyph`** (a bare name → one of the 25 16×16 icons shipped in the client jar; see
  `MenuIcon.Glyph` server-side and `CoiIcons.GLYPHS` client-side), `ability` (an ability id, drawn
  through the same `ui/AbilityIcons` route the HUD slots and the picker use), `none`.
  `glyph` is the only kind whose art does not depend on the player's resource pack, so it is the
  right choice for anything conceptual — a cost, a cooldown, a sequence — and `item` stays for
  things that really are an item.
- `toast.style` is one of `info`, `success`, `warn`, `error`.
- `{"session":…,"closed":true}` closes the screen. The client does **not** echo `__close` back for
  it — the server already knows, and echoing would race its next open.
- Every string arrives **already localized**. A lang key must never go over this wire.

### 2.3 Components

Inside `sections[].components[]`, and `footer[]` which takes buttons only:

| `type`      | Fields                                                                                                      | Renders as                                                    |
|-------------|-------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| `text`      | `text`, `style`, `align`                                                                                    | wrapped paragraph                                             |
| `note`      | `style` (note styles), `title?`, `text`                                                                     | callout box with an accent edge                               |
| `stat`      | `label`, `value`, `fraction?`, `color?`, `hint?`                                                            | label + right-aligned value, gauge when `fraction` is present |
| `kv`        | `rows[{label,value,color?,hint?}]`                                                                          | two-column table                                              |
| `checklist` | `items[{ok,label,detail?}]`                                                                                 | tick/cross list                                               |
| `button`    | `id`, `label`, `desc?`, `style`, `enabled`, `disabledReason?`, `confirm?{title,body,confirmLabel}`, `icon?` | full-width button, `desc` on a second line                    |
| `buttons`   | `buttons[<button>]`, `columns?`                                                                             | row or grid of buttons                                        |
| `toggle`    | `id`, `label`, `on`, `enabled`, `desc?`, `onText?`, `offText?`, `disabledReason?`                           | switch row                                                    |
| `list`      | `id`, `rows[<row>]`, `searchable?`, `maxVisible?`, `empty?`                                                 | scrollable card rows                                          |
| `grid`      | `cells[<row>]`, `columns?`                                                                                  | icon tiles with tooltips                                      |
| `input`     | `id`, `label?`, `placeholder?`, `value?`, `maxLength?`, `submit`, `submitLabel?`, `hint?`                   | text field + submit                                           |
| `divider`   | —                                                                                                           | hairline                                                      |
| `spacer`    | `size?`                                                                                                     | vertical gap                                                  |

A row (`list` / `grid`): `id`, `title`, `subtitle?`, `icon?`, `badge?`, `badgeColor?`, `tooltip[]?`,
`action?`, `enabled`, `color?`. A row **without** `action` is drawn but inert — that is how a
read-only ledger is expressed.

Style vocabularies (lowercase on the wire, from the server enums' `id()`):

- text: `body`, `muted`, `heading`, `warn`, `danger`, `success`
- note and toast: `info`, `success`, `warn`, `error`
- button: `primary`, `secondary`, `danger`, `success`, `ghost`
- align: `left`, `center`, `right`

**Forward compatibility is part of the contract:** an unknown `type` is skipped and the rest of the
screen still draws; unknown fields are ignored. A newer plugin against an older client must lose a
row, never a screen.

### 2.4 The click

```json
{
  "session": "7f3a…",
  "version": 4,
  "action": "kick_3",
  "value": "optional text"
}
```

- Action ids are opaque tokens the server minted for **that document version**. The client never
  invents one, except the two reserved lifecycle actions: `__back` (the server pops its stack) and
  `__close` (the screen went away; clears the session).
- `value` is only sent for an `input` submit.
- A click carrying a stale `version` is dropped: the screen it names no longer exists.

### 2.5 Security model

All enforced in `MenuChannelUtil`. An incoming plugin message is attacker-controlled.

On the netty thread: action id at most 64 chars, a live session must exist, the `session` token must
match, `value` at most 256 chars (**dropped, not truncated**), and at most 12 clicks per second per
player. `__close` bypasses the limiter — a dropped close leaks a session. Dispatch is then scheduled
onto the main thread, where **the version is re-checked**, closing the TOCTOU against a racing
refresh. A throwing handler or a throwing document build is logged and swallowed; a build failure
makes `tryOpen` return `false` so the caller still opens its chest GUI. Sessions die three ways:
`__close`, `PlayerQuitEvent`, and a 60-second sweep dropping offline players and anything idle for
over 10 minutes. Over 1 MiB the screen is closed rather than truncated. Back-stack depth is capped
at 24.

**Never trust a document's own gate.** A document can be minutes old, so every handler re-resolves
the player, the pathway and the permission before it mutates anything. Build-time role checks are
for *drawing*, never for *deciding*.

---

## 3. Server half (plugin)

Package `dev.ua.ikeepcalm.coi.menu`:

| File                                                 | Job                                                                                                                                                                                                                                                     |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MenuDocument.java`                                  | the document plus the whole nested builder tree (`Builder`, `SectionBuilder`, `FooterBuilder`, `ButtonBuilder`, `ButtonRowBuilder`, `ToggleBuilder`, `KeyValueBuilder`, `ChecklistBuilder`, `ListBuilder`, `GridBuilder`, `RowBuilder`, `InputBuilder`) |
| `MenuComponent.java`                                 | sealed, 13 records                                                                                                                                                                                                                                      |
| `MenuRow.java`, `MenuIcon.java`, `MenuHandler.java`  | row model, icon model, handler type                                                                                                                                                                                                                     |
| `MenuJson.java`                                      | package-private emission helpers                                                                                                                                                                                                                        |
| `TextStyle`, `TextAlign`, `NoteStyle`, `ButtonStyle` | wire vocabularies; `id()` is the lowercase name                                                                                                                                                                                                         |
| `MenuSession.java`                                   | per player: token, version, handler map, `Supplier<MenuDocument>` back stack (depth 24), idle clock                                                                                                                                                     |
| `MenuChannelUtil.java`                               | the channels, `open` / `push` / `refresh` / `back` / `close` / `clear` / `isOpen` / `handle`, validation, rate limit, sweep                                                                                                                             |
| `MenuRouter.java`                                    | the single decision point: `tryOpen(player, target, pathway, preferClientUi)`                                                                                                                                                                           |
| `adapter/…`                                          | one class per screen family                                                                                                                                                                                                                             |

Wiring: `ClientFeature.MENU_UI("menu_ui")` (not in `LEGACY`), `menu_ui` among
`ServerInfoChannelUtil`'s advertised features, `CircleOfImagination` constructs `menuChannelUtil` and
`menuRouter` and exposes getters, `ClientHelloListener` parses the incoming channel and reads `ui`,
and `onQuit` clears the session.

### 3.1 The builder DSL

An adapter is a plain function `(Player, FlexiblePathway) → MenuDocument`. **Handlers are lambdas and
the builder mints the action id**, so an adapter reads like the InvUI click handler it replaces:

```java
return MenuDocument.builder("map.visibility")
        .

title(tr(player, "menu.map.title"))
        .

subtitle(subtitle(player, protectedFromDivination, visible))
        .

accent(pathway.getColor())                    // NamedTextColor overload
        .

icon(MenuIcon.pathway(pathway.getName()))
        .

back(true)
        .

section(tr(player, "menu.map.section.status"),s ->s
        .

note(NoteStyle.SUCCESS, tr(player, "menu.map.note.hidden"))
        .

kv(rows ->rows.

row(label, value, GREEN))
        .

text(tr(player, "menu.map.explain.maps"),TextStyle.MUTED))
        .

section(tr(player, "menu.map.section.control"),s ->s
        .

toggle(t ->t.

label(label).

on(!visible).

enabled(canToggle)
                        .

states(onWord, offWord)
                        .

disabledReason(reason)
                        .

onActivate(MapVisibilityMenu::flip)))
        .

build();
```

Every builder has a short overload for the common case (`button(label, style, onClick)`,
`toggle(label, on, onActivate)`, `list(l -> l.row(title, onClick))`) and a `Consumer<XBuilder>` for
the full form. Inputs take `BiConsumer<Player, String>`.

**Two rules that shape every adapter:**

1. **A handler does not ask for a redraw.** After it returns, the screen it was clicked on is
   refreshed automatically *unless* the handler navigated — `push`, `back`, `close` and an explicit
   `refresh` all move the version. A toggle adapter is a state flip and nothing else.
2. **A disabled control carries no action id at all** — nothing to forge, nothing to find in the
   handler map. So `disabledReason` is the entire UX of a locked control: write it as a sentence
   that tells the player what to do about it.

Because the back stack stores `Supplier<MenuDocument>`, `__back` **rebuilds** the previous screen
against live state instead of replaying a stale snapshot. Document builds must therefore be
side-effect-free and cheap enough to run on every refresh.

### 3.2 Routing

```java
register(CharacterSheetChannelUtil.TARGET_MAP, MapVisibilityMenu::document);
```

`MenuRouter.tryOpen` returns `false` — meaning *the caller opens its own chest GUI* — when there is
no adapter, no `menu_ui` client, the player asked for server GUIs, the pathway is null, or the
document failed to build. `ClientHelloListener.openTarget` reads:

```java
if(!CharacterSheetChannelUtil.isActionAllowed(player, beyonder, pathway, target))return;
        if(plugin.

getMenuRouter().

tryOpen(player, target, pathway, clientUi))return;
        switch(target){ /* the original InvUI chain, unchanged */ }
```

All seven sheet targets are registered: `map`, `mythical`, `seat`, `uniqueness`, `honorific`,
`abilities`, `church`.

---

## 4. Client half (mod)

| Package / file                                                               | Job                                                                                                                                                         |
|------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `client/menu/MenuDocument`, `MenuComponent` (sealed), `MenuIcon`, `MenuParts` | the parsed model; colours are `0xRRGGBB` ints where `0` means "the server did not name one". `MenuParts` holds the structures that appear inside more than one component (a button alone, in `buttons` and in the footer; a row in a list and in a grid) |
| `client/menu/MenuParser`, `MenuJson`, `MenuStyles`, `MenuLimits`            | defensive Gson: never throws, skips unknown `type`s, clamps everything. `MenuJson` is the read helper, `MenuStyles` folds the wire's words onto this client's enums, `MenuLimits` is every cap in one place |
| `client/state/MenuState`                                                     | current document plus a `revision` the screen watches; `adopt` / `clear` / `reset` / `debugInject`; `openedFromSheet()` is what the back arrow consults |
| `client/screen/menu/MenuScreen`                                              | the renderer's shell: the `Screen` lifecycle, the `__close` guard, scroll and `approach`                                                                     |
| `client/screen/menu/MenuPartFactory`, `MenuPart`, `Menu{Text,Value,Control,Collection}Parts` | the document becomes a flat list of laid-out `MenuPart`s in content space, and one of four part families draws each: words, values, the things the player operates, the parts that repeat a cell |
| `client/screen/menu/MenuChrome`, `MenuGauges`, `MenuIcons`, `MenuScrollbar`, `MenuConfirmModal`, `MenuMetrics`, `MenuContext` | the card and its header, the meters, the icon sources, the draggable bar, the confirm modal, the geometry + its one hit test, and the interface a part is handed |
| `client/screen/menu/MenuTheme`                                               | every colour decision and primitive: button, badge, switch, tick/cross, chevron, gauge, hairline, small-caps heading, rounded panel, the icon sources        |
| `client/network/payload/MenuPayload`, `MenuActionPayload`                    | the two channels                                                                                                                                            |

Parser caps (`MenuLimits`): 32 sections, 96 components per section, 400 rows, 64 kv rows, 64 checks,
24 buttons, 10 tooltip lines, 12 chips / steps / panel cells, 8 details blocks, 4 hero chips;
128-char titles, 160-char labels, 2000-char text, 96-char ids, 32-char badges.

Routing lives in `CoiNetworking.handleMenu`: a `closed` document clears the state and
closes the screen quietly; otherwise the document is **adopted** and a screen is opened only if one
is not already open. That is the important part — a refresh must not recreate the screen, or
`removed()` would fire `__close` and kill the session the server is still using.

The sheet's destination cards send `ActionPayload.ofOpen(target)`, which stamps the player's
`useServerMenus` preference into the `ui` field.

### 4.1 Renderer decisions (wire unchanged, documented deviations)

1. **`maxVisible` is advisory.** The whole card scrolls; paging a list would hide rows behind a
   control the player has to find. It is parsed and ignored.
2. **`closable:false` hides the X but Esc still closes** (and sends `__close`). Honouring it for Esc
   too would let a server trap the player in a screen with no exit.
3. **`grid.columns` yields to an 18px tile floor** — a request for more columns than the card can
   hold gets fewer columns rather than a row running off the edge.
4. **`toast` is a persistent banner** for the document's life, with a transient arrival flash (epilepsy-gated), not a
   timed line — a line that disappears would reflow the card under the
   cursor.
5. **An absent `enabled` means true.** A server that must spell out `"enabled":true` on every button
   will eventually forget one.
6. **`EditBox`es are keyed by component id and outlive a rebuild.** The search box re-lays out the
   card on every keystroke; recreating the box would eat the caret. This was the one real trap in
   the design — keep it in mind during the visual redesign.

### 4.2 Player opt-out

`HudConfig.HudSettings.useServerMenus`, persisted in `config/coi_hud.json`, surfaced as a checkbox on
the **General** tab of HUD Settings (`screen.coi.menu_use_server` and `_hint`). Default **false**:
the point of the system is that a modded player never sees a chest again. When true, every sheet
button opens the plugin's original screen, byte-identical.

### 4.3 Testing without a server

**F8 → the *Menu* button** (dev environment only) feeds a hand-written sample document through the
real parser — it exercises all 13 component types, all five button styles, the confirm modal, a
toast and all three icon kinds — and opens `MenuScreen`. This is the right harness for the visual
redesign.

---

## 5. What was ported

All seven sheet destinations. Adapters live in `menu/adapter/`, plus `menu/adapter/church/`.

| Target       | Adapter                                                                                                              | What the native screen does that the chest could not                                                                                                                                                                                                                                                                                                                              |
|--------------|----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `map`        | `MapVisibilityMenu` — the reference adapter, read it first                                                           | explains what divination protection *is*, and what hiding does **not** cover (not compasses, not the tab list, not a stronger diviner)                                                                                                                                                                                                                                            |
| `mythical`   | `MythicalFormMenu`                                                                                                   | surfaces `isTransforming()` / `isReverting()` as their own state, so the screen moves dormant → transforming → awakened in place; states the 20-madness cost and the sanity drain up front; the cooldown is the disabled button's reason. Stability is recomputed from the live sequence rather than read off `MythicalForm.isComplete`, which only refreshes on transform        |
| `seat`       | `SeatChallengeMenu`                                                                                                  | `SeatInfoGUI` + `SeatConfirmGUI` + `SeatWarGUI` collapsed into one document, since the routing between them was never the player's choice: the six how-steps as prose, outcomes with the real percentages, the readiness `checklist`, the terms as a `kv` table, bid stepping (±10%), a confirm modal instead of a two-step, and read-only war standings with the withdraw action |
| `uniqueness` | `UniquenessMenu`                                                                                                     | the score wall becomes one `stat` per factor with its weight and an explanation; `UniquenessTrait.getDescription()` is shown for the first time; the forfeit keeps a confirm modal, because a one-click forfeit would regress on the chest's click-twice guard. The async score is cached 30 s and pulled in with a `refresh`                                                     |
| `honorific`  | `HonorificMenu`                                                                                                      | chat-driven line editing becomes real `input` fields with a live preview, and the validator's refusals **stay on screen** instead of scrolling away in chat                                                                                                                                                                                                                       |
| `abilities`  | `AbilityCatalogueMenu`                                                                                               | sequence sections; icons from the same item-model id `abilities_v2` sends, so a tile and the mod's picker can never disagree; the shortcut-item lore (including sequence-scaled damage) as tooltips; tile click pins/unpins the fast-access row; passives toggle in place; sealed abilities stay visible with the reason that lifts them                                          |
| `church`     | `church/ChurchMenu` + `ChurchMenus` (shared helpers) + `ChurchPeopleMenu` + `ChurchSitesMenu` + `ChurchSettingsMenu` | six role views; the roster as one searchable list plus a person card, replacing four paged selectors; sites, site detail, staff pickers, growth and regional reports; 14 settings toggles; leave and disband behind the confirm modal. `openPagedSelector` and `openTwoStepConfirm` are gone from the ported set                                                                  |

**Localization:** 349 `menu.*` keys in `src/main/resources/lang/lang.properties`, each with a real
Ukrainian translation in `lang_uk.properties` — verified as identical key sets with no collisions
against existing keys. Existing `gui.*`, `usurpation.*`, `uniqueness.*`, `honorific*` and
`mythical-form*` keys are reused wherever they already say the right thing, so a native screen and
its chest GUI cannot drift apart. Client-side there are 12 `screen.coi.menu_*` chrome keys and 92
`screen.coi.sheet_*` keys, in `en_us.json` and `uk_ua.json`.

**`%%` is a trap in this codebase:** `LocaleCache.getTranslation` does `replaceFirst("%s")` and never
`String.format`, so a doubled percent prints literally. Use a single `%`. Two pre-existing keys
(`uniqueness.gui.trait-locked`, `honorific.validation.at-line`) already have this bug.

---

## 6. What still opens a chest GUI

Each of these has a button whose description says so, in both languages. None of them is a dead end.

| Unported                                                                                           | Lands at                                                              | Why                                                                                                                              |
|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| Church artifact vault (deposit, request, approve/deny, recall, withdraw, remote slot)              | `ChurchGUI.openArtifactVault(player, church)` — straight at the vault | about 370 lines with snapshot-based staleness invalidation (`sameArtifactRequest` / `sameBorrowState`) and hand-item interaction |
| Church divine actions: gift target → gift type, blessing selector, ritual entry                    | chest root                                                            | the gift cost/burden/cooldown matrix and the anchor list are their own screen families                                           |
| Church diplomacy and war management (declare, accept, surrender, propose/accept draw, invite ally) | chest root                                                            | a state machine across two churches, about 280 lines                                                                             |
| Oversee church grounds                                                                             | chest root                                                            | live presence scan plus spectate handoff                                                                                         |
| Remote observation slots                                                                           | chest root                                                            | eligibility plus slot/leash rules                                                                                                |
| Visionary Mind-Dragon conception flow                                                              | `MindDragonFormGUI.open`                                              | the handler closes the native session first, so there is no orphan session and no refresh over a chest window                    |
| `Awareness`' configurable-passive screen, the cohort leave confirm, Gathering requests             | their InvUI windows                                                   | InvUI-native interactions                                                                                                        |
| `ChurchGUI`'s `pathway == null` core-access path (`openCoreAccess` / `openPublicCoreInfo`)         | unchanged                                                             | `MenuRouter.tryOpen` rejects a null pathway, so a placed core block keeps its chest screen unconditionally and needs no adapter  |

`ChurchGUI` has only four public entries (`buildGui`, `openArtifactVault`, `openCoreAccess`,
`openPublicCoreInfo`), and the GUI files are not edited, so anything without a public entry lands at
the chest **root** rather than at the exact sub-screen. Giving one of these a native screen means
either porting it or adding a public entry point.

---

## 7. Adding a new menu

1. Write `menu/adapter/XMenu.java` with
   `public static MenuDocument document(Player, FlexiblePathway)`. Copy `MapVisibilityMenu`'s shape.
2. Resolve every string through `LocaleCache.getTranslation` / `TranslationUtil`. Add new keys to **both**
   `lang.properties` and `lang_uk.properties`.
3. Re-check every gate inside every handler.
4. Register it: one line in `MenuRouter`'s constructor. For a screen reached from *inside* another
   menu, use `MenuChannelUtil.push(player, () -> XMenu.document(player, pathway))` instead — no
   registration needed.
5. Leave the InvUI screen untouched; it is still what unmodded players get.
6. `./gradlew build --offline`.

For a brand-new **entry point** rather than one of the seven sheet targets, the caller reads
`if (router.tryOpen(...)) return;` followed by the chest GUI it always had.

---

## 8. Known issues and open threads

- **`ChurchMenus.resolve` writes during a document build** — it reproduces `ChurchGUI.buildGui`'s
  side effect of clearing a stale `beyonder.setChurchLeaderId(null)`. Idempotent, but it now runs on
  every refresh rather than once per open. Worth moving out of the build path.
- **`AbilityCatalogueMenu` duplicates the `"fast_access_abilities"` PDC key** because
  `AbilityListGUI`'s copy is package-private; it is commented in place. If that file is ever opened
  for editing, share the constant.
- **`MenuScreen` is constructed with a `null` parent** from `handleMenu`, so Esc at the root of a
  menu returns to the game rather than to the character sheet. Deliberate for now; revisit if it
  reads wrong in play.
- The visual language is provisional — see below.

---

## Redesigning the visuals

The redesign should be confined to `screen/menu/` — `MenuScreen` and its part classes, and
`MenuTheme` — on the client, plus `screen/sheet/` (`CharacterSheetScreen`, the `Sheet*` sections and
`SheetGlyphs`) if the sheet is in scope. Nothing about the wire, the parser, the session model or any adapter needs to move: a
component is a *semantic* — a stat, a checklist, a locked button with a reason — and how it is drawn
is entirely the renderer's business.

Constraints worth keeping whatever the look becomes:

- every GUI scale, and a 1280x720 window;
- the card scrolls rather than pages (renderer decision 1);
- a disabled control must still say why (the `disabledReason` contract);
- `EditBox` identity must survive a rebuild (renderer decision 6);
- `epilepsyMode` suppresses anything that pulses or slides;
- the sheet and the menus should read as one interface — they share `CoiStyle`.

Judge it with F8 → *Menu*, which renders every component type without a server attached.
