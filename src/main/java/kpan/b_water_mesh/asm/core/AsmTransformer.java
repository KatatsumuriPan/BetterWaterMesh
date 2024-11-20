package kpan.b_water_mesh.asm.core;

import java.util.List;
import kpan.b_water_mesh.asm.core.adapters.InjectInstructionsAdapter;
import kpan.b_water_mesh.asm.core.adapters.Instructions;
import kpan.b_water_mesh.asm.core.adapters.MixinAccessorAdapter;
import kpan.b_water_mesh.asm.core.adapters.MyClassVisitor;
import kpan.b_water_mesh.asm.tf.TF_BlockFluidBase;
import kpan.b_water_mesh.asm.tf.TF_ModelFluid;
import kpan.b_water_mesh.util.ListUtil;
import kpan.b_water_mesh.util.ReflectionUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.ASMTransformerWrapper.TransformerWrapper;
import net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class AsmTransformer implements IClassTransformer {

    /**
     * クラスが最初に読み込まれた時に呼ばれる。
     *
     * @param name            クラスの難読化名(区切りは'.')
     * @param transformedName クラスの易読化名
     * @param bytes           オリジナルのクラス
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        try {
            if (transformedName.equals("kpan.srg2mcp_name_remapper.Remapper"))
                return bytes;
            AsmNameRemapper.init();
            if (bytes == null)
                return null;

            // byte配列を読み込み、利用しやすい形にする。
            ClassReader cr = new ClassReader(bytes);
            // これのvisitを呼ぶことによって情報が溜まっていく。
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);// maxStack,maxLocal,frameの全てを計算
            // Adapterを通して書き換え出来るようにする。
            ClassVisitor cv = cw;
            cv = rearrangeThisAfterDeobfuscation(cv, transformedName);
            cv = MixinAccessorAdapter.transformAccessor(cv, transformedName);
            cv = TF_BlockFluidBase.appendVisitor(cv, transformedName);
            cv = TF_ModelFluid.appendVisitor(cv, transformedName);

            if (cv == cw)
                return bytes;

            // 元のクラスと同様の順番でvisitメソッドを呼んでくれる
            cr.accept(cv, 0);

            byte[] new_bytes = cw.toByteArray();

            // Writer内の情報をbyte配列にして返す。
            return new_bytes;
        } catch (Throwable e) {
            AsmUtil.LOGGER.error("transformedName:" + transformedName);
            AsmUtil.LOGGER.error(e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static ClassVisitor rearrangeThisAfterDeobfuscation(ClassVisitor cv, String className) {
        if (!className.equals("net.minecraftforge.fml.common.Loader"))
            return cv;
        return new MyClassVisitor(cv, className) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("<init>")) {
                    mv = InjectInstructionsAdapter.injectBeforeReturns(mv, name
                            , Instructions.create()
                                    .invokeStatic(AsmTransformer.class.getName(), "rearrange", AsmTypes.METHOD_VOID)
                    );
                    success();
                }
                return mv;
            }
        };
    }

    @SuppressWarnings("unused")
    public static void rearrange() {
        ClassLoader classLoader = AsmTransformer.class.getClassLoader();
        List<IClassTransformer> transformers = ReflectionUtil.getPrivateField(classLoader, "transformers");

        int thisIndex = ListUtil.indexOf(transformers, transformer -> {
            if (transformer instanceof TransformerWrapper) {
                IClassTransformer parent = ReflectionUtil.getPrivateField(TransformerWrapper.class, transformer, "parent");
                return parent.getClass() == AsmTransformer.class;
            }
            return false;
        });
        int deobfIndex = ListUtil.indexOf(transformers, transformer -> {
            if (transformer instanceof TransformerWrapper) {
                IClassTransformer parent = ReflectionUtil.getPrivateField(TransformerWrapper.class, transformer, "parent");
                return parent.getClass() == DeobfuscationTransformer.class;
            }
            return transformer.getClass().getName().equals("net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer");
        });
        transformers.add(deobfIndex + 1, transformers.remove(thisIndex));
    }

}
