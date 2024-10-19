package kpan.b_water_mesh.renderer;

import java.util.Arrays;
import kpan.b_water_mesh.CustomWaterMeshUtil;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

public class CustomLiquidRenderer extends BlockFluidRenderer {

    private static final float SHIFT_FOR_FIX_Z_FIGHTING = 0.001F;
    private static final float DOWN_FACE_LIGHT = 0.5F;
    private static final float INNER_FACE_LIGHT = 0.7F;

    private boolean constructed;
    private final BlockColors blockColors;
    private LiquidTexture textureWater;
    private LiquidTexture textureLava;

    public CustomLiquidRenderer(BlockColors blockColors) {
        super(blockColors);
        this.blockColors = blockColors;
        constructed = true;
        initAtlasSprites();
    }

    @Override
    protected void initAtlasSprites() {
        super.initAtlasSprites();
        if (!constructed)
            return;
        TextureMap texturemap = Minecraft.getMinecraft().getTextureMapBlocks();
        textureWater = new LiquidTexture(texturemap.getAtlasSprite("minecraft:blocks/water_still"), texturemap.getAtlasSprite("minecraft:blocks/water_flow"), texturemap.getAtlasSprite("minecraft:blocks/water_overlay"));
        textureLava = new LiquidTexture(texturemap.getAtlasSprite("minecraft:blocks/lava_still"), texturemap.getAtlasSprite("minecraft:blocks/lava_flow"));
    }

    @Override
    public boolean renderFluid(IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn) {
        LiquidTexture texture = blockStateIn.getMaterial() == Material.LAVA ? textureLava : textureWater;
        return renderFluid(blockAccess, blockStateIn, blockPosIn, bufferBuilderIn, texture);
    }

    private boolean renderFluid(IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn, LiquidTexture texture) {
        boolean shouldRenderUp = blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.UP);
        boolean shouldRenderDown = blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.DOWN);
        boolean[] shouldRenderSides = new boolean[4];
        for (int i = 0; i < EnumFacing.HORIZONTALS.length; i++) {
            shouldRenderSides[i] = blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.HORIZONTALS[i]);
        }

        if (!shouldRenderUp && !shouldRenderDown && !shouldRenderSides[0] && !shouldRenderSides[1] && !shouldRenderSides[2] && !shouldRenderSides[3]) {
            return false;
        }
        int color = blockColors.colorMultiplier(blockStateIn, blockAccess, blockPosIn, 0);
        int light = blockStateIn.getPackedLightmapCoords(blockAccess, blockPosIn);

        Material material = blockStateIn.getMaterial();
        float[] heights16 = new float[16];
        if (blockAccess.getBlockState(blockPosIn.up()).getMaterial() == material) {
            Arrays.fill(heights16, 1);
        } else {
            boolean isOnGround = CustomWaterMeshUtil.canLiquidTouchWith(blockAccess, blockAccess.getBlockState(blockPosIn.down()), blockPosIn.down(), EnumFacing.UP);
            float[] sideLiquidHeights = CustomWaterMeshUtil.get4SideLiquidHeights(blockAccess, blockPosIn, material, isOnGround);
            float liquidHeight = CustomWaterMeshUtil.getLiquidHeightPercent(blockStateIn);
            heights16 = CustomWaterMeshUtil.get16Heights(sideLiquidHeights, CustomWaterMeshUtil.get4CornerMeshHeights(blockAccess, blockPosIn, material, sideLiquidHeights, liquidHeight), liquidHeight);
        }

        float flowAngle = BlockLiquid.getSlopeAngle(blockAccess, blockPosIn, material, blockStateIn);

        if (shouldRenderUp) {
            renderUp(blockPosIn, color, light, heights16, bufferBuilderIn, flowAngle, texture);
        }

        if (shouldRenderDown) {
            double x = blockPosIn.getX();
            double y = blockPosIn.getY() + SHIFT_FOR_FIX_Z_FIGHTING;
            double z = blockPosIn.getZ();
            float minU = texture.atlasSpriteStill.getMinU();
            float maxU = texture.atlasSpriteStill.getMaxU();
            float minV = texture.atlasSpriteStill.getMinV();
            float maxV = texture.atlasSpriteStill.getMaxV();
            int l = blockStateIn.getPackedLightmapCoords(blockAccess, blockPosIn.down());
            int l_sky = l >> 16 & 65535;
            int l_block = l & 65535;
            bufferBuilderIn.pos(x, y, z + 1).color(DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, 1.0F).tex(minU, maxV).lightmap(l_sky, l_block).endVertex();
            bufferBuilderIn.pos(x, y, z).color(DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, 1.0F).tex(minU, minV).lightmap(l_sky, l_block).endVertex();
            bufferBuilderIn.pos(x + 1, y, z).color(DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, 1.0F).tex(maxU, minV).lightmap(l_sky, l_block).endVertex();
            bufferBuilderIn.pos(x + 1, y, z + 1).color(DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, DOWN_FACE_LIGHT, 1.0F).tex(maxU, maxV).lightmap(l_sky, l_block).endVertex();
        }

        renderSides(blockAccess, blockPosIn, color, heights16, bufferBuilderIn, texture, shouldRenderSides);

        return true;
    }

    private void renderUp(BlockPos pos, int color, int light, float[] heights16, BufferBuilder bufferBuilder, float flowAngle, LiquidTexture texture) {

        // 0  4  8  c
        //
        // 1  5  9  d
        //     10
        // 2  6  a  e
        //
        // 3  7  b  f

        vertexUp(pos, bufferBuilder, color, light, new int[]{0x0, 0x1, 0x5, 0x4}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x1, 0x2, 0x6, 0x5}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x3, 0x7, 0x6, 0x2}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x4, 0x5, 0x9, 0x8}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x5, 0x6, 0xa, 0x9}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x6, 0x7, 0xb, 0xa}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x9, 0xd, 0xc, 0x8}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0x9, 0xa, 0xe, 0xd}, heights16, flowAngle, texture);
        vertexUp(pos, bufferBuilder, color, light, new int[]{0xa, 0xb, 0xf, 0xe}, heights16, flowAngle, texture);
    }

    private void vertexUp(BlockPos pos, BufferBuilder bufferBuilder, int color, int light, int[] indices, float[] heights16, float flowAngle, LiquidTexture texture) {
        for (int index : indices) {
            vertexUp(pos, bufferBuilder, color, light, index, heights16, flowAngle, texture);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            vertexUp(pos, bufferBuilder, color, light, indices[i], heights16, flowAngle, texture);
        }
    }

    private void vertexUp(BlockPos pos, BufferBuilder bufferBuilder, int color, int light, int idx, float[] heights16, float flowAngle, LiquidTexture texture) {

        double x, z;
        double y = heights16[idx];
        if (y <= SHIFT_FOR_FIX_Z_FIGHTING)
            y = SHIFT_FOR_FIX_Z_FIGHTING;
        if (idx == 0x10) {
            x = 0.5;
            z = 0.5;
        } else {
            x = switch (idx >> 2) {
                case 0 -> 0;
                case 1 -> CustomWaterMeshUtil.MODEL_BORDER;
                case 2 -> 1 - CustomWaterMeshUtil.MODEL_BORDER;
                case 3 -> 1;
                default -> throw new IllegalStateException("Unexpected value: " + (idx >> 2));
            };
            z = switch (idx % 4) {
                case 0 -> 0;
                case 1 -> CustomWaterMeshUtil.MODEL_BORDER;
                case 2 -> 1 - CustomWaterMeshUtil.MODEL_BORDER;
                case 3 -> 1;
                default -> throw new IllegalStateException("Unexpected value: " + (idx % 4));
            };
        }

        float u;
        float v;
        if (flowAngle < -999.0F) {
            TextureAtlasSprite textureAtlasSprite = texture.atlasSpriteStill;
            u = textureAtlasSprite.getInterpolatedU(x * 16);
            v = textureAtlasSprite.getInterpolatedV(z * 16);
        } else {
            TextureAtlasSprite textureAtlasSprite = texture.atlasSpriteFlow;
            float s = MathHelper.sin(flowAngle);
            float c = MathHelper.cos(flowAngle);
            float f23 = 8.0F;
            float uNW = textureAtlasSprite.getInterpolatedU(8.0F + (-c - s) * 5.0F);
            float vNW = textureAtlasSprite.getInterpolatedV(8.0F + (-c + s) * 5.0F);
            float uSW = textureAtlasSprite.getInterpolatedU(8.0F + (-c + s) * 5.0F);
            float vSW = textureAtlasSprite.getInterpolatedV(8.0F + (c + s) * 5.0F);
            float uSE = textureAtlasSprite.getInterpolatedU(8.0F + (c + s) * 5.0F);
            float vSE = textureAtlasSprite.getInterpolatedV(8.0F + (c - s) * 5.0F);
            float uNE = textureAtlasSprite.getInterpolatedU(8.0F + (c - s) * 5.0F);
            float vNE = textureAtlasSprite.getInterpolatedV(8.0F + (-c - s) * 5.0F);

            float uN = (uNE - uNW) * (float) x + uNW;
            float uS = (uSE - uSW) * (float) x + uSW;
            u = (uS - uN) * (float) z + uN;

            float vW = (vSW - vNW) * (float) z + vNW;
            float vE = (vSE - vNE) * (float) z + vNE;
            v = (vE - vW) * (float) x + vW;
        }
        int l_sky = light >> 16 & 65535;
        int l_block = light & 65535;
        bufferBuilder
                .pos(pos.getX() + x, pos.getY() + y, pos.getZ() + z)
                .color((color >> 16) & 0xff, (color >> 8) & 0xff, (color >> 0) & 0xff, 0xff)
                .tex(u, v)
                .lightmap(l_sky, l_block)
                .endVertex();
    }

    private void renderSides(IBlockAccess blockAccess, BlockPos pos, int color, float[] heights16, BufferBuilder bufferBuilder, LiquidTexture texture, boolean[] shouldRenderSides) {

        IBlockState state = blockAccess.getBlockState(pos);

        if (shouldRenderSides[EnumFacing.NORTH.getHorizontalIndex()]) {
            TextureAtlasSprite textureAtlasSprite = texture.atlasSpriteFlow;
            if (texture.atlasSpriteOverlay != null) {
                if (state.getBlockFaceShape(blockAccess, pos, EnumFacing.NORTH.getOpposite()) == BlockFaceShape.SOLID) {
                    textureAtlasSprite = texture.atlasSpriteOverlay;
                }
            }
            int light = state.getPackedLightmapCoords(blockAccess, pos.offset(EnumFacing.NORTH));
            vertexNorth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 0, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 0, heights16[0x0], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, color, light, 1, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1, heights16[0xc], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            // 逆
            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;
            vertexNorth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 0, heights16[0x0], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 0, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, colorDark, light, 1, heights16[0xc], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
        }

        if (shouldRenderSides[EnumFacing.SOUTH.getHorizontalIndex()]) {
            TextureAtlasSprite textureAtlasSprite = texture.atlasSpriteFlow;
            if (texture.atlasSpriteOverlay != null) {
                if (state.getBlockFaceShape(blockAccess, pos, EnumFacing.SOUTH.getOpposite()) == BlockFaceShape.SOLID) {
                    textureAtlasSprite = texture.atlasSpriteOverlay;
                }
            }
            int light = state.getPackedLightmapCoords(blockAccess, pos.offset(EnumFacing.SOUTH));
            vertexSouth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 0, heights16[0x3], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 0, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, color, light, 1, heights16[0xf], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            // 逆

            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;
            vertexSouth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 0, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 0, heights16[0x3], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, colorDark, light, 1, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1, heights16[0xf], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
        }

        if (shouldRenderSides[EnumFacing.WEST.getHorizontalIndex()]) {
            TextureAtlasSprite textureAtlasSprite = texture.atlasSpriteFlow;
            if (texture.atlasSpriteOverlay != null) {
                if (state.getBlockFaceShape(blockAccess, pos, EnumFacing.WEST.getOpposite()) == BlockFaceShape.SOLID) {
                    textureAtlasSprite = texture.atlasSpriteOverlay;
                }
            }
            int light = state.getPackedLightmapCoords(blockAccess, pos.offset(EnumFacing.WEST));
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x0], 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x3], 1, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);

            // 逆
            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;

            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x0], 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x3], 1, textureAtlasSprite);

        }

        if (shouldRenderSides[EnumFacing.EAST.getHorizontalIndex()]) {
            TextureAtlasSprite textureAtlasSprite = texture.atlasSpriteFlow;
            if (texture.atlasSpriteOverlay != null) {
                if (state.getBlockFaceShape(blockAccess, pos, EnumFacing.EAST.getOpposite()) == BlockFaceShape.SOLID) {
                    textureAtlasSprite = texture.atlasSpriteOverlay;
                }
            }
            int light = state.getPackedLightmapCoords(blockAccess, pos.offset(EnumFacing.EAST));
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xc], 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xf], 1, textureAtlasSprite);

            // 逆
            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xc], 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xf], 1, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - CustomWaterMeshUtil.MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);
        }

    }

    private void vertexNorth(BlockPos pos, BufferBuilder bufferBuilder, int color, int light, double x, double y, double z, TextureAtlasSprite textureAtlasSprite) {
        float u = textureAtlasSprite.getInterpolatedU(12 - x * 8);
        float v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
        int l_sky = light >> 16 & 65535;
        int l_block = light & 65535;
        bufferBuilder
                .pos(pos.getX() + x, pos.getY() + y, pos.getZ() + z)
                .color((color >> 16) & 0xff, (color >> 8) & 0xff, (color >> 0) & 0xff, 0xff)
                .tex(u, v)
                .lightmap(l_sky, l_block)
                .endVertex();
    }

    private void vertexSouth(BlockPos pos, BufferBuilder bufferBuilder, int color, int light, double x, double y, double z, TextureAtlasSprite textureAtlasSprite) {
        float u = textureAtlasSprite.getInterpolatedU(x * 8 + 4);
        float v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
        int l_sky = light >> 16 & 65535;
        int l_block = light & 65535;
        bufferBuilder
                .pos(pos.getX() + x, pos.getY() + y, pos.getZ() + z)
                .color((color >> 16) & 0xff, (color >> 8) & 0xff, (color >> 0) & 0xff, 0xff)
                .tex(u, v)
                .lightmap(l_sky, l_block)
                .endVertex();
    }

    private void vertexWest(BlockPos pos, BufferBuilder bufferBuilder, int color, int light, double x, double y, double z, TextureAtlasSprite textureAtlasSprite) {
        float u = textureAtlasSprite.getInterpolatedU(12 - z * 8);
        float v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
        int l_sky = light >> 16 & 65535;
        int l_block = light & 65535;
        bufferBuilder
                .pos(pos.getX() + x, pos.getY() + y, pos.getZ() + z)
                .color((color >> 16) & 0xff, (color >> 8) & 0xff, (color >> 0) & 0xff, 0xff)
                .tex(u, v)
                .lightmap(l_sky, l_block)
                .endVertex();
    }

    private void vertexEast(BlockPos pos, BufferBuilder bufferBuilder, int color, int light, double x, double y, double z, TextureAtlasSprite textureAtlasSprite) {
        float u = textureAtlasSprite.getInterpolatedU(z * 8 + 4);
        float v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
        int l_sky = light >> 16 & 65535;
        int l_block = light & 65535;
        bufferBuilder
                .pos(pos.getX() + x, pos.getY() + y, pos.getZ() + z)
                .color((color >> 16) & 0xff, (color >> 8) & 0xff, (color >> 0) & 0xff, 0xff)
                .tex(u, v)
                .lightmap(l_sky, l_block)
                .endVertex();
    }


    //

}
