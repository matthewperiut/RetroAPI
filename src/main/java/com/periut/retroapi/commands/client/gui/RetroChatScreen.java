package com.periut.retroapi.commands.client.gui;

import com.periut.retroapi.commands.client.ClientCommands;
import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.HoverEvent;
import com.periut.retroapi.text.Style;
import com.periut.retroapi.text.Text;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * The chat screen, opened with the chat key.
 *
 * <p>Written from scratch rather than grafted onto beta's {@code ChatScreen}, which is a single
 * string with a cursor pinned to its end. This one has modern Minecraft's behaviour: a real text
 * field with selection and clipboard support, live command colouring, a completion window, sent
 * message history on the arrow keys, a scrollable chat window behind it, and clickable, hoverable
 * chat lines.
 */
public class RetroChatScreen extends Screen {
    /** Sent messages, newest last, kept for the session as modern's history is. */
    private static final List<String> HISTORY = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    private final String initialText;

    private ChatInputField input;
    private CommandSuggestor suggestor;
    private int historyIndex = -1;
    private String draft = "";
    /** Set while a left-drag that began in the input box is still going. */
    private boolean selecting;

    public RetroChatScreen() {
        this("");
    }

    public RetroChatScreen(final String initialText) {
        this.initialText = initialText;
    }

    @Override
    public void init() {
        // Beta only reports the first press of a held key unless repeats are asked for.
        Keyboard.enableRepeatEvents(true);

        historyIndex = -1;
        draft = "";

        input = new ChatInputField(textRenderer, width - 8);
        input.setMaxLength(256);
        suggestor = new CommandSuggestor(this, input, textRenderer);
        input.setRenderTextProvider(suggestor::highlight);
        input.setChangedListener(text -> suggestor.refresh());
        input.setText(initialText);

        RetroChatHud.getInstance().reflow(Math.min(320, width - 10));
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
        RetroChatHud.getInstance().resetScroll();
    }

    @Override
    public void tick() {
        input.tick();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void keyPressed(final char typed, final int keyCode) {
        if (suggestor.keyPressed(typed, keyCode)) {
            return;
        }

        switch (keyCode) {
            case Keys.ESCAPE -> minecraft.setScreen(null);
            case Keys.RETURN, Keys.NUMPAD_ENTER -> send();
            case Keys.UP -> browseHistory(1);
            case Keys.DOWN -> browseHistory(-1);
            case Keys.PAGE_UP -> RetroChatHud.getInstance().scroll(RetroChatHud.PAGE_LINES);
            case Keys.PAGE_DOWN -> RetroChatHud.getInstance().scroll(-RetroChatHud.PAGE_LINES);
            default -> {
                if (input.keyPressed(typed, keyCode)) {
                    // Typing invalidates where the player was in their history.
                    historyIndex = -1;
                }
            }
        }
    }

    private void send() {
        final String text = input.getText().trim();
        if (!text.isEmpty()) {
            remember(text);
            dispatch(text);
        }
        minecraft.setScreen(null);
    }

    private void dispatch(final String text) {
        if (!text.startsWith("/")) {
            minecraft.player.sendChatMessage(text);
            return;
        }

        // On a server the server decides; in singleplayer this client is the only authority there is.
        if (ClientCommands.isRemote()) {
            minecraft.player.sendChatMessage(text);
        } else {
            ClientCommands.execute(text.substring(1));
        }
    }

    private static void remember(final String message) {
        HISTORY.remove(message);
        HISTORY.add(message);
        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.remove(0);
        }
    }

    /** @param direction 1 for older, -1 for newer */
    private void browseHistory(final int direction) {
        if (HISTORY.isEmpty()) {
            return;
        }

        if (historyIndex == -1) {
            if (direction < 0) {
                return;
            }
            draft = input.getText();
        }

        final int next = historyIndex + direction;
        if (next < -1 || next >= HISTORY.size()) {
            return;
        }

        historyIndex = next;
        input.setText(historyIndex == -1 ? draft : HISTORY.get(HISTORY.size() - 1 - historyIndex));
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        if (suggestor.mouseClicked(mouseX, mouseY, button)) {
            return;
        }

        if (button == 0) {
            final Style style = RetroChatHud.getInstance().getStyleAt(mouseX, mouseY);
            if (style != null && handleClick(style)) {
                return;
            }

            if (mouseY >= height - 14) {
                input.click(mouseX - 4, Keys.isShiftDown());
                selecting = true;
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    /** @return whether the click was consumed by a chat component */
    private boolean handleClick(final Style style) {
        final ClickEvent event = style.getClickEvent();
        if (event == null) {
            return false;
        }

        switch (event.getAction()) {
            case RUN_COMMAND -> {
                final String command = event.getValue();
                remember(command);
                dispatch(command);
                minecraft.setScreen(null);
            }
            case SUGGEST_COMMAND -> input.setText(event.getValue());
            case COPY_TO_CLIPBOARD -> Clipboard.write(event.getValue());
            case OPEN_URL -> openUrl(event.getValue());
            default -> {
                return false;
            }
        }

        return true;
    }

    private void openUrl(final String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (final Exception ignored) {
            // No browser, no desktop, or a malformed link - none of it worth a crash.
        }
    }

    /**
     * Beta routes every mouse event through here, wheel included, and the wheel is the only way to
     * reach chat scrollback.
     */
    @Override
    public void onMouseEvent() {
        super.onMouseEvent();

        final int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        final int lines = wheel > 0 ? 1 : -1;
        if (!suggestor.mouseScrolled(lines)) {
            RetroChatHud.getInstance().scroll(lines * (Keys.isControlDown() ? RetroChatHud.PAGE_LINES : 1));
        }
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float delta) {
        // Beta reports presses and releases and nothing in between, so dragging the cursor across
        // the text to select it means following the held button ourselves, frame by frame.
        if (selecting) {
            if (Mouse.isButtonDown(0)) {
                // Selecting, so the anchor the press dropped stays where it is.
                input.click(mouseX - 4, true);
            } else {
                selecting = false;
            }
        }

        // The chat window itself is drawn by the HUD, which runs before any screen; drawing it
        // again here would stack its translucent backgrounds.
        fill(2, height - 14, width - 2, height - 2, 0x80000000);
        input.render(4, height - 12);

        suggestor.render(mouseX, mouseY);

        final Style style = RetroChatHud.getInstance().getStyleAt(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            renderTooltip(style.getHoverEvent(), mouseX, mouseY);
        }

        super.render(mouseX, mouseY, delta);
    }

    private void renderTooltip(final HoverEvent hover, final int mouseX, final int mouseY) {
        if (hover.getAction() != HoverEvent.Action.SHOW_TEXT || hover.getValue() == null) {
            return;
        }

        final TextDrawer.Line line = TextDrawer.flatten(hover.getValue());
        final int textWidth = TextDrawer.INSTANCE.width(textRenderer, line);
        final int x = Math.min(mouseX + 6, width - textWidth - 6);
        final int y = Math.max(mouseY - 12, 2);

        fill(x - 3, y - 3, x + textWidth + 3, y + 9, 0xF0100010);
        TextDrawer.INSTANCE.draw(textRenderer, line, x, y, 255);
    }

    /** So other code can open chat with something already typed, e.g. a command suggestion. */
    public static RetroChatScreen withText(final Text text) {
        return new RetroChatScreen(text.getString());
    }
}
