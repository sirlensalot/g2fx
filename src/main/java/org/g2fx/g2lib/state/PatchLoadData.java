package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class PatchLoadData {

    private final FieldValues fvs;

    private LibProperty<Double> mem = new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
        @Override public Double get() {
            return getMem(); }
        @Override public void set(Double newValue) {}
    });

    private LibProperty<Double> cycles = new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
        @Override public Double get() {
            return getCycles(); }
        @Override public void set(Double newValue) {}
    });

    public PatchLoadData(FieldValues fvs) {
        this.fvs = fvs;
    }
    /**
     * Some values are msb * 128 + lsb for unknown reason
     */
    private int word15(int msb, int lsb) { return msb * 128 + lsb; }

    public double getMem() {
        //mem: fmax(fmax( 100 * InternalMem / 128, 100 * RAM / 260000), 100*Resource4 / 4315);
        int resource4 = word15(Protocol.PatchLoadData.Resource4Msb.intValue(fvs),
                Protocol.PatchLoadData.Resource4Lsb.intValue(fvs));
        long ram = Integer.toUnsignedLong(Protocol.PatchLoadData.RAM.intValue(fvs));
        return Math.max(Math.max(
                        100 * Protocol.PatchLoadData.InternalMem.intValue(fvs) / 128,
                        100 * ((int) (ram / (long) 260000)) ),
                100 * resource4 / 4315);
    }

    public double getCycles() {
        //cyc: fmax( 100 * CyclesRed1 / 1372 + 100 * CyclesBlue1 / 5000, 0);
        int cyclesRed1 = word15(Protocol.PatchLoadData.CyclesRed1Msb.intValue(fvs),
                Protocol.PatchLoadData.CyclesRed1Lsb.intValue(fvs));
        int cyclesBlue1 = word15(Protocol.PatchLoadData.CyclesBlue1Msb.intValue(fvs),
                Protocol.PatchLoadData.CyclesBlue1Lsb.intValue(fvs));
        return 100 * cyclesRed1 / 1372 + 100 * cyclesBlue1 / 5000;
    }

    public LibProperty<Double> mem() { return mem; }
    public LibProperty<Double> cycles() { return cycles; }
}
