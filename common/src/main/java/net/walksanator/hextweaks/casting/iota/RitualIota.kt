package net.walksanator.hextweaks.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.walksanator.hextweaks.casting.HexTweaksIotaTypes

class RitualIota(val ritualid: ResourceLocation) : Iota({ HexTweaksIotaTypes.RITUAL }) {

    class RitualIotaType : IotaType<RitualIota>() {
        override fun codec(): MapCodec<RitualIota> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, RitualIota> = STREAM_CODEC

        override fun color(): Int = 0xFF0000 // literally ChatGPT suggested color

        companion object {
            val CODEC: MapCodec<RitualIota> = ResourceLocation.CODEC
                .xmap(::RitualIota, RitualIota::ritualid)
                .fieldOf("id")

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RitualIota> = ResourceLocation.STREAM_CODEC
                .map(::RitualIota, RitualIota::ritualid)
                .mapStream { it }
        }
    }

    override fun isTruthy(): Boolean = true

    override fun toleratesOther(that: Iota): Boolean = typesMatch(this, that) && that is RitualIota && that.ritualid == ritualid

    override fun display(): Component = Component.translatable("hextweaks.iota.ritual")

    override fun hashCode(): Int = ritualid.hashCode()
}
