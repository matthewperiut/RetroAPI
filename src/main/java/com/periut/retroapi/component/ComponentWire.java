package com.periut.retroapi.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Puts an item's data components on the wire, for dedicated multiplayer. Beta serialises
 * ItemStacks inline as id/count/damage with NO nbt, so a client never learns about the
 * components the server set, the held item's texture and tooltip would never update. The
 * packet mixins call this to append a tiny component blob AFTER each stack (the stacks are
 * always the last fields of the slot/inventory packets, so trailing data is safe), and to
 * read it back on the other side.
 *
 * <p>Singleplayer needs none of this: beta's singleplayer is client-only, so the client IS
 * the world and already holds the real stacks with their components.</p>
 */
public final class ComponentWire {

	private ComponentWire() {
	}

	/** Appends a stack's components (a boolean flag, then the compound when present). */
	public static void write(DataOutputStream out, ItemStack stack) {
		try {
			NbtCompound nbt = new NbtCompound();
			if (stack != null) {
				ComponentNbt.write((RetroComponentHolder) (Object) stack, nbt);
			}
			boolean has = nbt.contains("retroapi:components");
			out.writeBoolean(has);
			if (has) {
				NbtIo.write(nbt.getCompound("retroapi:components"), out);
			}
		} catch (IOException e) {
			// A dropped component blob is non-fatal; the item still works, just unsynced.
		}
	}

	/** Reads a stack's components written by {@link #write} and applies them to the stack. */
	public static void read(DataInputStream in, ItemStack stack) {
		applyBlob(stack, readBlob(in));
	}

	/**
	 * Reads a component blob off the wire WITHOUT a stack to apply it to yet, returning the
	 * raw components compound (or null). The dropped-item spawn packet builds its stack on
	 * the client only after the whole packet is read, so it stashes this and applies it once
	 * the {@link ItemStack} exists (see {@link #applyBlob}).
	 */
	public static NbtCompound readBlob(DataInputStream in) {
		try {
			if (!in.readBoolean()) {
				return null;
			}
			return NbtIo.read(in);
		} catch (IOException e) {
			return null; // see write(); an unsynced item still works
		}
	}

	/** Applies a previously-read component blob to a stack once it exists. */
	public static void applyBlob(ItemStack stack, NbtCompound components) {
		if (stack == null || components == null) {
			return;
		}
		NbtCompound wrapper = new NbtCompound();
		wrapper.put("retroapi:components", components);
		ComponentNbt.read((RetroComponentHolder) (Object) stack, wrapper);
	}
}
