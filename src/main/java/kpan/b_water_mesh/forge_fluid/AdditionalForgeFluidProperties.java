package kpan.b_water_mesh.forge_fluid;

import net.minecraftforge.common.property.PropertyFloat;

public class AdditionalForgeFluidProperties {

    public static final PropertyFloat[] MESH_HEIGHTS = new PropertyFloat[16];

    static {
        for (int i = 0; i < MESH_HEIGHTS.length; i++) {
            MESH_HEIGHTS[i] = new PropertyFloat("mesh_heights_" + i, 0f, 1f);
        }
    }
}
