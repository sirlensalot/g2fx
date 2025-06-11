package org.g2fx.g2gui;

import g2lib.state.Devices;
import g2lib.state.Slot;
import g2lib.state.SynthSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class G2GuiApplication extends Application {

    public static final String TITLE = "g2fx nord modular g2 editor";
    public final Label welcomeText = new Label();
    private Stage stage;

    private Devices devices;

    private Node fontPane;

    @Override
    public void init() throws Exception {
        devices = new Devices();
        devices.addListener(d -> {
            Platform.runLater(() -> {
                SynthSettings ss = d.getSynthSettings();
                stage.setTitle(TITLE + ": " +
                        (ss == null ? "no settings" : ss.getDeviceName()));
            });
        });
    }

    @Override
    public void start(Stage stage) throws IOException {

        List<Tab> slots = new ArrayList<>();
        for (Slot slot : Slot.values()) {

            VBox pb = mkPatchBox(slot);
            Tab t = withClass(new Tab(slot.name(), pb),"slot-tab");
            t.setClosable(false);
            slots.add(t);

        }

        TabPane slotPager = withClass(new TabPane(slots.toArray(new Tab[]{})), "slots-pane"); // maybe a decent slot control
        //slotPager.setPageFactory(slots::get);

        HBox editorBar = withClass(new HBox(new Label("editorBar")),"editor-bar","bar","gfont");
        HBox globalBar = withClass(new HBox(new Label("globalBar")),"global-bar","bar","gfont");
        VBox topBox = withClass(
                new VBox(globalBar,editorBar,slotPager),"top-box");
        VBox.setVgrow(slotPager, Priority.ALWAYS);


        Scene scene = new Scene(topBox, 1280, 775);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("g2fx.css").toExternalForm());
        stage.show();
        this.stage = stage;
        devices.start();


    }


    private static <T extends Styleable> T withClass(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    private static VBox mkPatchBox(Slot slot) {
        Pane voicePane = withClass(
                new Pane(new Label("voice")),"voice-pane","area-pane","gfont"); // fixed-size area pane (although maybe no scroll unless modules are outside)
        ScrollPane voiceScroll =
                withClass(new ScrollPane(voicePane),"voice-scroll","area-scroll"); // scroll for area. investigate pannable. can prob use ctor instead of setContent

        Pane fxPane = withClass(new Pane(new Label("fx")),"fx-pane","area-pane","gfont");
        ScrollPane fxScroll = withClass(new ScrollPane(fxPane),"fx-scroll","area-scroll");

        SplitPane patchSplit =
                withClass(new SplitPane(voiceScroll,fxScroll),"patch-split"); // voice + fx
        patchSplit.setOrientation(Orientation.VERTICAL);
        HBox patchBar = withClass(new HBox(new Label("patchBar " + slot)),"patch-bar","bar","gfont");
        VBox patchBox = withClass(new VBox(patchBar,patchSplit),"patch-box"); // patch top bar + uis

        VBox.setVgrow(patchSplit,Priority.ALWAYS);
        return patchBox;
    }

    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @Override
    public void stop() throws Exception {
        devices.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
