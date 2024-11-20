package kpan.b_water_mesh.asm.core;

import kpan.srg2mcp_name_remapper.Remapper;
import org.jetbrains.annotations.Nullable;

// 難読化解除後にTransformerが呼ばれるようにしたため、クラスのruntime名は常にmcp名だし、メンバのruntime名は常にsrg名
public class AsmNameRemapper {

    public static void init() {
        Remapper.init();
    }
    public static String srg2McpFieldName(String srgFieldName) {
        return Remapper.srg2McpFieldName(srgFieldName);
    }

    public static String srg2McpMethodName(String srgMethodName) {
        return Remapper.srg2McpMethodName(srgMethodName);
    }

    public static String mcp2SrgFieldName(String owner, String mcpName) {
        return Remapper.mcp2SrgFieldName(owner, mcpName);
    }

    public static String mcp2SrgMethodName(String owner, String mcpName, @Nullable String methodDesc) {
        return Remapper.mcp2SrgMethodName(owner, mcpName, methodDesc);
    }

    public static String mcp2RuntimeFieldName(String owner, String mcpName) {
        if (AsmUtil.isDeobfEnvironment())
            return mcpName;
        else
            return mcp2SrgFieldName(owner, mcpName);
    }

    public static String mcp2RuntimeMethodName(String owner, String mcpName, @Nullable String methodDesc) {
        if (AsmUtil.isDeobfEnvironment())
            return mcpName;
        else
            return mcp2SrgMethodName(owner, mcpName, methodDesc);
    }

    public static String runtime2McpFieldName(String runtimeName) {
        if (AsmUtil.isDeobfEnvironment())
            return runtimeName;
        else
            return srg2McpFieldName(runtimeName);
    }

    public static String runtime2McpMethodName(String runtimeName) {
        if (AsmUtil.isDeobfEnvironment())
            return runtimeName;
        else
            return srg2McpMethodName(runtimeName);
    }

}
