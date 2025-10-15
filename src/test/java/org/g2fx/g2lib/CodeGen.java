package org.g2fx.g2lib;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CodeGen {

    /**
     * YAML munger
     */
    public static void main(String... args) throws IOException {
        ObjectMapper mapper = Util.mkYamlMapper();
        Map<String,Object> images = new HashMap<>();
        TreeMap<String,UiModule> m = mapper.readValue(
                new File("data/module-uis-input.yaml")
                , new TypeReference<>() {});
        m.put("Name",new UiModule("Name","Name",1, List.of()));
        List<ModuleType> all = new ArrayList<>(
                Arrays.stream(ModuleType.values()).toList());
        for (Map.Entry<String, UiModule> e : m.entrySet()) {
            String mtName = "M_" + e.getValue().Name.replace('-','_').replace("&","_and_");
            ModuleType mt = ModuleType.valueOf(mtName);
            all.remove(mt);

            for (Map<String, Object> c : e.getValue().Controls) {
                handleControl(e.getKey(), c, mt, images);
                for (String s : List.of("ZPos","Image","Style","FontSize")) {
                    c.remove(s);
                }
                for (String s : List.of("Graph Func","Text Func")) {
                    if (c.containsKey(s)) {
                        c.put(s.replace(" ",""), c.remove(s));
                    }
                }
                updateFields(c);
            }
        }


        assertEquals(List.of(),all);

        mapper.writeValue(
                new File("src/main/resources/org/g2fx/g2gui/module-uis.yaml"),
                m);

        Map<String,Integer> fieldCounts = new TreeMap<>();
        for (Map<String, FieldInfo> v : fields.values()) {
            for (String k : v.keySet()) {
                fieldCounts.compute(k,(fn,c) -> c == null ? 1 : c + 1);
                FieldInfo f = v.get(k);
                int s = f.values.size();
                if (s > 1) {
                    f.values.clear();
                    f.values.add("Size: " + s);
                }
            }
        }
        mapper.writeValue(
                new File("data/fields.yaml"),
                fields);
        mapper.writeValue(new File("data/fieldUse.yaml"),fieldCounts);
        try (PrintWriter w = new PrintWriter(new FileWriter("src/main/java/org/g2fx/g2gui/controls/UIElements.java"))) {
            w.println("""
                    package org.g2fx.g2gui.controls;
                    
                    import java.util.List;

                    public class UIElements {""");



            w.println("    public enum ElementType {");
            boolean first = true;
            for (String cls : fields.keySet()) {
                w.print("      ");
                w.print(first ? "  " : ", ");
                first = false;
                w.format("%s { @Override public Class<? extends UIElement> getType() { return %s.class; } }\n",cls,cls);
            }
            w.println("        ;\n        public abstract Class<? extends UIElement> getType();\n    }\n");
            for (String cls : fields.keySet()) {
                w.format("\n    public record %s (\n",cls.replace(" ",""));
                Map<String, FieldInfo> fs = fields.get(cls);
                first = true;
                for (String f : fs.keySet()) {
                    FieldInfo fi = fs.get(f);
                    w.print("      ");
                    w.print(first ? "  " : ", ");
                    first = false;
                    w.format("%s %s\n",fi.ty.getSimpleName().replace("ArrayList","List<String>"),f.replace(" ",""));
                }
                w.format("    ) implements UIElement%s {\n",fs.containsKey("Control") ? ", UIControl" : "");
                w.format("        @Override public ElementType elementType() { return ElementType.%s; }\n",cls);
                w.println("    }");
            }

            w.println("}\n");

        }
    }

    record FieldInfo(int count,Class<?> ty,Set<Object> values) {}

    static Map<String,Map<String,FieldInfo>> fields = new TreeMap<>();

    private static void updateFields(Map<String, Object> cc) {
        TreeMap<String, Object> c = new TreeMap<>(cc);
        String cls = (String)c.remove("Class");
        Map<String, FieldInfo> m = fields.computeIfAbsent(cls, s -> new TreeMap<>());
        for (String f : c.keySet()) {
            Object v= c.get(f);
            FieldInfo fi = m.computeIfAbsent(f, s -> new FieldInfo(0,v.getClass(),new HashSet<>()));
            fi.values.add(v);
            m.put(f,new FieldInfo(fi.count+1,fi.ty,fi.values));
            if (v.getClass()!=fi.ty) {
                throw new RuntimeException("Class mismatch:" + f + ", " + v.getClass() + ", " + fi);
            }
        }

    }

    private static void handleControl(String cn, Map<String, Object> c, ModuleType mt, Map<String, Object> images) throws IOException {
        String id = cn + "-" + c.get("ID");
        String cls = (String) c.get("Class");
        Integer cr = (Integer) c.get("CodeRef");


        boolean isAM = false;
        String name = null;

        if ("Input".equals(cls)) {
            name=mt.inPorts.get(cr).name();
        } else if ("Output".equals(cls)) {
            name=mt.outPorts.get(cr).name();
        } else if ("PartSelector".equals(cls)) {
            name=mt.modes.get(cr).name();
        } else if ("Led".equals(cls)) {
            Integer gid = (Integer) c.get("GroupId");
            List<Visual> lg = mt.getVisuals().get(Visual.VisualType.LedGroup);
            if ("Sequencer".equals(c.get("Type"))) {
                name=lg.get(gid).names().get(cr);
                c.put("LedGroup",true);
            } else { // Type: "Green"
                List<Visual> lv = mt.getVisuals().get(Visual.VisualType.Led);
                if (lv.isEmpty()) {
                    name=lg.get(gid).names().get(cr);
                    c.put("LedGroup",true);
                } else {
                    name=lv.get(cr).names().getFirst();
                }
            }
        } else if ("MiniVU".equals(cls)) {
            name=mt.getVisuals().get(Visual.VisualType.Meter).get((Integer) c.get("GroupId")).names().getFirst();
        } else if (cr != null) { // param controls
            NamedParam p = mt.getParams().get(cr);
            isAM = p.param()== ModParam.ActiveMonitor;
            name = p.name();
        }

        if (name != null) { c.put("Control",name); }

        if ("Bitmap".equals(cls)) {

            if (c.containsKey("skipImage")) {
                c.remove("Data");
                c.remove("skipImage");
                c.put("CustomText",true);
                return;
            }

            int w = (Integer) c.get("Width");
            int h = (Integer) c.get("Height");
            String data = (String) c.get("Data");
            if (images.containsKey(data)) {
                Object f = images.get(data);
                System.out.println("dupe [bitmap]: " + id + ": " + f);
                c.put("ImageFile",f);
            } else {
                String fn = writeImageFromString(
                        data, w, h,
                        id,
                        false
                );
                images.put(data,fn);
                c.put("ImageFile",fn);
            }
            c.remove("Data");
        } else if (c.containsKey("Image")) {
            if (isAM)  {
                System.out.println("Skipping image for ActiveMonitor: " + id);
                c.put("skipImage",1);
            }

            if (c.containsKey("skipImage")) { c.remove("Image"); c.remove("skipImage"); return; }

            String data = (String) c.get("Image");
            if (!"".equals(data)) {
                int w = (Integer) c.get("ImageWidth");
                int n = "ButtonRadio".equals(cls) ? ((Integer) c.get("ButtonCount")) :
                        c.containsKey("ImageCount") ? ((Integer) c.get("ImageCount")) : 1;
                List<String> bs = Arrays.stream(data.split(":")).toList();
                int l = bs.size();
                if (images.containsKey(data)) {
                    Object fs = images.get(data);
                    System.out.println("dupe [image]: " + id + ": " + fs);
                    c.put("Images",fs);
                } else {
                    List<String> files = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        int h = l / w / n;
                        int a = h * w;

                        String iid = "%s-%02d".formatted(id, i);
                        List<String> sl = bs.subList(i * a, (i + 1) * a);
                        files.add(writeImageFromString(
                                String.join(":", sl),
                                w, h,
                                iid,
                                true));
                    }
                    images.put(data,files);
                    c.put("Images",files);
                }
                c.remove("Image");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UiModule(
            String Name,
            //String FileName,
            String Tooltip,
            int Height,
            //int XPos,
            //int YPos,
            List<Map<String,Object>> Controls
    ){};

    public static String writeImageFromString(
            String data, int width, int height, String outputFile, boolean image) throws IOException {
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

                if (image) {
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

        String n = outputFile + ".png";
        ImageIO.write(img, "png", new File("data/img/" + n));
        return n;
    }

}
