package kpan.b_water_mesh.asm.tf;

import com.google.common.collect.ImmutableList;
import kpan.b_water_mesh.asm.core.AsmNameRemapper;
import kpan.b_water_mesh.asm.core.AsmTypes;
import kpan.b_water_mesh.asm.core.AsmUtil;
import kpan.b_water_mesh.asm.core.adapters.InjectInstructionsAdapter;
import kpan.b_water_mesh.asm.core.adapters.Instructions;
import kpan.b_water_mesh.asm.core.adapters.Instructions.OpcodeMethod;
import kpan.b_water_mesh.asm.core.adapters.MyClassVisitor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class TF_BlockFluidBase {

    private static final String TARGET = "net.minecraftforge.fluids.BlockFluidBase";
    private static final String HOOK = AsmTypes.HOOK + "HK_" + "BlockFluidBase";

    public static ClassVisitor appendVisitor(ClassVisitor cv, String className) {
        if (!TARGET.equals(className))
            return cv;
        ClassVisitor newcv = new MyClassVisitor(cv, className) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                String mcpName = AsmNameRemapper.runtime2McpMethodName(name);
                if (mcpName.equals("<clinit>")) {
                    mv = InjectInstructionsAdapter.before(mv, mcpName,
                            Instructions.create()
                                    .methodRep(OpcodeMethod.VIRTUAL, ImmutableList.Builder.class.getName(), "build")
                            ,
                            Instructions.create()
                                    .aload(0)
                                    .invokeStatic(HOOK, "onBuildFluidProperty", AsmUtil.toMethodDesc(AsmTypes.VOID, ImmutableList.Builder.class))
                    );
                    success();
                }
                if (mcpName.equals("getExtendedState")) {
                    mv = InjectInstructionsAdapter.before(mv, mcpName,
                            Instructions.create()
                                    .aload(4)
                                    .aret()
                            ,
                            Instructions.create()
                                    .aload(0)
                                    .aload(2)
                                    .aload(3)
                                    .aload(4)
                                    .invokeStatic(HOOK, "onGetExtendedState", AsmUtil.toMethodDesc(IExtendedBlockState.class, TARGET, IBlockAccess.class, BlockPos.class, IExtendedBlockState.class))
                                    .astore(4)
                    );
                    success();
                }
                return mv;
            }
        }.setSuccessExpected(2);
        return newcv;
    }
}
