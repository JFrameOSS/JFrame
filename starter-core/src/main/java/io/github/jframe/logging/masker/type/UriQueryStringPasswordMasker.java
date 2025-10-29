package io.github.jframe.logging.masker.type;

import io.github.jframe.logging.masker.MaskedPasswordBuilder;
import io.github.jframe.logging.masker.Masker;

import static io.github.jframe.util.constants.Constants.Characters.*;

/**
 * Class that tries to mask a POST body or URI Query for password fields.
 */
public class UriQueryStringPasswordMasker implements Masker {

    /**
     * {@inheritDoc}
     *
     * @param builder a "string builder" that builds a string without the password fields.
     * @return true if a password was masked, false otherwise.
     */
    @Override
    public boolean matches(final MaskedPasswordBuilder builder) {
        if (builder.currentCharIs(EQUALS)) {
            // Assumption: start of URI query string (post body).
            final int indexOfStartPassword = builder.getCurrentIndex();
            readUntilEndOfQueryParameterValue(builder);

            builder.maskPasswordAt(indexOfStartPassword + 1);
            return true;
        }

        return false;
    }

    /**
     * Returns the index of first character that is not part of the current query parameter.
     *
     * <p>That is, it returns the index of the first '&' following the {@code startIndex}, or, it
     * returns {@code input.length()}.
     */
    private static void readUntilEndOfQueryParameterValue(final MaskedPasswordBuilder builder) {
        while (builder.hasNext()) {

            if (builder.currentCharIsOneOf(AMPERSAND, QUOTE, LT, NEW_LINE, CARRIAGE_RETURN)) {
                break;
            }
            builder.next();
        }
    }
}
