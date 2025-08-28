package org.g2fx.g2gui;

import org.g2fx.g2gui.bridge.FxProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Undos {
    private final Deque<List<Undo<?>>> undoStack = new ArrayDeque<>();
    private final Deque<List<Undo<?>>> redoStack = new ArrayDeque<>();
    private boolean inUndoRedo = false;

    private List<Undo<?>> multi = null;

    public boolean isInUndoRedo() {
        return inUndoRedo;
    }

    public void beginMulti() {
        if (inMulti()) { throw new IllegalStateException("Undos.beginMulti: in multi already"); }
        multi = new ArrayList<>();
    }

    public void commitMulti() {
        if (!inMulti()) { throw new IllegalStateException("Undos.commitMulti: not in multi"); }
        push(multi);
        multi = null;
    }

    private boolean inMulti() { return multi != null; }

    public <T> void push(FxProperty<T> property, T oldValue, T newValue) {
        if (inUndoRedo) return;
        Undo<T> undo = new Undo<T>(property,oldValue,newValue);
        if (inMulti()) {
            multi.add(undo);
        } else {
            List<Undo<?>> u = List.of(undo);
            push(u);
        }
    }

    private void push(List<Undo<?>> u) {
        undoStack.push(u);
        redoStack.clear();
    }

    public void undo() {
        if (!canUndo()) return;
        inUndoRedo = true;
        List<Undo<?>> action = undoStack.pop();
        try {
            action.forEach(Undo::undo);
        } finally { inUndoRedo = false; }
        redoStack.push(action);
    }

    public void redo() {
        if (!canRedo()) return;
        inUndoRedo = true;
        List<Undo<?>> action = redoStack.pop();
        try {
            action.forEach(Undo::redo);
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
