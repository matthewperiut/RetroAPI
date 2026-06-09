package com.periut.retroapi.register.rendertype;

/**
 * Functional interface for custom block rendering.
 * <p>
 * Implementations receive a {@link BlockRenderContext} providing access to the block,
 * position, world, lighting, sprites, and rendering helpers.
 * <p>
 * Example:
 * <pre>
 * RenderType.register(NamespacedIdentifiers.from("mymod", "slab"), ctx -> {
 *     ctx.getBlock().setShape(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
 *     Tesselator t = ctx.tesselator();
 *     float brightness = ctx.getBrightness();
 *     t.color(brightness, brightness, brightness);
 *
 *     for (int face = 0; face &lt; 6; face++) {
 *         if (ctx.shouldRenderFace(face)) {
 *             ctx.renderFace(face, ctx.getSprite(face));
 *         }
 *     }
 *     return true;
 * });
 * </pre>
 */
@FunctionalInterface
public interface CustomBlockRenderer {
	/**
	 * Render a block in the world.
	 *
	 * @param ctx rendering context with block data and helpers
	 * @return true if any geometry was emitted
	 */
	boolean render(BlockRenderContext ctx);
}
