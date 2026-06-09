package com.periut.retroapi.component;

import net.minecraft.nbt.NbtCompound;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * A typed, persistent piece of data that can live on an ItemStack, the beta port of
 * modern Minecraft's {@code DataComponentType}.
 *
 * <p>Beta ItemStacks have NO nbt of their own, just id/count/damage, so "extra data on
 * an item" did not exist before. This (with {@link RetroComponents}) adds it the modern
 * way: instead of poking raw nbt tags, you declare a typed component once and then
 * {@code get}/{@code set} it on any stack. The {@link Serializer} is the equivalent of
 * the modern {@code persistent(codec)} call; it says how the value is written to and read
 * from nbt for saving (and, on a dedicated server, the network).</p>
 *
 * @param <T> the value type this component holds
 */
public final class RetroComponentType<T> {

	/** Reads/writes a component value to nbt under a key. The beta-era stand-in for a Codec. */
	public interface Serializer<T> {
		void write(NbtCompound nbt, String key, T value);

		T read(NbtCompound nbt, String key);
	}

	private final NamespacedIdentifier id;
	private final T defaultValue;
	private final Serializer<T> serializer;

	RetroComponentType(NamespacedIdentifier id, T defaultValue, Serializer<T> serializer) {
		this.id = id;
		this.defaultValue = defaultValue;
		this.serializer = serializer;
	}

	public NamespacedIdentifier getId() {
		return this.id;
	}

	public T getDefault() {
		return this.defaultValue;
	}

	public Serializer<T> getSerializer() {
		return this.serializer;
	}

	@Override
	public String toString() {
		return "RetroComponentType[" + this.id + "]";
	}

	// ------------------------------------------------------- built-in serializers --

	public static final Serializer<Integer> INT = new Serializer<Integer>() {
		public void write(NbtCompound nbt, String key, Integer value) {
			nbt.putInt(key, value);
		}

		public Integer read(NbtCompound nbt, String key) {
			return nbt.getInt(key);
		}
	};

	public static final Serializer<Float> FLOAT = new Serializer<Float>() {
		public void write(NbtCompound nbt, String key, Float value) {
			nbt.putFloat(key, value);
		}

		public Float read(NbtCompound nbt, String key) {
			return nbt.getFloat(key);
		}
	};

	public static final Serializer<Boolean> BOOL = new Serializer<Boolean>() {
		public void write(NbtCompound nbt, String key, Boolean value) {
			nbt.putBoolean(key, value);
		}

		public Boolean read(NbtCompound nbt, String key) {
			return nbt.getBoolean(key);
		}
	};

	public static final Serializer<String> STRING = new Serializer<String>() {
		public void write(NbtCompound nbt, String key, String value) {
			nbt.putString(key, value);
		}

		public String read(NbtCompound nbt, String key) {
			return nbt.getString(key);
		}
	};

	public static final Serializer<Long> LONG = new Serializer<Long>() {
		public void write(NbtCompound nbt, String key, Long value) {
			nbt.putLong(key, value);
		}

		public Long read(NbtCompound nbt, String key) {
			return nbt.getLong(key);
		}
	};

	public static final Serializer<Double> DOUBLE = new Serializer<Double>() {
		public void write(NbtCompound nbt, String key, Double value) {
			nbt.putDouble(key, value);
		}

		public Double read(NbtCompound nbt, String key) {
			return nbt.getDouble(key);
		}
	};

	/** An item reference (the id string), the beta stand-in for modern's item-holder components. */
	public static final Serializer<net.minecraft.item.Item> ITEM = new Serializer<net.minecraft.item.Item>() {
		public void write(NbtCompound nbt, String key, net.minecraft.item.Item value) {
			nbt.putInt(key, value == null ? -1 : value.id);
		}

		public net.minecraft.item.Item read(NbtCompound nbt, String key) {
			int id = nbt.getInt(key);
			return id >= 0 && id < net.minecraft.item.Item.ITEMS.length ? net.minecraft.item.Item.ITEMS[id] : null;
		}
	};

	/**
	 * A raw {@link NbtCompound}: store any structured nbt straight in a component, the escape
	 * hatch when no typed serializer fits. (Prefer a typed {@link #compound} for real data, but
	 * this is here for free-form or pre-built nbt.)
	 */
	public static final Serializer<NbtCompound> NBT = new Serializer<NbtCompound>() {
		public void write(NbtCompound nbt, String key, NbtCompound value) {
			nbt.put(key, value == null ? new NbtCompound() : value);
		}

		public NbtCompound read(NbtCompound nbt, String key) {
			return nbt.getCompound(key);
		}
	};

	/**
	 * A whole {@link net.minecraft.item.ItemStack}: id, count, damage, AND the stack's own
	 * components. So a component can hold an item that itself carries components. Combine with
	 * {@link #listOf} to keep a full INVENTORY in one component:
	 * {@code RetroComponentType.listOf(RetroComponentType.ITEM_STACK)} is a {@code List<ItemStack>}.
	 */
	public static final Serializer<net.minecraft.item.ItemStack> ITEM_STACK = new Serializer<net.minecraft.item.ItemStack>() {
		public void write(NbtCompound nbt, String key, net.minecraft.item.ItemStack value) {
			NbtCompound sub = new NbtCompound();
			if (value != null) {
				sub.putInt("id", value.itemId);
				sub.putInt("count", value.count);
				sub.putInt("damage", value.getDamage());
				ComponentNbt.write((RetroComponentHolder) (Object) value, sub);
			}
			nbt.put(key, sub);
		}

		public net.minecraft.item.ItemStack read(NbtCompound nbt, String key) {
			NbtCompound sub = nbt.getCompound(key);
			if (sub == null || !sub.contains("id")) {
				return null;
			}
			net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(
				sub.getInt("id"), sub.getInt("count"), sub.getInt("damage"));
			ComponentNbt.read((RetroComponentHolder) (Object) stack, sub);
			return stack;
		}
	};

	/**
	 * Builds a serializer for a composite value (a record, say) from a pair of functions
	 * that read/write the value into its own sub-compound, the beta-era equivalent of a
	 * {@code RecordCodecBuilder}. This is how you store multi-field component data, the
	 * fabric-docs "advanced" component (temperature + burnt) is exactly this shape.
	 */
	public static <T> Serializer<T> compound(java.util.function.BiConsumer<NbtCompound, T> writer,
			java.util.function.Function<NbtCompound, T> reader) {
		return new Serializer<T>() {
			public void write(NbtCompound nbt, String key, T value) {
				NbtCompound sub = new NbtCompound();
				writer.accept(sub, value);
				nbt.put(key, sub);
			}

			public T read(NbtCompound nbt, String key) {
				return reader.apply(nbt.getCompound(key));
			}
		};
	}

	/**
	 * Builds a serializer for a {@code List<E>} from an element serializer, the beta-era
	 * equivalent of {@code Codec.list(...)}. The list rides as an nbt list of compounds,
	 * each element written under the fixed key {@code "v"}.
	 */
	public static <E> Serializer<java.util.List<E>> listOf(Serializer<E> element) {
		return new Serializer<java.util.List<E>>() {
			public void write(NbtCompound nbt, String key, java.util.List<E> value) {
				net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
				for (E item : value) {
					NbtCompound entry = new NbtCompound();
					element.write(entry, "v", item);
					list.add(entry);
				}
				nbt.put(key, list);
			}

			public java.util.List<E> read(NbtCompound nbt, String key) {
				java.util.List<E> result = new java.util.ArrayList<>();
				net.minecraft.nbt.NbtList list = nbt.getList(key);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						result.add(element.read((NbtCompound) list.get(i), "v"));
					}
				}
				return result;
			}
		};
	}
}
