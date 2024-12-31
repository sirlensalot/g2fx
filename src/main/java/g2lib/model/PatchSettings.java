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
        Auto,
        Normal,
        Off;
        public static final Util.SafeLookup<Glide> LKP = Util.makeEnumLookup(values());
    }
    public enum Vibrato {
        Wheel,
        AftTouch,
        Off;
        public static final Util.SafeLookup<Vibrato> LKP = Util.makeEnumLookup(values());
    }
    public enum ArpDirection {
        Up,
        Dn,
        UpDn,
        Rnd;
    }
    public enum ArpTime {
        T1_8,
        T1_8T,
        T1_16,
        T1_16T;
    }

    // Gain group
    public int volume = 100;
    public boolean active = true;

    // Glide group
    public Glide glide = Glide.Off;
    public int glideTime = 0;

    // Bend group
    public boolean bendEnable = true;
    public int bendSemi = 1; //TODO default not 2??

    // Vibrato group
    public Vibrato vibrato = Vibrato.Wheel;
    public int vibCents = 0; // TODO default
    public int vibRate = 0; // TODO default

    // Arp group
    public boolean arpEnable = false;
    public ArpTime arpTime = ArpTime.T1_8;
    public ArpDirection arpDir = ArpDirection.Up;
    public int arpOctaves = 1;

    public int octShift = 2; // means 0
    public boolean sustainPedal = true;

}
