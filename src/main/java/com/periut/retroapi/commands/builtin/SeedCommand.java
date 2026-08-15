package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.HoverEvent;
import com.periut.retroapi.text.Text;
import net.minecraft.world.World;

import static com.periut.retroapi.commands.RetroCommandManager.literal;

/** {@code /seed}, with modern's click-to-copy on the number. */
public final class SeedCommand {
    private SeedCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("seed")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final World world = source.getWorld();
                if (world == null) {
                    throw RetroCommandSource.REQUIRES_PLAYER.create();
                }

                final long seed = world.getSeed();
                source.sendFeedback(Text.literal("Seed: ").append(
                    Text.literal("[" + seed + "]")
                        .formatted(Formatting.GREEN)
                        .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(seed)))
                            .withHoverEvent(HoverEvent.showText(Text.literal("Click to copy to clipboard"))))));
                return (int) seed;
            }));
    }
}
