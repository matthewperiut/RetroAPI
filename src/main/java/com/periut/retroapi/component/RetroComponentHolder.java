package com.periut.retroapi.component;

import java.util.Map;

/**
 * Duck interface injected onto every ItemStack by the component mixin, holding the
 * stack's component map. Prefer the static {@link RetroComponents} helpers; this is the
 * low-level handle they call through, and what the copy/persistence mixins use.
 */
public interface RetroComponentHolder {

	/** The live component map for this stack (component type to value). Never null. */
	Map<RetroComponentType<?>, Object> retroapi$components();

	/** Replaces this stack's component map (used by copy to deep-clone). */
	void retroapi$setComponents(Map<RetroComponentType<?>, Object> components);
}
