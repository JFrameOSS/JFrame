package io.github.jframe.logging.ecs;

/**
 * A wrapper around a EcsField where the field is closeable.
 *
 * <p>Closing the field will remove the field (and it's value) from the EcsFields, so further
 * logging will not be marked with the field.
 */
public class AutoCloseableEcsFieldImpl implements AutoCloseableEcsField {

    /** The delegate log field to close. */
    private final EcsField delegate;

    /**
     * The constructor.
     *
     * @param delegate The delegate log field to close.
     */
    public AutoCloseableEcsFieldImpl(final EcsField delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getKey() {
        return delegate.getKey();
    }

    @Override
    public boolean matches(final String key) {
        return delegate.matches(key);
    }

    @Override
    public void close() {
        EcsFields.clear(this);
    }
}
