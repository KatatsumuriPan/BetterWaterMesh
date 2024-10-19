package kpan.b_water_mesh.forge_fluid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import kpan.b_water_mesh.CustomWaterMeshUtil;
import kpan.b_water_mesh.renderer.LiquidTexture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.TRSRTransformer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.BlockFluidBase;
import org.apache.commons.lang3.tuple.Pair;

public class BakedLiquidModel implements IBakedModel {
    private static final int[] x = {0, 0, 1, 1};
    private static final int[] z = {0, 1, 1, 0};
    private static final float eps = 1e-3f;

    protected final Optional<TRSRTransformation> transformation;
    protected final ImmutableMap<TransformType, TRSRTransformation> transforms;
    protected final VertexFormat format;
    protected final int color;
    protected final TextureAtlasSprite still, flowing;
    protected final Optional<TextureAtlasSprite> overlay;
    protected final ImmutableMap<EnumFacing, ImmutableList<BakedQuad>> faceQuads;

    public BakedLiquidModel(Optional<TRSRTransformation> transformation, ImmutableMap<TransformType, TRSRTransformation> transforms, VertexFormat format, int color, TextureAtlasSprite still, TextureAtlasSprite flowing, Optional<TextureAtlasSprite> overlay, Optional<IExtendedBlockState> stateOption, boolean buildImmediately) {
        this.transformation = transformation;
        this.transforms = transforms;
        this.format = format;
        this.color = color;
        this.still = still;
        this.flowing = flowing;
        this.overlay = overlay;
        if (buildImmediately)
            faceQuads = buildQuads(stateOption);
        else
            faceQuads = ImmutableMap.of();
    }

    public BakedLiquidModel(Optional<TRSRTransformation> transformation, ImmutableMap<TransformType, TRSRTransformation> transforms, VertexFormat format, int color, TextureAtlasSprite still, TextureAtlasSprite flowing, Optional<TextureAtlasSprite> overlay, boolean[] sideOverlays, int flowRound, float[] heights16) {
        this.transformation = transformation;
        this.transforms = transforms;
        this.format = format;
        this.color = color;
        this.still = still;
        this.flowing = flowing;
        this.overlay = overlay;
        faceQuads = buildQuads(sideOverlays, flowRound, heights16);
    }

    protected ImmutableMap<EnumFacing, ImmutableList<BakedQuad>> buildQuads(Optional<IExtendedBlockState> stateOption) {
        if (stateOption.isPresent()) {
            return buildQuads(getOverlay(stateOption), getFlow(stateOption), getHeights16(stateOption.get()));
        } else {
            return buildQuads(stateOption.isPresent(), getCorners(stateOption), getFlow(stateOption), getOverlay(stateOption));
        }
    }

    protected ImmutableMap<EnumFacing, ImmutableList<BakedQuad>> buildQuads(boolean[] sideOverlays, int flowRound, float[] heights16) {
        EnumMap<EnumFacing, ImmutableList<BakedQuad>> faceQuads = new EnumMap<>(EnumFacing.class);
        for (EnumFacing side : EnumFacing.values()) {
            faceQuads.put(side, ImmutableList.of());
        }

        LiquidTexture texture = new LiquidTexture(still, flowing, overlay.orElse(null));
        boolean isFlowing = flowRound > -1000;
        float flow = isFlowing ? (float) Math.toRadians(flowRound) : 0f;

        // top
        {
            Builder<BakedQuad> builder = ImmutableList.builder();

            renderUp(heights16, builder, isFlowing, flow, texture, false);
            renderUp(heights16, builder, isFlowing, flow, texture, true);

            faceQuads.put(EnumFacing.UP, builder.build());
        }

        // bottom
        faceQuads.put(EnumFacing.DOWN, ImmutableList.of(
                buildQuad(EnumFacing.DOWN, still, false, false,
                        i -> z[i],
                        i -> 0,
                        i -> x[i],
                        i -> z[i] * 16,
                        i -> x[i] * 16
                ),
                buildQuad(EnumFacing.DOWN, still, true, true,
                        i -> z[i],
                        i -> 0,
                        i -> x[i],
                        i -> z[i] * 16,
                        i -> x[i] * 16
                )
        ));

        // sides
        renderSides(heights16, faceQuads, texture, sideOverlays);

        return ImmutableMap.copyOf(faceQuads);
    }

    protected ImmutableMap<EnumFacing, ImmutableList<BakedQuad>> buildQuads(boolean statePresent, int[] cornerRound, int flowRound, boolean[] sideOverlays) {
        EnumMap<EnumFacing, ImmutableList<BakedQuad>> faceQuads = new EnumMap<>(EnumFacing.class);
        for (EnumFacing side : EnumFacing.values()) {
            faceQuads.put(side, ImmutableList.of());
        }

        if (statePresent) {
            // y levels
            float[] y = new float[4];
            boolean fullVolume = true;
            for (int i = 0; i < 4; i++) {
                float value = cornerRound[i] / 864f;
                if (value < 1f)
                    fullVolume = false;
                y[i] = value;
            }

            // flow
            boolean isFlowing = flowRound > -1000;

            float flow = isFlowing ? (float) Math.toRadians(flowRound) : 0f;
            TextureAtlasSprite topSprite = isFlowing ? flowing : still;
            float scale = isFlowing ? 4f : 8f;

            float c = MathHelper.cos(flow) * scale;
            float s = MathHelper.sin(flow) * scale;

            // top
            EnumFacing top = EnumFacing.UP;

            // base uv offset for flow direction
            VertexParameter uv = i -> c * (x[i] * 2 - 1) + s * (z[i] * 2 - 1);

            VertexParameter topX = i -> x[i];
            VertexParameter topY = i -> y[i];
            VertexParameter topZ = i -> z[i];
            VertexParameter topU = i -> 8 + uv.get(i);
            VertexParameter topV = i -> 8 + uv.get((i + 1) % 4);

            {
                Builder<BakedQuad> builder = ImmutableList.builder();

                builder.add(buildQuad(top, topSprite, false, false, topX, topY, topZ, topU, topV));
                if (!fullVolume)
                    builder.add(buildQuad(top, topSprite, true, true, topX, topY, topZ, topU, topV));

                faceQuads.put(top, builder.build());
            }

            // bottom
            EnumFacing bottom = top.getOpposite();
            faceQuads.put(bottom, ImmutableList.of(
                    buildQuad(bottom, still, false, false,
                            i -> z[i],
                            i -> 0,
                            i -> x[i],
                            i -> z[i] * 16,
                            i -> x[i] * 16
                    )
            ));

            // sides
            for (int i = 0; i < 4; i++) {
                EnumFacing side = EnumFacing.byHorizontalIndex((5 - i) % 4); // [W, S, E, N]
                boolean useOverlay = overlay.isPresent() && sideOverlays[side.getHorizontalIndex()];
                int si = i; // local var for lambda capture

                VertexParameter sideX = j -> x[(si + x[j]) % 4];
                VertexParameter sideY = j -> z[j] == 0 ? 0 : y[(si + x[j]) % 4];
                VertexParameter sideZ = j -> z[(si + x[j]) % 4];
                VertexParameter sideU = j -> x[j] * 8;
                VertexParameter sideV = j -> (1 - sideY.get(j)) * 8;

                Builder<BakedQuad> builder = ImmutableList.builder();

                if (!useOverlay)
                    builder.add(buildQuad(side, flowing, false, true, sideX, sideY, sideZ, sideU, sideV));
                builder.add(buildQuad(side, useOverlay ? overlay.get() : flowing, true, false, sideX, sideY, sideZ, sideU, sideV));

                faceQuads.put(side, builder.build());
            }
        } else {
            // inventory
            faceQuads.put(EnumFacing.SOUTH, ImmutableList.of(
                    buildQuad(EnumFacing.UP, still, false, false,
                            i -> z[i],
                            i -> x[i],
                            i -> 0,
                            i -> z[i] * 16,
                            i -> x[i] * 16
                    )
            ));
        }

        return ImmutableMap.copyOf(faceQuads);
    }

    private BakedQuad buildQuad(EnumFacing side, TextureAtlasSprite texture, boolean flip, boolean offset, VertexParameter x, VertexParameter y, VertexParameter z, VertexParameter u, VertexParameter v) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);

        builder.setQuadOrientation(side);
        builder.setTexture(texture);
        builder.setQuadTint(0);

        boolean hasTransform = transformation.isPresent() && !transformation.get().isIdentity();
        IVertexConsumer consumer = hasTransform ? new TRSRTransformer(builder, transformation.get()) : builder;

        for (int i = 0; i < 4; i++) {
            int vertex = flip ? 3 - i : i;
            putVertex(
                    consumer, side, offset,
                    x.get(vertex), y.get(vertex), z.get(vertex),
                    texture.getInterpolatedU(u.get(vertex)),
                    texture.getInterpolatedV(v.get(vertex))
            );
        }

        return builder.build();
    }

    private void putVertex(IVertexConsumer consumer, EnumFacing side, boolean offset, float x, float y, float z, float u, float v) {
        for (int e = 0; e < format.getElementCount(); e++) {
            switch (format.getElement(e).getUsage()) {
                case POSITION:
                    float dx = offset ? side.getDirectionVec().getX() * eps : 0f;
                    float dy = offset ? side.getDirectionVec().getY() * eps : 0f;
                    float dz = offset ? side.getDirectionVec().getZ() * eps : 0f;
                    consumer.put(e, x - dx, y - dy, z - dz, 1f);
                    break;
                case COLOR:
                    float r = ((color >> 16) & 0xFF) / 255f;
                    float g = ((color >> 8) & 0xFF) / 255f;
                    float b = (color & 0xFF) / 255f;
                    float a = ((color >> 24) & 0xFF) / 255f;
                    consumer.put(e, r, g, b, a);
                    break;
                case NORMAL:
                    float offX = (float) side.getXOffset();
                    float offY = (float) side.getYOffset();
                    float offZ = (float) side.getZOffset();
                    consumer.put(e, offX, offY, offZ, 0f);
                    break;
                case UV:
                    if (format.getElement(e).getIndex() == 0) {
                        consumer.put(e, u, v, 0f, 1f);
                        break;
                    }
                    // else fallthrough to default
                default:
                    consumer.put(e);
                    break;
            }
        }
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return still;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (side == null)
            return ImmutableList.of();
        if (!faceQuads.isEmpty())
            return faceQuads.get(side);
        if (state instanceof IExtendedBlockState) {
            Optional<IExtendedBlockState> exState = Optional.of((IExtendedBlockState) state);
            return buildQuads(exState).get(side);
        }
        return ImmutableList.of();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        return PerspectiveMapWrapper.handlePerspective(this, transforms, type);
    }

    /**
     * Gets the quantized fluid levels for each corner.
     * <p>
     * Each value is packed into 10 bits of the model key, so max range is [0,1024).
     * The value is currently stored/interpreted as the closest multiple of 1/864.
     * The divisor is chosen here to allows likely flow values to be exactly representable
     * while also providing good use of the available value range.
     * (For fluids with default quanta, this evenly divides the per-block intervals of 1/9 by 96)
     */
    protected static int[] getCorners(Optional<IExtendedBlockState> stateOption) {
        int[] cornerRound = {0, 0, 0, 0};
        if (stateOption.isPresent()) {
            IExtendedBlockState state = stateOption.get();
            for (int i = 0; i < 4; i++) {
                Float level = state.getValue(BlockFluidBase.LEVEL_CORNERS[i]);
                cornerRound[i] = Math.round((level == null ? 8f / 9f : level) * 864);
            }
        }
        return cornerRound;
    }

    /**
     * Gets the quantized flow direction of the fluid.
     * <p>
     * This value comprises 11 bits of the model key, and is signed, so the max range is [-1024,1024).
     * The value is currently stored as the angle rounded to the nearest degree.
     * A value of -1000 is used to signify no flow.
     */
    protected static int getFlow(Optional<IExtendedBlockState> stateOption) {
        Float flow = -1000f;
        if (stateOption.isPresent()) {
            flow = stateOption.get().getValue(BlockFluidBase.FLOW_DIRECTION);
            if (flow == null)
                flow = -1000f;
        }
        int flowRound = (int) Math.round(Math.toDegrees(flow));
        flowRound = MathHelper.clamp(flowRound, -1000, 1000);
        return flowRound;
    }

    /**
     * Gets the overlay texture flag for each side.
     * <p>
     * This value determines if the fluid "overlay" texture should be used for that side,
     * instead of the normal "flowing" texture (if applicable for that fluid).
     * The sides are stored here by their regular horizontal index.
     */
    protected static boolean[] getOverlay(Optional<IExtendedBlockState> stateOption) {
        boolean[] overlaySides = new boolean[4];
        if (stateOption.isPresent()) {
            IExtendedBlockState state = stateOption.get();
            for (int i = 0; i < 4; i++) {
                Boolean overlay = state.getValue(BlockFluidBase.SIDE_OVERLAYS[i]);
                if (overlay != null)
                    overlaySides[i] = overlay;
            }
        }
        return overlaySides;
    }

    // maps vertex index to parameter value
    private interface VertexParameter {
        float get(int index);
    }

    //
    //
    //

    protected static float[] getHeights16(IExtendedBlockState state) {
        float[] res = new float[16];
        for (int i = 0; i < res.length; i++) {
            Float h = state.getValue(AdditionalForgeFluidProperties.MESH_HEIGHTS[i]);
            res[i] = h != null ? h : 0.5F;
        }
        return res;
    }

    private void renderUp(float[] heights16, Builder<BakedQuad> listBuilder, boolean isFlowing, float flowAngle, LiquidTexture texture, boolean inside) {

        // 0  4  8  c
        //
        // 1  5  9  d
        //     10
        // 2  6  a  e
        //
        // 3  7  b  f

        mquadUp(listBuilder, new int[]{0x0, 0x1, 0x5, 0x4}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x1, 0x2, 0x6, 0x5}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x3, 0x7, 0x6, 0x2}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x4, 0x5, 0x9, 0x8}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x5, 0x6, 0xa, 0x9}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x6, 0x7, 0xb, 0xa}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x9, 0xd, 0xc, 0x8}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0x9, 0xa, 0xe, 0xd}, heights16, isFlowing, flowAngle, texture, inside);
        mquadUp(listBuilder, new int[]{0xa, 0xb, 0xf, 0xe}, heights16, isFlowing, flowAngle, texture, inside);
    }

    private void mquadUp(Builder<BakedQuad> listBuilder, int[] indices, float[] heights16, boolean isFlowing, float flowAngle, LiquidTexture texture, boolean inside) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setQuadOrientation(EnumFacing.UP);
        builder.setQuadTint(0);
        for (int i = 0; i < indices.length; i++) {
            int index = indices[inside ? (indices.length - 1 - i) : i];

            boolean hasTransform = transformation.isPresent() && !transformation.get().isIdentity();
            IVertexConsumer consumer = hasTransform ? new TRSRTransformer(builder, transformation.get()) : builder;

            double x, z;
            double y = heights16[index];
            if (y <= CustomWaterMeshUtil.OFFSET_TO_FIX_Z_FIGHTING)
                y = CustomWaterMeshUtil.OFFSET_TO_FIX_Z_FIGHTING;
            if (index == 0x10) {
                x = 0.5;
                z = 0.5;
            } else {
                x = switch (index >> 2) {
                    case 0 -> 0;
                    case 1 -> CustomWaterMeshUtil.MODEL_BORDER;
                    case 2 -> 1 - CustomWaterMeshUtil.MODEL_BORDER;
                    case 3 -> 1;
                    default -> throw new IllegalStateException("Unexpected value: " + (index >> 2));
                };
                z = switch (index % 4) {
                    case 0 -> 0;
                    case 1 -> CustomWaterMeshUtil.MODEL_BORDER;
                    case 2 -> 1 - CustomWaterMeshUtil.MODEL_BORDER;
                    case 3 -> 1;
                    default -> throw new IllegalStateException("Unexpected value: " + (index % 4));
                };
            }

            float u;
            float v;
            TextureAtlasSprite textureAtlasSprite;
            if (!isFlowing) {
                textureAtlasSprite = texture.atlasSpriteStill;
                u = textureAtlasSprite.getInterpolatedU(x * 16);
                v = textureAtlasSprite.getInterpolatedV(z * 16);
            } else {
                textureAtlasSprite = texture.atlasSpriteFlow;
                float s = MathHelper.sin(flowAngle);
                float c = MathHelper.cos(flowAngle);
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

            builder.setTexture(textureAtlasSprite);

            putVertex(consumer, EnumFacing.UP.getOpposite(), false, (float) x, (float) y, (float) z, u, v);

        }
        listBuilder.add(builder.build());

        // TODO:裏
        // for (int i = indices.length - 1; i >= 0; i--) {
        //     vertexUp(pos, bufferBuilder, color, light, indices[i], heights16, flowAngle, texture);
        // }
    }


    private void renderSides(float[] heights16, EnumMap<EnumFacing, ImmutableList<BakedQuad>> faceQuads, LiquidTexture texture, boolean[] sideOverlays) {

        // 0  4  8  c
        //
        // 1  5  9  d
        //     10
        // 2  6  a  e
        //
        // 3  7  b  f

        {
            Builder<BakedQuad> builder = ImmutableList.builder();
            quadsNorth(heights16, builder, texture, sideOverlays, false);
            quadsNorth(heights16, builder, texture, sideOverlays, true);
            faceQuads.put(EnumFacing.NORTH, builder.build());
        }
        {
            Builder<BakedQuad> builder = ImmutableList.builder();
            quadsSouth(heights16, builder, texture, sideOverlays, false);
            quadsSouth(heights16, builder, texture, sideOverlays, true);
            faceQuads.put(EnumFacing.SOUTH, builder.build());
        }

        {
            Builder<BakedQuad> builder = ImmutableList.builder();
            quadsWest(heights16, builder, texture, sideOverlays, false);
            quadsWest(heights16, builder, texture, sideOverlays, true);
            faceQuads.put(EnumFacing.WEST, builder.build());
        }

        {
            Builder<BakedQuad> builder = ImmutableList.builder();
            quadsEast(heights16, builder, texture, sideOverlays, false);
            quadsEast(heights16, builder, texture, sideOverlays, true);
            faceQuads.put(EnumFacing.EAST, builder.build());
        }

    }

    private void quadsNorth(float[] heights16, Builder<BakedQuad> builder, LiquidTexture texture, boolean[] sideOverlays, boolean inside) {
        TextureAtlasSprite textureAtlasSprite = inside && sideOverlays[EnumFacing.NORTH.getHorizontalIndex()] && texture.atlasSpriteOverlay != null ? texture.atlasSpriteOverlay : texture.atlasSpriteFlow;
        quadNS(builder, 1, heights16[0xc], 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x8], textureAtlasSprite, inside, EnumFacing.NORTH);
        quadNS(builder, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x8], CustomWaterMeshUtil.MODEL_BORDER, heights16[0x4], textureAtlasSprite, inside, EnumFacing.NORTH);
        quadNS(builder, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x4], 0, heights16[0x0], textureAtlasSprite, inside, EnumFacing.NORTH);
    }

    private void quadsSouth(float[] heights16, Builder<BakedQuad> builder, LiquidTexture texture, boolean[] sideOverlays, boolean inside) {
        TextureAtlasSprite textureAtlasSprite = inside && sideOverlays[EnumFacing.SOUTH.getHorizontalIndex()] && texture.atlasSpriteOverlay != null ? texture.atlasSpriteOverlay : texture.atlasSpriteFlow;
        quadNS(builder, 0, heights16[0x3], CustomWaterMeshUtil.MODEL_BORDER, heights16[0x7], textureAtlasSprite, inside, EnumFacing.SOUTH);
        quadNS(builder, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x7], 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xb], textureAtlasSprite, inside, EnumFacing.SOUTH);
        quadNS(builder, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xb], 1, heights16[0xf], textureAtlasSprite, inside, EnumFacing.SOUTH);
    }

    private void quadsWest(float[] heights16, Builder<BakedQuad> builder, LiquidTexture texture, boolean[] sideOverlays, boolean inside) {
        TextureAtlasSprite textureAtlasSprite = inside && sideOverlays[EnumFacing.WEST.getHorizontalIndex()] && texture.atlasSpriteOverlay != null ? texture.atlasSpriteOverlay : texture.atlasSpriteFlow;
        quadWE(builder, 0, heights16[0x0], CustomWaterMeshUtil.MODEL_BORDER, heights16[0x1], textureAtlasSprite, inside, EnumFacing.WEST);
        quadWE(builder, CustomWaterMeshUtil.MODEL_BORDER, heights16[0x1], 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x2], textureAtlasSprite, inside, EnumFacing.WEST);
        quadWE(builder, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0x2], 1, heights16[0x3], textureAtlasSprite, inside, EnumFacing.WEST);
    }

    private void quadsEast(float[] heights16, Builder<BakedQuad> builder, LiquidTexture texture, boolean[] sideOverlays, boolean inside) {
        TextureAtlasSprite textureAtlasSprite = inside && sideOverlays[EnumFacing.WEST.getHorizontalIndex()] && texture.atlasSpriteOverlay != null ? texture.atlasSpriteOverlay : texture.atlasSpriteFlow;
        quadWE(builder, 1, heights16[0xf], 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xe], textureAtlasSprite, inside, EnumFacing.EAST);
        quadWE(builder, 1 - CustomWaterMeshUtil.MODEL_BORDER, heights16[0xe], CustomWaterMeshUtil.MODEL_BORDER, heights16[0xd], textureAtlasSprite, inside, EnumFacing.EAST);
        quadWE(builder, CustomWaterMeshUtil.MODEL_BORDER, heights16[0xd], 0, heights16[0xc], textureAtlasSprite, inside, EnumFacing.EAST);
    }

    private void quadNS(Builder<BakedQuad> listBuilder, float x1, float y1, float x2, float y2, TextureAtlasSprite textureAtlasSprite, boolean inside, EnumFacing side) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setQuadOrientation(side);
        builder.setTexture(textureAtlasSprite);
        builder.setQuadTint(0);
        float z;
        if (side == EnumFacing.NORTH)
            z = inside ? CustomWaterMeshUtil.OFFSET_TO_FIX_Z_FIGHTING : 0;
        else
            z = inside ? (1 - CustomWaterMeshUtil.OFFSET_TO_FIX_Z_FIGHTING) : 1;
        if (inside) {
            vertexXZ(builder, x2, y2, z, side, textureAtlasSprite);
            vertexXZ(builder, x2, 0, z, side, textureAtlasSprite);
            vertexXZ(builder, x1, 0, z, side, textureAtlasSprite);
            vertexXZ(builder, x1, y1, z, side, textureAtlasSprite);
        } else {
            vertexXZ(builder, x1, y1, z, side, textureAtlasSprite);
            vertexXZ(builder, x1, 0, z, side, textureAtlasSprite);
            vertexXZ(builder, x2, 0, z, side, textureAtlasSprite);
            vertexXZ(builder, x2, y2, z, side, textureAtlasSprite);
        }
        listBuilder.add(builder.build());

    }
    private void quadWE(Builder<BakedQuad> listBuilder, float z1, float y1, float z2, float y2, TextureAtlasSprite textureAtlasSprite, boolean inside, EnumFacing side) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setQuadOrientation(side);
        builder.setTexture(textureAtlasSprite);
        builder.setQuadTint(0);
        float x;
        if (side == EnumFacing.WEST)
            x = inside ? CustomWaterMeshUtil.OFFSET_TO_FIX_Z_FIGHTING : 0;
        else
            x = inside ? (1 - CustomWaterMeshUtil.OFFSET_TO_FIX_Z_FIGHTING) : 1;
        if (inside) {
            vertexXZ(builder, x, y2, z2, side, textureAtlasSprite);
            vertexXZ(builder, x, 0, z2, side, textureAtlasSprite);
            vertexXZ(builder, x, 0, z1, side, textureAtlasSprite);
            vertexXZ(builder, x, y1, z1, side, textureAtlasSprite);
        } else {
            vertexXZ(builder, x, y1, z1, side, textureAtlasSprite);
            vertexXZ(builder, x, 0, z1, side, textureAtlasSprite);
            vertexXZ(builder, x, 0, z2, side, textureAtlasSprite);
            vertexXZ(builder, x, y2, z2, side, textureAtlasSprite);
        }
        listBuilder.add(builder.build());

    }

    private void vertexXZ(UnpackedBakedQuad.Builder builder, float x, float y, float z, EnumFacing side, TextureAtlasSprite textureAtlasSprite) {
        float u;
        float v;
        switch (side) {
            case NORTH -> {
                u = textureAtlasSprite.getInterpolatedU(12 - x * 8);
                v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
            }
            case SOUTH -> {
                u = textureAtlasSprite.getInterpolatedU(x * 8 + 4);
                v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
            }
            case WEST -> {
                u = textureAtlasSprite.getInterpolatedU(12 - z * 8);
                v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
            }
            case EAST -> {
                u = textureAtlasSprite.getInterpolatedU(z * 8 + 4);
                v = textureAtlasSprite.getInterpolatedV(12 - y * 8);
            }
            default -> throw new IllegalStateException("Unexpected value: " + side);
        }
        boolean hasTransform = transformation.isPresent() && !transformation.get().isIdentity();
        IVertexConsumer consumer = hasTransform ? new TRSRTransformer(builder, transformation.get()) : builder;
        putVertex(consumer, side.getOpposite(), false, x, y, z, u, v);
    }
}
