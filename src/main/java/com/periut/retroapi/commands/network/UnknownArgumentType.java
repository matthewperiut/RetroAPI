package com.periut.retroapi.commands.network;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

/**
 * What the client parses an argument with when it does not know the real type.
 *
 * <p>It eats one whitespace-delimited token, whatever is in it. That is deliberately more permissive
 * than Brigadier's own {@code word()}, which stops at the first character outside its unquoted
 * alphabet - and {@code ':'} is outside it, so {@code word()} took {@code minecraft} out of
 * {@code minecraft:diamond_block} and left {@code :diamond_block} as trailing data. The client then
 * underlined a perfectly good command in red and reported a syntax error for something the server
 * would have run without complaint.
 *
 * <p>An unknown type should cost colour and completions. It must never cost correctness, because the
 * client's parse is what decides whether the player is told their command is wrong.
 */
public final class UnknownArgumentType implements ArgumentType<String> {

	public static final UnknownArgumentType INSTANCE = new UnknownArgumentType();

	private UnknownArgumentType() {
	}

	@Override
	public String parse(final StringReader reader) throws CommandSyntaxException {
		final int start = reader.getCursor();
		while (reader.canRead() && reader.peek() != ' ') {
			reader.skip();
		}
		return reader.getString().substring(start, reader.getCursor());
	}

	@Override
	public Collection<String> getExamples() {
		return List.of();
	}
}
