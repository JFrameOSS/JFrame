package io.github.jframe.util.constants;

/** Interface to allow wrapping of a void-returning call. */
@FunctionalInterface
public interface Invocable {

    /** The call to invoke. */
    void invoke();
}
