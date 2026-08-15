package com.periut.retroapi.config;

import com.periut.retroapi.RetroAPI;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflects over a config object graph once at startup and produces the tree that both the JSON file
 * and the config screen are driven from.
 *
 * <p>Options are declared as plain annotated fields (see {@link Opt}/{@link Cat}) rather than
 * registered by hand: RetroTweaks merges fifteen mods and carries several hundred options, and a
 * registration call per option would be several hundred lines that can silently drift out of sync
 * with the fields they describe.
 *
 * <p>Each option keeps two values. {@code userValue} is what the user chose and is what gets saved;
 * the field itself holds the <i>effective</i> value, which differs only when another mod already
 * provides the feature (see {@link Opt#source()}). Mixins therefore read a plain field with no
 * indirection, and uninstalling the other mod restores the user's choice untouched.
 */
public abstract class ConfigTree {

	public final String key;
	public final String name;
	public final String desc;
	/**
	 * Dotted path from the root, e.g. {@code graphics.video.fovOverride}. The one stable name a node
	 * has that survives being moved between screens, which is what lets something outside this tree -
	 * {@link com.periut.retrotweaks.compat.UniTweaksBridge} - name an option without holding a
	 * reference to it. The root itself is "".
	 */
	public final String path;

	/** {@code parentPath} is null for the root only, whose own path is empty so that its children are
	 *  named "system", "graphics", ... rather than "retrotweaks.system" - the tree's name for itself is
	 *  not part of a path THROUGH it. */
	protected ConfigTree(String parentPath, String key, String name, String desc) {
		this.key = key;
		this.name = name;
		this.desc = desc;
		this.path = parentPath == null ? "" : parentPath.isEmpty() ? key : parentPath + "." + key;
	}

	// ---------------------------------------------------------------- nodes

	/** A section: holds child options and further sections. */
	public static final class Category extends ConfigTree {
		public final List<ConfigTree> children = new ArrayList<>();

		/**
		 * Non-null when {@link Cat#gateOption()} named a sibling option: this section only does
		 * anything while that option holds one of {@link Gate#enabledValues}. Null means always
		 * available. Set once, during {@link #fill}, and never touches any option's value - only the
		 * screen reads it, to grey out the group the current choice ignores.
		 */
		public Gate gate;

		/** {@link Cat#gateOption()} as read off the annotation, kept only until {@link #fill} resolves it. */
		String pendingGateOption = "";
		/** {@link Cat#gateValues()} as read off the annotation, kept only until {@link #fill} resolves it. */
		String[] pendingGateValues = EMPTY_SOURCES;

		Category(String parentPath, String key, String name, String desc) {
			super(parentPath, key, name, desc);
		}

		/** True when this section contains at least one option the screen can show. */
		public boolean hasContent() {
			for (ConfigTree child : children) {
				if (child instanceof Option) return true;
				if (child instanceof Category category && category.hasContent()) return true;
			}
			return false;
		}

		/** True when this section is live: ungated, or its gate's controller currently agrees. */
		public boolean isGateOpen() {
			return gate == null || gate.isOpen();
		}
	}

	/**
	 * Ties a {@link Category} to a sibling {@link Option} whose current value decides whether that
	 * section is live. Read fresh on every check - there is nothing to keep in sync, which is also
	 * why switching the controller back and forth never disturbs the gated section's own values.
	 */
	public static final class Gate {
		public final Option controller;
		private final Object[] enabledValues;

		Gate(Option controller, Object[] enabledValues) {
			this.controller = controller;
			this.enabledValues = enabledValues;
		}

		public boolean isOpen() {
			Object current = controller.get();
			for (Object value : enabledValues) {
				if (java.util.Objects.equals(value, current)) return true;
			}
			return false;
		}

		/** The value(s) that would open the gate, formatted for the screen, e.g. "Advanced". */
		public String describeEnabledValues() {
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < enabledValues.length; i++) {
				if (i > 0) out.append(i == enabledValues.length - 1 ? " or " : ", ");
				out.append(enabledValues[i]);
			}
			return out.toString();
		}
	}

	/** What kind of widget an option needs. */
	public enum Kind { BOOLEAN, INT, FLOAT, STRING, ENUM, LIST }

	/** A single setting, backed by one field on one config instance. */
	public static final class Option extends ConfigTree {
		public final Field field;
		public final Object owner;
		public final Kind kind;
		public final double min;
		public final double max;
		public final boolean restart;
		public final String[] source;
		public final String[] requires;
		public final Scope scope;
		public final Object defaultValue;
		/** Enum constants, in declaration order. Null unless {@link #kind} is {@link Kind#ENUM}. */
		public final Object[] enumValues;

		private Object userValue;
		private boolean suppressed;
		/** Set while a server dictates this option's value; see {@link ConfigSync}. */
		private Object serverValue;
		private boolean serverControlled;

		Option(String parentPath, String key, Opt meta, Field field, Object owner, Object defaultValue, Scope inherited) {
			super(parentPath, key, meta.name(), meta.desc());
			this.field = field;
			this.owner = owner;
			this.kind = kindOf(field.getType());
			this.min = meta.min();
			this.max = meta.max();
			this.restart = meta.restart();
			this.source = meta.source();
			this.requires = meta.requires();
			this.scope = meta.scope() == Scope.INHERIT ? inherited : meta.scope();
			this.defaultValue = defaultValue;
			this.userValue = defaultValue;
			this.enumValues = field.getType().isEnum() ? field.getType().getEnumConstants() : null;
		}

		/** The value the user chose, ignoring any suppression by another mod. */
		public Object get() {
			return userValue;
		}

		/** Sets the user's value and pushes the resulting effective value into the backing field. */
		public void set(Object value) {
			userValue = value;
			push();
		}

		/**
		 * The value actually in force - what is in the backing field, which is what every mixin reads.
		 * Differs from {@link #get()} exactly when something has taken the option over: another mod
		 * providing the feature, or a server dictating it.
		 *
		 * <p>This is what a screen should SHOW. Showing {@link #get()} while the field says otherwise
		 * puts a tick beside a feature that is not running.
		 */
		public Object effective() {
			try {
				return field.get(owner);
			} catch (IllegalAccessException e) {
				return userValue;
			}
		}

		/** True when another mod already provides this feature and RetroAPI has stood down. */
		public boolean isSuppressed() {
			return suppressed;
		}

		void setSuppressed(boolean suppressed) {
			this.suppressed = suppressed;
			push();
		}

		/** True while a server is dictating this option, so the screen can say so. */
		public boolean isServerControlled() {
			return serverControlled;
		}

		/**
		 * Hands control of this option to the server. The user's own value is untouched and comes
		 * back the moment {@link #releaseToLocal()} is called on leaving.
		 */
		void takeOverByServer(Object value) {
			this.serverControlled = true;
			this.serverValue = value;
			push();
		}

		/** Gives the option back to the user's own value. */
		void releaseToLocal() {
			this.serverControlled = false;
			this.serverValue = null;
			push();
		}

		/**
		 * Writes the effective value into the field. A suppressed boolean reads as {@code false}
		 * so every gate downstream simply sees the feature as disabled; suppressed non-booleans are
		 * left alone because they are parameters of a boolean that is already off.
		 */
		void push() {
			Object effective = userValue;
			if (serverControlled) effective = serverValue;
			if (suppressed && kind == Kind.BOOLEAN) effective = Boolean.FALSE;
			try {
				field.set(owner, effective);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Could not write config field " + field, e);
			}
		}

		private static Kind kindOf(Class<?> type) {
			if (type == Boolean.class || type == boolean.class) return Kind.BOOLEAN;
			if (type == Integer.class || type == int.class) return Kind.INT;
			if (type == Float.class || type == float.class || type == Double.class || type == double.class) return Kind.FLOAT;
			if (type == String.class) return Kind.STRING;
			if (type.isEnum()) return Kind.ENUM;
			if (type.isArray()) return Kind.LIST;
			throw new IllegalArgumentException("Unsupported config field type: " + type);
		}
	}

	// ---------------------------------------------------------------- building

	/** Reflects over {@code root} and returns the top-level section. */
	public static Category build(String key, String name, Object root) {
		Category category = new Category(null, key, name, "");
		fill(category, root);
		return category;
	}

	private static void fill(Category parent, Object instance) {
		fill(parent, instance, Scope.WORLD);
	}

	private static void fill(Category parent, Object instance, Scope scope) {
		for (Field field : instance.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Cat cat = field.getAnnotation(Cat.class);
			if (cat != null) {
				Object child = read(field, instance);
				if (child == null) continue;
				Category node = new Category(parent.path, field.getName(), cat.name(), cat.desc());
				fill(node, child, cat.scope() == Scope.INHERIT ? scope : cat.scope());
				if (cat.source().length > 0) markSource(node, cat.source());
				if (!cat.gateOption().isEmpty()) {
					node.pendingGateOption = cat.gateOption();
					node.pendingGateValues = cat.gateValues();
				}
				parent.children.add(node);
				continue;
			}
			Opt opt = field.getAnnotation(Opt.class);
			if (opt == null) continue;
			parent.children.add(new Option(parent.path, field.getName(), opt, field, instance, read(field, instance), scope));
		}
		resolveGates(parent);
	}

	/**
	 * Wires up every gate recorded on a section that was just filled. Deferred to the end of the
	 * enclosing {@link #fill} call rather than resolved inline because the controlling option is a
	 * sibling declared anywhere in the same object - including after the gated section in field
	 * order - so it may not exist yet the moment the section's own node is built.
	 */
	private static void resolveGates(Category parent) {
		for (ConfigTree child : parent.children) {
			if (!(child instanceof Category node) || node.pendingGateOption.isEmpty()) continue;
			Option controller = findSibling(parent, node.pendingGateOption);
			if (controller == null) {
				throw new IllegalStateException("Cat.gateOption '" + node.pendingGateOption
					+ "' on section '" + node.key + "' does not name a sibling @Opt field");
			}
			node.gate = new Gate(controller, decodeGateValues(controller, node.pendingGateValues));
			node.pendingGateOption = "";
			node.pendingGateValues = EMPTY_SOURCES;
		}
	}

	private static Option findSibling(Category parent, String fieldName) {
		for (ConfigTree child : parent.children) {
			if (child instanceof Option option && option.key.equals(fieldName)) return option;
		}
		return null;
	}

	private static Object[] decodeGateValues(Option controller, String[] names) {
		Object[] values = new Object[names.length];
		for (int i = 0; i < names.length; i++) {
			if (controller.kind == Kind.ENUM) {
				values[i] = enumConstant(controller, names[i]);
			} else if (controller.kind == Kind.BOOLEAN) {
				values[i] = Boolean.parseBoolean(names[i]);
			} else {
				throw new IllegalArgumentException("Cat.gateOption '" + controller.key
					+ "' has kind " + controller.kind + ", which cannot gate a section");
			}
		}
		return values;
	}

	private static Object enumConstant(Option controller, String name) {
		for (Object constant : controller.enumValues) {
			if (((Enum<?>) constant).name().equalsIgnoreCase(name)) return constant;
		}
		throw new IllegalArgumentException("'" + name + "' is not one of "
			+ java.util.Arrays.toString(controller.enumValues) + " for gate '" + controller.key + "'");
	}

	/** Stamps a section's own sources onto every option inside that did not declare its own. */
	private static void markSource(Category category, String[] source) {
		for (ConfigTree child : category.children) {
			if (child instanceof Category nested) markSource(nested, source);
			else if (child instanceof Option option && option.source.length == 0) SOURCE_OVERRIDES.put(option, source);
		}
	}

	/**
	 * Sources inherited from a {@link Cat} rather than declared on the {@link Opt} itself.
	 * Kept beside the tree instead of in {@code Option.source} so the field stays final and the
	 * declared-vs-inherited distinction is not lost.
	 */
	private static final Map<Option, String[]> SOURCE_OVERRIDES = new LinkedHashMap<>();

	/** Every mod id that would suppress this option, declared or inherited from its section. */
	public static String[] sourcesOf(Option option) {
		return option.source.length > 0 ? option.source : SOURCE_OVERRIDES.getOrDefault(option, EMPTY_SOURCES);
	}

	private static final String[] EMPTY_SOURCES = new String[0];

	/**
	 * Asks whether an option's value still reaches the mod that has taken its feature over.
	 *
	 * <p>{@link Opt#source()} says "another mod owns this feature", which normally means the row is
	 * dead. Sometimes it is not: something can hand the player's value ON to that mod, in which case the
	 * row stays fully editable and only the implementation stands down. Whether such a hand-off exists
	 * is not something a config tree can know - it belongs to whoever built the bridge - so it is a hook
	 * rather than a hard-coded call. Answers false until someone installs one, which is the safe way
	 * round: an option with no bridge greys out and says who owns it.
	 *
	 * @see #install(java.util.function.Predicate)
	 */
	private static java.util.function.Predicate<String> delegation = path -> false;

	/**
	 * Installs the hand-off check described on {@link #delegation}. Called once, at init, by whoever
	 * owns the bridge; the tweaks half installs UniTweaks' at startup.
	 */
	public static void install(final java.util.function.Predicate<String> check) {
		delegation = check == null ? path -> false : check;
	}

	/** Whether this option's value is passed on to the mod that owns its feature. */
	public static boolean isDelegated(final String path) {
		return delegation.test(path);
	}

	/**
	 * The installed mod that is suppressing this option, or an empty string if none is. This is what
	 * the screen shows in the greyed-out row, so it names the mod actually responsible rather than
	 * every mod that could have been.
	 */
	public static String suppressorOf(Option option) {
		for (String modId : sourcesOf(option)) {
			if (FabricLoader.getInstance().isModLoaded(modId)) return modId;
		}
		return "";
	}

	/**
	 * The mod id an unsatisfied {@link Opt#requires()} would show the player, or an empty string
	 * when the option declares no requirement or at least one of the mods it names is installed.
	 * The mirror image of {@link #suppressorOf}: that one greys an option out because a mod IS
	 * present, this one because none of the required ones is.
	 */
	public static String missingRequirementOf(Option option) {
		if (option.requires.length == 0) return "";
		for (String modId : option.requires) {
			if (FabricLoader.getInstance().isModLoaded(modId)) return "";
		}
		return option.requires[0];
	}

	/**
	 * Why this option cannot be EDITED right now, or an empty string when it can.
	 *
	 * <p>Deliberately not the same question as {@link #isStoodDownFor}. An option whose behaviour
	 * another mod has taken over is still editable as long as RetroTweaks can pass the value on to that
	 * mod - which is what {@link com.periut.retrotweaks.compat.UniTweaksBridge} does. The value
	 * is kept in {@link Option#get()} either way, saved to RetroTweaks' own config either way, and
	 * becomes live again by itself the day the other mod is uninstalled; greying the row out only ever
	 * hid a setting the player could perfectly well have changed.
	 *
	 * <p>So only two things actually block editing: an optional API the option needs is not installed
	 * (nothing anywhere can act on the value), or another mod owns it and there is no bridge for it
	 * (the value would go nowhere).
	 */
	public static String unavailableReasonOf(Option option) {
		String missing = missingRequirementOf(option);
		if (!missing.isEmpty()) return "needs " + missing + " installed";
		String suppressor = suppressorOf(option);
		if (!suppressor.isEmpty() && !isDelegated(option.path)) {
			return "handled by " + suppressor;
		}
		return "";
	}

	/**
	 * Whether RetroTweaks' own implementation of this option must stand down - i.e. whether the
	 * effective value written into the backing field is forced off, regardless of what the player chose.
	 *
	 * <p>This is the runtime half of what {@link #unavailableReasonOf} used to decide on its own, split
	 * out because the two answers genuinely differ: a bridged option stands down here (RetroTweaks' own
	 * mixin must not act while another mod is doing the job) while remaining fully editable there.
	 * Keeping the stand-down is what makes bridging safe for options whose mixin still loads - the
	 * player's value reaches the other mod, and ours reads false, so the feature is never applied twice.
	 */
	public static boolean isStoodDownFor(Option option) {
		return !suppressorOf(option).isEmpty() || !missingRequirementOf(option).isEmpty();
	}

	/**
	 * A short note for a row whose behaviour belongs to another mod but which is still editable, or ""
	 * when there is nothing to say. Shown alongside the value rather than in place of it.
	 */
	public static String sharedNoteOf(Option option) {
		String suppressor = suppressorOf(option);
		if (suppressor.isEmpty()) return "";
		return isDelegated(option.path)
			? "shared with " + suppressor
			: "";
	}

	private static Object read(Field field, Object instance) {
		try {
			return field.get(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not read config field " + field, e);
		}
	}

	// ---------------------------------------------------------------- traversal

	/** Visits every option in this subtree, depth first, in declaration order. */
	public static void forEachOption(ConfigTree node, java.util.function.Consumer<Option> action) {
		if (node instanceof Option option) {
			action.accept(option);
			return;
		}
		for (ConfigTree child : ((Category) node).children) forEachOption(child, action);
	}

	/**
	 * Walks a path of {@link Cat} field names from {@code root} and returns the section at the end, or
	 * null if any segment is missing or is not itself a section. Lets a caller (e.g. the config screen
	 * recognising the HUD Positions page for the live preview) name a node by its stable position in
	 * the tree instead of keeping a reference from when the tree was built.
	 */
	public static Category find(Category root, String... keyPath) {
		Category current = root;
		for (String key : keyPath) {
			Category next = null;
			for (ConfigTree child : current.children) {
				if (child instanceof Category category && category.key.equals(key)) {
					next = category;
					break;
				}
			}
			if (next == null) return null;
			current = next;
		}
		return current;
	}

	/** The direct {@link Option} child of {@code category} named {@code key}, or null if none matches. */
	public static Option findOption(Category category, String key) {
		for (ConfigTree child : category.children) {
			if (child instanceof Option option && option.key.equals(key)) return option;
		}
		return null;
	}

	// ---------------------------------------------------------------- json

	/** Serialises this subtree to nested maps ready for {@code Json}. */
	public static Map<String, Object> toJson(Category category) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (ConfigTree child : category.children) {
			if (child instanceof Category nested) {
				out.put(nested.key, toJson(nested));
			} else {
				Option option = (Option) child;
				out.put(option.key, encode(option, option.get()));
			}
		}
		return out;
	}

	private static Object encode(Option option, Object value) {
		if (value == null) return null;
		if (option.kind == Kind.ENUM) return ((Enum<?>) value).name();
		if (option.kind == Kind.LIST) {
			List<Object> list = new ArrayList<>();
			int length = java.lang.reflect.Array.getLength(value);
			for (int i = 0; i < length; i++) list.add(java.lang.reflect.Array.get(value, i));
			return list;
		}
		return value;
	}

	/**
	 * Reads values out of a parsed JSON tree. Anything missing keeps its default and anything
	 * unparseable is logged and skipped, so a truncated or hand-mangled config never stops the game
	 * from starting - it just loses the settings it could not read.
	 */
	public static void fromJson(Category category, Map<String, Object> json) {
		for (ConfigTree child : category.children) {
			Object raw = json.get(child.key);
			if (child instanceof Category nested) {
				if (raw instanceof Map<?, ?> map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> typed = (Map<String, Object>) map;
					fromJson(nested, typed);
				}
				continue;
			}
			if (raw == null || !json.containsKey(child.key)) continue;
			Option option = (Option) child;
			try {
				option.set(decode(option, raw));
			} catch (RuntimeException e) {
				RetroAPI.LOGGER.warn("Ignoring bad config value for '{}': {}", option.key, e.getMessage());
			}
		}
	}

	private static Object decode(Option option, Object raw) {
		switch (option.kind) {
			case BOOLEAN:
				if (raw instanceof Boolean b) return b;
				return Boolean.parseBoolean(String.valueOf(raw));
			case INT:
				return (int) clamp(((Number) raw).doubleValue(), option);
			case FLOAT:
				return (float) clamp(((Number) raw).doubleValue(), option);
			case STRING:
				return String.valueOf(raw);
			case ENUM:
				for (Object constant : option.enumValues) {
					if (((Enum<?>) constant).name().equalsIgnoreCase(String.valueOf(raw))) return constant;
				}
				throw new IllegalArgumentException("'" + raw + "' is not one of " + java.util.Arrays.toString(option.enumValues));
			case LIST:
			default:
				List<?> list = (List<?>) raw;
				Class<?> component = option.field.getType().getComponentType();
				Object array = java.lang.reflect.Array.newInstance(component, list.size());
				for (int i = 0; i < list.size(); i++) {
					Object element = list.get(i);
					if (component == Integer.class && element instanceof Number n) element = n.intValue();
					else if (component == String.class) element = String.valueOf(element);
					java.lang.reflect.Array.set(array, i, element);
				}
				return array;
		}
	}

	// ---------------------------------------------------------------- the wire

	/**
	 * One value as a single string, for {@link ConfigSync}'s packets. Deliberately not JSON: a config
	 * payload is a flat list of one value per key, and a string per value keeps the packet readable and
	 * the same shape whether it carries the whole set at join or one option an operator just changed.
	 *
	 * <p>Lists have no string form here and are excluded from the wire by
	 * {@link #isSyncable(Option)}; they are edited in the file, which is what a list-shaped option is
	 * for.
	 */
	public static String encodeString(Option option, Object value) {
		if (value == null) return "";
		return option.kind == Kind.ENUM ? ((Enum<?>) value).name() : String.valueOf(value);
	}

	/** {@link #encodeString}'s inverse. Throws when the text does not name a value this option can hold. */
	public static Object decodeString(Option option, String text) {
		return switch (option.kind) {
			case BOOLEAN -> Boolean.parseBoolean(text);
			case INT -> (int) clamp(Double.parseDouble(text), option);
			case FLOAT -> (float) clamp(Double.parseDouble(text), option);
			case STRING -> text;
			case ENUM -> {
				for (Object constant : option.enumValues) {
					if (((Enum<?>) constant).name().equalsIgnoreCase(text)) yield constant;
				}
				throw new IllegalArgumentException("'" + text + "' is not one of "
					+ java.util.Arrays.toString(option.enumValues));
			}
			case LIST -> throw new IllegalArgumentException("a list option has no single-value form");
		};
	}

	/** Whether this option can travel over the wire at all - see {@link #encodeString}. */
	public static boolean isSyncable(Option option) {
		return option.kind != Kind.LIST;
	}

	private static double clamp(double value, Option option) {
		// A slider's bounds are the only validation these values get; a config that says the chat
		// history is 2 billion lines long should not be honoured just because it parsed.
		if (option.max > option.min) return Math.max(option.min, Math.min(option.max, value));
		return value;
	}
}
