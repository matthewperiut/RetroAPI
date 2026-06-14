package com.periut.retroapi;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.block.Block;

/**
 * Breaks the order-sensitive vanilla static-init cycle before <em>any</em> mod's {@code init} runs.
 *
 * <p>{@code Item} and {@code Block} are mutually dependent at class-init time:
 * <ul>
 *   <li>{@code Item.<clinit>} builds tool items (e.g. {@code ShovelItem}) that reference effectiveness
 *       {@code Block}s, so it triggers {@code Block.<clinit>}; and</li>
 *   <li>{@code Block.<clinit>} reaches {@code Stats} -> {@code Achievements}, whose constructors build an
 *       {@code ItemStack} from {@code Item} statics, so it needs {@code Item.<clinit>} to have run.</li>
 * </ul>
 * Whichever class is touched first becomes the cycle's entry point. Entering from {@code Block} is the
 * order vanilla itself uses and resolves cleanly; entering from {@code Item} - which is what happens the
 * instant a mod constructs a modded item ({@code new MyItem(...)} triggers {@code Item.<clinit>}) before
 * anything has touched {@code Block} - reads an {@code Item} static that hasn't been assigned yet and
 * throws {@code NullPointerException: ... item is null} from deep inside {@code ItemStack.<init>}.
 *
 * <p>{@link RetroAPI#init()} already forces {@code Block} first, but that only helps if RetroAPI's
 * {@code init} happens to run before the consuming mod's - and the {@code init} stage order is not
 * guaranteed (a dependent mod can, and intermittently does, init first), which is exactly why the crash
 * was non-deterministic. {@code preLaunch} runs strictly before every {@code init} entrypoint, so forcing
 * {@code Block.<clinit>} here makes the safe order deterministic for RetroAPI and every mod built on it,
 * no matter the init order.
 */
public class RetroAPIPreLaunch implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		// Referencing a non-constant Block static forces Block.<clinit> to run to completion now, with
		// Item still untouched - so the cycle is entered from Block, the vanilla-safe direction.
		RetroAPI.LOGGER.info("RetroAPI preLaunch: forcing Block-first static init ({} block slots)", Block.BLOCKS.length);
	}
}
