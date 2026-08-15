package com.periut.retroapi.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a config field as a nested section. The field holds an instance whose own {@link Opt} and
 * {@code @Cat} fields become the section's contents, which is what gives the config file its
 * subsections and the screen its drill-down categories.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Cat {

	/** Label shown in the config screen. */
	String name();

	/** One-line hint shown under the label. */
	String desc() default "";

	/**
	 * Mod ids that also provide everything in this section. When any is present, every boolean
	 * inside is forced off. See {@link Opt#source()}.
	 */
	String[] source() default {};

	/**
	 * Default scope for every option in this section that does not state its own, and for every
	 * subsection that does not state its own either. See {@link Scope}.
	 *
	 * <p>{@link Scope#INHERIT} - the default - means "whatever the enclosing section is". It has to be
	 * the default rather than {@code WORLD}: a subsection is written as part of its parent, and a
	 * plain {@code @Cat(name = "Video")} inside a {@code CLIENT} section obviously means client video
	 * settings. When this defaulted to {@code WORLD} instead, those subsections silently became
	 * world options - so a player joining a vanilla server had their FOV, brightness, clouds and
	 * render distance forced back to vanilla for the session, and joining a RetroTweaks server
	 * replaced them with the server admin's.
	 */
	Scope scope() default Scope.INHERIT;

	/**
	 * Field name of a sibling {@code @Opt} - declared in the same enclosing object as this section -
	 * whose current value decides whether this section does anything at all. Empty (the default)
	 * means the section is always available.
	 *
	 * <p>This is for mutually exclusive option groups where only one group is ever read (e.g. HUD
	 * Positions' Simple vs. Advanced systems): the screen greys out and explains the group(s) the
	 * current choice ignores, instead of leaving every group editable while only one does anything.
	 * It never touches any option's saved value - see {@link ConfigTree.Category#gate}.
	 */
	String gateOption() default "";

	/**
	 * The values of {@link #gateOption()} that keep this section enabled: enum constant names, or
	 * {@code "true"}/{@code "false"} for a boolean. Ignored unless {@link #gateOption()} is set.
	 */
	String[] gateValues() default {};
}
