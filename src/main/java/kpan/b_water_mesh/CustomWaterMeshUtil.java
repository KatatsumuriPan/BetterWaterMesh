package kpan.b_water_mesh;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class CustomWaterMeshUtil {
    public static final float OFFSET_TO_FIX_Z_FIGHTING = 0.001F;
    public static final float MODEL_BORDER = 0.2F;

    public static final float IS_WALL = -1F;
    public static final float IS_AIR = -2F;
    public static final float HAS_LIQUID_ABOVE = -3F;
    public static final float INNER_MAX_HEIGHT = 0.9F;

    public static float[] get4SideLiquidHeights(IBlockAccess blockAccess, BlockPos pos, Material materialCondition, boolean isOnGround) {
        float[] res = new float[4];
        for (int i = 0; i < EnumFacing.HORIZONTALS.length; i++) {
            EnumFacing side = EnumFacing.HORIZONTALS[i];
            IBlockState sideState = blockAccess.getBlockState(pos.offset(side));
            if (blockAccess.getBlockState(pos.offset(side).up()).getMaterial() == materialCondition)
                res[i] = HAS_LIQUID_ABOVE;
            else if (sideState.getMaterial() == materialCondition)
                res[i] = getLiquidHeightPercent(sideState);
            else if (isOnGround && canLiquidTouchWith(blockAccess, sideState, pos, side.getOpposite()))
                res[i] = IS_WALL;
            else
                res[i] = IS_AIR;
        }
        return res;
    }
    public static float[] get4CornerMeshHeights(IBlockAccess blockAccess, BlockPos pos, Material materialCondition, float[] sideLiquidHeights, float centerHeight) {
        float[] res = new float[4];
        for (int i = 0; i < EnumFacing.HORIZONTALS.length; i++) {
            EnumFacing side = EnumFacing.HORIZONTALS[i];
            int nextI = (i + 1) % 4;
            EnumFacing nextSide = EnumFacing.HORIZONTALS[nextI];
            if (sideLiquidHeights[i] == HAS_LIQUID_ABOVE || sideLiquidHeights[nextI] == HAS_LIQUID_ABOVE) {
                res[i] = 1;
                continue;
            }
            if (sideLiquidHeights[i] < 0 && sideLiquidHeights[nextI] < 0) {
                float den = 0;
                float sum = 0;
                float b = 9f;
                if (sideLiquidHeights[i] == IS_WALL) {
                    sum += b;
                    den += b;
                } else {
                    den += 1;
                }
                if (sideLiquidHeights[nextI] == IS_WALL) {
                    sum += b;
                    den += b;
                } else {
                    den += 1;
                }

                float a = 0.8F;
                float h = centerHeight / INNER_MAX_HEIGHT * a + (1 - a);
                sum *= h;
                res[i] = sum / den;
            } else {
                BlockPos cornerPos = pos.offset(side).offset(nextSide);
                if (blockAccess.getBlockState(cornerPos.up()).getMaterial() == materialCondition) {
                    res[i] = 1;
                } else {
                    int liquidNum = 0;
                    float den = 0;
                    float sum = 0;
                    float liquidWeight = 13F;
                    boolean wallExists = false;
                    if (sideLiquidHeights[i] >= 0) {
                        sum += sideLiquidHeights[i] * liquidWeight;
                        den += liquidWeight;
                        liquidNum++;
                    } else if (sideLiquidHeights[i] == IS_WALL) {
                        wallExists = true;
                        sum += 1.0f;
                        den += 1.0f;
                    } else {
                        den += 1;
                    }
                    if (sideLiquidHeights[nextI] >= 0) {
                        sum += sideLiquidHeights[nextI] * liquidWeight;
                        den += liquidWeight;
                        liquidNum++;
                    } else if (sideLiquidHeights[nextI] == IS_WALL) {
                        wallExists = true;
                        sum += 1.0f;
                        den += 1.0f;
                    } else {
                        den += 1;
                    }
                    IBlockState cornerState = blockAccess.getBlockState(cornerPos);
                    if (cornerState.getMaterial() == materialCondition) {
                        sum += getLiquidHeightPercent(cornerState) * liquidWeight;
                        den += liquidWeight;
                        liquidNum++;
                    } else if ((sideLiquidHeights[i] >= 0 && canLiquidTouchWith(blockAccess, cornerState, cornerPos, nextSide.getOpposite())
                            || sideLiquidHeights[nextI] >= 0 && canLiquidTouchWith(blockAccess, cornerState, cornerPos, side.getOpposite()))) {
                        wallExists = true;
                        sum += 1.0f;
                        den += 1.0f;
                    } else {
                        den += 1;
                    }

                    sum += centerHeight * liquidWeight;
                    den += liquidWeight;
                    if (!wallExists && liquidNum < 3)
                        sum *= 0.9F;
                    res[i] = sum / den;
                }
            }
        }
        return res;
    }
    public static float[] get16Heights(float[] sideLiquidHeights, float[] cornerMeshHeights, float centerHeight) {
        // 0  4  8  c
        //
        // 1  5  9  d
        //     10
        // 2  6  a  e
        //
        // 3  7  b  f
        float[] res = new float[16];

        res[0x0] = cornerMeshHeights[EnumFacing.WEST.getHorizontalIndex()];
        res[0xc] = cornerMeshHeights[EnumFacing.NORTH.getHorizontalIndex()];
        res[0xf] = cornerMeshHeights[EnumFacing.EAST.getHorizontalIndex()];
        res[0x3] = cornerMeshHeights[EnumFacing.SOUTH.getHorizontalIndex()];


        get16Height_edge(res, 0x1, 0x2, 0x0, 0x3, EnumFacing.WEST, sideLiquidHeights, centerHeight);
        get16Height_edge(res, 0x4, 0x8, 0x0, 0xc, EnumFacing.NORTH, sideLiquidHeights, centerHeight);
        get16Height_edge(res, 0x7, 0xb, 0x3, 0xf, EnumFacing.SOUTH, sideLiquidHeights, centerHeight);
        get16Height_edge(res, 0xd, 0xe, 0xc, 0xf, EnumFacing.EAST, sideLiquidHeights, centerHeight);
        res[0x5] = get16Height_inner(res, 0x0, 0x1, 0x4, EnumFacing.WEST, EnumFacing.NORTH, sideLiquidHeights, centerHeight);
        res[0x6] = get16Height_inner(res, 0x3, 0x2, 0x7, EnumFacing.WEST, EnumFacing.SOUTH, sideLiquidHeights, centerHeight);
        res[0x9] = get16Height_inner(res, 0xc, 0x8, 0xd, EnumFacing.EAST, EnumFacing.NORTH, sideLiquidHeights, centerHeight);
        res[0xa] = get16Height_inner(res, 0xf, 0xb, 0xe, EnumFacing.EAST, EnumFacing.SOUTH, sideLiquidHeights, centerHeight);

        return res;
    }
    public static void get16Height_edge(float[] heights, int index1, int index2, int cornerIndex1, int cornerIndex2, EnumFacing side, float[] sideLiquidHeights, float centerHeight) {
        if (sideLiquidHeights[side.getHorizontalIndex()] == HAS_LIQUID_ABOVE) {
            heights[index1] = heights[index2] = 1;
        } else if (sideLiquidHeights[side.getHorizontalIndex()] >= 0) {
            heights[index1] = heights[index2] = (sideLiquidHeights[side.getHorizontalIndex()] + centerHeight) / 2;
        } else if (sideLiquidHeights[side.getHorizontalIndex()] == IS_WALL) {
            float edgeMiddleHeight = (heights[cornerIndex1] + heights[cornerIndex2] + centerHeight * 14) / 16F;
            // 壁面なので普通に重み付き平均
            heights[index1] = (edgeMiddleHeight * MODEL_BORDER + heights[cornerIndex1] * (0.5F - MODEL_BORDER)) * 2;
            heights[index2] = (edgeMiddleHeight * MODEL_BORDER + heights[cornerIndex2] * (0.5F - MODEL_BORDER)) * 2;
        } else if (heights[cornerIndex1] + heights[cornerIndex2] < centerHeight * 2) {
            float edgeMiddleHeight = (heights[cornerIndex1] + heights[cornerIndex2] + centerHeight * 14) / 16F;
            // 角より中心の方が高いときは低くなりすぎないように
            heights[index1] = (edgeMiddleHeight * (MODEL_BORDER + 0.35F) + heights[cornerIndex1] * (0.65F - MODEL_BORDER));
            heights[index2] = (edgeMiddleHeight * (MODEL_BORDER + 0.35F) + heights[cornerIndex2] * (0.65F - MODEL_BORDER));
        } else {
            float edgeMiddleHeight = (heights[cornerIndex1] + heights[cornerIndex2] + centerHeight * 14) / 16F;
            // 中心より角の方が高いときはがっつり低めに
            heights[index1] = Math.max((edgeMiddleHeight * MODEL_BORDER + heights[cornerIndex1] * (0.5F - MODEL_BORDER)) * 2 * 0.7F - 0.1F, 0.02F);
            heights[index2] = Math.max((edgeMiddleHeight * MODEL_BORDER + heights[cornerIndex2] * (0.5F - MODEL_BORDER)) * 2 * 0.7F - 0.1F, 0.02F);
        }
    }
    public static float get16Height_inner(float[] heights, int cornerIndex, int sideIndex1, int sideIndex2, EnumFacing side1, EnumFacing side2, float[] sideLiquidHeights, float centerHeight) {
        if (sideLiquidHeights[side1.getHorizontalIndex()] >= 0) {
            float edge1Height = (sideLiquidHeights[side1.getHorizontalIndex()] + centerHeight) / 2;
            float side1Height = edge1Height * (1 - MODEL_BORDER) + centerHeight * MODEL_BORDER;
            if (sideLiquidHeights[side2.getHorizontalIndex()] >= 0) {
                float edge2Height = (sideLiquidHeights[side2.getHorizontalIndex()] + centerHeight) / 2;
                float side2Height = edge2Height * (1 - MODEL_BORDER) + centerHeight * MODEL_BORDER;
                return (side1Height + side2Height) / 2;
            } else {
                return side1Height;
            }
        } else {
            if (sideLiquidHeights[side2.getHorizontalIndex()] >= 0) {
                float edge2Height = (sideLiquidHeights[side2.getHorizontalIndex()] + centerHeight) / 2;
                float side2Height = edge2Height * (1 - MODEL_BORDER) + centerHeight * MODEL_BORDER;
                return side2Height;
            } else {
                return (heights[cornerIndex] + heights[sideIndex1] + heights[sideIndex2] + centerHeight * 10) / 13F;
            }
        }

    }

    public static boolean canLiquidTouchWith(IBlockAccess blockAccess, IBlockState state, BlockPos pos, EnumFacing side) {
        if (state.isFullBlock())
            return true;
        if (state.isSideSolid(blockAccess, pos, side))
            return true;
        if (state.getBlockFaceShape(blockAccess, pos, side.getOpposite()) == BlockFaceShape.SOLID)
            return true;

        return false;
    }
    public static float getLiquidHeightPercent(IBlockState state) {
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level >= 8)
            return 1;
        else
            return (1 - level / 7F) * INNER_MAX_HEIGHT;
    }
}
