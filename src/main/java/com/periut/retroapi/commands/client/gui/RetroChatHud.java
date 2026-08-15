package com.periut.retroapi.commands.client.gui;

import com.periut.retroapi.client.gui.RetroTextDrawer;
import com.periut.retroapi.text.Style;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.text.Texts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * The chat window, replacing beta's ten fading lines.
 *
 * <p>Backported from modern Minecraft: a hundred lines of scrollback, mouse-wheel scrolling, word
 * wrapping, per-message fade, and styled text that can be hovered for a tooltip or clicked to run,
 * suggest or copy something. Beta's own chat is suppressed by an {@code InGameHud} mixin and every
 * message it would have drawn is handed here instead.
 */
public class RetroChatHud {
    private static final RetroChatHud INSTANCE = new RetroChatHud();

    /** Modern's limits, which beta has no reason to differ from. */
    private static final int MAX_MESSAGES = 100;
    private static final int VISIBLE_LINES_CLOSED = 10;
    private static final int VISIBLE_LINES_OPEN = 20;
    /** How far Page Up and Page Down move, one screenful less a line of overlap. */
    public static final int PAGE_LINES = VISIBLE_LINES_OPEN - 1;
    private static final int LINE_HEIGHT = 9;
    private static final int WIDTH = 320;
    /** How long a message stays on an unfocused screen, and how long it takes to fade at the end. */
    private static final int LIFETIME_TICKS = 200;

    private final List<Text> messages = new ArrayList<>();
    private final List<VisibleLine> visible = new ArrayList<>();

    private int ticks;
    private int scrolledLines;
    private boolean hasUnreadMessages;
    private int wrapWidth = WIDTH;

    private RetroChatHud() {
    }

    public static RetroChatHud getInstance() {
        return INSTANCE;
    }

    /** One wrapped line, with the tick its message arrived so it can fade on its own schedule. */
    private record VisibleLine(RetroTextDrawer.Line line, int creationTick, boolean endOfMessage) {
    }

    public void addMessage(final Text message) {
        messages.add(0, message);
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(messages.size() - 1);
        }

        final List<RetroTextDrawer.Line> wrapped = RetroTextDrawer.INSTANCE.wrap(textRenderer(), message, wrapWidth);
        for (int i = wrapped.size() - 1; i >= 0; i--) {
            visible.add(0, new VisibleLine(wrapped.get(i), ticks, i == wrapped.size() - 1));
        }
        while (visible.size() > MAX_MESSAGES * 2) {
            visible.remove(visible.size() - 1);
        }

        // Keep whatever the player was reading in place rather than sliding it out from under them.
        if (scrolledLines > 0) {
            scrolledLines += wrapped.size();
            hasUnreadMessages = true;
        }
    }

    /** For plain beta chat lines, which arrive as one {@code §}-coded string. */
    public void addLegacyMessage(final String message) {
        addMessage(Texts.fromLegacy(message));
    }

    public void clear() {
        messages.clear();
        visible.clear();
        scrolledLines = 0;
        hasUnreadMessages = false;
    }

    public void tick() {
        ticks++;
    }

    public List<Text> getMessages() {
        return messages;
    }

    public void scroll(final int lines) {
        scrolledLines = Math.max(0, Math.min(scrolledLines + lines, Math.max(0, visible.size() - VISIBLE_LINES_OPEN)));
        if (scrolledLines == 0) {
            hasUnreadMessages = false;
        }
    }

    public void resetScroll() {
        scrolledLines = 0;
        hasUnreadMessages = false;
    }

    /** Re-wraps everything, for a window resize or a change of chat width. */
    public void reflow(final int width) {
        if (width == wrapWidth) {
            return;
        }
        wrapWidth = width;

        final List<Text> existing = new ArrayList<>(messages);
        messages.clear();
        visible.clear();
        for (int i = existing.size() - 1; i >= 0; i--) {
            addMessage(existing.get(i));
        }
    }

    public void render(final boolean focused) {
        final TextRenderer textRenderer = textRenderer();
        if (textRenderer == null || visible.isEmpty()) {
            return;
        }

        final int limit = focused ? VISIBLE_LINES_OPEN : VISIBLE_LINES_CLOSED;
        final int bottom = bottomY();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int drawn = 0;
        for (int index = scrolledLines; index < visible.size() && drawn < limit; index++, drawn++) {
            final VisibleLine line = visible.get(index);
            final int alpha = focused ? 255 : fadeAlpha(line);
            if (alpha <= 3) {
                continue;
            }

            final int y = bottom - drawn * LINE_HEIGHT;
            RetroTextDrawer.INSTANCE.box(2, y - 1, 2 + wrapWidth + 4, y + LINE_HEIGHT - 1, (alpha / 2) << 24);
            RetroTextDrawer.INSTANCE.draw(textRenderer, line.line(), 4, y, alpha);
        }

        if (focused && scrolledLines > 0) {
            renderScrollBar(bottom, limit);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderScrollBar(final int bottom, final int limit) {
        final int trackHeight = limit * LINE_HEIGHT;
        final int barHeight = Math.max(4, trackHeight * limit / visible.size());
        final int offset = (trackHeight - barHeight) * scrolledLines / Math.max(1, visible.size() - limit);
        final int top = bottom - trackHeight + LINE_HEIGHT;

        final int x = 2 + wrapWidth + 2;
        RetroTextDrawer.INSTANCE.box(x, top, x + 2, top + trackHeight, 0x30FFFFFF);
        RetroTextDrawer.INSTANCE.box(x, top + trackHeight - barHeight - offset, x + 2,
            top + trackHeight - offset, hasUnreadMessages ? 0xFFFFFF55 : 0xAAFFFFFF);
    }

    /** Modern's curve: full opacity for most of a message's life, then a quick fade at the end. */
    private int fadeAlpha(final VisibleLine line) {
        final int age = ticks - line.creationTick();
        if (age >= LIFETIME_TICKS) {
            return 0;
        }

        double fade = 1.0 - age / (double) LIFETIME_TICKS;
        fade *= 10.0;
        fade = Math.max(0.0, Math.min(1.0, fade));
        return (int) (255.0 * fade * fade);
    }

    /**
     * The style under the mouse, for hover tooltips and clicks. Coordinates are in the scaled GUI
     * space the screen itself uses.
     */
    public Style getStyleAt(final int mouseX, final int mouseY) {
        if (visible.isEmpty()) {
            return null;
        }

        final int bottom = bottomY();
        final int row = (bottom + LINE_HEIGHT - 1 - mouseY) / LINE_HEIGHT;
        if (row < 0 || row >= VISIBLE_LINES_OPEN) {
            return null;
        }

        final int index = scrolledLines + row;
        if (index >= visible.size()) {
            return null;
        }
        if (mouseX < 4 || mouseX > 4 + wrapWidth) {
            return null;
        }

        return RetroTextDrawer.INSTANCE.styleAt(textRenderer(), visible.get(index).line(), mouseX - 4);
    }

    private int bottomY() {
        final Minecraft minecraft = minecraft();
        if (minecraft == null || minecraft.currentScreen == null) {
            return scaledHeight() - 40;
        }
        // With the chat screen open the input box owns the bottom of the screen.
        return scaledHeight() - 40;
    }

    private int scaledHeight() {
        final Minecraft minecraft = minecraft();
        if (minecraft == null) {
            return 240;
        }
        return new net.minecraft.client.util.ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight).getScaledHeight();
    }

    private static TextRenderer textRenderer() {
        final Minecraft minecraft = minecraft();
        return minecraft == null ? null : minecraft.textRenderer;
    }

    private static Minecraft minecraft() {
        return (Minecraft) net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    }
}
