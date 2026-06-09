package com.periut.retroapi.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Small nbt helper for the component system. Beta's {@link NbtCompound} exposes no way to
 * iterate its keys, so {@link #keys} writes the compound to the binary nbt format and
 * re-reads it, recording the keys, the same trick RetroAPI's id map uses.
 */
public final class ComponentNbt {

	private ComponentNbt() {
	}

	/** Writes a stack's components into {@code retroapi:components} (no-op if it has none). */
	@SuppressWarnings("unchecked")
	public static void write(RetroComponentHolder holder, NbtCompound nbt) {
		java.util.Map<RetroComponentType<?>, Object> map = holder.retroapi$components();
		if (map.isEmpty()) {
			return;
		}
		NbtCompound components = new NbtCompound();
		for (java.util.Map.Entry<RetroComponentType<?>, Object> entry : map.entrySet()) {
			RetroComponentType<Object> type = (RetroComponentType<Object>) entry.getKey();
			type.getSerializer().write(components, type.getId().toString(), entry.getValue());
		}
		nbt.put("retroapi:components", components);
	}

	/** Reads {@code retroapi:components} from nbt onto a stack, replacing its components. */
	public static void read(RetroComponentHolder holder, NbtCompound nbt) {
		if (!nbt.contains("retroapi:components")) {
			return;
		}
		NbtCompound components = nbt.getCompound("retroapi:components");
		java.util.Map<RetroComponentType<?>, Object> map = holder.retroapi$components();
		map.clear();
		for (String key : keys(components)) {
			RetroComponentType<?> type = RetroComponents.byId(key);
			if (type != null) {
				map.put(type, type.getSerializer().read(components, key));
			}
		}
	}

	/** The set of keys in a compound (beta has no key-iteration API). */
	public static Set<String> keys(NbtCompound compound) {
		Set<String> keys = new HashSet<>();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			NbtCompound wrapper = new NbtCompound();
			wrapper.put("d", compound);
			NbtIo.write(wrapper, dos);
			dos.flush();

			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
			dis.readByte();       // root type (10)
			dis.readUTF();        // root name
			dis.readByte();       // inner type (10)
			dis.readUTF();        // "d"
			while (true) {
				byte type = dis.readByte();
				if (type == 0) {
					break;
				}
				keys.add(dis.readUTF());
				skip(dis, type);
			}
		} catch (IOException e) {
			// Byte-array streams do not throw in practice.
		}
		return keys;
	}

	private static void skip(DataInputStream dis, byte type) throws IOException {
		switch (type) {
			case 1: dis.readByte(); break;
			case 2: dis.readShort(); break;
			case 3: dis.readInt(); break;
			case 4: dis.readLong(); break;
			case 5: dis.readFloat(); break;
			case 6: dis.readDouble(); break;
			case 7: { int len = dis.readInt(); dis.skipBytes(len); break; }
			case 8: dis.readUTF(); break;
			case 9: {
				byte listType = dis.readByte();
				int count = dis.readInt();
				for (int i = 0; i < count; i++) {
					skip(dis, listType);
				}
				break;
			}
			case 10: {
				while (true) {
					byte t = dis.readByte();
					if (t == 0) {
						break;
					}
					dis.readUTF();
					skip(dis, t);
				}
				break;
			}
			case 11: { int len = dis.readInt(); dis.skipBytes(len * 4); break; }
			default: break;
		}
	}
}
