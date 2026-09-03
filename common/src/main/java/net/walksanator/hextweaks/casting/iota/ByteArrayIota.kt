package net.walksanator.hextweaks.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.walksanator.hextweaks.casting.HexTweaksIotaTypes

class ByteArrayIota(val bytes: ByteArray) : Iota({ HexTweaksIotaTypes.BYTEARRAY }) {
    override fun isTruthy(): Boolean = bytes.isNotEmpty()

    override fun toleratesOther(that: Iota): Boolean {
        return typesMatch(this, that) && that is ByteArrayIota && that.bytes.contentEquals(bytes)
    }

    override fun display(): Component = Component.translatable("hextweaks.iota.bytearray")

    override fun hashCode(): Int = bytes.contentHashCode()

    class ByteArrayIotaType : IotaType<ByteArrayIota>() {
        override fun codec(): MapCodec<ByteArrayIota> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ByteArrayIota> = STREAM_CODEC

        override fun color(): Int = 0xFFFF00 // literally ChatGPT suggested color

        companion object {
            val CODEC: MapCodec<ByteArrayIota> = Codec.BYTE.listOf()
                .xmap({ ByteArrayIota(it.toByteArray()) }, { it.bytes.toList() })
                .fieldOf("value")

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ByteArrayIota> = ByteBufCodecs.BYTE_ARRAY
                .map(::ByteArrayIota, ByteArrayIota::bytes)
                .mapStream { it }
        }
    }
}
