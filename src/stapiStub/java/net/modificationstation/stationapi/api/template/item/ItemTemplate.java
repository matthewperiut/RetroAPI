package net.modificationstation.stationapi.api.template.item;

import net.minecraft.item.Item;
import net.modificationstation.stationapi.api.util.Identifier;

/**
 * Stub - see src/stapiStub/java/README.md. The two static calls StationAPI's own
 * {@code TemplateItem(Identifier)} constructor is made of, split out so an {@code Item} subclass that
 * must not import a StationAPI type can still be registered the canonical way.
 *
 * <p>{@code getNextId} answers {@code ItemRegistry.AUTO_ID}, the sentinel StationAPI's mixin on
 * {@code Item(int)} resolves into a real free slot; {@code onConstructor} puts the finished item into
 * {@code ItemRegistry} under the given name, at the id the constructor settled on.
 *
 * <p>Both are declared here rather than the constant being copied, because a {@code static final int}
 * on a stub would be inlined by javac and freeze today's sentinel value into the jar.
 */
public interface ItemTemplate {

	static int getNextId() {
		throw new UnsupportedOperationException("stub");
	}

	static void onConstructor(Item item, Identifier id) {
		throw new UnsupportedOperationException("stub");
	}
}
