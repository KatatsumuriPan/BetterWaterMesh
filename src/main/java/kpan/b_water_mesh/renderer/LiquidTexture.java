package kpan.b_water_mesh.renderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

public class LiquidTexture {
    public final TextureAtlasSprite atlasSpriteStill;
    public final TextureAtlasSprite atlasSpriteFlow;
    public final @Nullable TextureAtlasSprite atlasSpriteOverlay;

    public LiquidTexture(TextureAtlasSprite atlasSpriteStill, TextureAtlasSprite atlasSpriteFlow) {
        this(atlasSpriteStill, atlasSpriteFlow, null);
    }

    public LiquidTexture(TextureAtlasSprite atlasSpriteStill, TextureAtlasSprite atlasSpriteFlow, @Nullable TextureAtlasSprite atlasSpriteOverlay) {
        this.atlasSpriteStill = atlasSpriteStill;
        this.atlasSpriteFlow = atlasSpriteFlow;
        this.atlasSpriteOverlay = atlasSpriteOverlay;
    }
}
