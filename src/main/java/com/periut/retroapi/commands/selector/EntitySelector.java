package com.periut.retroapi.commands.selector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A resolved {@code @a}-style selector, or a bare player name.
 *
 * <p>Resolution happens per execution rather than at parse time, so {@code @e[distance=..5]} means
 * "within five blocks of whoever runs it, now".
 */
public class EntitySelector {
    public static final SimpleCommandExceptionType NOT_FOUND_ENTITY = new SimpleCommandExceptionType(
        Text.literal("No entity was found"));
    public static final SimpleCommandExceptionType NOT_FOUND_PLAYER = new SimpleCommandExceptionType(
        Text.literal("No player was found"));
    public static final SimpleCommandExceptionType TOO_MANY_ENTITIES = new SimpleCommandExceptionType(
        Text.literal("Only one entity is allowed, but the provided selector allows more than one"));
    public static final SimpleCommandExceptionType TOO_MANY_PLAYERS = new SimpleCommandExceptionType(
        Text.literal("Only one player is allowed, but the provided selector allows more than one"));
    public static final SimpleCommandExceptionType PLAYER_SELECTOR_HAS_ENTITIES = new SimpleCommandExceptionType(
        Text.literal("Only players may be affected by this command, but the provided selector includes entities"));

    private final int limit;
    private final boolean playersOnly;
    private final boolean senderOnly;
    private final String playerName;
    private final Predicate<Entity> predicate;
    private final EntitySelectorReader.Sort sort;
    private final Double offsetX;
    private final Double offsetY;
    private final Double offsetZ;
    private final Double minDistance;
    private final Double maxDistance;
    private final Double boxX;
    private final Double boxY;
    private final Double boxZ;

    EntitySelector(final int limit, final boolean playersOnly, final boolean senderOnly, final String playerName,
                   final Predicate<Entity> predicate, final EntitySelectorReader.Sort sort,
                   final Double offsetX, final Double offsetY, final Double offsetZ,
                   final Double minDistance, final Double maxDistance,
                   final Double boxX, final Double boxY, final Double boxZ) {
        this.limit = limit;
        this.playersOnly = playersOnly;
        this.senderOnly = senderOnly;
        this.playerName = playerName;
        this.predicate = predicate;
        this.sort = sort;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.boxX = boxX;
        this.boxY = boxY;
        this.boxZ = boxZ;
    }

    /** A selector that always resolves to exactly the named player - what a bare name parses to. */
    public static EntitySelector ofName(final String name) {
        return new EntitySelector(1, true, false, name, entity -> true, EntitySelectorReader.Sort.ARBITRARY,
            null, null, null, null, null, null, null, null);
    }

    public boolean isSingleTarget() {
        return limit == 1;
    }

    public boolean includesNonPlayers() {
        return !playersOnly;
    }

    public List<? extends Entity> getEntities(final RetroCommandSource source) throws CommandSyntaxException {
        final List<Entity> matched = resolve(source);
        if (matched.isEmpty()) {
            throw playersOnly ? NOT_FOUND_PLAYER.create() : NOT_FOUND_ENTITY.create();
        }
        return matched;
    }

    public Entity getEntity(final RetroCommandSource source) throws CommandSyntaxException {
        final List<Entity> matched = resolve(source);
        if (matched.isEmpty()) {
            throw NOT_FOUND_ENTITY.create();
        }
        if (matched.size() > 1) {
            throw TOO_MANY_ENTITIES.create();
        }
        return matched.get(0);
    }

    public List<PlayerEntity> getPlayers(final RetroCommandSource source) throws CommandSyntaxException {
        final List<Entity> matched = resolve(source);
        final List<PlayerEntity> players = new ArrayList<>();
        for (final Entity entity : matched) {
            if (!(entity instanceof PlayerEntity)) {
                throw PLAYER_SELECTOR_HAS_ENTITIES.create();
            }
            players.add((PlayerEntity) entity);
        }
        if (players.isEmpty()) {
            throw NOT_FOUND_PLAYER.create();
        }
        return players;
    }

    public PlayerEntity getPlayer(final RetroCommandSource source) throws CommandSyntaxException {
        final List<PlayerEntity> players = getPlayers(source);
        if (players.size() > 1) {
            throw TOO_MANY_PLAYERS.create();
        }
        return players.get(0);
    }

    private List<Entity> resolve(final RetroCommandSource source) {
        if (playerName != null) {
            for (final PlayerEntity player : source.getPlayers()) {
                if (player.name.equals(playerName)) {
                    return Collections.singletonList(player);
                }
            }
            return Collections.emptyList();
        }

        if (senderOnly) {
            final Entity self = source.getEntity();
            return self != null && matches(self, source) ? Collections.singletonList(self) : Collections.emptyList();
        }

        final List<? extends Entity> candidates = playersOnly ? source.getPlayers() : source.getWorldEntities();
        final List<Entity> matched = new ArrayList<>();
        for (final Entity entity : candidates) {
            if (matches(entity, source)) {
                matched.add(entity);
            }
        }

        sort(matched, origin(source));

        return matched.size() > limit ? new ArrayList<>(matched.subList(0, limit)) : matched;
    }

    private Position origin(final RetroCommandSource source) {
        final Position base = source.getPosition();
        return new Position(
            offsetX == null ? base.x() : offsetX,
            offsetY == null ? base.y() : offsetY,
            offsetZ == null ? base.z() : offsetZ);
    }

    private boolean matches(final Entity entity, final RetroCommandSource source) {
        if (entity.dead || !predicate.test(entity)) {
            return false;
        }

        final Position origin = origin(source);

        if (minDistance != null || maxDistance != null) {
            final double distance = Math.sqrt(origin.squaredDistanceTo(entity.x, entity.y, entity.z));
            if (minDistance != null && distance < minDistance) {
                return false;
            }
            if (maxDistance != null && distance > maxDistance) {
                return false;
            }
        }

        // dx/dy/dz describe a box anchored at the origin, with negative sizes extending the other way.
        if (boxX != null && outsideBox(entity.x, origin.x(), boxX)) {
            return false;
        }
        if (boxY != null && outsideBox(entity.y, origin.y(), boxY)) {
            return false;
        }
        return boxZ == null || !outsideBox(entity.z, origin.z(), boxZ);
    }

    private static boolean outsideBox(final double value, final double origin, final double size) {
        final double min = Math.min(origin, origin + size);
        final double max = Math.max(origin, origin + size);
        return value < min || value > max;
    }

    private void sort(final List<Entity> entities, final Position origin) {
        switch (sort) {
            case NEAREST -> entities.sort((a, b) -> Double.compare(
                origin.squaredDistanceTo(a.x, a.y, a.z), origin.squaredDistanceTo(b.x, b.y, b.z)));
            case FURTHEST -> entities.sort((a, b) -> Double.compare(
                origin.squaredDistanceTo(b.x, b.y, b.z), origin.squaredDistanceTo(a.x, a.y, a.z)));
            case RANDOM -> Collections.shuffle(entities);
            case ARBITRARY -> {
            }
        }
    }
}
