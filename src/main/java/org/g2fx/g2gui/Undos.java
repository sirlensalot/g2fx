package org.g2fx.g2gui;

import java.util.ArrayDeque;
import java.util.Deque;

public class Undos {
    private final Deque<Undo<?>> undoStack = new ArrayDeque<>();
    private final Deque<Undo<?>> redoStack = new ArrayDeque<>();
    private boolean inUndoRedo = false;

    public boolean isInUndoRedo() {
        return inUndoRedo;
    }

    public <T> void push(Undo<T> undo) {
        if (inUndoRedo) return;
        undoStack.push(undo);
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        inUndoRedo = true;
        Undo<?> action = undoStack.pop();
        try {
            action.undo();
        } finally { inUndoRedo = false; }
        redoStack.push(action);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        inUndoRedo = true;
        Undo<?> action = redoStack.pop();
        try {
            action.redo();
        } finally { inUndoRedo = false; }
        undoStack.push(action);

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

    public record Undo<T>(FxProperty<T> property, T oldValue, T newValue) {
        public void undo() {
            property.setValue(oldValue);
        }
        public void redo() {
            property.setValue(newValue);
        }
    }
}
