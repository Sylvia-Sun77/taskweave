package taskweave.storage;

import java.util.Stack;

import taskweave.model.Workflow;

// keeps snapshots of the workflow, like taking a photo before every change
public class UndoManager {

    // Stack: last in, first out, so undo always goes back to the newest snapshot first
    private final Stack<Workflow> undoStack = new Stack<>();
    private final Stack<Workflow> redoStack = new Stack<>();

    // call this right before changing the workflow
    public void beforeChange(Workflow current) {
        // push a copy, not current itself, otherwise the snapshot would change when current changes
        undoStack.push(current.copy());
        // a new change starts a new history, the old redo steps don't match it anymore
        // e.g. add Tea -> undo -> add Rest: redo should not bring Tea back on top of Rest
        redoStack.clear();
    }

    // returns the workflow as it was before the last change
    public Workflow undo(Workflow current) {
        if (!canUndo()) {
            throw new IllegalStateException("Nothing to undo");
        }
        // save the current state to redo first, so redo() can come back to it
        redoStack.push(current.copy());
        return undoStack.pop();
    }

    public Workflow redo(Workflow current) {
        if (!canRedo()) {
            throw new IllegalStateException("Nothing to redo");
        }
        undoStack.push(current.copy());
        return redoStack.pop();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
