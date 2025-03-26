# Mintris

A modern Tetris implementation for Android, featuring official Tetris rules, smooth animations, and responsive controls.

## Features

### Core Gameplay
- Official Tetris rules and mechanics
- 7-bag randomizer for piece distribution
- Hold piece functionality
- Ghost piece preview
- Hard drop and soft drop
- Wall kick system for rotations
- T-Spin detection and scoring

### Modern Android Features
- Optimized for Android 10+ (API 29+)
- Hardware-accelerated rendering
- High refresh rate support
- Haptic feedback
- Dark theme support
- Responsive touch controls

### Scoring System

The game features a comprehensive scoring system based on official Tetris rules:

#### Base Scores
- Single line: 40 points
- Double: 100 points
- Triple: 300 points
- Tetris (4 lines): 1200 points

#### Multipliers

1. **Level Multiplier**
   - Score is multiplied by current level
   - Level increases every 10 lines cleared

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

#### Example Score Calculation
A back-to-back T-Spin Tetris with a 3x combo at level 2:
```
Base Score: 1200
Level: 2
Combo: 3x
Back-to-Back: 1.5x
T-Spin: 6x
Final Score: 1200 * 2 * 3 * 1.5 * 6 = 64,800
```

### Controls
- Tap left/right to move piece
- Tap up to rotate
- Double tap to hard drop
- Long press to hold piece
- Swipe down for soft drop

## Technical Details

### Requirements
- Android 10 (API 29) or higher
- OpenGL ES 2.0 or higher
- 2GB RAM minimum

### Performance Optimizations
- Hardware-accelerated rendering
- Efficient collision detection
- Optimized grid operations
- Smooth animations at 60fps

### Architecture
- Written in Kotlin
- Uses Android Canvas for rendering
- Implements MVVM architecture
- Follows Material Design guidelines

## Building from Source

1. Clone the repository:
```bash
git clone https://github.com/yourusername/mintris.git
```

2. Open the project in Android Studio

3. Build and run on your device or emulator

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 