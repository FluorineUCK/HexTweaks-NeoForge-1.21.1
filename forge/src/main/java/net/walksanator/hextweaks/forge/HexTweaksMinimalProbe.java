package net.walksanator.hextweaks.forge;

import at.petrak.hexcasting.api.item.PigmentItem;
import at.petrak.hexcasting.common.lib.HexRegistries;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.walksanator.hextweaks.HexTweaks;

/**
 * Dedicated-server smoke probe whose constant pool contains no optional-addon
 * classes. This lets the development runtime prove that ComputerCraft,
 * MoreIotas, Hexal, and their integrations are genuinely optional.
 */
public final class HexTweaksMinimalProbe {
    private static final List<String> ADDON_MODS = List.of("moreiotas", "hexal");

    private HexTweaksMinimalProbe() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HexTweaksMinimalProbe::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        int exitCode = 0;
        try {
            boolean expectAddons = Boolean.getBoolean("hextweaks.probe.expectAddons");
            require(!ModList.get().isLoaded("computercraft"), "unexpected optional mod loaded: computercraft");
            for (String modId : ADDON_MODS) {
                require(
                        ModList.get().isLoaded(modId) == expectAddons,
                        (expectAddons ? "missing expected addon: " : "unexpected optional mod loaded: ") + modId);
            }

            var itemId = id("rgb_pigment");
            var item = BuiltInRegistries.ITEM.get(itemId);
            require(BuiltInRegistries.ITEM.getKey(item).equals(itemId), "missing item " + itemId);
            require(item instanceof PigmentItem, "rgb_pigment is not a PigmentItem");

            var actions = event.getServer().registryAccess().registryOrThrow(HexRegistries.ACTION);
            var actionIds = new java.util.ArrayList<>(List.of(
                    id("infusion"),
                    id("page/right"),
                    id("page/left"),
                    id("while"),
                    id("wave"),
                    id("normal")));
            if (expectAddons) {
                actionIds.add(id("you_like_drinking_potions"));
            }
            for (var actionId : actionIds) {
                require(actions.containsKey(actionId), "missing action " + actionId);
            }

            var iotas = event.getServer().registryAccess().registryOrThrow(HexRegistries.IOTA_TYPE);
            for (String path : List.of("byte", "bytearray", "ritual")) {
                require(iotas.containsKey(id(path)), "missing iota type " + id(path));
            }

            var handlers =
                    event.getServer().registryAccess().registryOrThrow(HexRegistries.SPECIAL_HANDLER);
            require(handlers.containsKey(id("grand")), "missing special handler hextweaks:grand");

            var continuations =
                    event.getServer().registryAccess().registryOrThrow(HexRegistries.CONTINUATION_TYPE);
            var whileId = ResourceLocation.fromNamespaceAndPath("hexcasting", "while");
            require(continuations.containsKey(whileId), "missing continuation type " + whileId);

            HexTweaks.LOGGER.info(
                    "[HEXTWEAKS-MINIMAL-PROBE] aggregate=PASS optional_mods={} "
                            + "dedicated_server=PASS actions={} iotas=3 item={} continuation={}",
                    expectAddons ? "MOREIOTAS_HEXAL_NO_CC" : "ABSENT",
                    actionIds.size(),
                    itemId,
                    whileId);
        } catch (Throwable throwable) {
            exitCode = 1;
            HexTweaks.LOGGER.error("[HEXTWEAKS-MINIMAL-PROBE] aggregate=FAIL", throwable);
        } finally {
            scheduleHardExit(exitCode);
            event.getServer().halt(false);
        }
    }

    private static void scheduleHardExit(int exitCode) {
        Thread hardStop = new Thread(() -> {
            try {
                Thread.sleep(15_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(exitCode);
        }, "hextweaks-minimal-probe-hard-stop");
        hardStop.setDaemon(true);
        hardStop.start();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(HexTweaks.MOD_ID, path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
