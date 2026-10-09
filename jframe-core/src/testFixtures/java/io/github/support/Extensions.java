package io.github.support;

import io.github.jframe.exception.resource.ErrorResponseResource;

/**
 * Adds extension members to an {@link ErrorResponseResource}.
 */
public final class Extensions {

    private Extensions() {
    }

    /** Adds an extension member to the resource. */
    public static void add(final ErrorResponseResource resource, final String name, final Object value) {
        resource.addExtension(name, value);
    }
}
