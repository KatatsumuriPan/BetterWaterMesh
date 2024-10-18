package kpan.b_water_mesh;

import kpan.b_water_mesh.renderer.CustomLiquidRenderer;
import kpan.b_water_mesh.util.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

// 文字コードをMS932にすると日本語ベタ打ちしたものがゲーム時に文字化けしないが
// 色々と問題があるので
//.langをちゃんと使うのを推奨

@Mod(modid = ModTagsGenerated.MODID, version = ModTagsGenerated.VERSION, name = ModTagsGenerated.MODNAME, acceptedMinecraftVersions = "[1.12.2]"
        , dependencies = ""
        , acceptableRemoteVersions = ModTagsGenerated.VERSION_MAJOR + "." + ModTagsGenerated.VERSION_MINOR
        , clientSideOnly = true
//
//, serverSideOnly = true //サーバーのみにする場合に必要(acceptableRemoteVersionsを*に変えないとダメ)、デバッグ時はオフにする
)
public class ModMain {

    public static final Logger LOGGER = LogManager.getLogger(ModTagsGenerated.MODNAME);

    @Nullable
    public static MinecraftServer server = null;

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
    }

    @EventHandler
    public static void init(FMLInitializationEvent event) {
    }

    @EventHandler
    public static void postInit(FMLPostInitializationEvent event) {
        ReflectionUtil.setObfPrivateValue(Minecraft.getMinecraft().getBlockRendererDispatcher(), "fluidRenderer", "field_175025_e", new CustomLiquidRenderer(Minecraft.getMinecraft().getBlockColors()));
    }

    @EventHandler
    public static void serverInit(FMLServerStartingEvent event) {
    }

    @EventHandler
    public static void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        server = event.getServer();
    }

    @EventHandler
    public static void onServerStopped(FMLServerStoppedEvent event) {
        server = null;
    }
}
