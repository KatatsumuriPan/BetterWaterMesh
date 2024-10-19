package kpan.b_water_mesh.asm.tf;

import kpan.b_water_mesh.asm.core.AsmTypes;
import kpan.b_water_mesh.asm.core.adapters.ReplaceRefMethodAdapter;
import org.objectweb.asm.ClassVisitor;

public class TF_ModelFluid {

    private static final String TARGET = "net.minecraftforge.client.model.ModelFluid";
    private static final String HOOK = AsmTypes.HOOK + "HK_" + "ModelFluid";

    public static ClassVisitor appendVisitor(ClassVisitor cv, String className) {
        if (!TARGET.equals(className))
            return cv;
        ClassVisitor newcv = new ReplaceRefMethodAdapter(cv, HOOK, TARGET, "bake");
        return newcv;
    }
}
