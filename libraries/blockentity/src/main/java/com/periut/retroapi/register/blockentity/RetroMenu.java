package com.periut.retroapi.register.blockentity;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.menu.InventoryMenu;
import net.minecraft.inventory.menu.InventoryMenuListener;
import net.minecraft.inventory.slot.InventorySlot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for inventory menus with automatic {@link SyncField} support.
 * <p>
 * Subclass this instead of {@link InventoryMenu} to get automatic syncing
 * of integer fields between the block entity (server) and the menu (client).
 * Annotate {@code int} fields with {@link SyncField} to sync them.
 * <p>
 * Example:
 * <pre>
 * public class CrateMenu extends RetroMenu {
 *     &#64;SyncField("progress")
 *     public int progress;
 *
 *     public CrateMenu(PlayerInventory playerInv, CrateBlockEntity crate) {
 *         super(crate);
 *         // add slots...
 *     }
 * }
 * </pre>
 * <p>
 * Open a RetroMenu via {@link #open(PlayerEntity, RetroMenu)} or
 * {@link #open(PlayerEntity, RetroMenu, int)}.
 */
public abstract class RetroMenu extends InventoryMenu {
	private final Object blockEntity;
	private final SyncEntry[] syncEntries;

	protected RetroMenu(Object blockEntity) {
		this.blockEntity = blockEntity;
		this.syncEntries = discoverSyncFields();
	}

	public Object getBlockEntity() {
		return blockEntity;
	}

	/**
	 * Read the current values of all @SyncField-annotated block entity fields.
	 * Returns an array where index i corresponds to the i-th @SyncField in declaration order.
	 */
	public int[] readSyncValues() {
		int[] values = new int[syncEntries.length];
		for (int i = 0; i < syncEntries.length; i++) {
			values[i] = syncEntries[i].getBlockEntityValue(blockEntity);
		}
		return values;
	}

	public int getSyncCount() {
		return syncEntries.length;
	}

	private SyncEntry[] discoverSyncFields() {
		List<SyncEntry> entries = new ArrayList<>();
		for (Field menuField : getClass().getDeclaredFields()) {
			SyncField annotation = menuField.getAnnotation(SyncField.class);
			if (annotation == null) continue;
			if (menuField.getType() != int.class) {
				throw new IllegalStateException("@SyncField can only be applied to int fields, but " +
					menuField.getName() + " is " + menuField.getType().getName());
			}
			String beFieldName = annotation.value();
			Field beField;
			try {
				beField = blockEntity.getClass().getDeclaredField(beFieldName);
			} catch (NoSuchFieldException e) {
				throw new IllegalStateException("@SyncField(\"" + beFieldName + "\") on " +
					menuField.getName() + ": no such field on " + blockEntity.getClass().getName(), e);
			}
			if (beField.getType() != int.class) {
				throw new IllegalStateException("@SyncField(\"" + beFieldName + "\"): field must be int, but is " +
					beField.getType().getName());
			}
			menuField.setAccessible(true);
			beField.setAccessible(true);
			entries.add(new SyncEntry(menuField, beField));
		}
		return entries.toArray(new SyncEntry[0]);
	}

	@Override
	public void addListener(InventoryMenuListener listener) {
		super.addListener(listener);
		for (int i = 0; i < syncEntries.length; i++) {
			listener.onDataChanged(this, i, syncEntries[i].getBlockEntityValue(blockEntity));
		}
	}

	@Override
	public void updateListeners() {
		super.updateListeners();
		for (int i = 0; i < syncEntries.length; i++) {
			int beValue = syncEntries[i].getBlockEntityValue(blockEntity);
			int cachedValue = syncEntries[i].getMenuValue(this);
			if (beValue != cachedValue) {
				for (Object obj : listeners) {
					((InventoryMenuListener) obj).onDataChanged(this, i, beValue);
				}
				syncEntries[i].setMenuValue(this, beValue);
			}
		}
	}

	@Override
	public void setData(int id, int value) {
		if (id >= 0 && id < syncEntries.length) {
			syncEntries[id].setBlockEntityValue(blockEntity, value);
		}
	}

	/** Menu type constant for chest-style GUI. */
	public static final int MENU_CHEST = 0;
	/** Menu type constant for furnace-style GUI (input + fuel + result + progress bars). */
	public static final int MENU_FURNACE = 2;
	/** Menu type constant for dispenser-style GUI. */
	public static final int MENU_DISPENSER = 3;

	@FunctionalInterface
	public interface MenuOpener {
		void open(PlayerEntity player, RetroMenu menu, int menuType);
	}

	private static MenuOpener serverOpener;
	private static MenuOpener clientOpener;

	public static void setServerOpener(MenuOpener opener) {
		serverOpener = opener;
	}

	public static void setClientOpener(MenuOpener opener) {
		clientOpener = opener;
	}

	/**
	 * Open a RetroMenu for a player using the chest GUI.
	 */
	public static void open(PlayerEntity player, RetroMenu menu) {
		open(player, menu, MENU_CHEST);
	}

	/**
	 * Open a RetroMenu for a player.
	 * Automatically handles server (dedicated) vs client (singleplayer) environments.
	 *
	 * @param player   the player to open the menu for
	 * @param menu     the menu instance (must have slots already added)
	 * @param menuType the GUI type (use MENU_CHEST, MENU_FURNACE, or MENU_DISPENSER)
	 */
	public static void open(PlayerEntity player, RetroMenu menu, int menuType) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			if (serverOpener != null) {
				serverOpener.open(player, menu, menuType);
			}
		} else {
			if (clientOpener != null) {
				clientOpener.open(player, menu, menuType);
			}
		}
	}

	private static class SyncEntry {
		private final Field menuField;
		private final Field beField;

		SyncEntry(Field menuField, Field beField) {
			this.menuField = menuField;
			this.beField = beField;
		}

		int getBlockEntityValue(Object be) {
			try {
				return beField.getInt(be);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		void setBlockEntityValue(Object be, int value) {
			try {
				beField.setInt(be, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		int getMenuValue(RetroMenu menu) {
			try {
				return menuField.getInt(menu);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		void setMenuValue(RetroMenu menu, int value) {
			try {
				menuField.setInt(menu, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
