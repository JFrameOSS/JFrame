package io.github.jframe.logging.masker;

/**
 * Class that tries to mask a password in a string.
 *
 * <p>The type tries to mask a field for a specific format, for instance JSON or XML.
 */
@FunctionalInterface
public interface Masker {

    /**
     * Check if the builder matches the pattern could be masked.
     *
     * @param builder a "string builder" that builds a string without the password fields.
     * @return whether some masking has been done.
     */
    boolean matches(MaskedPasswordBuilder builder);
}
