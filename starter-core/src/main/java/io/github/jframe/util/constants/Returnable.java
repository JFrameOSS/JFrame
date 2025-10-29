package io.github.jframe.util.constants;

/**
 * Interface to allow wrapping of a returning call.
 *
 * @param <T> The factory to return.
 */
@FunctionalInterface
public interface Returnable<T> {

    /**
     * The call to invoke.
     *
     * @return the return value of the wrapped call.
     */
    T invoke() throws Throwable;
}
