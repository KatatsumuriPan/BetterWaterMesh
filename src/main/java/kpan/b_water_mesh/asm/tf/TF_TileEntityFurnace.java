package kpan.b_water_mesh.asm.tf;

import kpan.b_water_mesh.asm.core.AsmNameRemapper;
import kpan.b_water_mesh.asm.core.AsmTypes;
import kpan.b_water_mesh.asm.core.AsmUtil;
import kpan.b_water_mesh.asm.core.adapters.MixinAccessorAdapter;
import kpan.b_water_mesh.asm.core.adapters.MyClassVisitor;
import kpan.b_water_mesh.asm.core.adapters.RedirectInvokeAdapter;
import kpan.b_water_mesh.asm.core.adapters.ReplaceRefMethodAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class TF_TileEntityFurnace {

    private static final String TARGET = "net.minecraft.tileentity.TileEntityFurnace";
    private static final String HOOK = AsmTypes.HOOK + "HK_" + "TileEntityFurnace";
    private static final String ACC = AsmTypes.ACC + "ACC_" + "TileEntityFurnace";

    public static ClassVisitor appendVisitor(ClassVisitor cv, String className) {
        if (!TARGET.equals(className))
            return cv;
        ClassVisitor newcv = new MyClassVisitor(cv, className) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                String mcpName = AsmNameRemapper.runtime2McpMethodName(name);
                if (mcpName.equals("readFromNBT") || mcpName.equals("update")) {
                    mv = RedirectInvokeAdapter.static_(mv, mcpName, HOOK, TARGET, "getItemBurnTime");
                    success();
                }
                return mv;
            }
        }.setSuccessExpected(2);
        newcv = new ReplaceRefMethodAdapter(newcv, HOOK, TARGET, "getName", AsmUtil.toMethodDesc(AsmTypes.STRING));
        newcv = new MixinAccessorAdapter(newcv, className, ACC);
        return newcv;
    }
}
