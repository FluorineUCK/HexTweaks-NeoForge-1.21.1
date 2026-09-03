package net.walksanator.hextweaks.computer

import dan200.computercraft.api.pocket.IPocketUpgrade
import dan200.computercraft.api.turtle.ITurtleUpgrade
import dan200.computercraft.api.upgrades.UpgradeType
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.walksanator.hextweaks.HexTweaks

object ComputerCraftCompat {
    private val pocketUpgradeTypes: DeferredRegister<UpgradeType<out IPocketUpgrade>> =
        DeferredRegister.create(HexTweaks.MOD_ID, IPocketUpgrade.typeRegistry())
    private val turtleUpgradeTypes: DeferredRegister<UpgradeType<out ITurtleUpgrade>> =
        DeferredRegister.create(HexTweaks.MOD_ID, ITurtleUpgrade.typeRegistry())

    lateinit var wandTurtleType: RegistrySupplier<UpgradeType<out ITurtleUpgrade>>
        private set

    fun init(regMap: MutableMap<ResourceKey<Registry<*>>, DeferredRegister<*>>) {
        regMap[IPocketUpgrade.typeRegistry() as ResourceKey<Registry<*>>] = pocketUpgradeTypes
        regMap[ITurtleUpgrade.typeRegistry() as ResourceKey<Registry<*>>] = turtleUpgradeTypes

        pocketUpgradeTypes.register(ResourceLocation.fromNamespaceAndPath(HexTweaks.MOD_ID, "wand")) {
            WandPocketUpgrade.TYPE
        }
        wandTurtleType = turtleUpgradeTypes.register(ResourceLocation.fromNamespaceAndPath(HexTweaks.MOD_ID, "wand")) {
            WandTurtleUpgrade.TYPE
        }
    }

    fun register() {
        pocketUpgradeTypes.register()
        turtleUpgradeTypes.register()
    }
}
