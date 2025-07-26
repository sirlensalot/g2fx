package org.g2fx.g2gui.controls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2lib.model.ModuleType;

import java.io.File;
import java.util.*;

public record UIModule<C> (
        String Name,
        String Tooltip,
        int Height,
        List<C> Controls) {

    public static Map<ModuleType,UIModule<UIElement>> readModuleUIs() throws Exception {
        Map<ModuleType, UIModule<UIElement>> m = new TreeMap<>();
        ObjectMapper mapper = mkYamlMapper();
        HashMap<String,UIModule<Map<String,Object>>> y = mapper.readValue(
                FXUtil.getResource("module-uis.yaml")
                , new TypeReference<>() {});
        for (String mn : y.keySet()) {
            String mtName = "M_" + mn.replace('-','_').replace("&","_and_");
            ModuleType mt = ModuleType.valueOf(mtName);
            UIModule<Map<String, Object>> um = y.get(mn);
            List<UIElement> cs = new ArrayList<>();
            for (Map<String, Object> c : um.Controls) {
                String cls = (String) c.get("Class");
                UIElements.ElementType et = UIElements.ElementType.valueOf(cls);
                c.remove("Class");
                UIElement e = mapper.convertValue(c,et.getType());
                cs.add(e);
            }
            m.put(mt,new UIModule<>(um.Name,um.Tooltip,um.Height,cs));
        }
        return m;
    }

    public static void main(String[] args) throws Exception {
        Map<ModuleType, UIModule<UIElement>> m = readModuleUIs();
        ObjectMapper mapper = mkYamlMapper();
        mapper.writeValue(new File("data/module-uis-output.yaml"),m);

    }

    public static ObjectMapper mkYamlMapper() {
        return new ObjectMapper(
                new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }
}
