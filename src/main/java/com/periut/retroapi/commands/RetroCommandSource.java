package com.periut.retroapi.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Who is running a command, and everything a command is allowed to know about them.
 *
 * <p>The equivalent of modern Minecraft's {@code ServerCommandSource}, with one difference forced by
 * beta: singleplayer has no integrated server, so a source can also be a bare client that executes
 * commands in its own world. {@link #getServer()} is null in that case and anything server-only
 * guards on it.
 */
public class RetroCommandSource {
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_MODERATOR = 2;
    public static final int LEVEL_OWNER = 4;

    public static final SimpleCommandExceptionType REQUIRES_PLAYER = new SimpleCommandExceptionType(
        Text.literal("A player is required to run this command here"));
    public static final SimpleCommandExceptionType REQUIRES_ENTITY = new SimpleCommandExceptionType(
        Text.literal("An entity is required to run this command here"));
    public static final SimpleCommandExceptionType REQUIRES_SERVER = new SimpleCommandExceptionType(
        Text.literal("This command can only be run on a server"));

    private final FeedbackSink sink;
    private final Entity entity;
    private final World world;
    private final Position position;
    private final float yaw;
    private final float pitch;
    private final String name;
    private final int level;
    private final MinecraftServer server;
    private final boolean client;
    /**
     * The block under the player's crosshair, or null when they are not looking at one. Only a
     * client ever fills this in; it exists so coordinate arguments can complete to where the player
     * is looking, the way modern's {@code ClientCommandSource} does.
     */
    private final Position lookedAtBlock;

    public RetroCommandSource(final FeedbackSink sink, final Entity entity, final World world, final Position position,
                              final float yaw, final float pitch, final String name, final int level,
                              final MinecraftServer server, final boolean client) {
        this(sink, entity, world, position, yaw, pitch, name, level, server, client, null);
    }

    private RetroCommandSource(final FeedbackSink sink, final Entity entity, final World world, final Position position,
                               final float yaw, final float pitch, final String name, final int level,
                               final MinecraftServer server, final boolean client, final Position lookedAtBlock) {
        this.sink = sink;
        this.entity = entity;
        this.world = world;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.name = name;
        this.level = level;
        this.server = server;
        this.client = client;
        this.lookedAtBlock = lookedAtBlock;
    }

    /** A copy of this source pointed at a different entity - how selectors fan a command out. */
    public RetroCommandSource withEntity(final Entity entity) {
        if (this.entity == entity) {
            return this;
        }
        return new RetroCommandSource(sink, entity, entity.world, new Position(entity.x, entity.y, entity.z),
            entity.yaw, entity.pitch, name, level, server, client, lookedAtBlock);
    }

    public RetroCommandSource withSink(final FeedbackSink sink) {
        return new RetroCommandSource(sink, entity, world, position, yaw, pitch, name, level, server, client, lookedAtBlock);
    }

    public RetroCommandSource withLevel(final int level) {
        return new RetroCommandSource(sink, entity, world, position, yaw, pitch, name, level, server, client, lookedAtBlock);
    }

    /** A copy of this source that knows which block the player has under their crosshair. */
    public RetroCommandSource withLookedAtBlock(final Position lookedAtBlock) {
        return new RetroCommandSource(sink, entity, world, position, yaw, pitch, name, level, server, client, lookedAtBlock);
    }

    /** @return the block the player is looking at, or null if they are not looking at one */
    public Position getLookedAtBlock() {
        return lookedAtBlock;
    }

    public Entity getEntity() {
        return entity;
    }

    public Entity getEntityOrThrow() throws CommandSyntaxException {
        if (entity == null) {
            throw REQUIRES_ENTITY.create();
        }
        return entity;
    }

    public PlayerEntity getPlayer() {
        return entity instanceof PlayerEntity ? (PlayerEntity) entity : null;
    }

    public PlayerEntity getPlayerOrThrow() throws CommandSyntaxException {
        final PlayerEntity player = getPlayer();
        if (player == null) {
            throw REQUIRES_PLAYER.create();
        }
        return player;
    }

    public World getWorld() {
        return world;
    }

    public Position getPosition() {
        return position;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasPermissionLevel(final int level) {
        return this.level >= level;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public MinecraftServer getServerOrThrow() throws CommandSyntaxException {
        if (server == null) {
            throw REQUIRES_SERVER.create();
        }
        return server;
    }

    /** True on a client-run source, whether singleplayer or a client typing into a server's chat. */
    public boolean isClient() {
        return client;
    }

    public void sendFeedback(final Text message) {
        sink.send(message);
    }

    public void sendFeedback(final String message) {
        sink.send(Text.literal(message));
    }

    public void sendError(final Text message) {
        sink.send(Text.empty().append(message.copy().formatted(Formatting.RED)));
    }

    public void sendError(final String message) {
        sendError(Text.literal(message));
    }

    /** Every player this source could name, for suggestions and for selector resolution. */
    public List<PlayerEntity> getPlayers() {
        if (server != null) {
            return new ArrayList<>(server.playerManager.players);
        }
        if (world != null) {
            return new ArrayList<>(world.players);
        }
        return Collections.emptyList();
    }

    public List<String> getPlayerNames() {
        final java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (final PlayerEntity player : getPlayers()) {
            names.add(player.name);
        }

        // A client's world holds only the players near enough to be loaded, so completing a name from it
        // alone misses most of a busy server - and missing them is what used to make asking the server
        // worth the wait. The roster it already sends fills the rest in locally. Empty on a server,
        // where the world IS everybody.
        final String[] roster = RetroCommands.player_names;
        if (roster != null) {
            for (final String name : roster) {
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return new ArrayList<>(names);
    }

    /** Every entity the source's world knows about; empty when there is no world (early console use). */
    public List<Entity> getWorldEntities() {
        return world == null ? Collections.emptyList() : new ArrayList<>(world.entities);
    }
}
