package com.periut.retroapi.client.screen;

import com.periut.retroapi.client.gui.RetroTextField;
import com.periut.retroapi.client.gui.RetroKeys;
import com.periut.retroapi.gamemode.screen.CreativeScreenHandler;
import com.periut.retroapi.gamerule.RetroGameRules;
import com.periut.retroapi.itemgroup.RetroItemGroup;
import com.periut.retroapi.itemgroup.RetroItemGroups;
import com.periut.retroapi.itemgroup.VanillaItemGroups;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The creative inventory, ported from modern's {@code CreativeModeInventoryScreen}.
 *
 * <p>It is a real {@link HandledScreen} over a real {@link CreativeScreenHandler}, so everything a
 * container screen does in beta, it does here: vanilla draws the slots and the items in them (which
 * is what puts them at the right offset), vanilla carries the stack on the cursor, vanilla draws the
 * item tooltips, and the row along the bottom is the player's actual hotbar rather than a picture of
 * one. This class only adds what modern adds on top - the tab strip, the scrollbar, the search box
 * and the page arrows.
 *
 * <p>Measurements are modern's: a 195x136 panel, picker slots from {@code (9, 18)} on an 18-pixel
 * pitch, hotbar at {@code y=112}, destroy slot at {@code (173, 112)}, search field at {@code (82, 6)}
 * eighty wide, a 12x15 scroller at {@code x=175} riding {@code y=18..130}, and 26x32 tabs spaced 27
 * apart with the top row 28 above the panel and the bottom row four inside its lower edge.
 *
 * <p>Paging is Fabric API's: ten tabs a page, the search tab pinned to every page, 10x12 arrows at
 * {@code (171, 4)} and {@code (181, 4)}, Page Up and Page Down as shortcuts.
 */
public class RetroCreativeScreen extends HandledScreen {

    /**
     * Modern's own sprites, repacked into 256x256 sheets because beta's {@code drawTexture} maps
     * every texture as 256x256 while modern ships one file per sprite. Nothing is redrawn.
     */
    private static final String PANEL_TEXTURE = "/assets/retroapi/gui/creative_panel.png";
    private static final String SEARCH_PANEL_TEXTURE = "/assets/retroapi/gui/creative_panel_search.png";
    private static final String INVENTORY_PANEL_TEXTURE = "/assets/retroapi/gui/creative_panel_inventory.png";
    private static final String TABS_TEXTURE = "/assets/retroapi/gui/creative_tabs.png";
    private static final String WIDGETS_TEXTURE = "/assets/retroapi/gui/creative_widgets.png";
    /** Fabric API's own {@code fabric:textures/gui/creative_buttons.png}, unchanged. */
    private static final String BUTTONS_TEXTURE = "/assets/retroapi/gui/creative_buttons.png";

    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 9;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_SPACING = 27;
    private static final int TAB_SHEET_PITCH_X = 28;
    private static final int TAB_SHEET_PITCH_Y = 36;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    /** Fabric API adds its buttons at {@code leftPos + 171} and the ten pixels after it. */
    private static final int BUTTON_WIDTH = 10;
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTON_PREVIOUS_X = 171;
    private static final int BUTTON_NEXT_X = BUTTON_PREVIOUS_X + BUTTON_WIDTH;
    private static final int BUTTON_Y = 4;
    /** LWJGL's key code for {@code 1}; the hotbar keys run from it. */
    private static final int KEY_1 = 2;
    /**
     * Fabric API's page size, and the reason it is ten: modern's own layout keeps the top-right slot
     * for the hotbar tab and the two bottom-right ones for operator and inventory, leaving columns
     * 0..4 of each row.
     */
    private static final int TABS_PER_PAGE = 10;
    private static final int TABS_PER_ROW = 5;
    /**
     * The slot modern reserves for its hotbar tab (top row, column 5). RetroAPI has no hotbar tab, so
     * it is an empty space in the strip - and on the FIRST page it stays empty, because that is where
     * the eye expects vanilla's own layout. Later pages are all RetroAPI's, so a tab may sit there.
     */
    private static final int GAP_COLUMN = 5;
    private static final int TABS_PER_LATER_PAGE = TABS_PER_PAGE + 1;
    /**
     * The first page is modern's own ten: five along the top and five along the bottom, ending with
     * Spawn Eggs at bottom-4. The gap at top-5 is modern's hotbar tab, which RetroAPI has no
     * equivalent of, so it stays empty here - but only on this page, where the eye expects vanilla's
     * layout. Later pages are all RetroAPI's and may use it.
     */
    private static final int TABS_PER_FIRST_PAGE = TABS_PER_PAGE;
    /** Modern's search box: {@code (82,6)}, 80 wide, 9 tall. */
    private static final int SEARCH_X = 82;
    private static final int SEARCH_Y = 6;
    private static final int SEARCH_WIDTH = 80;
    private static final int SEARCH_HEIGHT = 9;
    /** Modern's inventory-tab player render: box {@code (73,6)-(105,49)}, size 20. See {@link #drawPlayer()}. */
    private static final float PLAYER_SCALE = 20.0F;
    private static final int PLAYER_X = 89;
    private static final int PLAYER_Y = 47;
    private static final int PLAYER_HEAD_OFFSET = 33;

    /** Kept between openings, exactly as modern keeps {@code selectedTab}. */
    private static int selectedGroup;
    private static int currentPage;

    private final CreativeScreenHandler creativeHandler;
    private final ItemRenderer tabIconRenderer = new ItemRenderer();

    private List<RetroItemGroup> groups = List.of();
    private List<ItemStack> visible = new ArrayList<>();
    /**
     * Modern's search box is an {@code EditBox}, and RetroAPI has that widget backported as
     * {@link RetroTextField} - selection, click-to-place and drag-to-select, {@code Ctrl+A/C/X/V},
     * word-wise movement, {@code Home}/{@code End}, and a window that scrolls with the cursor so a
     * long query is clipped to the box instead of running out of it.
     */
    private RetroTextField searchField;
    private boolean selecting;
    /** Ticked so a key that arrives twice in one tick swaps once. See {@code keyPressed}. */
    private int ticks;
    private int lastSwapTick = -1;
    private int lastSwapDigit = -1;
    private float scrollOffs;
    private boolean scrolling;

    public RetroCreativeScreen(final PlayerEntity player) {
        super(new CreativeScreenHandler(player));
        this.creativeHandler = (CreativeScreenHandler) handler;
        this.backgroundWidth = 195;
        this.backgroundHeight = 136;
    }

    @Override
    public void init() {
        super.init();
        // Modern's own EditBox: 80 wide at (82,6), fifty characters, white rather than the widget's
        // usual grey.
        searchField = new RetroTextField(textRenderer, SEARCH_WIDTH);
        searchField.setMaxLength(50);
        searchField.setTextColor(0xFFFFFF);
        groups = visibleGroups();
        if (selectedGroup >= groups.size()) {
            selectedGroup = 0;
        }
        currentPage = pageOf(selectedGroup);
        refresh();
    }

    /**
     * Every tab, less the ones this world hides.
     *
     * <p>Only Operator Utilities is hidden, and only when {@code operatorItemsTab} is off - the rule
     * standing in for modern's "Operator Items Tab" option, which is likewise off until asked for.
     */
    private static List<RetroItemGroup> visibleGroups() {
        final List<RetroItemGroup> all = RetroItemGroups.all();
        if (RetroGameRules.getBoolean(RetroGameRules.OPERATOR_ITEMS_TAB)) {
            return all;
        }

        final List<RetroItemGroup> shown = new ArrayList<>(all.size());
        for (final RetroItemGroup group : all) {
            if (group != VanillaItemGroups.OPERATOR) {
                shown.add(group);
            }
        }
        return shown;
    }

    private int left() {
        return (width - backgroundWidth) / 2;
    }

    private int top() {
        return (height - backgroundHeight) / 2;
    }

    // --- tabs, pages and contents -------------------------------------------------------------------

    /**
     * The tabs modern nails to fixed corners, which are also Fabric's "common" tabs: they sit on
     * every page rather than taking one of the ten paginated slots.
     *
     * <p>Modern's own coordinates: hotbar top-5, search top-6, operator bottom-5, inventory bottom-6,
     * all right-aligned. That is exactly why ten slots paginate - top 0..4 and bottom 0..4.
     *
     * @return {@code {row, column}} with row 0 for the top, or null when the tab paginates
     */
    private int[] pinnedPosition(final int groupIndex) {
        final RetroItemGroup group = groups.get(groupIndex);
        if (group == VanillaItemGroups.SEARCH) {
            return new int[]{0, 6};
        }
        if (group == VanillaItemGroups.OPERATOR) {
            return new int[]{1, 5};
        }
        if (group == VanillaItemGroups.INVENTORY) {
            return new int[]{1, 6};
        }
        return null;
    }

    private boolean isCommon(final int groupIndex) {
        return pinnedPosition(groupIndex) != null;
    }

    private int paginatedIndexOf(final int groupIndex) {
        int position = 0;
        for (int index = 0; index < groupIndex; index++) {
            if (!isCommon(index)) {
                position++;
            }
        }
        return position;
    }

    /**
     * How many tabs a page holds: nine on the first - the hotbar gap at top-5 and the spawn-egg slot
     * at bottom-4 are both held empty there - and eleven after it, where only the strip's shape
     * matters. See {@link #GAP_COLUMN} and {@link #TABS_PER_FIRST_PAGE}.
     */
    private static int capacityOf(final int page) {
        return page == 0 ? TABS_PER_FIRST_PAGE : TABS_PER_LATER_PAGE;
    }

    /** The page a paginated tab lands on, walking pages because the first one is smaller. */
    private int pageOf(final int groupIndex) {
        if (groupIndex >= groups.size() || isCommon(groupIndex)) {
            return currentPage;
        }
        int remaining = paginatedIndexOf(groupIndex);
        int page = 0;
        while (remaining >= capacityOf(page)) {
            remaining -= capacityOf(page);
            page++;
        }
        return page;
    }

    /** Its position within that page, or -1 when the page being shown is not its page. */
    private int slotOf(final int groupIndex) {
        if (isCommon(groupIndex)) {
            return -1;
        }
        int remaining = paginatedIndexOf(groupIndex);
        int page = 0;
        while (remaining >= capacityOf(page)) {
            remaining -= capacityOf(page);
            page++;
        }
        return page == currentPage ? remaining : -1;
    }

    private int pageCount() {
        int paginated = 0;
        for (int index = 0; index < groups.size(); index++) {
            if (!isCommon(index)) {
                paginated++;
            }
        }

        int pages = 1;
        int remaining = paginated;
        while (remaining > capacityOf(pages - 1)) {
            remaining -= capacityOf(pages - 1);
            pages++;
        }
        return pages;
    }

    /**
     * Where a page slot sits: {@code {row, column}}, row 0 for the top.
     *
     * <p>The first five are the top row, the last five the bottom row, and on a page that has one, the
     * eleventh is the gap at top-5.
     */
    private int[] slotPosition(final int slot) {
        if (currentPage > 0 && slot == TABS_PER_PAGE) {
            return new int[]{0, GAP_COLUMN};
        }
        return slot < TABS_PER_ROW
            ? new int[]{0, slot}
            : new int[]{1, slot - TABS_PER_ROW};
    }

    private boolean isTabVisible(final int groupIndex) {
        return isCommon(groupIndex) || slotOf(groupIndex) >= 0;
    }

    private boolean isSearchTab() {
        return !groups.isEmpty() && groups.get(selectedGroup) == VanillaItemGroups.SEARCH;
    }

    private boolean isInventoryTab() {
        return !groups.isEmpty() && groups.get(selectedGroup) == VanillaItemGroups.INVENTORY;
    }

    private void switchToPage(final int page) {
        final int clamped = Math.max(0, Math.min(pageCount() - 1, page));
        if (clamped == currentPage) {
            return;
        }
        currentPage = clamped;

        if (!isTabVisible(selectedGroup)) {
            for (int index = 0; index < groups.size(); index++) {
                if (isTabVisible(index) && !isCommon(index)) {
                    selectedGroup = index;
                    break;
                }
            }
            refresh();
        }
    }

    /** Rebuilds the list behind the grid, then hands the visible page to the container. */
    private void refresh() {
        // Modern swaps the menu's whole slot list when the inventory tab is picked; so does this.
        if (isInventoryTab() != creativeHandler.isInventoryTab()) {
            if (isInventoryTab()) {
                creativeHandler.showInventory();
            } else {
                creativeHandler.showPicker();
            }
        }
        if (isInventoryTab()) {
            visible = List.of();
            scrollOffs = 0.0F;
            return;
        }

        final List<ItemStack> all = groups.isEmpty() ? List.of() : groups.get(selectedGroup).collect();

        visible = new ArrayList<>();
        final String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        for (final ItemStack stack : all) {
            if (query.isEmpty() || displayName(stack).toLowerCase(Locale.ROOT).contains(query)) {
                visible.add(stack);
            }
        }
        scrollOffs = 0.0F;
        updateSlots();
    }

    private void updateSlots() {
        if (!creativeHandler.isInventoryTab()) {
            creativeHandler.setContents(visible, scrollRow() * NUM_COLS);
        }
    }

    /** Never null: beta returns no translation key for more items than you would think. */
    private static String displayName(final ItemStack stack) {
        if (stack == null) {
            return "";
        }
        try {
            final String key = stack.getTranslationKey();
            if (key == null) {
                return String.valueOf(stack.itemId);
            }
            final String name = I18n.getTranslation(key + ".name");
            return name == null || name.equals(key + ".name") ? key : name;
        } catch (final RuntimeException | LinkageError ignored) {
            return String.valueOf(stack.itemId);
        }
    }

    private int offscreenRows() {
        return Math.max(0, (visible.size() + NUM_COLS - 1) / NUM_COLS - NUM_ROWS);
    }

    private boolean canScroll() {
        return offscreenRows() > 0;
    }

    private int scrollRow() {
        return Math.round(scrollOffs * offscreenRows());
    }

    // --- geometry, all of it modern's ---------------------------------------------------------------

    private int tabX(final int groupIndex) {
        // Modern's right-aligned formula, so a pinned tab sits hard against the panel's edge; the
        // paginated ones are a plain 27 apart from the left.
        final int[] position = positionOf(groupIndex);
        return isCommon(groupIndex)
            ? backgroundWidth - TAB_SPACING * (7 - position[1]) + 1
            : TAB_SPACING * position[1];
    }

    private boolean isTopRow(final int groupIndex) {
        return positionOf(groupIndex)[0] == 0;
    }

    /** Which of modern's seven column sprites a tab uses, so neighbouring tabs join up. */
    private int tabColumn(final int groupIndex) {
        return positionOf(groupIndex)[1];
    }

    private int[] positionOf(final int groupIndex) {
        final int[] pinned = pinnedPosition(groupIndex);
        return pinned != null ? pinned : slotPosition(slotOf(groupIndex));
    }

    private int tabScreenY(final int groupIndex) {
        return isTopRow(groupIndex) ? top() - 28 : top() + backgroundHeight - 4;
    }

    // --- drawing ------------------------------------------------------------------------------------

    /**
     * Binds a sheet, and survives one that is not there.
     *
     * <p>Beta's own miss path is a crash: a texture it cannot find falls back to a placeholder image
     * that is itself null in this build, so {@code TextureManager.load} throws NPE and takes the game
     * with it. A creative screen missing one of its panels should draw wrong, not close the game, so
     * a failed bind is logged once and the panel is skipped.
     */
    private void bind(final String texture) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(texture));
        } catch (final RuntimeException | LinkageError failed) {
            if (missingTextures.add(texture)) {
                com.periut.retroapi.RetroAPI.LOGGER.error("Creative screen texture {} could not be loaded", texture, failed);
            }
        }
    }

    /** Reported once each, because this runs every frame. */
    private static final java.util.Set<String> missingTextures = new java.util.HashSet<>();

    /**
     * Everything behind the slots. Vanilla's {@code HandledScreen} draws the slots, their items, the
     * cursor stack and the tooltips on top of this, which is why none of that is here.
     */
    @Override
    protected void drawBackground(final float delta) {
        final int left = left();
        final int top = top();

        // Unselected tabs first, then the panel over them, then the selected tab: the order that
        // makes the selected tab look joined to the panel.
        for (int index = 0; index < groups.size(); index++) {
            if (index != selectedGroup && isTabVisible(index)) {
                drawTab(index);
            }
        }

        bind(isInventoryTab() ? INVENTORY_PANEL_TEXTURE
            : isSearchTab() ? SEARCH_PANEL_TEXTURE : PANEL_TEXTURE);
        drawTexture(left, top, 0, 0, backgroundWidth, backgroundHeight);

        if (isTabVisible(selectedGroup)) {
            drawTab(selectedGroup);
        }

        if (!isInventoryTab()) {
            drawScroller();
        }
        pageTooltip = null;
        if (pageCount() > 1) {
            drawPageButtons(lastMouseX, lastMouseY);
        }
        if (isInventoryTab()) {
            drawPlayer();
        }
    }

    /**
     * The player in the inventory tab's black box, which is beta's own {@code InventoryScreen} block
     * with modern's creative measurements: modern renders into {@code (73,6)-(105,49)} at size 20
     * where survival's box is {@code (26,8)-(75,78)} at 30, and this panel's box is those same pixels.
     *
     * <p>Beta anchors the render three pixels above the box's lower edge rather than at its centre, so
     * the anchor here is the same three pixels scaled with the render: {@code 49 - 3 * 20 / 30}. Every
     * offset in the block is in render units, so scaling the anchor with {@link #PLAYER_SCALE} keeps
     * the figure sitting in the small box exactly as the big one sits in survival's - the mouse-follow
     * head offset ({@code 50} at 30) included.
     */
    private void drawPlayer() {
        final PlayerEntity player = minecraft.player;
        if (player == null) {
            return;
        }

        final int x = left() + PLAYER_X;
        final int y = top() + PLAYER_Y;

        // Beta's InventoryScreen never touches the depth test because it is already on: nothing in a
        // vanilla container screen turns it off. This screen does - drawTabIcon leaves it off - and the
        // entity renderer disables face culling and leans on the depth buffer instead, so without this
        // the player's back faces draw over its front ones. Left on afterwards, which is the state
        // HandledScreen draws its slots in.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 50.0F);
        GL11.glScalef(-PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);

        final float bodyYaw = player.bodyYaw;
        final float yaw = player.yaw;
        final float pitch = player.pitch;
        final float towardsX = x - lastMouseX;
        final float towardsY = y - PLAYER_HEAD_OFFSET - lastMouseY;

        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        Lighting.turnOn();
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-((float) Math.atan(towardsY / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);

        player.bodyYaw = (float) Math.atan(towardsX / 40.0F) * 20.0F;
        player.yaw = (float) Math.atan(towardsX / 40.0F) * 40.0F;
        player.pitch = -((float) Math.atan(towardsY / 40.0F)) * 20.0F;
        player.minBrightness = 1.0F;

        GL11.glTranslatef(0.0F, player.standingEyeHeight, 0.0F);
        EntityRenderDispatcher.INSTANCE.yaw = 180.0F;
        EntityRenderDispatcher.INSTANCE.render(player, 0.0, 0.0, 0.0, 0.0F, 1.0F);

        player.minBrightness = 0.0F;
        player.bodyYaw = bodyYaw;
        player.yaw = yaw;
        player.pitch = pitch;

        GL11.glPopMatrix();
        Lighting.turnOff();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }

    private void drawTab(final int groupIndex) {
        final boolean top = isTopRow(groupIndex);
        final boolean selected = groupIndex == selectedGroup;
        final int x = left() + tabX(groupIndex);
        final int y = tabScreenY(groupIndex);

        // Modern has a sprite per column so neighbouring tabs join up; the repacked sheet keeps that,
        // column across and tab state down.
        final int column = tabColumn(groupIndex);
        final int variant = (top ? 0 : 2) + (selected ? 1 : 0);

        bind(TABS_TEXTURE);
        drawTexture(x, y, column * TAB_SHEET_PITCH_X, variant * TAB_SHEET_PITCH_Y, TAB_WIDTH, TAB_HEIGHT);

        // Modern nudges the icon a pixel towards the panel so it centres on the visible part.
        drawTabIcon(groups.get(groupIndex).getIcon(), x + 13 - 8, y + 16 - 8 + (top ? 1 : -1));
    }

    /** The tab icons are not slots, so they are the one place this screen draws an item itself. */
    private void drawTabIcon(final ItemStack stack, final int x, final int y) {
        if (stack == null) {
            return;
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        tabIconRenderer.renderGuiItem(textRenderer, minecraft.textureManager, stack, x, y);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void drawScroller() {
        final int x = left() + 175;
        final int top = top() + 18;
        final int bottom = top + 112;

        bind(WIDGETS_TEXTURE);
        drawTexture(x, top + (int) ((bottom - top - 17) * scrollOffs),
            canScroll() ? 0 : 16, 0, SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    /**
     * Fabric API's page arrows, its {@code CreativeModeTabButton} drawn by hand because beta's
     * {@code ButtonWidget} has none of its states: 10x12 at {@code (171, 4)} and {@code (181, 4)},
     * present only while there is more than one page, greyed out at the ends rather than hidden, and
     * captioned "Page n/m" whenever the pointer is over one - greyed or not, which is what Fabric does
     * by testing {@code isHovered()} outside the {@code active} check.
     */
    private void drawPageButtons(final int mouseX, final int mouseY) {
        drawPageButton(left() + BUTTON_PREVIOUS_X, top() + BUTTON_Y, false, currentPage > 0, mouseX, mouseY);
        drawPageButton(left() + BUTTON_NEXT_X, top() + BUTTON_Y, true, currentPage + 1 < pageCount(), mouseX, mouseY);
    }

    private void drawPageButton(final int x, final int y, final boolean forward, final boolean enabled,
                                final int mouseX, final int mouseY) {
        final boolean hovered = mouseX >= x && mouseX < x + BUTTON_WIDTH
            && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        // Fabric's own sheet coordinates: hovering moves 20 across, being disabled moves 12 down, and
        // the forward arrow is the 10 pixels after the back one. A disabled button has no hover state.
        final int u = enabled && hovered ? 20 : 0;
        final int v = enabled ? 0 : BUTTON_HEIGHT;

        bind(BUTTONS_TEXTURE);
        drawTexture(x, y, u + (forward ? BUTTON_WIDTH : 0), v, BUTTON_WIDTH, BUTTON_HEIGHT);

        if (hovered) {
            pageTooltip = "Page " + (currentPage + 1) + "/" + pageCount();
        }
    }

    /** Beta's own button click, the one {@code Screen} plays when a {@code ButtonWidget} is pressed. */
    private void playClick() {
        minecraft.soundManager.playSound("random.click", 1.0F, 1.0F);
    }

    /** Set while drawing a hovered page button; drawn after the panel so it is not covered. */
    private String pageTooltip;

    /** Drawn in the panel's own coordinate space by {@code HandledScreen}, as vanilla titles are. */
    @Override
    protected void drawForeground() {
        if (groups.isEmpty()) {
            return;
        }

        // Modern draws the tab's title unless the tab asked for it to be hidden, and the inventory tab
        // is the only builder in CreativeModeTabs that calls hideTitle() - the search tab keeps its
        // "Search Items" beside the box.
        if (!isInventoryTab()) {
            textRenderer.draw(groups.get(selectedGroup).getDisplayName().getString(), 8, 6, 0x404040);
        }

        if (isSearchTab()) {
            // The box is focused the whole time the tab is open, exactly as modern focuses it on the
            // way in, so it always draws its cursor.
            searchField.render(SEARCH_X, SEARCH_Y);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        searchField.tick();
    }

    /** {@code drawBackground} is not handed the pointer, and the buttons need it for their hover state. */
    private int lastMouseX;
    private int lastMouseY;

    @Override
    public void render(final int mouseX, final int mouseY, final float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (scrolling) {
            // Dragging the knob: modern maps the pointer onto the track, less the knob height.
            scrollOffs = Math.max(0.0F, Math.min(1.0F,
                (mouseY - (top() + 18) - SCROLLER_HEIGHT / 2.0F) / (112.0F - SCROLLER_HEIGHT - 2.0F)));
            updateSlots();
        }

        // Beta reports presses and releases and nothing between them, so dragging a selection across
        // the search box means following the held button ourselves, as the chat screen does.
        if (selecting) {
            if (Mouse.isButtonDown(0)) {
                searchField.click(mouseX - left() - SEARCH_X, true);
            } else {
                selecting = false;
            }
        }

        super.render(mouseX, mouseY, delta);

        // Tab tooltips sit above everything, including the item tooltips vanilla just drew.
        if (pageTooltip != null) {
            drawItemStyleTooltip(pageTooltip, mouseX, mouseY);
        } else {
            drawTabTooltip(mouseX, mouseY);
        }
    }

    private void drawTabTooltip(final int mouseX, final int mouseY) {
        for (int index = 0; index < groups.size(); index++) {
            if (!isTabVisible(index)) {
                continue;
            }
            final int x = left() + tabX(index);
            final int y = tabScreenY(index);
            // Modern's hover box is inset three pixels on each side of the tab.
            if (mouseX >= x + 3 && mouseX < x + 3 + 21 && mouseY >= y + 3 && mouseY < y + 3 + 27) {
                drawItemStyleTooltip(groups.get(index).getDisplayName().getString(), mouseX, mouseY);
                return;
            }
        }
    }

    /**
     * A tooltip drawn exactly the way beta draws the one over an item slot: the same offsets from the
     * pointer, the same three-pixel padding, the same {@code 0xC0000000} gradient box, the same white
     * shadowed text - and with lighting and the depth test off first, which is what stops the text
     * being drawn underneath the box and underneath the block models in the slots.
     */
    private void drawItemStyleTooltip(final String text, final int mouseX, final int mouseY) {
        if (text == null || text.isEmpty()) {
            return;
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        final int x = mouseX + 12;
        final int y = mouseY - 12;
        final int width = textRenderer.getWidth(text);
        fillGradient(x - 3, y - 3, x + width + 3, y + 8 + 3, 0xC0000000, 0xC0000000);
        textRenderer.drawWithShadow(text, x, y, -1);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    // --- input --------------------------------------------------------------------------------------

    /**
     * The draggable part of the scrollbar: the whole track, as modern has it.
     *
     * <p>It once stopped short, because the destroy slot sat at {@code (173, 112)} underneath it and a
     * click down there was aimed at the bin rather than the bar. The bin now exists only on the
     * inventory tab - which has no scrollbar - so nothing is under the track any more and the bottom
     * fifth of it was simply refusing to be grabbed.
     */
    private boolean insideScrollbar(final int mouseX, final int mouseY) {
        // The inventory tab has no scrollbar - it is not drawn there - and its destroy slot sits at
        // (173, 112), right under the track. Claiming that area on a tab with no bar to grab is what
        // stopped shift-clicking the bin from clearing the inventory.
        if (isInventoryTab()) {
            return false;
        }

        final int x = left() + 175;
        final int top = top() + 18;
        // The whole track, {@code 18..130}, which is modern's own insideScrollbar. It used to stop at
        // 112 to keep its hands off the destroy slot below - but that slot now exists only on the
        // inventory tab, which has no scrollbar at all, so the bottom of the bar was simply dead.
        final int bottom = top() + 130;
        return mouseX >= x && mouseX < x + SCROLLER_WIDTH && mouseY >= top && mouseY < bottom;
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        if (button == 0) {
            // A Fabric button swallows the click either way, but a greyed one does nothing and stays
            // silent: modern's AbstractWidget only runs onPress, sound included, while it is active.
            if (pageCount() > 1 && mouseY >= top() + BUTTON_Y && mouseY < top() + BUTTON_Y + BUTTON_HEIGHT) {
                if (mouseX >= left() + BUTTON_PREVIOUS_X && mouseX < left() + BUTTON_PREVIOUS_X + BUTTON_WIDTH) {
                    if (currentPage > 0) {
                        playClick();
                        switchToPage(currentPage - 1);
                    }
                    return;
                }
                if (mouseX >= left() + BUTTON_NEXT_X && mouseX < left() + BUTTON_NEXT_X + BUTTON_WIDTH) {
                    if (currentPage + 1 < pageCount()) {
                        playClick();
                        switchToPage(currentPage + 1);
                    }
                    return;
                }
            }

            if (isSearchTab() && insideSearchBox(mouseX, mouseY)) {
                searchField.click(mouseX - left() - SEARCH_X, RetroKeys.isShiftDown());
                selecting = true;
                return;
            }

            for (int index = 0; index < groups.size(); index++) {
                if (!isTabVisible(index)) {
                    continue;
                }
                final int x = left() + tabX(index);
                final int y = tabScreenY(index);
                if (mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT) {
                    selectedGroup = index;
                    searchField.setText("");
                    refresh();
                    return;
                }
            }

            if (insideScrollbar(mouseX, mouseY)) {
                scrolling = canScroll();
                return;
            }
        }

        // Everything else is a slot click, which is vanilla's job.
        super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Which hotbar slot a key means, or -1 for a key that is not one.
     *
     * <p>Read from the key code where there is one and from the typed character otherwise, because a
     * character-only event carries no usable code and must still be recognised - if only to be
     * swallowed rather than typed into the search box.
     */
    private static int retroapi$hotbarDigit(final char typed, final int keyCode) {
        if (keyCode >= KEY_1 && keyCode <= KEY_1 + NUM_COLS - 1) {
            return keyCode - KEY_1;
        }
        return typed >= '1' && typed <= '9' ? typed - '1' : -1;
    }

    /** Beta's own hit box, which {@code HandledScreen} keeps private: a pixel of slop on each side. */
    private int hoveredSlotId(final int mouseX, final int mouseY) {
        final int x = mouseX - left();
        final int y = mouseY - top();

        for (int id = 0; id < handler.slots.size(); id++) {
            final Slot slot = handler.slots.get(id);
            if (x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) {
                return id;
            }
        }
        return -1;
    }

    private boolean insideSearchBox(final int mouseX, final int mouseY) {
        final int x = left() + SEARCH_X;
        final int y = top() + SEARCH_Y;
        return mouseX >= x && mouseX < x + SEARCH_WIDTH && mouseY >= y && mouseY < y + SEARCH_HEIGHT;
    }

    @Override
    protected void mouseReleased(final int mouseX, final int mouseY, final int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            scrolling = false;
            selecting = false;
        }
    }

    @Override
    protected void keyPressed(final char typed, final int keyCode) {
        if (keyCode == RetroKeys.PAGE_UP) {
            switchToPage(currentPage - 1);
            return;
        }
        if (keyCode == RetroKeys.PAGE_DOWN) {
            switchToPage(currentPage + 1);
            return;
        }

        // Modern's hotbar swap, which beta has no equivalent of at all: a number key over ANY slot
        // puts what is under the pointer into that hotbar slot, over whatever was there.
        //
        // The pointer decides, not the result: while a slot is hovered the number is this screen's and
        // the search box never sees it, however the swap turned out. That matters here in a way it does
        // not in modern, because a key can arrive twice - the press itself and, on backends that send
        // text separately, a character-only echo with no key code. Swallowing on "a slot is hovered"
        // catches both, and the same-tick guard below stops the echo swapping a second time and undoing
        // the first.
        final int digit = retroapi$hotbarDigit(typed, keyCode);
        if (digit >= 0) {
            final int hovered = hoveredSlotId(lastMouseX, lastMouseY);
            if (hovered >= 0) {
                if (ticks != lastSwapTick || digit != lastSwapDigit) {
                    creativeHandler.swapToHotbar(hovered, digit);
                    lastSwapTick = ticks;
                    lastSwapDigit = digit;
                }
                return;
            }
        }

        // The search field only exists on the search tab, exactly as in modern; everywhere else the
        // keys belong to vanilla, so E still closes the screen. Modern rebuilds the results only when
        // the key actually changed the text - a bare arrow key or Ctrl+C must not re-search.
        if (isSearchTab()) {
            final String before = searchField.getText();
            if (searchField.keyPressed(typed, keyCode)) {
                if (!before.equals(searchField.getText())) {
                    refresh();
                }
                return;
            }

            // Typing is active, so the inventory key is a letter being typed, not a way out - it
            // belongs to the box exactly as it does to modern's focused EditBox. Held here rather
            // than left to the box's own character filter, because the key can be rebound to one
            // the filter does not take and closing the screen mid-search is the one thing it must
            // never do. Escape still closes, as it does in modern.
            if (keyCode == minecraft.options.inventoryKey.code) {
                return;
            }
        }

        super.keyPressed(typed, keyCode);
    }

    /** Beta routes every mouse event through here, wheel included. */
    @Override
    public void onMouseEvent() {
        super.onMouseEvent();

        final int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || isInventoryTab() || !canScroll()) {
            return;
        }
        scrollOffs = Math.max(0.0F, Math.min(1.0F, scrollOffs - (wheel > 0 ? 1.0F : -1.0F) / offscreenRows()));
        updateSlots();
    }
}
