package org.g2fx.g2gui.window;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.state.Entries;

import java.util.*;


/**
 * Patch Browser UI with Performance and Patch tabs
 * Each tab displays a TreeView of Banks (root) and Entries (children)
 */
public class PatchBrowser implements G2Window, Devices.DeviceListener {

    private final VBox root;
    private final Stage primaryStage;

    enum ItemType {
        Bank,
        Entry
    }

    record TreeNode (Entries.EntryType tab, ItemType type, String name, int index) {
        @Override
        public String toString() {
            return name;
        }
    }

    private final Map<Entries.EntryType,List<TreeItem<TreeNode>>> banks = new TreeMap<>();


    private Device device;
    private Map<Entries.EntryType, Map<Integer, Map<Integer, Entries.Entry>>> entries;

    public PatchBrowser() {

        primaryStage = new Stage();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab performanceTab = new Tab("Performance");
        performanceTab.setClosable(false);
        performanceTab.setContent(createTreeView(Entries.EntryType.Perf)); // false = performance

        Tab patchTab = new Tab("Patch");
        patchTab.setClosable(false);
        patchTab.setContent(createTreeView(Entries.EntryType.Patch)); // true = patch

        tabPane.getTabs().addAll(performanceTab, patchTab);

        // Create main layout
        root = new VBox(10);
        root.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Create scene and stage
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Patch Browser");
        primaryStage.setScene(scene);
        primaryStage.show();

        Map<Integer,Map<Integer, Entries.Entry>> dummy = new TreeMap<>();
        for (int b = 0; b < 32; b++) {
            Map<Integer, Entries.Entry> es = new HashMap<>();
            for (int e = 0; e < b%3; e++) {
                es.put(e+b,new Entries.Entry(e == 0 ? "Foo" : e == 1 ? "Bar" : "Baz",e));
            }
            dummy.put(b,es);
        }
        updateEntries(Map.of(Entries.EntryType.Perf,dummy, Entries.EntryType.Patch, dummy));

    }


    @Override
    public void onDeviceInitialized(Device d) throws Exception {
        device = d;
    }

    @Override
    public void onDeviceDisposal(Device d) throws Exception {
        device = null;
    }

    public void updateEntries(Map<Entries.EntryType,Map<Integer, Map<Integer, Entries.Entry>>> entries) {
        this.entries = entries;
        for (Entries.EntryType type : entries.keySet()) {
            for (TreeItem<TreeNode> bi : banks.get(type)) {
                TreeNode bn = bi.getValue();
                bi.getChildren().clear();
                for (Map.Entry<Integer, Entries.Entry> e : entries.get(type).get(bn.index()).entrySet()) {
                    bi.getChildren().add(new TreeItem<>(new TreeNode(type,ItemType.Entry,
                            bn.index + "-" + e.getKey() + ": " + e.getValue().name(),e.getKey()
                            )));
                }
            }
        }
    }

    private TreeView<TreeNode> createTreeView(Entries.EntryType type) {
        TreeView<TreeNode> treeView = new TreeView<>();
        treeView.setShowRoot(false); // No root node, banks are roots

        treeView.setOnContextMenuRequested(event ->
                createContextMenu(treeView.getSelectionModel().getSelectedItem().getValue()).show(
                        treeView,event.getScreenX(),event.getScreenY()));

        boolean isPatch = type == Entries.EntryType.Patch;
        String entryIcon = isPatch ? "\uD83D\uDCDC" : "\uD83C\uDFB5"; // Document vs Music note

        // Create root tree item (hidden)
        TreeItem<TreeNode> rootItem = new TreeItem<>(new TreeNode(type,null,"",0));
        treeView.setRoot(rootItem);

        int itemCount = type.getBanks();

        List<TreeItem<TreeNode>> banks = new ArrayList<>();
        // Add all banks as root children
        for (int bankIdx = 0; bankIdx < itemCount; bankIdx++) {
            TreeItem<TreeNode> bankItem = new TreeItem<>(new TreeNode(type,ItemType.Bank,
                    "Bank " + (bankIdx +1),bankIdx));
            bankItem.setExpanded(false);


            //bankItem.setGraphic(createFolderIcon(false, true));

//        // Add entries as children
//        for (Entry entry : entries) {
//            TreeItem<String> entryItem = createEntryTreeItem(entry, entryIcon);
//            bankItem.getChildren().add(entryItem);
//        }

            //bankItem.setContextMenu(createBankContextMenu(idx,isPatch,isPatch ? "Patch" : "Performance"));
            rootItem.getChildren().add(bankItem);
            banks.add(bankItem);
        }

        this.banks.put(type,banks);
        return treeView;
    }

    private ContextMenu createContextMenu(TreeNode node) {
        return switch (node.type()) {
            case Bank -> createBankContextMenu(node);
            case Entry -> createEntryContextMenu(node);
        };
    }

    private ContextMenu createEntryContextMenu(TreeNode node) {
        return new ContextMenu();
    }

    private ContextMenu createBankContextMenu(TreeNode node) {
        ContextMenu contextMenu = new ContextMenu();
        Menu saveSubMenu = new Menu("Save current " + node.tab + " to");

        if (entries != null) {
            Map<Integer, Entries.Entry> es = entries.get(node.tab).get(node.index());
            for (int loc = 0; loc < 128; loc++) {
                MenuItem locItem = new MenuItem();
                Entries.Entry e = es.get(loc);
                int loc1 = loc + 1;
                locItem.setText(loc1 + (e == null ?  " Empty Location" : " " + e.name()));
                locItem.setOnAction(ev -> {
                    System.out.println("Save current " + node.tab + " to location: " + (loc1-1));
                });
                saveSubMenu.getItems().add(locItem);
            }
        }

        MenuItem deleteMenuItem = new MenuItem("Delete Bank " + (node.index + 1));
        deleteMenuItem.setOnAction(e -> {
            System.out.println("Delete Bank " + node.index );
            // TODO: Implement delete logic
        });

        // Item 3: "Sort By" submenu
        Menu sortByMenu = new Menu("Sort By");

        MenuItem sortByProgram = new MenuItem("Program");
        sortByProgram.setOnAction(e -> System.out.println("Sort by Program"));

        MenuItem sortByName = new MenuItem("Name");
        sortByName.setOnAction(e -> System.out.println("Sort by Name"));

        MenuItem sortByCategory = new MenuItem("Category");
        sortByCategory.setOnAction(e -> System.out.println("Sort by Category"));

        sortByMenu.getItems().addAll(sortByProgram, sortByName, sortByCategory);

        contextMenu.getItems().addAll(saveSubMenu, deleteMenuItem, sortByMenu);

        return contextMenu;
    }



    /**
     * Create folder icon graphic for a bank
     */
    private Label createFolderIcon(boolean isOpen, boolean isEmpty) {
        String icon;
        if (isEmpty) {
            icon = "\u26AA"; // Grey circle for empty folder
        } else if (isOpen) {
            icon = "\uD83D\uDCC1"; // Open folder (U+1F4C1)
        } else {
            icon = "\uD83D\uDCC2"; // Closed folder (U+1F4C2)
        }
        
        Label label = new Label(icon);
        label.setStyle("-fx-font-size: 16px;");
        if (isEmpty) {
            label.setStyle(label.getStyle() + "-fx-text-fill: #888888;");
        }
        return label;
    }


    /**
     * Create entry icon graphic
     */
    private Label createEntryIcon(String icon) {
        Label label = new Label(icon);
        label.setStyle("-fx-font-size: 16px;");
        return label;
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
