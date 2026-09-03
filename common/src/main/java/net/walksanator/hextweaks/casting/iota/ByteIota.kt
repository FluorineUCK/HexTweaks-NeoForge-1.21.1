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

class ByteIota(val byte: Byte) : Iota({ HexTweaksIotaTypes.BYTE }) {
    override fun isTruthy(): Boolean = byte != Byte.MIN_VALUE

    override fun toleratesOther(that: Iota): Boolean {
        return typesMatch(this, that) && that is ByteIota && that.byte == byte
    }

    override fun display(): Component = Component.translatable("hextweaks.iota.byte")

    override fun hashCode(): Int = byte.hashCode()

    class ByteIotaType : IotaType<ByteIota>() {
        override fun codec(): MapCodec<ByteIota> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ByteIota> = STREAM_CODEC

        override fun color(): Int = 0xFF000 // literally ChatGPT suggested color

        companion object {
            val CODEC: MapCodec<ByteIota> = Codec.BYTE
                .xmap(::ByteIota, ByteIota::byte)
                .fieldOf("value")

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ByteIota> = ByteBufCodecs.BYTE
                .map(::ByteIota, ByteIota::byte)
                .mapStream { it }
        }
    }
}
