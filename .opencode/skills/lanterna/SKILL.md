---
name: lanterna
description: >
  Use this skill whenever the user is working with the Lanterna Java library for
  building terminal/console-based text UIs (TUIs) in Kotlin. Triggers include: any
  mention of "lanterna", terminal UI, console UI, TUI in Kotlin, text-based interface,
  curses-style terminal UI, or requests to build interactive terminal applications in
  Kotlin. Also use when the user needs screen-buffered rendering, keyboard/mouse input
  in a terminal, colored text output with ANSI/RGB colors, or component-based GUI
  widgets (buttons, text boxes, windows, panels) running inside a terminal. Always use
  this skill before writing any Lanterna code — it defines the correct APIs, layer
  architecture, and idiomatic Kotlin patterns the model must follow.
model_hint: Qwen3-27B
library_version: "3.1.2"
language: Kotlin
---

# Lanterna Text Graphics Library — Kotlin Coding Reference

Lanterna (`com.googlecode.lanterna`) is a Java library usable from Kotlin with no
wrappers needed. It exposes **three independent layers** of increasing abstraction.
All code below is idiomatic Kotlin: `val`/`var`, `use {}`, `when`, string templates,
no `new`, lambdas instead of SAM types, and `listOf()` in place of `List.of()`.

---

## 1. Dependency

```kotlin
// build.gradle.kts
implementation("com.googlecode.lanterna:lanterna:3.1.2")
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>com.googlecode.lanterna</groupId>
  <artifactId>lanterna</artifactId>
  <version>3.1.2</version>
</dependency>
```

---

## 2. Architecture — Three Layers

| Layer | Package prefix | Abstraction | Use when |
|-------|---------------|-------------|----------|
| **Terminal** | `com.googlecode.lanterna.terminal` | Low — direct TTY | Raw I/O, escape codes, special effects |
| **Screen** | `com.googlecode.lanterna.screen` | Mid — double-buffered | Custom drawing, games, full-screen apps |
| **GUI** | `com.googlecode.lanterna.gui2` | High — component tree | Standard widgets, dialogs, forms |

Choose the **highest layer that satisfies the requirements**. Layers are composable:
a GUI sits on a Screen which sits on a Terminal.

---

## 3. Core Types (used across all layers)

```kotlin
import com.googlecode.lanterna.TerminalSize        // (columns, rows)
import com.googlecode.lanterna.TerminalPosition    // (column, row) — 0-indexed
import com.googlecode.lanterna.TextCharacter       // single styled character
import com.googlecode.lanterna.TextColor           // color abstraction
import com.googlecode.lanterna.SGR                 // style flags (BOLD, ITALIC, …)
import com.googlecode.lanterna.graphics.TextGraphics // drawing canvas
import com.googlecode.lanterna.input.KeyStroke     // keyboard event
import com.googlecode.lanterna.input.KeyType       // key enum (ArrowUp, Enter, …)
```

### TerminalSize & TerminalPosition

```kotlin
val size = TerminalSize(80, 24)          // 80 cols, 24 rows
val cols = size.columns
val rows = size.rows

val pos     = TerminalPosition(10, 5)   // col 10, row 5
val shifted = pos.withRelativeColumn(3).withRelativeRow(-1)
```

### TextColor

```kotlin
// Named ANSI colors (always available)
val red:   TextColor = TextColor.ANSI.RED
val reset: TextColor = TextColor.ANSI.DEFAULT

// All 16 ANSI variants:
// BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE
// + BRIGHT_ variants: BRIGHT_BLACK … BRIGHT_WHITE

// 256-color indexed palette
val indexed = TextColor.Indexed(196)    // bright red in 256-palette

// True-color RGB (terminal must support it)
val orange = TextColor.RGB(255, 128, 0)
```

### SGR (style attributes)

```kotlin
val styles = arrayOf(SGR.BOLD, SGR.UNDERLINE)
// Others: ITALIC, REVERSE, STRIKETHROUGH, BLINK, FAINT, BORDERED
```

### TextCharacter

```kotlin
val ch = TextCharacter(
    'A',
    TextColor.ANSI.WHITE,   // foreground
    TextColor.ANSI.BLUE,    // background
    SGR.BOLD
)
```

---

## 4. Terminal Layer

Entry point: `DefaultTerminalFactory`.

```kotlin
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

val factory = DefaultTerminalFactory().apply {
    setInitialTerminalSize(TerminalSize(120, 40))  // optional; useful for headless
}
```

**Always close the terminal.** `Terminal` implements `Closeable`, so use `use {}`:

```kotlin
factory.createTerminal().use { terminal ->
    terminal.enterPrivateMode()         // saves cursor, clears screen
    terminal.clearScreen()
    terminal.setCursorPosition(5, 3)    // col 5, row 3

    terminal.setForegroundColor(TextColor.ANSI.GREEN)
    terminal.setBackgroundColor(TextColor.ANSI.BLACK)
    terminal.enableSGR(SGR.BOLD)
    terminal.putString("Hello, Lanterna!")

    terminal.resetColorAndSGR()
    terminal.flush()                    // must call — sends buffered bytes

    val key = terminal.readInput()      // blocks until keypress

    terminal.exitPrivateMode()
}
```

### Key Terminal methods

```kotlin
terminal.terminalSize                           // property → TerminalSize
terminal.addResizeListener { _, newSize -> }    // trailing lambda
terminal.bell()                                 // audible beep
terminal.setCursorVisible(false)
terminal.flush()                                // required after every draw
```

---

## 5. Screen Layer

`TerminalScreen` wraps a `Terminal` with **double-buffered** rendering: changes
accumulate in a back buffer; `refresh()` computes a diff and sends minimal escape
sequences.

```kotlin
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen

val terminal = DefaultTerminalFactory().createTerminal()
val screen: Screen = TerminalScreen(terminal)

screen.startScreen()
screen.cursorPosition = null            // hide cursor

try {
    while (true) {
        screen.clear()

        val g = screen.newTextGraphics().apply {
            foregroundColor = TextColor.ANSI.YELLOW
            backgroundColor = TextColor.ANSI.BLACK
        }
        g.putString(2, 1, "Press Q to quit")

        val sz = screen.terminalSize
        g.drawRectangle(TerminalPosition(0, 0), sz, '*')

        screen.refresh()                // back buffer → visible

        val key = screen.pollInput()    // non-blocking; null if no input
        if (key?.character == 'q') break
        Thread.sleep(50)
    }
} finally {
    screen.stopScreen()
}
```

### Key Screen methods

```kotlin
screen.startScreen()
screen.stopScreen()
screen.clear()
screen.refresh()                            // diff-based update
screen.refresh(Screen.RefreshType.COMPLETE) // force full repaint
screen.terminalSize                         // property → TerminalSize
screen.readInput()                          // blocking key read → KeyStroke
screen.pollInput()                          // non-blocking → KeyStroke?
screen.newTextGraphics()                    // → TextGraphics (full screen)
screen.cursorPosition = null                // null = hide cursor
screen.doResizeIfNecessary()                // → TerminalSize? (null = no resize)
```

### TextGraphics drawing API

```kotlin
val g = screen.newTextGraphics()

// Colors and style (Kotlin properties, not Java setters)
g.foregroundColor = TextColor.ANSI.WHITE
g.backgroundColor = TextColor.ANSI.BLUE
g.enableModifiers(SGR.BOLD)
g.disableModifiers(SGR.BOLD)

// Text
g.putString(col, row, "text")
g.putString(TerminalPosition(col, row), "text")

// Primitives
g.drawLine(pos1, pos2, '─')
g.drawRectangle(topLeft, size, '+')
g.fillRectangle(topLeft, size, ' ')     // fill with spaces = erase region
g.drawTriangle(p1, p2, p3, '*')
g.fillTriangle(p1, p2, p3, '#')

// Single cell
g.setCharacter(col, row, textCharacter)
```

---

## 6. GUI Layer

The GUI layer provides a Swing-like component tree designed for terminals.

### Setup

```kotlin
import com.googlecode.lanterna.gui2.*

val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
screen.startScreen()

val gui = MultiWindowTextGUI(
    screen,
    DefaultWindowManager(),
    EmptySpace(TextColor.ANSI.BLACK)    // background fill character
)
```

### Windows

```kotlin
val window = BasicWindow("My App").apply {
    hints = listOf(Window.Hint.CENTERED)
    // Other hints: FULL_SCREEN, FIXED_SIZE, NO_DECORATIONS, NO_POST_RENDERING
    component = somePanel
}

gui.addWindowAndWait(window)            // blocks until window.close()

// Non-blocking alternative:
gui.addWindow(window)
// … later:
gui.waitForWindowToClose(window)
```

### Panels and Layouts

```kotlin
// Vertical / horizontal stacks
val vPanel = Panel(LinearLayout(Direction.VERTICAL))
val hPanel = Panel(LinearLayout(Direction.HORIZONTAL))

// Grid (2 columns)
val grid = Panel(GridLayout(2)).apply {
    (layoutManager as GridLayout).apply {
        horizontalSpacing = 1
        verticalSpacing   = 0
    }
}

// Border layout (TOP, CENTER, BOTTOM, LEFT, RIGHT)
val border = Panel(BorderLayout()).apply {
    addComponent(titleLabel, BorderLayout.Location.TOP)
    addComponent(content,    BorderLayout.Location.CENTER)
    addComponent(statusBar,  BorderLayout.Location.BOTTOM)
}
```

### Component layout hints

```kotlin
// GridLayout data — attach when adding to a grid panel
component.setLayoutData(
    GridLayout.createLayoutData(
        GridLayout.Alignment.FILL,    // horizontal: FILL, CENTER, BEGINNING, END
        GridLayout.Alignment.CENTER,  // vertical
        true,   // grab extra horizontal space
        false,  // grab extra vertical space
        2,      // colspan
        1       // rowspan
    )
)

// LinearLayout fill
component.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill))
```

### Standard Components

#### Label

```kotlin
val label = Label("Status: OK").apply {
    foregroundColor = TextColor.ANSI.GREEN
}
label.text = "Status: ERROR"            // update at runtime
```

#### Button

```kotlin
val btn = Button("Click Me") {          // Runnable as trailing lambda
    label.text = "Clicked!"
}
btn.isEnabled = false                   // grey out
```

#### TextBox (single & multi-line)

```kotlin
// Single line
val input = TextBox(TerminalSize(30, 1)).apply { text = "default" }
val value: String = input.text

// Multi-line
val multiline = TextBox(TerminalSize(40, 5), TextBox.Style.MULTI_LINE)

// Password mask
val password = TextBox(TerminalSize(20, 1)).apply { setMask('*') }
```

#### CheckBox

```kotlin
val check = CheckBox("Enable feature").apply { isChecked = true }
val on: Boolean = check.isChecked
check.addListener { nowChecked -> /* onChange */ }
```

#### RadioBoxList

```kotlin
val radio = RadioBoxList<String>(TerminalSize(20, 4)).apply {
    addItem("Option A")
    addItem("Option B")
    addItem("Option C")
}
val idx: Int     = radio.checkedItemIndex   // -1 if none
val sel: String? = radio.checkedItem
```

#### ComboBox

```kotlin
val combo = ComboBox("Item 1", "Item 2", "Item 3").apply {
    addItem("Item 4")
    selectedIndex = 0
}
val selected: String = combo.selectedItem
combo.addListener { prev, now, byUser -> /* onChange */ }
```

#### ActionListBox (menu-like list)

```kotlin
val list = ActionListBox(TerminalSize(30, 5)).apply {
    addItem("Open file") { openFile() }
    addItem("Save file") { saveFile() }
    addItem("Quit")      { window.close() }
}
```

#### ProgressBar

```kotlin
val bar = ProgressBar(0, 100, 30)       // min, max, preferredWidth
bar.value = 42
val current: Int = bar.value
```

#### Table

```kotlin
val table = Table<String>("Name", "Age", "City").apply {
    tableModel.addRow("Alice", "30", "Helsinki")
    tableModel.addRow("Bob",   "25", "Tampere")
    setSelectAction {
        val row  = selectedRow
        val name = tableModel.getRow(row)[0]
    }
}
```

#### AnimatedLabel

```kotlin
val spinner = AnimatedLabel.createClassicSpinningLine()

// Custom frames:
val anim = AnimatedLabel("Frame0").apply {
    addFrame("Frame1")
    addFrame("Frame2")
    startAnimation(200)                 // ms per frame
}
anim.stopAnimation()
```

#### Separator

```kotlin
panel.addComponent(Separator(Direction.HORIZONTAL))
```

### Dialogs (built-in)

```kotlin
import com.googlecode.lanterna.gui2.dialogs.*

// Message box
MessageDialog.showMessageDialog(gui, "Title", "Message text", MessageDialogButton.OK)

// Yes/No confirmation
val result = MessageDialog.showMessageDialog(
    gui, "Confirm", "Delete file?",
    MessageDialogButton.Yes, MessageDialogButton.No
)
val confirmed = result == MessageDialogButton.Yes

// Text input
val input: String? = TextInputDialog.showDialog(gui, "Input", "Enter name:", "default")

// File picker
val file = FileDialogBuilder()
    .setTitle("Open File")
    .setDescription("Choose a file")
    .setActionLabel("Open")
    .build()
    .showDialog(gui)
```

---

## 7. Input Handling (all layers)

```kotlin
val key = screen.readInput()            // or terminal.readInput()

when (key.keyType) {
    KeyType.Character -> {
        val c     = key.character       // Char — printable character
        val ctrl  = key.isCtrlDown
        val alt   = key.isAltDown
        val shift = key.isShiftDown
    }
    KeyType.Enter      -> { }
    KeyType.Escape     -> { }
    KeyType.Backspace  -> { }
    KeyType.Delete     -> { }
    KeyType.ArrowUp    -> { }
    KeyType.ArrowDown  -> { }
    KeyType.ArrowLeft  -> { }
    KeyType.ArrowRight -> { }
    KeyType.F1, KeyType.F2  -> { }     // F3–F12 follow same pattern
    KeyType.Tab        -> { }
    KeyType.ReverseTab -> { }           // Shift+Tab
    KeyType.PageUp     -> { }
    KeyType.PageDown   -> { }
    KeyType.Home       -> { }
    KeyType.End        -> { }
    KeyType.Insert     -> { }
    KeyType.EOF        -> { }           // terminal closed / Ctrl+D
    KeyType.MouseEvent -> {
        val mouse = key as MouseAction
        val type  = mouse.actionType    // CLICK_DOWN, CLICK_RELEASE, SCROLL_UP, …
        val pos   = mouse.position      // TerminalPosition
    }
    else -> { }
}
```

Enable mouse reporting on the Terminal (optional):

```kotlin
terminal.setMouseCaptureMode(MouseCaptureMode.CLICK)
// Modes: CLICK, CLICK_RELEASE, DRAG, MOVE
```

---

## 8. Resize Handling

```kotlin
// Screen layer — call at the top of every frame
val newSize = screen.doResizeIfNecessary()  // TerminalSize? — null = no resize
if (newSize != null) {
    screen.clear()
    redraw(screen, newSize)
}

// Terminal layer — listener
terminal.addResizeListener { _, size ->
    // called on resize; size: TerminalSize
}
```

---

## 9. Threading Rules

- **GUI layer** event loop runs on the thread that calls `addWindowAndWait`.
  Mutate GUI components **inside action callbacks** or dispatch explicitly:
  ```kotlin
  gui.guiThread.invokeLater { label.text = "updated" }
  // Blocking variant:
  gui.guiThread.invokeAndWait { label.text = "updated" }
  ```
- **Screen layer** has no built-in thread safety. Use `@Synchronized` or a
  `Mutex` if multiple coroutines/threads call `screen.refresh()` or `screen.clear()`.
- **Terminal layer** — `flush()` must be called from one thread at a time.

---

## 10. SwingTerminal / AWTTerminal (desktop / IDE mode)

When running outside a real TTY (e.g., IntelliJ run), Lanterna auto-creates a Swing
window. Force it explicitly:

```kotlin
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame

val frame: SwingTerminalFrame = DefaultTerminalFactory()
    .setTerminalEmulatorTitle("My App")
    .setInitialTerminalSize(TerminalSize(120, 40))
    .createSwingTerminal()

frame.isVisible = true
// frame implements Terminal — all Terminal APIs apply identically
```

`AWTTerminalFrame` is available for AWT. Swap via `DefaultTerminalFactory` — no
other code changes needed.

---

## 11. Common Patterns & Pitfalls

### Pattern: Full-screen draw loop (Screen layer)

```kotlin
val screen: Screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
screen.startScreen()
screen.cursorPosition = null

try {
    var running = true
    while (running) {
        val size = screen.doResizeIfNecessary() ?: screen.terminalSize

        screen.clear()
        screen.newTextGraphics().apply {
            putString(2, 0, "Size: ${size.columns}x${size.rows}")
            // draw more content here…
        }
        screen.refresh()

        val key = screen.pollInput()
        if (key?.keyType == KeyType.Escape) running = false
        Thread.sleep(16)                // ~60 fps cap
    }
} finally {
    screen.stopScreen()
}
```

### Pattern: Minimal GUI app

```kotlin
val screen: Screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
screen.startScreen()

val gui = MultiWindowTextGUI(screen)

val panel = Panel(LinearLayout(Direction.VERTICAL)).apply {
    addComponent(Label("Hello from GUI layer"))
    addComponent(Button("Quit") { gui.windows.forEach { it.close() } })
}

val win = BasicWindow("Demo").apply {
    hints = listOf(Window.Hint.CENTERED)
    component = panel
}

gui.addWindowAndWait(win)               // blocks until win.close() called
screen.stopScreen()
```

### Pitfall: forgetting `flush()` on Terminal layer

Every direct terminal write must end with `terminal.flush()`. The Screen and GUI
layers call `flush()` internally inside `screen.refresh()`.

### Pitfall: calling `screen.clear()` before `doResizeIfNecessary()`

Always check for resize **before** clearing. `doResizeIfNecessary()` reallocates
the back buffer to the new size; clearing a stale-sized buffer produces glitches.

### Pitfall: mixing layers incorrectly

Do **not** call `terminal.putString(…)` after a `TerminalScreen` has been started —
the Screen owns the terminal buffer. Use `screen.newTextGraphics()` instead.

### Pitfall: component preferred size in GUI layer

Layouts query `component.preferredSize`. Override it with:
```kotlin
component.preferredSize = TerminalSize(40, 1)
```

### Pitfall: `null` safety on `pollInput()`

`pollInput()` returns `KeyStroke?`. Use safe-call or Elvis to avoid NPE:
```kotlin
val key = screen.pollInput() ?: continue
// or
screen.pollInput()?.let { key -> handleKey(key) }
```

---

## 12. Key Imports Quick Reference

```kotlin
// Core types
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.SGR

// Terminal
import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame

// Screen
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.graphics.TextGraphics

// Input
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.input.MouseAction
import com.googlecode.lanterna.input.MouseActionType
import com.googlecode.lanterna.input.MouseCaptureMode

// GUI
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.dialogs.*
import com.googlecode.lanterna.gui2.table.Table

// Layouts
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.gui2.Direction
```
