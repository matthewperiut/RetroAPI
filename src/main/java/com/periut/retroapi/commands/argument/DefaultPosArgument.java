package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;

/** Three world-space coordinates, each optionally relative to the source. */
public record DefaultPosArgument(CoordinateArgument x, CoordinateArgument y, CoordinateArgument z) implements PosArgument {
    @Override
    public Position toAbsolutePos(final RetroCommandSource source) {
        final Position base = source.getPosition();
        return new Position(x.toAbsolute(base.x()), y.toAbsolute(base.y()), z.toAbsolute(base.z()));
    }

    @Override
    public boolean isRelative() {
        return x.relative() || y.relative() || z.relative();
    }

    public static DefaultPosArgument parse(final StringReader reader, final boolean centerIntegers) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final CoordinateArgument x = CoordinateArgument.parse(reader, centerIntegers);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
        }
        reader.skip();
        final CoordinateArgument y = CoordinateArgument.parse(reader, false);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
        }
        reader.skip();
        final CoordinateArgument z = CoordinateArgument.parse(reader, centerIntegers);
        return new DefaultPosArgument(x, y, z);
    }
}
