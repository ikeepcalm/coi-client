# CLAUDE.md

The map. Detail lives in `docs/` — read the one file your task touches, not all of them.

## Project overview

COI Client is a **client-only** Minecraft Fabric mod: an ability system with a customizable HUD,
server-authored menus, pathway forms and screen effects, driven almost entirely by a Paper plugin
over Fabric custom payloads. Players bind up to 10 abilities to keys (1–6 default Z/X/C/V/B/N) and
arrange every HUD element themselves.

**Java 25 · MC 26.2 · Fabric Loader 0.19.5 · Fabric API 0.159.0+26.2**

```bash
./gradlew build        # → build/libs/coi-client-<version>.jar
./gradlew runClient    # dev client (F8 opens the effect/debug screen)
./gradlew genSources
```

## Layout

One source root, `src/main/java/dev/ua/ikeepcalm/coi/`, with `CoiClient` (the entrypoint) and
`DataGenerator` at its root and nine packages under it:

| Package    | Holds                                                                                                                                                              |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `config/`  | the three on-disk files, and the resource-pack data loader                                                                                                         |
| `network/` | the wire — channels, payloads, the capability handshake                                                                                                            |
| `domain/`  | what the mod *knows*: `ability` `beyonder` `menu` `ceremony` `effect` `form` `appearance` `gesture` `glyph`, each with `model/` and `service/` where it needs them |
| `hud/`     | what it *draws over the world* — `overlay/` `render/` `layout/`                                                                                                    |
| `screen/`  | its own screens — `ability/` `settings/` `menu/` `sheet/` `title/` `debug/` `glyph/`                                                                               |
| `ui/`      | shared chrome both `hud/` and `screen/` draw with                                                                                                                  |
| `mixin/`   | every mixin and accessor                                                                                                                                           |
| `input/`   | the keymappings                                                                                                                                                    |
| `util/`    | the logger, the defensive JSON reads, `duck/`, `hooks/`                                                                                                            |

## Critical rules

These have each cost real debugging. The doc named beside one explains why.

1. **Every HUD overlay's render gate opens with `HudGate.blocked(client, settings)`.** Forgetting
   the `HudLayout.editing()` term inside it is the one way to break the layout editor. → `docs/HUD.md`
2. **Duck interfaces live in `util/duck/`, never under `mixin/`.** The mixins.json `package` owns
   that whole subtree and Mixin refuses to let ordinary code load a class from it.
3. **`domain/form/model/VisionaryLower*` are Blockbench exports.** Re-export from the modelling
   tool; a hand edit is invisible until the next export silently reverts it. → `docs/MYTHICAL_FORMS.md`
4. **Parse every server payload through `util/JsonRead`** (or `domain/menu/service/MenuJson` for
   documents). Absent reads as the caller's default; only a non-object body is fatal.
5. **`MenuParser` must never throw.** An unknown component type is skipped and sizes are clamped —
   a newer plugin must degrade to a screen missing one row, never to no screen. → `docs/MENU_SYSTEM.md`
6. **Every payload is built from `network/payload/CoiPayloads`.** Both ends must agree on the size
   caps or the packet is rejected mid-flight. → `docs/NETWORK_PROTOCOL.md`
7. **A layout element's `bounds` and `moveTo` must stay exact inverses**, padding included, or
   elements jump when dragged. → `docs/HUD.md`
8. **Per-element scale and opacity go through `hud/HudScale` and `hud/HudOpacity`**, pushed around
   one element's draw. Every element scales about its own fill origin. → `docs/HUD.md`
9. **A mixin's injection point is load-bearing and the comment above it says why.** Do not move one
   without reading that comment — several encode a cull frustum, a pose space or an ordering.
10. **Anything that flashes honours `epilepsyMode`**, and anything that moves the camera honours
    `enableCameraShake` / `enableCinematicCamera`. Prefer a reduced variant to suppression.
    → `docs/ASCENSION_CEREMONY.md`
11. **Log through `util/CoiLog`.** No `System.out.println` survives in this repo.
12. **Every new lang key goes in both `en_us.json` and `uk_ua.json`.** → `docs/CONFIG.md`
13. **Comment the architecture and the traps; leave straightforward feature classes bare.** The
    repo sits at ~8% comment lines deliberately; essay javadoc on a 40-line class is noise.

## Documentation

| Doc                                                                                                   | Read it when                                                                                     |
|-------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md)                                                               | you need the annotated tree, the shared seams, the keybindings or the pathway colours            |
| [NETWORK_PROTOCOL.md](docs/NETWORK_PROTOCOL.md)                                                       | adding or changing anything on the wire                                                          |
| [VISUAL_EFFECTS.md](docs/VISUAL_EFFECTS.md)                                                           | adding an effect, or writing the server call that triggers one                                   |
| [HUD.md](docs/HUD.md)                                                                                 | touching any bar, the character plate, the health element, the layout editor or the HUD settings |
| [SCREENS.md](docs/SCREENS.md)                                                                         | the character sheet, the ability picker, or the first-party icon set                             |
| [MENU_SYSTEM.md](docs/MENU_SYSTEM.md)                                                                 | the server-authored menu documents, either end                                                   |
| [ASCENSION_CEREMONY.md](docs/ASCENSION_CEREMONY.md)                                                   | the six Sequence 0 set-piece effects and their mixins                                            |
| [TITLE_SCREEN.md](docs/TITLE_SCREEN.md)                                                               | the Pathway Wheel main menu or the haunting                                                      |
| [MYTHICAL_FORMS.md](docs/MYTHICAL_FORMS.md)                                                           | pathway forms, especially a partial one                                                          |
| [ARCHIVE_PRESENTATION.md](docs/ARCHIVE_PRESENTATION.md) · [ABILITY_MANUAL.md](docs/ABILITY_MANUAL.md) | the archive menu templates                                                                       |
| [CONFIG.md](docs/CONFIG.md)                                                                           | adding a setting, a config key or a lang string                                                  |
| [KNOWN_GAPS.md](docs/KNOWN_GAPS.md)                                                                   | something looks broken — check whether it is broken on purpose                                   |

Two handoffs sit at the repo root rather than in `docs/`, because they are contracts with the
plugin repo rather than descriptions of this one: `CLIENT_CEREMONY_HANDOFF.md` (what the server
asked this client to build for batch 7) and `SERVER_IMPACT_FRAMES_HANDOFF.md` (what this client
asks the server to do with `impact_frame`).
