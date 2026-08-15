package com.periut.retrotweaks.compat;

import com.periut.retrotweaks.RetroTweaks;

import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Talks to StationAPI and RetroAPI without compiling against either.
 *
 * <p>RetroTweaks must run on a bare loader, so it cannot import their classes - but when one of them
 * is installed it holds information vanilla does not. A modded fuel or smelting recipe registered
 * through StationAPI is invisible to vanilla's furnace, so a shift-click that asks vanilla "is this
 * fuel?" would say no and refuse to move it.
 *
 * <p>So each lookup is resolved once by name through {@link MethodHandles}, cached, and used from
 * then on; if the class is absent, or its signature has moved, the handle stays null and the caller
 * silently uses the vanilla answer. That is the whole pattern - anything else that needs an API's
 * knowledge should be added here rather than reaching for a hard dependency.
 */
public final class ApiBridge {

	private ApiBridge() {}

	private static final MethodHandle STATION_FUEL_TIME = find(
		"net.modificationstation.stationapi.api.recipe.FuelRegistry", "getFuelTime",
		MethodType.methodType(int.class, ItemStack.class));

	private static final MethodHandle STATION_SMELTING_RESULT = find(
		"net.modificationstation.stationapi.api.recipe.SmeltingRegistry", "getResultFor",
		MethodType.methodType(ItemStack.class, ItemStack.class));

	private static final MethodHandle RETRO_FUEL_TIME = find(
		"com.periut.retroapi.register.recipe.RetroRecipes", "getTotalFuelTime",
		MethodType.methodType(int.class, ItemStack.class));

	/**
	 * Burn time in ticks according to whichever API is installed, or {@code -1} when none can
	 * answer and the caller should ask vanilla.
	 */
	public static int fuelTime(ItemStack stack) {
		Integer station = invokeInt(STATION_FUEL_TIME, stack);
		if (station != null) return station;
		Integer retro = invokeInt(RETRO_FUEL_TIME, stack);
		if (retro != null) return retro;
		return -1;
	}

	/**
	 * What the stack smelts into according to StationAPI, or {@code null} when it cannot answer -
	 * which is not the same as "does not smelt", so the caller must still ask vanilla.
	 */
	public static ItemStack smeltingResult(ItemStack stack) {
		if (STATION_SMELTING_RESULT == null) return null;
		try {
			return (ItemStack) STATION_SMELTING_RESULT.invoke(stack);
		} catch (Throwable t) {
			return null;
		}
	}

	/** True when at least one API is answering these lookups. */
	public static boolean hasRecipeApi() {
		return STATION_FUEL_TIME != null || STATION_SMELTING_RESULT != null || RETRO_FUEL_TIME != null;
	}

	// --- RetroAPI item registry, used to register FishinFoodTweaks' four non-vanilla fish types (see
	// feature/fishing/Fishing.java) when RetroAPI is present. RetroItemAccess is a duck interface
	// RetroAPI mixes onto every net.minecraft.item.Item; once its mixin has loaded, a plain Item
	// instance gains texture()/food()/register() methods a bare loader's Item class simply does not
	// have, which is why these are java.lang.reflect.Method rather than the typed MethodHandles
	// above - their exact descriptors name RetroAPI-only argument types (NamespacedIdentifier,
	// RetroFood.OnEaten) that cannot be imported here. ---

	private static final Class<?> RETRO_ON_EATEN;
	private static final Method RETRO_ID_FROM;
	private static final Method RETRO_ALLOCATE_ITEM_ID;
	private static final Method RETRO_ITEM_TEXTURE;
	private static final Method RETRO_ITEM_FOOD;
	private static final Method RETRO_ITEM_REGISTER;

	/**
	 * {@code Item(int)} itself, not RetroAPI's - {@code protected}, so a class outside
	 * {@code net.minecraft.item} needs this to call it at all, RetroAPI installed or not.
	 */
	private static final java.lang.reflect.Constructor<Item> ITEM_CONSTRUCTOR = itemConstructor();

	private static java.lang.reflect.Constructor<Item> itemConstructor() {
		try {
			java.lang.reflect.Constructor<Item> ctor = Item.class.getDeclaredConstructor(int.class);
			ctor.setAccessible(true);
			return ctor;
		} catch (NoSuchMethodException e) {
			return null; // vanilla; should never happen
		}
	}

	static {
		Class<?> onEaten = null;
		Method idFrom = null;
		Method allocate = null;
		Method texture = null;
		Method food = null;
		Method register = null;
		try {
			// RetroAPI-only classes first, so a bare loader (which does have OSL's NamespacedIdentifier,
			// since OSL is bundled with RetroTweaks itself - see build.gradle) fails fast on these with a
			// plain ClassNotFoundException, before ever reaching the Item.getMethod calls below, which
			// would otherwise throw NoSuchMethodException on a bare Item class and be mistaken for "the
			// API moved" instead of "the API is not installed".
			Class<?> itemIds = Class.forName("com.periut.retroapi.register.item.RetroItemIds");
			onEaten = Class.forName("com.periut.retroapi.register.item.RetroFood$OnEaten");
			Class<?> namespacedId = Class.forName("net.ornithemc.osl.core.api.util.NamespacedIdentifier");
			Class<?> namespacedIds = Class.forName("net.ornithemc.osl.core.api.util.NamespacedIdentifiers");
			idFrom = namespacedIds.getMethod("from", String.class, String.class);
			allocate = itemIds.getMethod("allocate");
			texture = Item.class.getMethod("texture", namespacedId);
			food = Item.class.getMethod("food", int.class, boolean.class, onEaten);
			register = Item.class.getMethod("register", namespacedId);
		} catch (ClassNotFoundException | LinkageError e) {
			// RetroAPI is not installed; the non-vanilla fish types simply do not exist this session.
		} catch (ReflectiveOperationException e) {
			RetroTweaks.LOGGER.warn("RetroAPI is installed but its item registry was not where RetroTweaks "
				+ "expected it; the non-vanilla fish types will not be registered", e);
		}
		RETRO_ON_EATEN = onEaten;
		RETRO_ID_FROM = idFrom;
		RETRO_ALLOCATE_ITEM_ID = allocate;
		RETRO_ITEM_TEXTURE = texture;
		RETRO_ITEM_FOOD = food;
		RETRO_ITEM_REGISTER = register;
	}

	/** True when RetroAPI's item registry is usable: allocation, texturing, food and registration all resolved. */
	public static boolean hasRetroItemRegistry() {
		return RETRO_ID_FROM != null && RETRO_ALLOCATE_ITEM_ID != null && RETRO_ITEM_TEXTURE != null
			&& RETRO_ITEM_FOOD != null && RETRO_ITEM_REGISTER != null;
	}

	/**
	 * An eat effect for a RetroAPI food item, run after the heal RetroFood itself applies. See
	 * {@link #registerRetroFoodItem}, whose callers pass {@code health = 0} and do the entire heal
	 * in here instead, when the amount depends on something RetroFood does not know (e.g. the size
	 * a fish was caught at).
	 */
	@FunctionalInterface
	public interface RetroOnEaten {
		void onEaten(ItemStack stack, net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player);
	}

	/**
	 * Registers a new item through RetroAPI: a real, permanently reserved id from
	 * {@code RetroItemIds} (not the {@code AUTO_ID} sentinel - that only resolves correctly through a
	 * mixin-woven constructor path this bridge has no compile-time way to trigger, and reserving the
	 * id up front through the same allocator sidesteps needing it at all), a texture at
	 * {@code assets/retrotweaks/textures/item/<texturePath>.png}, and food behaviour.
	 *
	 * <p>Returns {@code null} when {@link #hasRetroItemRegistry()} is false or anything goes wrong -
	 * RetroAPI is not installed (the normal case on a bare loader) or it moved underneath this.
	 */
	public static Item registerRetroFoodItem(String path, String texturePath, int maxCount, int health,
			RetroOnEaten onEaten) {
		if (!hasRetroItemRegistry()) return null;
		try {
			int id = (int) RETRO_ALLOCATE_ITEM_ID.invoke(null);
			// Item(int) is protected - fine for vanilla's own subclasses, but ApiBridge is neither in
			// net.minecraft.item nor a subclass, so this reaches around it the same way every other
			// lookup here reaches around a missing compile-time type: reflectively.
			Item item = ITEM_CONSTRUCTOR.newInstance(id);
			item.setMaxCount(maxCount);
			Object textureId = RETRO_ID_FROM.invoke(null, RetroTweaks.MOD_ID, texturePath);
			RETRO_ITEM_TEXTURE.invoke(item, textureId);
			Object onEatenProxy = java.lang.reflect.Proxy.newProxyInstance(
				ApiBridge.class.getClassLoader(),
				new Class<?>[]{ RETRO_ON_EATEN },
				(java.lang.reflect.InvocationHandler) (proxy, method, args) -> {
					if (onEaten != null && "onEaten".equals(method.getName()) && args != null && args.length == 3) {
						onEaten.onEaten((ItemStack) args[0], (net.minecraft.world.World) args[1],
							(net.minecraft.entity.player.PlayerEntity) args[2]);
					}
					return null;
				});
			RETRO_ITEM_FOOD.invoke(item, health, false, onEatenProxy);
			Object itemId = RETRO_ID_FROM.invoke(null, RetroTweaks.MOD_ID, path);
			RETRO_ITEM_REGISTER.invoke(item, itemId);
			return item;
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("RetroAPI is installed but registering item '{}' failed; it will not "
				+ "appear this session", path, t);
			return null;
		}
	}

	private static Integer invokeInt(MethodHandle handle, ItemStack stack) {
		if (handle == null) return null;
		try {
			return (int) handle.invoke(stack);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Looks up a public static method, or returns null if the class or the method is not there.
	 * A missing class is the normal case (the API is not installed) and is not logged; a class that
	 * is present but missing the method means the API moved, which is worth a line in the log.
	 */
	private static MethodHandle find(String className, String methodName, MethodType type) {
		Class<?> owner;
		try {
			owner = Class.forName(className);
		} catch (ClassNotFoundException | LinkageError e) {
			return null;
		}
		try {
			return MethodHandles.lookup().findStatic(owner, methodName, type);
		} catch (ReflectiveOperationException | LinkageError e) {
			RetroTweaks.LOGGER.warn("{} is installed but {}.{} was not where RetroTweaks expected it; "
				+ "falling back to vanilla for this lookup", className, className, methodName);
			return null;
		}
	}

	// --- RetroAPI achievement pages, used by WhatAreYouScoring's WAYS achievement pages (see
	// feature/scoring/achievement/ScoreAchievements.java). Vanilla b1.7.3 has one flat achievement
	// list and no notion of a page, so this - like the recipe lookups above - only ever does
	// anything when RetroAPI supplies the registry. The achievements it returns are plain vanilla
	// Achievement/Stat objects once built: only the act of registering one needs RetroAPI. ---

	private static final MethodHandle RETRO_NAMESPACED_ID_FROM;
	private static final MethodHandle RETRO_ACHIEVEMENT_REGISTER_BLOCK;
	private static final MethodHandle RETRO_ACHIEVEMENT_REGISTER_ITEM;
	private static final MethodHandle RETRO_ACHIEVEMENT_PAGE_CTOR;
	private static final MethodHandle RETRO_ACHIEVEMENT_PAGE_ADD;
	private static final MethodHandle RETRO_ACHIEVEMENT_PAGE_HIDE;

	static {
		MethodHandle idFrom = null;
		MethodHandle registerBlock = null;
		MethodHandle registerItem = null;
		MethodHandle pageCtor = null;
		MethodHandle pageAdd = null;
		MethodHandle pageHide = null;
		try {
			Class<?> idClass = Class.forName("net.ornithemc.osl.core.api.util.NamespacedIdentifier");
			Class<?> idsClass = Class.forName("net.ornithemc.osl.core.api.util.NamespacedIdentifiers");
			Class<?> registerClass = Class.forName("com.periut.retroapi.achievement.RetroAchievements");
			Class<?> pageClass = Class.forName("com.periut.retroapi.achievement.AchievementPage");

			idFrom = MethodHandles.lookup().findStatic(idsClass, "from",
				MethodType.methodType(idClass, String.class, String.class));
			registerBlock = MethodHandles.lookup().findStatic(registerClass, "register", MethodType.methodType(
				Achievement.class, idClass, String.class, int.class, int.class, Block.class, Achievement.class));
			registerItem = MethodHandles.lookup().findStatic(registerClass, "register", MethodType.methodType(
				Achievement.class, idClass, String.class, int.class, int.class, Item.class, Achievement.class));
			pageCtor = MethodHandles.lookup().findConstructor(pageClass, MethodType.methodType(void.class, idClass));
			pageHide = MethodHandles.lookup().findVirtual(pageClass, "setHidden",
				MethodType.methodType(pageClass, boolean.class))
				.asType(MethodType.methodType(void.class, Object.class, boolean.class));
			pageAdd = MethodHandles.lookup().findVirtual(pageClass, "addAchievements",
				MethodType.methodType(void.class, Achievement[].class));
		} catch (ClassNotFoundException | LinkageError e) {
			// RetroAPI is not installed; the achievement pages simply do not exist.
		} catch (ReflectiveOperationException e) {
			RetroTweaks.LOGGER.warn("RetroAPI is installed but its achievement API was not where RetroTweaks "
				+ "expected it; the WAYS achievement pages will not be registered", e);
		}
		RETRO_NAMESPACED_ID_FROM = idFrom;
		RETRO_ACHIEVEMENT_REGISTER_BLOCK = registerBlock;
		RETRO_ACHIEVEMENT_REGISTER_ITEM = registerItem;
		RETRO_ACHIEVEMENT_PAGE_CTOR = pageCtor;
		RETRO_ACHIEVEMENT_PAGE_ADD = pageAdd;
		RETRO_ACHIEVEMENT_PAGE_HIDE = pageHide;
	}

	/** True when RetroAPI's achievement registry is available to register pages with. */
	public static boolean hasAchievementApi() {
		return RETRO_ACHIEVEMENT_PAGE_CTOR != null;
	}

	/**
	 * Runs {@code registration} at the moment RetroAPI has vanilla's block/item/stat static-init
	 * cycle ready and is asking mods to add achievements - a no-op when RetroAPI is absent, so a
	 * caller that only ever builds achievements from inside this callback never runs without the
	 * registry it needs.
	 */
	public static void onAchievementsRegister(Runnable registration) {
		if (!Mods.HAS_RETROAPI) return;
		// Called straight rather than through a method handle. The rest of this class reaches RetroAPI
		// reflectively because it was once an optional dependency; the two ship as one mod now, and for
		// this one - where the difference between register() and the underlying Event.register() decides
		// whether a late listener runs at all under StationAPI - naming the method in source is worth
		// more than the symmetry.
		com.periut.retroapi.achievement.event.AchievementRegistrationCallback.register(registration);
	}

	/** Runs {@code action} just before every achievements screen is built; a no-op without RetroAPI. */
	public static void onAchievementPagesPrepared(Runnable action) {
		if (!Mods.HAS_RETROAPI) return;
		com.periut.retroapi.achievement.AchievementPage.onBeforeScreen(action);
	}

	/** A new RetroAPI achievement page, or {@code null} when RetroAPI is absent. */
	public static Object newAchievementPage(String namespace, String path) {
		if (RETRO_ACHIEVEMENT_PAGE_CTOR == null) return null;
		try {
			Object id = namespacedId(namespace, path);
			return id == null ? null : RETRO_ACHIEVEMENT_PAGE_CTOR.invoke(id);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("Could not create a RetroAPI achievement page for '{}'", path, t);
			return null;
		}
	}

	/**
	 * Hides or shows a page from {@link #newAchievementPage}.
	 *
	 * <p>A hidden page keeps everything it had - its achievements are registered, its progress is
	 * tracked, its ids still agree with the server - it is simply skipped by the achievements screen's
	 * own navigation, and reached through the game menu's WAYS button instead. Settable after the fact
	 * because this follows a config toggle, not a launch flag.
	 */
	public static void setAchievementPageHidden(Object page, boolean hidden) {
		if (page == null || RETRO_ACHIEVEMENT_PAGE_HIDE == null) return;
		try {
			RETRO_ACHIEVEMENT_PAGE_HIDE.invoke(page, hidden);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("Could not change an achievement page's visibility", t);
		}
	}

	/** Adds achievements to a page from {@link #newAchievementPage}; a no-op if either is null. */
	public static void addAchievementsToPage(Object page, Achievement... achievements) {
		if (page == null || RETRO_ACHIEVEMENT_PAGE_ADD == null) return;
		try {
			RETRO_ACHIEVEMENT_PAGE_ADD.invoke(page, achievements);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("Could not add achievements to a RetroAPI achievement page", t);
		}
	}

	/**
	 * Registers a block-icon achievement through RetroAPI (auto-allocated id), or returns
	 * {@code null} when RetroAPI is absent or the registration itself failed.
	 */
	public static Achievement registerAchievement(String namespace, String path, String name,
			int column, int row, Block icon, Achievement parent) {
		if (RETRO_ACHIEVEMENT_REGISTER_BLOCK == null) return null;
		try {
			Object id = namespacedId(namespace, path);
			return id == null ? null
				: (Achievement) RETRO_ACHIEVEMENT_REGISTER_BLOCK.invoke(id, name, column, row, icon, parent);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("Could not register achievement '{}' with RetroAPI", path, t);
			return null;
		}
	}

	/** Same as above, for an item-icon achievement. */
	public static Achievement registerAchievement(String namespace, String path, String name,
			int column, int row, Item icon, Achievement parent) {
		if (RETRO_ACHIEVEMENT_REGISTER_ITEM == null) return null;
		try {
			Object id = namespacedId(namespace, path);
			return id == null ? null
				: (Achievement) RETRO_ACHIEVEMENT_REGISTER_ITEM.invoke(id, name, column, row, icon, parent);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("Could not register achievement '{}' with RetroAPI", path, t);
			return null;
		}
	}

	private static Object namespacedId(String namespace, String path) {
		if (RETRO_NAMESPACED_ID_FROM == null) return null;
		try {
			return RETRO_NAMESPACED_ID_FROM.invoke(namespace, path);
		} catch (Throwable t) {
			return null;
		}
	}

	// --- RetroDragon runtime settings (RetroDragon 0.1.5+), used by the video options screen to
	// split the vanilla "Advanced OpenGL" button into independent RGSS AA and Mipmaps toggles when
	// RetroDragon is installed. See mixin/client/gui/VideoOptionsScreenRetroDragonMixin. Older
	// RetroDragon builds only read RGSS/mipmap from launch-time system properties and have no
	// RetroSettings class at all, which hasRetroDragonSettings() below tells apart from "not
	// installed" the same way every other lookup in this file does: a null handle either way, no
	// hard dependency needed to know the difference. ---

	private static final MethodHandle RETRODRAGON_IS_RGSS = find(
		"com.periut.retrodragon.api.RetroSettings", "isRgss", MethodType.methodType(boolean.class));
	private static final MethodHandle RETRODRAGON_SET_RGSS = find(
		"com.periut.retrodragon.api.RetroSettings", "setRgss", MethodType.methodType(void.class, boolean.class));
	private static final MethodHandle RETRODRAGON_IS_MIPMAP = find(
		"com.periut.retrodragon.api.RetroSettings", "isMipmap", MethodType.methodType(boolean.class));
	private static final MethodHandle RETRODRAGON_SET_MIPMAP = find(
		"com.periut.retrodragon.api.RetroSettings", "setMipmap", MethodType.methodType(void.class, boolean.class));
	private static final MethodHandle RETRODRAGON_GET_FRAME_LIMIT = find(
		"com.periut.retrodragon.api.RetroSettings", "getFrameLimit", MethodType.methodType(int.class));
	private static final MethodHandle RETRODRAGON_SET_FRAME_LIMIT = find(
		"com.periut.retrodragon.api.RetroSettings", "setFrameLimit", MethodType.methodType(void.class, int.class));

	/**
	 * True when RetroDragon's numeric frame limit (0.1.6+) is installed and resolved.
	 *
	 * <p>Separate from {@link #hasRetroDragonSettings()} on purpose: RGSS and mipmaps arrived in
	 * 0.1.5 and the frame limit in 0.1.6, so a 0.1.5 install answers true to one and false to this.
	 */
	public static boolean hasFrameLimit() {
		return RETRODRAGON_GET_FRAME_LIMIT != null && RETRODRAGON_SET_FRAME_LIMIT != null;
	}

	/** RetroDragon's current frame cap in fps, or 0 for none. 0 when RetroDragon is absent. */
	public static int getFrameLimit() {
		if (RETRODRAGON_GET_FRAME_LIMIT == null) return 0;
		try {
			return (int) RETRODRAGON_GET_FRAME_LIMIT.invoke();
		} catch (Throwable t) {
			return 0;
		}
	}

	/**
	 * Sets RetroDragon's frame cap. {@code fps <= 0} clears it. A no-op when RetroDragon is absent,
	 * where {@code Display.sync} does the pacing instead.
	 */
	public static void setFrameLimit(int fps) {
		if (RETRODRAGON_SET_FRAME_LIMIT == null) return;
		try {
			RETRODRAGON_SET_FRAME_LIMIT.invoke(fps);
		} catch (Throwable t) {
			// A RetroDragon that moved the method is the same as one that is absent: pace with what
			// vanilla gives us instead of taking the game down over a frame cap.
		}
	}

	/**
	 * True when RetroDragon's live RGSS/Mipmap toggles are installed and resolved. False both when
	 * RetroDragon is absent and when an older RetroDragon without this API is present - either way
	 * there is nothing to call, so the caller should leave the vanilla Advanced OpenGL button alone.
	 */
	public static boolean hasRetroDragonSettings() {
		return RETRODRAGON_IS_RGSS != null && RETRODRAGON_SET_RGSS != null
			&& RETRODRAGON_IS_MIPMAP != null && RETRODRAGON_SET_MIPMAP != null;
	}

	/** Whether RetroDragon's terrain shader currently applies RGSS. False (harmlessly) when absent. */
	public static boolean isRgss() {
		if (RETRODRAGON_IS_RGSS == null) return false;
		try {
			return (boolean) RETRODRAGON_IS_RGSS.invoke();
		} catch (Throwable t) {
			return false;
		}
	}

	/** Sets RetroDragon's RGSS toggle. Instant on both of its backends. A no-op when absent. */
	public static void setRgss(boolean enabled) {
		if (RETRODRAGON_SET_RGSS == null) return;
		try {
			RETRODRAGON_SET_RGSS.invoke(enabled);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("RetroDragon is installed but RetroSettings.setRgss failed", t);
		}
	}

	/** Whether RetroDragon currently samples the terrain mip chain. False (harmlessly) when absent. */
	public static boolean isMipmap() {
		if (RETRODRAGON_IS_MIPMAP == null) return false;
		try {
			return (boolean) RETRODRAGON_IS_MIPMAP.invoke();
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Sets RetroDragon's mipmap toggle. A no-op when absent. Turning it OFF is free; turning it ON
	 * rebuilds and re-uploads the block atlas mip chain via {@code TextureManager.reload()} - on the
	 * render thread (which a video-options click always is) that runs synchronously before this
	 * returns, so do not call this in a loop or per frame. See RetroSettings' own javadoc for the
	 * full render-thread/deferred split.
	 */
	public static void setMipmap(boolean enabled) {
		if (RETRODRAGON_SET_MIPMAP == null) return;
		try {
			RETRODRAGON_SET_MIPMAP.invoke(enabled);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("RetroDragon is installed but RetroSettings.setMipmap failed", t);
		}
	}

	// --- RetroDragon render scale (RetroSettings, 0.1.7+), used by ConfigScreen's Graphics tab for
	// the render scale row and the resample-filter cycle button. Older RetroDragon (0.1.5/0.1.6, which
	// already has RGSS/Mipmaps above but not this) resolves every handle below to null exactly like
	// RetroDragon being absent entirely does - find() cannot and does not need to tell "too old" apart
	// from "not installed" for the caller, both mean "there is nothing to call". ---

	private static final MethodHandle RETRODRAGON_GET_RENDER_SCALE = find(
		"com.periut.retrodragon.api.RetroSettings", "getRenderScale", MethodType.methodType(float.class));
	private static final MethodHandle RETRODRAGON_SET_RENDER_SCALE = find(
		"com.periut.retrodragon.api.RetroSettings", "setRenderScale", MethodType.methodType(float.class, float.class));
	private static final MethodHandle RETRODRAGON_GET_SCALE_FILTER = find(
		"com.periut.retrodragon.api.RetroSettings", "getScaleFilter", MethodType.methodType(String.class));
	private static final MethodHandle RETRODRAGON_SET_SCALE_FILTER = find(
		"com.periut.retrodragon.api.RetroSettings", "setScaleFilter", MethodType.methodType(String.class, String.class));
	private static final MethodHandle RETRODRAGON_GET_AVAILABLE_SCALE_FILTERS = find(
		"com.periut.retrodragon.api.RetroSettings", "getAvailableScaleFilters", MethodType.methodType(String[].class, boolean.class));

	/**
	 * True when RetroDragon's live render-scale API (0.1.7+) is installed and resolved. False both
	 * when RetroDragon is absent and when an older RetroDragon without this API is present - either
	 * way there is nothing to call, so the caller (the Graphics tab) should not draw the render scale
	 * row, the filter cycle button, or anything else that depends on this.
	 */
	public static boolean hasRenderScale() {
		return RETRODRAGON_GET_RENDER_SCALE != null && RETRODRAGON_SET_RENDER_SCALE != null
			&& RETRODRAGON_GET_SCALE_FILTER != null && RETRODRAGON_SET_SCALE_FILTER != null
			&& RETRODRAGON_GET_AVAILABLE_SCALE_FILTERS != null;
	}

	/**
	 * The world's current render resolution as a multiple of the window's logical size - already
	 * seeded by RetroDragon from the window's pixels-per-point (1.0 normally, 2.0 on an untouched
	 * Retina/HiDPI window), so a caller should read this rather than assume 1.0. Returns 1.0
	 * (harmlessly) when absent; callers must still gate on {@link #hasRenderScale()} before showing
	 * anything driven by this, same as every other lookup in this file.
	 */
	public static float getRenderScale() {
		if (RETRODRAGON_GET_RENDER_SCALE == null) return 1.0F;
		try {
			return (float) RETRODRAGON_GET_RENDER_SCALE.invoke();
		} catch (Throwable t) {
			return 1.0F;
		}
	}

	/**
	 * Requests a new render scale. RetroDragon clamps to {@code [0.1, 4.0]} and returns the value it
	 * actually adopted - the caller MUST use that return value to update its own display rather than
	 * echoing back what it asked for, or the readout will lie at either end of the range. Returns the
	 * requested value unchanged (not clamped - there is nothing to clamp against) when absent.
	 */
	public static float setRenderScale(float scale) {
		if (RETRODRAGON_SET_RENDER_SCALE == null) return scale;
		try {
			return (float) RETRODRAGON_SET_RENDER_SCALE.invoke(scale);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("RetroDragon is installed but RetroSettings.setRenderScale failed", t);
			return scale;
		}
	}

	/** The resample filter currently resolving the world target back to the window. "" when absent. */
	public static String getScaleFilter() {
		if (RETRODRAGON_GET_SCALE_FILTER == null) return "";
		try {
			return (String) RETRODRAGON_GET_SCALE_FILTER.invoke();
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * Sets the resample filter by name. RetroDragon logs and falls back on an unknown or currently
	 * unavailable name rather than throwing, and returns whatever filter is actually in force
	 * afterwards - same "trust the return value" contract as {@link #setRenderScale}. Returns the
	 * requested name unchanged when absent.
	 */
	public static String setScaleFilter(String name) {
		if (RETRODRAGON_SET_SCALE_FILTER == null) return name;
		try {
			return (String) RETRODRAGON_SET_SCALE_FILTER.invoke(name);
		} catch (Throwable t) {
			RetroTweaks.LOGGER.warn("RetroDragon is installed but RetroSettings.setScaleFilter failed", t);
			return name;
		}
	}

	/**
	 * The filters usable right now for the given direction ({@code upscale = true} when the world
	 * target is smaller than the window, i.e. render scale below 1.0; {@code false} for the
	 * supersampling direction at or above 1.0). Availability is not fixed - macOS/WebGPU-only filters
	 * and upsamplers offered only below 1.0 both come and go - so this must be re-asked every time it
	 * is needed (e.g. every time the scale crosses 1.0) rather than cached. Never null; empty when
	 * absent or when this run genuinely has nothing to offer in that direction.
	 */
	public static String[] getAvailableScaleFilters(boolean upscale) {
		if (RETRODRAGON_GET_AVAILABLE_SCALE_FILTERS == null) return new String[0];
		try {
			String[] filters = (String[]) RETRODRAGON_GET_AVAILABLE_SCALE_FILTERS.invoke(upscale);
			return filters != null ? filters : new String[0];
		} catch (Throwable t) {
			return new String[0];
		}
	}
}
