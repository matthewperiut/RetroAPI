package com.periut.retroapi.testmod;

import com.periut.retroapi.component.RetroLayeredTexture;
import com.periut.retroapi.component.RetroTextureLayer;
import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.item.RetroItemAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Compile-time proof that RetroAPI's injected interfaces impose no implementation burden on a mod.
 *
 * <p>Nothing here runs. It exists so the build fails if {@link RetroItemAccess}/{@link RetroBlockAccess}
 * ever go back to declaring abstract methods: a mod class that extends {@code Item} (or {@code Block})
 * would then inherit 56 unimplemented methods. {@code javac} lets that slide because it does not
 * re-verify a binary superclass, so it only shows up as an IDE lighting the class up red - and only once
 * the class implements some interface of its own, which is what makes it look unrelated to RetroAPI.
 * These two classes are exactly that shape, plus a direct implementor, which javac <em>does</em> check.
 */
public final class InjectedInterfaceTest {
	private InjectedInterfaceTest() {}

	/** The shape that broke: a mod Item that also implements a RetroAPI interface of its own. */
	public static class LayeredModItem extends Item implements RetroLayeredTexture {
		public LayeredModItem(int id) {
			super(id);
		}

		@Override
		public List<RetroTextureLayer> getTextureLayers(ItemStack stack) {
			return null;
		}
	}

	/**
	 * A direct implementor with NO method bodies. javac fully checks this one, so it will not compile
	 * unless every injected-interface method carries a default.
	 */
	public static class BareItemAccess implements RetroItemAccess {
	}

	/** The same, for the block side. */
	public static class BareBlockAccess implements RetroBlockAccess {
	}

	/** Interface injection itself: these must compile with no cast to RetroItemAccess/RetroBlockAccess. */
	public static void injectionProbe() {
		Item.STICK.getToolKinds();
		net.minecraft.block.Block.STONE.isAlwaysDrops();
	}
}
