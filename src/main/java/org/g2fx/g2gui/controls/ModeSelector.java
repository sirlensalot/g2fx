package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.g2fx.g2gui.FXUtil;

import java.io.File;

import static org.g2fx.g2gui.FXUtil.withClass;

public class ModeSelector {

    private final ModulePane.IndexParam ip;
    private final Pane pane;
    private Property<Integer> selectedProperty;


    public ModeSelector(ModulePane.IndexParam ip, UIElements.PartSelector ps, ModulePane parent) {
        this.ip = ip;
        pane = mkControl(ps,parent);

    }

    public Pane getPane() {
        return pane;
    }

    private Pane mkControl(UIElements.PartSelector c, ModulePane parent) {

        // ListView for dropdown items
        ObservableList<Image> items = FXCollections.observableArrayList(c.Images().stream().map(f ->
                FXUtil.getImageResource("img" + File.separator + f)).toList());
        ListView<Image> listView = withClass(new ListView<>(items),"module-mode-list");
        selectedProperty = new SimpleObjectProperty<>(listView,parent.paramId(ip),ip.param().param().def);
        listView.setFixedCellSize(c.Height());
        listView.setPrefHeight(c.Height()*items.size());
        listView.setPrefWidth(c.ImageWidth()+12);

        ImageView current = new ImageView(items.getFirst());
        current.setViewport(new Rectangle2D(0,0,c.Width(),c.Height()));

        listView.setCellFactory(lv -> new ListCell<>() {
            private final ImageView view = new ImageView();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(Image item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : view);
                if (item != null && !empty) {
                    view.setImage(item);
                    view.setFitWidth(c.ImageWidth());   // change size as needed
                }
            }
        });


        // Popup to hold the ListView
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(listView);

        // Arrow button (styled to look like combo arrow)
        Button arrowButton = new Button();
        arrowButton.getStyleClass().add("module-mode-arrow-button");
        arrowButton.setFocusTraversable(false);
        arrowButton.setText("▼");
        arrowButton.setGraphic(null);

        // When arrow is clicked, show popup and hide arrow button
        arrowButton.setOnAction(e -> {
            if (!popup.isShowing()) {
                popup.show(arrowButton.getScene().getWindow());
                popup.setX(current.localToScreen(0, arrowButton.getHeight()).getX());
                popup.setY(arrowButton.localToScreen(0, arrowButton.getHeight()).getY());
                arrowButton.setVisible(false);
                listView.getSelectionModel().select(selectedProperty.getValue());
            }
        });

        // When an item is selected, update text field and hide popup + show arrow again
        listView.setOnMouseClicked((MouseEvent event) -> {
            Image selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                current.setImage(selected);
                //current.setFitWidth(c.ImageWidth());
                popup.hide();
                arrowButton.setVisible(true);
                selectedProperty.setValue(listView.getSelectionModel().getSelectedIndex());
            }
        });

        selectedProperty.addListener((cc,o,n) -> current.setImage(items.get(n)));

        // When popup hides (outside click), also show arrow again
        popup.setOnHidden(e -> arrowButton.setVisible(true));

        // Layout: HBox with arrow button on left, text field on right
        HBox hbox = withClass(new HBox(),"module-mode-box");
        hbox.getChildren().addAll(current,arrowButton);
        hbox.setAlignment(Pos.CENTER_LEFT);
        ModulePane.layout(c,hbox);
        return hbox;
    }

    public Property<Integer> selectedProperty() { return selectedProperty; }

}
