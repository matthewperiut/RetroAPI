package com.periut.retroapi.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A boolean property. Value order is [false, true], so the default state has it false. */
public final class RetroBoolProperty extends RetroProperty<Boolean> {

	private static final List<Boolean> VALUES =
		Collections.unmodifiableList(Arrays.asList(Boolean.FALSE, Boolean.TRUE));

	private RetroBoolProperty(String name) {
		super(name);
	}

	public static RetroBoolProperty of(String name) {
		return new RetroBoolProperty(name);
	}

	@Override
	public List<Boolean> values() {
		return VALUES;
	}

	@Override
	public String valueName(Boolean value) {
		return value.toString();
	}

	@Override
	public Boolean parse(String name) {
		if ("true".equals(name)) return Boolean.TRUE;
		if ("false".equals(name)) return Boolean.FALSE;
		return null;
	}
}
