package org.g2fx.g2gui;

public sealed interface VoiceMode permits VoiceMode.Legato, VoiceMode.Mono, VoiceMode.Poly {

    default String getDisplayName(int assigned) {
        return getClass().getSimpleName();
    }

    int getMonoPoly();

    default int getVoices() {
        return 0;
    }

    VoiceMode MONO = new Mono();
    VoiceMode LEGATO = new Legato();

    VoiceMode[] ALL = mkAll();

    private static VoiceMode[] mkAll() {
        VoiceMode[] all = new VoiceMode[33];
        all[0] = MONO;
        all[1] = LEGATO;
        for (int i = 2; i < 33; i++) {
            all[i] = new Poly(i);
        }
        return all;
    }

    static VoiceMode fromMonoPolyAndVoices(int monoPoly, int voices) {
        if (monoPoly == 1) return MONO;
        if (monoPoly == 2) return LEGATO;
        // admit invalid, ephemeral cases where monoPoly == 0 but voices == 1, etc
        // TODO look at the lib property being a composite
        if (monoPoly != 0 || voices < 0 || voices > 32) {
            throw new IllegalArgumentException("fromMonoPolyAndVoices: bad arguments: " + monoPoly + "," + voices);
        }
        return ALL[voices];
    }


    record Mono() implements VoiceMode {
        @Override
        public int getMonoPoly() {
            return 1;
        }
    }

    record Legato() implements VoiceMode {
        @Override
        public int getMonoPoly() {
            return 2;
        }
    }

    record Poly(int voices) implements VoiceMode {
        @Override
        public int getMonoPoly() {
            return 0;
        }

        @Override
        public int getVoices() {
            return voices;
        }

        @Override
        public String getDisplayName(int assigned) {
            return voices + " (" + assigned + ")";
        }
    }
}
