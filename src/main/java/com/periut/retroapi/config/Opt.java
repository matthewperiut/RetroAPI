package com.periut.retroapi.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a config field as a user-visible option. The field's declared type decides the widget:
 * {@code Boolean} -> toggle, {@code Integer}/{@code Float} -> slider, {@code String} -> text field,
 * any {@code enum} -> cycling button, {@code String[]}/{@code Integer[]} -> JSON-only list.
 *
 * <p>Fields without this annotation are ignored entirely, so a category can keep private helper
 * state without it leaking into the config file or the screen.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Opt {

	/** Label shown in the config screen. */
	String name();

	/** One-line hint shown under the label. Keep it under ~46 characters so it fits the row. */
	String desc() default "";

	/** Slider lower bound (numeric options only). */
	double min() default 0;

	/** Slider upper bound (numeric options only). */
	double max() default 1;

	/** Shows a "restart required" marker in the screen; purely informational. */
	boolean restart() default false;

	/**
	 * Mod ids that also provide this feature, or own the thing it patches. If any of them is
	 * installed the option is forced off and greyed out, so two mods never fight over the same
	 * behaviour. The user's own value is still remembered and written back to disk, so removing the
	 * other mod restores it.
	 */
	String[] source() default {};

	/**
	 * Mod ids that this option needs at least one of to do anything - the opposite of
	 * {@link #source()}. If none of them is installed the option is forced off and greyed out with
	 * an explanation, instead of sitting in the screen enabled and silently doing nothing (e.g. an
	 * option that only works through an optional API's registry).
	 */
	String[] requires() default {};

	/**
	 * Whether this option is safe to keep running on a server that does not have RetroTweaks.
	 * Defaults to inheriting the enclosing section's scope; see {@link Scope}.
	 */
	Scope scope() default Scope.INHERIT;
}
