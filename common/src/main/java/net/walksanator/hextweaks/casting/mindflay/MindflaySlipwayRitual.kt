package net.walksanator.hextweaks.casting.mindflay

import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.walksanator.hextweaks.casting.MindflayRegistry
import ram.talia.hexal.common.entities.BaseWisp
import ram.talia.hexal.common.entities.WanderingWisp
import ram.talia.hexal.common.lib.HexalBlocks

object MindflaySlipwayRitual {
    fun createSlipway(input: MindflayInput): net.walksanator.hextweaks.casting.mindflay.MindflayResult {
        if (input.target !is EntityIota) {return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        }
        val entity = input.target.getEntity(input.env.world)
        if (entity !is BaseWisp) {return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        } // there is no wisp
        if (!input.env.isVecInRange(entity.position())) {
            return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        }

        val points = MindflayRegistry.calcuateVillagerPoints(input.inputs)
        if (points < 80) { return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        } // insufficient points

        val block = input.env.world.getBlockState(entity.blockPosition())
        if (!block.isAir) { return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        } //wisp is inside a block. so no placing

        val placed = input.env.world.setBlock(
            entity.blockPosition(),
            HexalBlocks.SLIPWAY.defaultBlockState(),
            Block.UPDATE_ALL
        )
        if (!placed) return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        entity.discard()

        MindflayRegistry.performBrainsweeps(input.inputs,input.env.castingEntity as? ServerPlayer)

        return net.walksanator.hextweaks.casting.mindflay.MindflayResult(true)
    }

    fun burstSlipway(input: MindflayInput): net.walksanator.hextweaks.casting.mindflay.MindflayResult {
        if (input.target !is Vec3Iota) {return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        }
        val pos = input.target.vec3
        if (!input.env.isVecInRange(pos)) {return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        } // out of range

        val world = input.env.world
        val bpos = BlockPos.containing(pos)

        val points = MindflayRegistry.calcuateVillagerPoints(input.inputs)
        if (points < 16) { return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        }

        val state = world.getBlockState(bpos)
        if (state.block != HexalBlocks.SLIPWAY) { return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        } //that isn't a slipway so we cant collapse it

        if (!world.setBlock(bpos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
            return net.walksanator.hextweaks.casting.mindflay.MindflayResult(false)
        }

        repeat(world.random.nextInt(11) + 10) {
            val wisp = WanderingWisp(world, bpos.center)
            world.addFreshEntity(wisp)
        }

        MindflayRegistry.performBrainsweeps(input.inputs,input.env.castingEntity as? ServerPlayer)

        return net.walksanator.hextweaks.casting.mindflay.MindflayResult(true)
    }
}
