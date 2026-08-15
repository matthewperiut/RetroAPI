package com.periut.retroapi.commands.argument;

import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;

/** A position that still needs a source to resolve against, because of {@code ~} and {@code ^}. */
public interface PosArgument {
    Position toAbsolutePos(RetroCommandSource source);

    default Position toAbsoluteBlockPos(final RetroCommandSource source) {
        final Position pos = toAbsolutePos(source);
        return new Position(Math.floor(pos.x()), Math.floor(pos.y()), Math.floor(pos.z()));
    }

    boolean isRelative();
}
