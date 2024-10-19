package kpan.b_water_mesh.asm.core.adapters;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public abstract class ReplaceMethodAdapter extends MyClassVisitor {

    protected final String runtimeMethodName;
    @Nullable
    protected String methodDesc = null;
    protected int access;
    @Nullable
    protected String generics;
    @Nullable
    protected String[] exceptions;
    protected boolean useDesc = false;
    protected boolean useAccess = false;
    protected boolean useGenerics = false;
    protected boolean useExceptions = false;

    private boolean found = false;

    public ReplaceMethodAdapter(ClassVisitor cv, String runtimeMethodName) {
        super(cv, runtimeMethodName);
        this.runtimeMethodName = runtimeMethodName;
    }
    public ReplaceMethodAdapter(ClassVisitor cv, String runtimeMethodName, String methodDesc) {
        super(cv, runtimeMethodName + " " + methodDesc);
        this.runtimeMethodName = runtimeMethodName;
        setDesc(methodDesc);
    }
    @SuppressWarnings("unused")
    public void setDesc(String methodDesc) {
        this.methodDesc = methodDesc;
        useDesc = true;
    }
    @SuppressWarnings("unused")
    public void setAccess(int access) {
        this.access = access;
        useAccess = true;
    }
    @SuppressWarnings("unused")
    public void setGenerics(String generics) {
        this.generics = generics;
        useGenerics = true;
    }
    @SuppressWarnings("unused")
    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
        useExceptions = true;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (isTarget(access, name, desc, signature, exceptions)) {
            found = true;
            if (!useDesc)
                methodDesc = desc;
            if (!useAccess)
                this.access = access;
            if (!useGenerics)
                generics = signature;
            if (!useExceptions)
                this.exceptions = exceptions;
            return null;//既存のを削除、visitEndで追加
        } else
            return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (found) {
            MethodVisitor mv = super.visitMethod(access, runtimeMethodName, methodDesc, generics, exceptions);
            if (mv != null) {
                mv.visitCode();
                methodBody(mv);
                mv.visitMaxs(0, 0);//引数は無視され、再計算される(Write時に再計算されるのでtrace時点では0,0のまま)
                mv.visitEnd();
            }
            success();
        }
        super.visitEnd();
    }

    protected abstract void methodBody(MethodVisitor mv);

    private boolean isTarget(int access, String name, String desc, String signature, String[] exceptions) {
        if (!name.equals(runtimeMethodName))
            return false;

        if (useDesc) {
            if (methodDesc != null && !desc.equals(methodDesc))
                return false;
        }

        if (useAccess) {
            if (access != this.access)
                return false;
        }
        if (useGenerics) {
            if (!Objects.equals(signature, generics))
                return false;
        }
        if (useExceptions) {
            if (!Arrays.equals(exceptions, this.exceptions))
                return false;
        }
        return true;
    }

}
