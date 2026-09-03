package net.walksanator.hextweaks.computer

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.utils.TreeList
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import net.minecraft.nbt.NbtOps
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.pocket.IPocketAccess
import dan200.computercraft.api.turtle.ITurtleAccess
import dan200.computercraft.api.turtle.TurtleSide
import net.minecraft.server.level.ServerLevel
import net.walksanator.hextweaks.casting.environment.ComputerCastingEnv

class WandPeripheral(val turtleData: Pair<ITurtleAccess,TurtleSide>?, val pocketData: IPocketAccess?) : IPeripheral {
    lateinit var vm: CastingVM
    var isInit = false


    override fun attach(computer: IComputerAccess?) {
        vm = CastingVM(CastingImage(),
            ComputerCastingEnv(
                turtleData,
                pocketData,
                getWorld(),
                computer!!
            )
        )
        isInit = true
    }

    override fun detach(computer: IComputerAccess?) {
        isInit = false
    }


    private fun getWorld(): ServerLevel {
        return if (turtleData==null) {
            pocketData!!.level
        } else {
            turtleData.first.level
        } as ServerLevel
    }

    @Suppress("CovariantEquals")
    override fun equals(other: IPeripheral?): Boolean = other is WandPeripheral

    override fun getType(): String = "wand"

    @LuaFunction
    fun getStack(): MethodResult {
        val world = getWorld()
        return MethodResult.of(vm.image.stack.map { IotaSerdeRegistry.toLua(it, world) })
    }

    @LuaFunction
    fun pushStack(obj: Any?) {
        val iota = IotaSerdeRegistry.fromLua(obj, getWorld()) ?: GarbageIota()
        vm.image = vm.image.copy(stack = vm.image.stack.appended(iota))
    }

    @LuaFunction
    fun popStack(): Any? {
        if (vm.image.stack.isEmpty()) {
            throw LuaException("Stack is empty")
        }
        val iota = vm.image.stack.last()
        vm.image = vm.image.copy(stack = vm.image.stack.init())
        return IotaSerdeRegistry.toLua(iota, getWorld())
    }

    @LuaFunction
    fun clearStack(): Int {
        val size = vm.image.stack.size
        vm.image = vm.image.copy(stack = TreeList.empty())
        return size
    }


    @Suppress("UNCHECKED_CAST")
    @LuaFunction
    fun setStack(stack: Map<*,*>) {
        val world = getWorld()
        val iotas = (stack.filter { it.key is Number } as Map<Number,Any>).toSortedMap(compareBy { it.toLong() }).map {
            IotaSerdeRegistry.fromLua(it.value, world)?: GarbageIota()
        }
        vm.image = vm.image.copy(stack = TreeList.from(iotas))
    }

    @LuaFunction
    fun enlightened(): Boolean = vm.env.isEnlightened

    @LuaFunction
    fun getRavenmind(): Any? {
        val nbt = vm.image.userData.get(HexAPI.RAVENMIND_USERDATA) ?: return null
        val iota = IotaType.TYPED_CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(null)
        return iota?.let { IotaSerdeRegistry.toLua(it, getWorld()) }
    }

    @LuaFunction
    fun setRavenmind(iota: Any?) {
        val newLocal = IotaSerdeRegistry.fromLua(iota, getWorld())
        if ((newLocal?.type ?: HexIotaTypes.NULL.get()) == HexIotaTypes.NULL.get())
            vm.image.userData.remove(HexAPI.RAVENMIND_USERDATA)
        else
            IotaType.TYPED_CODEC.encodeStart(NbtOps.INSTANCE, newLocal).result().ifPresent {
                vm.image.userData.put(HexAPI.RAVENMIND_USERDATA, it)
            }
    }

    @LuaFunction(mainThread = true)
    fun runPattern(args: IArguments) {
        val iota = when (args.count()) {
            0 -> PatternIota(HexActions.EVAL.value().prototype)
            1 -> {
                val obj = args.getTable(0)
                IotaSerdeRegistry.fromLua(obj,getWorld())?: throw LuaException("Unable to convert input to Iota")
            }
            2 -> PatternIota(HexPattern.fromAngles(args.getString(1), HexDir.fromString(args.getString(0))))
            else -> GarbageIota()
        }
        val world = getWorld()
        if (vm.env.world != world) {
            vm = CastingVM(vm.image,
                ComputerCastingEnv(
                    vm.env as net.walksanator.hextweaks.casting.environment.ComputerCastingEnv,
                    world
                )
            )
        }
        vm.queueExecuteAndWrapIota(iota,getWorld())
    }

}
