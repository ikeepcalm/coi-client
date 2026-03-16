# Network Protocol

COI Client communicates with the server-side Paper plugin via **Fabric custom payloads** (plugin messaging channels). All channels are prefixed `coi-client:`.

## Channels

| Direction | Channel | Payload class |
|-----------|---------|---------------|
| C→S | `coi-client:use` | `AbilityUsePayload` |
| C→S | `coi-client:request` | `AbilityRequestPayload` |
| S→C | `coi-client:abilities` | `AbilitiesPayload` |
| S→C | `coi-client:cooldown` | `CooldownPayload` |
| S→C | `coi-client:effect` | `VisualEffectPayload` |

---

## C→S Payloads

### `coi-client:use`
Sent when a player activates an ability keybinding.
```
String abilityId   — e.g. "sun-9-0"
```

### `coi-client:request`
Empty payload. Sent when the player opens the Ability Binding screen (K).
Server should respond with `coi-client:abilities`.

---

## S→C Payloads

### `coi-client:abilities`
Delivers the full list of abilities available to this player.
```
String data   — semicolon-separated ability records:
                id|localizedName|englishName|category;...
```
**Example:** `sun-9-0|Пісня барда|bard-song|music;fool-1-0|Жарт|jest|trickery`

Fields:
- `id` — internal ability ID (pathway prefix before first `-` determines HUD color)
- `localizedName` — shown to the player in their locale
- `englishName` — used internally and in notifications
- `category` — grouping label (informational, currently unused by client)

### `coi-client:cooldown`
Notifies the client that an ability is on cooldown.
```
String abilityId   — must match an id from the abilities list
int    ticks       — cooldown length in game ticks (20 ticks = 1 second)
```

### `coi-client:effect`
Triggers (or stops) a client-side visual effect.
See **[docs/VISUAL_EFFECTS.md](VISUAL_EFFECTS.md)** for the full effect reference.
```
String effectId   — registered effect name, or "all"
String params     — comma-separated key=value pairs, or "stop"
```

---

## Ability ID Format

`<pathway>-<tier>-<index>`  — e.g. `sun-9-0`, `fool-1-2`, `tyrant-3-1`

The client extracts the pathway from the segment before the first `-` and uses it to color-code the HUD slot:

| Pathway | Color |
|---------|-------|
| fool | Purple |
| door | Blue |
| sun | Yellow |
| tyrant | Cyan |
| demoness | Red |
| priest | Orange |
| *(unknown)* | Gray |

---

## Codec Wire Format

All payloads use a simple sequential codec over `RegistryByteBuf`:
- Strings: `buf.writeString` / `buf.readString` (UTF-8, length-prefixed)
- Ints: `buf.writeInt` / `buf.readInt`

Register channels on the Paper side using the standard plugin messaging API with the channel identifiers listed above.
