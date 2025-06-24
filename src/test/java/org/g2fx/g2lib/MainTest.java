package org.g2fx.g2lib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void crcClavia() throws  Exception {


        assertEquals(0x9188, CRC16.crc16(new byte[] {(byte) 0x80},0,1));
        byte[] msg = Util.asBytes(
                0x80, 0x0a, 0x03, 0x00, 0x00, 0x1a, 0x00, 0x8c, 0x00, 0x12, 0x4d, 0x6f, 0x64, 0x75, 0x6c, 0x61,
                0x72, 0x47, 0x32, 0x00, 0x30, 0x03, 0x4c, 0x52, 0x00, 0x00, 0x01, 0x96, 0x28, 0x61, 0x00, 0x05,
                0x01, 0x0a, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        );
        assertEquals(0x3e24,CRC16.crc16(msg,0,msg.length));

        assertEquals(8, Util.b2i((byte) 0x82) >> 4);

        assertEquals(0x1bd6, Util.addb((byte) 0x1b, (byte) 0xd6));

        assertArrayEquals(Util.asBytes(0,1,2,3,4,5),
                Util.concat(Util.asBytes(0,1,2),Util.asBytes(3,4),Util.asBytes(5)));

        assertEquals("S&H", ModuleType.M_S_and_H.shortName);
        assertEquals("T&H", ModuleType.M_T_and_H.shortName);
        assertEquals("2-In",ModuleType.M_2_In.shortName);

        TreeSet<ModuleType.ModPageIx> ixs = new TreeSet<>();
        for (ModuleType mt : ModuleType.values()) {
            if (!ixs.add(mt.modPageIx)) {
                fail("Bad mod page ix: " + mt);
            }
        }
        {
            int i = -1;
            ModuleType.ModPage page = null;
            for (ModuleType.ModPageIx ix : ixs) {
                if (ix.page() != page) {
                    page = ix.page();
                    i = ix.ix();
                    if (i != 0) {
                        fail("Bad starting index: " + ix);
                    }

                } else {
                    if (++i != ix.ix()) {
                        fail("Non-monotonic ix: " + ix);
                    }
                }
            }
        }

        {
            ObjectMapper mapper = new YAMLMapper();
            Map<String,String> images = new HashMap<>();
            HashMap<String,UiModule> m = mapper.readValue(
                    FXUtil.getResource("module-uis.yaml")
                    , new TypeReference<>() {});
            List<ModuleType> all = new ArrayList<>(
                    Arrays.stream(ModuleType.values()).toList());
            for (Map.Entry<String, UiModule> e : m.entrySet()) {
                String mtName = "M_" + e.getValue().Name.replace('-','_').replace("&","_and_");
                all.remove(ModuleType.valueOf(mtName));

                for (Map<String, Object> c : e.getValue().controls) {
                    String id = e.getKey() + "-" + c.get("ID");
                    if ("Bitmap".equals(c.get("type"))) {
                        int w = (Integer) c.get("Width");
                        int h = (Integer) c.get("Height");
                        String data = (String) c.get("Data");
                        if (images.containsKey(data)) {
                            System.out.println("dupe [image]: " + id + ": " + images.get(data));
                        } else {
                            images.put(data,id);
                            writeImageFromString(
                                    data, w, h,
                                    id,
                                    false
                            );
                        }
                    } else if (c.containsKey("Image")) {
                        String data = (String) c.get("Image");
                        if (!"".equals(data)) {
                            int w = (Integer) c.get("ImageWidth");
                            int n = c.containsKey("ImageCount") ? ((Integer) c.get("ImageCount")) : 0;
                            List<String> bs = Arrays.stream(data.split(":")).toList();
                            int l = bs.size();
                            if (images.containsKey(data)) {
                                System.out.println("dupe [image]: " + id + ": " + images.get(data));
                            } else {
                                for (int i = 0; i < n; i++) {
                                    int h = l / w / n;
                                    int a = h * w;

                                    String iid = "%s-%02d".formatted(id, i);
                                    images.put(data,iid);
                                    List<String> sl = bs.subList(i * a, (i + 1) * a);
                                    writeImageFromString(
                                            String.join(":", sl),
                                            w, h,
                                            iid,
                                            true);

                                }
                            }
                        }
                    }
                }
            }
            //assertEquals(List.of(),all); TODO "Name" module
        }
    }
    record UiModule(
            String Name,
            String FileName,
            String Tooltip,
            int Height,
            int XPos,
            int YPos,
            List<Map<String,Object>> controls
    ){};

    public static void writeImageFromString(
            String data, int width, int height, String outputFile, boolean hex) throws IOException {
        String[] colorStrings = data.split(":");
        if (colorStrings.length != width * height) {
            throw new IllegalArgumentException("Pixel count does not match width*height");
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int pixel = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                String color = colorStrings[pixel++];
                int r, g, b;

                if (hex) {
                    // Expecting 6-digit hex string, e.g. "aabb99"
                    if (color.length() != 6)
                        throw new IllegalArgumentException("Hex color string not 6 digits: " + color);
                    r = Integer.parseInt(color.substring(0, 2), 16);
                    g = Integer.parseInt(color.substring(2, 4), 16);
                    b = Integer.parseInt(color.substring(4, 6), 16);
                } else {
                    // Expecting 9-digit decimal string, e.g. "191191191"
                    if (color.length() != 9 || !color.matches("\\d{9}"))
                        throw new IllegalArgumentException("Decimal color string not 9 digits: " + color);
                    r = Integer.parseInt(color.substring(0, 3));
                    g = Integer.parseInt(color.substring(3, 6));
                    b = Integer.parseInt(color.substring(6, 9));
                }
                // Clamp to 0-255
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                int argb;
                if (r == g && g == b && r > 188 & r < 193) {
                    // Fully transparent pixel
                    argb = 0x00000000;
                } else {
                    // Fully opaque pixel
                    argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                }
                //int rgb = (r << 16) | (g << 8) | b;
                img.setRGB(x, y, argb);
            }
        }

        ImageIO.write(img, "png", new File("data/img/" + outputFile + ".png"));
    }

}
