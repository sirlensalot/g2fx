package org.g2fx.g2gui.panel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.bridge.PropertyBridge;
import org.g2fx.g2gui.controls.*;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2gui.ui.UIModule;
import org.g2fx.g2gui.ui.UIParamControl;
import org.g2fx.g2lib.model.*;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.UserModuleData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static org.controlsfx.control.CheckComboBox.COMBO_BOX_ROWS_TO_MEASURE_WIDTH_KEY;
import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.ui.UIElements.Orientation.Horizontal;
import static org.g2fx.g2gui.ui.UIElements.Orientation.Vertical;

public class ModulePane {

    public static final int GRID_X = 255;
    public static final int GRID_Y = 15;
    public static final List<Image> LEVEL_SHIFT_IMAGES = Stream.of(
            "LS-0-Pos.png",
            "LS-1-PosInv.png",
            "LS-2-Neg.png",
            "LS-3-NegInv.png",
            "LS-4-Bip.png",
            "LS-5-BipInv.png").map(s -> FXUtil.getImageResource("img" + File.separator + s)).toList();


    /**
     * Lib-side module, ONLY ACCESS ON LIB THREAD or
     * in bridge constructors
     */
    private final PatchModule patchModule;
    private final Bridges bridges;
    private final UIModule<UIElement> ui;
    private final SlotPane slotPane;
    private final ModuleSelector moduleSelector;
    private final int height;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final int index;
    private final AreaPane areaPane;
    private boolean selected;

    private final ModuleTextFieldBuilder textFieldBuilder;

    private final ParamListener paramListener;

    private final List<RebindableControl<Integer,?>> varBindings = new ArrayList<>();


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

    private final Map<Connector.PortType,List<Connectors.Conn>> conns = Map.of(
            Connector.PortType.In,new ArrayList<>(),
            Connector.PortType.Out,new ArrayList<>());

    private Graphs graphs;

    public ModulePane(UIModule<UIElement> ui, ModuleSpec m,
                      FXUtil.TextFieldFocusListener textFocusListener,
                      Bridges bridges, PatchModule pm, SlotPane slotPane, AreaPane areaPane) {
        height = ui.Height();
        type = m.type;
        paramListener = new ParamListener(type,this);
        graphs = new Graphs(paramListener,type);
        this.index = m.index;
        this.bridges = bridges;
        this.patchModule = pm;
        this.ui = ui;
        this.slotPane = slotPane;
        this.textFocusListener = textFocusListener;
        this.areaPane = areaPane;
        moduleSelector = new ModuleSelector(m.index, "", m.type, textFocusListener);
        textFieldBuilder = new ModuleTextFieldBuilder(paramListener);

        List<Node> children = new ArrayList<>(List.of(moduleSelector.getPane()));
        children.addAll(renderControls());

        pane = withClass(new Pane(FXUtil.toArray(children)),"mod-pane");
        pane.setMinHeight(height * GRID_Y);
        pane.setMinWidth(GRID_X);
        pane.setMaxHeight(height * GRID_Y);
        pane.setMaxWidth(GRID_X);



        addBridge(bridges.bridge(moduleSelector.name(), dd -> patchModule.name()));

        addBridge(bridges.bridge(color,d -> patchModule.getUserModuleData().color()));
        color.addListener((c,o,n) -> pane.setBackground(FXUtil.rgbFill(ParamConstants.MODULE_COLORS[n])));

        addBridge(bridges.bridge(d->patchModule.getUserModuleData().coords(),
                new FxProperty.SimpleFxProperty<>(coords), Iso.id()));
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
            cs.add(renderElement(e));
        }
        return cs;
    }

    private Node renderElement(UIElement e) {
        return switch (e) {

            case UIParamControl uc -> renderParamControl(uc);

            case UIElements.TextField c -> textFieldBuilder.mkTextField(c);

            case UIElements.Text c -> mkText(c);

            case UIElements.Line c -> mkLine(c);

            case UIElements.Bitmap c -> mkBitmap(c);

            case UIElements.PartSelector c -> mkPartSelector(c);

            case UIElements.Symbol c -> mkSymbol(c);

            case UIElements.Input c -> mkInput(c);

            case UIElements.Output c -> mkOutput(c);

            case UIElements.Graph c -> graphs.mkGraph(c);

            case UIElements.Led c -> mkLed(c);

            default -> paramListener.empty(e, "renderElement");

        };
    }

    private Node mkLed(UIElements.Led c) {
        if (c.Type() == UIElements.LedType.Green) {
            Rectangle r = withClass(new Rectangle(7,7),"led-green","led-green-off");
            layout(c,r);
            return r;
        } else {
            return paramListener.empty(c,"LedSequencer");
        }
    }

    private Node mkOutput(UIElements.Output c) {
        return addConn(Connectors.makeOutput(c,this));
    }

    private Node mkInput(UIElements.Input c) {
        return addConn(Connectors.makeInput(c,this));
    }

    private Node addConn(Connectors.Conn conn) {
        conns.get(conn.portType()).add(conn);
        areaPane.getConns().addConn(conn);
        return conn.control();
    }

    public Connectors.Conn resolveConn(Connector.PortType type, int idx) {
        var l = conns.get(type);
        if (idx >= l.size()) { throw new IllegalArgumentException("Invalid " + type + " conn index: " + idx + ": " + conns + ", " + this); }
        return l.get(idx);
    }


    private Node mkSymbol(UIElements.Symbol c) {
        Label l = label(c.Type().toString());
        l.setFont(Font.font(6));
        l.setBorder(Border.stroke(Color.GREY));
        layout(c,l);
        l.setPrefWidth(c.Width());
        l.setPrefHeight(c.Height());
        return l;
    }

    private Node mkPartSelector(UIElements.PartSelector c) {
        IndexParam mip = new IndexParam(type.modes.get(c.CodeRef()),c.CodeRef(),this.toString());
        if (c.Images()==null) {
            //TODO this is probably going away as it is too wide, adapt ModeSelector to handle text
            ComboBox<String> combo = withClass(
                    new ComboBox<>(FXCollections.observableArrayList(mip.param().param().enums)),"module-mode-combo");
            addBridge(bridges.bridge(d -> patchModule.getUserModuleData().mode(mip.index()),
                    FxProperty.adaptReadOnly(combo.getSelectionModel().selectedIndexProperty(),n ->
                            combo.getSelectionModel().select(n.intValue())),Iso.INTEGER_NUMBER_ISO));

            combo.setPrefWidth(c.ImageWidth());
            combo.setMaxHeight(c.Height());
            combo.setFocusTraversable(false);
            combo.getProperties().put(COMBO_BOX_ROWS_TO_MEASURE_WIDTH_KEY,1);
            layout(c,combo);
            SimpleObjectProperty<Integer> selectedReadOnlyProperty = new SimpleObjectProperty<>();
            combo.getSelectionModel().selectedIndexProperty().addListener((cc,oo,n) ->
                    selectedReadOnlyProperty.setValue(n.intValue()));
            paramListener.addModeProp(mip,selectedReadOnlyProperty);
            return combo;
        } else {
            ModeSelector ms = new ModeSelector(mip, c, this);
            addBridge(bridges.bridge(ms.selectedProperty(),d -> patchModule.getUserModuleData().mode(mip.index())));
            paramListener.addModeProp(mip,ms.selectedProperty());
            return ms.getPane();
        }


    }

    private Node renderParamControl(UIParamControl uc) {

        IndexParam ip = paramListener.resolveParam(uc);

        return switch (uc) {

            case UIElements.ButtonText c -> mkButtonText(c,ip);

            case UIElements.ButtonFlat c -> mkButtonFlat(c,ip);

            case UIElements.ButtonIncDec c -> mkButtonIncDec(c,ip);

            case UIElements.TextEdit c -> mkTextEdit(c,ip);

            case UIElements.Knob c -> mkKnob(c,ip);

            case UIElements.LevelShift c -> mkLevelShift(c,ip);

            case UIElements.ButtonRadio c -> mkButtonRadio(c,ip);

            case UIElements.ButtonRadioEdit c -> mkButtonRadioEdit(c,ip);

        };

    }

    private Node mkButtonRadio(UIElements.ButtonRadio c, IndexParam ip) {
        ButtonRadio br = new ButtonRadio(this,c,ip);
        bindIntParam(ip,br.getControl(), br.selectedToggleIndexProperty(), null);
        return br.getControl();
    }

    private Node mkButtonRadioEdit(UIElements.ButtonRadioEdit c, IndexParam ip) {
        ButtonRadio br = new ButtonRadio(this,c,ip);
        bindIntParam(ip,br.getControl(), br.selectedToggleIndexProperty(), null);
        return br.getControl();
    }


    private Node mkButtonIncDec(UIElements.ButtonIncDec c, IndexParam ip) {
        ModParam mp = ip.param().param();
        Spinner<Integer> spinner = layout(c,withClass(
                new Spinner<>(mp.min, mp.max, mp.def),"module-spinner"),new Point2D(0,-1));
        if (c.Type() == Horizontal) {
            spinner.setRotate(90);
            spinner.setTranslateX(6);
            spinner.setTranslateY(-6);
        }
        bindIntParam(ip,spinner,spinner.getValueFactory().valueProperty(), null);
        return spinner;
    }

    private Node mkLevelShift(UIElements.LevelShift c, IndexParam ip) {
        return mkButtonFlat(c,ip,LEVEL_SHIFT_IMAGES.stream().map(
                i -> (TextOrImage) new TextOrImage.IsImage(i)).toList(),15);
    }

    private Node mkBitmap(UIElements.Bitmap c) {
        if (c.CustomText() != null && c.CustomText()) {
            Label l = label(c.Text());
            l.getStyleClass().addAll("custom-text","custom-text-" + c.Text().replace(' ','-'));
            l.setPrefWidth(c.Width());
            ModulePane.layout(c,l,new Point2D(0,.5));
            return l;
        } else {
            return layout(c, getImageViewResource("img" + File.separator + c.ImageFile()));
        }
    }



    private Node mkButtonFlat(UIElements.ButtonFlat c, IndexParam ip) {
        List<TextOrImage> ss = c.Text().isEmpty() ?
                TextOrImage.mkImages(c.Images()) : TextOrImage.mkTexts(c.Text());
        int width = c.Width();

        return mkButtonFlat(c, ip, ss, width);
    }

    private ToggleButton mkButtonFlat(UIElement c, IndexParam ip, List<TextOrImage> ss, int width) {
        MultiStateToggle mst = new MultiStateToggle(ss,
                ip.param().param().def, // using values from yaml but default from ModParam ... sketchy?
                "module-multi-toggle");
        ToggleButton toggle = mst.getToggle();
        bindIntParam(ip, toggle,mst.state(),null);
        toggle.setPrefWidth(width);
        layout(c, toggle);
        return toggle;
    }

    private static Label mkText(UIElements.Text c) {
        Label b = label(c.Text());
        b.getStyleClass().add("module-text");
        b.setLayoutX(c.XPos());
        b.setLayoutY(c.YPos()-2.5);
        return b;
    }




    private Node mkButtonText(UIElements.ButtonText c, IndexParam ip) {
        if (ip.param().param() == ModParam.ActiveMonitor) {
            return mkPowerButton(c, ip);
        } else if (c.Type() == UIElements.ButtonType.Check) {
            return mkTextToggle(c, ip);
        } else {
            return mkTextMomentary(c, ip);
        }
    }

    private Node mkTextEdit(UIElements.TextEdit c, IndexParam ip) {
        return c.Type() == UIElements.ButtonType.Check ?
                mkTextEditToggle(c, ip) :
                mkTextEditMomentary(c, ip);
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
            Knob knob = new Knob(ip.toString(), scale, c.Type().isReset,ip.param().param().min,ip.param().param().max);
            layout(c, knob);
            bindIntParam(ip, knob, knob.getValueProperty(), knob.valueChangingProperty());
            return knob;
        } else if (c.Type() == UIElements.KnobType.Slider) {
            var ms = Sliders.mkSlider(c,ip,this);
            bindIntParam(ip,ms.control(),ms.property(),ms.slider().valueChangingProperty());
            return ms.control();
        }
        return paramListener.empty(c, "Knob-SeqSlider");
    }

    private Function<Device, LibProperty<Integer>> mkParamIntProp(IndexParam ip, int v) {
        return d -> patchModule.getParamValueProperty(v, ip.index());
    }

    private void bindIntParam(IndexParam ip, Node ctl, Property<Integer> property, BooleanProperty changing) {
        paramListener.addIntProp(ip,property);
        bindVarControl(property, v -> {
            Property<Integer> p =
                    new SimpleObjectProperty<>(ctl, property.getName(), null);
            moduleBridges.add(bridges.bridge(mkParamIntProp(ip, v),
                    new FxProperty.SimpleFxProperty<>(p, changing),
                    Iso.id()));
            return p;
        });
    }

    private String varPropName(IndexParam ip, int v) {
        return type.shortName + ":" + ip.param().name() + ":" + v;
    }



    private ToggleButton mkPowerButton(UIElements.ButtonText c, IndexParam ip) {
        final ToggleButton b = new PowerButton().getButton();
        return layout(c,mkToggle(ip, b, b.selectedProperty()));
    }

    private Node mkTextEditMomentary(UIElements.TextEdit c, IndexParam ip) {
        MomentaryButton b = withClass(new MomentaryButton(c.Text(),null), "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,makeEditable(mkNoUndoToggle(ip,b,b.selectedProperty()), ip));
    }

    private Node mkTextEditToggle(UIElements.TextEdit c, IndexParam ip) {
        ToggleButton b =withClass(new ToggleButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,makeEditable(mkToggle(ip, b, b.selectedProperty()), ip));
    }

    private Node mkTextMomentary(UIElements.ButtonText c, IndexParam ip) {
        MomentaryButton b = withClass(
                c.Images() == null ?
                    new MomentaryButton(c.Text(),null) :
                        new MomentaryButton(null,FXUtil.getImageViewResource(c.Images().getFirst())),
                "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,mkNoUndoToggle(ip,b,b.selectedProperty()));
    }

    private ToggleButton mkTextToggle(UIElements.ButtonText c, IndexParam ip) {
        ToggleButton b = withClass(
                MultiStateToggle.mkTextOrImageToggle(
                        c.Images() == null ? new TextOrImage.IsText(c.Text()) :
                                TextOrImage.mkImages(c.Images()).getFirst()),
                "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,mkToggle(ip, b, b.selectedProperty()));
    }

    private <T extends Node> T mkToggle(IndexParam ip, T b, BooleanProperty selectedProperty) {
        return mkToggle(ip,b,selectedProperty,null);
    }

    private <T extends Node> T mkNoUndoToggle(IndexParam ip, T b, BooleanProperty selectedProperty) {
        return mkToggle(ip,b,selectedProperty,new SimpleBooleanProperty(true));
    }


    private <T extends Node> T mkToggle(IndexParam ip, T b, BooleanProperty selectedProperty,
                                        ObservableValue<Boolean> defeatUndoProperty) {
        paramListener.addBoolProp(ip,selectedProperty);
        bindVarControl(selectedProperty, v -> {
            SimpleBooleanProperty p =
                    new SimpleBooleanProperty(b, varPropName(ip, v),false);
            FxProperty.SimpleFxProperty<Boolean> fxProperty = defeatUndoProperty == null ?
                    new FxProperty.SimpleFxProperty<>(p) : new FxProperty.SimpleFxProperty<>(p,defeatUndoProperty);
            moduleBridges.add(bridges.bridge(mkParamIntProp(ip, v),
                    fxProperty,
                    Iso.BOOL_PARAM_ISO));
            return p;
                });
        return b;
    }


    public <T extends ButtonBase> Node makeEditable(T button, IndexParam ip) {

        TextField editor = makeButtonEditField(button);

        addBridge(bridges.bridge(d -> patchModule.getModuleLabels(ip.index()).getFirst(),
                FxProperty.adaptReadOnly(button.textProperty(), button::setText),
                Iso.id()
        ));

        return new Pane(button,editor);
    }

    public <T extends ButtonBase> TextField makeButtonEditField(T button) {
        TextField editor = setTextFieldMaxLength(new TextField("-------"),7);
        editor.setPrefWidth(64);
        editor.setVisible(false);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editNameItem = new MenuItem("Edit name...");
        contextMenu.getItems().add(editNameItem);

        button.setOnContextMenuRequested(e ->
                contextMenu.show(button, e.getScreenX(), e.getScreenY()));

        // When "Edit name..." is clicked, show text field for editing
        editNameItem.setOnAction(e -> {
                    contextMenu.hide();
                editor.setText(button.getText());
                editor.setVisible(true);
                textFocusListener.focusChange(true);
                editor.requestFocus();
                editor.selectAll();
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
        return editor;
    }

    // Commit changes from editor back to button text
    private void commitEdit(ButtonBase button, TextField editor) {
        button.setText(editor.getText());
        editor.setVisible(false);
        button.setVisible(true);
        textFocusListener.focusChange(false);
    }


    public static <T extends Node> T layout(UIElement c, T b) {
        return layout(c,b,Point2D.ZERO);
    }

    public static <T extends Node> T layout(UIElement c, T b, Point2D offset) {
        b.setLayoutX(c.XPos() + offset.getX());
        b.setLayoutY(c.YPos() + offset.getY());
        return b;
    }

    private <T> void bindVarControl(Property<T> control,
                                    IntFunction<Property<T>> varPropBuilder) {
        varBindings.add(slotPane.bindVarControl(control,varPropBuilder));
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
        return String.format("%s:%s:%s:%s", slotPane.getSlot(), areaPane.getAreaId(),type,index);
    }


    public ModuleType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public PatchModule getPatchModule() {
        return patchModule;
    }

    public Bridges getBridges() {
        return bridges;
    }

    public void unbindVarControls() {
        slotPane.unbindVarControls(varBindings);
    }
}
