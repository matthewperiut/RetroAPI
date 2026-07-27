package com.periut.retroapi.entrypoint;

/**
 * The {@code retroapi} entrypoint: the door RetroAPI itself knocks on, at the one moment when
 * registering content is guaranteed to work.
 *
 * <p>Declare it in {@code fabric.mod.json} exactly like the OSL ones:
 * <pre>
 * "entrypoints": {
 *     "retroapi":        [ "com.example.example_mod.ExampleMod" ],
 *     "retroapi-client": [ "com.example.example_mod.ExampleModClient" ],
 *     "retroapi-server": [ "com.example.example_mod.ExampleModServer" ]
 * }
 * </pre>
 *
 * <h2>Why not just use {@code init}?</h2>
 * The loader's {@code init} stage runs every mod's initializer in an <em>unspecified order</em>, so a mod
 * built on RetroAPI can - and intermittently does - initialize <em>before</em> RetroAPI has set up the
 * platform its registration calls rely on. That is the root of the "sometimes my recipes/blocks are just
 * missing" class of bug. Content registered from {@code initRetro()} is always ordered:
 *
 * <ul>
 *   <li>after RetroAPI's block/item registries, tag defaults, and lang files are ready;</li>
 *   <li>after vanilla's order-sensitive {@code Block}/{@code Item}/{@code Stats} static-init cycle has
 *       been entered from the safe side;</li>
 *   <li>before RetroAPI's own registration events fire, so anything you register is visible to them;</li>
 *   <li>before recipes are sorted, so recipes added here take part in vanilla's shaped-before-shapeless
 *       ordering instead of being appended after it;</li>
 *   <li>before world load, so every id your content gets is included in the world's id map.</li>
 * </ul>
 *
 * <p>It also fires <b>identically with and without StationAPI</b>. RetroAPI's own registration events do
 * not (StationAPI owns registration when it is present), so this is the only registration hook that
 * behaves the same in both worlds.
 *
 * <p>{@code init} keeps working exactly as before - nothing about it is deprecated, and a mod may use
 * both. Use {@code init} for things that are not RetroAPI registrations (mixin-adjacent setup, your own
 * config loading); use {@code retroapi} for the content.
 *
 * @see RetroClientModInitializer
 * @see RetroServerModInitializer
 */
@FunctionalInterface
public interface RetroModInitializer {

	/**
	 * Registers this mod's content: blocks, items, block entities, entities, dimensions, tags, features -
	 * everything RetroAPI hands out a {@code register(...)} for. Runs on both sides (client and dedicated
	 * server), exactly once, before any world exists.
	 *
	 * <p><b>Anything that REFERENCES another mod's content belongs in a registration callback instead,
	 * not here.</b> Every mod's {@code initRetro()} runs before any callback fires, but they run in mod
	 * load order relative to each other - so a recipe or an achievement icon built here that names another
	 * mod's item works or fails depending on which mod loaded first, which surfaces as an intermittent
	 * crash that moves around when you add an unrelated dependency. The callbacks exist precisely to give
	 * that a defined order:
	 *
	 * <pre>
	 * RecipeRegistrationCallback.EVENT.register(() -&gt; { ... });        // after ALL blocks and items
	 * AchievementRegistrationCallback.EVENT.register(() -&gt; { ... });   // after ALL blocks and items
	 * </pre>
	 *
	 * <p>The full order is: every mod's {@code initRetro()}, then
	 * {@link com.periut.retroapi.register.block.event.BlockRegistrationCallback}, then
	 * {@link com.periut.retroapi.register.item.event.ItemRegistrationCallback}, then achievements, then
	 * recipes. Registering your own blocks and items here (not in a callback) is right and is what the
	 * callbacks are ordered around.
	 */
	void initRetro();
}
