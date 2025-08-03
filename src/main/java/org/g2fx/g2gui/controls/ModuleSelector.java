package org.g2fx.g2gui.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.util.StringConverter;
import org.g2fx.g2lib.model.ModuleType;

import static org.g2fx.g2gui.FXUtil.withClass;

public class ModuleSelector {

    private final int id;
    private final ModuleType type;
    private final SimpleStringProperty name;
    private final HBox hb;


    public ModuleSelector(int id, String name, ModuleType type) {
        this.id = id;
        this.type = type;
        this.name = new SimpleStringProperty(name);
        hb = mkControl();
    }

    public Node getPane() {
        return hb;
    }

    record ModTypeShortName (ModuleType type) {
        @Override
        public String toString() {
            return type.shortName;
        }
    }


    private void valueChanged(String n) {
        name.set(n);
    }


    private HBox mkControl() {
        // Arrow button (styled to look like combo arrow)
        Button arrowButton = new Button();
        arrowButton.getStyleClass().add("modsel-arrow-button");
        arrowButton.setFocusTraversable(false);
        arrowButton.setText("▼");
        arrowButton.setGraphic(null);

        // TextField with transparent background
        TextField textField = new TextField(name.get()); //TODO not bridged
        //textField.setEditable(false);
        textField.setFocusTraversable(false);
        textField.setTextFormatter(new TextFormatter<String>(new StringConverter<>() {
            @Override public String toString(String object) { return object; }
            @Override public String fromString(String string) { return string; }
        },
                name.get()));
        textField.getTextFormatter().valueProperty().addListener((c,o,n) -> {
            if (n != null && !n.equals(o)) {
                valueChanged((String) n);
            }
        });
        textField.getStyleClass().add("modsel-text-field");

        // ListView for dropdown items
        ObservableList<ModTypeShortName> items = FXCollections.observableArrayList(
                ModuleType.BY_PAGE.get(type.modPageIx.page()).stream().map(ModTypeShortName::new).toList()); //FXCollections.observableArrayList("Apple", "Banana", "Cherry", "Date");
        ListView<ModTypeShortName> listView = withClass(new ListView<>(items),"modsel-list");
        listView.setFixedCellSize(18);
        listView.setPrefHeight(18 * items.size() + 2);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ModTypeShortName item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.toString());
                    if (item.type == type) {
                        setDisable(true);
                        setStyle("-fx-text-fill: gray;");
                    }
                }
            }
        });


        // Popup to hold the ListView
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(listView);

        // When arrow is clicked, show popup and hide arrow button
        arrowButton.setOnAction(e -> {
            if (!popup.isShowing()) {
                popup.show(arrowButton.getScene().getWindow());
                popup.setX(arrowButton.localToScreen(0, arrowButton.getHeight()).getX());
                popup.setY(arrowButton.localToScreen(0, arrowButton.getHeight()).getY());
                arrowButton.setVisible(false);
            }
        });

        // When an item is selected, update text field and hide popup + show arrow again
        listView.setOnMouseClicked((MouseEvent event) -> {
            ModTypeShortName selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                textField.setText(selected + "1");
                popup.hide();
                arrowButton.setVisible(true);
            }
        });

        // When popup hides (outside click), also show arrow again
        popup.setOnHidden(e -> arrowButton.setVisible(true));

        // Layout: HBox with arrow button on left, text field on right
        HBox hbox = new HBox(arrowButton, textField);
        hbox.setAlignment(Pos.CENTER_LEFT);
        return hbox;
    }



}
