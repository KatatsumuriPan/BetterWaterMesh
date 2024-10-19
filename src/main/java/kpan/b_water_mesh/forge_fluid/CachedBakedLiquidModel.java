package kpan.b_water_mesh.forge_fluid;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.jetbrains.annotations.Nullable;

public class CachedBakedLiquidModel extends BakedLiquidModel {

    private final LoadingCache<CacheKey, BakedLiquidModel> modelCache = CacheBuilder.newBuilder().maximumSize(200).build(new CacheLoader<>() {
        @Override
        public BakedLiquidModel load(CacheKey key) {
            return new BakedLiquidModel(transformation, transforms, format, color, still, flowing, overlay, key.getSideOverlays(), key.getFlowRound(), key.getHeights16());
        }
    });

    public CachedBakedLiquidModel(Optional<TRSRTransformation> transformation, ImmutableMap<TransformType, TRSRTransformation> transforms, VertexFormat format, int color, TextureAtlasSprite still, TextureAtlasSprite flowing, Optional<TextureAtlasSprite> overlay, Optional<IExtendedBlockState> stateOption) {
        super(transformation, transforms, format, color, still, flowing, overlay, stateOption, true);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (side != null && state instanceof IExtendedBlockState) {
            Optional<IExtendedBlockState> exState = Optional.of((IExtendedBlockState) state);

            CacheKey key = CacheKey.of(getOverlay(exState), getFlow(exState), getHeights16(((IExtendedBlockState) state)));

            return modelCache.getUnchecked(key).getQuads(state, side, rand);
        }
        return super.getQuads(state, side, rand);
    }


    private static final class CacheKey {
        public static CacheKey of(boolean[] sideOverlays, int flowRound, float[] heights16) {
            byte so = 0;
            for (int i = 0; i < sideOverlays.length; i++) {
                so |= (byte) (sideOverlays[i] ? (1 << i) : 0);
            }
            long heightsUpper = 0;
            long heightsLower = 0;
            for (int i = 0; i < 8; i++) {
                heightsLower |= (long) (heights16[i] * 255) << (i * 8);
            }
            for (int i = 0; i < 8; i++) {
                heightsUpper |= (long) (heights16[i + 8] * 255) << (i * 8);
            }
            return new CacheKey(so, (short) flowRound, heightsUpper, heightsLower);
        }
        private byte sideOverlays;
        private short flowRound;
        private long heightsUpper;
        private long heightsLower;

        private CacheKey(byte sideOverlays, short flowRound, long heightsUpper, long heightsLower) {
            this.sideOverlays = sideOverlays;
            this.flowRound = flowRound;
            this.heightsUpper = heightsUpper;
            this.heightsLower = heightsLower;
        }

        public boolean[] getSideOverlays() {
            boolean[] res = new boolean[4];
            for (int i = 0; i < 4; i++) {
                res[i] = (sideOverlays & (1 << i)) != 0;
            }
            return res;
        }

        public int getFlowRound() {
            return flowRound;
        }

        public float[] getHeights16() {
            float[] res = new float[16];
            for (int i = 0; i < 8; i++) {
                res[i] = ((heightsLower >>> (i * 8)) & 0xFF) / 255F;
            }
            for (int i = 0; i < 8; i++) {
                res[i + 8] = ((heightsUpper >>> (i * 8)) & 0xFF) / 255F;
            }
            return res;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CacheKey cacheKey))
                return false;
            return sideOverlays == cacheKey.sideOverlays && flowRound == cacheKey.flowRound && heightsUpper == cacheKey.heightsUpper && heightsLower == cacheKey.heightsLower;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sideOverlays, flowRound, heightsUpper, heightsLower);
        }
    }
}
