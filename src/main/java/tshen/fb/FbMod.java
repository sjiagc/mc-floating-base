package tshen.fb;

import net.minecraft.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tshen.fb.communication.FbChannelManager;
import tshen.fb.render.FbRenderer;
import tshen.fb.setup.FbEntities;
import tshen.fb.setup.FbItems;
import tshen.fb.setup.FbKeyBindings;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FbMod.MODID)
public class FbMod {
    // Directly reference a log4j logger.
    private static final Logger logger = LogManager.getLogger(FbMod.class);

    public static final String MODID = "floatingbase";

    public FbMod() {
        FbEntities.init();
        FbItems.init();
        FbChannelManager.getInstance();
        FbConfig.getInstance();

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // some preinit code
        logger.info("HELLO FROM PREINIT");
        logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        logger.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);

        RenderingRegistry.registerEntityRenderingHandler(FbEntities.BASE.get(), FbRenderer::new);
        FbKeyBindings.init();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        logger.info("Hello, it's FloatingBase!");
    }
}
