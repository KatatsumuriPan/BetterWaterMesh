package kpan.srg2mcp_name_remapper;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import kpan.b_water_mesh.asm.core.AsmUtil;
import kpan.b_water_mesh.util.ReflectionUtil;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.common.patcher.ClassPatchManager;
import org.objectweb.asm.ClassReader;

public class Remapper {
    private static boolean inited = false;

    private static LaunchClassLoader classLoader;

    private static ImmutableMap<String, String> srgMcpFieldMap = ImmutableMap.of(); // loadMcpMapで代入
    private static ImmutableMap<String, String> srgMcpMethodMap = ImmutableMap.of();    // loadMcpMapで代入

    private static Map<String, ImmutableMap<String, String>> fieldMcpSrgMap = Maps.newHashMap();        // owner -> (mcp -> srg)
    private static Map<String, ImmutableMap<String, String>> methodMcpSrgMap = Maps.newHashMap();       // owner -> (mcp+desc -> srg)
    private static Map<String, ImmutableMap<String, String>> rawFieldMcpSrgMap = Maps.newHashMap();     // owner -> (mcp -> srg)
    private static Map<String, ImmutableMap<String, String>> rawMethodMcpSrgMap = Maps.newHashMap();    // owner -> (mcp+desc -> srg)
    private static Set<String> loadedClassNames = Sets.newHashSet();


    public static void init() {
        if (!inited) {
            inited = true;
            try {
                classLoader = ReflectionUtil.getPrivateField(FMLDeobfuscatingRemapper.INSTANCE, "classLoader");
                loadMcpMap();
            } catch (IOException e) {
                AsmUtil.LOGGER.error("An error occurred loading the srg map data");
                throw new RuntimeException(e);
            }
        }
    }
    private static void loadMcpMap() throws IOException {
        InputStream stream = Remapper.class.getResourceAsStream("/nameremapper/srg2mcp.map");
        Map<String, String> srgMcpFieldMap = new HashMap<>();
        Map<String, String> srgMcpMethodMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {// readerがcloseされれば全部closeされる(はず)
            String owner = "";
            ImmutableMap.Builder<String, String> builderField = ImmutableMap.builder();
            ImmutableMap.Builder<String, String> builderMethod = ImmutableMap.builder();
            boolean uniqueMethod = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("*")) {
                    uniqueMethod = true;
                    continue;
                }
                String[] split = line.split(" ");
                if (split.length == 1) {
                    // class
                    if (!owner.isEmpty()) {
                        rawFieldMcpSrgMap.put(owner, builderField.build());
                        rawMethodMcpSrgMap.put(owner, builderMethod.build());
                    }
                    owner = "net/minecraft/" + line;
                    builderField = ImmutableMap.builder();
                    builderMethod = ImmutableMap.builder();
                    uniqueMethod = false;
                } else if (split.length == 2) {
                    // field or unique method
                    if (uniqueMethod) {
                        String srg = split[0];
                        String mcp = split[1];
                        builderMethod.put(mcp, srg);
                        srgMcpMethodMap.put(srg, mcp);
                    } else {
                        String srg = split[0];
                        String mcp = split[1];
                        builderField.put(mcp, srg);
                        srgMcpFieldMap.put(srg, mcp);
                    }
                } else {
                    String srg = split[0];
                    String mcp = split[1];
                    String methodDesc = split[2];
                    builderMethod.put(mcp + methodDesc, srg);
                    srgMcpMethodMap.put(srg, mcp);
                }
            }
            if (!owner.isEmpty()) {
                rawFieldMcpSrgMap.put(owner, builderField.build());
                rawMethodMcpSrgMap.put(owner, builderMethod.build());
            }
        }
        Remapper.srgMcpFieldMap = ImmutableMap.copyOf(srgMcpFieldMap);// 重複キーが発生しうるのでBuilderは使えない
        Remapper.srgMcpMethodMap = ImmutableMap.copyOf(srgMcpMethodMap);// 重複キーが発生しうるのでBuilderは使えない
    }

    public static void clearCaches() {
        loadedClassNames.clear();
        fieldMcpSrgMap.clear();
        methodMcpSrgMap.clear();
    }

    public static String srg2McpFieldName(String srgFieldName) {
        if (srgFieldName.startsWith("field_"))
            return srgMcpFieldMap.getOrDefault(srgFieldName.substring("field_".length()), srgFieldName);
        else
            return srgFieldName;
    }

    public static String srg2McpMethodName(String srgMethodName) {
        if (srgMethodName.startsWith("func_"))
            return srgMcpMethodMap.getOrDefault(srgMethodName.substring("func_".length()), srgMethodName);
        else
            return srgMethodName;
    }

    public static String mcp2SrgFieldName(String owner, String mcpName) {
        Map<String, String> map = getFieldMcp2SrgMap(owner.replace('.', '/'));
        if (map == null)
            return mcpName;
        String srgName = map.get(mcpName);
        if (srgName != null)
            return "field_" + srgName;
        return mcpName;
    }

    public static String mcp2SrgMethodName(String owner, String mcpName, @Nullable String methodDesc) {
        Map<String, String> map = getMethodMcp2SrgMap(owner.replace('.', '/'));
        if (map == null)
            return mcpName;
        String srgName = map.get(mcpName);
        if (srgName != null)
            return "func_" + srgName;
        if (methodDesc != null) {
            srgName = map.get(mcpName + methodDesc);
            if (srgName != null)
                return "func_" + srgName;
        }
        return mcpName;
    }

    private static @Nullable Map<String, String> getMethodMcp2SrgMap(String owner) {
        if (!loadedClassNames.contains(owner)) {
            findAndMergeSuperMaps(owner);
        }
        return methodMcpSrgMap.get(owner);
    }
    private static @Nullable Map<String, String> getFieldMcp2SrgMap(String owner) {
        if (!loadedClassNames.contains(owner)) {
            findAndMergeSuperMaps(owner);
        }
        return fieldMcpSrgMap.get(owner);
    }
    private static void findAndMergeSuperMaps(String className) {
        try {
            String superName = null;
            String[] interfaces = new String[0];
            byte[] classBytes = ClassPatchManager.INSTANCE.getPatchedResource(FMLDeobfuscatingRemapper.INSTANCE.unmap(className), className, classLoader);
            if (classBytes != null) {
                ClassReader cr = new ClassReader(classBytes);
                superName = cr.getSuperName();
                interfaces = cr.getInterfaces();
            }
            mergeSuperMaps(className, superName, interfaces);
            loadedClassNames.add(className);
        } catch (IOException e) {
            FMLLog.log.error("Error getting patched resource:", e);// for java8
        }
    }
    private static void mergeSuperMaps(String className, @Nullable String superName, String[] interfaces) {
        if (Strings.isNullOrEmpty(superName))
            return;

        List<String> allParents = ImmutableList.<String>builder().add(superName).addAll(Arrays.asList(interfaces)).build();
        // generate maps for all parent objects
        for (String parentThing : allParents) {
            if (parentThing.startsWith("java/"))
                continue;
            String deobfParent = FMLDeobfuscatingRemapper.INSTANCE.map(parentThing);
            if (!loadedClassNames.contains(deobfParent)) {
                findAndMergeSuperMaps(deobfParent);
            }
        }
        List<ImmutableMap<String, String>> methodMaps = new ArrayList<>();
        List<ImmutableMap<String, String>> fieldMaps = new ArrayList<>();
        for (String parentThing : allParents) {
            if (parentThing.startsWith("java/"))
                continue;
            String deobfParent = FMLDeobfuscatingRemapper.INSTANCE.map(parentThing);
            if (methodMcpSrgMap.containsKey(deobfParent)) {
                methodMaps.add(methodMcpSrgMap.get(deobfParent));
            }
            if (fieldMcpSrgMap.containsKey(deobfParent)) {
                fieldMaps.add(fieldMcpSrgMap.get(deobfParent));
            }
        }

        if (rawMethodMcpSrgMap.containsKey(className))
            methodMaps.add(rawMethodMcpSrgMap.get(className));
        if (rawFieldMcpSrgMap.containsKey(className))
            fieldMaps.add(rawFieldMcpSrgMap.get(className));

        if (methodMaps.size() == 1) {
            methodMcpSrgMap.put(className, methodMaps.get(0));
        } else if (methodMaps.size() > 1) {
            HashMap<String, String> merge = new HashMap<>();
            for (var map : methodMaps) {
                merge.putAll(map);
            }
            methodMcpSrgMap.put(className, ImmutableMap.copyOf(merge));// privateであれば親と同名のメンバを作成できるためbuilderは使えない
        }

        if (fieldMaps.size() == 1) {
            fieldMcpSrgMap.put(className, fieldMaps.get(0));
        } else if (fieldMaps.size() > 1) {
            HashMap<String, String> merge = new HashMap<>();
            for (var map : fieldMaps) {
                merge.putAll(map);
            }
            fieldMcpSrgMap.put(className, ImmutableMap.copyOf(merge));// 同
        }

    }

}
