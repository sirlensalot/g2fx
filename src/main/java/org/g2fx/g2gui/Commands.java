package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2gui.window.*;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.state.Slot;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Commands {

    public static final String PREF_RECENT_FILES = "recentFiles";
    public static final int MAX_RECENT_FILES = 15;


    private final Devices devices;

    private final Slots slots;
    private final Undos undos;
    private ScriptWindow scriptWindow;
    private final Set<File> recentFiles = new LinkedHashSet<>();
    private ParameterOverview parameterOverview;
    private PatchSettingsWindow patchSettings;
    private PerformanceSettingsWindow perfSettings;

    private final List<Menus> allMenus = new ArrayList<>();


    public class Menus {
        private final MenuBar menuBar;
        private Menu recentFilesMenu;

        public Menus(Stage stage) {
            menuBar = new MenuBar();

            menuBar.setUseSystemMenuBar(true);

            Menu editMenu = populateEditMenu();

            Menu fileMenu = populateFileMenu(stage);

            Menu patchMenu = populatePatchMenu();

            Menu toolsMenu = populateToolsMenu(stage);

            Menu perfMenu = populatePerfMenu();

            menuBar.getMenus().addAll(fileMenu,editMenu,patchMenu,perfMenu,toolsMenu);

        }

        private Menu populateFileMenu(Stage stage) {
            Menu fileMenu = new Menu("File");
            recentFilesMenu = new Menu("Recent Files");
            populateRecentFiles();

            MenuItem openItem = new MenuItem("Open...");

            fileMenu.getItems().addAll(openItem, recentFilesMenu);

            openItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
            openItem.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open File");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("G2 Perf Files (*.prf2)", "*.prf2")
                );
                File f = fileChooser.showOpenDialog(stage);
                if (f != null) { loadFile(f); }

            });
            return fileMenu;
        }

        private void populateRecentFiles() {
            recentFilesMenu.getItems().clear();

            int i = 0;
            for (File rf : new ArrayList<>(recentFiles).reversed()) {

                MenuItem mi = new MenuItem(rf.getName());
                recentFilesMenu.getItems().add(mi);
                if (i==0) {
                    mi.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+O"));
                }
                mi.setOnAction(e -> {
                    loadFile(rf);
                });

                if (i++>MAX_RECENT_FILES) { break; }
            }
            menuBar.setUseSystemMenuBar(false);
            menuBar.setUseSystemMenuBar(true);
        }


        private void loadFile(File f) {

            recentFiles.remove(f);
            recentFiles.add(f);

            Platform.runLater(Commands.this::updateRecentFiles);

            devices.execute(true,() -> devices.loadFile(f.getAbsolutePath()));
        }


        public MenuBar getMenuBar() {
            return menuBar;
        }
    }

    private Menu populatePerfMenu() {
        Menu perfMenu = new Menu("Performance");
        perfMenu.getItems().addAll(mkMenuItem("Performance Settings",e -> perfSettings.show(),
                KeyCombination.keyCombination("Shortcut+R")));
        return perfMenu;
    }


    public Commands(Devices devices, Slots slots, Undos undos) {
        this.devices = devices;
        this.slots = slots;
        this.undos = undos;
        String recentFilesString = FXUtil.getPrefs().get(PREF_RECENT_FILES, "");
        if (!recentFilesString.isEmpty()) {
            for (String path : List.of(recentFilesString.split("\n")).reversed()) {
                recentFiles.add(new File(path));
            }
        }
    }


    public MenuBar setupMenu(Stage stage) {
        Menus menus = new Menus(stage);
        allMenus.add(menus);
        return menus.getMenuBar();
    }

    public void updateRecentFiles() {
        allMenus.forEach(Menus::populateRecentFiles);
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (File rf : new ArrayList<>(recentFiles).reversed()) {
            if (!sb.isEmpty()) { sb.append("\n"); }
            sb.append(rf.getAbsolutePath());
            if (i++> MAX_RECENT_FILES) { break; }
        }
        FXUtil.getPrefs().put(PREF_RECENT_FILES, sb.toString());
    }

    private Menu populatePatchMenu() {
        Menu menu = new Menu("Patch");
        menu.getItems().addAll(
                mkMenuItem("Patch settings",e -> patchSettings.show(),
                        KeyCombination.keyCombination("Shortcut+P"))
        );
        return menu;
    }

    private Menu populateToolsMenu(Stage stage) {
        Menu menu = new Menu("Tools");
        MenuItem dumpYaml = new MenuItem("Dump Yaml");
        dumpYaml.setOnAction(e -> {
            String pname = devices.invoke(true,() -> devices.withCurrent(d -> d.getPerf().getName()));
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Dump Yaml File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("YAML Files", "*.yaml"));
            fileChooser.setInitialFileName(pname + ".yaml");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                devices.execute(true,() -> devices.runWithCurrent(d ->
                        d.getPerf().dumpYaml(file.getAbsolutePath())));

            }});


        menu.getItems().addAll(
                mkMenuItem("Parameter Overview",e -> parameterOverview.show(),
                        KeyCombination.keyCombination("Shortcut+L")),
                dumpYaml,
                mkMenuItem("Scripts", e -> scriptWindow.show()));
        return menu;
    }

    private static MenuItem mkMenuItem(String name, EventHandler<ActionEvent> action,KeyCombination shortcut) {
        MenuItem i = mkMenuItem(name,action);
        i.setAccelerator(shortcut);
        return i;
    }
    private static MenuItem mkMenuItem(String name, EventHandler<ActionEvent> action) {
        MenuItem repl = new MenuItem(name);
        repl.setOnAction(action);
        return repl;
    }

    private Menu populateEditMenu() {
        Menu editMenu = new Menu("Edit");
        MenuItem undo = new MenuItem("Undo");
        undo.setAccelerator(KeyCombination.keyCombination("Shortcut+Z"));
        undo.setOnAction(e -> undos.undo());
        MenuItem redo = new MenuItem("Redo");
        redo.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+Z"));
        redo.setOnAction(e -> undos.redo());
        editMenu.getItems().addAll(undo,redo);
        return editMenu;
    }



    public FXUtil.TextFieldFocusListener setupKeyBindings(Stage stage) {
        EventHandler<? super KeyEvent> globalKeyListener = e -> {
            KeyCode code = e.getCode();
            boolean anyModifiers = e.isShiftDown() || e.isShortcutDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown();
            switch (code) {
                case F:
                case V:
                    if (!anyModifiers) {
                        AreaId area = code == KeyCode.F ? AreaId.Fx : AreaId.Voice;
                        setArea(area);
                        e.consume();
                    }
                    return;
                case A:
                case B:
                case C:
                case D:
                    if (!anyModifiers) {
                        setSlot(Slot.fromIndex(code.getCode() - KeyCode.A.getCode()));
                        e.consume();
                    }
                    return;
                case SPACE:
                    if (!anyModifiers) {
                        slots.getSelectedSlotPane().toggleShowCables();
                        e.consume();
                    } else if (e.isShortcutDown()) {
                        slots.getSelectedSlotPane().manageCables(true);
                    }
                    return;
                case KeyCode.DIGIT1:
                case KeyCode.DIGIT2:
                case KeyCode.DIGIT3:
                case KeyCode.DIGIT4:
                case KeyCode.DIGIT5:
                case KeyCode.DIGIT6:
                case KeyCode.DIGIT7:
                case KeyCode.DIGIT8:
                    if (!anyModifiers) {
                        setVar(code.getCode() - KeyCode.DIGIT1.getCode());
                        e.consume();
                    }
                    return;

            }
        };

        return acquired -> {
            if (acquired) {
                stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, globalKeyListener);
            } else {
                stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, globalKeyListener);
            }
        };
    }

    public void setArea(AreaId area) {
        slots.getSelectedSlotPane().maximizeAreaPane(area);
    }

    public void setSlot(Slot slot) {
        slots.selectSlot(slot.ordinal());
    }

    public void setVar(int v) {
        slots.getSelectedSlotPane().selectVar(v);
    }

    public void setScriptWindow(ScriptWindow scriptWindow) {
        this.scriptWindow = setupWindowMenu(scriptWindow);
    }


    public void selectModule(Slot slot,AreaId area,int idx) {
        slots.getSlot(slot).getAreaPane(area).selectModule(idx);
    }

    private <T extends G2Window> T setupWindowMenu(T window) {
        window.setMenu(this::setupMenu);
        return window;
    }

    public void setParameterOverview(ParameterOverview parameterOverview) {
        this.parameterOverview = setupWindowMenu(parameterOverview);
    }

    public void setPatchSettings(PatchSettingsWindow patchSettings) {
        this.patchSettings = setupWindowMenu(patchSettings);
    }

    public void setPerfSettings(PerformanceSettingsWindow perfSettings) {
        this.perfSettings = setupWindowMenu(perfSettings);
    }
}
