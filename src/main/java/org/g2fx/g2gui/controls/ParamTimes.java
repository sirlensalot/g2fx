package org.g2fx.g2gui.controls;

import org.g2fx.g2lib.util.SafeLookup;

import static org.g2fx.g2lib.model.ParamConstants.DELAY_VALS;

public interface ParamTimes {

    enum DelayRange7 {
        Range5m, Range25m, Range100m, Range500m, Range1s, Range2s, Range2_7s;
        public static final SafeLookup<Integer,DelayRange7> LOOKUP =
                SafeLookup.makeEnumOrdLookup(values());
    }

    enum DelayRange4 {
        Range500m, Range1s, Range2s, Range2_7s;
        public static final SafeLookup<Integer,DelayRange4> LOOKUP =
                SafeLookup.makeEnumOrdLookup(values());
    }

    enum DelayRange3 {
        Range500m, Range1s, Range1_35s;
        public static final SafeLookup<Integer,DelayRange3> LOOKUP =
                SafeLookup.makeEnumOrdLookup(values());
    }

    static String delayDispValue(int type, int range, int val) {
        return switch (type) {
            case 0 -> formatDelayRange7(range, val);
            case 1 -> {
                double m = switch (range) {
                    case 0 -> 0.66f;
                    case 1 -> 3.14f;
                    case 2 -> 12.6f;
                    case 3 -> 62.5f;
                    case 4 -> 125f;
                    case 5 -> 250f;
                    case 6 -> 338f;
                    default -> 0f;
                };
                yield fmtDouble(m * val / 127, 4) + "m";
            }
            case 2 -> fmtDelayRange4(range, val);
            case 3 -> fmtDelayRange3(range, val);
            case 4 -> formatClkDelay(val);
            default -> throw new IllegalArgumentException("fmtDelayValue: invalid type: " + type);
        };
    }

    static String fmtDelayRange3(int range, int val) {
        return val == 0 ? "0.01m" : switch (DelayRange3.LOOKUP.get(range)) {
            case Range500m -> fmtDouble(500f * val / 127, 4) + "m";
            case Range1s -> val == 127 ? "1.00s" : fmtDouble(1000f * val / 127, 4) + "m";
            case Range1_35s -> {
                double f = 1351f;
                yield val >= 95 ? fmtDouble(f * val / 127000, 5) + "s" :
                        fmtDouble(f * val / 127, 4) + "m";
            }
        };
    }

    static String fmtDelayRange4(int range, int val) {
        return val == 0 ? "0.01m" : switch (DelayRange4.LOOKUP.get(range)) {
            case Range500m -> fmtDouble(500f * val / 127, 4) + "m";
            case Range1s -> val == 127 ? "1.00s" : fmtDouble(1000f * val / 127, 4) + "m";
            case Range2s -> {
                double f = 2000f;
                yield val >= 64 ? fmtDouble(f * val / 127000, 5) + "s" :
                        fmtDouble(f * val / 127, 4) + "m";
            }
            case Range2_7s -> {
                double f = 2700f;
                yield val >= 48 ? fmtDouble(f * val / 127000, 5) + "s" :
                        fmtDouble(f * val / 127, 4) + "m";
            }
        };
    }

    static String formatClkDelay(int val) {
        return DELAY_VALS[val / 4];
    }

    static String formatDelayRange7(int val, int range) {
        return val == 0 ? "0.01m" : switch (DelayRange7.LOOKUP.get(range)) {
            case Range5m -> fmtDouble(computeDelay(val, 0.05f, 5.3f), 4) + "m";
            case Range25m -> fmtDouble(computeDelay(val, 0.21f, 25.1f), 4) + "m";
            case Range100m -> fmtDouble(computeDelay(val, 0.8f, 100f), 4) + "m";
            case Range500m -> fmtDouble(computeDelay(val, 3.95f, 500f), 4) + "m";
            case Range1s -> val == 127 ? "1.000s" :
                    fmtDouble(computeDelay(val, 7.89f, 1000f), 4) + "m";
            case Range2s -> {
                double min = 15.8f;
                double max = 2000f;
                yield val >= 64 ?
                        fmtDouble(computeDelay(val, min, max) / 1000, 5) + "s" :
                        fmtDouble(computeDelay(val, min, max), 4) + "m";
            }
            case Range2_7s -> {
                double min = 21.3f;
                double max = 2700f;
                yield val >= 48 ?
                        fmtDouble(computeDelay(val, min, max) / 1000, 5) + "s" :
                        fmtDouble(computeDelay(val, min, max), 5) + "m";
            }
        };
    }

    private static double computeDelay(int aValue, double min, double max) {
        return min + (max - min) * (aValue - 1) / 126;
    }


    static String fmtDoubleFixed(double v, int totalLen) {
        if (totalLen < 3) { throw new IllegalArgumentException("fmtDoubleFixed: bad totalLen: " + totalLen); }
        String s = String.format("%.0" + (totalLen-2) + "f",v);
        int dot = s.indexOf('.');
        if (dot >= totalLen) { return s.substring(0,dot); }
        int declen = totalLen-dot-1;
        return String.format("%.0"+declen+"f",v);
    }

    static String fmtDouble(double v, int totalLen) {
        return fmtDoubleFixed(v,totalLen).replaceAll("\\.0+$", "");
    }

}
