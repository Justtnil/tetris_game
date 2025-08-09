# 👾 Pixel Tetris - A Jetpack Compose Classic

A fully-featured, classic block-stacking game built from the ground up using 100% Kotlin and Jetpack Compose. This project is a modern take on a nostalgic favorite, designed to demonstrate the power and simplicity of declarative UI in Android development.

## 🌟 About The Project

This isn't just another Tetris clone. It's a complete mobile game project that has evolved with features requested by the user, showcasing an iterative development process. The focus was on clean state management, responsive layouts, and creating a fun, playable experience. The entire game logic and UI are contained within a single activity, leveraging the power of composable functions and coroutines.

## ✨ Features

  * **Classic Gameplay:** Move, rotate, and drop tetrominoes to clear lines.
  * **Dynamic Scoring:** Score points for each line cleared, with a combo multiplier for clearing multiple lines at once\!
  * **Next Piece Preview:** Strategize your next move by seeing which block is coming up.
  * **Responsive Layout:** The UI adapts to place the game grid and info panel side-by-side, making efficient use of screen space.
  * **Pause/Resume:** Use the volume down key to pause and resume the action at any time.
  * **Game Over & Reset:** A "Game Over" screen displays your final score and lets you start a new game instantly.
  * **Info Popup:** An elegant "i" button provides a popup explaining the game controls.
  * **Custom App Icon:** Features a nostalgic, custom-designed pixel-art icon.

## 🛠️ Built With

This project is a testament to modern Android development practices.

  * [Kotlin](https://kotlinlang.org/) - The primary programming language.
  * [Jetpack Compose](https://developer.android.com/jetpack/compose) - For building the entire UI declaratively.
  * [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - For managing the game loop and timing without blocking the main thread.
  * [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) - For communicating UI state changes (like pausing) from the Activity to Composables.
  * [Android Studio](https://developer.android.com/studio) - The official IDE for Android development.

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

  * Android Studio (latest version recommended)
  * An Android device or emulator

### Installation

1.  **Clone the repo:**
    ```sh
    git clone https://github.com/your_username/your_repository_name.git
    ```
2.  **Open in Android Studio:**
      * Launch Android Studio.
      * Select `File > Open` and navigate to the cloned project directory.
3.  **Build and Run:**
      * Let Android Studio sync the Gradle files.
      * Click the "Run 'app'" button (the green play icon) to build and install the APK on your selected device or emulator.

## 🎮 How to Play

  * **Arrow Buttons (`←`, `→`, `↓`):** Move the falling piece left, right, or down.
  * **Rotate Button (`↻`):** Rotate the piece clockwise.
  * **Volume Down Key:** Toggle the pause menu on and off.
  * **Info Button (`i`):** Open a dialog to review the controls.

## 📄 License

Distributed under the MIT License. See `LICENSE.txt` for more information.

*(You'll need to create a file named `LICENSE.txt` and add the MIT License text if you choose to use it.)*
