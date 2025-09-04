package org.g2fx.g2lib.state;

import org.g2fx.g2lib.repl.Eval;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 * Implements an offline device. Subclass `UsbDevice` has all online functionality.
 */
public class Device {

    private static final Logger log = Util.getLogger(Device.class);

    protected Performance perf = new Performance();
    protected SynthSettings synthSettings = new SynthSettings();

    public Device() {}

    public Performance getPerf() {
        return perf;
    }

    public Eval.Path loadPerfFile(String filePath) throws Exception {
        perf = Performance.readFromFile(filePath);
        String name = new File(filePath).getName();
        String pn = name.substring(0, name.length() - 5);
        perf.setFileName(pn);
        if (online()) {
            sendPerf();
        }
        return getPath();
    }

    private void sendPerf() {
        //TODO
    }

    public boolean online() {
        return false;
    }


    public void dumpEntries(PrintWriter writer, UsbDevice.EntryType type, int bank) { }


    public void sendStartStopComm(boolean start) throws Exception { }

    public void loadEntry(int slotCode, int bank, int entry) throws Exception { }


    public SynthSettings getSynthSettings() {
        return synthSettings;
    }


    public Eval.SlotPatch getSlotPatch (Slot s) {
        return new Eval.SlotPatch(s,perf.getPerfSettings().getSlotSettings(s).patchName().get());
    }

    private int getVariation() {
        return perf.getSelectedPatch().getPatchSettings().variation().get();
    }

    private Performance assertPerf() {
        if (perf == null) { throw new IllegalStateException("No current performance"); }
        return perf;
    }

    public Eval.Path getPath() {
        return new Eval.Path(online() ? "online" : "offline",
            assertPerf().getName(), getSlotPatch(assertPerf().getSelectedSlot()),getVariation(),AreaId.Voice,null,null);
    }






}
