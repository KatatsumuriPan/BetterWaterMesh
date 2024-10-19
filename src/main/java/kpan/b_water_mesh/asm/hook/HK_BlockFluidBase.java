package kpan.b_water_mesh.asm.hook;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import kpan.b_water_mesh.CustomWaterMeshUtil;
import kpan.b_water_mesh.forge_fluid.AdditionalForgeFluidProperties;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.PropertyFloat;
import net.minecraftforge.fluids.BlockFluidBase;

public class HK_BlockFluidBase {

    public static void onBuildFluidProperty(ImmutableList.Builder builder) {
        for (PropertyFloat meshHeight : AdditionalForgeFluidProperties.MESH_HEIGHTS) {
            builder.add(meshHeight);
        }
    }

    public static IExtendedBlockState onGetExtendedState(BlockFluidBase self, IBlockAccess blockAccess, BlockPos pos, IExtendedBlockState state) {

        float[] heights16 = new float[16];
        Material material = state.getMaterial();
        if (blockAccess.getBlockState(pos.up()).getMaterial() == material) {
            Arrays.fill(heights16, 1);
        } else {
            boolean isOnGround = CustomWaterMeshUtil.canLiquidTouchWith(blockAccess, blockAccess.getBlockState(pos.down()), pos.down(), EnumFacing.UP);
            float[] sideLiquidHeights = CustomWaterMeshUtil.get4SideLiquidHeights(blockAccess, pos, material, isOnGround);
            float liquidHeight = CustomWaterMeshUtil.getLiquidHeightPercent(state);
            heights16 = CustomWaterMeshUtil.get16Heights(sideLiquidHeights, CustomWaterMeshUtil.get4CornerMeshHeights(blockAccess, pos, material, sideLiquidHeights, liquidHeight), liquidHeight);
        }
        for (int i = 0; i < heights16.length; i++) {
            state = state.withProperty(AdditionalForgeFluidProperties.MESH_HEIGHTS[i], heights16[i]);
        }

        return state;
    }
}
