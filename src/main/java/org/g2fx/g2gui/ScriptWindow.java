package org.g2fx.g2gui;

import javafx.application.Platform;
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
import org.g2fx.g2lib.repl.EvalResult;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.util.Util;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptWindow {

    private static final String PREF_SCRIPT = "ScriptText";
    private final Stage dialogStage;
    private final Eval eval;
    private final Logger log = Util.getLogger(getClass());
    private final CodeArea codeArea;
    private final StringWriter stringWriter;
    private final TextArea consoleOutput;

    private final Executor waitExecutor = Executors.newSingleThreadExecutor();
    private Integer waiting = null;

    public ScriptWindow(Devices devices,Commands commands) throws Exception {

        dialogStage = new Stage();

        stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        eval = new Eval(devices,false,pw,commands);

        codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.setPrefHeight(600);

        String scriptText = FXUtil.getPrefs().get(PREF_SCRIPT, "");
        codeArea.appendText(scriptText);

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
        if (waiting != null) { return; }
        String script = codeArea.getText();
        FXUtil.getPrefs().put(PREF_SCRIPT, script);
        BufferedReader br = new BufferedReader(new StringReader(script));
        runEval(br);
    }

    private void runEval(BufferedReader br) {
        waiting = null;
        EvalResult r;
        try {
            r = eval.runScript(br);
        } catch (Exception ex) {
            log.log(Level.SEVERE,"script run failed",ex);
            r = EvalResult.evalContinue();
        }
        String output = stringWriter.toString();
        consoleOutput.setText(output);
        if (r.isWait()) {
            final int ms = waiting = r.waitMs();
            updateTitle();
            waitExecutor.execute(() -> {
                try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> runEval(br));
            });
        } else {
            stringWriter.getBuffer().setLength(0);
            updateTitle();
        }
    }

    private void updateTitle() {
        dialogStage.setTitle("Scripts: " + eval +
                (waiting == null ? "" : " [Wait " + waiting + "]"));
    }

    public void show() {
        dialogStage.show();
    }

    public void updatePath() {
        eval.updatePath();
        updateTitle();
    }
}
