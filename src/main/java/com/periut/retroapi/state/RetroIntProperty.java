package com.periut.retroapi.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An integer range property, {@code min..max} inclusive. The default state has it at {@code min}. */
public final class RetroIntProperty extends RetroProperty<Integer> {

	private final int min;
	private final int max;
	private final List<Integer> values;

	private RetroIntProperty(String name, int min, int max) {
		super(name);
		if (max < min) {
			throw new IllegalArgumentException("max < min for property " + name);
		}
		this.min = min;
		this.max = max;
		List<Integer> list = new ArrayList<>(max - min + 1);
		for (int i = min; i <= max; i++) {
			list.add(i);
		}
		this.values = Collections.unmodifiableList(list);
	}

	public static RetroIntProperty of(String name, int min, int max) {
		return new RetroIntProperty(name, min, max);
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	@Override
	public List<Integer> values() {
		return values;
	}

	@Override
	public String valueName(Integer value) {
		return value.toString();
	}

	@Override
	public Integer parse(String name) {
		try {
			int v = Integer.parseInt(name);
			return (v >= min && v <= max) ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public int ordinalOf(Object value) {
		// Contiguous range: avoid the list scan.
		int v = (Integer) value;
		if (v < min || v > max) {
			throw new IllegalArgumentException(v + " is outside property " + name() + " range " + min + ".." + max);
		}
		return v - min;
	}
}
