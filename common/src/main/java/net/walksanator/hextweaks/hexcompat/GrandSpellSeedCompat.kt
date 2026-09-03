package net.walksanator.hextweaks.hexcompat

import net.minecraft.server.level.ServerLevel

/**
 * Keeps Grand-pattern personalization on one world seed across every
 * dimension. The two-argument seam makes the policy deterministic to probe
 * without constructing a synthetic ServerLevel.
 */
object GrandSpellSeedCompat {
    @JvmStatic
    fun forLevel(level: ServerLevel): Long =
        select(level.seed, level.server.overworld().seed)

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun select(currentDimensionSeed: Long, overworldSeed: Long): Long =
        overworldSeed
}
