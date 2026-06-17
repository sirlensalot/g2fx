BEGIN { i = -1 }

/Time since reference/ {
    ptime=sprintf("%s %s",substr($7,0,5),substr($8,0,3));
    next
}
/^[A-Z]/ { next }
/^   / { next }

/^0000/ { next }
/^$/ { next }

/^0010  / { ty=$16; next }
ty != "03" && ty != "81" && ty != "82" { next }

/^0020  .1/ && ty=="81" { next }

/^0020  / {
    i++;
    print "";
    if (ty=="03") {
        s = $5
        if ($5 == "28") { s = "SlotA" }
        if ($5 == "29") { s = "SlotB" }
        if ($5 == "2a") { s = "SlotC" }
        if ($5 == "2b") { s = "SlotD" }
        if ($5 == "38") { s = "SlotACmd" }
        if ($5 == "39") { s = "SlotBCmd" }
        if ($5 == "3a") { s = "SlotCCmd" }
        if ($5 == "3b") { s = "SlotDCmd" }
        if ($5 == "2c") { s = "Perf" }

        v = "v=" $6;
        if ($6 == "41") { v = "V_SYSTEM[41]" }
        if ($6 == "42") { v = "V_NEW_PERF[42]" }
        if ($6 == "53") { v = "V_NEW_PATCH[53]" }

        c = $7;
        cn = $7

        if ($7 == "02") { c = "O_SYNTH_SETTINGS" }
        if ($7 == "04") { c = "O_ASSIGNED_VOICES" }
        if ($7 == "09") { c = "O_SELECT_SLOT" }
        if ($7 == "0a") { c = "O_LOAD_ENTRY" }
        if ($7 == "0b") { c = "O_STORE_ENTRY" }
        if ($7 == "10") { c = "O_PERF_SETTINGS" }
        if ($7 == "14") { c = "O_LIST_NAMES" }
        if ($7 == "28") { c = "O_PATCH_NAME" }
        if ($7 == "2a") { c = "O_SET_UPRATE" }
        if ($7 == "2e") { c = "O_SELECTED_PARAM" }
        if ($7 == "2f") { c = "O_SELECT_PARAM" }
        if ($7 == "30") { c = "O_ADD_MODULE" }
        if ($7 == "35") { c = "O_VERSION" }
        if ($7 == "3b") { c = "O_MASTER_CLOCK" }
        if ($7 == "3c") { c = "O_PATCH" }
        if ($7 == "37") { c = "O_CREATE" }
        if ($7 == "4c") { c = "O_PARAMS" }
        if ($7 == "4f") { c = "O_PARAM_NAMES" }
        if ($7 == "50") { c = "O_ADD_CABLE" }
        if ($7 == "51") { c = "O_DELETE_CABLE" }
        if ($7 == "54") { c = "O_CABLE_COLOR" }
        if ($7 == "56") { c = "O_PLAY_NOTE" }
        if ($7 == "59") { c = "O_UNKNOWN2" }
        if ($7 == "5e") { c = "O_GLOBAL_KNOBS" }
        if ($7 == "6a") { c = "O_CHANGE_VARIATION" }
        if ($7 == "68") { c = "O_CURRENT_NOTE" }
        if ($7 == "6e") { c = "O_PATCH_TEXT" }
        if ($7 == "70") { c = "O_UNKNOWN6" }
        if ($7 == "71") { c = "O_RESOURCES_USED" }
        if ($7 == "7d") { c = "O_START_STOP_COM" }
        if ($7 == "81") { c = "O_UNKNOWN1" }

        if ($4 == "80") {
            c = "O_INIT"
            cn = "80"
            s = "SYS"
            v = "SYS"
        }

        printf("%03d:OUT[3] %s %s %s[%s]  %s\n",i,s,v,c,cn,ptime);
    }
    if (ty=="81") {
        ep="INI[81]"
        req=$3;
        pos=$4;
        ver=$5;
        cmd=$6;
    }
    if (ty=="82") {
        ep="INB[82]"
        req=$2;
        pos=$3;
        ver=$4;
        cmd=$5;
        if (req=="80") {
            cmd="I_INIT"
            ver="SYS"
            pos="SYS"
        }
    }
    if (pos == "00" || pos == "08") { pos = "SlotA" }
    if (pos == "01" || pos == "09") { pos = "SlotB" }
    if (pos == "02" || pos == "0a") { pos = "SlotC" }
    if (pos == "03" || pos == "0b") { pos = "SlotD" }
    if (pos == "04" || pos == "0c") { pos = "Perf" }
    cc=cmd;
    if (ver=="40") { ver="V_VERSION" } else { ver="v=" ver }

    if (cmd == "03") { cmd = "I_SYNTH_SETTINGS" }
    if (cmd == "05") { cmd = "I_ASSIGNED_VOICES" }
    if (cmd == "09") { cmd = "I_CHANGE_SLOT" }
    if (cmd == "13") { cmd = "I_ENTRY_LIST" }
    if (cmd == "1e") { cmd = "I_RESERVED_1E" }
    if (cmd == "1f") { cmd = "I_VERSION_LOAD_PERF" }
    if (cmd == "21") { cmd = "I_PATCH_DESCRIPTION" }
    if (cmd == "27") { cmd = "I_PATCH_NAME" }
    if (cmd == "29") { cmd = "I_PERFORMANCE_NAME" }
    if (cmd == "2f") { cmd = "I_SELECTED_PARAM" }
    if (cmd == "36") { cmd = "I_VERSION_UPDATE" }
    if (cmd == "38") { cmd = "I_VERSION_LOAD_PATCH" }
    if (cmd == "39") { cmd = "I_LED_DATA" }
    if (cmd == "3a") { cmd = "I_VOLUME_DATA" }
    if (cmd == "3f") { cmd = "I_SET_MASTER_CLOCK" }
    if (cmd == "40") { cmd = "I_SET_PARAM" }
    if (cmd == "4d") { cmd = "I_PARAMS" }
    if (cmd == "5b") { cmd = "I_PARAM_LABELS" }
    if (cmd == "5d") { cmd = "I_EXT_MASTER_CLOCK" }
    if (cmd == "5f") { cmd = "I_GLOBAL_KNOB_ASSIGMENTS" }
    if (cmd == "69") { cmd = "I_CURRENT_NOTE" }
    if (cmd == "6a") { cmd = "I_CHANGE_VARIATION" }
    if (cmd == "6f") { cmd = "I_TEXT_PAD" }
    if (cmd == "72") { cmd = "I_PATCH_LOAD_DATA" }
    if (cmd == "7f") { cmd = "I_OK" }
    if (cmd == "80") { cmd = "I_MIDI_CC?" }

    if (ty=="81" || ty=="82") {
        printf("%03d:%s %s %s %s[%s]  %s\n",i,ep,pos,ver,cmd,cc,ptime);
    }
}

{
    printf("%04x  %s\n",("0x" $1) - 32,
        substr($0,6,length($0)-5))
}
