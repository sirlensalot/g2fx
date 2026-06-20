package org.g2fx.g2gui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point2D;
import javafx.scene.control.Control;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Pane;
import org.g2fx.g2gui.controls.Cables;
import org.g2fx.g2gui.controls.Connectors;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.panel.SlotPane;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.g2fx.g2gui.controls.CableColor.Red;
import static org.g2fx.g2gui.controls.Cables.Cable;
import static org.g2fx.g2gui.controls.Cables.CableDelta;
import static org.g2fx.g2gui.controls.Connectors.Conn;
import static org.g2fx.g2gui.ui.UIElements.Bandwidth;
import static org.g2fx.g2gui.ui.UIElements.Bandwidth.Dynamic;
import static org.g2fx.g2gui.ui.UIElements.Bandwidth.Static;
import static org.g2fx.g2gui.ui.UIElements.ConnectorType;
import static org.g2fx.g2gui.ui.UIElements.ConnectorType.Audio;
import static org.g2fx.g2gui.ui.UIElements.ConnectorType.Control;
import static org.g2fx.g2lib.model.Connector.PortType;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CablesTest {

    public static final BiConsumer<Connectors.Conn, ContextMenuEvent> CTX_MENU_HDLR = (_, _) -> {
    };
    private Pane areaPane;
    private SlotPane slotPane;
    private Cables cables;

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
        new JFXPanel();
    }
    @BeforeEach
    public void beforeEach() {
        areaPane = mock(Pane.class);
        when(areaPane.getChildren()).thenReturn(FXCollections.observableArrayList());
        slotPane = mock(SlotPane.class);
        cables = new Cables(slotPane, areaPane);
    }

    @Test
    void testUprate1Cable() {
        ModulePane m0 = mockModule(0,true,mkConn(0,Out,Audio,Static));
        ModulePane m1 = mockModule(1,false,mkConn(0,In,Control,Dynamic));
        Cable c00_10 = cables.addCable(m0.getConns(Out).getFirst(), m1.getConns(In).getFirst());
        CableDelta<Cable> delta = new CableDelta<>(Set.of(c00_10),true);
        cables.checkUprate(c00_10.destConn(),true,delta);
        assertEquals(Map.of(m1.getIndex(), true),delta.uprateChanges());
    }

    @Test
    void testUprateLoop() {
        // M3 >-\  <- new cable, M3 audio static
        // M0 -> M1 -> M2 -> loop to M0, all control/dynamic
        ModulePane m0 = mockModule(0,false,mkConn(0,In,Control,Dynamic),
                mkConn(0,Out,Control,Dynamic));
        ModulePane m1 = mockModule(1,false,mkConn(0,In,Control,Dynamic),mkConn(1,In,Control,Dynamic),
                mkConn(0,Out,Control,Dynamic));
        ModulePane m2 = mockModule(2,false,mkConn(0,In,Control,Dynamic),
                mkConn(0,Out,Control,Dynamic));
        ModulePane m3 = mockModule(3,false,
                mkConn(0,Out,Audio,Static));
        Cable c01 = cables.addCable(m0.getConns(Out).getFirst(),m1.getConns(In).getFirst());
        Cable c12 = cables.addCable(m1.getConns(Out).getFirst(),m2.getConns(In).getFirst());
        Cable c20 = cables.addCable(m2.getConns(Out).getFirst(),m0.getConns(In).getFirst());
        // adding cable now, TODO conform once cable-adding order settled
        cables.addCable(m3.getConns(Out).getFirst(),m1.getConns(In).get(1));
        CableDelta<Cable> delta = new CableDelta<>(Set.of(),true);
        // NB: loop is guarded when m0->m1 check finds m1 already newly-uprated. other invariants not fired
        cables.checkUprate(m1.getConns(In).get(1),true,delta);
        assertEquals(Map.of(0,true,1,true,2,true),delta.uprateChanges());
        assertEquals(Map.of(c01,Red,c12,Red,c20,Red),delta.colorChanges());
    }

    record ConnF(Function<ModulePane, Conn> f) {}

    private ConnF mkConn(int index, PortType portType, ConnectorType connType, Bandwidth bandwidth) {
        javafx.scene.control.Control c = mock(Control.class);
        when(c.localToParent(anyDouble(),anyDouble())).thenReturn(new Point2D(20,20));
        return new ConnF(m -> new Conn(portType,connType,bandwidth,c,index,m,CTX_MENU_HDLR));
    }

    private static ModulePane mockModule(int index, boolean uprate, ConnF... cfs) {
        ModulePane m0 = mock(ModulePane.class,"m" + index);
        Pane mp = mock(Pane.class);
        when(m0.getPane()).thenReturn(mp);
        SimpleBooleanProperty m0Uprate = new SimpleBooleanProperty(uprate);
        when(m0.uprate()).thenReturn(m0Uprate);
        when(m0.getIndex()).thenReturn(index);
        List<Conn> conns = Arrays.stream(cfs).sequential().map(cf -> cf.f.apply(m0)).toList();
        List<Conn> ins = conns.stream().filter(c -> c.portType() == In).toList();
        List<Conn> outs = conns.stream().filter(c -> c.portType() == Out).toList();
        when(m0.getConns(In)).thenReturn(ins);
        when(m0.getConns(Out)).thenReturn(outs);
        return m0;
    }
}
