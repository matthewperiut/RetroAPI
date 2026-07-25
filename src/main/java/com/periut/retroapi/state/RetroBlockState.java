package com.periut.retroapi.state;

import net.minecraft.block.Block;

import java.util.List;

/**
 * One immutable state of a block: a value for every property of its definition, plus the
 * flattened index that IS the storage format (bits 0-3 ride vanilla metadata, bits 4-11
 * are secondary meta in the region sidecar). Instances are interned per block, so
 * {@code ==} comparison is valid and {@link #with} returns the sibling instance.
 */
public final class RetroBlockState {

	private final RetroStateDefinition definition;
	private final Object[] values;
	private final int index;

	RetroBlockState(RetroStateDefinition definition, Object[] values, int index) {
		this.definition = definition;
		this.values = values;
		this.index = index;
	}

	public Block getBlock() {
		return definition.block;
	}

	/** The flattened index: bits 0-3 = vanilla meta, bits 4-11 = xmeta. */
	public int getIndex() {
		return index;
	}

	@SuppressWarnings("unchecked")
	public <V> V get(RetroProperty<V> property) {
		int slot = definition.slotOf(property);
		return (V) values[slot];
	}

	/** The sibling state with one property changed. Interned: repeated calls return the same instance. */
	public <V> RetroBlockState with(RetroProperty<V> property, V value) {
		int slot = definition.slotOf(property);
		if (values[slot].equals(value)) {
			return this;
		}
		int delta = (property.ordinalOf(value) - property.ordinalOf(values[slot])) * definition.strides[slot];
		return definition.states[index + delta];
	}

	/** The properties of this state's definition, in declaration order. */
	public List<RetroProperty<?>> getProperties() {
		return definition.properties;
	}

	/** This state's property with the given name, or null if it has none. */
	public RetroProperty<?> getProperty(String name) {
		return definition.byName(name);
	}

	/**
	 * The value of a property looked up by NAME, or null if this state has no such property. Useful for
	 * generic code (renderers, the built-in facing textures) that must work with whatever a block declared
	 * without holding the property object; prefer {@link #get(RetroProperty)} when you have it.
	 */
	public Object getByName(String name) {
		RetroProperty<?> property = definition.byName(name);
		return property == null ? null : values[definition.slotOf(property)];
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(definition.block.getTranslationKey()).append('[');
		for (int i = 0; i < definition.properties.size(); i++) {
			if (i > 0) sb.append(',');
			@SuppressWarnings("unchecked")
			RetroProperty<Object> p = (RetroProperty<Object>) definition.properties.get(i);
			sb.append(p.name()).append('=').append(p.valueName(values[i]));
		}
		return sb.append(']').toString();
	}
}
