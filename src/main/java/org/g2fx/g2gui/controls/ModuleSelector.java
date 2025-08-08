package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2lib.model.ModuleType;

import static org.g2fx.g2gui.FXUtil.withClass;

public class ModuleSelector {

    private final int id;
    private final ModuleType type;
    private final SimpleStringProperty name;
    private final Pane pane;

    private boolean isModuleChange = false;


    public ModuleSelector(int id, String name, ModuleType type,
                          FXUtil.TextFieldFocusListener textFocusListener) {
        this.id = id;
        this.type = type;
        TextField tf = new TextField(name);
        this.name = FXUtil.mkTextFieldCommitProperty(tf,textFocusListener);
        pane = mkControl(tf);
    }

    public Pane getPane() {
        return pane;
    }

    record ModTypeShortName (ModuleType type) {
        @Override
        public String toString() {
            return type.shortName;
        }
    }


    public Property<String> name() {
        return name;
    }

    private Pane mkControl(TextField textField) {

        textField.setFocusTraversable(false);
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

        // Arrow button (styled to look like combo arrow)
        Button arrowButton = new Button();
        arrowButton.getStyleClass().add("modsel-arrow-button");
        arrowButton.setFocusTraversable(false);
        arrowButton.setText("▼");
        arrowButton.setGraphic(null);

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
                isModuleChange = true;
                textField.setText(selected + "1");
                popup.hide();
                arrowButton.setVisible(true);
            }
        });

        // When popup hides (outside click), also show arrow again
        popup.setOnHidden(e -> arrowButton.setVisible(true));

        // Layout: HBox with arrow button on left, text field on right
        HBox hbox = withClass(new HBox(),"modsel-box");
        if (type == ModuleType.M_Name) {
            hbox.getChildren().add(textField);
            hbox.setAlignment(Pos.CENTER);
            hbox.getStyleClass().add("modsel-text-field-name");
            textField.setAlignment(Pos.CENTER);
        } else {
            hbox.getChildren().addAll(arrowButton,textField);
            hbox.setAlignment(Pos.CENTER_LEFT);
        }
        return hbox;
    }



}
