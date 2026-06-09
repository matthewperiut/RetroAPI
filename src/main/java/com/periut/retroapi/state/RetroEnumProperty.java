package com.periut.retroapi.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An enum property. Constants serialize as their lowercase names unless the enum
 * implements {@link RetroNameable}. The default state has the first constant.
 */
public final class RetroEnumProperty<E extends Enum<E>> extends RetroProperty<E> {

	private final List<E> values;

	private RetroEnumProperty(String name, Class<E> enumClass) {
		super(name);
		this.values = Collections.unmodifiableList(Arrays.asList(enumClass.getEnumConstants()));
	}

	public static <E extends Enum<E>> RetroEnumProperty<E> of(String name, Class<E> enumClass) {
		return new RetroEnumProperty<>(name, enumClass);
	}

	@Override
	public List<E> values() {
		return values;
	}

	@Override
	public String valueName(E value) {
		if (value instanceof RetroNameable) {
			return ((RetroNameable) value).getName();
		}
		return value.name().toLowerCase(java.util.Locale.ROOT);
	}

	@Override
	public E parse(String name) {
		for (E value : values) {
			if (valueName(value).equals(name)) {
				return value;
			}
		}
		return null;
	}
}
