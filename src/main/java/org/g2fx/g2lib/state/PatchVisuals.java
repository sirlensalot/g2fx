package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.g2fx.g2lib.state.Device.dispatchSuccess;
import static org.g2fx.g2lib.util.Util.forEachIndexed;

public class PatchVisuals {

    private final List<PatchVisual> leds = new ArrayList<>();
    private final List<PatchVisual> metersAndGroups = new ArrayList<>();
    private final PatchArea voiceArea;
    private final PatchArea fxArea;
    private final Logger log;

    public PatchVisuals(Slot slot, PatchArea voiceArea, PatchArea fxArea) {
        this.voiceArea = voiceArea;
        this.fxArea = fxArea;
        this.log = Util.getLogger(getClass(),slot);
    }

    // usb, file-patch
    public void updateVisualIndex() {
        leds.clear();
        voiceArea.addVisuals(Visual.VisualType.Led,leds);
        fxArea.addVisuals(Visual.VisualType.Led,leds);
        log.info(() -> "leds: " + leds);

        metersAndGroups.clear();
        voiceArea.addVisuals(null,metersAndGroups);
        fxArea.addVisuals(null,metersAndGroups);
        log.info(() -> "metersAndGroups: " + metersAndGroups);
    }


    // usb
    public boolean readVolumeData(ByteBuffer buf) {
        List<PatchVisual> updated = new ArrayList<>();
        metersAndGroups.forEach(v -> {
            buf.get(); // unknown
            int i = Util.b2i(buf.get());
            if (v.update(i)) {
                updated.add(v);
            }
        });
        log.info(() -> "readVolumeData: " + updated);
        return dispatchSuccess(() -> "readVolumeData");
    }

    // usb
    public boolean readLedData(ByteBuffer buf) {
        buf.get(); //unknown
        ByteBuffer buf2 = buf.slice();
        List<PatchVisual> updated = new ArrayList<>();
        forEachIndexed(leds,(v,i) ->  {
            int bi = Math.floorDiv(i,4);
            int bm = Math.floorMod(i,4) * 2;
            int b = (buf2.get(bi) & (0x03 << bm)) >>> bm;
            //log.info(String.format("%s %s %s %s %s",i,bi,bm,b,Integer.toBinaryString(buf2.get(bi))));
            if (v.update(b)) {
                updated.add(v);
            }
        });
        log.info(() -> "readLedData: " + updated);
        return dispatchSuccess(() -> "readLedData");
    }



    public List<PatchVisual> getLeds() {
        return leds;
    }

    public List<PatchVisual> getMetersAndGroups() {
        return metersAndGroups;
    }
}
