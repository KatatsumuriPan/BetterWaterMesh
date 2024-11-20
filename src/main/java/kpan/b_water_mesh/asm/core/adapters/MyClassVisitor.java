package kpan.b_water_mesh.asm.core.adapters;

import kpan.b_water_mesh.asm.core.AsmUtil;
import org.objectweb.asm.ClassVisitor;

public class MyClassVisitor extends ClassVisitor {

    public final String nameForDebug;
    private int successed = 0;
    private int successExpectedMinInclusive;
    private int successExpectedMaxInclusive;

    public MyClassVisitor(ClassVisitor cv, String nameForDebug) {
        super(AsmUtil.ASM_VER, cv);
        this.nameForDebug = nameForDebug;
        setSuccessExpected(1);
    }
    @SuppressWarnings("unused")
    public MyClassVisitor setSuccessExpected(int successExpected) {
        setSuccessExpectedMin(successExpected);
        setSuccessExpectedMax(successExpected);
        return this;
    }
    @SuppressWarnings("unused")
    public MyClassVisitor setSuccessExpected(int successExpectedMinInclusive, int successExpectedMaxInclusive) {
        setSuccessExpectedMin(successExpectedMinInclusive);
        setSuccessExpectedMax(successExpectedMaxInclusive);
        return this;
    }
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public MyClassVisitor setSuccessExpectedMin(int minInclusive) {
        successExpectedMinInclusive = Math.max(minInclusive, 0);
        return this;
    }
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public MyClassVisitor setSuccessExpectedMax(int maxInclusive) {
        successExpectedMaxInclusive = Math.max(maxInclusive, 0);
        return this;
    }
    protected void success() {
        successed++;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (successed < successExpectedMinInclusive || successed > successExpectedMaxInclusive) {
            if (successExpectedMinInclusive == successExpectedMaxInclusive)
                throw new RuntimeException("transform failed:" + nameForDebug + "\nexpected:" + successExpectedMinInclusive + "\nactual:" + successed);
            else if (successExpectedMaxInclusive == Integer.MAX_VALUE)
                throw new RuntimeException("transform failed:" + nameForDebug + "\nexpected: " + successExpectedMinInclusive + "~\nactual:" + successed);
            else
                throw new RuntimeException("transform failed:" + nameForDebug + "\nexpected: " + successExpectedMinInclusive + "~" + successExpectedMaxInclusive + "\nactual:" + successed);
        }
    }
}
