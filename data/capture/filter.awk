BEGIN { i = -1 }

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
        if ($5 == "2c") { s = "Perf" }

        v = "v=" $6;
        if ($6 == "41") { v = "V_SYSTEM" }
        if ($6 == "42") { v = "V_NEW" }

        c = $7;
        if ($7 == "2e") { c = "O_SELECTED_PARAM" }
        if ($7 == "35") { c = "O_VERSION" }
        if ($7 == "37") { c = "O_CREATE" }
        if ($7 == "70") { c = "O_UNKNOWN6" }
        if ($7 == "71") { c = "O_RESOURCES_USED" }
        if ($7 == "10") { c = "O_PERF_SETTINGS" }
        if ($7 == "59") { c = "O_UNKNOWN2" }
        if ($7 == "4c") { c = "O_PARAMS" }
        if ($7 == "4f") { c = "O_PARAM_NAMES" }


        printf("%03d:OUT[3] %s %s %s[%s]\n",i,s,v,c,$7);
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
    }
    if (pos == "00" || pos == "08") { pos = "SlotA" }
    if (pos == "01" || pos == "09") { pos = "SlotB" }
    if (pos == "02" || pos == "0a") { pos = "SlotC" }
    if (pos == "03" || pos == "0b") { pos = "SlotD" }
    if (pos == "04" || pos == "0c") { pos = "Perf" }
    cc=cmd;
    if (ver=="40") { ver="V_VERSION" } else { ver="v=" ver }

    if (cmd == "36") { cmd = "I_VERSION1" }
    if (cmd == "1f") { cmd = "I_VERSION2" }
    if (cmd == "72") { cmd = "I_PATCH_LOAD_DATA" }
    if (cmd == "2f") { cmd = "I_SELECTED_PARAM" }
    if (cmd == "7f") { cmd = "I_OK" }
    if (cmd == "29") { cmd = "I_PERFORMANCE_NAME" }
    if (cmd == "1e") { cmd = "I_RESERVED_1E" }
    if (cmd == "4d") { cmd = "I_PARAMS" }
    if (cmd == "4d") { cmd = "I_PARAMS" }
    if (cmd == "5b") { cmd = "I_PARAM_NAMES" }
    if (cmd == "05") { cmd = "I_ASSIGNED_VOICES" }
    if (ty=="81" || ty=="82") {
        printf("%03d:%s %s %s %s[%s]\n",i,ep,pos,ver,cmd,cc);
    }
}

{
    printf("%04x  %s\n",("0x" $1) - 32,
        substr($0,6,length($0)-6))
}
