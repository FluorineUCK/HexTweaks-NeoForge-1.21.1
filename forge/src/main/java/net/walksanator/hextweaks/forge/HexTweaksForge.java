package net.walksanator.hextweaks.forge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.walksanator.hextweaks.HexTweaks;
import net.walksanator.hextweaks.HexTweaksRegistry;

@Mod(HexTweaks.MOD_ID)
public class HexTweaksForge {
    public HexTweaksForge(IEventBus bus, ModContainer container) {
        bus.addListener(HexTweaksForge::register);
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("computercraft")) {
            HexTweaksForgeClient.register(bus);
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerDevelopmentProbe(
                    "hextweaks.probe.validateClient",
                    "net.walksanator.hextweaks.forge.HexTweaksClientProbe"
            );
        }

        HexTweaks.init();
        HexTweaksRegistry.INSTANCE.init();
        registerDevelopmentProbe(
                "hextweaks.probe.validate",
                "net.walksanator.hextweaks.forge.HexTweaksProbe"
        );
        registerDevelopmentProbe(
                "hextweaks.probe.validateMinimal",
                "net.walksanator.hextweaks.forge.HexTweaksMinimalProbe"
        );
    }

    public static void register(RegisterEvent event) {
        ResourceKey<Registry<?>> key = (ResourceKey<Registry<?>>) event.getRegistryKey();
        HexTweaksRegistry.INSTANCE.register(key);
//        HexTweaks.LOGGER.info("performing registration on NEOFORGE");
//        HexTweaksRegistry.INSTANCE.register();
    }

    /**
     * Keeps heavyweight regression probes available to Loom runs without
     * linking or shipping them in the production mod JAR.
     */
    private static void registerDevelopmentProbe(String property, String className) {
        if (FMLEnvironment.production || !Boolean.getBoolean(property)) {
            return;
        }
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register development probe " + className, exception);
        }
    }
}
