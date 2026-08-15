package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;

/**
 * {@code ^left ^up ^forward} - a position in the source's own frame of reference.
 *
 * <p>The basis is built from the source's yaw and pitch the same way modern Minecraft builds it, so
 * {@code ^ ^ ^5} is five blocks along the line of sight regardless of which way that points.
 */
public record LookingPosArgument(double left, double up, double forward) implements PosArgument {
    @Override
    public Position toAbsolutePos(final RetroCommandSource source) {
        final Position base = source.getPosition();

        final float yawRadians = (float) Math.toRadians(source.getYaw() + 90.0f);
        final float pitchRadians = (float) Math.toRadians(-source.getPitch());
        final float rollRadians = (float) Math.toRadians(-source.getPitch() + 90.0f);

        final double forwardX = Math.cos(yawRadians) * Math.cos(pitchRadians);
        final double forwardY = Math.sin(pitchRadians);
        final double forwardZ = Math.sin(yawRadians) * Math.cos(pitchRadians);

        final double upX = Math.cos(yawRadians) * Math.cos(rollRadians);
        final double upY = Math.sin(rollRadians);
        final double upZ = Math.sin(yawRadians) * Math.cos(rollRadians);

        // Left is the cross product of forward and up, which keeps the basis right-handed.
        final double leftX = forwardY * upZ - forwardZ * upY;
        final double leftY = forwardZ * upX - forwardX * upZ;
        final double leftZ = forwardX * upY - forwardY * upX;

        return new Position(
            base.x() + leftX * left + upX * up + forwardX * forward,
            base.y() + leftY * left + upY * up + forwardY * forward,
            base.z() + leftZ * left + upZ * up + forwardZ * forward);
    }

    @Override
    public boolean isRelative() {
        return true;
    }

    public static LookingPosArgument parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final double left = readCoordinate(reader);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
        }
        reader.skip();
        final double up = readCoordinate(reader);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
        }
        reader.skip();
        final double forward = readCoordinate(reader);
        return new LookingPosArgument(left, up, forward);
    }

    private static double readCoordinate(final StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
        }
        reader.expect('^');
        if (!reader.canRead() || reader.peek() == ' ') {
            return 0.0;
        }
        return reader.readDouble();
    }
}
