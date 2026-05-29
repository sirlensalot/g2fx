package org.g2fx.g2gui.window;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2lib.state.Entries;

import java.util.*;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;


/**
 * Patch Browser UI with Performance and Patch tabs
 * Each tab displays a TreeView of Banks (root) and Entries (children)
 */
public class PatchBrowser implements G2Window {

    private final VBox root;
    private final Stage primaryStage;

    enum ItemType {
        Bank,
        Entry
    }

    record TreeNode (Entries.EntryType tab, ItemType type, String name, int index, int parent) {
        @Override
        public String toString() {
            return name;
        }
        int index1() { return index+1; }
        int bank1() { return parent+1; }
    }

    private final Map<Entries.EntryType,List<TreeItem<TreeNode>>> banks = new TreeMap<>();
    private final Slots slots;

    private Map<Entries.EntryType, Map<Integer, Map<Integer, Entries.Entry>>> entries = null;

    private ObjectProperty<Entries.EntriesEvent> eventProperty = new SimpleObjectProperty<>();

    private ContextMenu contextMenu;

    public PatchBrowser(Slots slots, Bridges bridges) {
        this.slots = slots;

        primaryStage = new Stage();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab performanceTab = new Tab("Performance");
        performanceTab.setClosable(false);
        performanceTab.setContent(createTreeView(Entries.EntryType.Performance)); // false = performance

        Tab patchTab = new Tab("Patch");
        patchTab.setClosable(false);
        patchTab.setContent(createTreeView(Entries.EntryType.Patch)); // true = patch

        tabPane.getTabs().addAll(performanceTab, patchTab);

        bridges.bridge(eventProperty,d->d.getEntries().getEventProp());
        eventProperty.addListener((c,o,n) -> handleEvent(n));

        // Create main layout
        root = new VBox(10);
        root.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Create scene and stage
        Scene scene = new Scene(root, 250, 600);
        primaryStage.setTitle("Patch Browser");
        primaryStage.setScene(addGlobalStylesheet(scene));
        primaryStage.show();

        populateDummy();

    }

    private void handleEvent(Entries.EntriesEvent e) {
        switch (e.type()) {
            case RefreshAll -> updateEntries(e.entries());
            default -> System.out.println(e);
        }
    }

    private void populateDummy() {
        Map<Integer,Map<Integer, Entries.Entry>> dummy = new TreeMap<>();
        for (int b = 0; b < 32; b++) {
            Map<Integer, Entries.Entry> es = new HashMap<>();
            for (int e = 0; e < b%4; e++) {
                es.put(e+b,new Entries.Entry(e == 0 ? "Foo" : e == 1 ? "Bar" : "Baz",e));
            }
            dummy.put(b,es);
        }
        updateEntries(Map.of(Entries.EntryType.Performance,dummy, Entries.EntryType.Patch, dummy));
    }


    public void updateEntries(Map<Entries.EntryType,Map<Integer, Map<Integer, Entries.Entry>>> entries) {
        for (List<TreeItem<TreeNode>> bis : banks.values()) {
            bis.forEach(bi -> bi.getChildren().clear());
        }
        this.entries = entries;
        for (Entries.EntryType type : entries.keySet()) {
            for (TreeItem<TreeNode> bi : banks.get(type)) {
                TreeNode bn = bi.getValue();
                bi.getChildren().clear();
                Map<Integer, Map<Integer, Entries.Entry>> bankEs = entries.get(type);
                Map<Integer, Entries.Entry> es = bankEs.get(bn.index());
                if (es != null) {
                    for (Map.Entry<Integer, Entries.Entry> e : es.entrySet()) {
                        bi.getChildren().add(new TreeItem<>(new TreeNode(type, ItemType.Entry,
                                bn.index1() + "-" + (e.getKey()+1) + ": " + e.getValue().name(), e.getKey(),
                                bn.index
                        )));
                    }
                }
            }
        }
    }

    private TreeView<TreeNode> createTreeView(Entries.EntryType type) {
        TreeView<TreeNode> treeView = withClass(new TreeView<>(),"entries-tree");
        treeView.setShowRoot(false);

        contextMenu = new ContextMenu();
        treeView.setOnContextMenuRequested(event -> {
                createContextMenu(treeView.getSelectionModel().getSelectedItem().getValue());
                contextMenu.show(treeView,event.getScreenX(),event.getScreenY());
        });
        treeView.setOnMousePressed(e -> {
            contextMenu.hide();
            if (e.getClickCount()==2) {
                TreeNode n = treeView.getSelectionModel().getSelectedItem().getValue();
                if (n.type == ItemType.Entry) {
                    eventProperty.set(Entries.EntriesEvent.loadEntry(
                            n.tab, n.parent, n.index,slots.getSelectedSlotPane().getSlot()));
                }
            }
        });


        TreeItem<TreeNode> rootItem = new TreeItem<>(new TreeNode(type,null,"",-1,-1));
        treeView.setRoot(rootItem);

        int itemCount = type.getBanks();

        List<TreeItem<TreeNode>> banks = new ArrayList<>();

        for (int bankIdx = 0; bankIdx < itemCount; bankIdx++) {
            TreeItem<TreeNode> bankItem = new TreeItem<>(new TreeNode(type,ItemType.Bank,
                    "Bank " + (bankIdx +1),bankIdx,-1));
            bankItem.setExpanded(false);
            rootItem.getChildren().add(bankItem);
            banks.add(bankItem);
        }

        this.banks.put(type,banks);
        return treeView;
    }

    private void createContextMenu(TreeNode node) {
        if (contextMenu != null) {
            contextMenu.getItems().clear();
        }
        switch (node.type()) {
            case Bank -> createBankContextMenu(node);
            case Entry -> createEntryContextMenu(node);
        }
    }

    private void createEntryContextMenu(TreeNode node) {
        if (entries == null) { return; }
        MenuItem load = new MenuItem("Load");
        load.setOnAction(e -> eventProperty.set(Entries.EntriesEvent.loadEntry(
                node.tab,node.parent, node.index,slots.getSelectedSlotPane().getSlot())));
        Map<Integer, Entries.Entry> es = entries.get(node.tab).get(node.index());
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(e -> {
            if (confirmDelete(node.name())) {
                eventProperty.set(Entries.EntriesEvent.deleteEntry(node.tab,node.parent,node.index));
            }
        });
        MenuItem saveSubMenu = new MenuItem(
                "Save " +
                (node.tab == Entries.EntryType.Performance ? "current performance" :
                        "Slot " + slots.getSelectedSlotPane().getSlot()) +
                        " to " + node.name);
        saveSubMenu.setOnAction(e ->
            eventProperty.set(Entries.EntriesEvent.saveEntry(node.tab,node.index,node.parent)));
        contextMenu.getItems().addAll(load,deleteMenuItem,saveSubMenu,makeSortByMenu());

    }

    private void createBankContextMenu(TreeNode node) {

        Menu saveSubMenu = new Menu(
                node.tab == Entries.EntryType.Performance ?
                "Save current performance to" :
                "Save Slot " + slots.getSelectedSlotPane().getSlot() + " to");

        if (entries != null && ! entries.isEmpty()) {
            Map<Integer, Entries.Entry> es = entries.get(node.tab).get(node.index());
            if (es == null) { es = Map.of(); }
            for (int loc = 0; loc < 128; loc++) {
                MenuItem locItem = new MenuItem();
                Entries.Entry e1 = es.get(loc);
                locItem.setText((loc + 1) + (e1 == null ? " Empty Location" : " " + e1.name()));
                final int lloc = loc;
                locItem.setOnAction(ev -> {
                    eventProperty.set(Entries.EntriesEvent.saveEntry(node.tab,node.index,lloc));
                });
                saveSubMenu.getItems().add(locItem);
            }
        }

        MenuItem deleteMenuItem = new MenuItem("Delete Bank " + (node.index + 1));
        deleteMenuItem.setOnAction(e -> {
            if (confirmDelete("Bank " + (node.index+1))) {
                eventProperty.set(Entries.EntriesEvent.deleteBank(node.tab,node.index));
            }
        });

        Menu sortByMenu = makeSortByMenu();

        contextMenu.getItems().addAll(saveSubMenu, deleteMenuItem, sortByMenu);

    }

    public boolean confirmDelete(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm delete");
        alert.setHeaderText(null);
        alert.setContentText("Delete " + msg + "?");

        ButtonType deleteButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(deleteButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == deleteButton;
    }

    private static Menu makeSortByMenu() {
        Menu sortByMenu = new Menu("Sort By");

        MenuItem sortByProgram = new MenuItem("Program");
        sortByProgram.setOnAction(e -> System.out.println("Sort by Program"));

        MenuItem sortByName = new MenuItem("Name");
        sortByName.setOnAction(e -> System.out.println("Sort by Name"));

        MenuItem sortByCategory = new MenuItem("Category");
        sortByCategory.setOnAction(e -> System.out.println("Sort by Category"));

        sortByMenu.getItems().addAll(sortByProgram, sortByName, sortByCategory);
        return sortByMenu;
    }


    @Override
    public Stage getStage() {
        return primaryStage;
    }

    @Override
    public Pane getRoot() {
        return root;
    }

}
