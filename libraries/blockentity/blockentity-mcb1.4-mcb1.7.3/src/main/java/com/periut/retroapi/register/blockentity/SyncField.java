package com.periut.retroapi.register.blockentity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@code int} field on a {@link RetroMenu} subclass for automatic
 * server→client synchronization via the vanilla inventory menu data protocol.
 * <p>
 * The annotation's value is the name of the corresponding {@code int} field
 * on the block entity. Each tick, the menu compares its cached copy to the
 * block entity's value and sends an update packet when they differ.
 * <p>
 * Example:
 * <pre>
 * public class FurnaceMenu extends RetroMenu {
 *     &#64;SyncField("cookTime")
 *     public int cookTime;
 *
 *     &#64;SyncField("fuelTime")
 *     public int fuelTime;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SyncField {
	/**
	 * The name of the {@code int} field on the block entity to sync from.
	 */
	String value();
}
