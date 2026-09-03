package net.walksanator.hextweaks.forge

import at.petrak.hexcasting.api.casting.circles.CircleExecutionState
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.BooleanIota
import at.petrak.hexcasting.api.casting.iota.ContinuationIota
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.math.HexAngle
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.utils.TreeList
import at.petrak.hexcasting.common.lib.HexDataComponents
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import com.mojang.authlib.GameProfile
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.pocket.IPocketAccess
import dan200.computercraft.api.pocket.IPocketUpgrade
import dan200.computercraft.api.turtle.ITurtleAccess
import dan200.computercraft.api.turtle.ITurtleUpgrade
import dan200.computercraft.api.turtle.TurtleSide
import dan200.computercraft.shared.computer.core.ComputerFamily
import dan200.computercraft.shared.computer.core.ServerComputer
import dan200.computercraft.shared.pocket.core.PocketBrain
import dan200.computercraft.shared.pocket.core.PocketHolder
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.InteractionHand
import net.minecraft.world.SimpleContainer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.fml.ModList
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.util.FakePlayer
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.walksanator.hextweaks.HexTweaks
import net.walksanator.hextweaks.HexTweaksConfig
import net.walksanator.hextweaks.HexTweaksRegistry
import net.walksanator.hextweaks.SecurityLevel
import net.walksanator.hextweaks.casting.HexTweaksContinuationTypes
import net.walksanator.hextweaks.casting.HexTweaksIotaTypes
import net.walksanator.hextweaks.casting.MindflayRegistry
import net.walksanator.hextweaks.casting.PatternRegistry
import net.walksanator.hextweaks.casting.actions.OpBiggerBomb
import net.walksanator.hextweaks.casting.actions.OpEgyptianPlagues
import net.walksanator.hextweaks.casting.actions.OpEnlightenPattern
import net.walksanator.hextweaks.casting.actions.OpLackingWill
import net.walksanator.hextweaks.casting.actions.OpLoopWhile
import net.walksanator.hextweaks.casting.actions.OpMindflayPlus
import net.walksanator.hextweaks.casting.actions.OpPageFlip
import net.walksanator.hextweaks.casting.actions.OpSlateNormal
import net.walksanator.hextweaks.casting.actions.OpSuicide
import net.walksanator.hextweaks.casting.actions.OpWaveRefl
import net.walksanator.hextweaks.casting.continuation.ContinuationWhile
import net.walksanator.hextweaks.casting.environment.ComputerCastingEnv
import net.walksanator.hextweaks.casting.handler.GrandSpellHandler
import net.walksanator.hextweaks.casting.iota.ByteArrayIota
import net.walksanator.hextweaks.casting.iota.ByteIota
import net.walksanator.hextweaks.casting.iota.RitualIota
import net.walksanator.hextweaks.casting.mindflay.MindflayInput
import net.walksanator.hextweaks.computer.IotaSerdeRegistry
import net.walksanator.hextweaks.hexcompat.SpellContinuationCodecCompat
import net.walksanator.hextweaks.hexcompat.GrandSpellSeedCompat
import net.walksanator.hextweaks.computer.WandPeripheral
import org.ejml.simple.SimpleMatrix
import ram.talia.hexal.api.casting.iota.GateIota
import ram.talia.hexal.common.entities.WanderingWisp
import ram.talia.hexal.common.lib.HexalBlocks
import ram.talia.moreiotas.MoreIotasConfig
import ram.talia.moreiotas.api.casting.iota.EntityTypeIota
import ram.talia.moreiotas.api.casting.iota.IotaTypeIota
import ram.talia.moreiotas.api.casting.iota.ItemStackIota
import ram.talia.moreiotas.api.casting.iota.ItemTypeIota
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.casting.iota.StringIota
import java.lang.reflect.Proxy
import java.util.Optional
import java.util.UUID

/**
 * Development-only, property-gated regression probes. None of these hooks are
 * active in a normal packaged runtime.
 */
object HexTweaksProbe {
    private val log = HexTweaks.LOGGER
    private var hasRun = false
    private var itemJoinCapture: MutableList<ItemEntity>? = null

    @JvmStatic
    fun register() {
        NeoForge.EVENT_BUS.addListener(::onServerTick)
        NeoForge.EVENT_BUS.addListener(::onEntityJoin)
    }

    private fun onEntityJoin(event: EntityJoinLevelEvent) {
        val item = event.entity as? ItemEntity ?: return
        itemJoinCapture?.add(item)
    }

    private fun captureJoinedItems(action: () -> Unit): List<ItemEntity> {
        check(itemJoinCapture == null) { "nested item-join capture" }
        val captured = mutableListOf<ItemEntity>()
        itemJoinCapture = captured
        try {
            action()
        } finally {
            itemJoinCapture = null
        }
        return captured
    }

    private fun onServerTick(event: ServerTickEvent.Post) {
        if (hasRun || event.server.tickCount < 20) {
            return
        }
        hasRun = true
        val server = event.server
        var failures = 0
        try {
            failures += probe("registries") { checkRegistries(server) }
            failures += probe("action_tags") { checkActionTags(server) }
            failures += probe("iota_codecs") { checkIotaCodecs(server) }
            failures += probe("while_continuation") { checkWhileContinuation(server) }
            failures += probe("core_actions") { checkCoreActions(server) }
            failures += probe("grand_spell_handler") { checkGrandSpellHandler(server) }
            failures += probe("give_grand_command") { checkGiveGrandCommand(server) }
            failures += probe("computercraft") { checkComputerCraft(server) }
            failures += probe("mindflay_restock") { checkRestockRitual(server) }
            if (ModList.get().isLoaded("moreiotas")) {
                failures += probe("moreiotas_actions") { checkMoreIotasActions(server) }
                failures += probe("moreiotas_serde") { checkMoreIotasSerde(server) }
            }
            if (ModList.get().isLoaded("hexal")) {
                failures += probe("hexal_slipway_rituals") { checkHexalRituals(server) }
            }

            if (failures == 0) {
                log.info(
                    "[HEXTWEAKS-PROBE] aggregate=PASS hexcasting=pre-39 registries=PASS codecs=PASS while=PASS grand=PASS cc=PASS rituals=PASS"
                )
            } else {
                log.error("[HEXTWEAKS-PROBE] aggregate=FAIL failure_count={}", failures)
            }
        } catch (t: Throwable) {
            log.error("[HEXTWEAKS-PROBE] aggregate=FAIL exception", t)
            failures++
        } finally {
            scheduleHardExit(if (failures == 0) 0 else 1)
            server.halt(false)
        }
    }

    private fun scheduleHardExit(exitCode: Int) {
        Thread({
            try {
                Thread.sleep(15_000L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            Runtime.getRuntime().halt(exitCode)
        }, "hextweaks-probe-hard-stop").apply {
            isDaemon = true
            start()
        }
    }

    private inline fun probe(label: String, check: () -> String): Int =
        try {
            log.info("[HEXTWEAKS-PROBE] {}=PASS {}", label, check())
            0
        } catch (t: Throwable) {
            log.error("[HEXTWEAKS-PROBE] {}=FAIL", label, t)
            1
        }

    private fun id(path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(HexTweaks.MOD_ID, path)

    private fun entityProbeBase(server: MinecraftServer): BlockPos {
        val level = server.overworld()
        val spawnChunk = ChunkPos(level.sharedSpawnPos)
        val y = (level.sharedSpawnPos.y + 64)
            .coerceIn(level.minBuildHeight + 16, level.maxBuildHeight - 16)
        return BlockPos(spawnChunk.middleBlockX, y, spawnChunk.middleBlockZ)
    }

    private fun checkRegistries(server: MinecraftServer): String {
        val item = BuiltInRegistries.ITEM.get(id("rgb_pigment"))
        check(BuiltInRegistries.ITEM.getKey(item) == id("rgb_pigment"))
        check(item is PigmentItem)
        check(item.defaultMaxStackSize == 1)

        val pigmentStack = ItemStack(item)
        pigmentStack.set(
            DataComponents.CUSTOM_DATA,
            CustomData.of(CompoundTag().also { it.putInt("rgb", 0x123456) })
        )
        val pigmentColor = (item as PigmentItem)
            .provideColor(pigmentStack, UUID(0L, 0L))
            .getColor(0f, Vec3.ZERO) and 0x00ffffff
        check(pigmentColor == 0x123456)

        val actions = server.registryAccess().registryOrThrow(HexRegistries.ACTION)
        val expectedActions = mutableListOf(
            id("infusion"),
            id("page/right"),
            id("page/left"),
            id("while"),
            id("wave"),
            id("normal")
        )
        if (ModList.get().isLoaded("moreiotas")) {
            expectedActions += id("you_like_drinking_potions")
        }
        check(expectedActions.all(actions::containsKey)) {
            "missing actions ${expectedActions.filterNot(actions::containsKey)}"
        }

        val specialHandlers = server.registryAccess().registryOrThrow(HexRegistries.SPECIAL_HANDLER)
        check(specialHandlers.get(id("grand")) === HexTweaksRegistry.GRAND_HANDLER.get())

        val iotaTypes = server.registryAccess().registryOrThrow(HexRegistries.IOTA_TYPE)
        check(iotaTypes.get(id("byte")) === HexTweaksIotaTypes.BYTE)
        check(iotaTypes.get(id("bytearray")) === HexTweaksIotaTypes.BYTEARRAY)
        check(iotaTypes.get(id("ritual")) === HexTweaksIotaTypes.RITUAL)

        val continuations = server.registryAccess().registryOrThrow(HexRegistries.CONTINUATION_TYPE)
        val stableWhileId = ResourceLocation.fromNamespaceAndPath("hexcasting", "while")
        check(continuations.get(stableWhileId) === HexTweaksContinuationTypes.WHILE)
        check(!continuations.containsKey(id("while")))

        val advancementIds = listOf("explode", "fireball", "mindflayplus")
            .map { id("grandspell/$it") }
        check(advancementIds.all { server.advancements.get(it) != null }) {
            "missing advancements ${advancementIds.filter { server.advancements.get(it) == null }}"
        }

        return "actions=${expectedActions.size} iotas=3 special=hextweaks:grand continuation=hexcasting:while item=hextweaks:rgb_pigment"
    }

    private fun checkActionTags(server: MinecraftServer): String {
        val registry = server.registryAccess().registryOrThrow(HexRegistries.ACTION)
        val expected = mutableListOf(id("infusion"))
        if (ModList.get().isLoaded("moreiotas")) {
            expected += id("you_like_drinking_potions")
        }

        fun missing(tag: net.minecraft.tags.TagKey<at.petrak.hexcasting.api.casting.ActionRegistryEntry>) =
            expected.filter { actionId ->
                val action = registry.get(actionId)
                action == null || !registry.wrapAsHolder(action).`is`(tag)
            }

        val missingPerWorld = missing(HexTags.Actions.PER_WORLD_PATTERN)
        val missingRequires = missing(HexTags.Actions.REQUIRES_ENLIGHTENMENT)
        val missingCanStart = missing(HexTags.Actions.CAN_START_ENLIGHTEN)
        check(missingPerWorld.isEmpty()) { "missing per-world $missingPerWorld" }
        check(missingRequires.isEmpty()) { "missing requires-enlightenment $missingRequires" }
        check(missingCanStart.isEmpty()) { "missing can-start-enlighten $missingCanStart" }
        return "members=${expected.joinToString()}"
    }

    private fun checkIotaCodecs(server: MinecraftServer): String {
        val originals = listOf<Iota>(
            ByteIota((-17).toByte()),
            ByteArrayIota(byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)),
            RitualIota(id("restock"))
        )

        val nbtRoundTrips = originals.map { original ->
            val decoded = roundTripIotaNbt(server, original)
            check(Iota.tolerates(original, decoded)) {
                "NBT round trip changed ${original.javaClass.simpleName}: $decoded"
            }
            decoded.javaClass.simpleName
        }

        val streamRoundTrips = originals.map { original ->
            val decoded = roundTripIotaStream(server, original)
            check(Iota.tolerates(original, decoded)) {
                "stream round trip changed ${original.javaClass.simpleName}: $decoded"
            }
            decoded.javaClass.simpleName
        }

        check(ByteArrayIota(byteArrayOf(1, 2, 3)) == ByteArrayIota(byteArrayOf(1, 2, 3)))
        check(ByteArrayIota(byteArrayOf(1, 2, 3)).hashCode() == ByteArrayIota(byteArrayOf(1, 2, 3)).hashCode())
        return "nbt=${nbtRoundTrips.joinToString()} stream=${streamRoundTrips.joinToString()}"
    }

    private fun roundTripIotaNbt(server: MinecraftServer, original: Iota): Iota {
        val ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess())
        val encoded = IotaType.TYPED_CODEC.encodeStart(ops, original).getOrThrow()
        return IotaType.TYPED_CODEC.parse(ops, encoded).getOrThrow()
    }

    private fun roundTripIotaStream(server: MinecraftServer, original: Iota): Iota {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess())
        return try {
            IotaType.TYPED_STREAM_CODEC.encode(buffer, original)
            IotaType.TYPED_STREAM_CODEC.decode(buffer)
        } finally {
            buffer.release()
        }
    }

    private fun checkWhileContinuation(server: MinecraftServer): String {
        val level = server.overworld()
        val fake = FakePlayerFactory.getMinecraft(level)
        val env = StaffCastEnv(fake, InteractionHand.MAIN_HAND)
        val loop = TreeList.from(listOf(DoubleIota(2.0), BooleanIota(true)))
        val frame = ContinuationWhile(loop)
        val ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess())

        val encodedFrame = ContinuationFrame.Type.TYPED_CODEC
            .encodeStart(ops, frame)
            .getOrThrow()
        val decodedFrame = ContinuationFrame.Type.TYPED_CODEC
            .parse(ops, encodedFrame)
            .getOrThrow()
        check(decodedFrame is ContinuationWhile)
        check(decodedFrame.loop.toList().size == 2)

        val continuation = SpellContinuation.Done.pushFrame(frame)
        val encodedContinuation = SpellContinuationCodecCompat.encode(continuation, ops)
        val decodedContinuation = SpellContinuationCodecCompat.decode(encodedContinuation, ops)
        check(decodedContinuation is SpellContinuation.NotDone)
        check(decodedContinuation.frame is ContinuationWhile)

        // Exercise the actual Hex Iota codecs used by ravenmind storage and
        // network synchronization, rather than only HexTweaks' Lua adapter.
        val continuationIota = ContinuationIota(continuation)
        val nbtContinuation = roundTripIotaNbt(server, continuationIota) as ContinuationIota
        check(nbtContinuation.continuation is SpellContinuation.NotDone)
        check(
            (nbtContinuation.continuation as SpellContinuation.NotDone).frame
                is ContinuationWhile
        )
        val streamContinuation =
            roundTripIotaStream(server, continuationIota) as ContinuationIota
        check(streamContinuation.continuation is SpellContinuation.NotDone)
        check(
            (streamContinuation.continuation as SpellContinuation.NotDone).frame
                is ContinuationWhile
        )

        val truthyVm = CastingVM(
            CastingImage().copy(stack = TreeList.from(listOf<Iota>(BooleanIota(true)))),
            env
        )
        val truthy = frame.evaluate(SpellContinuation.Done, level, truthyVm)
        val truthyImage = truthy.newData
        check(truthyImage != null && truthyImage.opsConsumed == 1L)
        check(truthy.continuation is SpellContinuation.NotDone)
        check((truthy.continuation as SpellContinuation.NotDone).frame is FrameEvaluate)

        val falseVm = CastingVM(
            CastingImage().copy(stack = TreeList.from(listOf<Iota>(BooleanIota(false)))),
            env
        )
        val falseResult = frame.evaluate(SpellContinuation.Done, level, falseVm)
        check(falseResult.continuation === SpellContinuation.Done)
        check(falseResult.newData === falseVm.image)

        val emptyFrame = ContinuationWhile(TreeList.empty())
        val emptyResult = emptyFrame.evaluate(SpellContinuation.Done, level, truthyVm)
        check(emptyResult.newData == null)
        check(emptyResult.sideEffects.singleOrNull() is OperatorSideEffect.DoMishap)

        val breakdownStack = TreeList.from(listOf<Iota>(DoubleIota(1.0)))
        val breakdown = frame.breakDownwards(breakdownStack)
        check(breakdown.first && breakdown.second == breakdownStack)

        try {
            OpLoopWhile.operate(env, CastingImage(), SpellContinuation.Done)
            error("OpLoopWhile accepted an empty stack")
        } catch (_: MishapNotEnoughArgs) {
        }

        val operateResult = OpLoopWhile.operate(
            env,
            CastingImage().copy(stack = TreeList.from(listOf<Iota>(ListIota(loop)))),
            SpellContinuation.Done
        )
        check(operateResult.newImage.stack.isEmpty())
        check(operateResult.newContinuation is SpellContinuation.NotDone)
        check((operateResult.newContinuation as SpellContinuation.NotDone).frame is ContinuationWhile)

        return "typed_codec=PASS spell_continuation=PASS continuation_iota_nbt_stream=PASS evaluate_true_false_empty=PASS operate=PASS"
    }

    private fun checkCoreActions(server: MinecraftServer): String {
        val level = server.overworld()
        // Keep entity-effect checks inside the permanently ticking spawn
        // chunk. Loading block data with getChunk() alone does not immediately
        // make a distant entity section visible to explosions in this tick.
        val base = entityProbeBase(server)
        level.getChunk(base)
        val fake = FakePlayerFactory.get(
            level,
            GameProfile(UUID(0L, 42L), "HexTweaksActionProbe")
        )
        fake.setPos(base.x + 0.5, base.y.toDouble(), base.z + 0.5)
        val env = StaffCastEnv(fake, InteractionHand.MAIN_HAND)

        val book = ItemStack(HexItems.SPELLBOOK.get())
        book.set(
            HexDataComponents.SPELLBOOK_PAGES.get(),
            mapOf(
                "1" to NullIota(),
                "2" to BooleanIota(true)
            )
        )
        book.set(HexDataComponents.SELECTED_SPELLBOOK_PAGE.get(), 1)
        fake.setItemInHand(InteractionHand.MAIN_HAND, book)
        OpPageFlip(true).execute(emptyList(), env)
        check(book.get(HexDataComponents.SELECTED_SPELLBOOK_PAGE.get()) == 2)
        OpPageFlip(false).execute(emptyList(), env)
        check(book.get(HexDataComponents.SELECTED_SPELLBOOK_PAGE.get()) == 1)

        val sourcePattern = HexPattern.fromAngles("qaq", HexDir.EAST)
        val infusion = OpEnlightenPattern().execute(
            listOf(PatternIota(sourcePattern)),
            env
        )
        check(infusion.cost == MediaConstants.DUST_UNIT * sourcePattern.angles.size)
        val infusedImage = infusion.effect.cast(env, CastingImage())
            ?: error("infusion did not return an updated casting image")
        val infusedPattern = (infusedImage.stack.single() as PatternIota).pattern
        check(
            infusedPattern == PatternRegistry.getGrandSpellPattern(
                fake.uuid,
                level.seed,
                sourcePattern
            )
        )

        val oversizedPattern = HexPattern(
            HexDir.EAST,
            MutableList(129) { HexAngle.FORWARD }
        )
        try {
            val oversized = OpEnlightenPattern().execute(
                listOf(PatternIota(oversizedPattern)),
                env
            )
            oversized.effect.cast(env, CastingImage())
            error("infusion accepted a Grand source pattern with 129 angles")
        } catch (_: MishapInvalidIota) {
            // Expected: invalid caster input must remain a controlled Hex mishap.
        } catch (unchecked: IllegalArgumentException) {
            throw IllegalStateException(
                "infusion leaked the Grand codec's unchecked 128-angle limit",
                unchecked
            )
        }

        try {
            OpMindflayPlus.execute(
                listOf(ListIota(emptyList()), DoubleIota(1.0)),
                env
            )
            error("Mindflay+ accepted an invalid target iota")
        } catch (mishap: MishapInvalidIota) {
            check(mishap.reverseIdx == 0) {
                "Mindflay+ highlighted reverse stack index ${mishap.reverseIdx}, expected 0"
            }
        }

        val bomb = OpBiggerBomb(false).execute(
            listOf(Vec3Iota(fake.position()), DoubleIota(12.0)),
            env
        )
        check(bomb.cost == (MediaConstants.DUST_UNIT * 30.125).toLong())
        check(bomb.particles.size == 1)

        val originalCasterPos = fake.position()
        val bombTarget = Villager(EntityType.VILLAGER, level)
        try {
            val bombPos = Vec3(base.x + 10.5, 220.0, base.z + 0.5)
            level.getChunk(BlockPos.containing(bombPos))
            fake.setPos(bombPos.x - 2.0, bombPos.y, bombPos.z)
            bombTarget.setPos(bombPos.x, bombPos.y, bombPos.z)
            check(env.canEditBlockAt(BlockPos.containing(bombPos))) {
                "Bigger explosion probe target was not editable"
            }
            check(level.addFreshEntity(bombTarget))
            check(level.getEntity(bombTarget.uuid) === bombTarget) {
                "Bigger explosion probe target was not indexed in the level"
            }
            val healthBefore = bombTarget.health
            OpBiggerBomb(false).execute(
                listOf(Vec3Iota(bombPos), DoubleIota(1.0)),
                env
            ).effect.cast(env)
            check(bombTarget.isDeadOrDying || bombTarget.health < healthBefore) {
                "Bigger explosion did not affect a target at its center: " +
                    "before=$healthBefore after=${bombTarget.health} " +
                    "removed=${bombTarget.isRemoved} invulnerable=${bombTarget.isInvulnerable} " +
                    "invulnerableTime=${bombTarget.invulnerableTime}"
            }
        } finally {
            bombTarget.discard()
            fake.setPos(originalCasterPos.x, originalCasterPos.y, originalCasterPos.z)
        }

        val supportPos = base.offset(3, 0, 0)
        val slatePos = supportPos.above()
        try {
            level.setBlockAndUpdate(supportPos, Blocks.STONE.defaultBlockState())
            val slate = HexBlocks.SLATE.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            level.setBlockAndUpdate(slatePos, slate)

            val constructor = CircleExecutionState::class.java.declaredConstructors
                .single { it.parameterCount == 11 }
            constructor.isAccessible = true
            val state = constructor.newInstance(
                base,
                Direction.NORTH,
                hashSetOf(slatePos),
                slatePos,
                Direction.NORTH,
                CastingImage(),
                fake.uuid,
                null,
                1L,
                slatePos,
                slatePos
            ) as CircleExecutionState
            val circleEnv = CircleCastEnv(level, state)

            val wave = OpWaveRefl.execute(emptyList(), circleEnv)
            check(wave.single() is Vec3Iota)
            check((wave.single() as Vec3Iota).vec3 == Vec3.atCenterOf(slatePos))

            val normal = OpSlateNormal.execute(emptyList(), circleEnv)
            check(normal.single() is Vec3Iota)
            check((normal.single() as Vec3Iota).vec3 == Vec3(0.0, 1.0, 0.0))
        } finally {
            level.setBlockAndUpdate(slatePos, Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(supportPos, Blocks.AIR.defaultBlockState())
        }

        val clericPos = base.offset(6, 0, 0)
        val cleric = at.petrak.hexcasting.common.blocks.circles.impetuses.BlockEntityRedstoneImpetus(
            clericPos,
            HexBlocks.IMPETUS_REDSTONE.get().defaultBlockState()
        )
        cleric.setLevel(level)
        cleric.postPrint(Component.literal("HexTweaks cleric probe"))
        check(cleric.displayMsg?.string == "HexTweaks cleric probe")

        val suicideCaster = object : FakePlayer(
            level,
            GameProfile(UUID(0L, 43L), "HexTweaksSuicideProbe")
        ) {
            override fun isInvulnerableTo(source: DamageSource): Boolean = false
        }
        suicideCaster.setPos(base.x + 0.5, base.y.toDouble(), base.z + 0.5)
        suicideCaster.isInvulnerable = true
        suicideCaster.abilities.invulnerable = true
        val suicideDamageType = level.registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
            .getHolderOrThrow(HexTweaksRegistry.SUS_DAMAGE_TYPE)
        val suicideDamage = DamageSource(suicideDamageType)
        check(suicideDamage.msgId == "hextweaks.death.sus")
        check(suicideDamage.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY))
        OpSuicide().execute(
            emptyList(),
            StaffCastEnv(suicideCaster, InteractionHand.MAIN_HAND)
        )
        check(suicideCaster.isDeadOrDying)

        return "page_flip=PASS infusion=PASS infusion_limit=PASS mindflay_invalid_target=PASS bomb_render=PASS bomb_world=PASS wave=PASS slate_normal=PASS cleric_post=PASS suicide=PASS"
    }

    private fun checkMoreIotasActions(server: MinecraftServer): String {
        val level = server.overworld()
        val base = entityProbeBase(server)
        level.getChunk(base)
        val fake = FakePlayerFactory.get(
            level,
            GameProfile(UUID(0L, 44L), "HexTweaksPlagueProbe")
        )
        fake.setPos(base.x + 0.5, base.y.toDouble(), base.z + 0.5)
        val env = StaffCastEnv(fake, InteractionHand.MAIN_HAND)
        val target = Villager(EntityType.VILLAGER, level)
        target.setPos(base.x + 1.5, base.y.toDouble(), base.z + 0.5)
        check(level.addFreshEntity(target))
        try {
            val targetIota = EntityIota(target)
            check(targetIota.getEntity(level) === target) {
                "newly added plague target was not resolvable by UUID"
            }
            try {
                OpEgyptianPlagues.execute(
                    listOf(
                        targetIota,
                        DoubleIota(1.0),
                        DoubleIota(2.0),
                        DoubleIota(1.0)
                    ),
                    env
                )
                error("Nadith accepted a non-string effect identifier")
            } catch (mishap: MishapInvalidIota) {
                check(mishap.reverseIdx == 2) {
                    "Nadith highlighted reverse stack index ${mishap.reverseIdx}, expected 2"
                }
            }
            val result = OpEgyptianPlagues.execute(
                listOf(
                    targetIota,
                    StringIota.make("minecraft:speed"),
                    DoubleIota(2.0),
                    DoubleIota(1.0)
                ),
                env
            )
            val image = result.effect.cast(env, CastingImage())
                ?: error("plague action did not return an updated casting image")
            check(image.opsConsumed == 1L)
            val effect = target.getEffect(MobEffects.MOVEMENT_SPEED)
                ?: error("plague action did not apply speed")
            check(effect.duration == 40)
            check(effect.amplifier == 1)
            check(result.cost > 0L)
        } finally {
            target.discard()
        }
        return "nadith_invalid_arg=PASS nadith_effect=PASS duration=40 amplifier=1"
    }

    private fun checkGrandSpellHandler(server: MinecraftServer): String {
        val level = server.overworld()
        val fake = FakePlayerFactory.getMinecraft(level)
        fake.setPos(0.5, 80.0, 0.5)
        val env = StaffCastEnv(fake, InteractionHand.MAIN_HAND)
        val factory = GrandSpellHandler.Factory()

        val alternateDimensionSeed = level.seed xor 0x5a17c0de5a17c0deL
        check(
            GrandSpellSeedCompat.select(alternateDimensionSeed, level.seed) == level.seed
        ) {
            "Grand encoder selected the current dimension seed instead of the canonical overworld seed"
        }

        val personalizedExplode = PatternRegistry.getGrandSpellPattern(
            fake.uuid,
            level.seed,
            HexActions.EXPLODE.value().prototype
        )
        val secondPass = PatternRegistry.getGrandSpellPattern(
            fake.uuid,
            level.seed,
            HexActions.EXPLODE.value().prototype
        )
        check(personalizedExplode == secondPass)
        val explodeHandler = factory.tryMatch(personalizedExplode, env)
        check(explodeHandler != null)
        check(explodeHandler.act() is OpBiggerBomb)

        val personalizedMindflay = PatternRegistry.getGrandSpellPattern(
            fake.uuid,
            level.seed,
            HexActions.BRAINSWEEP.value().prototype
        )
        check(factory.tryMatch(personalizedMindflay, env) != null)

        // The on-wire format must also round-trip source patterns which contain
        // BACK. Force the first two personalization bits to 0,1: the legacy
        // greedy decoder mistakes BACK + the next true-bit marker for a marker.
        val backAngleUuid = UUID((1L shl 62) xor level.seed, level.seed)
        val backAngleCaster = FakePlayer(
            level,
            GameProfile(backAngleUuid, "HexTweaksGrandBackProbe")
        )
        backAngleCaster.setPos(0.5, 80.0, 0.5)
        val backAngleSource = HexPattern(
            HexDir.EAST,
            mutableListOf(HexAngle.BACK, HexAngle.FORWARD)
        )
        PatternRegistry.registerGrandSpells(
            backAngleSource.angles,
            OpBiggerBomb(false),
            id("probe_grand_back_angle")
        )
        val personalizedBackAngle = PatternRegistry.getGrandSpellPattern(
            backAngleUuid,
            level.seed,
            backAngleSource
        )
        check(
            factory.tryMatch(
                personalizedBackAngle,
                StaffCastEnv(backAngleCaster, InteractionHand.MAIN_HAND)
            ) != null
        ) { "personalized Grand pattern containing BACK did not round-trip" }

        val random = java.util.Random(0x5eedd00dL)
        val allAngles = HexAngle.values()
        val allDirs = HexDir.values()
        repeat(512) { iteration ->
            val uuid = UUID(random.nextLong(), random.nextLong())
            val seed = random.nextLong()
            val source = HexPattern(
                allDirs[random.nextInt(allDirs.size)],
                MutableList(1 + random.nextInt(64)) {
                    allAngles[random.nextInt(allAngles.size)]
                }
            )
            val encoded = PatternRegistry.getGrandSpellPattern(uuid, seed, source)
            val decoded = PatternRegistry.decodeGrandSpellPattern(uuid, seed, encoded)
            check(decoded == source) {
                "Grand codec mismatch at deterministic fuzz iteration $iteration"
            }
        }

        val malformed = HexPattern(
            HexDir.EAST,
            mutableListOf(HexAngle.BACK, HexAngle.BACK)
        )
        check(factory.tryMatch(malformed, env) == null)

        val unpersonalized = HexPattern(
            personalizedExplode.startDir,
            personalizedExplode.angles.toMutableList().also {
                if (it.isNotEmpty()) {
                    it[0] = if (it[0] == HexAngle.LEFT) HexAngle.RIGHT else HexAngle.LEFT
                }
            }
        )
        check(factory.tryMatch(unpersonalized, env) == null)

        return "canonical_seed=PASS deterministic=PASS explode=PASS mindflay=PASS back_angle=PASS fuzz_512=PASS malformed=PASS wrong_bits=PASS"
    }

    private fun checkGiveGrandCommand(server: MinecraftServer): String {
        val level = server.overworld()
        val base = entityProbeBase(server).center
        var capturedStack: ItemStack? = null
        val fake = object : FakePlayer(
            level,
            GameProfile(UUID(0L, 43L), "HexTweaksCommandProbe")
        ) {
            override fun drop(stack: ItemStack, throwRandomly: Boolean): ItemEntity? {
                capturedStack = stack.copy()
                return super.drop(stack, throwRandomly)
            }
        }
        fake.setPos(base.x, base.y, base.z)
        val dispatcher = server.commands.dispatcher
        val root = dispatcher.root.getChild("hextweaks")
            ?: error("hextweaks command root is missing")
        val giveGrand = root.getChild("give-grand")
            ?: error("give-grand command is missing")
        check(!giveGrand.canUse(fake.createCommandSourceStack().withPermission(2)))

        fun expectSyntaxFailure(
            command: String,
            source: net.minecraft.commands.CommandSourceStack
        ) {
            try {
                dispatcher.execute(command, source)
                error("$command unexpectedly succeeded")
            } catch (_: com.mojang.brigadier.exceptions.CommandSyntaxException) {
            }
        }
        val playerSource = fake.createCommandSourceStack().withPermission(4)
        expectSyntaxFailure("hextweaks give-grand qxq", playerSource)
        expectSyntaxFailure("hextweaks give-grand ${"q".repeat(129)}", playerSource)
        expectSyntaxFailure(
            "hextweaks give-grand qaq",
            server.createCommandSourceStack().withPermission(4)
        )

        val bounds = AABB.ofSize(base, 8.0, 8.0, 8.0)
        val before = level.getEntitiesOfClass(ItemEntity::class.java, bounds)
            .map { it.uuid }
            .toSet()
        val result = dispatcher.execute(
            "hextweaks give-grand qaq",
            playerSource
        )
        check(result == 1)

        val drops = level.getEntitiesOfClass(ItemEntity::class.java, bounds)
            .filterNot { it.uuid in before }
        try {
            val stack = capturedStack
                ?: error("give-grand did not call ServerPlayer.drop")
            check(stack.`is`(HexItems.SCROLL_LARGE.get()))
            val written = HexItems.SCROLL_LARGE.get().readIota(stack) as? PatternIota
                ?: error("give-grand scroll did not contain a PatternIota")
            val source = PatternRegistry.patternAllowIllegal(HexDir.WEST, "qaq")
            val expected = PatternRegistry.getGrandSpellPattern(fake, level, source)
            check(written.pattern == expected)
            return "registered=PASS permission=PASS validation=PASS player_only=PASS execution=PASS scroll_component=PASS personalized_pattern=PASS"
        } finally {
            drops.forEach { it.discard() }
        }
    }

    private fun checkComputerCraft(server: MinecraftServer): String {
        check(ModList.get().isLoaded("computercraft"))
        val upgradeId = id("wand")
        val pocketRegistry = server.registryAccess().registryOrThrow(IPocketUpgrade.REGISTRY)
        val turtleRegistry = server.registryAccess().registryOrThrow(ITurtleUpgrade.REGISTRY)
        val pocket = pocketRegistry.get(upgradeId)
        val turtle = turtleRegistry.get(upgradeId)
        check(pocket != null)
        check(turtle != null)
        check(pocket.craftingItem.`is`(HexItems.STAFF_MINDSPLICE.get()))
        check(turtle.craftingItem.`is`(HexItems.STAFF_MINDSPLICE.get()))

        val level = server.overworld()
        val position = BlockPos(8, 80, 8)
        level.getChunk(position)
        val inventory = SimpleContainer(16)
        val turtleAccess = proxy<ITurtleAccess> { methodName, returnType ->
            when (methodName) {
                "getLevel" -> level
                "getPosition" -> position
                "getDirection" -> net.minecraft.core.Direction.NORTH
                "getSelectedSlot" -> 0
                "getColour" -> 0x5a31d6
                "getInventory" -> inventory
                "getOwningPlayer" -> GameProfile(UUID(0L, 41L), "HexTweaksProbe")
                "isFuelNeeded", "isRemoved" -> false
                "getFuelLevel", "getFuelLimit" -> 1000
                else -> defaultValue(returnType)
            }
        }
        val events = mutableListOf<Pair<String, List<Any?>>>()
        val computerAccess = Proxy.newProxyInstance(
            IComputerAccess::class.java.classLoader,
            arrayOf(IComputerAccess::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "toString" -> "HexTweaksProbeProxy<IComputerAccess>"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "getID" -> 41
                "getAttachmentName" -> "hextweaks_probe"
                "getAvailablePeripherals" ->
                    emptyMap<String, dan200.computercraft.api.peripheral.IPeripheral>()
                "queueEvent" -> {
                    val name = args?.getOrNull(0) as String
                    val payload = when (val raw = args.getOrNull(1)) {
                        is Array<*> -> raw.toList()
                        null -> emptyList()
                        else -> listOf(raw)
                    }
                    events += name to payload
                    null
                }
                else -> defaultValue(method.returnType)
            }
        } as IComputerAccess

        val pocketCarrier = ItemEntity(
            level,
            position.x + 0.5,
            position.y + 0.5,
            position.z + 0.5,
            ItemStack(Items.STONE)
        )
        val pocketHolder = PocketHolder.ItemEntityHolder(pocketCarrier)
        val standardPocket = PocketBrain(
            pocketHolder,
            null,
            -1,
            ServerComputer.properties(42, ComputerFamily.ADVANCED)
        )
        try {
            val standardPocketEnv =
                ComputerCastingEnv(null, standardPocket, level, computerAccess)
            check(standardPocketEnv.isEnlightened)
        } finally {
            standardPocket.computer().close()
        }

        val peripheral = WandPeripheral(Pair(turtleAccess, TurtleSide.LEFT), null)
        peripheral.attach(computerAccess)
        check(peripheral.isInit)

        peripheral.pushStack(7.5)
        check(peripheral.vm.image.stack.single() is DoubleIota)
        check((peripheral.popStack() as Number).toDouble() == 7.5)
        try {
            peripheral.popStack()
            error("empty peripheral stack did not throw")
        } catch (_: dan200.computercraft.api.lua.LuaException) {
        }

        peripheral.setStack(mapOf(2 to false, 1 to 3.0))
        check((peripheral.vm.image.stack[0] as DoubleIota).double == 3.0)
        check(!(peripheral.vm.image.stack[1] as BooleanIota).bool)
        check(peripheral.clearStack() == 2)

        val customIotas = listOf<Iota>(
            ByteIota((-42).toByte()),
            ByteArrayIota(byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)),
            RitualIota(id("restock"))
        )
        for (original in customIotas) {
            val lua = IotaSerdeRegistry.toLua(original, level)
            check(lua is Map<*, *>)
            val decoded = IotaSerdeRegistry.fromLua(lua, level)
                ?: error("Lua decode returned null for ${original.javaClass.simpleName}")
            check(Iota.tolerates(original, decoded))
        }

        val canonicalProfile = GameProfile(
            UUID.fromString("78e4af6d-6245-4e8c-8a99-dfd0a8cc2f74"),
            "HexTweaksCanonical"
        )
        server.profileCache?.add(canonicalProfile)
            ?: error("server profile cache is unavailable")
        val trueNameConfig = replaceConfigForProbe(
            HexTweaksConfig(allowUnsafeDeserialization = SecurityLevel.TRUENAME)
        )
        try {
            val decoded = IotaSerdeRegistry.fromLua(
                mapOf(
                    "iota\$serde" to "hextweaks:entity",
                    "uuid" to canonicalProfile.id.toString(),
                    "name" to "SpoofedLuaName"
                ),
                level
            ) as? EntityIota ?: error("TRUENAME entity decode returned null")
            check(decoded.entityName?.string == canonicalProfile.name) {
                "TRUENAME trusted the Lua display name: ${decoded.entityName?.string}"
            }
        } finally {
            replaceConfigForProbe(trueNameConfig)
        }

        if (ModList.get().isLoaded("hexal")) {
            val gate = GateIota.fromLegacyLocation(17, Vec3(1.25, 80.5, -4.75))
            val lua = IotaSerdeRegistry.toLua(gate, level)
            check(lua is Map<*, *>)
            check(lua["iota\$serde"] == "hextweaks:typed_nbt")
            val decoded = IotaSerdeRegistry.fromLua(lua, level)
                ?: error("generic typed-NBT Lua fallback failed for GateIota")
            check(Iota.tolerates(gate, decoded))
        }

        fun argumentsFor(iota: Iota): IArguments {
            val table = IotaSerdeRegistry.toLua(iota, level)
            return Proxy.newProxyInstance(
                IArguments::class.java.classLoader,
                arrayOf(IArguments::class.java)
            ) { proxy, method, args ->
                when (method.name) {
                    "toString" -> "HexTweaksProbeProxy<IArguments>"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    "count" -> 1
                    "getTable" -> {
                        check((args?.getOrNull(0) as Number).toInt() == 0)
                        table
                    }
                    else -> defaultValue(method.returnType)
                }
            } as IArguments
        }

        peripheral.runPattern(
            argumentsFor(PatternIota(HexActions.`CONST$TRUE`.value().prototype))
        )
        check((peripheral.vm.image.stack.singleOrNull() as? BooleanIota)?.bool == true)
        peripheral.runPattern(argumentsFor(PatternIota(HexActions.PRINT.value().prototype)))
        check(events.any { (name, payload) ->
            name == "reveal" &&
                payload.firstOrNull() == "hextweaks_probe" &&
                payload.drop(1).any { it.toString().contains("True", ignoreCase = true) }
        }) { "print action did not queue a reveal event: $events" }

        val actions = server.registryAccess().registryOrThrow(HexRegistries.ACTION)
        val trueActionId = actions.getKey(HexActions.`CONST$TRUE`.value())
            ?: error("const/true action has no registry id")
        val configBeforeBan = replaceConfigForProbe(
            HexTweaksConfig(computerBanList = listOf(trueActionId.toString()))
        )
        try {
            events.clear()
            peripheral.clearStack()
            peripheral.runPattern(
                argumentsFor(PatternIota(HexActions.`CONST$TRUE`.value().prototype))
            )
            val mishapEvent = events.singleOrNull { it.first == "mishap" }
            check(mishapEvent != null) {
                "computer ban list did not queue a mishap event: $events"
            }
            check(mishapEvent.second.size == 4)
            check(mishapEvent.second.all { it is String }) {
                "mishap event exposed non-Lua payloads: ${mishapEvent.second}"
            }
            check(mishapEvent.second[1] == MishapDisallowedSpell::class.java.name)
        } finally {
            replaceConfigForProbe(configBeforeBan)
        }

        val grandBanConfig = replaceConfigForProbe(
            HexTweaksConfig(computerBanList = listOf(id("explode").toString()))
        )
        try {
            events.clear()
            peripheral.clearStack()
            val personalizedExplode = PatternRegistry.getGrandSpellPattern(
                UUID(0L, 0L),
                level.seed,
                HexActions.EXPLODE.value().prototype
            )
            peripheral.runPattern(argumentsFor(PatternIota(personalizedExplode)))
            val mishapEvent = events.singleOrNull { it.first == "mishap" }
                ?: error("grand-spell ban did not queue a mishap event: $events")
            check(mishapEvent.second[1] == MishapDisallowedSpell::class.java.name) {
                "grand-spell ban produced the wrong mishap: ${mishapEvent.second}"
            }
        } finally {
            replaceConfigForProbe(grandBanConfig)
        }

        val specialHandlerBanConfig = replaceConfigForProbe(
            HexTweaksConfig(computerBanList = listOf(id("grand").toString()))
        )
        try {
            events.clear()
            peripheral.clearStack()
            val personalizedExplode = PatternRegistry.getGrandSpellPattern(
                UUID(0L, 0L),
                level.seed,
                HexActions.EXPLODE.value().prototype
            )
            peripheral.runPattern(argumentsFor(PatternIota(personalizedExplode)))
            val mishapEvent = events.singleOrNull { it.first == "mishap" }
                ?: error("special-handler ban did not queue a mishap event: $events")
            check(mishapEvent.second[1] == MishapDisallowedSpell::class.java.name) {
                "special-handler ban produced the wrong mishap: ${mishapEvent.second}"
            }
        } finally {
            replaceConfigForProbe(specialHandlerBanConfig)
        }

        val turtleEnv = peripheral.vm.env as ComputerCastingEnv
        check(turtleEnv.mishapSprayPos() == position.center)
        check(turtleEnv.isVecInRange(position.center))
        check(!turtleEnv.isVecInRange(position.center.add(10_000.0, 0.0, 0.0)))

        inventory.setItem(0, ItemStack(Items.STICK))
        check(
            turtleEnv.replaceItem(
                { it.`is`(Items.STICK) },
                ItemStack(Items.GOLD_INGOT),
                InteractionHand.MAIN_HAND
            )
        )
        check(inventory.getItem(0).`is`(Items.GOLD_INGOT))
        inventory.setItem(0, ItemStack(Items.STONE))
        inventory.setItem(7, ItemStack(Items.EMERALD))
        check(
            turtleEnv.replaceItem(
                { it.`is`(Items.EMERALD) },
                ItemStack(Items.COPPER_INGOT),
                null
            )
        )
        check(inventory.getItem(7).`is`(Items.COPPER_INGOT))
        check(
            !turtleEnv.replaceItem(
                { it.`is`(Items.COPPER_INGOT) },
                ItemStack(Items.IRON_INGOT),
                InteractionHand.OFF_HAND
            )
        )

        inventory.setItem(0, ItemStack(Items.STICK))
        val turtleDrops = captureJoinedItems {
            turtleEnv.mishapEnvironment.yeetHeldItemsTowards(position.center.add(2.0, 0.0, 0.0))
        }
        check(inventory.getItem(0).isEmpty)
        check(turtleDrops.any { it.item.`is`(Items.STICK) }) {
            "turtle mishap did not yeet the selected item: " +
                turtleDrops.map { "${it.item}@${it.position()}" }
        }
        turtleDrops.forEach { it.discard() }

        inventory.setItem(14, ItemStack(Items.DIAMOND))
        check(turtleEnv.queryForMatchingStack { it.`is`(Items.DIAMOND) } === inventory.getItem(14))
        val turtlePigment = turtleEnv.pigment
        check(
            (turtlePigment.colorProvider.getColor(0f, Vec3.ZERO) and 0x00ffffff) ==
                0x5a31d6
        )
        inventory.setItem(15, ItemStack(HexItems.AMETHYST_DUST.get()))
        check(turtleEnv.extractMedia(MediaConstants.DUST_UNIT, false) == 0L)
        check(inventory.getItem(15).isEmpty)

        val pocketPosition = Vec3(24.5, 90.0, 24.5)
        val pocketAccess = Proxy.newProxyInstance(
            IPocketAccess::class.java.classLoader,
            arrayOf(IPocketAccess::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "toString" -> "HexTweaksProbeProxy<IPocketAccess>"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "getLevel" -> level
                "getPosition" -> pocketPosition
                "getEntity" -> null
                "getColour" -> 0x2468ac
                else -> defaultValue(method.returnType)
            }
        } as IPocketAccess
        val lecternPeripheral = WandPeripheral(null, pocketAccess)
        lecternPeripheral.attach(computerAccess)
        try {
            val lecternEnv = lecternPeripheral.vm.env as ComputerCastingEnv
            check(lecternEnv.castingEntity == null)
            check(lecternEnv.mishapSprayPos() == pocketPosition)
            check(lecternEnv.isVecInRange(pocketPosition))
            check(
                lecternEnv.extractMedia(MediaConstants.DUST_UNIT, false) ==
                    MediaConstants.DUST_UNIT
            )
            val lecternPigment = lecternEnv.pigment
            check(
                (lecternPigment.colorProvider.getColor(0f, Vec3.ZERO) and 0x00ffffff) ==
                    0x2468ac
            )
        } finally {
            lecternPeripheral.detach(computerAccess)
        }

        val nonPlayerHost = Villager(EntityType.VILLAGER, level)
        nonPlayerHost.setPos(pocketPosition.x + 4.0, pocketPosition.y, pocketPosition.z)
        check(level.addFreshEntity(nonPlayerHost))
        val nonPlayerPocket = Proxy.newProxyInstance(
            IPocketAccess::class.java.classLoader,
            arrayOf(IPocketAccess::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "toString" -> "HexTweaksProbeProxy<NonPlayerPocket>"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "getLevel" -> level
                "getPosition" -> nonPlayerHost.position()
                "getEntity" -> nonPlayerHost
                "getColour" -> 0x654321
                else -> defaultValue(method.returnType)
            }
        } as IPocketAccess
        val nonPlayerEnv = ComputerCastingEnv(null, nonPlayerPocket, level, computerAccess)
        var nonPlayerDrops: List<ItemEntity> = emptyList()
        try {
            nonPlayerHost.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DIAMOND))
            check(
                nonPlayerEnv.replaceItem(
                    { it.`is`(Items.DIAMOND) },
                    ItemStack(Items.GOLD_INGOT),
                    InteractionHand.MAIN_HAND
                )
            )
            check(nonPlayerHost.mainHandItem.`is`(Items.GOLD_INGOT))

            val mishap = nonPlayerEnv.mishapEnvironment
            nonPlayerHost.airSupply = 100
            val healthBeforeDrown = nonPlayerHost.health
            mishap.drown()
            check(nonPlayerHost.airSupply == 0)
            check(nonPlayerHost.health < healthBeforeDrown)

            nonPlayerHost.health = nonPlayerHost.maxHealth
            val healthBeforeDamage = nonPlayerHost.health
            mishap.damage(0.25f)
            check(nonPlayerHost.health < healthBeforeDamage)
            mishap.blind(40)
            mishap.nauseate(50)
            check(nonPlayerHost.hasEffect(MobEffects.BLINDNESS))
            check(nonPlayerHost.hasEffect(MobEffects.CONFUSION))

            nonPlayerHost.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.STICK))
            nonPlayerHost.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.EMERALD))
            nonPlayerDrops = captureJoinedItems {
                mishap.yeetHeldItemsTowards(nonPlayerHost.position().add(2.0, 0.0, 0.0))
            }
            check(nonPlayerHost.mainHandItem.isEmpty)
            check(nonPlayerHost.offhandItem.isEmpty)
            check(nonPlayerDrops.any { it.item.`is`(Items.STICK) })
            check(nonPlayerDrops.any { it.item.`is`(Items.EMERALD) })
        } finally {
            nonPlayerDrops.forEach { it.discard() }
            nonPlayerHost.discard()
        }

        val playerHost = FakePlayer(
            level,
            GameProfile(UUID(0L, 42L), "HexTweaksInventoryProbe")
        )
        playerHost.inventory.clearContent()
        playerHost.inventory.selected = 4
        playerHost.inventory.setItem(4, ItemStack(Items.STICK))
        playerHost.inventory.setItem(5, ItemStack(Items.DIAMOND))
        playerHost.inventory.setItem(35, ItemStack(Items.COPPER_INGOT, 3))
        playerHost.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.EMERALD))
        val playerPocket = Proxy.newProxyInstance(
            IPocketAccess::class.java.classLoader,
            arrayOf(IPocketAccess::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "toString" -> "HexTweaksProbeProxy<PlayerPocket>"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "getLevel" -> level
                "getPosition" -> playerHost.position()
                "getEntity" -> playerHost
                "getColour" -> 0x123456
                else -> defaultValue(method.returnType)
            }
        } as IPocketAccess
        val playerEnv = ComputerCastingEnv(null, playerPocket, level, computerAccess)
        check(
            playerEnv.queryForMatchingStack { it.`is`(Items.EMERALD) } ===
                playerHost.getItemInHand(InteractionHand.OFF_HAND)
        )
        check(
            playerEnv.queryForMatchingStack { it.`is`(Items.DIAMOND) } ===
                playerHost.inventory.getItem(5)
        )
        val stackDiscoveryModeClass = Class.forName(
            "at.petrak.hexcasting.api.casting.eval.CastingEnvironment\$StackDiscoveryMode"
        )
        val extractionMode = stackDiscoveryModeClass.enumConstants
            .first { (it as Enum<*>).name == "EXTRACTION" }
        val usableStacksMethod = ComputerCastingEnv::class.java
            .getDeclaredMethod("getUsableStacks", stackDiscoveryModeClass)
            .also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val extractionStacks = usableStacksMethod.invoke(
            playerEnv,
            extractionMode
        ) as List<ItemStack>
        check(extractionStacks.first() === playerHost.inventory.getItem(35))
        check(extractionStacks.last() === playerHost.inventory.getSelected())
        check(
            playerEnv.getHeldItemToOperateOn { it.`is`(Items.EMERALD) }?.hand ==
                InteractionHand.OFF_HAND
        )
        check(
            playerEnv.replaceItem(
                { it.`is`(Items.EMERALD) },
                ItemStack(Items.GOLD_INGOT),
                InteractionHand.OFF_HAND
            )
        )
        check(playerHost.getItemInHand(InteractionHand.OFF_HAND).`is`(Items.GOLD_INGOT))

        val pattern = PatternIota(HexPattern.fromAngles("qaq", HexDir.EAST))
        peripheral.setRavenmind(IotaSerdeRegistry.toLua(pattern, level))
        val ravenmind = peripheral.getRavenmind()
        check(ravenmind is Map<*, *>)
        check(ravenmind["iota\$serde"] == "hextweaks:pattern")

        if (ModList.get().isLoaded("moreiotas")) {
            val typed = IotaTypeIota(HexTweaksIotaTypes.BYTE)
            peripheral.setRavenmind(IotaSerdeRegistry.toLua(typed, level))
            val typedRavenmind = peripheral.getRavenmind()
            check(typedRavenmind is Map<*, *>)
            check(typedRavenmind["iota\$serde"] == "hextweaks:iotatype")

            val ravenmindStack = ItemStack(Items.DIAMOND_SWORD)
            ravenmindStack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Ravenmind Registry Probe")
            )
            ravenmindStack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(CompoundTag().also { it.putInt("ravenmind_probe", 91) })
            )
            peripheral.setRavenmind(null)
            peripheral.setRavenmind(
                IotaSerdeRegistry.toLua(
                    ItemStackIota.createFiltered(ravenmindStack),
                    level
                )
            )
            val stackRavenmind = peripheral.getRavenmind()
                ?: error("registry-backed ItemStackIota vanished from ravenmind storage")
            val decodedStackRavenmind =
                IotaSerdeRegistry.fromLua(stackRavenmind, level) as? ItemStackIota
                    ?: error("registry-backed ravenmind did not decode as ItemStackIota")
            check(
                decodedStackRavenmind.itemStack
                    .get(DataComponents.CUSTOM_NAME)
                    ?.string == "Ravenmind Registry Probe"
            )
            check(
                decodedStackRavenmind.itemStack
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag()
                    .getInt("ravenmind_probe") == 91
            )

            events.clear()
            OpLackingWill.execute(emptyList(), turtleEnv)
            val reveal = events.singleOrNull { it.first == "reveal" }
                ?: error("Lacking Will did not queue a reveal event")
            check(reveal.second.drop(1).any { it.toString().isNotBlank() })
        }

        val originalConfig = replaceConfigForProbe(
            HexTweaksConfig(allowUnsafeDeserialization = SecurityLevel.UNSAFE)
        )
        try {
            val continuation = SpellContinuation.Done.pushFrame(
                ContinuationWhile(
                    TreeList.from(listOf(DoubleIota(9.0), BooleanIota(true)))
                )
            )
            peripheral.setRavenmind(
                IotaSerdeRegistry.toLua(ContinuationIota(continuation), level)
            )
            val continuationLua = peripheral.getRavenmind()
            check(continuationLua is Map<*, *>)
            check(continuationLua["iota\$serde"] == "hextweaks:continuation")
            val continuationRoundTrip =
                IotaSerdeRegistry.fromLua(continuationLua, level) as ContinuationIota
            check(continuationRoundTrip.continuation is SpellContinuation.NotDone)
            check(
                (continuationRoundTrip.continuation as SpellContinuation.NotDone).frame
                    is ContinuationWhile
            )
        } finally {
            replaceConfigForProbe(originalConfig)
        }

        peripheral.detach(computerAccess)
        check(!peripheral.isInit)
        return "dynamic_upgrades=PASS crafting_item=mindsplice peripheral_stack=PASS custom_iota_lua=PASS generic_iota_lua=PASS entity_truename=PASS run_pattern=PASS reveal_event=PASS lacking_will=PASS ban_list_mishap=PASS grand_ban_list=PASS special_handler_ban=PASS lua_event_payload=PASS media=PASS pigment=PASS standard_pocket_family=PASS turtle_inventory=PASS turtle_replace=PASS turtle_mishap=PASS player_inventory_parity=PASS nonplayer_pocket=PASS nonplayer_mishap=PASS lectern_pocket=PASS ravenmind=PASS registry_ravenmind=PASS continuation_ravenmind=PASS"
    }

    private inline fun <reified T> proxy(
        crossinline resolver: (String, Class<*>) -> Any?
    ): T {
        val type = T::class.java
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "toString" -> "HexTweaksProbeProxy<${type.simpleName}>"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> resolver(method.name, method.returnType)
            }
        } as T
    }

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Character.TYPE -> '\u0000'
        Optional::class.java -> Optional.empty<Any>()
        else -> null
    }

    private fun checkRestockRitual(server: MinecraftServer): String {
        val level = server.overworld()
        val base = entityProbeBase(server)
        level.getChunk(base)
        val fake = FakePlayerFactory.getMinecraft(level)
        fake.setPos(base.x + 0.5, base.y.toDouble(), base.z + 0.5)
        val env = StaffCastEnv(fake, InteractionHand.MAIN_HAND)

        val target = Villager(EntityType.VILLAGER, level)
        target.setPos(base.x + 1.5, base.y.toDouble(), base.z + 0.5)
        val offer = MerchantOffer(
            ItemCost(Items.EMERALD),
            ItemStack(Items.BREAD),
            0,
            10,
            0f
        )
        offer.setToOutOfStock()
        target.offers = MerchantOffers().also { it.add(offer) }

        val sacrifice = Villager(EntityType.VILLAGER, level)
        sacrifice.setPos(base.x + 2.5, base.y.toDouble(), base.z + 0.5)
        level.addFreshEntity(target)
        level.addFreshEntity(sacrifice)

        try {
            val result = MindflayRegistry.performMindflays(
                MindflayInput(
                    listOf(sacrifice),
                    at.petrak.hexcasting.api.casting.iota.EntityIota(target),
                    env
                )
            )
            check(result.first)
            check(result.second == id("restock"))
            check(offer.uses == 0)
            check(at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE.isBrainswept(sacrifice))
            return "id=${result.second} offer_uses=${offer.uses} sacrifice_brainswept=true"
        } finally {
            target.discard()
            sacrifice.discard()
        }
    }

    private fun checkMoreIotasSerde(server: MinecraftServer): String {
        val level = server.overworld()
        val originalConfig = replaceConfigForProbe(
            HexTweaksConfig(allowUnsafeDeserialization = SecurityLevel.UNSAFE)
        )
        try {
            fun roundTrip(original: Iota): Iota {
                val lua = IotaSerdeRegistry.toLua(original, level)
                return IotaSerdeRegistry.fromLua(lua, level)
                    ?: error("Lua decode returned null for ${original.javaClass.simpleName}")
            }

            val string = roundTrip(StringIota.makeUnchecked("hex tweaks")) as StringIota
            check(string.string == "hex tweaks")

            val iotaType = roundTrip(IotaTypeIota(HexTweaksIotaTypes.BYTE)) as IotaTypeIota
            check(iotaType.iotaType === HexTweaksIotaTypes.BYTE)

            val entityType = roundTrip(EntityTypeIota(EntityType.COW)) as EntityTypeIota
            check(entityType.entityType === EntityType.COW)

            val itemType = roundTrip(ItemTypeIota(Items.DIAMOND)) as ItemTypeIota
            check(itemType.item === Items.DIAMOND)

            val stack = ItemStack(Items.DIAMOND_SWORD, 1)
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("HexTweaks Probe"))
            stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(CompoundTag().also { it.putInt("probe", 73) })
            )
            val itemStack = roundTrip(ItemStackIota.createFiltered(stack)) as ItemStackIota
            check(itemStack.itemStack.get(DataComponents.CUSTOM_NAME)?.string == "HexTweaks Probe")
            check(
                itemStack.itemStack
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag()
                    .getInt("probe") == 73
            )

            val matrix = SimpleMatrix(2, 2)
            matrix.set(0, 1.25)
            matrix.set(1, -2.5)
            matrix.set(2, 3.75)
            matrix.set(3, 9.5)
            val decodedMatrix = (roundTrip(MatrixIota(matrix)) as MatrixIota).matrix
            check(decodedMatrix.numRows == 2 && decodedMatrix.numCols == 2)
            check(decodedMatrix.get(0) == 1.25)
            check(decodedMatrix.get(1) == -2.5)
            check(decodedMatrix.get(2) == 3.75)
            check(decodedMatrix.get(3) == 9.5)

            val negativeMatrix = mapOf(
                "iota\$serde" to "hextweaks:matrix",
                "row" to -1,
                "col" to 2,
                "matrix" to emptyMap<Double, Double>()
            )
            check(IotaSerdeRegistry.fromLua(negativeMatrix, level) == null) {
                "negative matrix dimensions must be rejected without allocating"
            }
            val oversizedMatrix = mapOf(
                "iota\$serde" to "hextweaks:matrix",
                "row" to MoreIotasConfig.maxMatrixSize.get() + 1,
                "col" to 1,
                "matrix" to emptyMap<Double, Double>()
            )
            check(IotaSerdeRegistry.fromLua(oversizedMatrix, level) == null) {
                "matrix dimensions above MoreIotas' configured limit must be rejected"
            }
            val overflowMatrix = mapOf(
                "iota\$serde" to "hextweaks:matrix",
                "row" to Int.MAX_VALUE,
                "col" to Int.MAX_VALUE,
                "matrix" to emptyMap<Double, Double>()
            )
            check(IotaSerdeRegistry.fromLua(overflowMatrix, level) == null) {
                "overflowing matrix dimensions must be rejected before multiplication"
            }

            val nested = ListIota(
                listOf(
                    DoubleIota(4.0),
                    NullIota(),
                    StringIota.makeUnchecked("nested")
                )
            )
            val decodedNested = roundTrip(nested) as ListIota
            check(decodedNested.list.toList().size == 3)
            check(decodedNested.list.toList()[1] is NullIota)

            val frame = ContinuationWhile(TreeList.from(listOf(DoubleIota(1.0))))
            val continuation = ContinuationIota(SpellContinuation.Done.pushFrame(frame))
            val decodedContinuation = roundTrip(continuation) as ContinuationIota
            check(decodedContinuation.continuation is SpellContinuation.NotDone)
            check(
                (decodedContinuation.continuation as SpellContinuation.NotDone).frame is ContinuationWhile
            )

            return "string,iotatype,entitytype,itemtype,itemstack_components,matrix,matrix_bounds,nested_null,continuation=PASS"
        } finally {
            replaceConfigForProbe(originalConfig)
        }
    }

    private fun replaceConfigForProbe(config: HexTweaksConfig): HexTweaksConfig {
        val field = HexTweaks::class.java.getDeclaredField("CONFIG")
        field.isAccessible = true
        val previous = field.get(null) as? HexTweaksConfig ?: HexTweaksConfig.DEFAULT
        field.set(null, config)
        return previous
    }

    private fun checkHexalRituals(server: MinecraftServer): String {
        val level = server.overworld()
        val base = entityProbeBase(server)
        level.getChunk(base)
        level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState())
        val fake = FakePlayerFactory.getMinecraft(level)
        fake.setPos(base.x + 0.5, base.y.toDouble(), base.z + 0.5)
        val env = StaffCastEnv(fake, InteractionHand.MAIN_HAND)

        val createSacrifices = (0 until 3).map { index ->
            Villager(EntityType.VILLAGER, level).also { villager ->
                villager.villagerData = villager.villagerData.setLevel(5)
                villager.setPos(base.x + index + 2.5, base.y.toDouble(), base.z + 0.5)
                level.addFreshEntity(villager)
            }
        }
        val wisp = WanderingWisp(level, base.center)
        level.addFreshEntity(wisp)

        val spawned = mutableListOf<WanderingWisp>()
        try {
            val create = MindflayRegistry.performMindflays(
                MindflayInput(
                    createSacrifices,
                    at.petrak.hexcasting.api.casting.iota.EntityIota(wisp),
                    env
                )
            )
            check(create.first && create.second == id("slipway/create"))
            check(level.getBlockState(base).block === HexalBlocks.SLIPWAY)
            check(wisp.isRemoved)
            check(createSacrifices.all {
                at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE.isBrainswept(it)
            })

            val burstSacrifice = Villager(EntityType.VILLAGER, level).also { villager ->
                villager.villagerData = villager.villagerData.setLevel(5)
                villager.setPos(base.x + 5.5, base.y.toDouble(), base.z + 0.5)
                level.addFreshEntity(villager)
            }
            try {
                val bounds = AABB.ofSize(base.center, 16.0, 16.0, 16.0)
                val before = level.getEntitiesOfClass(WanderingWisp::class.java, bounds)
                    .map { it.uuid }
                    .toSet()
                val burst = MindflayRegistry.performMindflays(
                    MindflayInput(
                        listOf(burstSacrifice),
                        Vec3Iota(base.center),
                        env
                    )
                )
                check(burst.first && burst.second == id("slipway/destroy"))
                check(level.getBlockState(base).isAir)
                spawned += level.getEntitiesOfClass(WanderingWisp::class.java, bounds)
                    .filterNot { it.uuid in before }
                check(spawned.size in 10..20)
                check(
                    at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE
                        .isBrainswept(burstSacrifice)
                )
                return "create=PASS burst=PASS spawned=${spawned.size}"
            } finally {
                burstSacrifice.discard()
            }
        } finally {
            createSacrifices.forEach { it.discard() }
            wisp.discard()
            spawned.forEach { it.discard() }
            level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState())
        }
    }
}
