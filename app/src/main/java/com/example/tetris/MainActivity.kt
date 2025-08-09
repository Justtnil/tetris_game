package com.example.tetris

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlin.random.Random

// Enum to define the screens in our app
enum class Screen {
    Start,
    Game
}

// --- State Management for Pause (Shared between Activity and Composable) ---
private val _pausedState = MutableStateFlow(false)
val pausedState: StateFlow<Boolean> = _pausedState.asStateFlow()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // State to manage which screen is currently visible
            var currentScreen by remember { mutableStateOf(Screen.Start) }
            val activity = LocalContext.current as Activity

            // Navigation logic
            when (currentScreen) {
                Screen.Start -> {
                    StartScreen(
                        onPlayClick = { currentScreen = Screen.Game },
                        onExitClick = { activity.finish() }
                    )
                }
                Screen.Game -> {
                    // This handles the device's back button press
                    BackHandler {
                        currentScreen = Screen.Start
                    }
                    val isPaused by pausedState.collectAsState()
                    TetrisGame(isPaused = isPaused) {
                        _pausedState.value = it
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            _pausedState.value = !_pausedState.value
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun StartScreen(onPlayClick: () -> Unit, onExitClick: () -> Unit) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Pixel Tetris",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onPlayClick,
                modifier = Modifier.width(120.dp)
            ) {
                Text("Play", fontSize = 22.sp)
            }
            Button(
                onClick = { showInfoDialog = true },
                modifier = Modifier.width(120.dp)
            ) {
                Text("Info", fontSize = 22.sp)
            }
            Button(
                onClick = onExitClick,
                modifier = Modifier.width(120.dp)
            ) {
                Text("Exit", fontSize = 22.sp)
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Game Controls", fontSize = 24.sp) },
            text = {
                Column {
                    Text("• Arrow Buttons: Move and rotate the piece.", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("• Volume Down Button: Pause or resume the game.", fontSize = 16.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) {
                    Text("Got it!")
                }
            }
        )
    }
}


@Composable
fun TetrisGame(
    isPaused: Boolean,
    onPauseToggle: (Boolean) -> Unit
) {
    // --- Game Constants ---
    val gridWidth = 10
    val gridHeight = 20

    // --- Colors ---
    val black = Color(0, 0, 0)
    val gray = Color(128, 128, 128)
    val white = Color(255, 255, 255)
    val colors = listOf(
        Color(163, 218, 212), // I
        Color(168, 207, 255), // J
        Color(255, 213, 153), // L
        Color(255, 243, 193), // O
        Color(191, 216, 184), // S
        Color(211, 188, 230), // T
        Color(242, 182, 182)  // Z
    )

    // --- Shapes ---
    val shapes = listOf(
        listOf(listOf(1, 1, 1, 1)), // I
        listOf(listOf(1, 0, 0), listOf(1, 1, 1)), // J
        listOf(listOf(0, 0, 1), listOf(1, 1, 1)), // L
        listOf(listOf(1, 1), listOf(1, 1)), // O
        listOf(listOf(0, 1, 1), listOf(1, 1, 0)), // S
        listOf(listOf(0, 1, 0), listOf(1, 1, 1)), // T
        listOf(listOf(1, 1, 0), listOf(0, 1, 1))  // Z
    )

    // --- Game State ---
    var score by remember { mutableStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var currentPiece by remember { mutableStateOf(getNewPiece(shapes, colors, gridWidth)) }
    var nextPiece by remember { mutableStateOf(getNewPiece(shapes, colors, gridWidth)) }
    var lockedPositions by remember { mutableStateOf(mapOf<Pair<Int, Int>, Color>()) }
    var gameOver by remember { mutableStateOf(false) }

    // --- Reset Game Function ---
    fun resetGame() {
        score = 0
        lockedPositions = emptyMap()
        currentPiece = getNewPiece(shapes, colors, gridWidth)
        nextPiece = getNewPiece(shapes, colors, gridWidth)
        gameOver = false
    }


    // --- Game Loop ---
    LaunchedEffect(key1 = isPaused, key2 = gameOver) {
        if (!isPaused && !gameOver) {
            val fallSpeed = 550L
            while (isActive) {
                delay(fallSpeed)
                val newPiece = currentPiece.copy(y = currentPiece.y + 1)
                if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                    currentPiece = newPiece
                } else {
                    val shape = currentPiece.rotatedShape()
                    for (i in shape.indices) {
                        for (j in shape[i].indices) {
                            if (shape[i][j] == 1) {
                                lockedPositions = lockedPositions + (Pair(currentPiece.x + j, currentPiece.y + i) to currentPiece.color)
                            }
                        }
                    }
                    val (newLockedPositions, linesCleared) = clearRows(lockedPositions, gridWidth, gridHeight)
                    lockedPositions = newLockedPositions

                    if (linesCleared > 0) {
                        val basePoints = 10
                        score += (linesCleared * basePoints) * linesCleared
                    }

                    currentPiece = nextPiece
                    nextPiece = getNewPiece(shapes, colors, gridWidth)
                    if (!validSpace(currentPiece, lockedPositions, gridWidth, gridHeight)) {
                        gameOver = true
                    }
                }
            }
        }
    }

    // --- UI Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer to push the game area down from the status bar
            Spacer(modifier = Modifier.height(100.dp))

            // Main Row for the Game Grid and the Side Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                // Game Canvas on the left
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                ) {
                    val newBlockSize = minOf(size.width / gridWidth, size.height / gridHeight)
                    drawGrid(lockedPositions, gridWidth, gridHeight, newBlockSize, black, gray)
                    drawPiece(currentPiece, newBlockSize)
                }

                // Side Panel on the right
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.3f)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Info Button at the top
                    Button(
                        onClick = { showInfoDialog = true },
                    ) {
                        Text("i", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(150.dp))

                    // 2. Score display below the button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Score", color = Color.LightGray, fontSize = 20.sp)
                        Text(
                            text = "$score",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // This weighted spacer pushes the Next Piece Preview to the bottom
                    Spacer(Modifier.weight(10f))

                    // 3. Next Piece Preview at the bottom
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Next", color = Color.LightGray, fontSize = 20.sp)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp)
                                .aspectRatio(1f)
                        ) {
                            val previewGridSize = 4
                            val previewBlockSize = size.width / previewGridSize
                            val shape = nextPiece.rotatedShape()
                            val pieceToDraw = nextPiece.copy(
                                x = (previewGridSize - shape[0].size) / 2,
                                y = (previewGridSize - shape.size) / 2
                            )
                            drawPiece(pieceToDraw, previewBlockSize)
                        }
                    }
                }
            }

            // --- ADAPTIVE CONTROLS (SOLUTION 3) ---
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Check the available width. The 360.dp is a common breakpoint for small phones.
                if (maxWidth < 360.dp) {
                    // --- ON NARROW SCREENS: Show two rows ---
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Button(onClick = {
                                if (!isPaused && !gameOver) {
                                    val newPiece = currentPiece.copy(x = currentPiece.x - 1)
                                    if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                        currentPiece = newPiece
                                    }
                                }
                            }) { Text("←") }
                            Button(onClick = {
                                if (!isPaused && !gameOver) {
                                    val newPiece = currentPiece.copy(x = currentPiece.x + 1)
                                    if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                        currentPiece = newPiece
                                    }
                                }
                            }) { Text("→") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Button(onClick = {
                                if (!isPaused && !gameOver) {
                                    val newPiece = currentPiece.copy(y = currentPiece.y + 1)
                                    if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                        currentPiece = newPiece
                                    }
                                }
                            }) { Text("↓") }
                            Button(onClick = {
                                if (!isPaused && !gameOver) {
                                    val newPiece = currentPiece.copy(rotation = (currentPiece.rotation + 1) % 4)
                                    if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                        currentPiece = newPiece
                                    }
                                }
                            }) { Text("↻") }
                        }
                    }
                } else {
                    // --- ON WIDER SCREENS: Show a single row ---
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = {
                            if (!isPaused && !gameOver) {
                                val newPiece = currentPiece.copy(x = currentPiece.x - 1)
                                if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                    currentPiece = newPiece
                                }
                            }
                        }) { Text("←") }
                        Button(onClick = {
                            if (!isPaused && !gameOver) {
                                val newPiece = currentPiece.copy(x = currentPiece.x + 1)
                                if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                    currentPiece = newPiece
                                }
                            }
                        }) { Text("→") }
                        Button(onClick = {
                            if (!isPaused && !gameOver) {
                                val newPiece = currentPiece.copy(y = currentPiece.y + 1)
                                if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                    currentPiece = newPiece
                                }
                            }
                        }) { Text("↓") }
                        Button(onClick = {
                            if (!isPaused && !gameOver) {
                                val newPiece = currentPiece.copy(rotation = (currentPiece.rotation + 1) % 4)
                                if (validSpace(newPiece, lockedPositions, gridWidth, gridHeight)) {
                                    currentPiece = newPiece
                                }
                            }
                        }) { Text("↻") }
                    }
                }
            }
        }

        // --- Overlays ---
        if (isPaused || gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                if (gameOver) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Game Over\nFinal Score: $score",
                            color = white,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 60.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { resetGame() }) {
                            Text("Reset Game", fontSize = 20.sp)
                        }
                    }
                } else {
                    Text(
                        text = "Paused",
                        color = white,
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Game Controls", fontSize = 24.sp) },
                text = {
                    Column {
                        Text("• Arrow Buttons: Move and rotate the piece.", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("• Volume Down Button: Pause or resume the game.", fontSize = 16.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { showInfoDialog = false }) {
                        Text("Got it!")
                    }
                }
            )
        }
    }
}


// --- Data Class & Utility Functions ---

data class Piece(
    val x: Int, val y: Int, val shape: List<List<Int>>, val color: Color, val rotation: Int = 0
) {
    fun rotatedShape(): List<List<Int>> {
        var result = shape
        repeat(rotation) {
            result = result[0].indices.map { j ->
                result.indices.reversed().map { i -> result[i][j] }
            }
        }
        return result
    }
}

fun getNewPiece(shapes: List<List<List<Int>>>, colors: List<Color>, gridWidth: Int): Piece {
    val index = Random.nextInt(shapes.size)
    val shape = shapes[index]
    val startX = (gridWidth - shape[0].size) / 2
    return Piece(startX, -1, shape, colors[index])
}

fun validSpace(piece: Piece, locked: Map<Pair<Int, Int>, Color>, gridWidth: Int, gridHeight: Int): Boolean {
    val shape = piece.rotatedShape()
    for (i in shape.indices) {
        for (j in shape[i].indices) {
            if (shape[i][j] == 1) {
                val newX = piece.x + j
                val newY = piece.y + i
                if (newX < 0 || newX >= gridWidth || newY >= gridHeight) {
                    return false
                }
                if (newY >= 0 && locked.containsKey(Pair(newX, newY))) {
                    return false
                }
            }
        }
    }
    return true
}

fun clearRows(
    locked: Map<Pair<Int, Int>, Color>,
    gridWidth: Int,
    gridHeight: Int
): Pair<Map<Pair<Int, Int>, Color>, Int> {

    val fullRows = (0 until gridHeight).filter { y ->
        (0 until gridWidth).all { x -> locked.containsKey(Pair(x, y)) }
    }

    if (fullRows.isEmpty()) {
        return Pair(locked, 0)
    }

    val newLocked = locked.toMutableMap()

    fullRows.forEach { y ->
        (0 until gridWidth).forEach { x ->
            newLocked.remove(Pair(x, y))
        }
    }

    val shiftedLocked = mutableMapOf<Pair<Int, Int>, Color>()
    newLocked.entries.sortedBy { it.key.second }.forEach { (pos, color) ->
        val (x, y) = pos
        val rowsToShiftDown = fullRows.count { it > y }
        if (rowsToShiftDown > 0) {
            shiftedLocked[Pair(x, y + rowsToShiftDown)] = color
        } else {
            shiftedLocked[pos] = color
        }
    }

    return Pair(shiftedLocked, fullRows.size)
}

fun DrawScope.drawGrid(
    locked: Map<Pair<Int, Int>, Color>,
    gridWidth: Int,
    gridHeight: Int,
    blockSize: Float,
    black: Color,
    gray: Color
) {
    for (y in 0 until gridHeight) {
        for (x in 0 until gridWidth) {
            val color = locked.getOrDefault(Pair(x, y), black)
            drawRect(
                color = color,
                topLeft = Offset(x * blockSize, y * blockSize),
                size = Size(blockSize, blockSize)
            )
        }
    }
    for (i in 0..gridWidth) {
        drawLine(
            color = gray,
            start = Offset(i * blockSize, 0f),
            end = Offset(i * blockSize, gridHeight * blockSize)
        )
    }
    for (i in 0..gridHeight) {
        drawLine(
            color = gray,
            start = Offset(0f, i * blockSize),
            end = Offset(gridWidth * blockSize, i * blockSize)
        )
    }
}

fun DrawScope.drawPiece(piece: Piece, blockSize: Float) {
    val shape = piece.rotatedShape()
    for (i in shape.indices) {
        for (j in shape[i].indices) {
            if (shape[i][j] == 1 && piece.y + i >= 0) {
                val xPos = (piece.x + j) * blockSize
                val yPos = (piece.y + i) * blockSize
                drawRect(
                    color = piece.color,
                    topLeft = Offset(xPos, yPos),
                    size = Size(blockSize, blockSize)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    topLeft = Offset(xPos, yPos),
                    size = Size(blockSize, blockSize),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}