package kpan.b_water_mesh.asm.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import kpan.b_water_mesh.ModTagsGenerated;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class AsmUtil {
    public static final int ASM_VER = Opcodes.ASM5;

    public static final Logger LOGGER = LogManager.getLogger(ModTagsGenerated.MODNAME);

    public static boolean isDeobfEnvironment() { return FMLLaunchHandler.isDeobfuscatedEnvironment(); }

    public static String toMethodDesc(Object returnType, Object... rawDesc) {
        StringBuilder sb = new StringBuilder("(");
        for (Object o : rawDesc) {
            sb.append(toDesc(o));
        }
        sb.append(')');
        sb.append(toDesc(returnType));
        return sb.toString();
    }

    public static String toDesc(Object raw) {
        if (raw instanceof Class<?> clazz) {
            return Type.getDescriptor(clazz);
        } else if (raw instanceof String desc) {
            int arr_dim = 0;
            while (arr_dim < desc.length() - 1) {
                if (desc.charAt(arr_dim) != '[')
                    break;
                arr_dim++;
            }
            String arr_str = arr_dim > 0 ? StringUtils.repeat('[', arr_dim) : "";
            desc = desc.substring(arr_dim);
            if (desc.equals(AsmTypes.VOID) || desc.equals(AsmTypes.BOOL) || desc.equals(AsmTypes.CHAR) || desc.equals(AsmTypes.BYTE) || desc.equals(AsmTypes.SHORT) || desc.equals(AsmTypes.INT)
                    || desc.equals(AsmTypes.LONG) || desc.equals(AsmTypes.FLOAT) || desc.equals(AsmTypes.DOUBLE))
                return arr_str + desc;
            desc = desc.replace('.', '/');
            desc = desc.matches("L.+;") ? desc : "L" + desc + ";";// 全体とマッチ
            return arr_str + desc;
        } else if (raw instanceof Object[]) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (Object[]) raw) {
                sb.append(toDesc(o));
            }
            return sb.toString();
        } else if (raw instanceof Method) {
            return Type.getMethodDescriptor((Method) raw);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("unused")
    public static MethodVisitor traceMethod(MethodVisitor mv, @Nullable String methodName) {
        Textifier p = new MyTextifier(methodName);
        return new TraceMethodVisitor(mv, p);
    }

    public static int toLoadOpcode(String desc) {
        switch (desc) {
            case AsmTypes.BOOL:
            case AsmTypes.CHAR:
            case AsmTypes.BYTE:
            case AsmTypes.SHORT:
            case AsmTypes.INT:
                return Opcodes.ILOAD;
            case AsmTypes.LONG:
                return Opcodes.LLOAD;
            case AsmTypes.FLOAT:
                return Opcodes.FLOAD;
            case AsmTypes.DOUBLE:
                return Opcodes.DLOAD;
            default:
                return Opcodes.ALOAD;
        }
    }
    public static int loadLocals(MethodVisitor mv, String[] descs, int offset) {
        for (String desc : descs) {
            int opcode = AsmUtil.toLoadOpcode(desc);
            mv.visitVarInsn(opcode, offset);
            if (opcode == Opcodes.LLOAD || opcode == Opcodes.DOUBLE)
                offset += 2;
            else
                offset += 1;
        }
        return offset;
    }

    public static int toReturnOpcode(String type) {
        switch (type) {
            case AsmTypes.VOID:
                return Opcodes.RETURN;
            case AsmTypes.BOOL:
            case AsmTypes.CHAR:
            case AsmTypes.BYTE:
            case AsmTypes.SHORT:
            case AsmTypes.INT:
                return Opcodes.IRETURN;
            case AsmTypes.LONG:
                return Opcodes.LRETURN;
            case AsmTypes.FLOAT:
                return Opcodes.FRETURN;
            case AsmTypes.DOUBLE:
                return Opcodes.DRETURN;
            default:
                return Opcodes.ARETURN;
        }
    }

    public static String insertToMethodDesc(String methodDesc, int index, String desc) {
        List<String> list = decomposeMethodDesc(methodDesc);
        String returnDesc = list.remove(list.size() - 1);
        if (index < 0)
            index += list.size() + 1;
        list.add(index, desc);
        return toMethodDesc(returnDesc, list.toArray());
    }

    public static List<String> decomposeMethodDesc(String methodDesc) {
        if (methodDesc.charAt(0) != '(')
            throw new IllegalArgumentException("無効なMethodDesc：" + methodDesc);
        List<String> res = new ArrayList<>();
        int i = 1;
        int startIdx = -1;
        while (true) {
            switch (methodDesc.charAt(i)) {
                case ')' -> {
                    res.add(methodDesc.substring(i + 1));// 戻り値は最後
                    return res;
                }
                case 'L' -> {
                    startIdx = i;
                }
                case ';' -> {
                    res.add(methodDesc.substring(startIdx, i + 1));
                    startIdx = -1;
                }
                default -> {
                    if (startIdx == -1)
                        res.add(String.valueOf(methodDesc.charAt(i)));
                }
            }
            i++;
        }
    }

}
