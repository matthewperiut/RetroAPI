package com.periut.retroapi.component;

import net.minecraft.item.ItemStack;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The data-component registry and the ergonomic get/set API, the beta port of the modern
 * {@code DataComponents} registry plus the {@code ItemStack.get/set/has/remove} methods.
 *
 * <p>Register a component once from your init:</p>
 * <pre>{@code
 * public static final RetroComponentType<Integer> CLICK_COUNT =
 *     RetroComponents.register(id("click_count"), 0, RetroComponentType.INT);
 * }</pre>
 *
 * <p>Then read and write it on any stack. Beta cannot add methods to ItemStack, so these
 * are static (the only difference from modern's {@code stack.get(TYPE)}):</p>
 * <pre>{@code
 * int n = RetroComponents.get(stack, CLICK_COUNT);     // value or the type's default
 * RetroComponents.set(stack, CLICK_COUNT, n + 1);
 * boolean present = RetroComponents.has(stack, CLICK_COUNT);
 * RetroComponents.remove(stack, CLICK_COUNT);
 * }</pre>
 *
 * <p>Values persist (saved through RetroAPI's item sidecar / world save) and, in
 * singleplayer, are simply present because the client IS the world. See the components
 * chapter for the dedicated-server sync note.</p>
 */
public final class RetroComponents {

	private static final Map<String, RetroComponentType<?>> REGISTRY = new HashMap<>();

	private RetroComponents() {
	}

	/** Registers a component type. Mirrors modern's {@code DataComponentType.builder().persistent(codec)}. */
	public static <T> RetroComponentType<T> register(NamespacedIdentifier id, T defaultValue,
			RetroComponentType.Serializer<T> serializer) {
		RetroComponentType<T> type = new RetroComponentType<>(id, defaultValue, serializer);
		REGISTRY.put(id.toString(), type);
		return type;
	}

	/** The registered type for an id string ("mod:name"), or null. Used by persistence/sync. */
	public static RetroComponentType<?> byId(String id) {
		return REGISTRY.get(id);
	}

	// --------------------------------------------------------------- stack API --

	/** The component value on this stack, or the type's default if unset. */
	@SuppressWarnings("unchecked")
	public static <T> T get(ItemStack stack, RetroComponentType<T> type) {
		Object value = holder(stack).retroapi$components().get(type);
		return value != null ? (T) value : type.getDefault();
	}

	/** The component value on this stack, or the supplied fallback if unset. */
	@SuppressWarnings("unchecked")
	public static <T> T getOrDefault(ItemStack stack, RetroComponentType<T> type, T fallback) {
		Object value = holder(stack).retroapi$components().get(type);
		return value != null ? (T) value : fallback;
	}

	/** Sets the component value on this stack. */
	public static <T> void set(ItemStack stack, RetroComponentType<T> type, T value) {
		holder(stack).retroapi$components().put(type, value);
	}

	/** Whether this stack explicitly has the component set. */
	public static boolean has(ItemStack stack, RetroComponentType<?> type) {
		return holder(stack).retroapi$components().containsKey(type);
	}

	/** Removes the component from this stack. */
	public static void remove(ItemStack stack, RetroComponentType<?> type) {
		holder(stack).retroapi$components().remove(type);
	}

	private static RetroComponentHolder holder(ItemStack stack) {
		return (RetroComponentHolder) (Object) stack;
	}
}
