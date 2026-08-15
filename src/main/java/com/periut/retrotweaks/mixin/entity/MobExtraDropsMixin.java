package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
/**
 * Chosen drops for zombies and zombie pigmen. From MiscTweaks.
 *
 * <p>One mixin covers both because {@code PigZombieEntity} extends {@code ZombieEntity} and the two
 * source mods' versions differed only in the item table. Neither class declares {@code dropItems}
 * itself, so this is a plain override of {@code LivingEntity.dropItems} - which is where the vanilla
 * "{@code nextInt(3)} of {@code getDroppedItemId()}" roll (feathers, or cooked porkchops for a pigman)
 * actually lives, and what {@code super.dropItems()} below runs.
 *
 * <p>The two mobs deliberately differ in what their option does. A zombie's choice REPLACES the
 * feather: "what does a zombie leave behind" only has one answer, and a zombie carrying a feather and
 * a lump of clay was never what the option was for - so the vanilla roll is skipped and the same
 * {@code nextInt(3)} count is rolled for the chosen stack instead (leaving the choice on Feathers is
 * therefore exactly vanilla). A pigman's stays an EXTRA drop on top of its porkchops, which is what it
 * has always been.
 */
@Mixin(ZombieEntity.class)
public abstract class MobExtraDropsMixin extends LivingEntity {

	protected MobExtraDropsMixin(net.minecraft.world.World world) {
		super(world);
	}

	@Override
	protected void dropItems() {
		boolean pigman = (Object) this instanceof PigZombieEntity;

		if (!pigman && Config.MOBS.zombieDropItem) {
			retrotweaks$dropRolled(this.world.random.nextInt(3), false);
			return;
		}

		super.dropItems();

		if (!pigman || !Config.MOBS.pigmanDropItem) return;
		// GOLD_SWORD halves the roll (nextInt(2) instead of nextInt(3)) to slow down sword farming.
		// From MiscTweaks' ZombiePigmanMixin.dropItems.
		int count = Config.MOBS.pigmanDropChoice == Enums.PigmanDrop.GOLD_SWORD
				? this.world.random.nextInt(2)
				: this.world.random.nextInt(3);
		retrotweaks$dropRolled(count, true);
	}

	/** Drops {@code count} of the configured stack, built fresh each time so no two entities share one. */
	private void retrotweaks$dropRolled(int count, boolean pigman) {
		for (int i = 0; i < count; i++) {
			ItemStack drop = pigman ? retrotweaks$pigmanDrop() : retrotweaks$zombieDrop();
			if (drop != null) this.dropItem(drop, 0.0F);
		}
	}

	private ItemStack retrotweaks$zombieDrop() {
		Enums.ZombieDrop choice = Config.MOBS.zombieDropChoice;
		return switch (choice) {
			case FEATHER -> new ItemStack(Item.FEATHER, 1);
			case RED_MUSHROOM -> new ItemStack(Block.RED_MUSHROOM, 1);
			case CYAN_DYE -> new ItemStack(Item.DYE, 1, 6);
			case GREEN_DYE -> new ItemStack(Item.DYE, 1, 2);
			case CLAY -> new ItemStack(Item.CLAY, 1);
			case PAPER -> new ItemStack(Item.PAPER, 1);
			case NOTHING -> null;
		};
	}

	private ItemStack retrotweaks$pigmanDrop() {
		Enums.PigmanDrop choice = Config.MOBS.pigmanDropChoice;
		return switch (choice) {
			case COOKED_PORKCHOP -> new ItemStack(Item.COOKED_PORKCHOP, 1);
			case RAW_PORKCHOP -> new ItemStack(Item.RAW_PORKCHOP, 1);
			case BROWN_MUSHROOM -> new ItemStack(Block.BROWN_MUSHROOM, 1);
			// Damaged so it cannot be repaired into a pristine sword by farming pigmen.
			case GOLD_SWORD -> new ItemStack(Item.GOLDEN_SWORD, 1, this.world.random.nextInt(30));
			case BONE_MEAL -> new ItemStack(Item.DYE, 1, 15);
			case BRICK -> new ItemStack(Item.BRICK, 1);
			case GLOWSTONE_DUST -> new ItemStack(Item.GLOWSTONE_DUST, 1);
			case NOTHING -> null;
		};
	}
}
