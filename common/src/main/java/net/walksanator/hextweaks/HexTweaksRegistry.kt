package net.walksanator.hextweaks

import at.petrak.hexcasting.common.lib.HexRegistries
import dev.architectury.platform.Platform
import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.item.Item
import net.walksanator.hextweaks.casting.HexTweaksContinuationTypes
import net.walksanator.hextweaks.casting.HexTweaksIotaTypes
import net.walksanator.hextweaks.casting.MindflayRegistry
import net.walksanator.hextweaks.casting.PatternRegistry
import net.walksanator.hextweaks.casting.handler.GrandSpellHandler
import net.walksanator.hextweaks.computer.ComputerCraftCompat
import net.walksanator.hextweaks.items.VirtualPigment


object HexTweaksRegistry {
    var REGISTERED = false
    val regMap: HashMap<ResourceKey<Registry<*>>,DeferredRegister<*>> = HashMap()

    // vanilla registries
    val BLOCKS = reg(Registries.BLOCK)
    val ITEMS = reg(Registries.ITEM)

    val SPECIAL_HANDLERS = reg(HexRegistries.SPECIAL_HANDLER)
    val ACTIONS = reg(HexRegistries.ACTION)

    init {
        if (Platform.isModLoaded("computercraft")) {
            ComputerCraftCompat.init(regMap)
        }
    }

    val RGB_PIGMENT = ITEMS.register(ResourceLocation.fromNamespaceAndPath(net.walksanator.hextweaks.HexTweaks.MOD_ID, "rgb_pigment")) {
        net.walksanator.hextweaks.items.VirtualPigment(Item.Properties().stacksTo(1));
    }

    val GRAND_HANDLER = SPECIAL_HANDLERS.register(ResourceLocation.fromNamespaceAndPath(net.walksanator.hextweaks.HexTweaks.MOD_ID, "grand")) { GrandSpellHandler.Factory() }

    val SUS_DAMAGE_TYPE: ResourceKey<DamageType> = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(HexTweaks.MOD_ID, "sus")
    )



    fun init() {
        HexTweaksIotaTypes.init()
        MindflayRegistry.register()
        HexTweaksContinuationTypes.init()
    }

    fun register(key: ResourceKey<Registry<*>>?) {
        if (!REGISTERED) {
            PatternRegistry.register { are, rl -> ACTIONS.register(rl) { are } }
            REGISTERED = true
        }
        if (key == null) {
            if (Platform.isModLoaded("computercraft")) {
                ComputerCraftCompat.register()
            }
            BLOCKS.register()
            ITEMS.register()
            SPECIAL_HANDLERS.register()
            HexTweaksIotaTypes.IOTATYPE.register()
            ACTIONS.register()
//            MindflayRegistry.register()
            HexTweaksContinuationTypes.CONTINUATION_REGISTRY.register()
        } else {
            // NeoForge emits RegisterEvent once for every registry.  Most of
            // those registries intentionally have no HexTweaks entries, so an
            // absent DeferredRegister is the normal case rather than an error.
            // Logging every unrelated registry produced hundreds of warnings
            // during each client/server startup.
            regMap[key]?.register()
        }
    }

    fun <T> reg(key: ResourceKey<Registry<T>>): DeferredRegister<T> {
        val reg2 = DeferredRegister.create(net.walksanator.hextweaks.HexTweaks.MOD_ID,key)
        regMap[key as ResourceKey<Registry<*>>] = reg2
        return reg2
    }
}
