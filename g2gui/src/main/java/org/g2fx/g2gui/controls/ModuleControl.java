package org.g2fx.g2gui.controls;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.g2fx.g2lib.model.ModuleType;

import static org.g2fx.g2gui.FXUtil.withClass;

public class ModuleControl {

    private final int id;
    private final ModuleType type;
    private String name;

    private final Pane pane;

    record NameAndType (String name,ModuleType type) {
        @Override
        public String toString() {
            return name != null ? name : type.shortName;
        }
    }
    private final ComboBox<NameAndType> nameAndTypeCombo;

    public ModuleControl(int id, String name, ModuleType type) {
        this.id = id;
        this.type = type;
        this.name = name;
        nameAndTypeCombo = withClass(mkModuleSelectCombo());
        pane = switch (type) {
            case M_ClkGen -> mkClkGen();
            default -> new Pane(new Label("Unsupported: " + type));
        };
        pane.getStyleClass().add("module-control");
    }

    public Pane getPane() {
        return pane;
    }

    private ComboBox<NameAndType> mkModuleSelectCombo() {
        ComboBox<NameAndType> cb = new ComboBox<>(FXCollections.observableArrayList(
                ModuleType.BY_PAGE.get(type.modPageIx.page()).stream().map(
                        mt -> new NameAndType(null, mt)).toList()));

        cb.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NameAndType item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.toString());
                    setDisable(item.type == type);
                }
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(NameAndType item, boolean empty) {
                super.updateItem(item,empty);
                if (item != null) { setText(item.toString()); }
            }
        });
        cb.setValue(new NameAndType(name,type));
        cb.setEditable(false);
        cb.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                cb.setEditable(true);
                Platform.runLater(cb::requestFocus);
            }
        });
        cb.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                cb.setEditable(false);
                //commit value to backend here
            }
        });
        cb.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && cb.isEditable()) {
                cb.setEditable(false);
                //commit here too?
            }
        });
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(NameAndType object) {
                return object != null ? object.toString() : "";
            }

            @Override
            public NameAndType fromString(String string) {
                return new NameAndType(string,type);
            }
        });
        return cb;
    }


    private Pane mkClkGen() {
        return withClass(new Pane(nameAndTypeCombo));
    }


}
