package com.periut.retroapi.commands.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.periut.retroapi.commands.RetroCommandSource;
import net.ornithemc.osl.networking.api.PacketBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes a command tree flat and reads it back, the way modern Minecraft's command-tree packet does.
 *
 * <p>Nodes are numbered and referred to by index rather than nested, so that a redirect - which is
 * how {@code /tp} points at {@code /teleport} - costs one number instead of a duplicated subtree.
 *
 * <p>What the client rebuilds is a parsing skeleton: no requirements, because the server already
 * removed what this player may not use, and no executors, because the client never runs it.
 */
public final class CommandTreeSerializer {
    private static final int TYPE_ROOT = 0;
    private static final int TYPE_LITERAL = 1;
    private static final int TYPE_ARGUMENT = 2;
    private static final int TYPE_MASK = 0x03;
    private static final int FLAG_EXECUTABLE = 0x04;
    private static final int FLAG_REDIRECT = 0x08;

    private CommandTreeSerializer() {
    }

    public static void write(final RootCommandNode<RetroCommandSource> root, final PacketBuffer buffer) {
        final Map<CommandNode<RetroCommandSource>, Integer> indices = new HashMap<>();
        final List<CommandNode<RetroCommandSource>> nodes = new ArrayList<>();
        collect(root, indices, nodes);

        buffer.writeVarInt(nodes.size());
        for (final CommandNode<RetroCommandSource> node : nodes) {
            writeNode(node, indices, buffer);
        }
        buffer.writeVarInt(indices.get(root));
    }

    private static void collect(final CommandNode<RetroCommandSource> node,
                                final Map<CommandNode<RetroCommandSource>, Integer> indices,
                                final List<CommandNode<RetroCommandSource>> nodes) {
        if (indices.containsKey(node)) {
            return;
        }

        indices.put(node, nodes.size());
        nodes.add(node);

        for (final CommandNode<RetroCommandSource> child : node.getChildren()) {
            collect(child, indices, nodes);
        }
        if (node.getRedirect() != null) {
            collect(node.getRedirect(), indices, nodes);
        }
    }

    private static void writeNode(final CommandNode<RetroCommandSource> node,
                                  final Map<CommandNode<RetroCommandSource>, Integer> indices,
                                  final PacketBuffer buffer) {
        int flags;
        if (node instanceof RootCommandNode) {
            flags = TYPE_ROOT;
        } else if (node instanceof LiteralCommandNode) {
            flags = TYPE_LITERAL;
        } else {
            flags = TYPE_ARGUMENT;
        }

        if (node.getCommand() != null) {
            flags |= FLAG_EXECUTABLE;
        }
        if (node.getRedirect() != null) {
            flags |= FLAG_REDIRECT;
        }

        buffer.writeByte(flags);

        buffer.writeVarInt(node.getChildren().size());
        for (final CommandNode<RetroCommandSource> child : node.getChildren()) {
            buffer.writeVarInt(indices.get(child));
        }

        if (node.getRedirect() != null) {
            buffer.writeVarInt(indices.get(node.getRedirect()));
        }

        if ((flags & TYPE_MASK) != TYPE_ROOT) {
            buffer.writeString(node.getName());
        }
        if ((flags & TYPE_MASK) == TYPE_ARGUMENT) {
            ArgumentTypes.write(((ArgumentCommandNode<RetroCommandSource, ?>) node).getType(), buffer);
        }
    }

    public static CommandDispatcher<RetroCommandSource> read(final PacketBuffer buffer) {
        final int count = buffer.readVarInt();
        final RawNode[] raw = new RawNode[count];

        for (int i = 0; i < count; i++) {
            raw[i] = readRaw(buffer);
        }
        final int rootIndex = buffer.readVarInt();

        final CommandNode<RetroCommandSource>[] built = build(raw);

        // Children are attached after every node exists, because a node's children may appear
        // later in the list than the node itself.
        for (int i = 0; i < count; i++) {
            for (final int child : raw[i].children) {
                if (built[i] != null && built[child] != null && !(built[child] instanceof RootCommandNode)) {
                    built[i].addChild(built[child]);
                }
            }
        }

        final CommandNode<RetroCommandSource> root = built[rootIndex];
        return new CommandDispatcher<>(root instanceof RootCommandNode
            ? (RootCommandNode<RetroCommandSource>) root
            : new RootCommandNode<>());
    }

    private record RawNode(int flags, int[] children, int redirect, String name, ArgumentType<?> argumentType) {
        int nodeType() {
            return flags & TYPE_MASK;
        }

        boolean executable() {
            return (flags & FLAG_EXECUTABLE) != 0;
        }

        boolean hasRedirect() {
            return (flags & FLAG_REDIRECT) != 0;
        }
    }

    private static RawNode readRaw(final PacketBuffer buffer) {
        final int flags = buffer.readByte();

        final int[] children = new int[buffer.readVarInt()];
        for (int i = 0; i < children.length; i++) {
            children[i] = buffer.readVarInt();
        }

        final int redirect = (flags & FLAG_REDIRECT) != 0 ? buffer.readVarInt() : -1;
        final String name = (flags & TYPE_MASK) != TYPE_ROOT ? buffer.readString() : "";
        final ArgumentType<?> type = (flags & TYPE_MASK) == TYPE_ARGUMENT ? ArgumentTypes.read(buffer) : null;

        return new RawNode(flags, children, redirect, name, type);
    }

    /**
     * Builds nodes in two rounds: everything without a redirect first, then the redirecting nodes,
     * whose targets exist by then. Brigadier forbids a redirecting node from having children, so no
     * cycle can outlive the second round.
     */
    @SuppressWarnings("unchecked")
    private static CommandNode<RetroCommandSource>[] build(final RawNode[] raw) {
        final CommandNode<RetroCommandSource>[] built = new CommandNode[raw.length];
        final Set<Integer> deferred = new LinkedHashSet<>();

        for (int i = 0; i < raw.length; i++) {
            if (raw[i].hasRedirect()) {
                deferred.add(i);
                continue;
            }
            built[i] = create(raw[i], null);
        }

        for (final int index : deferred) {
            final RawNode node = raw[index];
            final CommandNode<RetroCommandSource> target = node.redirect() >= 0 ? built[node.redirect()] : null;
            built[index] = create(node, target);
        }

        return built;
    }

    private static CommandNode<RetroCommandSource> create(final RawNode node, final CommandNode<RetroCommandSource> redirect) {
        switch (node.nodeType()) {
            case TYPE_LITERAL -> {
                final LiteralArgumentBuilder<RetroCommandSource> builder = LiteralArgumentBuilder.literal(node.name());
                if (redirect != null) {
                    builder.redirect(redirect);
                }
                // The client never executes; the flag only has to make the node look runnable so
                // usage generation and error messages come out the same as on the server.
                if (node.executable()) {
                    builder.executes(context -> 0);
                }
                return builder.build();
            }
            case TYPE_ARGUMENT -> {
                final RequiredArgumentBuilder<RetroCommandSource, ?> builder =
                    RequiredArgumentBuilder.argument(node.name(), node.argumentType());
                if (redirect != null) {
                    builder.redirect(redirect);
                }
                if (node.executable()) {
                    builder.executes(context -> 0);
                }
                return builder.build();
            }
            default -> {
                return new RootCommandNode<>();
            }
        }
    }
}
