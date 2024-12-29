package g2lib.model;

public class PatchSettings {
    public enum Glide {
        Auto,
        Normal,
        Off
    }
    public enum Vibrato {
        Wheel,
        AftTouch,
        Off
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
