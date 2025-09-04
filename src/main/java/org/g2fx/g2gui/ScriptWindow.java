package org.g2fx.g2gui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.g2fx.g2lib.repl.Eval;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.util.Util;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptWindow {

    private final Stage dialogStage;
    private final Eval eval;
    private final Logger log = Util.getLogger(getClass());
    private final CodeArea codeArea;
    private final StringWriter stringWriter;
    private final TextArea consoleOutput;

    public ScriptWindow(Devices devices) throws Exception {

        dialogStage = new Stage();

        stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        eval = new Eval(devices,false,pw);

        codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.setPrefHeight(600);

        // Create the output console area (JavaFX TextArea)
        consoleOutput = new TextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setPrefRowCount(36);

        // Button to simulate running the script or current line
        Button runButton = new Button("Run Script");
        runButton.setOnAction(e -> {
            // Example: just append the current text to console output
            runScript();
        });

        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            KeyCombination cmdEnter = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
            if (cmdEnter.match(event)) {
                runScript();
            }
        });
        // Layout: VBox with editor taking most space, console at bottom
        VBox root = new VBox(5, codeArea, runButton, consoleOutput);
        root.setPrefSize(600, 800);

        // Make the editor grow vertically, console fixed size
        VBox.setVgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);


        dialogStage.setTitle("Script Editor");
        dialogStage.setScene(new Scene(root));
        dialogStage.setWidth(600);
        dialogStage.setHeight(400);

        updatePath();

        dialogStage.show();
    }


    private void runScript() {
        String script = codeArea.getText();
        stringWriter.getBuffer().setLength(0);
        BufferedReader br = new BufferedReader(new StringReader(script));
        try {
            eval.runScript(br);
        } catch (Exception ex) {
            log.log(Level.SEVERE,"script run failed",ex);
        }
        String output = stringWriter.toString();
        consoleOutput.setText(output);
        updateTItle();
    }

    private void updateTItle() {
        dialogStage.setTitle("Script Editor: " +
                (eval.getPath() == null ? "offline" : eval.getPath()));
    }

    public void show() {
        dialogStage.show();
    }

    public void updatePath() {
        eval.updatePath();
        updateTItle();
    }
}
