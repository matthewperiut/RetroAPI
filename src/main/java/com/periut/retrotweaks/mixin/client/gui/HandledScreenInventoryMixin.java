/*
 * Ported from InventoryTweaks by Telvarost, which itself merged MouseTweaks. The click, drag and
 * scroll behaviour is unchanged; the StationAPI smelting/fuel lookups were swapped for vanilla ones
 * so this works with no API installed.
 */
package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.inventory.PriorityShiftClick;
import com.periut.retrotweaks.feature.inventory.SlotHelper;
import com.periut.retrotweaks.feature.inventory.WheelTweak;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ArmorItem;
import net.minecraft.recipe.SmeltingRecipeManager;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.DispenserScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;

import static java.lang.Math.abs;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenInventoryMixin extends Screen {

	@Shadow
	protected int backgroundWidth;

	@Shadow
	protected int backgroundHeight;

	@Shadow
	public net.minecraft.screen.ScreenHandler handler;

	@Shadow
	protected abstract Slot getSlotAt(int x, int y);

	@Shadow
	protected abstract boolean isPointOverSlot(Slot slot, int x, int Y);

	@Unique long currentTime;

	@Unique private Slot slot;

	@Unique Slot lastRMBSlot = null;

	@Unique Slot lastLMBSlot = null;

	@Unique int lastRMBSlotId = -1;

	@Unique int lastLMBSlotId = -1;

	@Unique
	private ItemStack leftClickMouseTweaksPersistentStack = null;

	@Unique
	private ItemStack leftClickPersistentStack = null;

	@Unique
	private ItemStack rightClickPersistentStack = null;

	@Unique
	private boolean isLeftClickDragMouseTweaksStarted = false;

	@Unique
	private boolean isLeftClickDragStarted = false;

	@Unique
	private boolean isRightClickDragStarted = false;

	@Unique
	private final List<Slot> leftClickHoveredSlots = new ArrayList<>();

	@Unique final List<Slot> rightClickHoveredSlots = new ArrayList<>();

	@Unique Integer leftClickItemAmount;

	@Unique Integer rightClickItemAmount;

	@Unique final List<Integer> leftClickExistingAmount = new ArrayList<>();

	/**
	 * True for the two kinds of slot RetroAPI's creative screen owns outright - a bottomless picker
	 * template, and the bin.
	 *
	 * <p>Per SLOT, not per screen. The creative screen carries the player's own inventory too, and
	 * there it is an ordinary inventory: dragging a stack across it, splitting one over several slots,
	 * shift-clicking - all of that should work exactly as it does in a chest, and standing the whole
	 * file down on the screen threw it away for no reason.
	 *
	 * <p>What genuinely cannot follow these rules is the other two. Every tweak here works by sending
	 * {@code clickSlot} for the server to carry out as a MOVE; a picker slot mints a fresh stack when
	 * clicked and the bin destroys what it is handed, so a right-drag sweeping across the item list
	 * would conjure a row of items rather than distribute one. Those slots belong to the creative
	 * handler alone - see {@code CreativeScreenHandler.isCreativeSlot}.
	 */
	@Unique
	private boolean retrotweaks$creativeOwns(Slot candidate) {
		return candidate != null
			&& com.periut.retroapi.gamemode.screen.CreativeScreenHandler.isCreativeSlot(candidate);
	}

	@Unique final List<Integer> rightClickExistingAmount = new ArrayList<>();

	@Unique List<Integer> leftClickAmountToFillPersistent = new ArrayList<>();

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	protected void retrotweaks$mouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
		Slot clickedSlot = this.getSlotAt(mouseX, mouseY);
		if (retrotweaks$creativeOwns(clickedSlot)) return;
		retrotweaks$beginLeftClickDragMouseTweaks(button, clickedSlot);

		/** - Handle crafting slot click */
		if (clickedSlot instanceof CraftingResultSlot) {
			/** - Handle ctrl click crafting */
			if (Config.INVENTORY.crafting.ctrlClickCrafting) {
				if (retrotweaks$handleCtrlClickCrafting(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}

			/** - Handle right click crafting */
			if (Config.INVENTORY.crafting.rightClickCrafting) {
				if (retrotweaks$handleRightClickCrafting(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}

			/** - Handle shift click crafting */
			if (Config.INVENTORY.crafting.shiftClickCrafting) {
				if (retrotweaks$handleShiftClickCrafting(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}
		}

		/** - Check special click behavior for current screen */
		if (minecraft.currentScreen instanceof InventoryScreen) {
			/** - Handle shift click into armor slots */
			if (Config.INVENTORY.modern.shiftClickIntoArmorSlots) {
				if (retrotweaks$handleShiftClickIntoArmorSlots(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}
		} else if (minecraft.currentScreen instanceof FurnaceScreen) {
			/** - Handle shift click into furnace */
			if (Config.INVENTORY.modern.shiftClickIntoFurnaces) {
				if (retrotweaks$handleShiftClickIntoFurnace(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}
		}

		/** - Check if client is on a server */
		boolean isClientOnServer = minecraft.world.isRemote;

		/** - Right-click */
		if (button == 1) {
			boolean exitFunction = false;

			/** - Should click cancel Left-click + Drag */
			if (!retrotweaks$cancelLeftClickDrag(isClientOnServer)) {

				/** - Handle Right-click */
				if (Config.INVENTORY.drag.rightClickDrag) {
					exitFunction = retrotweaks$handleRightClick(mouseX, mouseY, clickedSlot);
				}
			} else {
				exitFunction = true;
			}

			if (exitFunction) {
				/** - Handle if a button was clicked */
				super.mouseClicked(mouseX, mouseY, button);
				ci.cancel();
				return;
			}
		}

		/** - Left-click */
		if (button == 0) {
			boolean exitFunction = false;

			/** - Should click cancel Right-click + Drag */
			if (!retrotweaks$cancelRightClickDrag(isClientOnServer)) {

				/** - Handle Left-click */
				ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
				if (cursorStack != null) {
					/** - Check for double left-click fill cursor stack (checking for second click close to time of first click) */
					if (Config.INVENTORY.modern.doubleClickCollect) {
						if (null != clickedSlot && false == clickedSlot.hasStack() && null != minecraft.world) {
							if (5 > (minecraft.world.getTime() - currentTime)) {
								if (retrotweaks$handleDoubleClickEmptyCursor(mouseX, mouseY, button, clickedSlot)) {
									/** - Handle if a button was clicked */
									super.mouseClicked(mouseX, mouseY, button);
									ci.cancel();
									return;
								}
							}
						}
					}

					if (Config.INVENTORY.drag.leftClickDrag) {
						exitFunction = retrotweaks$handleLeftClickWithItem(cursorStack, clickedSlot, isClientOnServer);
					}
				} else {
					/** - Begin double left-click fill cursor stack (first click registered) */
					if (Config.INVENTORY.modern.doubleClickCollect) {
						if (null != clickedSlot && clickedSlot.hasStack() && null != minecraft.world) {
							currentTime = minecraft.world.getTime();
						}
					}

					/** - Handle Glass Inventory Tweaks' priority-routed [Shift-Click] */
					if (Config.INVENTORY.modern.prioritySlotShiftClick && retrotweaks$isPriorityShiftClickEligible(clickedSlot)) {
						exitFunction = PriorityShiftClick.shiftClick(clickedSlot, handler);
					} else {
						exitFunction = retrotweaks$handleLeftClickWithoutItem(clickedSlot);
					}
				}
			} else {
				exitFunction = true;
			}

			if (exitFunction) {
				/** - Handle if a button was clicked */
				super.mouseClicked(mouseX, mouseY, button);
				ci.cancel();
				return;
			}
		}
	}

	/**
	 * MouseTweaks arms its `Left-Click + Drag` on the press itself, before anything else looks at the
	 * click. Every shortcut below cancels the event once it acts, so arming the drag here is what keeps
	 * a shift-drag alive when the first click landed somewhere one of them claimed - a chest slot taken
	 * by priority [Shift-Click], say, which is where most of a shift-drag happens.
	 */
	@Unique private void retrotweaks$beginLeftClickDragMouseTweaks(int button, Slot clickedSlot) {
		isLeftClickDragMouseTweaksStarted = false;

		if (button != 0 || minecraft.player.inventory.getCursorStack() != null) return;

		isLeftClickDragMouseTweaksStarted = true;

		/** - With shift held the drag shift-clicks whatever it crosses, so it remembers no item on
		 *    purpose; without it the drag only sweeps up more of the item the first click picked up. */
		boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		leftClickMouseTweaksPersistentStack = (!isShiftKeyDown && clickedSlot != null) ? clickedSlot.getStack() : null;

		/** - The slot the press landed on is dealt with by whichever handler claims the click, so the
		 *    drag must not act on it again when the mouse wobbles inside it. */
		lastLMBSlot = clickedSlot;
		lastLMBSlotId = (clickedSlot != null) ? clickedSlot.id : -1;
	}

	@Unique private boolean retrotweaks$handleCtrlClickCrafting(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isCtrlKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
		/** - Ctrl-click */
		if (true == isCtrlKeyDown && clickedSlot.hasStack()) {
			int maxStackSize = clickedSlot.getStack().getMaxCount();
			int numCrafted = 0;
			for (int craftingAttempts = 0; craftingAttempts < 256; craftingAttempts++) {
				if (clickedSlot.hasStack() && numCrafted < maxStackSize) {
					numCrafted += clickedSlot.getStack().count;
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, true, this.minecraft.player);
				} else {
					break;
				}
			}
			return true;
		}

		return false;
	}

	@Unique private boolean retrotweaks$handleRightClickCrafting(int mouseX, int mouseY, int button, Slot clickedSlot) {
		/** - Right-click */
		if (button == 1 && clickedSlot.hasStack()) {
			/** - Abort and do normal shift key crafting if shift key is down */
			boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
			if (!isShiftKeyDown) {
				int maxStackSize = clickedSlot.getStack().getMaxCount();
				int numCrafted = 0;
				for (int craftingAttempts = 0; craftingAttempts < 256; craftingAttempts++) {
					if (clickedSlot.hasStack() && numCrafted < maxStackSize) {
						numCrafted += clickedSlot.getStack().count;
						retrotweaks$internalMouseClicked(mouseX, mouseY, button);
					} else {
						break;
					}
				}
				return true;
			}
		}

		return false;
	}

	@Unique private boolean retrotweaks$handleShiftClickCrafting(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		if (true == isShiftKeyDown && clickedSlot.hasStack()) {
			int itemId = clickedSlot.getStack().itemId;
			for (int craftingAttempts = 0; craftingAttempts < 256; craftingAttempts++) {
				if (Config.INVENTORY.crafting.stopShiftClickWhenItemChanges) {
					if (clickedSlot.hasStack() && itemId == clickedSlot.getStack().itemId) {
						retrotweaks$internalMouseClicked(mouseX, mouseY, button);
					} else {
						break;
					}
				} else {
					if (clickedSlot.hasStack()) {
						retrotweaks$internalMouseClicked(mouseX, mouseY, button);
					} else {
						break;
					}
				}
			}
			return true;
		}

		return false;
	}

	@Unique private boolean retrotweaks$handleShiftClickIntoArmorSlots(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		if (isShiftKeyDown) {

			if (null != clickedSlot && clickedSlot.hasStack()) {
				InventoryScreen inventoryScreen = (InventoryScreen) minecraft.currentScreen;

				if (  (clickedSlot != ((Slot)inventoryScreen.handler.slots.get(5)))
				   && (clickedSlot != ((Slot)inventoryScreen.handler.slots.get(6)))
				   && (clickedSlot != ((Slot)inventoryScreen.handler.slots.get(7)))
				   && (clickedSlot != ((Slot)inventoryScreen.handler.slots.get(8)))
				) {
					ItemStack slotStack = clickedSlot.getStack();
					int shiftToSlot = -1;
					boolean isPumpkin = false;

					if (slotStack.getItem() instanceof ArmorItem) {
						int equipmentSlot = ((ArmorItem)slotStack.getItem()).equipmentSlot;

						if (0 == equipmentSlot) {
							if (false == ((Slot)inventoryScreen.handler.slots.get(5)).hasStack()) {
								shiftToSlot = 5;
							}
						} else if (1 == equipmentSlot) {
							if (false == ((Slot)inventoryScreen.handler.slots.get(6)).hasStack()) {
								shiftToSlot = 6;
							}
						} else if (2 == equipmentSlot) {
							if (false == ((Slot)inventoryScreen.handler.slots.get(7)).hasStack()) {
								shiftToSlot = 7;
							}
						} else if (3 == equipmentSlot) {
							if (false == ((Slot)inventoryScreen.handler.slots.get(8)).hasStack()) {
								shiftToSlot = 8;
							}
						}
					} else if (Block.PUMPKIN.id == slotStack.itemId) {
						if (false == ((Slot)inventoryScreen.handler.slots.get(5)).hasStack()) {
							shiftToSlot = 5;
							isPumpkin = true;
						}
					}

					if (0 <= shiftToSlot) {
						if (null != minecraft.player.inventory.getCursorStack()) {
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)inventoryScreen.handler.slots.get(shiftToSlot)).id, button, false, this.minecraft.player);
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
						} else {
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)inventoryScreen.handler.slots.get(shiftToSlot)).id, button, false, this.minecraft.player);

							if (isPumpkin) {
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, button, false, this.minecraft.player);
							}
						}

						if (isPumpkin) {
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, button, true, this.minecraft.player);
						}

						return true;
					}
				}
			}
		}

		return false;
	}

	@Unique private boolean retrotweaks$handleShiftClickIntoFurnace(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		if (isShiftKeyDown) {

			if (null != clickedSlot && clickedSlot.hasStack()) {
				FurnaceScreen furnaceScreen = (FurnaceScreen) minecraft.currentScreen;

				if (  (clickedSlot != ((Slot)furnaceScreen.handler.slots.get(0)))
				   && (clickedSlot != ((Slot)furnaceScreen.handler.slots.get(1)))
				   && (clickedSlot != ((Slot)furnaceScreen.handler.slots.get(2)))
				) {
					try {
						ItemStack slotStack = clickedSlot.getStack();
						int shiftToSlot = -1;

						// Smeltable goes to the input slot, fuel goes to the fuel slot, and -2 means
						// "belongs here but there is no room", which stops the click falling through
						// to vanilla's shift-click and flinging the stack somewhere unhelpful.
						if (null != SlotHelper.getSmeltingResult(slotStack)) {
							shiftToSlot = 0 <= SlotHelper.canItemFitInSlot(slotStack, ((Slot)furnaceScreen.handler.slots.get(0))) ? 0 : -2;
						} else if (0 < SlotHelper.getFuelTime(slotStack)) {
							shiftToSlot = 0 <= SlotHelper.canItemFitInSlot(slotStack, ((Slot)furnaceScreen.handler.slots.get(1))) ? 1 : -2;
						}

						if (-2 == shiftToSlot) {
							return true;
						} else if (0 <= shiftToSlot) {
							if (null != minecraft.player.inventory.getCursorStack()) {
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)furnaceScreen.handler.slots.get(shiftToSlot)).id, button, false, this.minecraft.player);
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
							} else {
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)furnaceScreen.handler.slots.get(shiftToSlot)).id, button, false, this.minecraft.player);
								if (null != minecraft.player.inventory.getCursorStack()) {
									this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, button, false, this.minecraft.player);
								}
							}

							return true;
						}
					} catch (Exception ex) {
						/* Do nothing */
					}
				}
			}
		}

		return false;
	}

	@Unique private boolean retrotweaks$handleDoubleClickEmptyCursor(int mouseX, int mouseY, int button, Slot clickedSlot) {
		if (null != clickedSlot) {
			HandledScreen handledScreen = (HandledScreen) minecraft.currentScreen;

			ItemStack slotStack = minecraft.player.inventory.getCursorStack();
			int playerInventorySlotIndex;

			/** - Shift item back into player inventory */
			for (playerInventorySlotIndex = 0; playerInventorySlotIndex < handledScreen.handler.slots.size(); playerInventorySlotIndex++) {

				if (SlotHelper.isItemInSlot(slotStack, ((Slot)handledScreen.handler.slots.get(playerInventorySlotIndex)))) {
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)handledScreen.handler.slots.get(playerInventorySlotIndex)).id, 0, false, this.minecraft.player);
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)handledScreen.handler.slots.get(playerInventorySlotIndex)).id, 0, false, this.minecraft.player);

					ItemStack itemStack = ((Slot)handledScreen.handler.slots.get(playerInventorySlotIndex)).getStack();
					if (  null != itemStack
					   && itemStack.getItem().getMaxCount() == itemStack.count
					) {
						this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 0, false, this.minecraft.player);
						this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)handledScreen.handler.slots.get(playerInventorySlotIndex)).id, 0, false, this.minecraft.player);
						this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 0, false, this.minecraft.player);
						this.minecraft.interactionManager.clickSlot(this.handler.syncId, ((Slot)handledScreen.handler.slots.get(playerInventorySlotIndex)).id, 0, false, this.minecraft.player);
						this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 0, false, this.minecraft.player);

						/** - Stack is full */
						break;
					}
				}

				ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
				if (  null != cursorStack
				   && cursorStack.getItem().getMaxCount() == cursorStack.count
				) {
					/** - Stack is full */
					break;
				}
			}

			/** - Still pick up the item even if no items are shifted */
			return true;
		}

		return false;
	}

	@Unique private void retrotweaks$internalMouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 || button == 1) {
			Slot var4 = this.getSlotAt(mouseX, mouseY);
			int var5 = (this.width - this.backgroundWidth) / 2;
			int var6 = (this.height - this.backgroundHeight) / 2;
			boolean var7 = mouseX < var5 || mouseY < var6 || mouseX >= var5 + this.backgroundWidth || mouseY >= var6 + this.backgroundHeight;
			int var8 = -1;
			if (var4 != null) {
				var8 = var4.id;
			}

			if (var7) {
				var8 = -999;
			}

			if (var8 != -1) {
				boolean var9 = var8 != -999 && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));
				this.minecraft.interactionManager.clickSlot(this.handler.syncId, var8, button, var9, this.minecraft.player);
			}
		}
	}

	@Inject(method = "mouseReleased", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$mouseReleasedOrSlotChanged(int mouseX, int mouseY, int button, CallbackInfo ci) {
		slot = this.getSlotAt(mouseX, mouseY);
		// Before the wheel is read, and before any drag step: over the creative item list the wheel
		// scrolls that list, and Mouse.getDWheel() below would eat the movement first. This also keeps
		// a drag that began in the player's inventory from clicking picker slots as it sweeps across
		// them - it does nothing while the cursor is over one, and carries on when it leaves.
		if (retrotweaks$creativeOwns(slot)) return;

		/** - Do nothing if mouse is not over a slot */
		if (slot == null)
			return;

		/** - MouseTweaks' wheel tweak drives plain clicks, so it is the only one of the three that also
		 *    runs on a server; the other two edit stacks directly and stay in singleplayer. The wheel is
		 *    only read when one of them could act on it, so nothing else on screen loses its scroll. */
		boolean localScrollAvailable = !minecraft.world.isRemote
			&& (Config.INVENTORY.scroll.enableScrollWheelTweaks || Config.INVENTORY.scroll.priorityRoutedScroll);

		if (Config.INVENTORY.scroll.inventoryTransferScroll || localScrollAvailable) {
			int currentWheelDegrees = Mouse.getDWheel();
			if ((0 != currentWheelDegrees)
					&& (isLeftClickDragStarted == false)
					&& (isRightClickDragStarted == false)
			) {
				/** - Glass Inventory Tweaks' priority-routed scroll takes the notch first, then
				 *    MouseTweaks' between-inventory wheel, then InventoryTweaks' cursor/slot transfer,
				 *    each only if the one before it declined. */
				boolean handled = localScrollAvailable
					&& Config.INVENTORY.scroll.priorityRoutedScroll
					&& PriorityShiftClick.handleScroll(handler, slot);

				if (!handled && Config.INVENTORY.scroll.inventoryTransferScroll) {
					handled = WheelTweak.handleScroll(handler, slot, currentWheelDegrees);
				}

				if (!handled && localScrollAvailable && Config.INVENTORY.scroll.enableScrollWheelTweaks) {
					retrotweaks$handleScrollWheel(currentWheelDegrees);
				}
			}
		}

		/** - Right-click + Drag logic = distribute one item from held items to each slot */
		if (  ( button == -1 )
		   && ( Mouse.isButtonDown(1) )
		   && ( isLeftClickDragStarted == false )
		   && ( isLeftClickDragMouseTweaksStarted == false )
		   && ( rightClickPersistentStack != null )
		) {
			ItemStack slotItemToExamine = slot.getStack();

			/** - Do nothing if slot item does not match held item or if the slot is full */
			if (  (null != slotItemToExamine)
			   && (  (!slotItemToExamine.isItemEqual(rightClickPersistentStack))
				  || (slotItemToExamine.count == rightClickPersistentStack.getMaxCount())
			      )
			) {
				return;
			}

			/** - Do nothing if there are no more items to distribute */
			ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
			if (null == cursorStack) {
				return;
			}

			if (!rightClickHoveredSlots.contains(slot)) {
				retrotweaks$handleRightClickDrag(slotItemToExamine);
			} else if (Config.INVENTORY.drag.rmbDragOverFilledSlots) {
				retrotweaks$handleRightClickDragMouseTweaks();
			}
		} else {
			retrotweaks$resetRightClickDragVariables();
		}

		/** - Left-click + Drag logic = evenly distribute held items over slots */
		if (  ( button == -1 )
		   && ( Mouse.isButtonDown(0) )
		   && ( isRightClickDragStarted == false )
		) {
			if (isLeftClickDragMouseTweaksStarted) {
				retrotweaks$handleLeftClickDragMouseTweaks();
			} else if ( leftClickPersistentStack != null ) {
				if (retrotweaks$handleLeftClickDrag()) {
					return;
				}
			} else {
				retrotweaks$resetLeftClickDragVariables();
			}
		} else {
			retrotweaks$resetLeftClickDragVariables();
		}
	}

	@Unique private void retrotweaks$handleScrollWheel(int wheelDegrees) {
		ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
		ItemStack slotItemToExamine = slot.getStack();

		if (  (null != cursorStack)
		   || (null != slotItemToExamine)
		   )
		{
			//boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
			boolean transferAllowed = true;
			float numberOfTurns = (float)wheelDegrees / 120.0f;
			int cursorStackAmount = 0;
			int slotStackAmount = 0;
			ItemStack itemBeingTransfered = null;

			if (null != cursorStack) {
				itemBeingTransfered = cursorStack;
				cursorStackAmount = cursorStack.count;
			}

			if (null != slotItemToExamine) {
				itemBeingTransfered = slotItemToExamine;
				slotStackAmount = slotItemToExamine.count;
			}

			/** - Allow transfers if one or both of the slots are empty */
			if (  (null != cursorStack)
			   && (null != slotItemToExamine)
			) {
				/** - Prevent transfers if items in slots do not match */
				transferAllowed = cursorStack.isItemEqual(slotItemToExamine);
			}

			/** - Prevent illegal transfers that can cause bugs/dupes */
			if (  (slot.id == 0) && (handler instanceof CraftingScreenHandler)
			   || (slot.id == 2) && (handler instanceof FurnaceScreenHandler)
			   || (  (handler instanceof PlayerScreenHandler)
				  && (  (slot.id == 0)
					 ||	(slot.id == 5)
					 ||	(slot.id == 6)
					 ||	(slot.id == 7)
					 ||	(slot.id == 8)
				     )
			      )
			) {
				transferAllowed = false;
			}

			if (transferAllowed) {
				retrotweaks$scrollCursorSlotTransfer(numberOfTurns, cursorStackAmount, slotStackAmount, itemBeingTransfered);
			}
//			if (isShiftKeyDown) {
//				if (Config.ScrollWheelConfig.shiftScrollWheelBehavior) {
//					retrotweaks$scrollInventoryTransfer(numberOfTurns, cursorStackAmount, slotStackAmount, itemBeingTransfered);
//				} else {
//					retrotweaks$scrollCursorSlotTransfer(numberOfTurns, cursorStackAmount, slotStackAmount, itemBeingTransfered);
//				}
//			} else {
//				if (Config.ScrollWheelConfig.scrollWheelBehavior) {
//					retrotweaks$scrollCursorSlotTransfer(numberOfTurns, cursorStackAmount, slotStackAmount, itemBeingTransfered);
//				} else {
//					retrotweaks$scrollInventoryTransfer(numberOfTurns, cursorStackAmount, slotStackAmount, itemBeingTransfered);
//				}
//			}
		}
	}

	@Unique private void retrotweaks$scrollCursorSlotTransfer(float numTurns, int cursorAmount, int slotAmount, ItemStack transferItem) {
		if (Config.INVENTORY.scroll.invertCursorSlotDirection) {
			numTurns *= -1;
		}

		if (0 > numTurns) {
			/** - Transfer items to slot from cursor */
			if (0 != cursorAmount) {
				for (int turnIndex = 0; turnIndex < abs(numTurns); turnIndex++) {
					if (slotAmount != transferItem.getMaxCount()) {
						if (0 == (cursorAmount - 1)) {
							minecraft.player.inventory.setCursorStack(null);
						} else {
							minecraft.player.inventory.setCursorStack(new ItemStack(transferItem.itemId, (cursorAmount - 1), transferItem.getDamage()));
						}
						slot.setStack(new ItemStack(transferItem.itemId, (slotAmount + 1), transferItem.getDamage()));
					}
				}
			}
		} else {
			/** - Transfer items to cursor from slot */
			if (0 != slotAmount) {
				for (int turnIndex = 0; turnIndex < abs(numTurns); turnIndex++) {
					if (cursorAmount != transferItem.getMaxCount()) {
						if (0 == (slotAmount - 1)) {
							slot.setStack(null);
						} else {
							slot.setStack(new ItemStack(transferItem.itemId, (slotAmount - 1), transferItem.getDamage()));
						}
						minecraft.player.inventory.setCursorStack(new ItemStack(transferItem.itemId, (cursorAmount + 1), transferItem.getDamage()));
					}
				}
			}
		}
	}

//	@Unique private void retrotweaks$scrollInventoryTransfer(float numTurns, int cursorAmount, int slotAmount, ItemInstance transferItem) {
//		int itemsLeftToAdd = 0;
//
//		if (Config.ScrollWheelConfig.invertScrollInventoryDirection) {
//			numTurns *= -1;
//		}
//
//		if (minecraft.player.handler == minecraft.player.playerContainer) {
//			System.out.println("Only one container exists");
//		}
//
//		if (0 > numTurns) {
//			/** - Transfer items out of slot */
//			if (0 != slotAmount) {
//				for (int containerIndex = 0; containerIndex < minecraft.player.handler.slots.lastIndexOf(Slot) - 1; containerIndex++) {
//					Slot curSlot = (Slot)minecraft.player.playerContainer.slots.get(containerIndex);
////					ItemInstance curSlotItem = curSlot.getItem();
////					int curSlotAmount = (null != curSlotItem) ? curSlotItem.count : 0;
////
////					if (  (null == curSlotItem)
////					   || (  (curSlotItem.isDamageAndIDIdentical(transferItem))
////					      && (curSlotAmount != transferItem.getMaxStackSize())
////					      )
////					) {
////						while (  (itemsLeftToAdd < abs(numTurns))
////						      && (curSlotAmount != transferItem.getMaxStackSize())
////					    ) {
////							if (0 == (slotAmount - 1)) {
////								slot.setStack(null);
////							} else {
////								slot.setStack(new ItemInstance(transferItem.itemId, (slotAmount - 1), transferItem.getDamage()));
////							}
////
////							curSlot.setStack(new ItemInstance(transferItem.itemId, (curSlotAmount + 1), transferItem.getDamage()));
////
////							curSlotItem = curSlot.getItem();
////							curSlotAmount = (null != curSlotItem) ? curSlotItem.count : 0;
////							itemsLeftToAdd++;
////						}
////
////						if (itemsLeftToAdd == numTurns) {
////							return;
////						}
////					}
//				}
//			}
//		} else {
//			/** - Transfer items into slot */
//			boolean itemExistsInContainer = true;
//			if (itemExistsInContainer) {
////				for (int turnIndex = 0; turnIndex < abs(numTurns); turnIndex++) {
////				}
//				System.out.println("Scroll up");
//				Slot slotToModify = (Slot)minecraft.player.handler.slots.get(0);
//
//				slotToModify.setStack(new ItemInstance(transferItem.itemId, 7, transferItem.getDamage()));
//			}
//		}
//	}

	@Unique private boolean retrotweaks$handleRightClick(int mouseX, int mouseY, Slot clickedSlot) {
		/** - Get held item */
		ItemStack cursorStack = minecraft.player.inventory.getCursorStack();

		/** - Handle Right-click if an item is held */
		if (null != cursorStack) {

			/** - Ensure a slot was clicked */
			if (null != clickedSlot) {

				/** - Record how many items are in the slot */
				if (null != clickedSlot.getStack()) {

					/** - Let vanilla minecraft handle right click with an item onto a different item */
					if ( !cursorStack.isItemEqual(clickedSlot.getStack()) ) {
						return false;
					}

					rightClickExistingAmount.add(clickedSlot.getStack().count);
				} else {
					rightClickExistingAmount.add(0);
				}

				/** - Begin Right-click + Drag */
				if (cursorStack != null && rightClickPersistentStack == null && isRightClickDragStarted == false) {
					rightClickPersistentStack = cursorStack;
					rightClickItemAmount = rightClickPersistentStack.count;
					isRightClickDragStarted = true;
				}

				/** - Handle initial Right-click */
				lastRMBSlotId = clickedSlot.id;
				lastRMBSlot = clickedSlot;
				if (Config.INVENTORY.drag.rmbPreferShiftClick) {
					boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 1, isShiftKeyDown, this.minecraft.player);

					if (isShiftKeyDown) {
						retrotweaks$resetRightClickDragVariables();
					}
				} else {
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 1, false, this.minecraft.player);
				}

				return true;
			}
		}

		return false;
	}

	@Unique private void retrotweaks$handleRightClickDragMouseTweaks() {
		if (slot.id != lastRMBSlotId) {
			ItemStack cursorStack = minecraft.player.inventory.getCursorStack();

			if (null != cursorStack ) {
				/** - Distribute one item to the slot */
				lastRMBSlotId = slot.id;
				this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 1, false, this.minecraft.player);
			}
		}
	}

	@Unique private void retrotweaks$handleRightClickDrag(ItemStack slotItemToExamine) {
		/** - First slot is handled instantly in mouseClicked function */
		if (slot.id != lastRMBSlotId) {
			if (0 == rightClickHoveredSlots.size())
			{
				/** - Add slot to item distribution */
				rightClickHoveredSlots.add(lastRMBSlot);
			}

			/** - Add slot to item distribution */
			rightClickHoveredSlots.add(slot);

			/** - Record how many items are in the slot */
			if (null != slotItemToExamine) {
				rightClickExistingAmount.add(slotItemToExamine.count);
			}
			else
			{
				rightClickExistingAmount.add(0);
			}

			/** - Distribute one item to the slot */
			lastRMBSlotId = slot.id;
			this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 1, false, this.minecraft.player);
		}
	}

	@Unique private boolean retrotweaks$cancelRightClickDrag(boolean isClientOnServer)
	{
		/** - Cancel Right-click + Drag */
		if (isRightClickDragStarted) {
			if (rightClickHoveredSlots.size() > 1) {
				/** - Slots cannot return to normal on a server */
				if (!isClientOnServer) {
					/** - Return all slots to normal */
					minecraft.player.inventory.setCursorStack(new ItemStack(rightClickPersistentStack.itemId, rightClickItemAmount, rightClickPersistentStack.getDamage()));
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < rightClickHoveredSlots.size(); leftClickHoveredSlotsIndex++) {
						if (0 != rightClickExistingAmount.get(leftClickHoveredSlotsIndex)) {
							rightClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(new ItemStack(rightClickPersistentStack.itemId, rightClickExistingAmount.get(leftClickHoveredSlotsIndex), rightClickPersistentStack.getDamage()));
						} else {
							rightClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(null);
						}
					}
				}

				/** - Reset Right-click + Drag variables and exit function */
				retrotweaks$resetRightClickDragVariables();

				return true;
			}
		}

		return false;
	}

	@Unique private void retrotweaks$resetRightClickDragVariables()
	{
		rightClickExistingAmount.clear();
		rightClickHoveredSlots.clear();
		rightClickPersistentStack = null;
		rightClickItemAmount = 0;
		isRightClickDragStarted = false;
	}

	@Unique private boolean retrotweaks$handleLeftClickWithItem(ItemStack cursorStack, Slot clickedSlot, boolean isClientOnServer) {
		/** - Ensure a slot was clicked */
		if (null != clickedSlot) {

			/** - Record how many items are in the slot and how many items are needed to fill the slot */
			if (null != clickedSlot.getStack()) {

				if (null != cursorStack) {
					/** - Let vanilla minecraft handle left click with an item onto any item */
					if (isClientOnServer) {
						return false;
					}

					/** - Let vanilla minecraft handle left click with an item onto a different item */
					if ( !cursorStack.isItemEqual(clickedSlot.getStack()) ) {
						return false;
					}
				}

				leftClickAmountToFillPersistent.add(cursorStack.getMaxCount() - clickedSlot.getStack().count);
				leftClickExistingAmount.add(clickedSlot.getStack().count);
			} else {
				leftClickAmountToFillPersistent.add(cursorStack.getMaxCount());
				leftClickExistingAmount.add(0);
			}

			/** - Begin Left-click + Drag */
			if (cursorStack != null && leftClickPersistentStack == null && isLeftClickDragStarted == false) {
				leftClickPersistentStack = cursorStack;
				leftClickItemAmount = leftClickPersistentStack.count;
				isLeftClickDragStarted = true;
			}

			/** - Handle initial Left-click */
			lastLMBSlotId = clickedSlot.id;
			lastLMBSlot = clickedSlot;
			if (Config.INVENTORY.drag.lmbPreferShiftClick) {
				boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
				this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 0, isShiftKeyDown, this.minecraft.player);

				if (isShiftKeyDown) {
					retrotweaks$resetLeftClickDragVariables();
					leftClickMouseTweaksPersistentStack = cursorStack;
					isLeftClickDragMouseTweaksStarted = true;
				}
			} else {
				this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 0, false, this.minecraft.player);
			}

			return true;
		}

		return false;
	}

	/**
	 * Ported from Glass Inventory Tweaks' shift-click gate, scoped down to the screens vanilla and
	 * RetroTweaks' own {@code quickMove}/shift-click features do not already handle correctly:
	 * {@link PlayerScreenHandler} keeps its own armor routing ({@code retrotweaks$handleShiftClickIntoArmorSlots}),
	 * and {@link DispenserScreenHandler} keeps the "server must run RetroTweaks too" gate on
	 * {@code multiplayerShiftClickDispensers} - Glass Inventory Tweaks' router drives plain clicks
	 * that need no server-side support at all, and would silently bypass that gate.
	 */
	@Unique private boolean retrotweaks$isPriorityShiftClickEligible(Slot clickedSlot) {
		return clickedSlot != null
			&& clickedSlot.hasStack()
			&& !(clickedSlot instanceof CraftingResultSlot)
			&& !(handler instanceof PlayerScreenHandler)
			&& !(handler instanceof DispenserScreenHandler)
			// Same reason as the two above: priority routing decides which OTHER container a stack
			// should land in, and the creative screen has none. Its own shift-click already means two
			// specific things - destroy the stack on an item tab, move it between hotbar and storage on
			// the inventory tab - and both are in CreativeScreenHandler.quickMove, which is exactly
			// where a plain shift-click ends up once this declines to intercept it.
			&& !(handler instanceof com.periut.retroapi.gamemode.screen.CreativeScreenHandler)
			&& (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
	}

	@Unique private boolean retrotweaks$handleLeftClickWithoutItem(Slot clickedSlot) {
		/** - Ensure a slot was clicked; the MouseTweaks drag state is already armed by
		 *    retrotweaks$beginLeftClickDragMouseTweaks */
		if (clickedSlot != null) {
			boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));

			/** - Handle initial Left-click */
			this.minecraft.interactionManager.clickSlot(this.handler.syncId, clickedSlot.id, 0, isShiftKeyDown, this.minecraft.player);

			return true;
		}

		return false;
	}

	@Unique private void retrotweaks$handleLeftClickDragMouseTweaks() {
		if (slot.id != lastLMBSlotId) {
			lastLMBSlotId = slot.id;

			ItemStack slotItemToExamine = slot.getStack();
			if (null != slotItemToExamine)
			{
				if (null != leftClickMouseTweaksPersistentStack)
				{
					if (slotItemToExamine.isItemEqual(leftClickMouseTweaksPersistentStack))
					{
						if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
							if (Config.INVENTORY.drag.lmbDragShiftClickHeld)
							{
								retrotweaks$dragShiftClick();
							}
						} else {
							if (Config.INVENTORY.drag.lmbDragPickUp) {
								ItemStack cursorStack = minecraft.player.inventory.getCursorStack();

								if (cursorStack == null) {
									/** - Pick up items from slot */
									this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
								} else if (cursorStack.count < leftClickMouseTweaksPersistentStack.getMaxCount()) {
									int amountAbleToPickUp = leftClickMouseTweaksPersistentStack.getMaxCount() - cursorStack.count;
									int amountInSlot = slotItemToExamine.count;

									/** - Pick up items from slot */
									if (amountInSlot <= amountAbleToPickUp) {
										this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
										this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
									} else if (cursorStack.count == leftClickMouseTweaksPersistentStack.getMaxCount()) {
										slot.setStack(new ItemStack(leftClickMouseTweaksPersistentStack.itemId, cursorStack.count, leftClickMouseTweaksPersistentStack.getDamage()));
										minecraft.player.inventory.setCursorStack(new ItemStack(leftClickMouseTweaksPersistentStack.itemId, amountInSlot, leftClickMouseTweaksPersistentStack.getDamage()));
									} else {
										this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);

										slotItemToExamine = slot.getStack();
										cursorStack = minecraft.player.inventory.getCursorStack();
										amountInSlot = slotItemToExamine.count;

										slot.setStack(new ItemStack(leftClickMouseTweaksPersistentStack.itemId, cursorStack.count, leftClickMouseTweaksPersistentStack.getDamage()));
										minecraft.player.inventory.setCursorStack(new ItemStack(leftClickMouseTweaksPersistentStack.itemId, amountInSlot, leftClickMouseTweaksPersistentStack.getDamage()));
									}
								}
							}
						}
					}
				} else if (  (Config.INVENTORY.drag.lmbDragShiftClickAny)
						  && (  (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
						     || (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
				             )
				) {
					retrotweaks$dragShiftClick();
				}
			}
		}
	}

	/**
	 * Shift-click the slot the drag has just entered, routed the same way the first click of the drag
	 * was - otherwise a shift-drag across a chest would scatter items into the main inventory while a
	 * plain shift-click on the same slot puts them in the hotbar.
	 */
	@Unique private void retrotweaks$dragShiftClick() {
		if (Config.INVENTORY.modern.prioritySlotShiftClick && retrotweaks$isPriorityShiftClickEligible(slot)) {
			PriorityShiftClick.shiftClick(slot, handler);
		} else {
			this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, true, this.minecraft.player);
		}
	}

	@Unique private boolean retrotweaks$handleLeftClickDrag()
	{
		/** - Do nothing if slot has already been added to Left-click + Drag logic */
		if (!leftClickHoveredSlots.contains(slot)) {
			ItemStack slotItemToExamine = slot.getStack();

			/** - Check if client is on a server */
			boolean isClientOnServer = minecraft.world.isRemote;

			/** - Do nothing if slot item does not match held item */
			if (null != slotItemToExamine){

				if (isClientOnServer) {
					return true;
				}

				if (!slotItemToExamine.isItemEqual(leftClickPersistentStack)) {
					return true;
				}
			}

			/** - Do nothing if there are no more items to distribute */
			if (1.0 == (double)leftClickItemAmount / (double)leftClickHoveredSlots.size()) {
				return true;
			}

			/** - First slot is handled instantly in mouseClicked function */
			if (slot.id != lastLMBSlotId) {
				if (leftClickHoveredSlots.isEmpty())
				{
					/** - Add slot to item distribution */
					leftClickHoveredSlots.add(lastLMBSlot);
				}

				/** - Add slot to item distribution */
				leftClickHoveredSlots.add(slot);

				/** - Record how many items are in the slot and how many items are needed to fill the slot */
				if (null != slotItemToExamine) {
					leftClickAmountToFillPersistent.add(leftClickPersistentStack.getMaxCount() - slotItemToExamine.count);
					leftClickExistingAmount.add(slotItemToExamine.count);
				}
				else
				{
					leftClickAmountToFillPersistent.add(leftClickPersistentStack.getMaxCount());
					leftClickExistingAmount.add(0);
				}

				/** - Slots cannot return to normal on a server */
				List<Integer> leftClickAmountToFill = new ArrayList<>();
				if (!isClientOnServer) {
					/** - Return all slots to normal */
					minecraft.player.inventory.setCursorStack(new ItemStack(leftClickPersistentStack.itemId, leftClickItemAmount, leftClickPersistentStack.getDamage()));
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < leftClickHoveredSlots.size(); leftClickHoveredSlotsIndex++) {
						leftClickAmountToFill.add(leftClickAmountToFillPersistent.get(leftClickHoveredSlotsIndex));
						if (0 != leftClickExistingAmount.get(leftClickHoveredSlotsIndex)) {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(new ItemStack(leftClickPersistentStack.itemId, leftClickExistingAmount.get(leftClickHoveredSlotsIndex), leftClickPersistentStack.getDamage()));
						} else {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(null);
						}
					}
				}

				/** - Prepare to distribute over slots */
				int numberOfSlotsRemainingToFill = leftClickHoveredSlots.size();
				int itemsPerSlot = leftClickItemAmount / numberOfSlotsRemainingToFill;
				int leftClickRemainingItemAmount = leftClickItemAmount;
				boolean rerunLoop;

				/** - Slots cannot return to normal on a server */
				if (!isClientOnServer) {
					/** - Distribute fewer items to slots whose max stack size will be filled */
					do {
						rerunLoop = false;
						if (0 < numberOfSlotsRemainingToFill) {
							itemsPerSlot = leftClickRemainingItemAmount / numberOfSlotsRemainingToFill;

							if (0 != itemsPerSlot) {
								for (int slotsToCheckIndex = 0; slotsToCheckIndex < leftClickAmountToFill.size(); slotsToCheckIndex++) {
									if (0 != leftClickAmountToFill.get(slotsToCheckIndex) && leftClickAmountToFill.get(slotsToCheckIndex) < itemsPerSlot) {
										/** - Just fill the slot and return */
										for (int fillTheAmountIndex = 0; fillTheAmountIndex < leftClickAmountToFill.get(slotsToCheckIndex); fillTheAmountIndex++) {
											this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(slotsToCheckIndex).id, 1, false, this.minecraft.player);
										}

										leftClickRemainingItemAmount = leftClickRemainingItemAmount - leftClickAmountToFill.get(slotsToCheckIndex);
										leftClickAmountToFill.set(slotsToCheckIndex, 0);
										numberOfSlotsRemainingToFill--;
										rerunLoop = true;
									}
								}
							}
						}
					} while (rerunLoop && 0 < numberOfSlotsRemainingToFill);
				} else {
					/** - Return slots to normal on when client is on a server */
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < (leftClickHoveredSlots.size() - 1); leftClickHoveredSlotsIndex++)
					{
						ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
						if (leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).hasStack() && leftClickHoveredSlots.size() > 1)
						{
							if (cursorStack != null)
							{
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
							}
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
						}
					}
				}

				/** - Distribute remaining items evenly over remaining slots that were not already filled to max stack size */
				for (int distributeSlotsIndex = 0; distributeSlotsIndex < leftClickHoveredSlots.size(); distributeSlotsIndex++) {
					if (isClientOnServer) {
						if (0 != leftClickAmountToFillPersistent.get(distributeSlotsIndex)) {
							for (int addSlotIndex = 0; addSlotIndex < itemsPerSlot; addSlotIndex++) {
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(distributeSlotsIndex).id, 1, false, this.minecraft.player);
							}
						}
					} else {
						if (0 != leftClickAmountToFill.get(distributeSlotsIndex)) {
							for (int addSlotIndex = 0; addSlotIndex < itemsPerSlot; addSlotIndex++) {
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(distributeSlotsIndex).id, 1, false, this.minecraft.player);
							}
						}
					}
				}
			}
		}

		return false;
	}

	@Unique private boolean retrotweaks$cancelLeftClickDrag(boolean isClientOnServer)
	{
		/** - Cancel Left-click + Drag */
		if (isLeftClickDragStarted) {
			if (leftClickHoveredSlots.size() > 1) {
				/** - Check if client is running on a server or not */
				if (!isClientOnServer) {
					/** - Return all slots to normal */
					minecraft.player.inventory.setCursorStack(new ItemStack(leftClickPersistentStack.itemId, leftClickItemAmount, leftClickPersistentStack.getDamage()));
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < leftClickHoveredSlots.size(); leftClickHoveredSlotsIndex++) {
						if (0 != leftClickExistingAmount.get(leftClickHoveredSlotsIndex)) {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(new ItemStack(leftClickPersistentStack.itemId, leftClickExistingAmount.get(leftClickHoveredSlotsIndex), leftClickPersistentStack.getDamage()));
						} else {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(null);
						}
					}
				} else {
					/** - Return slots to normal on when client is on a server */
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < (leftClickHoveredSlots.size() - 1); leftClickHoveredSlotsIndex++)
					{
						ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
						if (leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).hasStack() && leftClickHoveredSlots.size() > 1)
						{
							if (cursorStack != null)
							{
								this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
							}
							this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
						}
					}
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get((leftClickHoveredSlots.size() - 1)).id, 0, false, this.minecraft.player);
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, leftClickHoveredSlots.get((leftClickHoveredSlots.size() - 1)).id, 0, false, this.minecraft.player);
				}

				/** - Reset Left-click + Drag variables and exit function */
				retrotweaks$resetLeftClickDragVariables();
				return true;
			}
		}

		return false;
	}

	@Unique private void retrotweaks$resetLeftClickDragVariables()
	{
		leftClickExistingAmount.clear();
		leftClickAmountToFillPersistent.clear();
		leftClickHoveredSlots.clear();
		leftClickPersistentStack = null;
		leftClickMouseTweaksPersistentStack = null;
		leftClickItemAmount = 0;
		isLeftClickDragStarted = false;
		isLeftClickDragMouseTweaksStarted = false;
	}

	@Unique
	private boolean drawingHoveredSlot;

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;isPointOverSlot(Lnet/minecraft/screen/slot/Slot;II)Z"))
	private boolean retrotweaks$isMouseOverSlot(HandledScreen guiContainer, Slot slot, int x, int y) {
		if (Config.INVENTORY.drag.dragGraphics) {
			return (  (drawingHoveredSlot = rightClickHoveredSlots.contains(slot))
				   || (drawingHoveredSlot = leftClickHoveredSlots.contains(slot))
				   || isPointOverSlot(slot, x, y)
				   );
		} else {
			return isPointOverSlot(slot, x, y);
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;fillGradient(IIIIII)V", ordinal = 0), require = 0)
	private void retrotweaks$fillGradient(HandledScreen instance, int startX, int startY, int endX, int endY, int colorStart, int colorEnd) {
		if (Config.INVENTORY.drag.dragGraphics) {
			if (colorStart != colorEnd) throw new AssertionError();
			int color = drawingHoveredSlot ? 0x20ffffff : colorStart;
			this.fillGradient(startX, startY, endX, endY, color, color);
		} else {
			this.fillGradient(startX, startY, endX, endY, colorStart, colorEnd);
		}
	}

	// Glass Inventory Tweaks also drew a preview stack over every slot the drag had crossed, because
	// its own drag holds the items on the cursor until the button comes up. RetroTweaks inherited
	// InventoryTweaks' drag instead, which clicks the items into the slots as it goes and rebuilds the
	// whole split each time the drag reaches a new slot - so the counts on screen are the real ones
	// already, and a preview on top of them only ever showed double.

	@Inject(method = "keyPressed", at = @At("RETURN"))
	private void retrotweaks$keyPressed(char character, int keyCode, CallbackInfo ci) {
		// A creative slot has no stack to drop - the picker's is a template and the bin's is gone.
		if (this.slot == null || retrotweaks$creativeOwns(this.slot)) {
			return;
		}

		if (Config.INVENTORY.modern.dropKeyInInventory) {
			if (keyCode == this.minecraft.options.dropKey.code) {
				if (this.minecraft.player.inventory.getCursorStack() != null) {
					return;
				}

				this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
				if (Config.INVENTORY.modern.ctrlDropWholeStack) {
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, -999, Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ? 0 : 1, false, this.minecraft.player);
				} else {
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, -999, 1, false, this.minecraft.player);
				}
				this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
			}
		}

		if (Config.INVENTORY.modern.numberKeyHotbarSwap) {
			// UniTweaks remaps the hotbar keys to its own keybinds; without it the vanilla number
			// key codes arrive unchanged and need no translation.

			if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
				if (  (null != this.handler.slots)
				   && (10 <= this.handler.slots.size())
				) {
					if (this.minecraft.player.inventory.getCursorStack() == null) {
						this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
					}
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, (this.handler.slots.size() - 10) + keyCode - 1, 0, false, this.minecraft.player);
					this.minecraft.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, false, this.minecraft.player);
				}
			}
		}
	}
}
