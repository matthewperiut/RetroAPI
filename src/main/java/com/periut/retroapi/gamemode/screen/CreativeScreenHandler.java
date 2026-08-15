package com.periut.retroapi.gamemode.screen;

import com.periut.retroapi.gamemode.CreativeSync;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.List;

/**
 * The creative inventory's container - a port of modern's {@code ItemPickerMenu}.
 *
 * <p>This is a <b>real</b> {@link ScreenHandler} with real {@link Slot}s, which is what makes the
 * screen behave like every other container in the game: vanilla draws the slots and their items,
 * vanilla carries the stack on the cursor, vanilla draws the tooltips, and the player's own hotbar
 * is genuinely their hotbar rather than a picture of one.
 *
 * <p>Slot geometry is modern's: the 45 picker slots at {@code (9, 18)} on an 18-pixel pitch, the
 * player's hotbar at {@code y=112}, and the destroy slot at {@code (173, 112)}.
 *
 * <p>The picker slots are bottomless. Taking from one hands out a <em>copy</em> and leaves the slot
 * as it was, which is modern's behaviour and the whole point of the mode.
 */
public class CreativeScreenHandler extends ScreenHandler {

    public static final int PICKER_SLOTS = 45;
    private static final int COLUMNS = 9;
    private static final int SLOT = 18;

    /** The 45 visible picker slots. Refilled whenever the tab or the scroll position changes. */
    private final CreativeInventory picker = new CreativeInventory(PICKER_SLOTS);
    /** Modern's destroy slot: anything dropped here is gone. */
    private final CreativeInventory destroy = new CreativeInventory(1);

    private final PlayerEntity player;

    /** Which player-inventory index each slot id maps to, or -1. Rebuilt with the slot list. */
    private int[] inventoryIndexBySlot = new int[0];
    private int destroySlotId = -1;
    private boolean inventoryTab;

    public CreativeScreenHandler(final PlayerEntity player) {
        this.player = player;
        showPicker();
    }

    /**
     * The item-picking layout: 45 bottomless slots, the player's hotbar, and the destroy slot.
     */
    public void showPicker() {
        inventoryTab = false;
        slots.clear();

        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new PickerSlot(picker, row * COLUMNS + column, 9 + column * SLOT, 18 + row * SLOT));
            }
        }

        final PlayerInventory inventory = player.inventory;
        for (int slot = 0; slot < COLUMNS; slot++) {
            addSlot(new Slot(inventory, slot, 9 + slot * SLOT, 112));
        }

        finishLayout();
    }

    /**
     * Modern's inventory tab, at modern's own coordinates: armour in a block at
     * {@code (54 + col*54, 6 + row*27)}, the three storage rows from {@code (9, 54)}, the hotbar at
     * {@code y=112}. Beta's player inventory indexes armour at 36..39, after the 36 main slots.
     */
    public void showInventory() {
        inventoryTab = true;
        slots.clear();

        final PlayerInventory inventory = player.inventory;
        for (int armour = 0; armour < 4; armour++) {
            addSlot(new Slot(inventory, 36 + armour, 54 + (armour / 2) * 54, 6 + (armour % 2) * 27));
        }
        for (int index = 9; index < 36; index++) {
            final int position = index - 9;
            addSlot(new Slot(inventory, index, 9 + (position % 9) * SLOT, 54 + (position / 9) * SLOT));
        }
        for (int slot = 0; slot < COLUMNS; slot++) {
            addSlot(new Slot(inventory, slot, 9 + slot * SLOT, 112));
        }

        finishLayout();
    }

    /** Adds the destroy slot where modern has one, and rebuilds the slot id -> inventory index table. */
    private void finishLayout() {
        // The bin belongs to the inventory tab and only to it: modern builds it inside that tab's
        // branch of selectTab and throws the slot list away again on the way out, so on an item tab
        // there is nothing at (173, 112) to click - which is also what the item panel's own texture
        // says, since it draws plain panel there while the inventory panel draws the bin.
        //
        // A plain Slot would refuse a stack it cannot stack onto, and refusing is the one thing this
        // slot must never do: anything dropped here is meant to disappear.
        destroySlotId = -1;
        if (inventoryTab) {
            addSlot(new DestroySlot(destroy, 0, 173, 112));
            destroySlotId = slots.size() - 1;
        }

        inventoryIndexBySlot = new int[slots.size()];
        for (int id = 0; id < slots.size(); id++) {
            inventoryIndexBySlot[id] = -1;
        }

        // Everything that is not a picker slot and not the destroy slot belongs to the player, and
        // the id -> index table is how a click on one gets reported to the server.
        int inventorySlot = 0;
        for (int id = 0; id < slots.size(); id++) {
            if (id == destroySlotId || getSlot(id) instanceof PickerSlot) {
                continue;
            }
            inventoryIndexBySlot[id] = inventoryIndexAt(id, inventorySlot++);
        }
    }

    /** The player-inventory index a slot id addresses, in the order the layouts add them. */
    private int inventoryIndexAt(final int slotId, final int ordinal) {
        if (!inventoryTab) {
            return ordinal;                       // picker layout: nine hotbar slots, 0..8
        }
        if (ordinal < 4) {
            return 36 + ordinal;                  // armour
        }
        if (ordinal < 31) {
            return 9 + (ordinal - 4);             // storage
        }
        return ordinal - 31;                      // hotbar
    }

    public boolean isInventoryTab() {
        return inventoryTab;
    }

    /** Puts the visible page of items into the picker slots. */
    public void setContents(final List<ItemStack> stacks, final int firstIndex) {
        for (int index = 0; index < PICKER_SLOTS; index++) {
            final int source = firstIndex + index;
            picker.setStack(index, source >= 0 && source < stacks.size() ? stacks.get(source).copy() : null);
        }
    }

    @Override
    public boolean canUse(final PlayerEntity player) {
        return true;
    }

    /**
     * Vanilla's click handling, with two creative differences: the picker is bottomless, and the
     * destroy slot swallows whatever lands in it.
     *
     * <p>Anything that ends up changing the player's own inventory is sent to the server, which is
     * what modern's {@code ServerboundSetCreativeModeSlotPacket} does - a creative client is trusted
     * only after the server has re-checked that it really is in creative.
     */
    @Override
    public ItemStack onSlotClick(final int slotId, final int button, final boolean shift, final PlayerEntity clicker) {
        if (slotId >= 0 && slotId < slots.size() && getSlot(slotId) instanceof PickerSlot) {
            return clickPicker(getSlot(slotId).getStack(), button, shift, clicker.inventory);
        }

        if (destroySlotId >= 0 && slotId == destroySlotId) {
            // Modern checks the bin before anything else, and a shift-click on it empties the whole
            // inventory rather than just the cursor.
            if (shift) {
                clearInventory();
            } else {
                clicker.inventory.setCursorStack(null);
            }
            return null;
        }

        final ItemStack result = super.onSlotClick(slotId, button, shift, clicker);
        syncPlayerInventory(slotId);
        return result;
    }

    /**
     * A click on one of the bottomless slots, which is modern's {@code slot.container == CONTAINER}
     * branch of {@code CreativeModeInventoryScreen.slotClicked} and nothing else.
     *
     * <p>The counts are the part worth naming: a plain click hands out <em>the picker stack's own
     * count</em>, which is one, and a shift-click hands out a full stack. Holding the same item
     * already, a left click adds one to it - or fills it to the top with shift - and a right click
     * takes one off. Holding anything else, the picker is a bin: a left click throws the lot away, a
     * right click a single item.
     */
    private ItemStack clickPicker(final ItemStack template, final int button, final boolean shift,
                                  final PlayerInventory inventory) {
        final ItemStack cursor = inventory.getCursorStack();

        if (cursor != null && template != null && cursor.isItemEqual(template)) {
            if (button == 0) {
                if (shift) {
                    cursor.count = cursor.getMaxCount();
                } else if (cursor.count < cursor.getMaxCount()) {
                    cursor.count++;
                }
            } else {
                shrinkCursor(inventory);
            }
            return null;
        }

        if (cursor == null) {
            if (template == null) {
                return null;
            }
            final ItemStack taken = template.copy();
            taken.count = shift ? taken.getMaxCount() : template.count;
            inventory.setCursorStack(taken);
            return null;
        }

        if (button == 0) {
            inventory.setCursorStack(null);
        } else {
            shrinkCursor(inventory);
        }
        return null;
    }

    private static void shrinkCursor(final PlayerInventory inventory) {
        final ItemStack cursor = inventory.getCursorStack();
        if (cursor != null && --cursor.count <= 0) {
            inventory.setCursorStack(null);
        }
    }

    /**
     * Modern's SWAP: with the pointer over a picker slot, a number key puts a <em>full</em> stack of
     * that item into the matching hotbar slot - {@code setItem(buttonNum, copyWithCount(max))}.
     *
     * @return whether the key did anything, so the screen can leave it to vanilla if not
     */
    public boolean swapToHotbar(final int slotId, final int hotbarIndex) {
        if (slotId < 0 || slotId >= slots.size() || hotbarIndex < 0 || hotbarIndex >= COLUMNS) {
            return false;
        }

        if (getSlot(slotId) instanceof PickerSlot) {
            final ItemStack template = getSlot(slotId).getStack();
            if (template == null) {
                return false;
            }

            final ItemStack given = template.copy();
            given.count = given.getMaxCount();
            player.inventory.setStack(hotbarIndex, given);
            sync(hotbarIndex);
            return true;
        }

        // One of the player's own slots. Modern answers a number key over those too, and with a plain
        // swap rather than a copy - it is the inventory's own stack being moved to hand, so an empty
        // hovered slot swaps just as happily as a full one, and whatever sat in the target hotbar slot
        // goes back where the hovered stack came from rather than being refused.
        if (slotId == destroySlotId || slotId >= inventoryIndexBySlot.length) {
            return false;
        }
        final int index = inventoryIndexBySlot[slotId];
        if (index < 0) {
            return false;
        }
        if (index == hotbarIndex) {
            return true;   // already there; handled, with nothing to do
        }

        final ItemStack held = player.inventory.getStack(hotbarIndex);
        player.inventory.setStack(hotbarIndex, player.inventory.getStack(index));
        player.inventory.setStack(index, held);
        sync(hotbarIndex);
        sync(index);
        return true;
    }

    /** Tells the server what an inventory index now holds, by index rather than by slot id. */
    private void sync(final int inventoryIndex) {
        final CreativeSync sync = CreativeSync.Holder.get();
        if (sync != null) {
            sync.setSlot(inventoryIndex, player.inventory.getStack(inventoryIndex));
        }
    }

    /** Modern's shift-click on the bin: every slot of the player's inventory emptied, armour included. */
    private void clearInventory() {
        final PlayerInventory inventory = player.inventory;
        final CreativeSync sync = CreativeSync.Holder.get();

        for (int index = 0; index < inventory.size(); index++) {
            if (inventory.getStack(index) == null) {
                continue;
            }
            inventory.setStack(index, null);
            if (sync != null) {
                sync.setSlot(index, null);
            }
        }
    }

    /**
     * Shift-click.
     *
     * <p>Two different things, as in modern. On an item tab the picker is a bin, so shift-clicking
     * one of your own slots <em>destroys</em> what is in it. On the inventory tab there is no bin and
     * the tab is an inventory screen, so it does what every inventory screen does: moves the stack
     * between the hotbar and the three rows above it.
     */
    @Override
    public ItemStack quickMove(final int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return null;
        }
        final Slot slot = getSlot(slotId);
        if (slot == null || !slot.hasStack() || slot instanceof PickerSlot || slotId == destroySlotId) {
            return null;
        }

        if (!inventoryTab) {
            slot.setStack(null);
            slot.markDirty();
            syncPlayerInventory(slotId);
            return null;
        }

        // Hotbar <-> storage. The hotbar is the last nine player slots in this layout, so anything
        // before them is storage, and each is the other's destination.
        final int firstHotbar = slots.size() - 1 - COLUMNS;
        final ItemStack moving = slot.getStack();
        final ItemStack before = moving.copy();

        if (slotId >= firstHotbar) {
            insertItem(moving, 4, firstHotbar, false);
        } else {
            insertItem(moving, firstHotbar, firstHotbar + COLUMNS, false);
        }

        if (moving.count == 0) {
            slot.setStack(null);
        }
        slot.markDirty();

        // Everything that moved has to be told to the server, both ends of it.
        for (int id = 0; id < slots.size(); id++) {
            syncPlayerInventory(id);
        }
        return before;
    }

    /**
     * Tells the server what a player-inventory slot now holds.
     *
     * <p>Keyed off the slot id rather than off the slot's own inventory, because beta's {@code Slot}
     * keeps its index private and there is nothing to ask.
     */
    private void syncPlayerInventory(final int slotId) {
        if (slotId < 0 || slotId >= inventoryIndexBySlot.length) {
            return;
        }
        final int index = inventoryIndexBySlot[slotId];
        if (index < 0) {
            return;
        }
        final CreativeSync sync = CreativeSync.Holder.get();
        if (sync != null) {
            sync.setSlot(index, player.inventory.getStack(index));
        }
    }

    /** The bin: takes anything, keeps nothing. */
    /**
     * True for a slot this screen owns outright: a bottomless picker template, or the bin.
     *
     * <p>For anything else that patches container screens generically. The player's own inventory is
     * on this screen too, and behaves exactly as it does anywhere else - dragging a stack across it,
     * splitting one, shift-clicking it - so a tweak that stands down on the whole screen loses those
     * for no reason. These two are the slots where the usual rules genuinely do not apply: taking from
     * a picker MINTS a stack rather than moving one, and the bin destroys whatever it is given.
     */
    public static boolean isCreativeSlot(final Slot slot) {
        return slot instanceof PickerSlot || slot instanceof DestroySlot;
    }

    private static final class DestroySlot extends Slot {
        private DestroySlot(final Inventory inventory, final int index, final int x, final int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(final ItemStack stack) {
            return true;
        }

        @Override
        public void setStack(final ItemStack stack) {
            // Swallow it. Nothing is ever stored, so the slot always draws empty and always accepts.
        }
    }

    /** A picker slot: shows an item, never runs out, and refuses anything put into it. */
    private static final class PickerSlot extends Slot {
        private PickerSlot(final Inventory inventory, final int index, final int x, final int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(final ItemStack stack) {
            return false;
        }
    }

    /** A plain array-backed container; beta has no {@code SimpleContainer}. */
    public static final class CreativeInventory implements Inventory {
        private final ItemStack[] stacks;

        CreativeInventory(final int size) {
            this.stacks = new ItemStack[size];
        }

        @Override
        public int size() {
            return stacks.length;
        }

        @Override
        public ItemStack getStack(final int slot) {
            return slot >= 0 && slot < stacks.length ? stacks[slot] : null;
        }

        @Override
        public ItemStack removeStack(final int slot, final int amount) {
            return null;
        }

        @Override
        public void setStack(final int slot, final ItemStack stack) {
            if (slot >= 0 && slot < stacks.length) {
                stacks[slot] = stack;
            }
        }

        @Override
        public String getName() {
            return "Creative";
        }

        @Override
        public int getMaxCountPerStack() {
            return 64;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean canPlayerUse(final PlayerEntity player) {
            return true;
        }
    }
}
