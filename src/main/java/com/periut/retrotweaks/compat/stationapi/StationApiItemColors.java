package com.periut.retrotweaks.compat.stationapi;

import com.periut.retrotweaks.config.ConfigManager;
import com.periut.retrotweaks.feature.flora.TallPlantVariants;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.client.color.world.GrassColors;
import net.modificationstation.stationapi.api.client.event.color.item.ItemColorsRegisterEvent;
import net.modificationstation.stationapi.api.item.ItemConvertible;

/**
 * Tints grass and tall grass as ITEMS when StationAPI is drawing them.
 *
 * <p>StationAPI's arsenic renderer replaces every item render path in the game, so the two mixins
 * that fix this on a bare install ({@code TallPlantColorMixin} and {@code BlockRenderManagerMixin}'s
 * grass-top section) are never reached there. Its own renderer asks a colour registry instead - and
 * while it registers providers for BLOCKS in the world, it ships none for items, which is why a grass
 * block in the inventory comes out grey under it. This fills that gap with the same colour the world
 * already uses.
 *
 * <p>The tint only reaches a face whose model declares a {@code tintindex}, which no vanilla block
 * model does; the item models shipped alongside this are what supply it, and they are ITEM models, so
 * world rendering is untouched:
 * <ul>
 *   <li>{@code assets/minecraft/stationapi/models/item/grass_block.json} - the grass block, tinted on
 *       its top face and its side overlay, exactly where the world tints it.
 *   <li>{@code assets/minecraft/stationapi/models/item/grass.json} - block 31, whose three metas are
 *       the dead shrub, short grass and the fern. StationAPI keys item models by item and not by
 *       damage, so that one is a base model plus two {@code meta} predicate overrides; see the file.
 * </ul>
 *
 * <p><b>This class is the only one in RetroTweaks allowed to import a StationAPI type.</b> It is
 * reached solely through the {@code stationapi:event_bus_client} entrypoint key in fabric.mod.json,
 * which a loader with no StationAPI installed never asks for - so on a bare install it is never
 * loaded and its missing types never matter. It compiles against signature-only stubs rather than the
 * real artifact; see {@code src/stapiStub}. Same arrangement, and the same reasoning, as
 * {@code compat.retroapi.RetroTweaksRetroInitializer}.
 */
public final class StationApiItemColors {

	@EventListener
	public void registerItemColors(ItemColorsRegisterEvent event) {
		if (event == null || event.itemColors == null) return;
		event.itemColors.register(
			// tintIndex is ignored: every face the models tint uses index 0, and answering the same
			// colour for any other index is harmless because nothing asks for one. White is the "no
			// tint" answer - a provider has to return something, and multiplying by white is a no-op.
			(stack, tintIndex) -> {
				// The player's CHOSEN value, not the effective Config field: with UniTweaks installed
				// the option is suppressed ("UniTweaks provides this"), but under StationAPI UniTweaks
				// provides nothing - arsenic replaces the item render path its mixins patch, which is
				// why UniTweaks 0.29.0 ships its own grass_block model renamed to .jsonx (disabled).
				// The models RetroTweaks ships load regardless, so a suppressed provider answering
				// white here is what turned the grass block item grey on UniTweaks+StationAPI installs.
				if (!ConfigManager.chosenBoolean("bugfixes.grassBlockItemFix", true)) return 0xFFFFFF;
				// Meta 0 of the tall-plant block is the dead shrub, which stays brown - the same
				// exception TallPlantColorMixin makes on the non-StationAPI path, so the two agree.
				// Only reachable because TallPlantBlockMixin keeps the variant on the sheared item.
				if (stack != null && stack.itemId == Block.GRASS.id
						&& stack.getDamage() == TallPlantVariants.DEAD_SHRUB) {
					return 0xFFFFFF;
				}
				return GrassColors.getColor(0.5, 1.0);
			},
			// Cast through Object because vanilla b1.7.3 blocks only implement ItemConvertible at
			// runtime, via StationAPI's own StationFlatteningBlock - there is no compile-time
			// relationship to declare, with the stubs or with the real artifact.
			(ItemConvertible) (Object) Block.GRASS_BLOCK,
			(ItemConvertible) (Object) Block.GRASS);
	}
}
