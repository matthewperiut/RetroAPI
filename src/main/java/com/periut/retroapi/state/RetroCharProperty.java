package com.periut.retroapi.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A character property - a letter, digit or symbol as a state value, so a block that stores "which
 * letter am I" stops converting {@code 'a'} to {@code 0} and back by hand.
 *
 * <pre>
 * static final RetroCharProperty LETTER = RetroCharProperty.letters("letter");   // 'a'..'z'
 *
 * LETTER_BLOCK = RetroBlockAccess.create(Material.STONE)
 *     .states(LETTER)
 *     .register(id("letter"));
 *
 * RetroStates.set(world, x, y, z, RetroStates.getDefault(LETTER_BLOCK).with(LETTER, 'q'));
 * char shown = state.get(LETTER);
 * </pre>
 *
 * <p>Values serialize as the character itself, so a blockstate JSON matches on {@code "letter=q"} and a
 * model variant key reads the way you'd write it. Use {@link #of(String, char, char)} for a contiguous
 * range or {@link #of(String, char...)} for an explicit set (e.g. just vowels).
 */
public final class RetroCharProperty extends RetroProperty<Character> {

	private final List<Character> values;
	private final char min;
	private final char max;
	private final boolean contiguous;

	private RetroCharProperty(String name, List<Character> values, char min, char max, boolean contiguous) {
		super(name);
		this.values = Collections.unmodifiableList(values);
		this.min = min;
		this.max = max;
		this.contiguous = contiguous;
	}

	/** An inclusive character range, e.g. {@code of("letter", 'a', 'z')} or {@code of("digit", '0', '9')}. */
	public static RetroCharProperty of(String name, char min, char max) {
		if (max < min) {
			throw new IllegalArgumentException("max < min for property " + name);
		}
		List<Character> list = new ArrayList<>(max - min + 1);
		for (char c = min; c <= max; c++) {
			list.add(c);
		}
		return new RetroCharProperty(name, list, min, max, true);
	}

	/** An explicit set of characters, in the order given (the first is the default state's value). */
	public static RetroCharProperty of(String name, char... chars) {
		if (chars.length == 0) {
			throw new IllegalArgumentException("property " + name + " needs at least one character");
		}
		List<Character> list = new ArrayList<>(chars.length);
		for (char c : chars) {
			list.add(c);
		}
		return new RetroCharProperty(name, list, chars[0], chars[chars.length - 1], false);
	}

	/** The 26 lowercase letters, {@code 'a'} to {@code 'z'}. */
	public static RetroCharProperty letters(String name) {
		return of(name, 'a', 'z');
	}

	/** The ten digits, {@code '0'} to {@code '9'}. */
	public static RetroCharProperty digits(String name) {
		return of(name, '0', '9');
	}

	public char min() {
		return min;
	}

	public char max() {
		return max;
	}

	@Override
	public List<Character> values() {
		return values;
	}

	@Override
	public String valueName(Character value) {
		return String.valueOf((char) value);
	}

	@Override
	public Character parse(String name) {
		if (name == null || name.length() != 1) {
			return null;
		}
		char c = name.charAt(0);
		return values.contains(c) ? c : null;
	}

	@Override
	public int ordinalOf(Object value) {
		char c = (Character) value;
		if (contiguous) {
			if (c < min || c > max) {
				throw new IllegalArgumentException("'" + c + "' is outside property " + name() + " range '"
					+ min + "'..'" + max + "'");
			}
			return c - min;
		}
		int i = values.indexOf(c);
		if (i < 0) {
			throw new IllegalArgumentException("'" + c + "' is not a value of property " + name());
		}
		return i;
	}
}
