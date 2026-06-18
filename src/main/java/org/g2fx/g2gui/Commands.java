package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2gui.window.*;
import org.g2fx.g2lib.device.Devices;
import org.g2fx.g2lib.device.LibExecutor;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.state.Slot;

import java.io.File;
import java.util.*;

import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;

public class Commands {

    public static final String PREF_RECENT_FILES = "recentFiles";
    public static final int MAX_RECENT_FILES = 15;
    private static final DataFormat MODULE_PANES_FMT = new DataFormat("application/x-g2fx-module-panes");

    private final Devices devices;

    private final Slots slots;
    private final Undos undos;
    private ScriptWindow scriptWindow;
    private final Set<File> recentFiles = new LinkedHashSet<>();
    private ParameterOverview parameterOverview;
    private PatchSettingsWindow patchSettings;
    private PerformanceSettingsWindow perfSettings;
    private PatchBrowser patchBrowser;


    private final List<Menus> allMenus = new ArrayList<>();


    public class Menus {
        private final MenuBar menuBar;
        private Menu recentFilesMenu;

        public Menus(Stage stage) {
            menuBar = new MenuBar();

            menuBar.setUseSystemMenuBar(true);

            Menu fileMenu = populateFileMenu(stage);

            Menu editMenu = populateEditMenu();

            Menu patchMenu = populatePatchMenu();

            Menu perfMenu = populatePerfMenu();

            Menu toolsMenu = populateToolsMenu(stage);

            menuBar.getMenus().addAll(fileMenu,editMenu,patchMenu,perfMenu,toolsMenu);

        }

        private Menu populateFileMenu(Stage stage) {

            recentFilesMenu = new Menu("Recent Files");
            populateRecentFiles();

            MenuItem openItem = mkMenuItem("Open...",shortcutKey(KeyCode.O), _ -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open File");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("G2 Perf/Patch Files (*.prf2, *.pch2)", "*.prf2", "*.pch2")
                );
                File f = fileChooser.showOpenDialog(stage);
                if (f != null) { loadFile(f); }
            });

            return mkMenu("File",
                    mkMenuItem("New Performance", execute(devices::newPerformance)),
                    openItem,
                    mkMenuItem("Save",shortcutKey(KeyCode.S), _ -> savePerf(stage)),
                    recentFilesMenu);
        }

        private void populateRecentFiles() {
            recentFilesMenu.getItems().clear();

            int i = 0;
            for (File rf : new ArrayList<>(recentFiles).reversed()) {

                MenuItem mi = new MenuItem(rf.getName());
                recentFilesMenu.getItems().add(mi);
                if (i==0) {
                    mi.setAccelerator(shortcutKey(KeyCode.O,SHIFT_DOWN));
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

            devices.execute(() -> devices.loadFile(f.getAbsolutePath(), null));
        }


        public MenuBar getMenuBar() {
            return menuBar;
        }
    }

    private EventHandler<ActionEvent> execute(LibExecutor.ThrowingRunnable r) {
        return _ -> devices.execute(r);
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


    private void savePerf(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Performance File");
        String name = devices.invokeWithCurrentPerf(d -> d.perfName().get());
        fileChooser.setInitialFileName(name+".prf2");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("G2 Perf Files (*.prf2)", "*.prf2")
        );
        File f = fileChooser.showSaveDialog(stage);
        if (f == null) { return; }
        devices.runWithCurrentPerf(d -> d.writeToFile(f));
    }

    private Menu populatePerfMenu() {
        return mkMenu("Performance",
                mkMenuItem("Performance Settings", shortcutKey(KeyCode.R), _ -> perfSettings.show()));
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
        return mkMenu("Patch",
                mkMenuItem("Patch settings", shortcutKey(KeyCode.P), _ -> patchSettings.show())
        );
    }

    private Menu populateToolsMenu(Stage stage) {

        MenuItem dumpYaml = mkMenuItem("Dump Yaml", _ -> {
            String pname = devices.invokeWithCurrentPerf(Performance::getName);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Dump Yaml File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("YAML Files", "*.yaml"));
            fileChooser.setInitialFileName(pname + ".yaml");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                devices.runWithCurrentPerf(d ->
                        d.dumpYaml(file.getAbsolutePath()));
            }});


        return mkMenu("Tools",
                mkMenuItem("Parameter Overview", shortcutKey(KeyCode.L), _ -> parameterOverview.show()),
                mkMenuItem("Patch Browser", shortcutKey(KeyCode.B), _ -> patchBrowser.show()),
                dumpYaml,
                mkMenuItem("Scripts", _ -> scriptWindow.show()));
    }

    private Menu populateEditMenu() {
        return mkMenu("Edit",
                mkMenuItem("Undo",shortcutKey(KeyCode.Z), _ -> undos.undo()),
                mkMenuItem("Redo",shortcutKey(KeyCode.Z, SHIFT_DOWN), _ -> undos.redo()),
                mkMenuItem("Cut",shortcutKey(KeyCode.X),_ -> doCut()),
                mkMenuItem("Copy",shortcutKey(KeyCode.C), _ -> doCopy()),
                mkMenuItem("Paste",shortcutKey(KeyCode.V), _ -> doPaste()),
                mkMenuItem("Delete",_ -> doDelete())
        );
    }

    private void doDelete() {
        slots.doDelete();
    }

    private void doCut() {
        Clipboard.getSystemClipboard().setContent(Map.of(MODULE_PANES_FMT, slots.doCut()));
    }

    private void doCopy() {
        Clipboard.getSystemClipboard().setContent(Map.of(MODULE_PANES_FMT, slots.doCopy()));
        //could do hasString() test here when wanting to support text and mids is empty
    }

    private void doPaste() {
        Clipboard c = Clipboard.getSystemClipboard();
        if (c.hasContent(MODULE_PANES_FMT)) {
            slots.doPaste();
        }
    }


    private void todo(String s) { System.out.println("menu item TODO: " + s);}


    private KeyCombination shortcutKey(KeyCode code, KeyCombination.Modifier... mods) {
        KeyCombination.Modifier[] ms = new KeyCombination.Modifier[mods.length+1];
        ms[0] = SHORTCUT_DOWN;
        System.arraycopy(mods, 0, ms, 1, mods.length);
        return new KeyCodeCombination(code, ms);
    }



    public MenuBar setupMenu(Stage stage) {
        Menus menus = new Menus(stage);
        allMenus.add(menus);
        return menus.getMenuBar();
    }


    public static Menu mkMenu(String name, MenuItem... items) {
        Menu i = new Menu(name);
        i.getItems().addAll(items);
        return i;
    }




    public static MenuItem mkMenuItem(String name, KeyCombination shortcut, EventHandler<ActionEvent> action) {
        MenuItem i = mkMenuItem(name,action);
        i.setAccelerator(shortcut);
        return i;
    }
    public static MenuItem mkMenuItem(String name, EventHandler<ActionEvent> action) {
        MenuItem repl = new MenuItem(name);
        repl.setOnAction(action);
        return repl;
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
                        e.consume();
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
                case KeyCode.ESCAPE:
                    slots.clearPaste();
                    e.consume();
                    return;
                case KeyCode.BACK_SPACE:
                    doDelete();
                    e.consume();
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


    public void setPatchBrowser(PatchBrowser patchBrowser) {
        this.patchBrowser = setupWindowMenu(patchBrowser);
    }
}
