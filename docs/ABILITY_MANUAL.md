# Ability field manual and category bindings

The server catalogue opens a searchable ability index with a detail leaf for descriptions,
costs, casting modes and bindings. Narrow windows show the index and detail separately.
Pathway controls returns to the complete server document, including passive configuration,
puppets and other actions; M reopens the manual. The original menu session stays active.

Each named category and secondary action is its own entry, grouped under the spell's name.
Named categories appear first; Use current mode remains an explicit entry for dynamic bindings.
Search matches spell names and individual mode labels. In the manual, select an entry and
choose Assign to see hotkey, wheel and gesture slots with their existing bindings.

From K, clicking a slot opens a compact Choose ability list: clicking an entry immediately
binds it to that slot and returns to K. There is no second mode selector or destination step.
Hover shows the spell description. Cancel and Unbind remain available in the footer.
Up/down selects entries; Enter assigns in the picker or opens details/destinations in the
manual. Tab/Enter operate visible controls and Page Up/Down scroll manual details. Escape
backs out of assignment or compact details before closing the manual.

Passive details also show their enabled/disabled status and an Enable/Disable button. The
button invokes the matching server-authored menu row action through the existing session
and document version checks. Disabled rows show the server's reason (including always-on
passives). No local state is flipped: menu refreshes and ability-state pushes confirm changes.
The slot picker remains assignment-only. Server menu feedback is shown in the manual footer.

Use current mode preserves the existing ability binding. A named mode stores the server's
opaque category key, independently of its localized label. Fixed modes have separate HUD
cooldown timers; whole-ability bindings follow the current mode. Server ability locks apply
to every category. Selecting a mode in the manual does not cast it or change server state.
Casting a fixed binding selects that mode on the server and leaves it selected, matching
existing category controls. Costs in the detail leaf describe the current server mode.

## Protocol additions (protocol 2)

Both peers advertise `ability_categories` and `ability_manual`. The latter enables the
optional menu presentation hint `{"template":"ability_manual"}`; sections and actions remain
intact for fallback rendering. Ability metadata and category snapshots remain server-owned.

`coi-client:use_category` is C2S with exactly two VarInt-length UTF-8 strings: ability ID and
raw category key. It deliberately uses a separate channel from `coi-client:use`, so older
servers cannot interpret a fixed category as an ordinary cast. The client refuses unavailable
categories or unsupported servers. The server resolves the live allowed ability, validates
membership in `CategorizedAbility.getCategories()`, and selects/casts on its main thread
through the existing casting guards. Passive category bindings are not advertised.

`abilities_v2` entries and `state` snapshots add `selectedCategory`, `abilityLockTicks`, and
`castCategories`: an array of `{id,name,cooldownSeconds,cooldownRemainingTicks}`. State snapshots
also include `selectedCost`, `selectedDrainPerSecond` and `selectedCooldownSeconds`. Category
selection, casts (including item casts), cooldown resets and group cooldown changes refresh
these snapshots. Serialization never changes the selected category. Legacy ability-wide
cooldown packets remain for older clients; category-aware clients use the snapshots.

Bindings retain the existing string format. A fixed category is stored as
`abilityId#category=<base64url UTF-8 key> - display name`; normal and `#left_click` bindings
keep their meanings. Base64 encoding protects separators and localized category keys.

## Verification

Unit tests cover legacy bindings, opaque keys, independent timers, lock reset, malformed
metadata, presentation fallback and locale coverage. The archive preview smoke run captures
wide and compact manuals, mode selection, target selection and saved-binding feedback. It
assigns a category through UI events and reloads the configuration to verify persistence.
It also opens the actual K screen, searches and assigns category and secondary entries, and
captures the simplified picker at wide and compact GUI sizes.
The smoke world uses sample metadata; live multiplayer casts still need an in-server check.
