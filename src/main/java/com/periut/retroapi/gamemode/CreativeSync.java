package com.periut.retroapi.gamemode;

import net.minecraft.item.ItemStack;

/**
 * How the creative screen tells a server what it just did.
 *
 * <p>A seam, not a convenience. The screen's container is common code - a dedicated server loads it
 * along with everything else in this package - while the code that actually sends the packets is
 * client-only and reaches {@code Minecraft}, whose fields name {@code ClientPlayerEntity}. A direct
 * call would put that class in the container's constant pool and the server would refuse to load it
 * at all ("Cannot load class ... in environment type SERVER"). The client fills this in at start-up;
 * on a server it stays empty and nothing asks.
 *
 * <p>Same shape as {@code DimensionTeleporter} and {@code GuiOpener} elsewhere in RetroAPI.
 */
public interface CreativeSync {

    /** Hand me this stack. */
    void give(ItemStack stack);

    /** This inventory slot now holds this stack. */
    void setSlot(int slot, ItemStack stack);

    /** Set once from {@code RetroAPIClient}; null on a dedicated server. */
    class Holder {
        private static CreativeSync instance;

        public static void set(final CreativeSync sync) {
            instance = sync;
        }

        public static CreativeSync get() {
            return instance;
        }
    }
}
