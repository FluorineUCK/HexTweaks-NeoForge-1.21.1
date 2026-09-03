package net.walksanator.hextweaks.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.core.registries.Registries
import net.minecraft.world.damagesource.DamageSource
import net.walksanator.hextweaks.HexTweaksRegistry

class OpSuicide : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, ctx: CastingEnvironment): List<Iota> {
        val caster = ctx.castingEntity ?: return listOf()
        caster.isInvulnerable = false // foolish mortal
        val damageType = ctx.world.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(HexTweaksRegistry.SUS_DAMAGE_TYPE)
        caster.hurt(
            DamageSource(damageType),
            Float.MAX_VALUE
        )
        return listOf()
    }
}
