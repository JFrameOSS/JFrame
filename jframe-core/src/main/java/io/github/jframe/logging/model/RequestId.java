package io.github.jframe.logging.model;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/** Class that holds a request id in a ThreadLocal. */
@UtilityClass
public class RequestId {

    /** The thread local id. */
    private static final ThreadLocal<UUID> UUID_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * Return the id as string.
     *
     * @return The ID as string, or {@code null} if not set.
     */
    public static String get() {
        if (UUID_THREAD_LOCAL.get() == null) {
            return null;
        }
        return UUID_THREAD_LOCAL.get().toString();
    }

    /**
     * Set the request id.
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
