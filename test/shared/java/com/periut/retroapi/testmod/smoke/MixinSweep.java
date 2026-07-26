package com.periut.retroapi.testmod.smoke;

import com.periut.retroapi.testmod.TestMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads every class RetroAPI mixes into, so every mixin is applied in one pass.
 *
 * <p>Mixin validates a mixin lazily - the moment the game happens to load the class it targets. A
 * {@code @Shadow} naming a field that does not exist, an {@code @At} whose instruction is not in the
 * method, an injector whose callback signature is wrong: none of it is a compile error, and none of it
 * shows up until something touches that one class. A mixin on a class the title screen never loads can
 * ship broken and only crash for the player who opens the right screen.
 *
 * <p>This reads RetroAPI's mixin configs off the classpath, pulls the target list out of each mixin
 * class's {@code @Mixin} annotation, and force-loads every target with {@code initialize = false} - the
 * class is defined (so every mixin on it is applied and every injector runs) but no static initializer
 * fires, so touching render classes on a dedicated server is harmless. Whatever Mixin rejects surfaces
 * here, all at once, in a run that takes seconds.
 *
 * <p>The configs are read as resources rather than through {@code Mixins.getConfigs()}, which reports
 * nothing from mod code, and the {@code @Mixin} annotation is parsed from the class bytes because it is
 * {@code RetentionPolicy.CLASS} - invisible to reflection. Target names come out of the annotation
 * already in whatever namespace the jar was remapped to, so this works in dev and in production.
 */
public final class MixinSweep {
	private MixinSweep() {}

	/** The mods to sweep. The StationAPI compat mod is only installed in that build; absent is fine. */
	public static final String[] RETROAPI_MODS = {
		"retroapi",
		"retroapi-stationapi",
	};

	private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";

	/** What a sweep found. */
	public record Report(int configs, int targets, int loaded, int absent,
			List<String> failures, List<String> misfiled) {
		public boolean pass() {
			return configs > 0 && targets > 0 && failures.isEmpty() && misfiled.isEmpty();
		}

		public List<String> lines() {
			List<String> out = new ArrayList<>();
			if (configs == 0) {
				out.add("mixinSweep: FAIL - no mixin configs found on the classpath");
				return out;
			}
			if (targets == 0) {
				out.add("mixinSweep: FAIL - " + configs + " config(s) but no @Mixin targets parsed");
				return out;
			}
			out.add("mixinSweep: " + (pass() ? "PASS" : "FAIL") + " - " + loaded + "/" + targets
				+ " target classes applied cleanly, from " + configs + " config(s)"
				+ (absent > 0 ? ", " + absent + " not on this side's classpath" : "")
				+ (failures.isEmpty() ? "" : ", " + failures.size() + " FAILED")
				+ (misfiled.isEmpty() ? "" : ", " + misfiled.size() + " MISFILED"));
			out.addAll(failures);
			if (!misfiled.isEmpty()) {
				out.add("  these mixins are in the config's common \"mixins\" list but every class they");
				out.add("  target is absent on this side - move them to \"client\"/\"server\" so the game");
				out.add("  stops trying to load them:");
				out.addAll(misfiled);
			}
			return out;
		}
	}

	/** Sweeps the named mods' mixin configs. Mods that are not installed are skipped silently. */
	public static Report run(String... modIds) {
		ClassLoader loader = MixinSweep.class.getClassLoader();
		boolean client = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

		int configs = 0;
		Set<String> mixinClasses = new LinkedHashSet<>();
		// Mixins from the config's common "mixins" list, so a server-only one sitting there can be named.
		Set<String> commonMixins = new LinkedHashSet<>();
		List<ModContainer> mods = new ArrayList<>();
		for (String modId : modIds) {
			FabricLoader.getInstance().getModContainer(modId).ifPresent(mods::add);
		}

		for (ModContainer mod : mods) {
			String meta = read(mod, "fabric.mod.json");
			if (meta == null) continue;
			for (String configName : array(meta, "mixins")) {
				String json = read(mod, configName);
				if (json == null) {
					TestMod.LOGGER.warn("[smoke] {} declares {} but it is not in the mod", mod.getMetadata().getId(), configName);
					continue;
				}
				configs++;
				String pkg = string(json, "package");
				if (pkg == null) {
					TestMod.LOGGER.warn("[smoke] {} has no mixin package", configName);
					continue;
				}
				for (String n : array(json, "mixins")) {
					String c = (pkg + "." + n).replace('.', '/');
					mixinClasses.add(c);
					commonMixins.add(c);
				}
				for (String n : array(json, client ? "client" : "server")) {
					mixinClasses.add((pkg + "." + n).replace('.', '/'));
				}
				TestMod.LOGGER.debug("[smoke] {} -> {} mixins for this side", configName, mixinClasses.size());
			}
		}

		Map<String, Set<String>> byMixin = new LinkedHashMap<>();
		Set<String> targets = new TreeSet<>();
		for (String mixinClass : mixinClasses) {
			Set<String> own = targetsOf(mods, loader, mixinClass);
			byMixin.put(mixinClass, own);
			targets.addAll(own);
		}

		List<String> failures = new ArrayList<>();
		Set<String> absentTargets = new LinkedHashSet<>();
		int loaded = 0;
		for (String target : targets) {
			String binary = target.replace('/', '.');
			try {
				// initialize = false: define + transform the class (applying every mixin on it) without
				// running its static initializer.
				Class.forName(binary, false, loader);
				loaded++;
			} catch (ClassNotFoundException | NoClassDefFoundError e) {
				// A target that only exists on the other side, or behind a mod that is not installed.
				absentTargets.add(target);
			} catch (Throwable t) {
				// Fabric refuses to load a server class on a client (and vice versa) before Mixin ever
				// sees it - that is "not on this side", not an apply failure.
				if (isWrongSide(t)) {
					absentTargets.add(target);
					continue;
				}
				failures.add("  " + binary + "  <-  " + describe(t));
				TestMod.LOGGER.error("[smoke] mixin apply failed for {}", binary, t);
			}
		}

		// A common-section mixin whose every target is missing here is filed wrong: the game tries to
		// load each of those classes on every launch and logs a warning for each. It belongs in the
		// "client"/"server" list instead. (A mixin with SOME targets present is a legitimate straddle.)
		List<String> misfiled = new ArrayList<>();
		for (String mixinClass : commonMixins) {
			Set<String> own = byMixin.getOrDefault(mixinClass, Set.of());
			if (!own.isEmpty() && absentTargets.containsAll(own)) {
				misfiled.add("    " + mixinClass.replace('/', '.') + "  ->  " + own);
			}
		}
		return new Report(configs, targets.size(), loaded, absentTargets.size(), failures, misfiled);
	}

	// --- reading the mixin metadata -----------------------------------------------------------------

	/** Target classes named by one mixin's {@code @Mixin(value = ..., targets = ...)}. */
	private static Set<String> targetsOf(List<ModContainer> mods, ClassLoader loader, String mixinClass) {
		Set<String> out = new LinkedHashSet<>();
		byte[] bytes = bytes(mods, loader, mixinClass + ".class");
		if (bytes == null) {
			TestMod.LOGGER.warn("[smoke] mixin class {} listed in a config but not on the classpath", mixinClass);
			return out;
		}
		ClassNode node = new ClassNode();
		new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		List<AnnotationNode> annotations = node.invisibleAnnotations;
		if (annotations == null) return out;
		for (AnnotationNode annotation : annotations) {
			if (!MIXIN_DESC.equals(annotation.desc) || annotation.values == null) continue;
			for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
				Object key = annotation.values.get(i);
				Object value = annotation.values.get(i + 1);
				if (!(value instanceof List<?> list)) continue;
				if ("value".equals(key)) {
					for (Object o : list) {
						if (o instanceof Type type) out.add(type.getInternalName());
					}
				} else if ("targets".equals(key)) {
					for (Object o : list) {
						if (o instanceof String s) out.add(s.replace('.', '/'));
					}
				}
			}
		}
		return out;
	}

	// --- tiny readers -------------------------------------------------------------------------------

	/**
	 * Reads a file out of a mod. Goes through {@link ModContainer#findPath} rather than the classloader:
	 * a mod's own resources are not reliably visible to another mod's {@code getResourceAsStream}, in dev
	 * or in production. Falls back to the classpath for anything the mod roots do not hold.
	 */
	private static byte[] bytes(List<ModContainer> mods, ClassLoader loader, String path) {
		for (ModContainer mod : mods) {
			byte[] b = readBytes(mod, path);
			if (b != null) return b;
		}
		try (InputStream in = loader.getResourceAsStream(path)) {
			return in == null ? null : in.readAllBytes();
		} catch (Exception e) {
			return null;
		}
	}

	/** One mod's copy of a file, or null. In dev a mod has several roots, so ask the container. */
	private static byte[] readBytes(ModContainer mod, String path) {
		try {
			Optional<Path> found = mod.findPath(path);
			return found.isPresent() ? Files.readAllBytes(found.get()) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String read(ModContainer mod, String path) {
		byte[] b = readBytes(mod, path);
		return b == null ? null : new String(b, StandardCharsets.UTF_8);
	}

	/**
	 * Pulls {@code "key": "value"} / {@code "key": [ "a", "b" ]} out of a mixin config. Deliberately not a
	 * JSON parser: these are RetroAPI's own configs, with a flat shape and no nested arrays, and a test
	 * harness should not care which JSON library happens to be on the classpath.
	 */
	private static String string(String json, String key) {
		Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
		return m.find() ? m.group(1) : null;
	}

	private static List<String> array(String json, String key) {
		List<String> out = new ArrayList<>();
		Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
		if (!m.find()) return out;
		Matcher item = Pattern.compile("\"([^\"]+)\"").matcher(m.group(1));
		while (item.find()) {
			out.add(item.group(1));
		}
		return out;
	}

	/** Fabric's env stripping: "Cannot load class X in environment type CLIENT". */
	private static boolean isWrongSide(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			String message = c.getMessage();
			if (message != null && message.contains("in environment type")) {
				return true;
			}
			if (c.getCause() == c) break;
		}
		return false;
	}

	/** Mixin nests the useful message a couple of causes deep; pull the whole chain onto one line. */
	private static String describe(Throwable t) {
		StringBuilder sb = new StringBuilder();
		for (Throwable c = t; c != null && sb.length() < 600; c = c.getCause()) {
			if (sb.length() > 0) sb.append(" | caused by ");
			sb.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
			if (c.getCause() == c) break;
		}
		return sb.toString();
	}
}
