package io.github.jframe.logging.masker.type;

import io.github.jframe.logging.masker.MaskedPasswordBuilder;
import io.github.jframe.logging.masker.Masker;

import static io.github.jframe.util.constants.Constants.Characters.*;

/**
 * Masks passwords in a json key value.
 */
public class JsonPasswordMasker implements Masker {

    /**
     * {@inheritDoc}
     *
     * @param builder a "string builder" that builds a string without the password fields.
     * @return {@code true} if a password was masked, {@code false} otherwise.
     */
    @Override
    public boolean matches(final MaskedPasswordBuilder builder) {
        if (builder.currentCharIs(QUOTE)) {
            // Assumption: start of json.
            builder.mark();
            builder.next();
            if (readUntilStartOfJsonValue(builder)) {
                final int indexOfStartPassword = builder.getCurrentIndex();
                if (readUntilEndOfJsonValue(builder)) {
                    builder.maskPasswordAt(indexOfStartPassword + 1);
                    return true;
                }
            }

            builder.reset();
        }
        return false;
    }

    /**
     * Returns the index of the QUOTE that starts the JSON value.
     *
     * <p>Will return {@code null} if there is no quote found.
     */
    private static boolean readUntilStartOfJsonValue(final MaskedPasswordBuilder builder) {
        readWhiteSpaces(builder);
        if (builder.currentCharIs(COLON)) {
            builder.next();
            readWhiteSpaces(builder);

            return builder.currentCharIs(QUOTE);
        }
        return false;
    }

    /**
     * Returns the index of the QUOTE that ends the JSON value.
     *
     * <p>Will return {@code null} if there is no quote found.
     */
    private static boolean readUntilEndOfJsonValue(final MaskedPasswordBuilder builder) {
        boolean escape = false;
        while (builder.hasNext()) {
            builder.next();
            if (!escape && builder.currentCharIs(QUOTE)) {
                return true;
            }
            escape = builder.currentCharIs(JSON_ESCAPE);
        }
        return false;
    }

    private static void readWhiteSpaces(final MaskedPasswordBuilder builder) {
        while (builder.currentCharIsWhitespace() && builder.hasNext()) {
            builder.next();
        }
    }
}
