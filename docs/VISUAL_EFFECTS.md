# Visual Effects — Server Integration Reference

The client renders screen-space visual effects triggered by the server via the `coi-client:effect` S→C payload.

## Payload Structure

```
String effectId   — effect name (see table below), or "all"
String params     — comma-separated key=value overrides, or "stop"
```

### Stopping effects
```
effectId = "<id>",  params = "stop"   → stop one specific effect
effectId = "all",   params = "stop"   → stop every active effect immediately
```

Triggering an effect that is already active replaces it (restarts from scratch).

---

## Effect Reference

### `vignette`
Darkens screen edges with a smooth gradient falloff and a slow breathing pulse. Fades in over 350ms. Good for: cursed zones, low sanity, death proximity.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Darkness and width of the vignette |
| `color` | hex RGB (no `#`) | `000000` | Vignette tint — pathway-colored vignettes work well |
| `duration` | long ms | `-1` | How long to show. `-1` = persistent until stopped |

**Examples:**
```
effect("vignette", "intensity=0.5,duration=10000")   // mild, 10 seconds
effect("vignette", "intensity=1.0")                  // maximum, persistent
effect("vignette", "intensity=0.8,color=2A0038")     // purple Fool-tinted edges
effect("vignette", "stop")                           // remove
```

---

### `heartbeat`
Vignette that pulses with a smooth lub-DUB rhythm: a constant dark edge surges on each beat, with a blood-colored inner glow and a faint whole-screen flush at the DUB peak. Good for: high tension, near-death, possessed states.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.85` | Peak darkness per beat |
| `bpm` | float | `75` | Beats per minute |
| `color` | hex RGB (no `#`) | `8A0000` | Color of the pulsing inner glow |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("heartbeat", "intensity=0.8,bpm=90,duration=15000")   // fast, anxious
effect("heartbeat", "intensity=1.0,bpm=50")                  // slow, ominous
```

---

### `cracks`
Branching fracture lines shatter inward from screen corners with an initial impact flash, each crack growing along its own length. Cracks render as glassy strokes (dark outline + bright core) over a damage vignette; with `pulse` they glow red rhythmically. Good for: reality breaking, max madness, catastrophic events.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | How far cracks extend toward center |
| `pulse` | boolean | `false` | If true, cracks glow red rhythmically |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("cracks", "intensity=0.5")                          // subtle corner cracks
effect("cracks", "intensity=1.0,pulse=true")               // max — red pulsing cracks
effect("cracks", "intensity=0.8,pulse=true,duration=8000") // timed
```

**Note:** Cracks are generated once on first render, seeded by trigger time — each trigger produces a unique pattern.

---

### `eyes`
Creepy eyes open from the darkness of the screen, stare, then close. The screen darkens while they watch; each eye has a pulsing red halo, bobs slowly, pops in slightly under-sized, and trembles just before shutting. Eyes are distributed evenly across the screen in a grid (up to 3 columns). Good for: being watched, high madness, cursed locations.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `count` | int | `2` | Number of eyes (1–6 recommended) |
| `duration` | long ms | `8000` | Total lifetime including open/close animation |

**Timing breakdown** (per eye, slightly staggered):
- 0–500ms: ambient dark glow fades in
- 500–2000ms: eyelids open
- 2000–(duration−2500)ms: staring
- last 2000ms: eyelids close + fade out

**Examples:**
```
effect("eyes", "count=1,duration=6000")    // single eye, quick
effect("eyes", "count=4,duration=12000")   // four eyes across the screen
effect("eyes", "count=6")                  // six eyes, default 8s
```

---

### `glitch`
VHS-style corruption strobing in bursts, with a tracking bar that rolls down the screen continuously between them. Artifacts flicker ~12×/second (not every frame). Good for: teleportation, reality corruption, spell side-effects, wrong-place warnings.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Artifact count, alpha, burst frequency |
| `duration` | long ms | `3000` | Auto-expires by default |

Burst artifacts (randomly mixed):
- Dark / white horizontal bands, full-width or torn partial
- RGB chromatic fringe (red above / cyan below)
- Corrupted blocks with red/cyan RGB split
- Static noise specks and faint scanlines
- Horizontal split block

**Examples:**
```
effect("glitch", "intensity=0.5,duration=2000")   // brief mild glitch on teleport
effect("glitch", "intensity=1.0,duration=5000")   // heavy sustained corruption
effect("glitch", "intensity=0.3,duration=500")    // quick flicker
```

---

---

### `bloodrain`
Tapered red streaks fall down the screen in two depth layers (dim slow background, bold fast foreground), while slow smears run down the "glass" with hanging droplets. A red edge tint frames the screen. Good for: combat, rituals, curses, death proximity.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Number and density of drops |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("bloodrain", "intensity=0.5,duration=8000")   // light drizzle
effect("bloodrain", "intensity=1.0")                 // heavy, persistent
```

---

### `frost`
Fern-like ice crystals grow inward from screen edges — thick bright trunks with side needles — over a layered icy blue-white haze. Twinkling glints appear along grown crystals. Good for: frozen spells, cold zones, ice pathway abilities.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Reach and brightness of frost |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("frost", "intensity=0.6,duration=10000")
effect("frost", "intensity=1.0")                    // heavy frost, persistent
```

---

### `whispers`
Cryptic text phrases fade in and out around the screen periphery, drifting slowly upward with a ghost double-image and candle-like flicker. Each whisper has a random size and tilt; SHOUTED phrases are tinted blood red. Spawn rate scales with intensity. Good for: high madness, haunted locations, Fool/Door pathways, forbidden knowledge.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Spawn frequency |
| `duration` | long ms | `-1` | Persistent by default |
| `text` | pipe-separated strings | *(built-in pool)* | Custom phrase pool |

Default text pool: "they can see you", "don't look back", "it knows your name", "run", "the door is open", "help me", "it's behind you", and several others.

**Examples:**
```
effect("whispers", "intensity=0.5")
effect("whispers", "intensity=0.9,text=run|almost|listen|wrong place")
effect("whispers", "duration=15000,text=the rite begins|do not resist")
```

---

### `tunnel`
Circular vignette that closes inward, leaving only a shrinking oval of visibility with a soft gradient edge. The closing is eased, and the hole breathes and drifts slightly off-center. Rendered via scanline fill (a few hundred draw calls per frame while closed). Good for: exhaustion, confusion, extreme madness, near-death.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | How far the tunnel closes (1.0 = near-blackout) |
| `duration` | long ms | `6000` | Total lifetime |
| `closeDuration` | long ms | `2000` | Time to reach full intensity; also used for fade-out |

**Examples:**
```
effect("tunnel", "intensity=0.6,duration=8000")              // moderate, 8 seconds
effect("tunnel", "intensity=0.95,duration=5000,closeDuration=3000")  // dramatic slow close
effect("tunnel", "intensity=0.4,duration=3000,closeDuration=500")    // quick flutter
```

---

### `flash`
Full-screen color wash with a sharp attack, quadratic decay, a brighter center bloom, and a brief dark afterimage as the eyes readjust. Can re-strike multiple times like lightning. Good for: ability activation feedback, pathway-colored casts, heals, explosions.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `color` | hex RGB (no `#`) | `FFFFFF` | Flash color |
| `intensity` | float 0–1 | `0.6` | Peak opacity |
| `duration` | long ms | `500` | Total fade time (afterimage adds ~60% on top) |
| `flashes` | int | `1` | Number of re-strikes within `duration`, each weaker |

Pathway color reference: `8B00FF` (Fool/purple), `0055FF` (Door/blue), `FFDD00` (Sun/yellow), `00CCCC` (Tyrant/cyan), `FF2200` (Demoness/red), `FF8800` (Priest/orange).

**Examples:**
```
effect("flash", "color=FF2200,intensity=0.7,duration=400")   // Demoness red burst
effect("flash", "color=FFFFFF,intensity=0.9,duration=600")   // Priest holy light
effect("flash", "color=00CCCC,intensity=0.5,duration=300")   // Tyrant ability hit
effect("flash", "color=000000,intensity=0.8,duration=800")   // darkness flash
effect("flash", "color=FFFFFF,intensity=0.8,duration=700,flashes=3")  // lightning triple-strike
```

---

### `impact`
World-space spell impact VFX rendered at the hit position — the MMORPG-style "your spell connected" moment. Each style composes a soft core flash, expanding shockwave rings (on the ground and facing the camera), radial light spikes, and physical spark streaks with gravity, plus style-specific extras (light pillar, ice crystals, ground cracks, implosion). Good for: spell hits, ultimates, parries, executions, divine reveals, void effects, and boss attacks.

The effect no longer flashes the whole screen and is not blocked by epilepsy mode. An optional screen component (`scope=screen` or `both`) renders a single subtle accent-colored edge pulse (~450 ms max) on the receiving player.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `style` | see presets below | `burst` | Visual preset |
| `scope` | `screen`, `world`, `both` | `world` | World VFX, subtle screen edge pulse, or both |
| `x` | double | crosshair/front of camera | World X coordinate |
| `y` | double | crosshair/front of camera | World Y coordinate |
| `z` | double | crosshair/front of camera | World Z coordinate |
| `color` | hex RGB (no `#`) | `FFFFFF` | Primary geometry/spark color |
| `accent` | hex RGB (no `#`) | `FF7A22` | Ring/spike/glow accent color — use the pathway color |
| `intensity` | float 0-1 | `0.85` | Overall opacity and spark count |
| `radius` | float | `2.0` | Approximate world radius in blocks |
| `duration` | long ms | `900` | Total lifetime (world VFX enforces a 450 ms minimum) |
| `frames` | int | — | Deprecated; accepted and ignored |

**Examples:**
```
effect("impact", "style=burst,x=120.5,y=64.0,z=-33.5,accent=FF7A22,intensity=0.9,radius=2.0,duration=900") // heavy explosive hit
effect("impact", "style=slash,x=120.5,y=64.0,z=-33.5,accent=FF2200,intensity=0.95,radius=1.8,duration=600") // sword/cut/parry
effect("impact", "style=holy,x=120.5,y=64.0,z=-33.5,accent=FFD966,intensity=0.85,radius=2.5,duration=1200") // sun/priest reveal, light pillar
effect("impact", "style=void,x=120.5,y=64.0,z=-33.5,color=111122,accent=8B00FF,intensity=1.0,radius=2.5,duration=1000") // dark implosion finisher
effect("impact", "style=pierce,x=120.5,y=64.0,z=-33.5,accent=66CCFF,intensity=0.9,radius=1.5,duration=700") // beam/lance hit
effect("impact", "style=crush,x=120.5,y=64.0,z=-33.5,accent=888888,intensity=0.95,radius=2.5,duration=800") // gravity/blunt slam with ground cracks
effect("impact", "style=ripple,x=120.5,y=64.0,z=-33.5,accent=00CCCC,intensity=0.75,radius=3.0,duration=1100") // spatial ripple, sequential rings
effect("impact", "style=fracture,x=120.5,y=64.0,z=-33.5,accent=FF2200,intensity=0.85,radius=2.0,duration=900") // reality crack, flying shards
effect("impact", "style=blood,x=120.5,y=64.0,z=-33.5,color=CC2222,accent=AA0000,intensity=0.9,radius=1.8,duration=800") // brutal splatter arcs
effect("impact", "style=frost,x=120.5,y=64.0,z=-33.5,color=DDF6FF,accent=AADDFF,intensity=0.8,radius=2.0,duration=1100") // ice crystals + glitter
effect("impact", "style=burst,scope=both,x=120.5,y=64.0,z=-33.5,accent=FF2200,duration=900") // world VFX + edge pulse on the victim
```

Presets:
- `burst`: core flash, double ground shockwave, camera ring, 10 radial spikes, ~30 gravity sparks. Good default.
- `slash`: two crossing slash arcs with white cores plus low-arc sparks — cut/parry/execution.
- `void`: implosion — rings and sparks converge inward over a dark core, then a snap of light mid-way.
- `holy`: vertical pillar of light with a white core, ground glow, and slowly rising sparks.
- `pierce`: long horizontal lance with a white core and sequential camera-facing rings.
- `crush`: flat ground flash, heavy double shockwave, glowing ground cracks, high-gravity dust.
- `ripple`: three sequential ground rings plus camera rings, minimal sparks — space-time distortion.
- `fracture`: 14 jagged uneven spikes, glowing ground cracks, wide shard-like sparks.
- `blood`: dark red glow with ~34 heavy splatter arcs that rain back down.
- `frost`: ice crystal shards growing from the ground, slow twinkling glitter sparks.

When tested from the debug screen without params, the client uses a longer `style=burst,scope=world` preview, closes the debug screen, and spawns the impact at the current crosshair hit position or about 4 blocks in front of the camera.

---

### `hallucination`
Fires a single madness-hallucination event and finishes immediately — no visual of its own. Phantom positional audio (footsteps sneaking up behind the player, whispers over the shoulder, cave ambience, doors/chests nobody touched) or a brief visual flicker (single eye / short glitch). These events also fire autonomously on the client once madness passes 25, scaling in frequency and boldness with the madness stages (25/50/75) — and up to ~2.5x more often in darkness or at night; this effect id lets the server force one at any time. At madness ≥ 75 the ability HUD itself occasionally lies for a few hundred ms (wrong cooldown number, glitched keybind, two slots trading places), and permanent madness persists locally to haunt the title screen (vignette, eye apparitions, whisper splash lines) until it is cured.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `event` | `footsteps`, `whisper`, `cave`, `block`, `flicker`, `random` | `random` | Which hallucination to fire |

**Examples:**
```
effect("hallucination", "event=footsteps")  // steps approaching from behind
effect("hallucination", "event=whisper")    // whisper just over the shoulder
effect("hallucination", "event=flicker")    // brief eye/glitch glimpse
```

Players can disable autonomous hallucinations ("Madness Hallucinations" in HUD Settings); the server-triggered pseudo-effect still works. The `flicker` variant respects epilepsy mode via the normal glitch guard.

---

## Effect Sounds

Effects with audio companions play them automatically: `heartbeat`, `whispers`, `tunnel` loop for the effect's lifetime and stop with it; `cracks`, `frost`, `glitch` play a one-shot on trigger. Volume is controlled by the "Effect Sounds" slider in HUD Settings (0% mutes all effect and hallucination audio). No server-side action is needed.

---

## Combining Effects

Effects are independent layers — multiple can be active simultaneously:

```java
// Madness level 3: vignette + heartbeat + cracks
sendEffect(player, "vignette",   "intensity=0.6");
sendEffect(player, "heartbeat",  "intensity=0.9,bpm=95");
sendEffect(player, "cracks",     "intensity=0.6,pulse=true");

// Teleportation: brief glitch
sendEffect(player, "glitch", "intensity=0.8,duration=1500");

// Big spell hit: world impact VFX plus a short pathway flash
sendEffect(player, "impact", "style=burst,x=120.5,y=64.0,z=-33.5,color=FFFFFF,accent=00CCCC,intensity=0.9,radius=2.5,duration=900");
sendEffect(player, "flash", "color=00CCCC,intensity=0.35,duration=450");

// Clear everything on respawn / sanity restore
sendEffect(player, "all", "stop");
```

---

## Paper Plugin Integration

Send effects using the standard plugin messaging API:

```java
// Helper — call from your Paper plugin
void sendEffect(Player player, String effectId, String params) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF(effectId);
    out.writeUTF(params);
    player.sendPluginMessage(plugin, "coi-client:effect", out.toByteArray());
}
```

Register the outgoing channel in `onEnable`:
```java
getServer().getMessenger().registerOutgoingPluginChannel(this, "coi-client:effect");
```

The client only processes this payload while connected (receiver is registered globally via `ClientPlayNetworking.registerGlobalReceiver`), so it is safe to send at any time after the player joins.

---

## Adding New Effects (client-side)

1. Create `effects/impl/YourEffect.java` implementing `VisualEffect`
2. Register in `EffectManager.initialize()`: `register(YourEffect.ID, YourEffect::new)`
3. Update this file with the new effect's params and examples
