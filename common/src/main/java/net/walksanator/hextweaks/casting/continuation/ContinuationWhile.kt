package net.walksanator.hextweaks.casting.continuation

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.*
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapEvalTooMuch
import at.petrak.hexcasting.api.utils.TreeList
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import kotlin.math.max

class ContinuationWhile(val loop: TreeList<Iota>) : ContinuationFrame {
    override val type = WhileType

    object WhileType : ContinuationFrame.Type<ContinuationWhile> {
        private val CODEC: MapCodec<ContinuationWhile> = TreeList.codecOf(IotaType.TYPED_CODEC)
            .xmap(::ContinuationWhile, ContinuationWhile::loop)
            .fieldOf("loop")

        private val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ContinuationWhile> =
            IotaType.TYPED_STREAM_CODEC.apply(TreeList.streamCodecOp())
            .map(::ContinuationWhile, ContinuationWhile::loop)

        override fun codec(): MapCodec<ContinuationWhile> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ContinuationWhile> = STREAM_CODEC
    }

    override fun breakDownwards(stack: TreeList<Iota>): Pair<Boolean, TreeList<Iota>> = Pair(true, stack)

    override fun evaluate(continuation: SpellContinuation, level: ServerLevel, harness: CastingVM): CastResult {
        val (cont, img) = if (harness.image.stack.getOrNull(max(harness.image.stack.size-1,0))?.isTruthy == true) {
            if (loop.isEmpty()) {
                // An empty loop that is about to start will never end, so just throw this mishap without wasting time.
                return CastResult(
                    ListIota(loop), continuation, null,
                    listOf(
                        OperatorSideEffect.DoMishap(
                            MishapEvalTooMuch(),
                            Mishap.Context(
                                null,
                                null
                            )
                        )
                    ),
                    ResolvedPatternType.ERRORED,
                    HexEvalSounds.MISHAP.get()
                )
            }
            val cont = continuation.pushFrame(this).pushFrame(FrameEvaluate(loop,true))
            Pair(cont,harness.image.withUsedOp())
        } else {
            Pair(continuation,harness.image)
        }
        return CastResult(
            ListIota(loop),
            cont, img, listOf(), ResolvedPatternType.EVALUATED,
            HexEvalSounds.THOTH.get()
        )
    }

    override fun size(): Int {
        val iotas = mutableListOf<Iota>()
        iotas.addAll(this.loop)
        var accu = 0
        while (iotas.isNotEmpty()) {
            val iota = iotas.removeFirst()
            accu += 1;
            iotas.addAll(iota.subIotas()?: emptyList())
        }
        return accu
    }
}
