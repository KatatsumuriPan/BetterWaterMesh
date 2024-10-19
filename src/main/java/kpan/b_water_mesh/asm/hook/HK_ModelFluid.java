package kpan.b_water_mesh.asm.hook;

import java.util.Optional;
import java.util.function.Function;
import kpan.b_water_mesh.forge_fluid.CachedBakedLiquidModel;
import kpan.b_water_mesh.util.ReflectionUtil;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fluids.Fluid;

public class HK_ModelFluid {

    public static IBakedModel bake(ModelFluid self, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        Fluid fluid = ReflectionUtil.getPrivateField(self, "fluid");
        return new CachedBakedLiquidModel(
                state.apply(Optional.empty()),
                PerspectiveMapWrapper.getTransforms(state),
                format,
                fluid.getColor(),
                bakedTextureGetter.apply(fluid.getStill()),
                bakedTextureGetter.apply(fluid.getFlowing()),
                Optional.ofNullable(fluid.getOverlay()).map(bakedTextureGetter),
                Optional.empty()
        );
    }
}
