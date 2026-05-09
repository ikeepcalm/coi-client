# Circle of Imagination Client

A Minecraft Fabric mod that adds a customizable ability system to Minecraft, allowing players to bind and use special abilities through a sleek HUD interface.

[//]: # (![COI Client]&#40;https://via.placeholder.com/800x400?text=COI+Client+Screenshot&#41;)

## Features

- **Ability System**: Bind up to 6 different abilities to customizable keybindings (Z, X, C, V, B, N)
- **Ability Wheel**: Quick access to abilities via an interactive wheel (G)
- **Visual HUD**: Displays your bound abilities with cooldown timers and visual effects
- **Visual Effects System**: Supports server-triggered visual effects like vignettes, heartbeats, and screen cracks
- **Customizable Interface**: Adjust the HUD position, size, spacing, and visual elements
- **Easy Configuration**: Simple in-game menu for binding abilities and adjusting settings

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1+
2. Download the latest version of Fabric API
3. Download the latest version of COI Client
4. Place both the Fabric API and COI Client jar files in your Minecraft `mods` folder
5. Launch Minecraft with the Fabric profile

### Requirements

- Minecraft 1.21.1+
- Fabric Loader 0.16.10+
- Fabric API 0.119.3+

## Usage

### Keybindings

The mod comes with the following default keybindings:

- **Z, X, C, V, B, N**: Use abilities in slots 1–6
- **G**: Open Ability Wheel
- **K**: Open Ability Binding menu

You can change these keybindings in the Minecraft controls settings under the "Circle of Imagination" category.

### Binding Abilities

1. Press **K** to open the ability binding menu
2. Select an ability from the dropdown for each slot
3. Access HUD settings via the button in the binding menu to customize your interface
4. Click "Save" to apply your changes
5. Use the abilities in-game with the corresponding keybindings or the Ability Wheel (G)

### Visual Effects

The client supports various visual effects triggered by the server:
- **Vignette & Heartbeat**: Health and status indicators
- **Cracks & Glitch**: Damage and sanity effects
- **Blood Rain & Frost**: Environmental and status effects
- **Whispers & Tunnel**: Mental state effects

## Configuration

The mod creates two configuration files in your Minecraft config directory:

### Ability Bindings (`coi_abilities.json`)

Stores which abilities are bound to each slot.

### HUD Settings (`coi_hud.json`)

Allows customization of the HUD display:

| Setting | Description | Default |
|---------|-------------|---------|
| `enabled` | Toggle HUD visibility | `true` |
| `hudX` | Horizontal position | `20` |
| `hudYOffset` | Vertical position | `80` |
| `slotSize` | Size of ability slots | `50` |
| `slotSpacing` | Spacing between slots | `60` |
| `showKeybinds` | Show keybinding indicators | `true` |
| `showAbilityNames` | Show ability names | `true` |
| `showGlowEffect` | Show glow effect when abilities are ready | `true` |
| `hudScale` | Scale of the entire HUD | `1.0` |

#### HUD Settings Screen

Access the HUD settings screen through the button in the **Ability Binding (K)** menu. This screen provides:

- Sliders and input fields for precise adjustment of all HUD parameters
- Toggles for visual elements (keybinds, ability names, glow effects)
- Built-in presets for quick configuration (Default, Compact, Large, Minimal)

## Development

### Building from Source

1. Clone the repository
2. Run `./gradlew build` to build the mod
3. The compiled jar will be in `build/libs/`

## License

Copyright (c) 2025 ikeepcaIm. All rights reserved.

## Credits

- Developed by ikeepcaIm
- Built with [Fabric](https://fabricmc.net/)
- Special thanks to the Minecraft modding community
