package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import org.g2fx.g2gui.*;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.UserModuleData;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.controls.UIElements.Orientation.Vertical;

public class ModulePane {

    public static final int GRID_X = 255;
    public static final int GRID_Y = 15;
    /**
     * Lib-side module, ONLY ACCESS ON LIB THREAD or
     * in bridge constructors
     */
    private final PatchModule patchModule;
    private final Bridges bridges;
    private final UIModule<UIElement> ui;
    private final SlotPane parent;
    private final ModuleSelector moduleSelector;
    private final int height;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final int index;
    private boolean selected;

    public static String[] MODULE_COLORS = new String[] {
            "#EEEEEE", // JavaFX default-ish, as opposed to "#C0C0C0" from o/s editor
            "#BABACC", // 1
            "#BACCBA", // 2
            "#CCBAB0", // 3
            "#AACBD0", // 4
            "#D4A074", // 5
            "#7A77E5", // 6 R
            "#BDC17B", // 7
            "#80B982", // 8
            "#48D1E7", // 9
            "#62D193", // 10
            "#7DC7DE", // 11
            "#C29A8F", // 12
            "#817DBA", // 13
            "#8D8DCA", // 14
            "#A5D1DE", // 15
            "#9CCF94", // 16
            "#C7D669", // 17
            "#C8D2A0", // 18
            "#D2D2BE", // 19
            "#C08C80", // 20
            "#C773D6", // 21
            "#BE82BE", // 22
            "#D2A0CD", // 23
            "#D2BED2" // 24
    };

    private final Map<Integer,Property<Integer>> intProps = new TreeMap<>();
    private final Map<Integer,Property<Boolean>> boolProps = new TreeMap<>();


    /**
     * Captures initial module info that is then one-way UI -> backend from then on.
     */
    public record ModuleSpec(
            int index,
            ModuleType type,
            int uprate,
            boolean leds,
            List<Integer> modes) {}

    private final Pane pane;
    private final ModuleType type;

    private final List<PropertyBridge<?,?>> moduleBridges = new ArrayList<>();

    private final SimpleObjectProperty<UserModuleData.Coords> coords =
            new SimpleObjectProperty<>(new UserModuleData.Coords(0,0));

    private final SimpleObjectProperty<Integer> color = new SimpleObjectProperty<>();
    
    public ModulePane(UIModule<UIElement> ui, ModuleSpec m,
                      FXUtil.TextFieldFocusListener textFocusListener,
                      Bridges bridges, PatchModule pm, SlotPane parent) {
        height = ui.Height();
        type = m.type;
        this.index = m.index;
        this.bridges = bridges;
        this.patchModule = pm;
        this.ui = ui;
        this.parent = parent;
        this.textFocusListener = textFocusListener;
        moduleSelector = new ModuleSelector(m.index, "", m.type, textFocusListener);

        List<Node> children = new ArrayList<>(List.of(moduleSelector.getPane()));
        children.addAll(renderControls());
        pane = withClass(new Pane(FXUtil.toArray(children)),"mod-pane");
        pane.setMinHeight(height * GRID_Y);
        pane.setMinWidth(GRID_X);
        pane.setMaxHeight(height * GRID_Y);
        pane.setMaxWidth(GRID_X);



        addBridge(bridges.bridge(moduleSelector.name(), dd -> patchModule.name()));

        addBridge(bridges.bridge(color,d -> patchModule.getUserModuleData().color()));
        color.addListener((c,o,n) -> {
            pane.setBackground(FXUtil.rgbFill(MODULE_COLORS[n]));
        });

        addBridge(bridges.bridge(d->patchModule.getUserModuleData().coords(),
                new FxProperty.SimpleFxProperty<>(coords),Iso.id()));
        coords.addListener((c,o,n) -> {
            pane.setLayoutX(n.column()* GRID_X);
            pane.setLayoutY(n.row()* GRID_Y);
        });
    }


    public void setSelected(boolean selected) {
        this.selected = selected;
        moduleSelector.setSelected(selected);
    }

    public boolean isSelected() {
        return selected;
    }

    private Collection<? extends Node> renderControls() {
        List<Node> cs = new ArrayList<>(ui.Controls().size());
        for (UIElement e : ui.Controls()) {
            cs.add(renderControl(e));
        }
        return cs;
    }

    private Node renderControl(UIElement e) {
        return switch (e) {

            case UIElements.ButtonText c -> mkButtonText(e, c);

            case UIElements.TextEdit c -> mkTextEdit(c);

            case UIElements.Knob c -> mkKnob(c,resolveParam(c.Control()));

            case UIElements.Text c -> mkText(c);

            case UIElements.Line c -> mkLine(c);

            case UIElements.TextField c -> mkTextField(c);

            default -> empty(e);
        };
    }

    private static Label mkText(UIElements.Text c) {
        Label b = label(c.Text());
        b.setLayoutX(c.XPos());
        b.setLayoutY(c.YPos()-2.5);
        return b;
    }

    private Node mkTextField(UIElements.TextField c) {
        IndexParam ip = resolveParam(c.MasterRef());
        Property<Integer> p = intProps.get(ip.index);
        Label l = layout(c,withClass(new Label("0"),"module-text-label"));
        l.setAlignment(Pos.CENTER);
        l.setPrefWidth(c.Width());
        Function<Integer, String> f = ip.param.param().formatter;
        if (p != null && f != null) {
            formatParam(l,p,f);
        } else {
            System.out.println("TextField TODO: " + this + ":" + ip);
        }

        return l;
    }

    private <T> void formatParam(Label l, Property<T> p, Function<T,String> f) {
        p.addListener((c,o,n) -> l.setText(n != null ? f.apply(n) : ""));
    }

    private Node mkButtonText(UIElement e, UIElements.ButtonText c) {
        IndexParam ip = resolveParam(c.Control());
        if (ip.param().param() == ModParam.ActiveMonitor) {
            return mkPowerButton(c, ip);
        } else if (c.Images() != null) {
            return empty(e);
        } else if (c.Type() == UIElements.ButtonType.Check) {
            return mkTextToggle(c, ip);
        } else {
            return mkTextMomentary(c, ip);
        }
    }

    private Node mkTextEdit(UIElements.TextEdit c) {
        return c.Type() == UIElements.ButtonType.Check ?
                mkTextEditToggle(c, resolveParam(c.Control())) :
                mkTextEditMomentary(c, resolveParam(c.Control()));
    }

    private static Line mkLine(UIElements.Line c) {
        boolean vertical = Vertical == c.Orientation();
        Line line = withClass(new Line(c.XPos(), c.YPos(),
                vertical ? c.XPos() : c.XPos() + c.Length() - 1,
                vertical ? c.YPos() + c.Length() - 1 : c.YPos()
        ), "module-line", "module-line-" + c.Weight());
        line.setViewOrder(5);
        return line;
    }

    private Node mkKnob(UIElements.Knob c, IndexParam ip) {
        if (c.Type().isKnob) {
            double scale = switch (c.Type()) {
                case Small, Reset -> 0.78;
                case Medium, ResetMedium -> 0.88;
                default -> 1.0;
            };
            Knob knob = new Knob(ip.param.name(), scale, c.Type().isReset);
            layout(c, knob);
            bindVarControl(ip,intProps,knob.getValueProperty(), v -> {
                Property<Integer> p =
                        new SimpleObjectProperty<>(knob, varPropName(ip, v), null);
                moduleBridges.add(bridges.bridge(d -> patchModule.getParamValueProperty(v, ip.index),
                        new FxProperty.SimpleFxProperty<>(p,knob.valueChangingProperty()),
                        Iso.id()));
                return p;
            });
            return knob;
        }
        return empty(c);
    }

    private String varPropName(IndexParam ip, int v) {
        return type.shortName + ":" + ip.param().name() + ":" + v;
    }

    private Node empty(UIElement e) {
        //System.out.println("TODO: " + e);
        return new Pane();
    }

    private ToggleButton mkPowerButton(UIElements.ButtonText c, IndexParam ip) {
        final ToggleButton b = new PowerButton().getButton();
        return layout(c,mkToggle(c, ip, b, b.selectedProperty()));
    }

    private Node mkTextEditMomentary(UIElements.TextEdit c, IndexParam ip) {
        MomentaryButton b = withClass(new MomentaryButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        return layout(c,makeEditable(mkNoUndoToggle(c,ip,b,b.selectedProperty()), ip));
    }

    private Node mkTextEditToggle(UIElements.TextEdit c, IndexParam ip) {
        ToggleButton b =withClass(new ToggleButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        return layout(c,makeEditable(mkToggle(c, ip, b, b.selectedProperty()), ip));
    }

    private Node mkTextMomentary(UIElements.ButtonText c, IndexParam ip) {
        MomentaryButton b = withClass(new MomentaryButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        return layout(c,mkNoUndoToggle(c,ip,b,b.selectedProperty()));
    }

    private ToggleButton mkTextToggle(UIElements.ButtonText c, IndexParam ip) {
        ToggleButton b = withClass(new ToggleButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        return layout(c,mkToggle(c, ip, b, b.selectedProperty()));
    }

    private <T extends Node> T mkToggle(UIElement c, IndexParam ip, T b, BooleanProperty selectedProperty) {
        return mkToggle(c,ip,b,selectedProperty,null);
    }

    private <T extends Node> T mkNoUndoToggle(UIElement c, IndexParam ip, T b, BooleanProperty selectedProperty) {
        return mkToggle(c,ip,b,selectedProperty,new SimpleBooleanProperty(true));
    }


    private <T extends Node> T mkToggle(UIElement c, IndexParam ip, T b, BooleanProperty selectedProperty,
                                        ObservableValue<Boolean> defeatUndoProperty) {
        bindVarControl(ip,boolProps,selectedProperty, v -> {
            SimpleBooleanProperty p =
                    new SimpleBooleanProperty(b, varPropName(ip, v),false);
            FxProperty.SimpleFxProperty<Boolean> fxProperty = defeatUndoProperty == null ?
                    new FxProperty.SimpleFxProperty<>(p) : new FxProperty.SimpleFxProperty<>(p,defeatUndoProperty);
            moduleBridges.add(bridges.bridge(d -> patchModule.getParamValueProperty(v, ip.index),
                    fxProperty,
                    Iso.BOOL_PARAM_ISO));
            return p;
                });
        return b;
    }


    public  <T extends ButtonBase> Node makeEditable(T button, IndexParam ip) {

        TextField editor = setTextFieldMaxLength(new TextField("-------"),7);
        editor.setPrefWidth(64);
        editor.setVisible(false);

        Pane root = new Pane(button,editor);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editNameItem = new MenuItem("Edit name...");
        contextMenu.getItems().add(editNameItem);

        button.setOnContextMenuRequested(e -> {
            contextMenu.show(button, e.getScreenX(), e.getScreenY());
        });

        // When "Edit name..." is clicked, show text field for editing
        editNameItem.setOnAction(e -> {
                    contextMenu.hide();
                editor.setText(button.getText());
                editor.setVisible(true);
                textFocusListener.focusChange(true);
                //editor.setPrefWidth(button.getWidth());
                //editor.setLayoutX(button.getLayoutX());
                //editor.setLayoutY(button.getLayoutY());
                editor.requestFocus();
                editor.selectAll();
                root.requestLayout();

                button.setVisible(false);
            });

        // Commit edit: apply text on Enter or focus lost
        editor.setOnAction(e -> commitEdit(button, editor));
        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitEdit(button, editor);
            }
        });

        // Cancel edit on ESC
        editor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                editor.setVisible(false);
                button.setVisible(true);
            }
        });

        //Platform.runLater(()->editor.setVisible(false));

        addBridge(bridges.bridge(d -> patchModule.getModuleLabels(ip.index()).getFirst(),
                FxProperty.adaptReadOnly(button.textProperty(), button::setText),
                Iso.id()
        ));//TODO for sw8-1 and friends

        return root;
    }

    // Commit changes from editor back to button text
    private void commitEdit(ButtonBase button, TextField editor) {
        button.setText(editor.getText());
        editor.setVisible(false);
        button.setVisible(true);
        textFocusListener.focusChange(false);
    }


    private static <T extends Node> T layout(UIElement c, T b) {
        b.setLayoutX(c.XPos());
        b.setLayoutY(c.YPos());
        return b;
    }

    record IndexParam(NamedParam param, int index) {}
    
    private IndexParam resolveParam(String control) {
        for (int i = 0; i < type.getParams().size(); i++) {
            NamedParam p = type.getParams().get(i);
            if (p.name().equals(control)) {
                return new IndexParam(p,i);
            }
        }
        throw new IllegalArgumentException("Bad control name: " + control + ", module: " + this);
    }

    private IndexParam resolveParam(int index) {
        if (index > type.getParams().size()) {
            throw new IllegalArgumentException("Bad param index: " + index + ", module: " + this);
        }
        return new IndexParam(type.getParams().get(index),index);
    }

    private <T> void bindVarControl(IndexParam ip, Map<Integer,Property<T>> coll, Property<T> control, IntFunction<Property<T>> varPropBuilder) {
        parent.bindVarControl(control,varPropBuilder);
        coll.put(ip.index,control);
    }


    public void addBridge(PropertyBridge<?, ?> bridge) {
        moduleBridges.add(bridge);
    }

    public Pane getPane() {
        return pane;
    }

    public List<PropertyBridge<?, ?>> getModuleBridges() {
        return moduleBridges;
    }

    public Property<Integer> color() { return color; }

    public Property<UserModuleData.Coords> coords() { return coords; }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return type + ":" + index + ":" + moduleSelector.name().getValue();
    }
}
