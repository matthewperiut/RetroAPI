package com.periut.retroapi.state;

import net.minecraft.block.Block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A block's ordered property list and the interned Cartesian product of its states.
 * The first-declared property varies SLOWEST in the flattened index. Definitions are
 * capped at 4096 states (12 storage bits) and fail fast beyond that.
 */
final class RetroStateDefinition {

	final Block block;
	final List<RetroProperty<?>> properties;
	/** Index step per property slot (product of later properties' value counts). */
	final int[] strides;
	/** Every state, by flattened index. */
	final RetroBlockState[] states;
	final RetroBlockState defaultState;

	RetroStateDefinition(Block block, List<RetroProperty<?>> properties, java.util.function.UnaryOperator<RetroBlockState> defaultOp) {
		this.block = block;
		this.properties = Collections.unmodifiableList(properties);

		int count = 1;
		this.strides = new int[properties.size()];
		for (int i = properties.size() - 1; i >= 0; i--) {
			strides[i] = count;
			count *= properties.get(i).values().size();
			if (count > 4096) {
				throw new IllegalArgumentException("Block state definition for " + block.getTranslationKey()
					+ " exceeds 4096 states (12 storage bits); trim the properties");
			}
		}

		this.states = new RetroBlockState[count];
		for (int index = 0; index < count; index++) {
			Object[] values = new Object[properties.size()];
			for (int slot = 0; slot < properties.size(); slot++) {
				List<?> propertyValues = properties.get(slot).values();
				values[slot] = propertyValues.get((index / strides[slot]) % propertyValues.size());
			}
			states[index] = new RetroBlockState(this, values, index);
		}

		RetroBlockState def = states[0];
		if (defaultOp != null) {
			def = defaultOp.apply(def);
		}
		this.defaultState = def;
	}

	int slotOf(RetroProperty<?> property) {
		int slot = properties.indexOf(property);
		if (slot < 0) {
			throw new IllegalArgumentException("Property " + property.name() + " is not part of "
				+ block.getTranslationKey() + "'s state definition " + Arrays.toString(properties.toArray()));
		}
		return slot;
	}

	RetroProperty<?> byName(String name) {
		for (RetroProperty<?> property : properties) {
			if (property.name().equals(name)) {
				return property;
			}
		}
		return null;
	}
}
