package net.walksanator.hextweaks.computer

import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.pocket.AbstractPocketUpgrade
import dan200.computercraft.api.pocket.IPocketAccess
import dan200.computercraft.api.upgrades.UpgradeType
import net.minecraft.world.item.ItemStack

class WandPocketUpgrade(val stack: ItemStack) : AbstractPocketUpgrade(
    "Magical",
    stack
) {
    companion object {
        val TYPE: UpgradeType<WandPocketUpgrade> = UpgradeType.simpleWithCustomItem { WandPocketUpgrade(it) }
    }

    override fun getType(): UpgradeType<WandPocketUpgrade> = TYPE

    override fun update(access: IPocketAccess?, peripheral: IPeripheral?) {
        if (peripheral is WandPeripheral) {
            if (!peripheral.isInit) {return}
            peripheral.vm.image = peripheral.vm.image.copy(opsConsumed = 0)
        }
    }
    override fun createPeripheral(access: IPocketAccess?): IPeripheral = WandPeripheral(null,access)
}
