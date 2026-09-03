package net.walksanator.hextweaks.casting


import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.math.HexAngle
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.items.ItemLoreFragment
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.server.ScrungledPatternsSave
import at.petrak.hexcasting.xplat.IXplatAbstractions
import dev.architectury.platform.Platform
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.walksanator.hextweaks.casting.actions.*
import net.walksanator.hextweaks.hexcompat.GrandSpellSeedCompat
import java.util.*
import java.util.function.BiConsumer
import java.util.function.BiFunction


object PatternRegistry {
    const val GRAND_SPELL_BIT_COUNT = 128

    private val GRAND_REGISTRY: HashMap<List<HexAngle>, Pair<Action, ResourceLocation>> = HashMap()
    private val ALTERNATIVE_REGISTRY: MutableList<GrandPatternResolve> = mutableListOf()
    private val DEFERRED_GRAND_REGISTRY: MutableList<Triple<Holder<ActionRegistryEntry>, Action, ResourceLocation>> = mutableListOf()
    private val deferred: HashMap<ResourceLocation, ActionRegistryEntry> = HashMap()
    fun registerGrandSpells(pattern: List<HexAngle>, action: Action, namespace: ResourceLocation) {
        if (GRAND_REGISTRY.containsKey(pattern)) {
            throw IllegalArgumentException(
                "a pattern is allready registered under that pattern id: %s sig: %s trying to register %s".format(
                    GRAND_REGISTRY[pattern]?.second,
                    pattern.toSig(),
                    namespace

                )
            )
        }
        GRAND_REGISTRY[pattern] = Pair(action, namespace)
    }

    val THE_FUNNY = pattern(HexDir.WEST, "dewdeqwwedaqedwadweqewwd", "suicide", OpSuicide(), true)
    val INFUSE_WILL = pattern(HexDir.SOUTH_WEST, "waawaawaqwaeaeaeaeaea", "infusion", OpEnlightenPattern())
    val PAGE_RIGHT = pattern(HexDir.SOUTH_WEST, "qqaw", "page/right", OpPageFlip(true))
    val PAGE_LEFT = pattern(HexDir.SOUTH_EAST, "eedw", "page/left", OpPageFlip(false))
    val WHILE = pattern(HexDir.NORTH_EAST, "wdwadad", "while", OpLoopWhile)


    val GETWAVE = pattern(HexDir.SOUTH_EAST, "qdewedq", "wave", OpWaveRefl)
    val SLATE_NORMAL = pattern(HexDir.SOUTH_EAST, "qdewedqwqaq", "normal", OpSlateNormal)



    init {
        // grand flaying
        patternGrand(
            HexActions.BRAINSWEEP,
            "mindflayplus",
            OpMindflayPlus,
            true
        )
        patternGrand(HexActions.EXPLODE,"explode", OpBiggerBomb(false), false)
        patternGrand(HexActions.`EXPLODE$FIRE`, "fireball", OpBiggerBomb(true), false)
        if (Platform.isModLoaded("moreiotas")) {
            val hell = pattern(HexDir.EAST,"wqwqwqwqwqwewawwwawwwaw","you_like_drinking_potions", OpLackingWill)
            patternGrand(
                Holder.direct(hell),"nadith", OpEgyptianPlagues,true
            )
        }
    }


    private fun pattern(
        start: HexDir,
        angles: String,
        name: String,
        action: Action,
        isGrand: Boolean = false
    ): ActionRegistryEntry {
        val pat = patternAllowIllegal(start, angles)
        val resourceLocation = ResourceLocation.fromNamespaceAndPath(net.walksanator.hextweaks.HexTweaks.MOD_ID, name)

        val ARE = ActionRegistryEntry(pat, action)
        if (isGrand) {
            registerGrandSpells(pat.angles, action, resourceLocation)
        } else {
            if (deferred.containsKey(resourceLocation)) throw IllegalArgumentException("two patterns are vying for $resourceLocation id. fix this")
            deferred[resourceLocation] = ARE
        }
        return ARE
    }

    private fun patternGrand(
        parent: Holder<ActionRegistryEntry>,
        name: String,
        action: Action,
        parentIsGreat: Boolean = false
    ) {
        if (parentIsGreat) {
            registerAlternative { angles, env ->
                val parentEntry = parent.value()
                val reg = IXplatAbstractions.INSTANCE.actionRegistry
                val save = ScrungledPatternsSave.open(env.world)
                val lookup = save.lookup(angles.toSig())
                if (lookup != null) {
                    if (reg.get(lookup.key) == parentEntry) {
                        return@registerAlternative Optional.of(
                            Pair(
                                action,
                                ResourceLocation.fromNamespaceAndPath("hextweaks", name)
                            )
                        )
                    }
                } else if (parentEntry.prototype.angles == angles) {
                    return@registerAlternative Optional.of(
                        Pair(
                            action,
                        ResourceLocation.fromNamespaceAndPath("hextweaks", name)
                        )
                    )
                }
                return@registerAlternative Optional.empty<Pair<Action,ResourceLocation>>()
            }
        } else {
            val resourceLocation = ResourceLocation.fromNamespaceAndPath("hextweaks", name)
            DEFERRED_GRAND_REGISTRY.add(Triple(parent, action, resourceLocation))
        }
    }

    private fun registerDeferredGrandSpells() {
        synchronized(DEFERRED_GRAND_REGISTRY) {
            if (DEFERRED_GRAND_REGISTRY.isEmpty()) return
            DEFERRED_GRAND_REGISTRY.forEach { (parent, action, resourceLocation) ->
                registerGrandSpells(parent.value().prototype.angles, action, resourceLocation)
            }
            DEFERRED_GRAND_REGISTRY.clear()
        }
    }

    fun registerAlternative(fn: GrandPatternResolve) = ALTERNATIVE_REGISTRY.add(fn)

    fun getGrandEntry(sigs: List<HexAngle>, env: CastingEnvironment): Pair<Action, ResourceLocation>? {
        registerDeferredGrandSpells()
        var registry_check = GRAND_REGISTRY[sigs]
        if (registry_check == null) {
            for (func in ALTERNATIVE_REGISTRY) {
                val res = func.apply(sigs, env)
                if (res.isPresent) {
                    registry_check = res.get()
                }
            }
        }
        val caster = env.castingEntity as? ServerPlayer
        if (registry_check != null) {
            if (caster != null) {
                val resloc = registry_check.second
                val advid = ResourceLocation.fromNamespaceAndPath(resloc.namespace, "grandspell/%s".format(resloc.path))
                //HexTweaks.LOGGER.info("Trying to grant %s advancement".format(advid))
                val adv = caster.server.advancements.get(advid)
                if (adv != null) {
                    caster.advancements.award(adv, ItemLoreFragment.CRITEREON_KEY)
                } else {
                    net.walksanator.hextweaks.HexTweaks.LOGGER.warn("Advancement from grand spell {} does not exists",advid)
                }
            } else {
                net.walksanator.hextweaks.HexTweaks.LOGGER.info("There is no player to grant advancement to for grand spell")
            }
        }
        return registry_check
    }

    fun patternAllowIllegal(start: HexDir, angles: String): HexPattern {
        val pat = HexPattern(start, mutableListOf())
        for ((idx, c) in angles.withIndex()) {
            val angle = when (c) {
                'w' -> HexAngle.FORWARD
                'e' -> HexAngle.RIGHT
                'd' -> HexAngle.RIGHT_BACK
                // for completeness ... >:)
                's' -> HexAngle.BACK
                'a' -> HexAngle.LEFT_BACK
                'q' -> HexAngle.LEFT
                else -> throw IllegalArgumentException("Cannot match $c at idx $idx to a direction")
            }
            pat.angles.add(angle)
        }
        return pat
    }

    fun getGrandSpellPattern(player: ServerPlayer, level: ServerLevel, pat: HexPattern): HexPattern =
        getGrandSpellPattern(player.uuid, GrandSpellSeedCompat.forLevel(level), pat)

    fun getGrandSpellPattern(uuid: UUID, seed: Long, pat: HexPattern): HexPattern {
        require(pat.angles.size <= GRAND_SPELL_BIT_COUNT) {
            "Grand spell patterns cannot contain more than $GRAND_SPELL_BIT_COUNT angles"
        }
        val expectedBits = grandSpellBits(uuid, seed)

        val resulting: MutableList<HexAngle> = mutableListOf()
        for ((index, sig) in pat.angles.withIndex()) {
            if (expectedBits[index]) {
                resulting.add(HexAngle.BACK)
                resulting.add(HexAngle.BACK)
            }
            resulting.add(sig)
        }
        return HexPattern(pat.startDir, resulting)
    }

    /**
     * Reverses [getGrandSpellPattern] using the expected personalization bits as
     * framing information. A greedy BACK/BACK scan is ambiguous whenever the
     * source pattern itself contains BACK.
     */
    fun decodeGrandSpellPattern(uuid: UUID, seed: Long, pat: HexPattern): HexPattern? {
        val expectedBits = grandSpellBits(uuid, seed)
        val decoded = mutableListOf<HexAngle>()
        var encodedIndex = 0
        var bitIndex = 0

        while (encodedIndex < pat.angles.size) {
            if (bitIndex >= expectedBits.size) {
                return null
            }

            if (expectedBits[bitIndex]) {
                if (
                    encodedIndex + 2 >= pat.angles.size ||
                    pat.angles[encodedIndex] != HexAngle.BACK ||
                    pat.angles[encodedIndex + 1] != HexAngle.BACK
                ) {
                    return null
                }
                decoded.add(pat.angles[encodedIndex + 2])
                encodedIndex += 3
            } else {
                decoded.add(pat.angles[encodedIndex])
                encodedIndex += 1
            }
            bitIndex += 1
        }

        return HexPattern(pat.startDir, decoded)
    }

    private fun grandSpellBits(uuid: UUID, seed: Long): BooleanArray {
        val upper = uuid.mostSignificantBits.xor(seed)
        val lower = uuid.leastSignificantBits.xor(seed)
        return BooleanArray(GRAND_SPELL_BIT_COUNT).also { bits ->
            for (i in 0 until 64) {
                bits[i] = ((upper shr (63 - i)) and 1L) == 1L
                bits[i + 64] = ((lower shr (63 - i)) and 1L) == 1L
            }
        }
    }

    fun register(r: BiConsumer<ActionRegistryEntry, ResourceLocation>) {
        for ((key, value) in deferred) {
            r.accept(value, key)
        }
    }

}

typealias GrandPatternResolve = BiFunction<
        List<HexAngle>,
        CastingEnvironment,
        Optional<Pair<Action, ResourceLocation>>
        >

fun List<HexAngle>.toSig(): String {
    var out = ""
    for (angle in this) {
        out += when (angle) {
            HexAngle.FORWARD -> 'w'
            HexAngle.RIGHT -> 'e'
            HexAngle.RIGHT_BACK -> 'd'
            HexAngle.BACK -> 's'
            HexAngle.LEFT -> 'q'
            HexAngle.LEFT_BACK -> 'a'
        }
    }
    return out
}
