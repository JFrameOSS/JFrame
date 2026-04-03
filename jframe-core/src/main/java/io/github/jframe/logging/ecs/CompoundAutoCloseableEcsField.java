package io.github.jframe.logging.ecs;


import static java.util.Objects.requireNonNull;

/**
 * A wrapper around a EcsField where the field is closeable.
 *
 * <p>Closing the field will remove the field (and it's value) from the EcsFields, so further
 * logging will not be marked with the field.
 */
public class CompoundAutoCloseableEcsField implements AutoCloseableEcsField {

    private final AutoCloseableEcsField[] fields;

    /**
     * Create an instance that closes the given {@code fields}.
     *
     * @param fields The, never {@code null}, auto-closeable log fields.
     */
    public CompoundAutoCloseableEcsField(final AutoCloseableEcsField... fields) {
        this.fields = requireNonNull(fields);
    }

    @Override
    public void close() {
        for (final AutoCloseableEcsField field : fields) {
            field.close();
        }
    }

    @Override
    public String getKey() {
        return "compound[" + fields.length + "]";
    }
}
