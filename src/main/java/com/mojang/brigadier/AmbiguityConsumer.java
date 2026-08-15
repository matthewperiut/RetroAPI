package com.mojang.brigadier;

import com.mojang.brigadier.tree.CommandNode;

import java.util.Collection;

@FunctionalInterface
public interface AmbiguityConsumer<S> {
    void ambiguous(CommandNode<S> parent, CommandNode<S> child, CommandNode<S> sibling, Collection<String> inputs);
}
