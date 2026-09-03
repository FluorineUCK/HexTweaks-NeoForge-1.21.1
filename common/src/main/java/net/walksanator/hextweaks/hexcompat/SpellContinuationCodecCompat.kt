package net.walksanator.hextweaks.hexcompat

import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import com.mojang.serialization.Codec
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

/**
 * Pre-39 continuation codec adapter with read compatibility for saves emitted
 * by the earlier pre-2 port.
 *
 * Hex Casting pre-39 provides correct public data and stream codecs, so all new
 * values use those codecs directly. The custom compound format remains a
 * decode-only fallback so worlds created with the pre-2 port keep their stored
 * ComputerCraft/ravenmind continuations.
 */
object SpellContinuationCodecCompat {
    private const val FORMAT_KEY = "hextweaks:format"
    private const val FRAMES_KEY = "hextweaks:frames"
    private const val FORMAT_VERSION = 1
    private const val MAX_FRAMES = 65_536

    @JvmField
    val CODEC: Codec<SpellContinuation> = SpellContinuation.CODEC

    @JvmField
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SpellContinuation> =
        SpellContinuation.STREAM_CODEC

    fun encode(
        continuation: SpellContinuation,
        ops: DynamicOps<Tag>
    ): Tag = CODEC.encodeStart(ops, continuation).getOrThrow()

    fun decode(
        encoded: Tag,
        ops: DynamicOps<Tag>
    ): SpellContinuation {
        if (encoded is CompoundTag && encoded.contains(FRAMES_KEY, Tag.TAG_LIST.toInt())) {
            return decodeLegacyHexTweaksFormat(encoded, ops)
        }

        return CODEC.parse(ops, encoded).getOrThrow()
    }

    private fun decodeLegacyHexTweaksFormat(
        encoded: CompoundTag,
        ops: DynamicOps<Tag>
    ): SpellContinuation {
        check(encoded.getInt(FORMAT_KEY) == FORMAT_VERSION) {
            "Unsupported HexTweaks continuation format ${encoded.getInt(FORMAT_KEY)}"
        }

        val encodedFrames = encoded.getList(FRAMES_KEY, Tag.TAG_COMPOUND.toInt())
        check(encodedFrames.size <= MAX_FRAMES) {
            "Spell continuation exceeds $MAX_FRAMES frames"
        }

        val frames = ArrayList<ContinuationFrame>(encodedFrames.size)
        for (index in encodedFrames.indices) {
            frames.add(
                ContinuationFrame.Type.TYPED_CODEC
                    .parse(ops, encodedFrames.getCompound(index))
                    .getOrThrow()
            )
        }
        return fromFrames(frames)
    }

    private fun fromFrames(frames: List<ContinuationFrame>): SpellContinuation {
        check(frames.size <= MAX_FRAMES) {
            "Spell continuation exceeds $MAX_FRAMES frames"
        }

        var continuation: SpellContinuation = SpellContinuation.Done
        for (index in frames.indices.reversed()) {
            continuation = continuation.pushFrame(frames[index])
        }
        return continuation
    }
}
