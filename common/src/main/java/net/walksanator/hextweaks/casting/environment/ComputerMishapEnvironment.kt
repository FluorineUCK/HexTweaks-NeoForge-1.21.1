package net.walksanator.hextweaks.casting.environment

import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedMishapEnv
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.common.lib.HexDamageTypes
import dan200.computercraft.shared.turtle.core.InteractDirection
import dan200.computercraft.shared.turtle.core.TurtleDropCommand
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.core.registries.Registries
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

class ComputerMishapEnvironment(world: ServerLevel, player: ServerPlayer?,val env: net.walksanator.hextweaks.casting.environment.ComputerCastingEnv) : MishapEnvironment(world,player) {
    private val playerDelegate = player?.let(::PlayerBasedMishapEnv)

    private fun spawnYeetedItem(stack: ItemStack, pos: Vec3, velocity: Vec3): Boolean {
        if (stack.isEmpty) return true

        val random = world.random
        val dropped = ItemEntity(
            world,
            pos.x,
            pos.y,
            pos.z,
            stack.copy(),
            velocity.x + (random.nextDouble() - 0.5) * 0.1,
            velocity.y + (random.nextDouble() - 0.5) * 0.1,
            velocity.z + (random.nextDouble() - 0.5) * 0.1
        )
        dropped.setPickUpDelay(40)
        return world.addFreshEntity(dropped)
    }

    override fun yeetHeldItemsTowards(targetPos: Vec3) {
        playerDelegate?.let {
            it.yeetHeldItemsTowards(targetPos)
            return
        }
        if (env.turtleData != null) {
            val slot = env.turtleData.first.selectedSlot
            val item = env.turtleData.first.inventory.getItem(slot)
            val pos = env.turtleData.first.position.center
            val delta = targetPos.subtract(pos).normalize().scale(0.5)
            if (spawnYeetedItem(item, pos, delta)) {
                env.turtleData.first.inventory.setItem(slot, ItemStack.EMPTY)
            }
        } else {
            val host = env.castingEntity ?: return
            val pos = host.position()
            val delta = targetPos.subtract(pos).normalize().scale(0.5)

            for (hand in InteractionHand.entries) {
                val stack = host.getItemInHand(hand)
                if (spawnYeetedItem(stack, pos, delta)) {
                    host.setItemInHand(hand, ItemStack.EMPTY)
                }
            }
        }
    }

    override fun dropHeldItems() {
        playerDelegate?.let {
            it.dropHeldItems()
            return
        }
        if (env.turtleData != null) {
            env.turtleData.first.executeCommand(
                TurtleDropCommand(InteractDirection.FORWARD,64)
            )
        } else {
            val host = env.castingEntity ?: return
            this.yeetHeldItemsTowards(host.position().add(host.lookAngle))
        }
    }

    override fun drown() {
        playerDelegate?.let {
            it.drown()
            return
        }
        val host = env.castingEntity ?: return
        if (host.airSupply < 200) {
            host.hurt(host.damageSources().drown(), 2.0f)
        }
        host.airSupply = 0
    }

    override fun damage(healthProportion: Float) {
        playerDelegate?.let {
            it.damage(healthProportion)
            return
        }
        val host = env.castingEntity ?: return
        val overcastType = host.level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(HexDamageTypes.OVERCAST)
        Mishap.trulyHurt(
            host,
            DamageSource(overcastType),
            host.health * healthProportion
        )
    }

    override fun removeXp(amount: Int) {
        playerDelegate?.removeXp(amount)
    }

    override fun blind(ticks: Int) {
        playerDelegate?.let {
            it.blind(ticks)
            return
        }
        env.castingEntity?.addEffect(MobEffectInstance(MobEffects.BLINDNESS, ticks))
    }

    override fun nauseate(ticks: Int) {
        playerDelegate?.let {
            it.nauseate(ticks)
            return
        }
        env.castingEntity?.addEffect(MobEffectInstance(MobEffects.CONFUSION, ticks))
    }
}
