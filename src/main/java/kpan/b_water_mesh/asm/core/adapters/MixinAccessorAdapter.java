package kpan.b_water_mesh.asm.core.adapters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import kpan.b_water_mesh.ModTagsGenerated;
import kpan.b_water_mesh.asm.core.AccessTransformerForMixin;
import kpan.b_water_mesh.asm.core.AsmNameRemapper;
import kpan.b_water_mesh.asm.core.AsmTypes;
import kpan.b_water_mesh.asm.core.AsmTypes.MethodDesc;
import kpan.b_water_mesh.asm.core.AsmUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

@SuppressWarnings("unused")
public class MixinAccessorAdapter extends MyClassVisitor {

    private final String targetClassName;
    private final Class<?> accessor;
    private final Map<String, FieldInfo> fieldInfoMap = new HashMap<>(); // mcpName -> info
    private final Map<String, MethodInfo> methodInfoMap = new HashMap<>(); // mcpName -> info
    private final Set<String> newFields = new HashSet<>();
    public MixinAccessorAdapter(ClassVisitor cv, String targetClassName, String accessorClassName) {
        super(cv, targetClassName);
        this.targetClassName = targetClassName.replace('.', '/');
        mixinTarget = targetClassName.replace('.', '/');
        try {
            accessor = Class.forName(accessorClassName.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        mixinTarget = null;
    }
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, ArrayUtils.add(interfaces, accessor.getName().replace('.', '/')));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        fieldInfoMap.put(AsmNameRemapper.runtime2McpFieldName(name), new FieldInfo(name, desc, (access & Opcodes.ACC_STATIC) != 0));
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        methodInfoMap.put(AsmNameRemapper.runtime2McpMethodName(name), new MethodInfo(name, desc, (access & Opcodes.ACC_STATIC) != 0, (access & Opcodes.ACC_PRIVATE) != 0));
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        for (Method method : accessor.getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("get_")) {
                // getter
                String mcpName = methodName.substring("get_".length());
                Class<?> type = method.getReturnType();
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                if (type == void.class)
                    throw new IllegalStateException("return type of getter is void!:" + methodName);
                if (method.getParameterCount() != 0)
                    throw new IllegalStateException("parameters of getter are empty!:" + methodName);

                String runtimeName;
                String desc = AsmUtil.toDesc(type);
                if (method.getAnnotation(NewField.class) != null) {
                    if (!newFields.contains(mcpName)) {
                        if (fieldInfoMap.containsKey(mcpName))
                            throw new IllegalStateException("Field duplicated!:" + methodName);
                        String generics = null;// TODO
                        FieldVisitor fv = super.visitField(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0), mcpName, desc, generics, null);
                        if (fv != null)
                            fv.visitEnd();
                        newFields.add(mcpName);
                    }
                    runtimeName = mcpName;// フィールド追加なのでget_の名前からそのまま作る
                } else {
                    FieldInfo fieldInfo = fieldInfoMap.get(mcpName);
                    if (fieldInfo == null)
                        throw new IllegalStateException("Unknown field:" + mcpName + "(" + methodName + ")");
                    runtimeName = fieldInfo.runtimeName;
                    if (!fieldInfo.desc.equals(desc))
                        throw new IllegalStateException("Unmatched field type:" + runtimeName + " of " + methodName + " (" + fieldInfo.desc + "!=" + desc + ")");
                    if (fieldInfo.isStatic != isStatic)
                        throw new IllegalStateException("Unmatched field access(static):" + runtimeName + "(" + methodName + ")");
                }

                MethodVisitor mv = visitMethod(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0), methodName, AsmUtil.toMethodDesc(desc), null, null);
                if (mv != null) {
                    mv.visitCode();
                    if (isStatic) {
                        mv.visitFieldInsn(Opcodes.GETSTATIC, targetClassName, runtimeName, desc);
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitFieldInsn(Opcodes.GETFIELD, targetClassName, runtimeName, desc);
                    }
                    mv.visitInsn(AsmUtil.toReturnOpcode(desc));
                    mv.visitMaxs(0, 0);// 引数は無視され、再計算される(Write時に再計算されるのでtrace時点では0,0のまま)
                    mv.visitEnd();
                }

            } else if (methodName.startsWith("set_")) {
                // setter
                String mcpName = methodName.substring("set_".length());
                Class<?> type = method.getParameterTypes()[0];
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                if (method.getReturnType() != void.class)
                    throw new IllegalStateException("return type of getter is not void!:" + methodName);
                if (method.getParameterCount() != 1)
                    throw new IllegalStateException("parameters num of getter is not 1!:" + methodName);

                String runtimeName;
                String desc = AsmUtil.toDesc(type);
                if (method.getAnnotation(NewField.class) != null) {
                    if (!newFields.contains(mcpName)) {
                        if (fieldInfoMap.containsKey(mcpName))
                            throw new IllegalStateException("Field duplicated!:" + methodName);
                        String generics = null;// TODO
                        FieldVisitor fv = visitField(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0), mcpName, desc, generics, null);
                        if (fv != null)
                            fv.visitEnd();
                        newFields.add(mcpName);
                    }
                    runtimeName = mcpName;
                } else {
                    FieldInfo fieldInfo = fieldInfoMap.get(mcpName);
                    if (fieldInfo == null)
                        throw new IllegalStateException("Unknown field:" + mcpName + "(" + methodName + ")");
                    runtimeName = fieldInfo.runtimeName;
                    if (!fieldInfo.desc.equals(desc))
                        throw new IllegalStateException("Unmatched field type:" + runtimeName + "(" + methodName + ")");
                    if (fieldInfo.isStatic != isStatic)
                        throw new IllegalStateException("Unmatched field access(static):" + runtimeName + "(" + methodName + ")");
                }

                MethodVisitor mv = visitMethod(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0), methodName, AsmUtil.toMethodDesc(AsmTypes.VOID, desc), null, null);
                if (mv != null) {
                    mv.visitCode();
                    if (isStatic) {
                        mv.visitVarInsn(AsmUtil.toLoadOpcode(desc), 0);
                        mv.visitFieldInsn(Opcodes.PUTSTATIC, targetClassName, runtimeName, desc);
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitVarInsn(AsmUtil.toLoadOpcode(desc), 1);
                        mv.visitFieldInsn(Opcodes.PUTFIELD, targetClassName, runtimeName, desc);
                    }
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);// 引数は無視され、再計算される(Write時に再計算されるのでtrace時点では0,0のまま)
                    mv.visitEnd();
                }
            } else if (method.getAnnotation(NewMethod.class) != null || !methodInfoMap.containsKey(methodName)) {
                // staticじゃないならinterfaceがメソッドを持ってるので問題ない
                // staticなものは無効とする（無視）
            } else {
                // bridge
                String methodDesc = AsmUtil.toDesc(method);
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                MethodInfo methodInfo = methodInfoMap.get(methodName);
                String runtimeName = methodInfo.runtimeName;
                if (!methodInfo.methodDesc.equals(methodDesc))
                    throw new IllegalStateException("Unknown method desc:" + runtimeName + "(" + methodName + ")");
                if (methodInfo.isStatic != isStatic)
                    throw new IllegalStateException("Unmatched method access(static):" + runtimeName + "(" + methodName + ")");

                if (methodName.equals(runtimeName)) {
                    AccessTransformerForMixin.toPublic(targetClassName, methodName, methodDesc);
                } else {
                    boolean is_private = methodInfo.isPrivate;
                    MethodVisitor mv = visitMethod(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0), methodName, methodDesc, null, null);
                    if (mv != null) {
                        mv.visitCode();
                        int offset = 0;
                        // this
                        if (!isStatic) {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            offset = 1;
                        }
                        // params
                        for (int i = 0; i < method.getParameterCount(); i++) {
                            int opcode = AsmUtil.toLoadOpcode(AsmUtil.toDesc(method.getParameterTypes()[i]));
                            mv.visitVarInsn(opcode, offset);
                            if (opcode == Opcodes.LLOAD || opcode == Opcodes.DOUBLE)
                                offset += 2;
                            else
                                offset += 1;
                        }
                        // invoke
                        if (isStatic)
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, targetClassName, runtimeName, methodDesc, false);
                        else if (is_private)
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, targetClassName, runtimeName, methodDesc, false);
                        else
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, targetClassName, runtimeName, methodDesc, false);
                        // return
                        mv.visitInsn(AsmUtil.toReturnOpcode(AsmUtil.toDesc(method.getReturnType())));

                        mv.visitMaxs(0, 0);// 引数は無視され、再計算される(Write時に再計算されるのでtrace時点では0,0のまま)
                        mv.visitEnd();
                    }
                }
            }
        }
        success();
        super.visitEnd();
    }

    private static String mixinTarget = null;

    public static ClassVisitor transformAccessor(ClassVisitor cv, String transformedName) {
        if (mixinTarget == null || !transformedName.startsWith(ModTagsGenerated.MODGROUP + ".asm.acc."))
            return cv;

        cv = new MyClassVisitor(cv, transformedName) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ((access & Opcodes.ACC_STATIC) != 0) {
                    mv = new MethodVisitor(AsmUtil.ASM_VER, mv) {
                        @Override
                        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                        }
                        @Override
                        public void visitInsn(int opcode) {
                        }
                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                        }
                        @Override
                        public void visitVarInsn(int opcode, int var) {
                        }
                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                        }
                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        }
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        }
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        }
                        @Override
                        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                        }
                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                        }
                        @Override
                        public void visitLabel(Label label) {
                        }
                        @Override
                        public void visitLdcInsn(Object cst) {
                        }
                        @Override
                        public void visitIincInsn(int var, int increment) {
                        }
                        @Override
                        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                        }
                        @Override
                        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                        }
                        @Override
                        public void visitMultiANewArrayInsn(String desc, int dims) {
                        }
                        @Override
                        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                            return null;
                        }
                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                        }
                        @Override
                        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                            return null;
                        }
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        }
                        @Override
                        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
                            return null;
                        }
                        @Override
                        public void visitLineNumber(int line, Label start) {
                        }
                        @Override
                        public void visitMaxs(int maxStack, int maxLocals) {
                        }
                        @Override
                        public void visitEnd() {
                            if (name.startsWith("get_")) {
                                // invoke
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, mixinTarget.replace('.', '/'), name, desc, false);
                                // return
                                mv.visitInsn(AsmUtil.toReturnOpcode(desc.substring("()".length())));
                            } else if (name.startsWith("set_")) {
                                // params
                                mv.visitVarInsn(AsmUtil.toLoadOpcode(desc.substring(1, desc.length() - 2)), 0);
                                // invoke
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, mixinTarget.replace('.', '/'), name, desc, false);
                                // return
                                mv.visitInsn(Opcodes.RETURN);
                            } else {
                                MethodDesc md = MethodDesc.fromMethodDesc(desc);
                                // params
                                AsmUtil.loadLocals(mv, md.paramsDesc, 0);
                                // invoke
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, mixinTarget.replace('.', '/'), name, desc, false);
                                // return
                                mv.visitInsn(AsmUtil.toReturnOpcode(md.returnDesc));
                            }
                            mv.visitMaxs(0, 0);// 引数は無視され、再計算される(Write時に再計算されるのでtrace時点では0,0のまま)
                            super.visitEnd();
                        }
                    };
                }
                return mv;
            }
        }.setSuccessExpected(0);

        return cv;
    }

    /**
     * ターゲットクラスに新しいフィールドとして追加する。
     * getterとsetterの両方で重複して付ける必要がある
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NewField {
    }

    /**
     * ターゲットクラスに新しいメソッドとして追加する。
     * staticメソッドに対しては使用できない。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NewMethod {
    }

    private static class FieldInfo {
        public final String runtimeName;
        public final String desc;
        public final boolean isStatic;

        private FieldInfo(String runtimeName, String desc, boolean isStatic) {
            this.runtimeName = runtimeName;
            this.desc = desc;
            this.isStatic = isStatic;
        }
    }

    private static class MethodInfo {
        public final String runtimeName;
        public final String methodDesc;
        public final boolean isStatic;
        public final boolean isPrivate;

        private MethodInfo(String runtimeName, String methodDesc, boolean isStatic, boolean isPrivate) {
            this.runtimeName = runtimeName;
            this.methodDesc = methodDesc;
            this.isStatic = isStatic;
            this.isPrivate = isPrivate;
        }
    }
}
