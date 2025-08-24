package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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
import org.g2fx.g2lib.model.ParamFormatter;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.UserModuleData;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.controls.UIElements.Orientation.Vertical;
import static org.g2fx.g2lib.model.ModParam.formatHz;
import static org.g2fx.g2lib.model.ModParam.formatMillisSecs;
import static org.g2fx.g2lib.model.ParamConstants.*;

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
    private final AreaId area;
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
                      Bridges bridges, PatchModule pm, SlotPane parent, AreaId area) {
        height = ui.Height();
        type = m.type;
        this.index = m.index;
        this.bridges = bridges;
        this.patchModule = pm;
        this.ui = ui;
        this.parent = parent;
        this.textFocusListener = textFocusListener;
        this.area = area;
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
        color.addListener((c,o,n) -> pane.setBackground(FXUtil.rgbFill(MODULE_COLORS[n])));

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
            cs.add(renderElement(e));
        }
        return cs;
    }

    private Node renderElement(UIElement e) {
        return switch (e) {

            case UIParamControl uc -> renderParamControl(uc);

            case UIElements.TextField c -> mkTextField(c);

            case UIElements.Text c -> mkText(c);

            case UIElements.Line c -> mkLine(c);

            default -> empty(e, "renderElement");

        };
    }

    private Node renderParamControl(UIParamControl uc) {

        IndexParam ip = resolveParam(uc);

        return switch (uc) {

            case UIElements.ButtonText c -> mkButtonText(c,ip);

            case UIElements.ButtonFlat c -> mkButtonFlat(c,ip);

            case UIElements.TextEdit c -> mkTextEdit(c,ip);

            case UIElements.Knob c -> mkKnob(c,ip);

            default -> empty(uc, "renderParamControl");
        };

    }



    private Node mkButtonFlat(UIElements.ButtonFlat c, IndexParam ip) {
        if (!c.Text().isEmpty()) {
            MultiStateToggle mst = new MultiStateToggle(c.Text(),
                    ip.param().param().def, // using values from yaml but default from ModParam ... sketchy?
                    "module-multi-toggle");
            ToggleButton toggle = mst.getToggle();
            bindIntParam(ip, toggle,mst.state(),null);
            layout(c, toggle);
            toggle.setPrefWidth(c.Width());
            return toggle;
        }
        return empty(c, "mkButtonFlat");
    }

    private static Label mkText(UIElements.Text c) {
        Label b = label(c.Text());
        b.setLayoutX(c.XPos());
        b.setLayoutY(c.YPos()-2.5);
        return b;
    }

    private Node mkTextField(UIElements.TextField c) {

        Label l = layout(c,withClass(new Label("0"),"module-text-label"));
        l.setAlignment(Pos.CENTER);
        l.setPrefWidth(c.Width());

        IndexParam ip = resolveParam(c.MasterRef());

        ParamFormatter pf = ip.param.param().formatter;

        Property<Boolean> pb = boolProps.get(ip.index);
        if (pb != null && pf != null && pf.boolFmt() != null) {
            return formatParam(l,pb,pf.boolFmt());
        }

        Property<Integer> pi = intProps.get(ip.index);
        if (pi != null && pf != null && pf.intFmt() != null) {
            return formatParam(l,pi,pf.intFmt());
        }

        switch (c.TextFunc()) {
            case TF_OSC_FREQ: return formatOscFreq(c, l);
            case TF_LFO_FREQ: return formatLfoFreq(c, l);
            case TF_OPERATOR_FREQ: return formatOperatorFreq(c,l);
            case TF_CLK_GEN: return formatClkTempo(c,l);
            case TF_PULSE_TIME: return formatPulseTime(c,l);
            case TF_PSHIFT_FREQ: return formatPshiftFreq(c,l);
        }

        System.out.format("%s, pi: %s, pb: %s\n",ip,pi,pb);
        return empty(c,"mkTextField");

    }

    private Node formatPshiftFreq(UIElements.TextField c, Label l) {
        Property<Integer> pCoarse = resolveDepParam(c, 0);
        Property<Integer> pFine = resolveDepParam(c, 1);
        ChangeListener<Integer> listener =
                mkFreqFormatListener(l, pCoarse, pFine, new SimpleObjectProperty<>(4));
        pCoarse.addListener(listener);
        pFine.addListener(listener);
        return l;
    }

    private Node formatPulseTime(UIElements.TextField c, Label l) {
        Property<Integer> pTime = resolveDepParam(c,0);
        Property<Integer> pRange = resolveDepParam(c,1);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            double t = PULSE_DELAY_RANGE[pTime.getValue()];
            l.setText(formatMillisSecs(switch (pRange.getValue()) {
                case 0 -> t/100;
                case 1 -> t/10;
                default -> t;
            }));
        };
        pTime.addListener(listener);
        pRange.addListener(listener);
        return l;
    }

    private Node formatClkTempo(UIElements.TextField c, Label l) {
        Property<Integer> pRateBpm = resolveDepParam(c,0);
        Property<Boolean> pActive = resolveBoolDepParam(c,1);
        Property<Integer> pSource = resolveDepParam(c,2);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            l.setText(!pActive.getValue() ? "--" : pSource.getValue() == 1 ? "MASTER" :
                    (g2BPM(pRateBpm.getValue()) + " BPM"));
        };
        pRateBpm.addListener(listener);
        pActive.addListener((cc, o, n) -> listener.changed(null,0,0));
        pSource.addListener(listener);
        return l;
    }

    private Node formatOperatorFreq(UIElements.TextField c, Label l) {
        Property<Integer> pCoarse = resolveDepParam(c,0);
        Property<Integer> pFine = resolveDepParam(c,1);
        Property<Integer> pRatio = resolveDepParam(c,2);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            int aValue = pCoarse.getValue();
            int iValue1 = pFine.getValue();
            // TODO these are both bananas, port logic anew
            if (pRatio.getValue()==0) {
                double Fact = aValue == 0 ? 0.5 : aValue;
                l.setText(String.format("x%.01f",Fact + Fact * iValue1 / 100));
            } else {
                l.setText(formatHz(Math.pow(10, Math.divideExact(aValue,4))));
            }
        };
        pCoarse.addListener(listener);
        pFine.addListener(listener);
        pRatio.addListener(listener);
        return l;
    }

    private Label formatLfoFreq(UIElements.TextField c, Label l) {
        Property<Integer> pRate = resolveDepParam(c,0);
        Property<Integer> pRange = resolveDepParam(c,1);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            int r = pRate.getValue();
            l.setText(switch (pRange.getValue()) {
                case 0 -> String.format("%.02f",699/(double)(r+1)); //Rate Sub
                case 1 -> r < 32 ? // Rate Lo
                        String.format("%.02fs",1/(0.0159 * Math.pow(2, (double) r / 12))) :
                        String.format("%.02fHz",0.0159 * Math.pow(2, (double) r / 12));
                case 2 -> String.format("%.01fHz",0.2555 * Math.pow(2, (double) r / 12)); // Rate Hi
                case 3 -> Integer.toString(g2BPM(r));
                default -> LFO_CLOCK_VALS[r/4];
            });
        };
        pRange.addListener(listener);
        pRate.addListener(listener);
        return l;
    }

    private static int g2BPM(int rateParam) {
        return rateParam <= 32 ? 24 + 2 * rateParam :
                rateParam <= 96 ? 88 + rateParam - 32 :
                        152 + (rateParam - 96) * 2;
    }

    private Label formatOscFreq(UIElements.TextField c, Label l) {
        Property<Integer> pCoarse = resolveDepParam(c, 0);
        Property<Integer> pFine = resolveDepParam(c, 1);
        Property<Integer> pMode = resolveDepParam(c, 2);
        ChangeListener<Integer> listener = mkFreqFormatListener(l, pCoarse, pFine, pMode);
        pCoarse.addListener(listener);
        pFine.addListener(listener);
        pMode.addListener(listener);
        return l;
    }

    private static ChangeListener<Integer> mkFreqFormatListener(
            Label l, Property<Integer> pCoarse, Property<Integer> pFine, Property<Integer> pMode) {
        return (cc, o, n) -> {
            StringBuilder result = new StringBuilder();
            final int coarse = pCoarse.getValue();
            final int fine = pFine.getValue();
            switch (pMode.getValue()) {
                case 0 -> { // Semi
                    formatFreq(coarse - 64, result);
                    result.append("  ");
                    formatFreq((fine - 64) * 100 / 128, result);
                }
                case 1 -> { // Freq
                    double exponent = (double) ((coarse - 69) + (fine - 64) / 128) / 12;
                    result.append(formatHz(440.0 * Math.pow(2, exponent)));
                }
                case 2 -> { // Fac
                    double exponent = (double) ((coarse - 64) + (fine - 64) / 128) / 12;
                    result.append(String.format("x%.03f", Math.pow(2, exponent)));
                }
                case 3 -> { // Part
                    if (coarse <= 32) {
                        double Exponent = (double) -(((32 - coarse) * 4) + 77 - (fine - 64) / 128) / 12;
                        result.append(String.format("x%.02fHz", 440.0 * Math.pow(2, Exponent)));
                    } else if (coarse <= 64) {
                        result.append("1:").append(64 - coarse + 1);
                    } else {
                        result.append(coarse - 64 + 1).append(":1");
                    }
                    result.append("  ");
                    formatFreq((fine - 64) * 100 / 128, result);
                }
                case 4 -> { // Semi PShift
                    int cv = coarse - 64;
                    if (cv < 0) {
                        result.append(String.format("%.02f", (double) cv / 4));
                    } else {
                        result.append("+").append(String.format("%.02f", (double) cv / 4));
                    }
                    result.append("  ");
                    formatFreq((fine - 64) * 100 / 128, result);
                }
            }
            l.setText(result.toString());
        };
    }

    private static void formatFreq(int iValue1, StringBuilder result) {
        if (iValue1 < 0) {
            result.append(iValue1);
        } else {
            result.append("+").append(iValue1);
        }
    }

    private Property<Integer> resolveDepParam(ControlDependencies c, int ix) {
        IndexParam ip = resolveParam(c.Dependencies().get(ix).index());
        Property<Integer> p = intProps.get(ip.index());
        if (p == null) { throw new IllegalArgumentException("resoveDepParam: no property found " + ip); }
        return p;
    }

    private Property<Boolean> resolveBoolDepParam(ControlDependencies c, int ix) {
        IndexParam ip = resolveParam(c.Dependencies().get(ix).index());
        Property<Boolean> p = boolProps.get(ip.index());
        if (p == null) { throw new IllegalArgumentException("resoveBoolDepParam: no property found " + ip); }
        return p;
    }

    private <T> Label formatParam(Label l, Property<T> p, Function<T,String> f) {
        p.addListener((c,o,n) -> l.setText(n != null ? f.apply(n) : ""));
        return l;
    }

    private Node mkButtonText(UIElements.ButtonText c, IndexParam ip) {
        if (ip.param().param() == ModParam.ActiveMonitor) {
            return mkPowerButton(c, ip);
        } else if (c.Images() != null) {
            return empty(c, "mkButtonText+Images");
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
            Knob knob = new Knob(ip.param.name(), scale, c.Type().isReset);
            layout(c, knob);
            bindIntParam(ip, knob, knob.getValueProperty(), knob.valueChangingProperty());
            return knob;
        }
        return empty(c, "Slider/Knob");
    }

    private void bindIntParam(IndexParam ip, Node ctl, Property<Integer> property, BooleanProperty changing) {
        bindVarControl(ip,intProps, property, v -> {
            Property<Integer> p =
                    new SimpleObjectProperty<>(ctl, varPropName(ip, v), null);
            moduleBridges.add(bridges.bridge(d -> patchModule.getParamValueProperty(v, ip.index),
                    new FxProperty.SimpleFxProperty<>(p, changing),
                    Iso.id()));
            return p;
        });
    }

    private String varPropName(IndexParam ip, int v) {
        return type.shortName + ":" + ip.param().name() + ":" + v;
    }

    private Node empty(UIElement e, String msg) {
        System.out.println(msg + " TODO: " + e + ": " + this);
        return new Pane();
    }

    private ToggleButton mkPowerButton(UIElements.ButtonText c, IndexParam ip) {
        final ToggleButton b = new PowerButton().getButton();
        return layout(c,mkToggle(ip, b, b.selectedProperty()));
    }

    private Node mkTextEditMomentary(UIElements.TextEdit c, IndexParam ip) {
        MomentaryButton b = withClass(new MomentaryButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,makeEditable(mkNoUndoToggle(ip,b,b.selectedProperty()), ip));
    }

    private Node mkTextEditToggle(UIElements.TextEdit c, IndexParam ip) {
        ToggleButton b =withClass(new ToggleButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,makeEditable(mkToggle(ip, b, b.selectedProperty()), ip));
    }

    private Node mkTextMomentary(UIElements.ButtonText c, IndexParam ip) {
        MomentaryButton b = withClass(new MomentaryButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        b.setPrefWidth(c.Width());
        return layout(c,mkNoUndoToggle(ip,b,b.selectedProperty()));
    }

    private ToggleButton mkTextToggle(UIElements.ButtonText c, IndexParam ip) {
        ToggleButton b = withClass(new ToggleButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
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

        button.setOnContextMenuRequested(e ->
                contextMenu.show(button, e.getScreenX(), e.getScreenY()));

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

    public record IndexParam(NamedParam param, int index) {}
    
    private IndexParam resolveParam(UIParamControl uc) {
        int cr = uc.CodeRef();
        if (cr > type.getParams().size()) { throw new IllegalArgumentException("resolveParam: bad index: " + uc); }
        NamedParam p = type.getParams().get(cr);
        if (!p.name().equals(uc.Control())) { throw new IllegalArgumentException("resolveParam: bad name: " + uc); }
        return new IndexParam(p,cr);
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
        return String.format("%s:%d:%s [%s:%s]",type,index,moduleSelector.name().getValue(),parent.getSlot(),area);
    }
}
