package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

public class PatchLoadData {

    private final FieldValues fvs;

    public PatchLoadData(FieldValues fvs) {
        this.fvs = fvs;
    }

    public int getMem() {
        //mem: fmax(fmax( 100 * InternalMem / 128, 100 * RAM / 260000), 100*Resource4 / 4315);
        return Math.max(Math.max(
                        100 * Protocol.PatchLoadData.MemInternalMem.intValueRequired(fvs) / 128,
                        100 * Protocol.PatchLoadData.MemRAM.intValueRequired(fvs) / 260000),
                100 * Protocol.PatchLoadData.MemResource4.intValueRequired(fvs) / 4315);
    }

    public int getCycles() {
        //cyc: fmax( 100 * CyclesRed1 / 1372 + 100 * CyclesBlue1 / 5000, 0);
        return 100 * Protocol.PatchLoadData.CycCyclesRed1.intValueRequired(fvs) / 1372
                + 100 * Protocol.PatchLoadData.CycCyclesBlue1.intValueRequired(fvs) / 5000;
    }
}
