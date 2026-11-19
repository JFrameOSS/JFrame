package io.github.jframe.logging.masker.type;

import io.github.jframe.logging.masker.MaskedPasswordBuilder;
import io.github.jframe.logging.masker.Masker;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Class the mask passwords in a string, so log files will not contain plain text (or encrypted) passwords.
 */
public class PasswordMasker {

    /** The list of password maskers. */
    private static final List<Masker> PASSWORD_MASKERS = List.of(new JsonPasswordMasker(), new UriQueryStringPasswordMasker());

    /** The list of fields to mask. */
    private final Set<String> fieldsToMask = new LinkedHashSet<>();

    /**
     * The constructor.
     *
     * @param fieldsToMask The list of fields to mask.
     */
    public PasswordMasker(final Collection<String> fieldsToMask) {
        if (fieldsToMask != null && !fieldsToMask.isEmpty()) {
            this.fieldsToMask.addAll(fieldsToMask);
        } else {
            this.fieldsToMask.add("password");
        }
    }

    /**
     * Mask the password with {@code ***} in the {@code input}.
     *
     * @param input the input to mask passwords in.
     * @return The masked result.
     */
    public String maskPasswordsIn(final String input) {
        String masked = input;
        for (final String fieldToMask : fieldsToMask) {
            masked = maskPasswords(masked, fieldToMask);
        }
        return masked;
    }

    /**
     * Returns {@code true} if {@code name} contains a configured field to mask, case-insensitively.
     *
     * @param name the field or header name.
     * @return {@code true} if values named {@code name} must be masked.
     */
    public boolean masksField(final String name) {
        final String lowerName = name.toLowerCase(Locale.ROOT);
        return fieldsToMask.stream().anyMatch(field -> lowerName.contains(field.toLowerCase(Locale.ROOT)));
    }

    private static String maskPasswords(final String input, final String pattern) {
        final MaskedPasswordBuilder builder = new MaskedPasswordBuilder(input, pattern);
        if (!builder.findNextPassword()) {
            return input;
        }
        builder.reset();
        return maskPasswords(builder);
    }

    @SuppressWarnings("CyclomaticComplexity")
    private static String maskPasswords(final MaskedPasswordBuilder builder) {

        while (builder.findNextPassword()) {
            while (builder.hasNext()) {
                boolean fieldMasked = false;
                for (final Masker masker : PASSWORD_MASKERS) {
                    if (masker.matches(builder)) {
                        fieldMasked = true;
                        break;
                    }
                }
                if (fieldMasked) {
                    break;
                }
                if (builder.currentCharIsWhitespace()) {
                    // We've found a whitespace, this means the input is not in one of the expected formats,
                    // break the loop and search again.
                    break;
                }
                builder.next();
            }
        }

        return builder.build();
    }
}
