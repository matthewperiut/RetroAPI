package com.periut.retroapi.register.block.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired when a player right-clicks a block, BEFORE the block's own {@code onUse} runs - the safe way to
 * give right-click behavior to a block that never declared any.
 *
 * <p>This exists because the obvious approach is a trap. Beta's {@code CropBlock} (and most blocks) does
 * not override {@code onUse} at all, so "add right-click harvest to crops" tempts you into mixing a fresh
 * {@code onUse} INTO {@code CropBlock}. That method then shadows {@code Block.onUse}, and every other
 * mod's {@code @Inject} into {@code Block.onUse} silently stops running for crops. The block worked, and
 * you broke someone else's mod from three dependencies away, with no error anywhere.
 *
 * <p>Listeners here compose instead. Register as many as you like, from as many mods as you like; they
 * run in registration order until one stops the chain:
 *
 * <pre>
 * BlockUseCallback.EVENT.register((player, world, held, x, y, z, face) -&gt; {
 *     if (world.getBlockId(x, y, z) != Block.WHEAT.id) return BlockUseCallback.Result.PASS;
 *     if (world.getBlockMeta(x, y, z) &lt; 7) return BlockUseCallback.Result.PASS;   // not grown yet
 *     if (!world.isRemote) harvestAndReplant(world, x, y, z);
 *     return BlockUseCallback.Result.SUCCESS;
 * });
 * </pre>
 *
 * <p><b>Where it fires.</b> On the side that actually decides the interaction: the client in
 * singleplayer (b1.7.3 singleplayer IS the client, there is no integrated server) and the dedicated
 * server in multiplayer. It is NOT fired by the multiplayer client's optimistic local copy, so a
 * listener never runs twice for one click and never has to guess which call is the real one. Guard world
 * writes with {@code !world.isRemote} anyway if your listener could ever be reached from client code.
 *
 * <p>Fires for EVERY block, including ones that do override {@code onUse} (chests, furnaces, doors), and
 * fires first - so returning anything but {@link Result#PASS} also lets you replace or veto vanilla
 * behavior, which is what a protection mod wants.
 */
public final class BlockUseCallback {

	/** What a listener decided, mirroring modern Minecraft's {@code ActionResult}. */
	public enum Result {
		/** Not my block: keep going, and fall through to vanilla if nobody claims it. */
		PASS,
		/** Handled. Vanilla's {@code onUse} is skipped and the interaction counts as a success. */
		SUCCESS,
		/** Handled by refusing. Vanilla's {@code onUse} is skipped and the interaction counts as a miss. */
		FAIL
	}

	@FunctionalInterface
	public interface Listener {
		/**
		 * @param player the player clicking
		 * @param world  the world the block is in
		 * @param held   the stack in the player's hand, or null for an empty hand
		 * @param face   the side of the block that was clicked (0-5, vanilla face order)
		 */
		Result onUseBlock(PlayerEntity player, World world, ItemStack held, int x, int y, int z, int face);
	}

	public static final Event<Listener> EVENT = Event.of(listeners ->
		(player, world, held, x, y, z, face) -> {
			for (Listener listener : listeners) {
				Result result = listener.onUseBlock(player, world, held, x, y, z, face);
				if (result != null && result != Result.PASS) {
					return result;
				}
			}
			return Result.PASS;
		});

	private BlockUseCallback() {}
}
