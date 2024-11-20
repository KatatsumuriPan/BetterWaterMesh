package kpan.b_water_mesh.asm.core;

import java.util.ArrayList;
import java.util.List;
import kpan.b_water_mesh.ModTagsGenerated;

@SuppressWarnings("unused")
public class AsmTypes {

    public static final String HOOK = ModTagsGenerated.MODGROUP.replace('.', '/') + "/asm/hook/";
    public static final String ACC = ModTagsGenerated.MODGROUP.replace('.', '/') + "/asm/acc/";
    public static final String VOID = "V";
    public static final String BOOL = "Z";
    public static final String CHAR = "C";
    public static final String BYTE = "B";
    public static final String SHORT = "S";
    public static final String INT = "I";
    public static final String LONG = "J";
    public static final String FLOAT = "F";
    public static final String DOUBLE = "D";
    public static final String OBJECT = "java/lang/Object";
    public static final String OBJECT_ARR = "[java/lang/Object";
    public static final String STRING = "java/lang/String";
    public static final String BLOCKPOS = "net/minecraft/util/math/BlockPos";
    public static final String BLOCK = "net/minecraft/block/Block";
    public static final String IBLOCKSTATE = "net/minecraft/block/state/IBlockState";
    public static final String WORLD = "net/minecraft/world/World";
    public static final String ENTITY = "net/minecraft/entity/Entity";
    public static final String ENTITY_LIVING_BASE = "net/minecraft/entity/EntityLivingBase";
    public static final String PLAYER = "net/minecraft/entity/player/EntityPlayer";
    public static final String PLAYERSP = "net/minecraft/client/entity/EntityPlayerSP";
    public static final String TILEENTITY = "net/minecraft/tileentity/TileEntity";
    public static final String NBTTAGCOMPOUND = "net/minecraft/nbt/NBTTagCompound";
    public static final String MINECRAFT = "net/minecraft/client/Minecraft";
    public static final String LIST = "java/util/List";
    public static final String SET = "java/util/Set";
    public static final String INT_CLASS = "java/lang/Integer";
    public static final String ITEMSTACK = "net/minecraft/item/ItemStack";
    public static final String ITEM = "net/minecraft/item/Item";
    public static final String IBLOCKPROPERTIES = "net/minecraft/block/state/IBlockProperties";
    public static final String IBLOCKACCESS = "net/minecraft/world/IBlockAccess";
    public static final String FLUID = "net/minecraftforge/fluids/Fluid";
    public static final String FLUIDSTACK = "net/minecraftforge/fluids/FluidStack";
    public static final String CONTAINER = "net/minecraft/inventory/Container";

    public static final String METHOD_VOID = "()V";

    public static String toArray(String desc) {
        return "[" + desc;
    }

    public static String toGenerics(String base, String... descs) {
        StringBuilder sb = new StringBuilder();
        sb.append(AsmUtil.toDesc(base));
        sb.setLength(sb.length() - 1);
        sb.append('<');
        for (String desc : descs) {
            sb.append(AsmUtil.toDesc(desc));
        }
        sb.append(">;");
        return sb.toString();
    }
    public static String listGenerics(String desc) {
        return "L" + LIST + "<" + AsmUtil.toDesc(desc) + ">;";
    }

    public static class MethodDesc {
        public static MethodDesc fromMethodDesc(String methodDesc) {
            if (methodDesc.charAt(0) != '(')
                throw new IllegalArgumentException("methodDesc is not valid!");
            List<String> params = new ArrayList<>();
            int index = 1;
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = methodDesc.charAt(index);
                if (c == ')')
                    break;
                switch (c) {
                    case 'Z':
                    case 'C':
                    case 'B':
                    case 'S':
                    case 'I':
                    case 'J':
                    case 'F':
                    case 'D':
                        sb.append(c);
                        params.add(sb.toString());
                        sb.setLength(0);
                        index++;
                        break;
                    case '[':
                        sb.append('[');
                        index++;
                        break;
                    case 'L': {
                        int end_index = methodDesc.indexOf(';', index);
                        params.add(methodDesc.substring(index + 1, end_index));
                        index = end_index + 1;
                        break;
                    }
                    default:
                        throw new RuntimeException("Invalid Char:" + c);
                }
            }
            return new MethodDesc(methodDesc.substring(index + 1), params.toArray(new String[0]));
        }

        public final String returnDesc;
        public final String[] paramsDesc;

        public MethodDesc(String returnDesc, String[] paramsDesc) {
            this.returnDesc = returnDesc;
            this.paramsDesc = paramsDesc;
        }
    }

}
