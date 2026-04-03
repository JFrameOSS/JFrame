package io.github.jframe.logging.model;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/** Class that holds a transaction id in a ThreadLocal. */
@UtilityClass
public class TransactionId {

    /** The thread local id. */
    private static final ThreadLocal<UUID> UUID_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * Return the id as string.
     *
     * @return The ID as string, or {@code null} if not set.
     */
    public static String get() {
        final UUID uuid = UUID_THREAD_LOCAL.get();
        if (uuid == null) {
            return null;
        }
        return uuid.toString();
    }

    /**
     * Set the transaction id.
     *
     * @param value The UUID to set.
     */
    public static void set(final UUID value) {
        UUID_THREAD_LOCAL.set(value);
    }

    /** Clear the thread local. */
    public static void remove() {
        UUID_THREAD_LOCAL.remove();
    }
}
