package com.periut.retroapi.register.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * A tiny food system, the beta-friendly take on modern's {@code .food(FoodComponent)}. Any
 * item becomes edible by being registered here (the builder does it for you via
 * {@code RetroItemAccess.food(...)}); no FoodItem subclass needed. Beta 1.7.3 predates the
 * hunger bar AND status effects, so a food restores HEALTH (hearts*2 points), and anything
 * fancier, a buff, a hit of damage, a teleport, rides an {@link OnEaten} callback that fires
 * right after the heal. That callback is the whole point: it lets a food do something other
 * than heal.
 */
public final class RetroFood {

	/** Runs the instant a food is eaten, after the heal. Use it for effects beyond healing. */
	@FunctionalInterface
	public interface OnEaten {
		void onEaten(ItemStack stack, World world, PlayerEntity player);
	}

	private record Props(int health, boolean meat, OnEaten onEaten) {
	}

	private static final Map<Item, Props> FOODS = new HashMap<>();

	private RetroFood() {
	}

	/** Registers an item as food restoring {@code health} points, with an optional eat effect. */
	public static void register(Item item, int health, boolean meat, OnEaten onEaten) {
		FOODS.put(item, new Props(health, meat, onEaten));
	}

	public static boolean isFood(Item item) {
		return FOODS.containsKey(item);
	}

	public static boolean isMeat(Item item) {
		Props p = FOODS.get(item);
		return p != null && p.meat;
	}

	/** Eats one from the stack: heal, then run the effect. Called by the use hook in ItemMixin. */
	public static ItemStack eat(ItemStack stack, World world, PlayerEntity player) {
		Props props = FOODS.get(stack.getItem());
		if (props == null) {
			return stack;
		}
		stack.count--;
		player.heal(props.health);
		if (props.onEaten != null) {
			props.onEaten.onEaten(stack, world, player);
		}
		return stack;
	}
}
