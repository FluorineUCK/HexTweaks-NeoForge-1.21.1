package net.walksanator.hextweaks.forge;

import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import dev.architectury.platform.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.walksanator.hextweaks.HexTweaks;
import net.walksanator.hextweaks.computer.ComputerCraftCompat;
import net.walksanator.hextweaks.computer.WandTurtleUpgrade;

final class HexTweaksForgeClient {
    private static boolean turtleModellerRegistered;

    private HexTweaksForgeClient() {
    }

    static void register(IEventBus bus) {
        bus.addListener(HexTweaksForgeClient::client);
        bus.addListener(HexTweaksForgeClient::registerTurtleModellers);
    }

    private static void client(FMLClientSetupEvent event) {
        HexTweaks.LOGGER.info("performing client setup on NEOFORGE");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerTurtleModellers(RegisterTurtleModellersEvent event) {
        if (Platform.isModLoaded("computercraft")) {
            event.register(
                    (UpgradeType<WandTurtleUpgrade>) (UpgradeType) ComputerCraftCompat.INSTANCE.getWandTurtleType().get(),
                    TurtleUpgradeModeller.flatItem()
            );
            turtleModellerRegistered = true;
        }
    }

    static boolean isTurtleModellerRegistered() {
        return turtleModellerRegistered;
    }
}
