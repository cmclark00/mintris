# Mintris

A modern Tetris implementation for Android, featuring smooth animations, responsive controls, and a beautiful minimalist design.

## Features

### Core Gameplay
- Classic Tetris mechanics
- 7-bag randomizer for piece distribution
- Ghost piece preview
- Hard drop and soft drop
- T-Spin detection and scoring
- High score system with top 5 scores

### Modern Android Features
- Optimized for Android 11+ (API 30+)
- Hardware-accelerated rendering
- High refresh rate support
- Haptic feedback for piece movement and line clears
- Dark theme support
- Responsive touch controls
- Edge-to-edge display support

### Scoring System

The game features a comprehensive scoring system:

#### Base Scores
- Single line: 40 points
- Double: 100 points
- Triple: 300 points
- Tetris (4 lines): 1200 points

#### Multipliers

1. **Level Multiplier**
   - Score is multiplied by current level
   - Level increases every 10 lines cleared
   - Maximum level is 20

2. **Combo System**
   - Combo counter increases with each line clear
   - Resets if no lines are cleared
   - Multipliers:
     - 1 combo: 1.0x
     - 2 combos: 1.5x
     - 3 combos: 2.0x
     - 4 combos: 2.5x
     - 5+ combos: 3.0x

3. **Back-to-Back Tetris**
   - 50% bonus (1.5x) for consecutive Tetris clears
   - Resets if a non-Tetris clear is performed

4. **Perfect Clear**
   - 2x for single line
   - 3x for double
   - 4x for triple
   - 5x for Tetris
   - Awarded when clearing lines without leaving blocks

5. **All Clear**
   - 2x multiplier when clearing all blocks
   - Requires no blocks in grid and no pieces in play

6. **T-Spin Bonuses**
   - Single: 2x
   - Double: 4x
   - Triple: 6x

### Controls
- Swipe left/right to move piece
- Swipe down quickly for hard drop
- Swipe down slowly for soft drop
- Double tap to rotate

### Visual Effects
- Smooth piece movement animations
- Pulsing border glow during line clears
- Ghost piece preview
- Block glow effects
- Subtle grid lines
- Animated title screen with falling pieces

## Technical Details

### Requirements
- Android 11 (API 30) or higher
- Hardware acceleration support
- 2GB RAM minimum

### Performance Optimizations
- Hardware-accelerated rendering
- Efficient collision detection
- Optimized grid operations
- Smooth animations at 60fps
- Background thread for score calculations

### Architecture
- Written in Kotlin
- Uses Android Canvas for rendering
- Follows Material Design guidelines
- Implements high score persistence using SharedPreferences

## Building from Source

1. Clone the repository:
```bash
git clone https://github.com/cmclark00/mintris.git
```

2. Open the project in Android Studio

3. Build and run on your device or emulator

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 