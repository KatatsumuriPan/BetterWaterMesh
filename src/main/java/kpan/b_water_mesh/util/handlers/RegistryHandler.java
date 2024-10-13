package kpan.b_water_mesh.util.handlers;

import kpan.b_water_mesh.ModMain;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@EventBusSubscriber
public class RegistryHandler {

    public static void preInitRegistries(@SuppressWarnings("unused") FMLPreInitializationEvent event) {
        ModMain.proxy.registerOnlyClient();
    }

    public static void initRegistries() {
    }

    public static void postInitRegistries() {
    }

    public static void serverRegistries(@SuppressWarnings("unused") FMLServerStartingEvent event) {
    }

}
