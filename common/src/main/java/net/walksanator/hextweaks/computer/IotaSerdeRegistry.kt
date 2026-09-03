package net.walksanator.hextweaks.computer

import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.*
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.datafixers.util.Either
import com.mojang.serialization.DynamicOps

import dev.architectury.platform.Platform
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.Component
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import net.walksanator.hextweaks.HexTweaks
import net.walksanator.hextweaks.SecurityLevel
import net.walksanator.hextweaks.casting.HexTweaksIotaTypes
import net.walksanator.hextweaks.casting.PatternRegistry
import net.walksanator.hextweaks.casting.iota.ByteArrayIota
import net.walksanator.hextweaks.casting.iota.ByteIota
import net.walksanator.hextweaks.casting.iota.RitualIota
import net.walksanator.hextweaks.hexcompat.SpellContinuationCodecCompat
import org.ejml.simple.SimpleMatrix
import ram.talia.moreiotas.api.casting.iota.*
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes
import java.util.*
import kotlin.collections.HashMap


object IotaSerdeRegistry {
    private val DEFAULT_ID = modloc("typed_nbt")
    private val TypeToID = mutableMapOf<IotaType<*>, ResourceLocation>() //encode half
    private val IDToSerde = mutableMapOf<ResourceLocation, IotaSerde<*>>() //decode half
    private const val MORE_IOTAS_MATRIX_HARD_LIMIT = 512
    private val moreIotasMatrixLimit: Int by lazy {
        // MoreIotas exposes its configurable limit through a NeoForge-specific
        // field type. Reflect at this optional-integration boundary so common
        // code does not acquire a loader API dependency.
        runCatching {
            val configClass = Class.forName("ram.talia.moreiotas.MoreIotasConfig")
            val configValue = configClass.getField("maxMatrixSize").get(null)
            val configured = (
                configValue.javaClass.getMethod("get").invoke(configValue) as Number
            ).toInt()
            configured.coerceIn(0, MORE_IOTAS_MATRIX_HARD_LIMIT)
        }.getOrDefault(MORE_IOTAS_MATRIX_HARD_LIMIT)
    }

    private fun nbtOps(world: Level?): DynamicOps<Tag> =
        if (world == null) {
            NbtOps.INSTANCE
        } else {
            RegistryOps.create(NbtOps.INSTANCE, world.registryAccess())
        }

    private val DEFAULT = object : IotaSerde<Iota> {
        override fun serialize(input: Iota, world: Level?): Any {
            val encoded = IotaType.TYPED_CODEC
                .encodeStart(nbtOps(world), input)
                .result()
                .orElseThrow {
                    IllegalStateException("Unable to encode unregistered iota type ${input.type}")
                }
            return mutableMapOf<String, Any>(
                "typed_nbt" to encoded.toString()
            )
        }

        override fun deserialize(value: Map<*, *>, world: Level): Iota? {
            val encoded = value["typed_nbt"] as? String ?: return null
            val decoded = runCatching {
                IotaType.TYPED_CODEC
                    .parse(nbtOps(world), TagParser.parseTag(encoded))
                    .result()
                    .orElse(null)
            }.getOrNull() ?: return null

            // Known types must use their dedicated decoder. This prevents a
            // caller from bypassing the stricter entity/continuation policies
            // by relabelling their payload as the generic fallback.
            return decoded.takeIf { TypeToID[it.type] == null }
        }
    }

    fun <T: Iota> register(id: ResourceLocation, type: IotaType<T>, serde: IotaSerde<T>) {
        if (TypeToID[type] != null) {
            throw IllegalStateException("IotaType %s already registered with id %s, trying to overwrite with %s".format(
                type, TypeToID[type], id
            ))
        }
        TypeToID[type] = id
        IDToSerde[id] = serde
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Iota> toLua(iota: T): Any? {
        return toLua(iota, null)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Iota> toLua(iota: T, world: Level?): Any? {
        val id = TypeToID[iota.type] ?: DEFAULT_ID
        val serde = if (id == DEFAULT_ID) DEFAULT else IDToSerde[id]
        var value = (serde as? IotaSerde<T>)?.serialize(iota, world)
            ?: DEFAULT.serialize(iota, world)
        if (value is Map<*, *>) {
            value = value.toMutableMap()
            value["iota\$serde"] = id.toString()
        }
        return value
    }

    fun fromLua(data: Any?, world: Level): Iota? {
        return when (data) {
            is String -> if (Platform.isModLoaded("moreiotas")) {StringIota.make(data)} else {GarbageIota()}
            is Number -> DoubleIota(data.toDouble())
            is Boolean -> BooleanIota(data)
            is Map<*, *> -> {
                val type = data["iota\$serde"]
                if (type.toString() != "null" && type is String) {
                    val resloc = ResourceLocation.tryParse(type) ?: ResourceLocation.fromNamespaceAndPath("hextweaks", "invalid")
                    val serializer = if (resloc == DEFAULT_ID) DEFAULT else IDToSerde[resloc]
                    if (serializer == null) {
                        HexTweaks.LOGGER.warn("Unknown Lua iota serializer {}", resloc)
                        GarbageIota()
                    } else {
                        serializer.deserialize(data,world)
                    }
                } else {GarbageIota()}
            }
            else -> {
                if (data == null) {
                    NullIota()
                } else {
                    HexTweaks.LOGGER.warn("Unsupported Lua value class {}", data.javaClass.name)
                    GarbageIota()
                }
            }
        }
    }

    //HexCasting Iotas implementations
    init {
        register(modloc("list"), HexIotaTypes.LIST.get(), object : IotaSerde<ListIota> {
            override fun serialize(input: ListIota, world: Level?): Any {
                val tag = mutableMapOf<Any, Any?>()
                for ((i, iota) in input.list.withIndex()) {
                    tag[i+1] = toLua(iota, world)
                }
                return tag
            }

            override fun deserialize(value: Map<*, *>, world: Level): ListIota {
                val entries = value.entries
                    .mapNotNull { entry ->
                        (entry.key as? Number)?.toLong()?.let { it to entry.value }
                    }
                    .sortedBy { it.first }
                return ListIota(entries.map { fromLua(it.second, world) ?: GarbageIota() })
            }
        })
        register(modloc("double"), HexIotaTypes.DOUBLE.get(), object : IotaSerde<DoubleIota> {
            override fun serialize(input: DoubleIota, world: Level?): Any = input.double
            override fun deserialize(value: Map<*, *>, world: Level): DoubleIota? = null
        })
        register(modloc("boolean"), HexIotaTypes.BOOLEAN.get(), object : IotaSerde<BooleanIota> {
            override fun serialize(input: BooleanIota, world: Level?): Any = input.bool
            override fun deserialize(value: Map<*, *>, world: Level): BooleanIota = BooleanIota(value["value"] as? Boolean ?: false)
        })
        register(modloc("null"), HexIotaTypes.NULL.get(), object : IotaSerde<NullIota> {
            override fun serialize(input: NullIota, world: Level?): Any? = null
            override fun deserialize(value: Map<*, *>, world: Level): NullIota = NullIota()
        })
        register(modloc("garbage"), HexIotaTypes.GARBAGE.get(), object : IotaSerde<GarbageIota> {
            override fun serialize(input: GarbageIota, world: Level?): Any = mutableMapOf<Any, Any>()
            override fun deserialize(value: Map<*, *>, world: Level): GarbageIota = GarbageIota()
        })
        register(modloc("pattern"), HexIotaTypes.PATTERN.get(), object : IotaSerde<PatternIota> {
            override fun serialize(input: PatternIota, world: Level?): Any {
                val result = mutableMapOf<String,Any>()
                result["startDir"] = input.pattern.startDir.toString()
                result["angles"] = input.pattern.anglesSignature()
                return result
            }

            override fun deserialize(value: Map<*, *>, world: Level): PatternIota? {
                val angles = value["angles"] as? String ?: return null
                val startDir = value["startDir"] as? String ?: return null
                return runCatching {
                    PatternIota(
                        PatternRegistry.patternAllowIllegal(
                            HexDir.fromString(startDir),
                            angles
                        )
                    )
                }.getOrNull()
            }
        })
        register(modloc("vec3"), HexIotaTypes.VEC3.get(), object : IotaSerde<Vec3Iota> {
            override fun serialize(input: Vec3Iota, world: Level?): Any = mutableMapOf(
                "x" to input.vec3.x,
                "y" to input.vec3.y,
                "z" to input.vec3.z
            )

            override fun deserialize(value: Map<*, *>, world: Level): Vec3Iota? {
                val x = (value["x"] as? Number)?.toDouble()?: return null
                val y = (value["y"] as? Number)?.toDouble()?: return null
                val z = (value["z"] as? Number)?.toDouble()?: return null
                return Vec3Iota(Vec3(x,y,z))
            }
        })
        register(modloc("continuation"), HexIotaTypes.CONTINUATION.get(), object : IotaSerde<ContinuationIota> {
            override fun serialize(input: ContinuationIota, world: Level?): Any {
                val encoded = SpellContinuationCodecCompat.encode(
                    input.continuation,
                    nbtOps(world)
                )
                return mutableMapOf("continuation_stack" to encoded.toString())
            }

            override fun deserialize(value: Map<*, *>, world: Level): ContinuationIota? {
                if (net.walksanator.hextweaks.HexTweaks.getCONFIG().allowUnsafeDeserialization != SecurityLevel.UNSAFE) {return null}
                val encoded = value["continuation_stack"] as? String ?: return null
                return runCatching {
                    val tag = TagParser(StringReader(encoded)).readValue()
                    val stack = SpellContinuationCodecCompat.decode(tag, nbtOps(world))
                    ContinuationIota(stack)
                }.getOrNull()
            }
        })

        register(modloc("entity"), HexIotaTypes.ENTITY.get(), object : IotaSerde<EntityIota> {
            override fun serialize(input: EntityIota, world: Level?): Any {
                return  mutableMapOf(
                    "uuid" to input.entityId.toString(),
                    "name" to (input.entityName?.string ?: input.entityId.toString()),
                    "isPlayer" to input.isPlayer
                )
            }

            override fun deserialize(value: Map<*, *>, world: Level): EntityIota? {
                val security =
                    net.walksanator.hextweaks.HexTweaks.getCONFIG().allowUnsafeDeserialization
                if (security == SecurityLevel.RESTRICT) return null
                val uuid = runCatching {
                    UUID.fromString(value["uuid"] as? String ?: return null)
                }.getOrNull() ?: return null

                val canonicalProfile = if (security == SecurityLevel.TRUENAME) {
                    val profileCache = world.server?.profileCache ?: return null
                    profileCache.get(uuid).orElse(null) ?: return null
                } else {
                    null
                }

                // The original implementation deliberately discarded Lua's
                // caller-controlled display name and delegated reconstruction
                // to Hex Casting. Preserve that security boundary: use a live
                // entity when available, the authenticated profile name in
                // TRUENAME mode, and no display name for an unknown UNSAFE UUID.
                val liveEntity = (world as? ServerLevel)?.getEntity(uuid)
                if (liveEntity != null) return EntityIota(liveEntity)
                if (canonicalProfile != null) {
                    return EntityIota(
                        uuid,
                        Component.literal(canonicalProfile.name ?: uuid.toString()),
                        true
                    )
                }
                return EntityIota(uuid, null, value["isPlayer"] as? Boolean ?: true)

            }
        })
        register(modloc("byte"), HexTweaksIotaTypes.BYTE, object : IotaSerde<ByteIota> {
            override fun serialize(input: ByteIota, world: Level?): Any =
                mutableMapOf("value" to input.byte.toInt())

            override fun deserialize(value: Map<*, *>, world: Level): ByteIota? {
                val number = (value["value"] as? Number)?.toInt() ?: return null
                if (number !in Byte.MIN_VALUE..Byte.MAX_VALUE) return null
                return ByteIota(number.toByte())
            }
        })
        register(modloc("bytearray"), HexTweaksIotaTypes.BYTEARRAY, object : IotaSerde<ByteArrayIota> {
            override fun serialize(input: ByteArrayIota, world: Level?): Any =
                input.bytes.mapIndexed { index, byte -> (index + 1) to byte.toInt() }.toMap()

            override fun deserialize(value: Map<*, *>, world: Level): ByteArrayIota? {
                val numericEntries = value.entries.filter { it.key is Number }
                val ordered = numericEntries
                    .mapNotNull { entry ->
                        val index = (entry.key as? Number)?.toInt() ?: return@mapNotNull null
                        val byte = (entry.value as? Number)?.toInt() ?: return@mapNotNull null
                        if (index < 1 || byte !in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                            return@mapNotNull null
                        }
                        index to byte.toByte()
                    }
                    .sortedBy { it.first }
                if (ordered.size != numericEntries.size) return null
                if (ordered.map { it.first } != (1..ordered.size).toList()) return null
                return ByteArrayIota(ordered.map { it.second }.toByteArray())
            }
        })
        register(modloc("ritual"), HexTweaksIotaTypes.RITUAL, object : IotaSerde<RitualIota> {
            override fun serialize(input: RitualIota, world: Level?): Any =
                mutableMapOf("id" to input.ritualid.toString())

            override fun deserialize(value: Map<*, *>, world: Level): RitualIota? =
                ResourceLocation.tryParse(value["id"] as? String ?: return null)?.let(::RitualIota)
        })
    }

    //MoreIotas iotatypes
    init {
        if (Platform.isModLoaded("moreiotas")) {
            register(modloc("string"), MoreIotasIotaTypes.STRING, object : IotaSerde<StringIota> {
                override fun serialize(input: StringIota, world: Level?): Any = input.string
                override fun deserialize(value: Map<*, *>, world: Level): StringIota = throw IllegalStateException("This should have been handled by fromLua's when statement")
            })
            register(modloc("iotatype"), MoreIotasIotaTypes.IOTA_TYPE, object : IotaSerde<IotaTypeIota> {
                @Suppress("UNCHECKED_CAST")
                val REGISTRY: Registry<IotaType<*>> = BuiltInRegistries.REGISTRY.get(HexRegistries.IOTA_TYPE.location()) as? Registry<IotaType<*>>?: throw IllegalStateException("This should be loaded by now... this class isn't even touched until CC starts executing... why does the registry not exists")
                override fun serialize(input: IotaTypeIota, world: Level?): Any = mutableMapOf(
                    "id" to REGISTRY.getKey(input.iotaType).toString()
                )

                override fun deserialize(value: Map<*, *>, world: Level): IotaTypeIota? {
                    val id = ResourceLocation.tryParse(value["id"] as? String ?: return null) ?: return null
                    if (!REGISTRY.containsKey(id)) return null
                    return IotaTypeIota(REGISTRY.get(id) ?: return null)
                }
            })
            register(modloc("entitytype"), MoreIotasIotaTypes.ENTITY_TYPE, object : IotaSerde<EntityTypeIota> {
                val REGISTRY: Registry<EntityType<*>> = BuiltInRegistries.ENTITY_TYPE
                override fun serialize(input: EntityTypeIota, world: Level?): Any = mutableMapOf(
                    "id" to REGISTRY.getKey(input.entityType).toString()
                )

                override fun deserialize(value: Map<*, *>, world: Level): EntityTypeIota? {
                    val id = ResourceLocation.tryParse(value["id"] as? String ?: return null) ?: return null
                    if (!REGISTRY.containsKey(id)) return null
                    return EntityTypeIota(REGISTRY.get(id) ?: return null)
                }
            })
            register(modloc("itemstack"), MoreIotasIotaTypes.ITEM_STACK, object : IotaSerde<ItemStackIota> {
                val REGISTRY = BuiltInRegistries.ITEM
                override fun serialize(input: ItemStackIota, world: Level?): Any {
                    val stack = input.itemStack
                    val result = mutableMapOf<String, Any?>(
                        "id" to REGISTRY.getKey(stack.item).toString(),
                        "count" to stack.count,
                        "data" to stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                            .copyTag()
                            .takeIf { !it.isEmpty }
                            ?.toString()
                    )
                    if (world != null) {
                        ItemStack.CODEC
                            .encodeStart(nbtOps(world), stack)
                            .result()
                            .ifPresent { result["stack"] = it.toString() }
                    }
                    return result
                }

                override fun deserialize(value: Map<*, *>, world: Level): ItemStackIota? {
                    val encodedStack = value["stack"] as? String
                    if (encodedStack != null) {
                        val decoded = runCatching {
                            ItemStack.CODEC
                                .parse(nbtOps(world), TagParser.parseTag(encodedStack))
                                .result()
                                .orElse(null)
                        }.getOrNull()
                        if (decoded != null) {
                            return ItemStackIota.createFiltered(decoded)
                        }
                    }

                    val id = value["id"] as? String ?: return null
                    val resloc = ResourceLocation.tryParse(id)?: return null
                    if (!REGISTRY.containsKey(resloc)) {return null}
                    val stack = ItemStack(
                        REGISTRY.get(resloc),
                        (value["count"] as? Number)?.toInt()?: 1
                    )
                    value["data"].let {
                        if (it is String) {
                            try {
                                CustomData.set(DataComponents.CUSTOM_DATA, stack, TagParser.parseTag(it))
                            } catch(_: CommandSyntaxException) { }
                        }
                    }
                    return ItemStackIota.createFiltered(stack)
                }

            })
            register(modloc("itemtype"), MoreIotasIotaTypes.ITEM_TYPE, object : IotaSerde<ItemTypeIota> {
                val BLOCKS = BuiltInRegistries.BLOCK
                val ITEMS  = BuiltInRegistries.ITEM
                override fun serialize(input: ItemTypeIota, world: Level?): Any {
                    val either: Either<Item, Block> = input.either
                    val oleft = either.left()
                    val map = mutableMapOf<String,Any?>()
                    if (oleft.isPresent) {
                        val left = oleft.get()
                        map["type"] = "item"
                        map["id"] = ITEMS.getKey(left).toString()
                    } else {
                        val right = either.right().get()
                        map["type"] = "block"
                        map["id"] = BLOCKS.getKey(right).toString()
                    }
                    return map
                }

                override fun deserialize(value: Map<*, *>, world: Level): ItemTypeIota? {
                    val type = value["type"] as? String ?: return null
                    val id = ResourceLocation.tryParse(value["id"] as? String ?: return null) ?: return null
                    return when (type) {
                        "item" -> {
                            if (!ITEMS.containsKey(id)) return null
                            ItemTypeIota(ITEMS.get(id))
                        }
                        "block" -> {
                            if (!BLOCKS.containsKey(id)) return null
                            ItemTypeIota(BLOCKS.get(id))
                        }
                        else -> null
                    }
                }

            })
            register(modloc("matrix"), MoreIotasIotaTypes.MATRIX, object : IotaSerde<MatrixIota> {
                override fun serialize(input: MatrixIota, world: Level?): Any {
                    val matrix = input.matrix
                    val matrixTable: MutableMap<String, Any> = HashMap()
                    matrixTable["col"] = matrix.numCols
                    matrixTable["row"] = matrix.numRows
                    val matrixData: MutableMap<Double, Any> = HashMap()
                    for (i in 1..(matrix.numCols * matrix.numRows)) {
                        matrixData[i.toDouble()] = matrix.get(i - 1)
                    }
                    matrixTable["matrix"] = matrixData
                    return matrixTable
                }

                override fun deserialize(value: Map<*, *>, world: Level): MatrixIota? {
                    val col = (value["col"] as? Number)?.toInt()?: return null
                    val row = (value["row"] as? Number)?.toInt()?: return null
                    val matrixTable = value["matrix"] as? Map<*, *> ?: return null
                    val maxSize = moreIotasMatrixLimit
                    if (row < 0 || col < 0 || row > maxSize || col > maxSize) {
                        return null
                    }

                    val matrix = SimpleMatrix(row, col)
                    val cellCount = row * col
                    // Ignore missing/extra cells and leave missing values at zero.
                    for ((rawIndex, rawCell) in matrixTable) {
                        val index = (rawIndex as? Number)?.toInt() ?: continue
                        val cell = rawCell as? Number ?: continue
                        if (index in 1..cellCount) {
                            matrix.set(index - 1, cell.toDouble())
                        }
                    }
                    return try {
                        MatrixIota(matrix)
                    } catch (e: MishapInvalidIota) {
                        null
                    }
                }

            })
        }
    }

}

private fun modloc(string: String) = ResourceLocation.fromNamespaceAndPath(net.walksanator.hextweaks.HexTweaks.MOD_ID, string)

/**
 * this is an interface that controls Iota Serialization (to lua) and deserialization (from lua)
 */
interface IotaSerde<T: Iota> {
    /**
     * WARNING! although the output if of "Any" if it is anything other than Map you will need to mixin to fromLua to make it work (and various Cobalt functions to make it fix)
     * @param input the Iota input to be serialized
     * @return the value sent to lua
     */
    fun serialize(input: T, world: Level?): Any?
    fun deserialize(value: Map<*, *>, world: Level): T?
}
