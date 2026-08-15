/**
 * A reimplementation of Mojang's Brigadier command parser, kept in Brigadier's own package.
 *
 * <p>Brigadier is MIT licensed (<a href="https://github.com/Mojang/brigadier">Mojang/brigadier</a>).
 * This is a fresh implementation of that public API rather than a copy of its source: b1.7.3 cannot
 * pull the real artifact - it would have to be published as a transitive dependency, which this mod
 * forbids - and the real jar reaches for Guava, which beta does not ship. Everything here depends on
 * the JDK alone.
 *
 * <p>The package name is deliberate. Command code written against modern Minecraft, and any snippet
 * a modder copies out of it, compiles here unchanged.
 */
package com.mojang.brigadier;
