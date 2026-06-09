package com.periut.retroapi.sound.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Clean reimplementation of StationAPI's deprecated {@code RecursiveReader}: given a classpath
 * resource path (e.g. {@code "/assets/aether/stationapi/sounds/sound"}) it enumerates every matching
 * resource URL underneath it, handling both jar-packaged ({@code jar:} / {@link JarURLConnection})
 * and exploded ({@code file:}) classpath layouts.
 *
 * <p>Deliberate deviations from StationAPI's {@code RecursiveReader}:</p>
 * <ul>
 *   <li>Resolves resources off <em>this class's</em> loader (the same strategy
 *       {@code LangLoader} uses successfully), NOT {@code getClassLoader().getParent()} - the
 *       {@code getParent()} approach breaks under Fabric's {@code KnotClassLoader}.</li>
 *   <li>Uses a clean {@code while (hasMoreElements())} loop over jar entries instead of
 *       StationAPI's {@code for}-loop that skipped the last entry and NPE'd on an empty jar.</li>
 * </ul>
 */
public final class RetroResourceWalker {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/Sound");

	private RetroResourceWalker() {
	}

	/**
	 * Walk every resource under {@code basePath} (an absolute classpath path beginning with
	 * {@code /assets/...}) and return the URLs whose lowercased name matches {@code filter}.
	 *
	 * @param basePath absolute classpath path, e.g. {@code "/assets/modid/stationapi/sounds/sound"}
	 * @param filter   predicate over the lowercased resource path/URL string
	 * @return a stable-ordered set of matching resource URLs (empty if the base path is absent)
	 */
	public static Set<URL> walk(String basePath, Predicate<String> filter) {
		Set<URL> out = new LinkedHashSet<>();
		try {
			// Enumerate all classpath roots that provide this path (multiple mod jars can).
			String lookup = basePath.startsWith("/") ? basePath.substring(1) : basePath;
			Enumeration<URL> roots = RetroResourceWalker.class.getClassLoader().getResources(lookup);
			while (roots.hasMoreElements()) {
				URL root = roots.nextElement();
				collect(root, lookup, filter, out);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to walk resources under {}", basePath, e);
		}
		return out;
	}

	private static void collect(URL root, String lookup, Predicate<String> filter, Set<URL> out) {
		try {
			URLConnection connection = root.openConnection();
			if (connection instanceof JarURLConnection) {
				collectFromJar((JarURLConnection) connection, lookup, filter, out);
			} else if ("file".equals(root.getProtocol())) {
				collectFromDir(root, filter, out);
			} else {
				LOGGER.warn("Unsupported sound resource protocol for {}", root);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to read sound resources from {}", root, e);
		}
	}

	private static void collectFromJar(JarURLConnection connection, String lookup,
	                                   Predicate<String> filter, Set<URL> out) throws IOException {
		JarFile jar = connection.getJarFile();
		String prefix = lookup.endsWith("/") ? lookup : lookup + "/";
		String jarUrl = connection.getJarFileURL().toString();
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry.isDirectory()) {
				continue;
			}
			String name = entry.getName();
			if (name.startsWith(prefix) && filter.test(name.toLowerCase())) {
				try {
					out.add(new URL("jar:" + jarUrl + "!/" + name));
				} catch (IOException e) {
					LOGGER.error("Bad jar entry URL for {}", name, e);
				}
			}
		}
	}

	private static void collectFromDir(URL root, Predicate<String> filter, Set<URL> out) {
		File dir;
		try {
			dir = new File(root.toURI());
		} catch (Exception e) {
			dir = new File(root.getPath());
		}
		Path base = dir.toPath();
		try (Stream<Path> walk = Files.walk(base)) {
			walk.filter(Files::isRegularFile)
				.filter(p -> filter.test(p.toString().toLowerCase()))
				.forEach(p -> {
					try {
						out.add(p.toUri().toURL());
					} catch (IOException e) {
						LOGGER.error("Bad file URL for {}", p, e);
					}
				});
		} catch (IOException e) {
			LOGGER.error("Failed to walk directory {}", base, e);
		}
	}

	/**
	 * Compute the path relative to {@code "/<channel>/"} inside a resource URL, with the leading
	 * slash stripped. Mirrors StationAPI's {@code SoundManagerMixin} split-on-channel logic.
	 *
	 * @param url     the resource URL, e.g. {@code jar:.../sounds/sound/other/x/y.ogg}
	 * @param channel one of {@code "sound"}, {@code "streaming"}, {@code "music"}
	 * @return the relative path, e.g. {@code "other/x/y.ogg"}, or {@code null} if the marker is absent
	 */
	public static String relativize(URL url, String channel) {
		String s = url.toString().replace("\\", "/");
		String marker = "/" + channel + "/";
		int idx = s.lastIndexOf(marker);
		if (idx < 0) {
			return null;
		}
		String rel = s.substring(idx + marker.length());
		while (rel.startsWith("/")) {
			rel = rel.substring(1);
		}
		return rel;
	}
}
