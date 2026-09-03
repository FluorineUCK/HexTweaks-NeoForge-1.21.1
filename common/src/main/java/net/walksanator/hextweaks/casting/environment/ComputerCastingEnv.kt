package net.walksanator.hextweaks.casting.environment

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.addldata.ADMediaHolder
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.utils.compareMediaItem
import at.petrak.hexcasting.api.utils.otherHand
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.pocket.IPocketAccess
import dan200.computercraft.api.turtle.ITurtleAccess
import dan200.computercraft.api.turtle.TurtleSide
import dan200.computercraft.shared.computer.core.ComputerFamily
import dan200.computercraft.shared.computer.core.ServerComputer
import dan200.computercraft.shared.pocket.core.PocketBrain
import dan200.computercraft.shared.turtle.core.TurtleBrain
import net.minecraft.Util.NIL_UUID
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import net.walksanator.hextweaks.HexTweaks
import net.walksanator.hextweaks.HexTweaksRegistry
import java.util.function.Predicate

class ComputerCastingEnv(val turtleData: Pair<ITurtleAccess, TurtleSide>?, val pocketData: IPocketAccess?,level: ServerLevel,val computer: IComputerAccess) : CastingEnvironment(level) {

    constructor(old: net.walksanator.hextweaks.casting.environment.ComputerCastingEnv, newWorld: ServerLevel) : this(old.turtleData,old.pocketData,newWorld,old.computer)

    private val mishap = ComputerMishapEnvironment(
        world,
        pocketData?.entity as? ServerPlayer,
        this
    )

    override fun getCastingEntity(): LivingEntity? {
        if (pocketData != null) {
            if (pocketData.entity is LivingEntity) {
                return pocketData.entity as LivingEntity
            }
        }
        return null
    }
    override fun getMishapEnvironment(): MishapEnvironment = mishap

    override fun mishapSprayPos(): Vec3 {
        if (turtleData != null) {
            val bpos = turtleData.first.position
            return bpos.center
        } else {
            return castingEntity?.position() ?: pocketData!!.position
        }
    }

    private fun getInventory(): Container {
        return if (turtleData != null) {
          turtleData.first.inventory
        } else {
            val ent = pocketData!!.entity
            if (ent is ServerPlayer) {
                ent.inventory
            } else {
                SimpleContainer(0)
            }
        }
    }

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
        @Suppress("NAME_SHADOWING") var cost = (cost * net.walksanator.hextweaks.HexTweaks.getCONFIG().computerCostMult).toLong()
        val inventory  = getInventory()
        val adMediaHolders: ArrayList<ADMediaHolder> = ArrayList()
        for (i in 0 until inventory.containerSize) {
            val item = inventory.getItem(i)
            val media = HexAPI.instance().findMediaHolder(item)
            if (media?.canProvide() == true) {
                adMediaHolders.add(media)
            }
        }
        adMediaHolders.sortWith(::compareMediaItem)
        adMediaHolders.reverse()
        for (source in adMediaHolders) {
            val found = source.withdrawMedia(cost, simulate)
            cost -= found
            if (cost <= 0) {
                break
            }
        }
        return cost
    }

    override fun isVecInRangeEnvironment(vec: Vec3): Boolean {
        val position: Vec3
        if (pocketData != null) {
            val ent = pocketData.entity
            if (ent is ServerPlayer) {
                val sentinel = HexAPI.instance().getSentinel(ent)
                if ((sentinel != null && sentinel.extendsRange()) && ent.level()
                        .dimension() === sentinel.dimension() && (vec.distanceToSqr(sentinel.position()) <= PlayerBasedCastEnv.DEFAULT_SENTINEL_RADIUS * PlayerBasedCastEnv.DEFAULT_SENTINEL_RADIUS * net.walksanator.hextweaks.HexTweaks.getCONFIG().computerAmbitMult)
                ) {
                    return true
                }
            }
            position = pocketData.position
        } else {
            position = turtleData!!.first.position.center
        }

        return vec.distanceToSqr(position) <= PlayerBasedCastEnv.DEFAULT_AMBIT_RADIUS * PlayerBasedCastEnv.DEFAULT_AMBIT_RADIUS * net.walksanator.hextweaks.HexTweaks.getCONFIG().computerAmbitMult
    }

    override fun hasEditPermissionsAtEnvironment(pos: BlockPos): Boolean {
        if (pocketData != null) {
            val ent = pocketData.entity
            if (ent is ServerPlayer) {
                return ent.gameMode.gameModeForPlayer != GameType.ADVENTURE && this.world.mayInteract(
                    ent, pos
                )
            }
        } else {//it is a turtle. we are just going to give it god mode
            return true
        }
        return true
    }

    override fun getCastingHand(): InteractionHand {
        return if (turtleData != null) {
            when (turtleData.second) {
                TurtleSide.LEFT -> InteractionHand.MAIN_HAND
                TurtleSide.RIGHT -> InteractionHand.OFF_HAND
            }
        } else {
            InteractionHand.MAIN_HAND
        }
    }

    override fun getUsableStacks(mode: StackDiscoveryMode): List<ItemStack> {
        val castingPlayer = this.castingEntity as? ServerPlayer
        if (castingPlayer != null) {
            return getUsableStacksForPlayer(mode, castingHand, castingPlayer)
        }

        // Turtles and non-player pocket hosts expose their whole container.
        val inventory = getInventory()
        val out = ArrayList<ItemStack>(inventory.containerSize)
        for (i in 0 until inventory.containerSize) {
            out.add(inventory.getItem(i))
        }
        return out
    }

    override fun getPrimaryStacks(): MutableList<HeldItemInfo> {
        val castingPlayer = this.castingEntity as? ServerPlayer
        if (castingPlayer != null) {
            return getPrimaryStacksForPlayer(castingHand, castingPlayer).toMutableList()
        }
        if (pocketData != null) {
            val ent = this.castingEntity
            if (ent == null) {return mutableListOf()}
            return mutableListOf(
                 HeldItemInfo(ent.getItemInHand(this.otherHand), this.otherHand),
                 HeldItemInfo(ent.getItemInHand(this.castingHand), this.castingHand)
            )
        } else {
            val slot = turtleData!!.first.selectedSlot
            return mutableListOf(HeldItemInfo(
                turtleData.first.inventory.getItem(slot),
                InteractionHand.MAIN_HAND
            ))
        }
    }

    override fun getHeldItemToOperateOn(stackOk: Predicate<ItemStack>): HeldItemInfo? {
        if (turtleData != null) {
            val inv = turtleData.first.inventory
            val slot = turtleData.first.selectedSlot
            val item = inv.getItem(slot)
            return if (item == ItemStack.EMPTY || !stackOk.test(item)) {
                null
            } else {
                HeldItemInfo(item,InteractionHand.MAIN_HAND)
            }
        } else {
            return super.getHeldItemToOperateOn(stackOk)
        }
    }

    override fun replaceItem(stackOk: Predicate<ItemStack>?, replaceWith: ItemStack, hand: InteractionHand?): Boolean {
        val predicate = stackOk ?: Predicate<ItemStack> { true }
        val player = castingEntity as? ServerPlayer
        if (player != null) {
            return replaceItemForPlayer(predicate, replaceWith, hand, player)
        }

        if (turtleData != null) {
            val inventory = turtleData.first.inventory
            val preferredSlot = turtleData.first.selectedSlot
            val preferred = inventory.getItem(preferredSlot)
            if (predicate.test(preferred)) {
                inventory.setItem(preferredSlot, replaceWith.copy())
                return true
            }
            if (hand == null) {
                for (slot in 0 until inventory.containerSize) {
                    if (slot == preferredSlot) continue
                    if (predicate.test(inventory.getItem(slot))) {
                        inventory.setItem(slot, replaceWith.copy())
                        return true
                    }
                }
            }
            return false
        }

        val host = castingEntity ?: return false
        val targetHand = hand ?: castingHand
        if (!predicate.test(host.getItemInHand(targetHand))) return false
        host.setItemInHand(targetHand, replaceWith.copy())
        return true
    }

    override fun getPigment(): FrozenPigment {
        val color = turtleData?.first?.colour ?: pocketData!!.colour
        val stack = ItemStack(HexTweaksRegistry.RGB_PIGMENT.get())
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().also { it.putInt("rgb", color) }))
        return FrozenPigment(stack, NIL_UUID)
    }

    override fun setPigment(pigment: FrozenPigment?): FrozenPigment? {
        if (pigment == null) {
            return getPigment()
        }
        if (turtleData != null) {
            turtleData.first.colour = pigment.colorProvider.getColor(0f,Vec3.ZERO) and 0x00ffffff
        } else {
            pocketData!!.colour = pigment.colorProvider.getColor(0f,Vec3.ZERO) and 0x00ffffff
        }
        val color = turtleData?.first?.colour ?: pocketData!!.colour
        val stack = ItemStack(HexTweaksRegistry.RGB_PIGMENT.get())
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().also { it.putInt("rgb", color) }))
        return FrozenPigment(stack,NIL_UUID)
    }

    override fun produceParticles(particles: ParticleSpray, colorizer: FrozenPigment) {
        particles.sprayParticles(this.world, colorizer)
    }

    override fun isEnlightened(): Boolean {
        val family = getServerComputer().family
        return when (family) {
            ComputerFamily.NORMAL -> false
            else -> true
        }
    }

    override fun isCreativeMode(): Boolean = getServerComputer().family == ComputerFamily.COMMAND

    override fun printMessage(message: Component) {
        computer.queueEvent("reveal",
            computer.attachmentName,
            message.string
        )
    }

    private fun getServerComputer(): ServerComputer {
        return if (pocketData != null) {
            if (pocketData is PocketBrain) {
                pocketData.computer()
            } else {
                pocketData as? ServerComputer
                    ?: error("Unsupported ComputerCraft pocket access: ${pocketData.javaClass.name}")
            }
        } else {
            (turtleData!!.first as TurtleBrain).owner.serverComputer!!
        }
    }


    override fun postExecution(result: CastResult) {
        super.postExecution(result)

        for (sideEffect in result.sideEffects) {
            if (sideEffect is OperatorSideEffect.DoMishap) {
                val msg = sideEffect.mishap.errorMessageWithName(this, sideEffect.errorCtx)
                if (msg != null) {
                    computer.queueEvent(
                        "mishap",
                        computer.attachmentName,
                        sideEffect.mishap.javaClass.name,
                        msg.string,
                        sideEffect.errorCtx.pattern.toString()
                    )
                }
            }
        }
    }

    override fun precheckAction(match: PatternShapeMatch) {
        super.precheckAction(match)

        // Hex pre-2 maps Special matches to their special-handler registry ID.
        // GrandSpellHandler additionally checks the resolved personalized spell ID.
        val key = actionKey(match)
        if (!net.walksanator.hextweaks.HexTweaks.getCONFIG().isPatternAllowed(key)) {
            throw MishapDisallowedSpell("disallowed", key)
        }
    }
}
