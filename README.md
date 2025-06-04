# Circle of Imagination Client

A Minecraft Fabric mod that adds a customizable ability system to Minecraft, allowing players to bind and use special abilities through a sleek HUD interface.

[//]: # (![COI Client]&#40;https://via.placeholder.com/800x400?text=COI+Client+Screenshot&#41;)

## Features

- **Ability System**: Bind up to 3 different abilities to customizable keybindings
- **Visual HUD**: Displays your bound abilities with cooldown timers and visual effects
- **Customizable Interface**: Adjust the HUD position, size, spacing, and visual elements
- **Easy Configuration**: Simple in-game menu for binding abilities and adjusting settings

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.4
2. Download the latest version of Fabric API for 1.21.4
3. Download the latest version of COI Client
4. Place both the Fabric API and COI Client jar files in your Minecraft `mods` folder
5. Launch Minecraft with the Fabric profile

### Requirements

- Minecraft 1.21.4
- Fabric Loader 0.16.10+
- Fabric API 0.119.3+1.21.4

## Usage

### Keybindings

The mod comes with the following default keybindings:

- **Z**: Use ability in slot 1
- **X**: Use ability in slot 2
- **C**: Use ability in slot 3
- **K**: Open ability binding menu

You can change these keybindings in the Minecraft controls settings.

### Binding Abilities

1. Press **K** to open the ability binding menu
2. Select an ability from the dropdown for each slot
3. Click "Save" to apply your changes
4. Use the abilities in-game with the corresponding keybindings

### HUD Display

The ability HUD displays:
- Icons for each bound ability
- Cooldown timers when abilities are on cooldown
- Keybinding indicators
- Ability names (optional)
- Visual effects when abilities are ready (optional)

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

Access the HUD settings screen through the game options menu. This screen provides:

- Sliders and input fields for precise adjustment of all HUD parameters
- Toggles for visual elements (keybinds, ability names, glow effects)
- Built-in presets for quick configuration:
  - **Default**: Standard balanced layout
  - **Compact**: Smaller, minimalist design for less screen space
  - **Large**: Bigger elements for better visibility
  - **Minimal**: Smallest possible footprint with minimal visual elements

You can adjust these settings in-game through the HUD settings screen or manually edit the configuration files.

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

## Support

If you encounter any issues or have suggestions, please open an issue on the project repository.
