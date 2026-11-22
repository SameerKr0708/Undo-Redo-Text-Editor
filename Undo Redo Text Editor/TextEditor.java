/**
 * Core logic of the Undo/Redo Text Editor.
 * Keeps the current text plus two stacks:
 *   undoStack - earlier versions of the text
 *   redoStack - versions that were undone and can be restored
 */
public class TextEditor {

    private String text = "";
    private final Stack undoStack = new Stack();
    private final Stack redoStack = new Stack();

    /** Saves the current text for undo, sets the new text and clears redo history. */
    public void type(String newText) {
        if (newText.equals(text)) {
            return;                       // nothing changed
        }
        undoStack.push(text);
        text = newText;
        redoStack.clear();
    }

    /** Moves the current text to the redo stack and restores the previous text. */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        redoStack.push(text);
        text = undoStack.pop();
        return true;
    }

    /** Moves the current text back to the undo stack and restores the undone text. */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        undoStack.push(text);
        text = redoStack.pop();
        return true;
    }

    /** Prints the current text (console view). */
    public void display() {
        System.out.println("Text: " + text);
    }

    /** Resets the editor and wipes all history. */
    public void reset() {
        text = "";
        undoStack.clear();
        redoStack.clear();
    }

    public String getText()          { return text; }
    public boolean canUndo()         { return !undoStack.isEmpty(); }
    public boolean canRedo()         { return !redoStack.isEmpty(); }
    public int undoCount()           { return undoStack.size(); }
    public int redoCount()           { return redoStack.size(); }
    public String[] undoHistory()    { return undoStack.toArray(); }
    public String[] redoHistory()    { return redoStack.toArray(); }
}
