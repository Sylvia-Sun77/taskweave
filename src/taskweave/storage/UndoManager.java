package taskweave.storage;

import java.util.Stack;

import taskweave.model.Workflow;

// keeps snapshots of the workflow, like taking a photo before every change
public class UndoManager {

    private final Stack<Workflow> undoStack = new Stack<>();
    private final Stack<Workflow> redoStack = new Stack<>();

    // call this right before changing the workflow
    public void beforeChange(Workflow current) {
        undoStack.push(current.copy());
        redoStack.clear();
    }

    // returns the workflow as it was before the last change
    public Workflow undo(Workflow current) {
        if (!canUndo()) {
            throw new IllegalStateException("Nothing to undo");
        }
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
