package org.g2fx.g2lib.protocol;

public class Codes {

    // Inbound command codes (endpoints 81/82)
    public static final int I_SYNTH_SETTINGS = 0x03;
    public static final int I_ASSIGNED_VOICES = 0x05;
    public static final int I_CHANGE_SLOT = 0x09;
    public static final int I_ENTRY_LIST = 0x13;
    public static final int I_RESERVED_1E = 0x1e;
    public static final int I_VERSION_LOAD_PERF = 0x1f;
    public static final int I_PATCH_DESCRIPTION = 0x21;
    public static final int I_PATCH_NAME = 0x27;
    public static final int I_PERFORMANCE_NAME = 0x29;
    public static final int I_SEL_PARAM_PAGE = 0x2d;
    public static final int I_SELECTED_PARAM = 0x2f;
    public static final int I_VERSION_UPDATE = 0x36;
    public static final int I_VERSION_LOAD_PATCH = 0x38;
    public static final int I_LED_DATA = 0x39;
    public static final int I_VOLUME_DATA = 0x3a;
    public static final int I_SET_MASTER_CLOCK = 0x3f;
    public static final int I_SET_PARAM = 0x40;
    public static final int I_PARAMS = 0x4d;
    public static final int I_PARAM_LABELS = 0x5b;
    public static final int I_EXT_MASTER_CLOCK = 0x5d;
    public static final int I_GLOBAL_KNOB_ASSIGMENTS = 0x5f;
    public static final int I_CURRENT_NOTE = 0x69;
    public static final int I_CHANGE_VARIATION = 0x6a;
    public static final int I_TEXT_PAD = 0x6f;
    public static final int I_PATCH_LOAD_DATA = 0x72;
    public static final int I_OK = 0x7f;

    // "Magic version" in patch/perf version position
    public static final int V_VERSION = 0x40;
    // "System version" in perf requests
    public static final int V_SYSTEM = 0x41;
    // "New version" in perf create
    public static final int V_NEW_PERF = 0x42;
    // "New version" in patch create
    public static final int V_NEW_PATCH = 0x53;

    // Perf slot codes
    public static final int S_SLOT_00 = 0x00;
    public static final int S_PERF_04 = 0x04; // for slots 00-03
    public static final int S_SLOT_08 = 0x08;
    public static final int S_PERF_0C = 0x0c; // for slots 08-0b
    public static final int S_PERF_REQ = 0x2c; // for slots 28-2b
    public static final int S_SLOT_REQ = 0x28; // base for slot selector
    public static final int S_SLOT_CMD = 0x38; // base for no-response slot selector

    // Message types
    public static final int M_INIT = 0x80;
    public static final int M_CMD = 0x01;

    // Outbound command codes (endpoint 03)
    public static final int O_SYNTH_SETTINGS = 0x02;
    public static final int O_ASSIGNED_VOICES = 0x04;
    public static final int O_SELECT_SLOT = 0x09;
    public static final int O_LOAD_ENTRY = 0x0a;
    public static final int O_STORE_ENTRY = 0x0b;
    public static final int O_PERF_SETTINGS = 0x10;
    public static final int O_LIST_NAMES = 0x14;
    public static final int O_PATCH_NAME = 0x28;
    public static final int O_SELECTED_PARAM = 0x2e;
    public static final int O_VERSION = 0x35;
    public static final int O_MASTER_CLOCK = 0x3b;
    public static final int O_PATCH = 0x3c;
    public static final int O_CREATE = 0x37;
    public static final int O_PARAMS = 0x4c;
    public static final int O_PARAM_NAMES = 0x4f;
    public static final int O_UNKNOWN2 = 0x59;
    public static final int O_GLOBAL_KNOBS = 0x5e;
    public static final int O_CURRENT_NOTE = 0x68;
    public static final int O_PATCH_TEXT = 0x6e;
    public static final int O_UNKNOWN6 = 0x70;
    public static final int O_RESOURCES_USED = 0x71;
    public static final int O_START_STOP_COM = 0x7d;
    public static final int O_UNKNOWN1 = 0x81;


}
