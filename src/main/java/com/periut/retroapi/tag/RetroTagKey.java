package com.periut.retroapi.tag;

import java.util.Objects;

/**
 * Identifies a tag by category and path, e.g. {@code block("mineable/pickaxe")}.
 *
 * <p>RetroAPI tags are deliberately namespace-blind: a tag file at
 * {@code data/minecraft/tags/block/mineable/pickaxe.json} and one at
 * {@code data/mymod/tags/block/mineable/pickaxe.json} both feed the SAME tag
 * ({@code mineable/pickaxe}), so vanilla data files copied straight from modern
 * Minecraft and mod additions union together with zero ceremony. References
 * ({@code "#ns:path"}) likewise resolve by path, ignoring the namespace.</p>
 */
public final class RetroTagKey {

	private final String category;
	private final String path;

	private RetroTagKey(String category, String path) {
		this.category = category;
		this.path = path;
	}

	/** A block tag, e.g. {@code block("mineable/pickaxe")} or {@code block("custom/bouncy")}. */
	public static RetroTagKey block(String path) {
		return new RetroTagKey("block", normalize(path));
	}

	/** An item tag, e.g. {@code item("tools/pickaxes")} or {@code item("custom/magic")}. */
	public static RetroTagKey item(String path) {
		return new RetroTagKey("item", normalize(path));
	}

	/** True for item tags (as opposed to block tags). */
	public boolean isItem() {
		return "item".equals(category);
	}

	/** Strips a leading {@code #} and any {@code namespace:} prefix; tags resolve by path. */
	static String normalize(String path) {
		if (path.startsWith("#")) {
			path = path.substring(1);
		}
		int colon = path.indexOf(':');
		if (colon >= 0) {
			path = path.substring(colon + 1);
		}
		return path;
	}

	public String getCategory() {
		return category;
	}

	public String getPath() {
		return path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RetroTagKey)) return false;
		RetroTagKey other = (RetroTagKey) o;
		return category.equals(other.category) && path.equals(other.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, path);
	}

	@Override
	public String toString() {
		return category + "/" + path;
	}
}
