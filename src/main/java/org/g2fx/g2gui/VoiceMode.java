package org.g2fx.g2gui;

public enum VoiceMode {
    Mono {
        @Override
        public String getDisplayName(int i) {
            return name();
        }

        @Override
        public int getMonoPoly() {
            return 1;
        }

        @Override
        public int getVoices() {
            return 1;
        }
    },
    Legato {
        @Override
        public String getDisplayName(int i) {
            return name();
        }

        @Override
        public int getMonoPoly() {
            return 2;
        }

        @Override
        public int getVoices() {
            return 1;
        }
    },
    P2, P3, P4, P5, P6, P7, P8, P9,
    P10, P11, P12, P13, P14, P15, P16, P17, P18, P19,
    P20, P21, P22, P23, P24, P25, P26, P27, P28, P29,
    P30, P31, P32;

    public String getDisplayName(int assigned) {
        return ordinal() + " (" + assigned + ")";
    }
    public static VoiceMode fromMonoPolyAndVoices(int monoPoly, int voices) {
        if (monoPoly == 1) return Mono;
        if (monoPoly == 2) return Legato;
        if (monoPoly == 0 && voices >= 2 && voices <= 32) { return values()[voices]; }
        throw new IllegalArgumentException("Bad monoPoly, voices values: " + monoPoly + ", " + voices);
    }

    public int getMonoPoly() {
        return 0;
    }

    public int getVoices() {
        return ordinal();
    }
}
