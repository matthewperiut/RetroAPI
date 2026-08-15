package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.feature.flora.TallPlantVariants;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.TranslationStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies the names for the plants b1.7.3 never gave one, whether RetroTweaks invented the key or
 * Mojang did.
 *
 * <p>Two separate gaps meet here:
 *
 * <ul>
 *   <li><b>RetroTweaks' own three</b> ({@code tile.retrotweaks.dead_shrub}, {@code .short_grass},
 *       {@code .fern}). b1.7.3 has no way for a mod to add translations at all -
 *       {@code TranslationStorage} reads the game's own lang files and nothing else. RetroAPI ships a
 *       loader that scans every mod's {@code assets/<modid>/lang}, and RetroTweaks uses it for the
 *       fish names, but on a bare install nothing reads it, and under StationAPI the whole table is
 *       cleared and refilled from the resource manager on every reload.
 *   <li><b>Two of vanilla's own</b> ({@code tile.deadbush}, {@code tile.tallgrass}). Blocks 32 and 31
 *       set those keys in their constructors, and {@code lang/en_US.lang} translates neither, because
 *       neither block was obtainable in b1.7.3 - so nothing ever asked. RetroTweaks makes both
 *       obtainable (shears, and the craftable-dead-bush recipe), which is exactly what turns a key
 *       nobody looked up into a blank line in a tooltip. They are the only two vanilla block or item
 *       keys in the whole game with no {@code .name} entry; {@code tile.stoneSlab},
 *       {@code item.dyePowder} and {@code tile.diode} look missing too but resolve through their
 *       per-meta subkeys, which are all present.
 * </ul>
 *
 * <p><b>Both lookups, because they fail differently.</b> {@code get} answers an unknown key with the
 * key itself, so a missing name shows as "tile.retrotweaks.fern.name". {@code getClientTranslation} -
 * which appends {@code .name} itself, and is what the inventory tooltip actually calls - answers with
 * the EMPTY STRING, and {@code HandledScreen} then skips drawing a tooltip whose text is empty. That
 * second path is why an unnamed plant looked like it had no tooltip at all rather than an ugly one,
 * and why covering only {@code get} fixed nothing visible.
 *
 * <p>Either way the shim only answers when the key is genuinely untranslated, so a real lang entry
 * always wins and the shipped {@code en_US.lang} stays authoritative (and translatable) wherever a
 * loader does read it.
 *
 * <p>Deliberately narrow: it matches five exact keys and defers on everything else, so it is not a
 * general translation backdoor.
 */
@Environment(EnvType.CLIENT)
@Mixin(TranslationStorage.class)
public class TallPlantNameShimMixin {

	/** Vanilla keys with no {@code .name} in {@code lang/en_US.lang}, and what they should say. */
	@Unique
	private static final String[][] RETROTWEAKS_UNTRANSLATED_VANILLA = {
		// Block 32. Named apart from block 31's meta 0, which RetroTweaks calls the Dead Shrub even
		// though the two share a sprite - they are different blocks and behave differently.
		{ "tile.deadbush.name", "Dead Bush" },
		// Block 31 as a whole. Only reached with Tall Grass Items off; with it on, the per-meta keys
		// above take over before this is ever asked.
		{ "tile.tallgrass.name", "Tall Grass" },
	};

	/**
	 * The English name for a full {@code <key>.name} lookup, or null if this is not one of ours.
	 * The variant keys are read from {@link TallPlantVariants} rather than repeated here, so they
	 * live in exactly one place alongside the metas they name.
	 */
	@Unique
	private static String retrotweaks$plantName(String fullKey) {
		for (int meta = 0; meta <= TallPlantVariants.MAX_META; meta++) {
			if (fullKey.equals(TallPlantVariants.TRANSLATION_KEYS[meta] + ".name")) {
				return TallPlantVariants.NAMES[meta];
			}
		}
		for (String[] entry : RETROTWEAKS_UNTRANSLATED_VANILLA) {
			if (fullKey.equals(entry[0])) return entry[1];
		}
		return null;
	}

	@Inject(method = "get(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$plantNames(String key, CallbackInfoReturnable<String> cir) {
		// Untranslated is vanilla's "returned the key unchanged"; anything else is a real entry.
		if (!key.equals(cir.getReturnValue())) return;
		String name = retrotweaks$plantName(key);
		if (name != null) cir.setReturnValue(name);
	}

	@Inject(method = "getClientTranslation(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"),
		cancellable = true)
	private void retrotweaks$plantClientNames(String key, CallbackInfoReturnable<String> cir) {
		// Untranslated here is the empty string - this method's own default, not the key.
		String existing = cir.getReturnValue();
		if (existing != null && !existing.isEmpty()) return;
		String name = retrotweaks$plantName(key + ".name");
		if (name != null) cir.setReturnValue(name);
	}
}
