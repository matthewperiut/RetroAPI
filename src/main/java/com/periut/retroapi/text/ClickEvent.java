package com.periut.retroapi.text;

import java.util.Locale;
import java.util.Objects;

public class ClickEvent {
    private final Action action;
    private final String value;

    public ClickEvent(final Action action, final String value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClickEvent)) {
            return false;
        }
        final ClickEvent that = (ClickEvent) o;
        return action == that.action && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, value);
    }

    @Override
    public String toString() {
        return "ClickEvent{action=" + action + ", value='" + value + "'}";
    }

    public enum Action {
        OPEN_URL("open_url", true),
        /** Beta has no file-open handling; kept so a modern payload deserialises rather than failing. */
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        SUGGEST_COMMAND("suggest_command", true),
        CHANGE_PAGE("change_page", false),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true);

        private final String name;
        private final boolean userDefinable;

        Action(final String name, final boolean userDefinable) {
            this.name = name;
            this.userDefinable = userDefinable;
        }

        public String getName() {
            return name;
        }

        public boolean isUserDefinable() {
            return userDefinable;
        }

        public static Action byName(final String name) {
            if (name == null) {
                return null;
            }
            final String lower = name.toLowerCase(Locale.ROOT);
            for (final Action action : values()) {
                if (action.name.equals(lower)) {
                    return action;
                }
            }
            return null;
        }
    }
}
