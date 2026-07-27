package com.periut.retroapi.register;

/**
 * Support for the duck interfaces RetroAPI injects into vanilla classes
 * ({@link com.periut.retroapi.register.item.RetroItemAccess},
 * {@link com.periut.retroapi.register.block.RetroBlockAccess}).
 *
 * <p>Those interfaces are added to {@code Item}/{@code Block} at build time by Loom's
 * {@code loom:injected_interfaces}, while the bodies come from a mixin at runtime. If their methods were
 * declared abstract, every class a mod writes that extends {@code Item} or {@code Block} would inherit
 * 56 unimplemented methods: {@code javac} does not re-check a binary superclass so the build still
 * passes, but an IDE recomputes the hierarchy and demands the mod implement all of them - which is what
 * "it wants me to implement everything" looks like, and it appears the moment the class implements any
 * other interface of its own.
 *
 * <p>So each one is a {@code default} that throws this. The mixin's real implementation is a method
 * <em>on the class</em>, and a class method always wins over an interface default, so nothing here ever
 * runs in a working setup. Seeing it means the mixin did not apply. (This is the same shape Fabric API
 * uses for its own injected interfaces.)
 */
public final class RetroInjected {
	private RetroInjected() {}

	/** Thrown when an injected-interface method is reached, which means its mixin is not applied. */
	public static UnsupportedOperationException missing() {
		return new UnsupportedOperationException(
			"RetroAPI's implementation for this method is supplied by a mixin, and that mixin is not "
				+ "applied here. Either RetroAPI failed to load, or this is a class RetroAPI does not "
				+ "mix into - the injected interfaces only carry implementations on Block and Item.");
	}
}
