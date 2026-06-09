package com.periut.retroapi.state;

import java.util.List;

/**
 * One property of a block state definition: a name plus an ordered, immutable value list.
 * Three concrete kinds exist: {@link RetroBoolProperty}, {@link RetroIntProperty} and
 * {@link RetroEnumProperty}. The value ORDER is significant: it defines how states flatten
 * into indices (first value of every property = the default state).
 */
public abstract class RetroProperty<T> {

	private final String name;

	protected RetroProperty(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	/** The ordered, immutable value list. */
	public abstract List<T> values();

	/** The serialized form of a value (variant keys, JSON), e.g. "true", "3", "north". */
	public abstract String valueName(T value);

	/** Parses a serialized value, or null if it isn't one of this property's values. */
	public abstract T parse(String name);

	/** The position of a value in {@link #values()}, used for index flattening. */
	public int ordinalOf(Object value) {
		int i = values().indexOf(value);
		if (i < 0) {
			throw new IllegalArgumentException(value + " is not a value of property " + name);
		}
		return i;
	}

	@Override
	public String toString() {
		return name;
	}
}
