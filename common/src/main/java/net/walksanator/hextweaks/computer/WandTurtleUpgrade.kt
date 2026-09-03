package net.walksanator.hextweaks.computer

import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade
import dan200.computercraft.api.turtle.ITurtleAccess
import dan200.computercraft.api.turtle.TurtleSide
import dan200.computercraft.api.turtle.TurtleUpgradeType
import dan200.computercraft.api.upgrades.UpgradeType
import net.minecraft.world.item.ItemStack

class WandTurtleUpgrade(val item:ItemStack) : AbstractTurtleUpgrade(
    TurtleUpgradeType.PERIPHERAL,
    "Magical",
    item
) {
    companion object {
        val TYPE: UpgradeType<WandTurtleUpgrade> = UpgradeType.simpleWithCustomItem { WandTurtleUpgrade(it) }
    }

    override fun getType(): UpgradeType<WandTurtleUpgrade> = TYPE

    override fun update(access: ITurtleAccess, side: TurtleSide) {
        val peripheral = access.getPeripheral(side)
        if (peripheral is WandPeripheral) {
            if (!peripheral.isInit) {return}
            peripheral.vm.image = peripheral.vm.image.copy(opsConsumed = 0)
        }
    }

    override fun createPeripheral(turtle: ITurtleAccess, side: TurtleSide): IPeripheral = WandPeripheral(Pair(turtle,side),null)
}
