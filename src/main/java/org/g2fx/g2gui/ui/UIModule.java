package org.g2fx.g2gui.ui;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiFunction;

public record UIModule<C> (
        String Name,
        String Tooltip,
        int Height,
        List<C> Controls) {

    public record UIModules(Map<ModuleType,UIModule<UIElement>> modules) {
        public UIModule<UIElement> get(ModuleType mt) {
            return modules.get(mt);
        }
    }

    public static UIModules readModuleUIs() throws Exception {
        Map<ModuleType, UIModule<UIElement>> m = new TreeMap<>();
        ObjectMapper mapper = Util.mkYamlMapper();
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
            //sort controls so dependency-having controls go last
            cs.sort(Comparator.comparing(UIElement::elementType));
            m.put(mt,new UIModule<>(um.Name,um.Tooltip,um.Height,cs));
        }
//        doTypeParamQry(m,"Led",true,true,"Type","LedGroup","GroupId","CodeRef","Control");
        //doTypeParamQry(m,"Led",true,true);
        //doBipUniQry(m);
        //doTf(m);
        //doGraph(m);
        return new UIModules(m);
    }
    private static void doTypeParamQry(Map<ModuleType, UIModule<UIElement>> m, String cls,
                                       boolean inclModule, boolean modFirst, String... params) throws Exception {
        doQuery(m,cls + "-" + String.join("-",params),(mt,ctl) -> {
            if (cls.equals(ctl.elementType().name())) {
                String out = null;
                if (params.length==0) {
                    out = ctl.toString();
                } else {
                    for (String param : params) {
                        out = out == null ? "" : out + ",";
                        out += String.format("%s=%s", param, invoke(ctl, param));
                    }
                }
                if (inclModule) out = modFirst ? mt + ":" + out : out + ":" + mt;
                return out;
            }
            return null;
        });
    }


    private static void doBipUniQry(Map<ModuleType, UIModule<UIElement>> m) throws Exception {
        doQuery(m,"BipUni",(mt, ctl) -> {
            if (ctl instanceof UIElements.TextField tf) {
                if (mt.getParams().get(tf.MasterRef()).param() == ModParam.LevBipUni) {
                    return String.format("%s:BipUni.TextFunc=%s, %s", mt, tf.TextFunc(),
                            tf.Dependencies().stream().map(d->mt.getParams().get(d.index())).toList());
                }
            }
            return null;
        });
    }
    private static void doTf(Map<ModuleType, UIModule<UIElement>> m) throws Exception {
        doQuery(m,"Tf-all",(mt, ctl) -> {
            if (ctl instanceof UIElements.TextField f) {
                ModParam param = mt.getParams().get(f.MasterRef()).param();
                return String.format("%d:%s:%s,deps=%s", f.TextFunc(), mt, param,f.Dependencies() == null ? "[]" :
                        f.Dependencies().stream().map(d ->
                        String.format("%s.%s->%s",d.index(),d.type(),(d.type()== UIElements.DepType.Param ?
                                mt.getParams().get(d.index()) : mt.modes.get(d.index())).param())).toList());
            }
            return null;
        });
    }

    private static void doGraph(Map<ModuleType, UIModule<UIElement>> m) throws Exception {
        doQuery(m,"Graph-all",(mt, ctl) -> {
            if (ctl instanceof UIElements.Graph f) {
                return String.format("%d:%s,deps=%s", f.GraphFunc(), mt,f.Dependencies() == null ? "[]" :
                        f.Dependencies().stream().map(d ->
                                String.format("%s.%s->%s",d.index(),d.type(),(d.type()== UIElements.DepType.Param ?
                                        mt.getParams().get(d.index()) : mt.modes.get(d.index())).param())).toList());
            }
            return null;
        });
    }


    private static void doQuery(Map<ModuleType, UIModule<UIElement>> m, String name,
                                BiFunction<ModuleType,UIElement,String> ef) throws Exception {
        String fn = String.format("data/uiqry-%s.txt", name);
        try (PrintWriter bw = new PrintWriter(new FileWriter(fn))) {
            Set<String> ls = new TreeSet<>();
            for (Map.Entry<ModuleType, UIModule<UIElement>> e : m.entrySet()) {
                e.getValue().Controls.forEach(c -> {
                    String o = ef.apply(e.getKey(),c);
                    if (o != null) { ls.add(o); }
                });
            }
            ls.forEach(bw::println);
        }
        System.out.println("Query written to " + fn);
        System.exit(0);
    }


    private static Object invoke(UIElement ctl, String param) {
        try {
            Object v = ctl.getClass().getDeclaredMethod(param).invoke(ctl);
            return v;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void main(String[] args) throws Exception {
        Map<ModuleType, UIModule<UIElement>> m = readModuleUIs().modules;
        ObjectMapper mapper = Util.mkYamlMapper();
        mapper.writeValue(new File("data/module-uis-output.yaml"),m);

    }

    public static class StringToListDesz extends JsonDeserializer<List<String>> {

        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            return (value == null || value.isEmpty()) ? Collections.emptyList() :
                    Arrays.asList(value.split("\\s*,\\s*"));
        }
    }

    public static class DependecyDesz extends JsonDeserializer<List<ControlDependencies.Dependency>> {

        StringToListDesz delegate = new StringToListDesz();

        private int parseInt(String s) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failure deserializing Dependency int value: " + s,e);
            }
        }
        @Override
        public List<ControlDependencies.Dependency> deserialize(
                JsonParser p, DeserializationContext ctxt) throws IOException {
            return delegate.deserialize(p,ctxt).stream().map(s ->
                    s.charAt(0) == 'S' ?
                            new ControlDependencies.Dependency(UIElements.DepType.Mode,parseInt(s.substring(1))) :
                            new ControlDependencies.Dependency(UIElements.DepType.Param,parseInt(s)))
                    .toList();
        }
    }

}
