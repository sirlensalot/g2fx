package org.g2fx.g2gui.window;

import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.function.Function;

public interface G2Window {
    default void show() {
        Stage stage = getStage();
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }
    default void setMenu(Function<Stage,MenuBar> menuBar) {
        getRoot().getChildren().addFirst(menuBar.apply(getStage()));
    }

    Stage getStage();

    Pane getRoot();
}
