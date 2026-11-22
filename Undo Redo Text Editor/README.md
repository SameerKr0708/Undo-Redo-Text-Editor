# Undo/Redo Text Editor (Java, Swing)

Tech stack: **Stack** and **Linked List** (the stack is built on a singly linked list of nodes).

## Files
- `Stack.java`      - LIFO stack on a linked list (push, pop, peek, isEmpty)
- `TextEditor.java` - undo/redo logic using two stacks (undoStack, redoStack)
- `EditorGUI.java`  - minimalist Swing interface (run this one)

## Run
    javac *.java
    java EditorGUI

## Use
- Type in the editor. A short pause while typing counts as one undo step.
- Click **Undo** / **Redo** or use Ctrl+Z / Ctrl+Y.
- The side panels show the top of the undo and redo stacks live.
- **Clear** empties the text and the history.
