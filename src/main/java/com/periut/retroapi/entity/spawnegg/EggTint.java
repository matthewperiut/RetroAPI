package com.periut.retroapi.entity.spawnegg;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * A representative colour for a mob, read off the mob's own skin.
 *
 * <p>A mod that has not drawn a spawn egg gets the plain one, and a mod with six mobs gets the plain one
 * six times over - six identical items in the tab. Tinting each of them by its mob's own colours makes
 * them tellable apart before anybody draws a sprite, and costs the mod nothing.
 *
 * <p><b>Why the mean is weighted by saturation.</b> A flat average of every pixel makes almost every mob
 * the same muddy grey-brown, because a skin is mostly shading, hooves, snout and shadow - large areas of
 * near-grey - with the colour that actually identifies the mob confined to a few small patches. Weighting
 * each pixel by its HSB saturation lets those patches carry the result: the creeper comes out green and
 * the pig comes out pink, rather than both coming out beige.
 *
 * <p>The weight is {@code 0.05 + saturation} rather than plain {@code saturation} so that a texture which
 * is genuinely greyscale everywhere - saturation zero on every pixel - still has weight to divide by, and
 * degrades to an ordinary unweighted mean instead of a division by zero.
 *
 * <p>Deliberately standalone: nothing here touches RetroAPI, and nothing logs. The caller decides what a
 * failure means and says so. Anything that goes wrong - no texture, no such constructor, a constructor
 * that will not run without a world - comes back as {@link #NO_TINT}.
 */
public final class EggTint {
	private EggTint() {
	}

	/** Returned when no colour could be read, so the caller can leave the egg untinted. */
	private static final int NO_TINT = -1;

	/** Guards against a pack shipping something absurd; a mob skin is 64x32. */
	private static final int MAX_PIXELS = 4096 * 4096;

	/** Keeps an all-grey texture from summing to zero weight. See the class javadoc. */
	private static final float SATURATION_FLOOR = 0.05F;

	/**
	 * Derived tints, by entity class.
	 *
	 * <p>A plain {@link HashMap} on purpose: every call happens during init, on the one thread that
	 * registers items, so there is nothing to synchronise against. Failures are cached too - a mob whose
	 * constructor needs a world will fail identically every time, and is not worth retrying.
	 */
	private static final Map<Class<?>, Integer> CACHE = new HashMap<>();

	/**
	 * The colour to tint this mob's spawn egg.
	 *
	 * @param entityClass the mob, e.g. {@code Creeper.class}
	 * @return a colour as {@code 0xRRGGBB}, or {@code -1} if none could be derived
	 */
	public static int forEntityClass(final Class<?> entityClass) {
		if (entityClass == null) {
			return NO_TINT;
		}

		final Integer cached = CACHE.get(entityClass);
		if (cached != null) {
			return cached;
		}

		final int tint = derive(entityClass);
		CACHE.put(entityClass, tint);
		return tint;
	}

	private static int derive(final Class<?> entityClass) {
		// Only a living entity has a skin: Entity.getTexture() returns null, and the field it would read
		// is declared on LivingEntity.
		if (!LivingEntity.class.isAssignableFrom(entityClass)) {
			return NO_TINT;
		}

		final BufferedImage texture = load(texturePath(entityClass));
		if (texture == null) {
			return NO_TINT;
		}
		return averageColour(texture);
	}

	/**
	 * The mob's texture path, which is only readable off an instance.
	 *
	 * <p>There is no world at init time, so the mob is built against a null one. {@code Entity(World)} and
	 * {@code LivingEntity(World)} only store the reference, but a subclass constructor is free to walk it -
	 * to ask for a random number, or register itself with something - and that is fine: it throws, and this
	 * mob simply does not get a tint. Hence {@link Throwable} rather than {@link Exception}, since a
	 * constructor can fail as a {@code NullPointerException}, an {@code ExceptionInInitializerError} or a
	 * {@code NoSuchMethodError} depending on how far it gets.
	 */
	private static String texturePath(final Class<?> entityClass) {
		try {
			final Constructor<?> constructor = entityClass.getDeclaredConstructor(World.class);
			constructor.setAccessible(true);   // a mob class need not be public
			final LivingEntity mob = (LivingEntity) constructor.newInstance((Object) null);
			return mob.getTexture();
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * The texture as an image, or null.
	 *
	 * <p>{@code getTexture()} gives a classloader path such as {@code /assets/mymod/textures/entity/dog.png}.
	 * A mod laid out for StationAPI keeps the same file one segment deeper, under {@code stationapi/}, so
	 * both layouts are tried - the given path first, then the same path with that segment added or removed.
	 * This is the policy {@code AtlasExpander} uses for block and item sprites.
	 */
	private static BufferedImage load(final String path) {
		if (path == null || path.isBlank()) {
			return null;
		}

		final String resource = path.startsWith("/") ? path : "/" + path;
		final BufferedImage image = read(resource);
		if (image != null) {
			return image;
		}

		final String alternate = swapStationApi(resource);
		return alternate == null ? null : read(alternate);
	}

	private static BufferedImage read(final String resource) {
		try (InputStream in = EggTint.class.getResourceAsStream(resource)) {
			return in == null ? null : ImageIO.read(in);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * The same resource in the other layout: {@code /textures/} becomes {@code /stationapi/textures/} and
	 * back again. Null when the path has no {@code /textures/} segment to move.
	 */
	private static String swapStationApi(final String resource) {
		final int station = resource.indexOf("/stationapi/textures/");
		if (station >= 0) {
			return resource.substring(0, station) + "/textures/"
				+ resource.substring(station + "/stationapi/textures/".length());
		}

		final int plain = resource.indexOf("/textures/");
		if (plain >= 0) {
			return resource.substring(0, plain) + "/stationapi/textures/"
				+ resource.substring(plain + "/textures/".length());
		}
		return null;
	}

	/**
	 * The saturation-weighted mean of every pixel that is not fully transparent.
	 *
	 * <p>Read through {@code getRGB}, which normalises whatever the file's own layout is to ARGB, so an
	 * indexed or greyscale PNG needs no special case.
	 */
	private static int averageColour(final BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
			return NO_TINT;
		}

		double red = 0.0;
		double green = 0.0;
		double blue = 0.0;
		double totalWeight = 0.0;
		int opaquePixels = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0) {
					continue;   // fully transparent: not part of the mob
				}

				final int pixelRed = (argb >> 16) & 0xFF;
				final int pixelGreen = (argb >> 8) & 0xFF;
				final int pixelBlue = argb & 0xFF;

				final float weight = SATURATION_FLOOR
					+ Color.RGBtoHSB(pixelRed, pixelGreen, pixelBlue, null)[1];
				red += pixelRed * weight;
				green += pixelGreen * weight;
				blue += pixelBlue * weight;
				totalWeight += weight;
				opaquePixels++;
			}
		}

		if (opaquePixels == 0) {
			return NO_TINT;   // nothing but transparency; the floor guarantees weight otherwise
		}

		return channel(red / totalWeight) << 16
			| channel(green / totalWeight) << 8
			| channel(blue / totalWeight);
	}

	private static int channel(final double value) {
		final long rounded = Math.round(value);
		return (int) Math.max(0L, Math.min(255L, rounded));
	}
}
