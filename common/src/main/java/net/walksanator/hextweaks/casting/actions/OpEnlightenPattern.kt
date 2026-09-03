package net.walksanator.hextweaks.casting.actions

import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getPattern
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.Util
import net.minecraft.network.chat.Component
import net.walksanator.hextweaks.casting.PatternRegistry
import net.walksanator.hextweaks.hexcompat.GrandSpellSeedCompat

class OpEnlightenPattern : SpellAction {
    override val argc = 1
    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val input = args.getPattern(0)
        if (input.angles.size > PatternRegistry.GRAND_SPELL_BIT_COUNT) {
            throw MishapInvalidIota(
                PatternIota(input),
                0,
                Component.translatable(
                    "hextweaks.mishap.expected.grand_pattern",
                    PatternRegistry.GRAND_SPELL_BIT_COUNT
                )
            )
        }
        return SpellAction.Result(EnlightenResult(input),MediaConstants.DUST_UNIT * input.angles.size,listOf())
    }

    class EnlightenResult(val arg: HexPattern) : VariableMediaActionResult() {
        override fun execute(env: CastingEnvironment): List<Iota> = listOf(
            PatternIota(
                PatternRegistry.getGrandSpellPattern(
                    env.castingEntity?.uuid?: Util.NIL_UUID,
                    GrandSpellSeedCompat.forLevel(env.world), arg)
            )
        )


    }
}
