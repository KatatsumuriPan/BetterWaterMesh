package kpan.b_water_mesh.asm.hook;

import kpan.b_water_mesh.asm.acc.ACC_TileEntityFurnace;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

@SuppressWarnings("unused")
public class HK_TileEntityFurnace {

    public static int getItemBurnTime(ItemStack stack) {
        int burnTime = TileEntityFurnace.getItemBurnTime(stack);
        if (burnTime <= 0)
            return burnTime;
        //燃料の燃焼時間を1/8に
        return Math.max(burnTime / 8, 1);
    }

    public static String getName(TileEntityFurnace self) {
        //めんどくさかったので超テキトー
        //色々設定して色々表示する
        ACC_TileEntityFurnace acc = (ACC_TileEntityFurnace) self;
        acc.set_furnaceCustomName("FURNACE");
        acc.set_openCount(acc.get_openCount() + 1);
        ACC_TileEntityFurnace.set_openCountStatic(ACC_TileEntityFurnace.get_openCountStatic() + 1);
        return "furnace:" + (acc.get_cookTime()) + " SLOTS" + ACC_TileEntityFurnace.get_SLOTS_BOTTOM().length + " OPEN" + acc.get_openCount() + " TOTAL" + ACC_TileEntityFurnace.get_openCountStatic();
    }
}
