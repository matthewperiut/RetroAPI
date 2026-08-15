package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.periut.retroapi.text.Text;

/** One coordinate of a position: either absolute, or an offset from the source's own. */
public record CoordinateArgument(boolean relative, double value) {
    public static final SimpleCommandExceptionType MISSING_COORDINATE = new SimpleCommandExceptionType(
        Text.literal("Incomplete (expected 3 coordinates)"));
    public static final SimpleCommandExceptionType MIXED_COORDINATE = new SimpleCommandExceptionType(
        Text.literal("Cannot mix world & local coordinates (everything must either use ^ or not)"));

    public double toAbsolute(final double base) {
        return relative ? base + value : value;
    }

    /**
     * @param centerIntegers whether a whole absolute number means the middle of that block, which is
     *                       what teleporting to {@code 10} rather than {@code 10.5} should do
     */
    public static CoordinateArgument parse(final StringReader reader, final boolean centerIntegers) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            throw MIXED_COORDINATE.createWithContext(reader);
        }
        if (!reader.canRead()) {
            throw MISSING_COORDINATE.createWithContext(reader);
        }

        if (reader.peek() == '~') {
            reader.skip();
            // A bare '~' means "no offset", so only read a number when one is actually there.
            final double offset = readOptionalDouble(reader);
            return new CoordinateArgument(true, offset);
        }

        final int start = reader.getCursor();
        final double value = reader.readDouble();
        final boolean whole = reader.getString().substring(start, reader.getCursor()).indexOf('.') < 0;
        return new CoordinateArgument(false, centerIntegers && whole ? value + 0.5 : value);
    }

    private static double readOptionalDouble(final StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead() || reader.peek() == ' ') {
            return 0.0;
        }
        return reader.readDouble();
    }
}
