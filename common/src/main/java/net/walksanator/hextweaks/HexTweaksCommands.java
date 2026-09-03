package net.walksanator.hextweaks;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.common.lib.HexItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.walksanator.hextweaks.casting.PatternRegistry;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class HexTweaksCommands {
    private static final SimpleCommandExceptionType INVALID_ANGLES =
            new SimpleCommandExceptionType(Component.translatable(
                    "commands.hextweaks.give_grand.invalid_angles"
            ));
    private static final SimpleCommandExceptionType TOO_MANY_ANGLES =
            new SimpleCommandExceptionType(Component.translatable(
                    "commands.hextweaks.give_grand.too_many_angles"
            ));

    static void register(CommandDispatcher<CommandSourceStack> it) {
        LiteralArgumentBuilder<CommandSourceStack> main = literal("hextweaks");

        getAnglesig(main);

        it.register(main);
    }

    private static void getAnglesig(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.then(literal("give-grand").requires(i -> i.hasPermission(3))
                .then(argument("anglesig",StringArgumentType.string())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String sig = ctx.getArgument("anglesig",String.class);
                    if (sig.length() > PatternRegistry.GRAND_SPELL_BIT_COUNT) {
                        throw TOO_MANY_ANGLES.create();
                    }
                    HexPattern pat;
                    try {
                        pat = PatternRegistry.INSTANCE.patternAllowIllegal(HexDir.WEST,sig);
                    } catch (IllegalArgumentException exception) {
                        throw INVALID_ANGLES.create();
                    }
                    HexPattern real = PatternRegistry.INSTANCE.getGrandSpellPattern(
                            player,
                            ctx.getSource().getLevel(),
                            pat
                    );
                    ctx.getSource().sendSystemMessage(Component.literal("%s".formatted(real)));

                    var stack = new ItemStack(HexItems.SCROLL_LARGE.get());
                    HexItems.SCROLL_LARGE.get().writeDatum(stack, new PatternIota(real));
                    var stackEntity = player.drop(stack, false);
                    if (stackEntity != null) {
                        stackEntity.setNoPickUpDelay();
                        stackEntity.setThrower(player);
                    }
                    return 1;
                })
        ));
    }


}
