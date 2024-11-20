package kpan.b_water_mesh.asm.core;

import com.google.common.io.CharSource;
import java.io.IOException;
import net.minecraftforge.fml.common.asm.transformers.AccessTransformer;

public class AccessTransformerForMixin extends AccessTransformer {

    private static AccessTransformerForMixin INSTANCE;
    private static boolean callSuper = false;


    public static void toPublic(String owner, String mcpMethodName, String methodDesc) {
        String rule = "public " + owner + " " + mcpMethodName + methodDesc;
        callSuper = true;
        INSTANCE.processATFile(CharSource.wrap(rule));
        callSuper = false;
    }

    public AccessTransformerForMixin() throws IOException {
        super();
        INSTANCE = this;
    }

    @Override
    protected void processATFile(CharSource rulesResource) {
        if (!callSuper)
            return;
        try {
            super.processATFile(rulesResource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
