package kpan.b_water_mesh.renderer;

import java.util.Arrays;
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

    private static final float MODEL_BORDER = 0.2F;
    private static final float INNER_MAX_HEIGHT = 0.9F;

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
            boolean isOnGround = canLiquidTouchWith(blockAccess, blockAccess.getBlockState(blockPosIn.down()), blockPosIn.down(), EnumFacing.UP);
            float[] sideLiquidHeights = get4SideLiquidHeights(blockAccess, blockPosIn, material, isOnGround);
            float liquidHeight = getLiquidHeightPercent(blockStateIn);
            heights16 = get16Heights(sideLiquidHeights, get4CornerMeshHeights(blockAccess, blockPosIn, material, sideLiquidHeights, liquidHeight), liquidHeight);
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
                case 1 -> MODEL_BORDER;
                case 2 -> 1 - MODEL_BORDER;
                case 3 -> 1;
                default -> throw new IllegalStateException("Unexpected value: " + (idx >> 2));
            };
            z = switch (idx % 4) {
                case 0 -> 0;
                case 1 -> MODEL_BORDER;
                case 2 -> 1 - MODEL_BORDER;
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
            vertexNorth(pos, bufferBuilder, color, light, MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 0, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 0, heights16[0x0], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, color, light, 1, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, color, light, 1, heights16[0xc], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            // 逆
            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;
            vertexNorth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 0, heights16[0x0], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 0, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, heights16[0x4], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexNorth(pos, bufferBuilder, colorDark, light, 1, heights16[0xc], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, heights16[0x8], SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexNorth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, 0, SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
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
            vertexSouth(pos, bufferBuilder, color, light, MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 0, heights16[0x3], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 0, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, color, light, 1, heights16[0xf], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1 - MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, color, light, 1, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            // 逆

            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;
            vertexSouth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 0, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 0, heights16[0x3], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, MODEL_BORDER, heights16[0x7], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);

            vertexSouth(pos, bufferBuilder, colorDark, light, 1, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, 0, 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
            vertexSouth(pos, bufferBuilder, colorDark, light, 1 - MODEL_BORDER, heights16[0xb], 1 - SHIFT_FOR_FIX_Z_FIGHTING, textureAtlasSprite);
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
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x0], 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x3], 1, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, color, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);

            // 逆
            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;

            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x0], 0, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x1], MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - MODEL_BORDER, textureAtlasSprite);

            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);
            vertexWest(pos, bufferBuilder, colorDark, light, SHIFT_FOR_FIX_Z_FIGHTING, heights16[0x2], 1 - MODEL_BORDER, textureAtlasSprite);
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
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xc], 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, color, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xf], 1, textureAtlasSprite);

            // 逆
            int darkR = (color >> 16) & 0xff;
            int darkG = (color >> 8) & 0xff;
            int darkB = (color >> 0) & 0xff;
            darkR *= INNER_FACE_LIGHT;
            darkG *= INNER_FACE_LIGHT;
            darkB *= INNER_FACE_LIGHT;
            int colorDark = darkR << 16 | darkG << 8 | darkB << 0;
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xc], 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 0, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xd], MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);

            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xf], 1, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, heights16[0xe], 1 - MODEL_BORDER, textureAtlasSprite);
            vertexEast(pos, bufferBuilder, colorDark, light, 1 - SHIFT_FOR_FIX_Z_FIGHTING, 0, 1 - MODEL_BORDER, textureAtlasSprite);
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

    private static float getLiquidHeightPercent(IBlockState state) {
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level >= 8)
            return 1;
        else
            return (1 - level / 7F) * INNER_MAX_HEIGHT;
    }

    private static boolean canLiquidTouchWith(IBlockAccess blockAccess, IBlockState state, BlockPos pos, EnumFacing side) {
        if (state.isFullBlock())
            return true;
        if (state.isSideSolid(blockAccess, pos, side))
            return true;
        if (state.getBlockFaceShape(blockAccess, pos, side.getOpposite()) == BlockFaceShape.SOLID)
            return true;

        return false;
    }


    //

    private static final float IS_WALL = -1F;
    private static final float IS_AIR = -2F;
    private static final float HAS_LIQUID_ABOVE = -3F;
    private static float[] get4SideLiquidHeights(IBlockAccess blockAccess, BlockPos pos, Material materialCondition, boolean isOnGround) {
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
    private static float[] get4CornerMeshHeights(IBlockAccess blockAccess, BlockPos pos, Material materialCondition, float[] sideLiquidHeights, float centerHeight) {
        float[] res = new float[4];
        for (int i = 0; i < EnumFacing.HORIZONTALS.length; i++) {
            EnumFacing side = EnumFacing.HORIZONTALS[i];
            int nextI = (i + 1) % 4;
            EnumFacing nextSide = EnumFacing.HORIZONTALS[nextI];
            if (sideLiquidHeights[i] == HAS_LIQUID_ABOVE || sideLiquidHeights[nextI] == HAS_LIQUID_ABOVE) {
                res[i] = 1;
            } else if (sideLiquidHeights[i] < 0 && sideLiquidHeights[nextI] < 0) {
                float den = 0;
                float sum = 0;
                float b = 1f;
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
                    float den = 0;
                    float sum = 0;
                    float liquidWeight = 13F;
                    boolean wallExists = false;
                    if (sideLiquidHeights[i] >= 0) {
                        sum += sideLiquidHeights[i] * liquidWeight;
                        den += liquidWeight;
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
                    if (!wallExists)
                        sum *= 0.9F;
                    res[i] = sum / den;
                }
            }
        }
        return res;
    }
    private static float[] get16Heights(float[] sideLiquidHeights, float[] cornerMeshHeights, float centerHeight) {
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
    private static void get16Height_edge(float[] heights, int index1, int index2, int cornerIndex1, int cornerIndex2, EnumFacing side, float[] sideLiquidHeights, float centerHeight) {
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
            // 中心の方が高いときは低くなりすぎないように
            heights[index1] = (edgeMiddleHeight * (MODEL_BORDER + 0.35F) + heights[cornerIndex1] * (0.65F - MODEL_BORDER));
            heights[index2] = (edgeMiddleHeight * (MODEL_BORDER + 0.35F) + heights[cornerIndex2] * (0.65F - MODEL_BORDER));
        } else {
            float edgeMiddleHeight = (heights[cornerIndex1] + heights[cornerIndex2] + centerHeight * 14) / 16F;
            // 角の方が高いときはがっつり低めに
            heights[index1] = (edgeMiddleHeight * MODEL_BORDER + heights[cornerIndex1] * (0.5F - MODEL_BORDER)) * 2 * 0.7F;
            heights[index2] = (edgeMiddleHeight * MODEL_BORDER + heights[cornerIndex2] * (0.5F - MODEL_BORDER)) * 2 * 0.7F;
        }
    }
    private static float get16Height_inner(float[] heights, int cornerIndex, int sideIndex1, int sideIndex2, EnumFacing side1, EnumFacing side2, float[] sideLiquidHeights, float centerHeight) {
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
}
