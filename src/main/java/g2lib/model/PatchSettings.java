package g2lib.model;

import g2lib.Util;

import java.util.Map;

public class PatchSettings {

    /*
    Note for location 2 knobs/ccs:
    index < 2 -> morphs
      param 0-7: morph dials
      param 8-15: morph modes
    index 3-8: settings groups (gain,glide,bend,vib,arp,misc)

    (morphs do not appear to apply to location 2)
     */

    public enum Glide {
        Off,
        Auto,
        Normal;
        public static final Util.SafeLookup<Glide> LKP = Util.makeEnumLookup(values());
    }
    public enum Vibrato {
        Off,
        Wheel,
        AftTouch;
        public static final Util.SafeLookup<Vibrato> LKP = Util.makeEnumLookup(values());
    }
    public Glide glide = Glide.Off;
    public int glideTime = 0; // TODO default?

    public Vibrato vibrato = Vibrato.Off;
    public int vibCents = 0; // TODO default
    public int vibRate = 0; // TODO default

    public boolean arpEnable = false;
    public int arpTime = 0; //TODO default
    public int arpType = 0; //TODO values, default
    public int arpOctaves = 1;

    public int volume = 100; //TODO default
    public boolean active = true;

    public boolean bendEnable = true;
    public int bendSemi = 1; //TODO default not 2??

    public int octShift = 2; // means 0

    public boolean sustainPedal = true;

}
