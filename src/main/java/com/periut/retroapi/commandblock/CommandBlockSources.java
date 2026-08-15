package com.periut.retroapi.commandblock;

import com.periut.retroapi.commands.FeedbackSink;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;
import net.minecraft.world.World;

/**
 * The command source a command block runs as.
 *
 * <p>Modern's {@code BaseCommandBlock} names itself {@code @}, sits at the block's own position,
 * carries permission level 2 and has no entity - all four are what make {@code @p} mean "nearest to
 * the block" and {@code ~ ~ ~} mean the block's own coordinates.
 */
public final class CommandBlockSources {
    private CommandBlockSources() {
    }

    /** Modern's {@code BaseCommandBlock.DEFAULT_NAME}. */
    public static final String NAME = "@";

    public static RetroCommandSource forBlock(final World world, final Position position) {
        return new RetroCommandSource(
            FeedbackSink.SILENT,
            null,
            world,
            position,
            0.0F,
            0.0F,
            NAME,
            RetroCommandSource.LEVEL_MODERATOR,
            null,
            false);
    }
}
