package net.walksanator.hextweaks.casting.handler

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.castables.SpecialHandler
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell
import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.network.chat.Component
import net.walksanator.hextweaks.HexTweaks
import net.walksanator.hextweaks.casting.PatternRegistry
import net.walksanator.hextweaks.casting.environment.ComputerCastingEnv
import net.walksanator.hextweaks.hexcompat.GrandSpellSeedCompat

class GrandSpellHandler(private val action: Action) : SpecialHandler {
    override fun act(): Action = action

    override fun getName(): Component = Component.translatable("hextweaks.handler.grand")

     class Factory : SpecialHandler.Factory<GrandSpellHandler> {
        override fun tryMatch(pattern: HexPattern, env: CastingEnvironment): GrandSpellHandler? {
            val uuid = env.castingEntity?.uuid ?: net.minecraft.Util.NIL_UUID
            val seed = GrandSpellSeedCompat.forLevel(env.world)
            val decoded = PatternRegistry.decodeGrandSpellPattern(uuid, seed, pattern)
                ?: return null
            val act = PatternRegistry.getGrandEntry(decoded.angles, env)
                ?: return null
            if (!HexConfig.server().isActionAllowed(act.second)) {
                return null
            }
            if (
                env is ComputerCastingEnv &&
                !HexTweaks.getCONFIG().isPatternAllowed(act.second)
            ) {
                throw MishapDisallowedSpell("disallowed", act.second)
            }
            return GrandSpellHandler(act.first)
        }

    }
}
