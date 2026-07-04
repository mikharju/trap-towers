---
name: terminaluitesting
version: "1.0"
tags: [testing, terminal, tui, lanterna, tmux, screenshot, headless]
description: |
  Skill for running terminal UI programs (Lanterna/TUI) inside a dev container,
  sending key inputs via tmux, and capturing text screenshots to test the visual
  state of the terminal interface. No virtual display or GPU is needed.

  Activate this skill when the user asks:
  - "Test if X looks correct / works visually" (and the program is a TUI)
  - "Check what the screen looks like when I do X"
  - "Run the game and take a screenshot" (terminal/Lanterna mode)
  - "Verify the UI / layout / rendering of X" (terminal UI)
  - "Does X appear on screen after doing Y?"
  - Any request to observe terminal program behavior that requires seeing the output

  Prefer this skill over the graphical screenshot skill when the program uses
  a terminal UI (Lanterna), the user mentions TUI or terminal mode, or the
  program renders characters rather than pixels.

  Do NOT activate for pure logic tests, compilation checks, or graphical
  programs that render to a window. Use the graphical screenshottesting skill
  for LibGDX or other windowed programs.

  Think step by step before writing a test script: identify what inputs are
  needed, what state the terminal should be in before capturing, and what
  text or UI elements to look for in the result.
language: bash
---

## Terminal UI Testing (Lanterna)

Use this approach when the program under test uses a terminal UI (Lanterna) instead of
a graphical window. No virtual display or OpenGL is needed. Only `tmux` is required
inside the sbx container.

### When to Use Terminal Testing Instead of Graphical Testing

- The program is launched with the Lanterna backend
- The user says "terminal mode", "TUI", or "Lanterna"
- The program runs in a terminal and renders characters rather than pixels
- You want text output instead of a bitmap screenshot

---

### How to Start the Program in tmux

Create a detached tmux session with a fixed terminal size. The size should match
what the game expects — 120x40 is a safe default:

```bash
tmux new-session -d -s gametest -x 120 -y 40 "java -jar build/libs/mygame.jar"
sleep 3  # wait for the game to reach an interactive state
```

If the game is launched via Gradle:

```bash
tmux new-session -d -s gametest -x 120 -y 40 "./gradlew run"
sleep 5  # Gradle startup takes longer
```

Check that the session started and the game is running:

```bash
tmux list-sessions
tmux capture-pane -t gametest -p  # print current screen contents
```

---

### How to Send Key Inputs

Use `tmux send-keys`. The second argument is the key, the third (`""`) sends it
immediately without an extra Enter:

```bash
# Press a single character key
tmux send-keys -t gametest "w" ""

# Press Enter
tmux send-keys -t gametest "" ""

# Press Escape
tmux send-keys -t gametest "Escape" ""

# Press arrow keys
tmux send-keys -t gametest "Up" ""
tmux send-keys -t gametest "Down" ""
tmux send-keys -t gametest "Left" ""
tmux send-keys -t gametest "Right" ""

# Press Space
tmux send-keys -t gametest " " ""

# Press F12 (for in-app screenshot if implemented)
tmux send-keys -t gametest "F12" ""

# Type a string of characters
tmux send-keys -t gametest "hello" ""
```

Always add a short `sleep` after each input to give the game time to react before
sending the next one:

```bash
tmux send-keys -t gametest "w" ""
sleep 0.3
tmux send-keys -t gametest " " ""
sleep 0.5
```

---

### How to Capture the Screen

`tmux capture-pane` reads the current terminal contents as plain text. Save it to
the screenshots directory:

```bash
SCREENSHOT_DIR="<project-root>/screenshots"
mkdir -p "$SCREENSHOT_DIR"

tmux capture-pane -t gametest -p > "$SCREENSHOT_DIR/screen.txt"
```

The `-p` flag prints to stdout instead of the tmux buffer, making it easy to
redirect to a file. The output is the exact characters currently visible in the
terminal, one line per row.

To include terminal history above the current view (scrollback):

```bash
tmux capture-pane -t gametest -p -S -100 > "$SCREENSHOT_DIR/screen_with_history.txt"
```

---

### Where to Find Screenshots

Text screenshots are saved to the same directory as graphical screenshots:

```
<project-root>/screenshots/
```

List the most recent:

```bash
ls -lt <project-root>/screenshots/*.txt | head -5
```

Read the latest capture:

```bash
cat <project-root>/screenshots/screen.txt
```

The content is plain text — read it directly. Look for expected strings, UI elements,
or game state indicators that should be present on screen.

---

### How to Clean Up

Kill the tmux session when the test is done:

```bash
tmux kill-session -t gametest 2>/dev/null
```

If a previous test run left a session behind, kill it before starting a new one:

```bash
tmux kill-session -t gametest 2>/dev/null || true
```

---

### Full Terminal Test Script Template

```bash
#!/bin/bash
set -e

PROJECT_ROOT="$(pwd)"
SCREENSHOT_DIR="$PROJECT_ROOT/screenshots"
SESSION="gametest"

mkdir -p "$SCREENSHOT_DIR"

# Kill any leftover session from a previous run
tmux kill-session -t "$SESSION" 2>/dev/null || true

# Start game in detached tmux session
tmux new-session -d -s "$SESSION" -x 120 -y 40 "./gradlew run"
sleep 5  # wait for game to reach interactive state

# --- Inputs go here ---
tmux send-keys -t "$SESSION" " " ""   # example: press space to start
sleep 0.5
tmux send-keys -t "$SESSION" "Right" ""
sleep 0.3
tmux send-keys -t "$SESSION" "Right" ""
sleep 0.3
# ----------------------

# Capture terminal state
tmux capture-pane -t "$SESSION" -p > "$SCREENSHOT_DIR/screen.txt"

# Clean up
tmux kill-session -t "$SESSION" 2>/dev/null || true

# Report
echo "Screenshot saved to:"
echo "$SCREENSHOT_DIR/screen.txt"
echo ""
echo "Contents:"
cat "$SCREENSHOT_DIR/screen.txt"
```

---

### After the Screenshot Is Taken

1. Read `<project-root>/screenshots/screen.txt`
2. Look for expected text, labels, UI elements, or game state that should be visible
3. Report what is present and whether it matches the expected behavior
4. Report pass or fail based on what the text content shows

Since the output is plain text, assertions are straightforward:
- Is a specific word or phrase visible?
- Is a UI element (menu item, score, status) present at the expected position?
- Has the screen changed after an input compared to a previous capture?
