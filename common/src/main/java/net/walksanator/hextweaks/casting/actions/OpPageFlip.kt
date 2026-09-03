package net.walksanator.hextweaks.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.common.items.storage.ItemSpellbook
import at.petrak.hexcasting.common.lib.HexItems

class OpPageFlip(
    private val rotateRight: Boolean
) : ConstMediaAction {
    override val argc = 0
    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {

        val res = env.getHeldItemToOperateOn {
            it.`is`(HexItems.SPELLBOOK.get().asItem())
        }
        if (res == null) {return listOf()}
        val handStack = res.component1()
        val hand = res.component2()

        if (handStack.`is`(HexItems.SPELLBOOK.get().asItem())) {
            ItemSpellbook.rotatePageIdx(handStack,rotateRight, env.world)
        }

        return listOf()
    }

}
