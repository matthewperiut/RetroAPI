package com.periut.retroapi.sound.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.world.World;

/**
 * A jukebox record (music disc) backed by a streamed ogg. The audio lives in the
 * STREAMING channel, so drop it at {@code assets/{mod}/sounds/streaming/{name}.ogg}
 * and pass the derived event id ({@code "{mod}:{name}"}) here.
 *
 * <p>This subclasses vanilla {@link MusicDiscItem} so the play mechanism is entirely
 * vanilla: {@code MusicDiscItem.useOnBlock} inserts the disc into a jukebox and fires
 * the {@code 1005} world event, whose handler looks up {@code Item.ITEMS[id]}, casts it
 * to {@code MusicDiscItem}, and calls {@code World.playStreaming(disc.sound, ...)}.
 * Because the disc is a real MusicDiscItem with our streaming id in {@code sound}, a
 * modded record plays exactly like a vanilla one, including the multiplayer broadcast.</p>
 *
 * <p><b>The "now playing" name.</b> Vanilla hard-codes the jukebox overlay to
 * {@code "C418 - " + disc.sound}, which for a modded disc reads as the raw streaming id.
 * This class fixes that the way aether-fabric-b1.7.3 does, by re-setting the overlay
 * right after the vanilla call, but pulls the text from the LANG file so it localises:
 * the key is {@code record.<namespace>.<name>} (derived from the streaming id). Add a
 * line like {@code record.@.sample_disc=Sample Track} to your {@code en_US.lang} and the
 * jukebox shows "Sample Track". With no such line it falls back to the raw id, so nothing
 * breaks if you forget.</p>
 */
public class RetroRecordItem extends MusicDiscItem {

	private final String descriptionKey;

	/**
	 * @param id          the placeholder item id (from {@code RetroItemAccess.allocateId()})
	 * @param streamingId the streaming sound event id, e.g. {@code "example_mod:sample_disc"}
	 */
	public RetroRecordItem(int id, String streamingId) {
		super(id, streamingId);
		this.maxCount = 1;
		// record.<namespace>.<name>, e.g. "record.example_mod.sample_disc".
		this.descriptionKey = "record." + streamingId.replace(':', '.');
	}

	/** The streaming sound event id this disc plays. */
	public String getStreamingId() {
		return this.sound;
	}

	/** The lang key the jukebox overlay is read from. */
	public String getDescriptionKey() {
		return this.descriptionKey;
	}

	@Override
	public boolean useOnBlock(ItemStack stack, PlayerEntity player, World world,
			int x, int y, int z, int side) {
		boolean used = super.useOnBlock(stack, player, world, x, y, z, side);
		// Only override the overlay when the disc actually went into a jukebox (vanilla
		// returns true). Client-only: the overlay is HUD text. The holder class keeps
		// client types off the server's class path entirely.
		if (used && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			ClientOverlay.set(this.descriptionKey);
		}
		return used;
	}

	/** Loaded only on the client, so the server never resolves InGameHud / TranslationStorage. */
	private static final class ClientOverlay {
		static void set(String key) {
			String text = net.minecraft.client.resource.language.TranslationStorage.getInstance().get(key);
			// get() returns the key unchanged when there is no translation; fall back then.
			net.ornithemc.osl.lifecycle.api.client.MinecraftInstance.get()
				.inGameHud.setRecordPlayingOverlay(text);
		}
	}
}
