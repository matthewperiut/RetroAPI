package com.periut.retroapi.registry;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.mixin.achievement.AchievementAccessor;
import com.periut.retroapi.mixin.recipe.CraftingManagerAccessor;
import com.periut.retroapi.mixin.recipe.ShapedRecipeAccessor;
import com.periut.retroapi.mixin.recipe.ShapelessRecipeAccessor;
import com.periut.retroapi.mixin.recipe.SmeltingManagerAccessor;
import com.periut.retroapi.register.recipe.RetroRecipes;
import com.periut.retroapi.register.recipe.RetroShapedRecipe;
import com.periut.retroapi.register.recipe.RetroShapelessRecipe;
import com.periut.retroapi.registry.event.IdRemapCallback;
import net.minecraft.achievement.Achievement;
import net.minecraft.achievement.Achievements;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SmeltingRecipeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The old-id -&gt; new-id table produced by an id assignment pass, and the sweep that repairs everything
 * still holding the old numbers.
 *
 * <h2>Why this exists</h2>
 * A mod registers its content during init, when RetroAPI hands out <em>provisional</em> ids. The real ids
 * arrive later: opening a world applies that world's saved {@code id_map.dat} ({@link IdAssigner#assignIds}),
 * and joining a server applies the server's table ({@link IdAssigner#applyFromNetwork}). Both can move a
 * block or item to a different number.
 *
 * <p>Every {@link ItemStack} built before that point stores the <em>provisional</em> number in its
 * {@code itemId} field, and an {@code ItemStack} is exactly how recipes, smelting results, fuels and
 * achievement icons are declared. Left alone, they keep pointing at whatever now lives at the old slot -
 * usually nothing - so a recipe silently stops matching even though it is still in the list. That is the
 * "my recipes exist but the game doesn't know them, and relaunching sometimes fixes it" bug: it depends
 * entirely on whether this session's provisional ids happened to match the world's saved ones.
 *
 * <p>So after a remap pass RetroAPI sweeps its own id-keyed tables ({@link #apply}) and fires
 * {@link IdRemapCallback} so mods can fix any {@code ItemStack} they cached themselves:
 * <pre>
 * IdRemapCallback.EVENT.register(remap -&gt; remap.fix(MyMod.RUBY_STACK));
 * </pre>
 */
public final class IdRemap {

	private final Map<Integer, Integer> changes = new LinkedHashMap<>();

	/** Records that everything referring to {@code oldId} must now refer to {@code newId}. */
	public void record(int oldId, int newId) {
		if (oldId != newId) {
			changes.put(oldId, newId);
		}
	}

	/** True when nothing actually moved (the common case: the world's ids already matched). */
	public boolean isEmpty() {
		return changes.isEmpty();
	}

	/** The number of ids that moved. */
	public int size() {
		return changes.size();
	}

	/** The new id for an old one, or the id unchanged if it did not move. */
	public int map(int id) {
		Integer mapped = changes.get(id);
		return mapped != null ? mapped : id;
	}

	/** An unmodifiable view of the old -&gt; new table. */
	public Map<Integer, Integer> asMap() {
		return java.util.Collections.unmodifiableMap(changes);
	}

	/**
	 * Repoints a stack at its item's new id. Safe to call on null, on vanilla stacks, and on stacks that
	 * did not move (all no-ops). Returns true if the stack was changed.
	 */
	public boolean fix(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		Integer mapped = changes.get(stack.itemId);
		if (mapped == null) {
			return false;
		}
		stack.itemId = mapped;
		return true;
	}

	/** {@link #fix(ItemStack)} for a whole array (entries may be null). Returns how many changed. */
	public int fix(ItemStack[] stacks) {
		if (stacks == null) {
			return 0;
		}
		int fixed = 0;
		for (ItemStack stack : stacks) {
			if (fix(stack)) fixed++;
		}
		return fixed;
	}

	/** {@link #fix(ItemStack)} for a list of stacks (entries may be null or non-stacks). */
	public int fix(List<?> stacks) {
		if (stacks == null) {
			return 0;
		}
		int fixed = 0;
		for (Object o : stacks) {
			if (o instanceof ItemStack stack && fix(stack)) fixed++;
		}
		return fixed;
	}

	/**
	 * Repairs everything RetroAPI knows about that stores a numeric id: crafting recipes (RetroAPI's and
	 * vanilla's), smelting inputs and outputs, fuel entries and achievement icons - then fires
	 * {@link IdRemapCallback} for mods, and re-sorts the crafting list so recipes registered after
	 * RetroAPI's own sort still land in vanilla's shaped-before-shapeless order.
	 *
	 * <p>No-op when nothing moved.
	 */
	public void apply() {
		if (changes.isEmpty()) {
			return;
		}
		int stacks = fixCraftingRecipes() + fixSmelting() + fixAchievements();
		fixFuels();

		IdRemapCallback.EVENT.invoker().onIdsRemapped(this);

		// Late-registered recipes (a mod that registered from its own init, after RetroAPI's init) have
		// never been through vanilla's ordering. This is the last moment before play, so sort here too.
		RetroRecipes.sortCraftingRecipes();

		RetroAPI.LOGGER.debug("Applied {} id change(s); repointed {} cached item stack(s)", changes.size(), stacks);
	}

	private int fixCraftingRecipes() {
		List<CraftingRecipe> recipes = ((CraftingManagerAccessor) CraftingRecipeManager.getInstance())
			.retroapi$getRecipes();
		int fixed = 0;
		for (CraftingRecipe recipe : recipes) {
			if (recipe instanceof RetroShapedRecipe retro) {
				fixed += fix(retro.getGrid());
				if (fix(retro.output)) fixed++;
			} else if (recipe instanceof RetroShapelessRecipe retro) {
				fixed += fix(retro.getIngredients());
				if (fix(retro.output)) fixed++;
			} else if (recipe instanceof ShapedRecipe shaped) {
				ShapedRecipeAccessor access = (ShapedRecipeAccessor) shaped;
				fixed += fix(access.retroapi$getInput());
				ItemStack output = access.retroapi$getOutput();
				if (fix(output)) {
					fixed++;
					access.retroapi$setOutputId(output.itemId);
				}
			} else if (recipe instanceof ShapelessRecipe shapeless) {
				ShapelessRecipeAccessor access = (ShapelessRecipeAccessor) shapeless;
				fixed += fix(access.retroapi$getInput());
				if (fix(access.retroapi$getOutput())) fixed++;
			}
		}
		return fixed;
	}

	@SuppressWarnings("unchecked")
	private int fixSmelting() {
		Map<Integer, ItemStack> recipes =
			((SmeltingManagerAccessor) SmeltingRecipeManager.getInstance()).retroapi$getRecipes();
		int fixed = 0;
		// Inputs are map KEYS, so rebuild rather than mutate in place.
		Map<Integer, ItemStack> rebuilt = new HashMap<>();
		boolean keysMoved = false;
		for (Map.Entry<Integer, ItemStack> entry : recipes.entrySet()) {
			int input = map(entry.getKey());
			keysMoved |= input != entry.getKey();
			if (fix(entry.getValue())) fixed++;
			rebuilt.put(input, entry.getValue());
		}
		if (keysMoved) {
			recipes.clear();
			recipes.putAll(rebuilt);
			fixed += rebuilt.size();
		}
		return fixed;
	}

	private void fixFuels() {
		RetroRecipes.remapFuelIds(this);
	}

	private int fixAchievements() {
		int fixed = 0;
		List<?> achievements = new ArrayList<>(Achievements.ACHIEVEMENTS);
		for (Object o : achievements) {
			if (o instanceof Achievement achievement
				&& fix(((AchievementAccessor) achievement).retroapi$getIcon())) {
				fixed++;
			}
		}
		return fixed;
	}
}
