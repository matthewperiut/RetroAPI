package com.mojang.brigadier.context;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.tree.CommandNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommandContext<S> {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();

    static {
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
    }

    private final S source;
    private final String input;
    private final Command<S> command;
    private final Map<String, ParsedArgument<S, ?>> arguments;
    private final CommandNode<S> rootNode;
    private final List<ParsedCommandNode<S>> nodes;
    private final StringRange range;
    private final CommandContext<S> child;
    private final RedirectModifier<S> modifier;
    private final boolean forks;

    public CommandContext(final S source, final String input, final Map<String, ParsedArgument<S, ?>> arguments, final Command<S> command, final CommandNode<S> rootNode, final List<ParsedCommandNode<S>> nodes, final StringRange range, final CommandContext<S> child, final RedirectModifier<S> modifier, final boolean forks) {
        this.source = source;
        this.input = input;
        this.arguments = arguments;
        this.command = command;
        this.rootNode = rootNode;
        this.nodes = nodes;
        this.range = range;
        this.child = child;
        this.modifier = modifier;
        this.forks = forks;
    }

    public CommandContext<S> copyFor(final S source) {
        if (this.source == source) {
            return this;
        }
        return new CommandContext<>(source, input, arguments, command, rootNode, nodes, range, child, modifier, forks);
    }

    public CommandContext<S> getChild() {
        return child;
    }

    public CommandContext<S> getLastChild() {
        CommandContext<S> result = this;
        while (result.getChild() != null) {
            result = result.getChild();
        }
        return result;
    }

    public Command<S> getCommand() {
        return command;
    }

    public S getSource() {
        return source;
    }

    @SuppressWarnings("unchecked")
    public <V> V getArgument(final String name, final Class<V> clazz) {
        final ParsedArgument<S, ?> argument = arguments.get(name);

        if (argument == null) {
            throw new IllegalArgumentException("No such argument '" + name + "' exists on this command");
        }

        final Object result = argument.getResult();
        if (PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(result.getClass())) {
            return (V) result;
        }
        throw new IllegalArgumentException("Argument '" + name + "' is defined as " + result.getClass().getSimpleName() + ", not " + clazz);
    }

    /** Null rather than throwing, for optional arguments a command declares but did not receive. */
    public boolean hasArgument(final String name) {
        return arguments.containsKey(name);
    }

    public RedirectModifier<S> getRedirectModifier() {
        return modifier;
    }

    public StringRange getRange() {
        return range;
    }

    public String getInput() {
        return input;
    }

    public CommandNode<S> getRootNode() {
        return rootNode;
    }

    public List<ParsedCommandNode<S>> getNodes() {
        return nodes;
    }

    public boolean hasNodes() {
        return !nodes.isEmpty();
    }

    public boolean isForked() {
        return forks;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommandContext)) {
            return false;
        }
        final CommandContext<?> that = (CommandContext<?>) o;
        return Objects.equals(arguments, that.arguments)
            && rootNode.equals(that.rootNode)
            && nodes.size() == that.nodes.size()
            && nodes.equals(that.nodes)
            && Objects.equals(command, that.command)
            && source.equals(that.source)
            && Objects.equals(child, that.child);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, arguments, command, rootNode, nodes, child);
    }
}
