package io.github.jframe.logging.masker.type;

import io.github.support.UnitTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PasswordMasker}.
 *
 * <p>Verifies the PasswordMasker functionality including:
 * <ul>
 * <li>Masking passwords in JSON format</li>
 * <li>Masking passwords in URI query strings</li>
 * <li>Masking passwords in POST body format</li>
 * <li>Handling multiple field names</li>
 * <li>Default field name behavior</li>
 * <li>Handling strings without passwords</li>
 * </ul>
 */
@DisplayName("Logging - PasswordMasker")
class PasswordMaskerTest extends UnitTest {

    @Test
    @DisplayName("Should mask password in JSON format")
    void maskPasswordsIn_withJsonPassword_shouldMaskPassword() {
        // Given: A PasswordMasker and JSON string with password field
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"username\":\"john\",\"password\":\"secret123\"}";

        // When: Masking passwords in the JSON string
        final String result = masker.maskPasswordsIn(input);

        // Then: Password value is masked with asterisks
        assertThat(result).isEqualTo("{\"username\":\"john\",\"password\":\"***\"}");
    }

    @Test
    @DisplayName("Should mask password in JSON with whitespace")
    void maskPasswordsIn_withJsonPasswordAndWhitespace_shouldMaskPassword() {
        // Given: A PasswordMasker and JSON string with whitespace around password
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"username\": \"john\", \"password\" : \"secret123\"}";

        // When: Masking passwords in the JSON string with whitespace
        final String result = masker.maskPasswordsIn(input);

        // Then: Password value is masked regardless of whitespace
        assertThat(result).isEqualTo("{\"username\": \"john\", \"password\" : \"***\"}");
    }

    @Test
    @DisplayName("Should mask password in URI query string")
    void maskPasswordsIn_withUriQueryString_shouldMaskPassword() {
        // Given: A PasswordMasker and URI query string with password parameter
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "username=john&password=secret123&remember=true";

        // When: Masking passwords in the query string
        final String result = masker.maskPasswordsIn(input);

        // Then: Password parameter value is masked
        assertThat(result).isEqualTo("username=john&password=***&remember=true");
    }

    @Test
    @DisplayName("Should mask password at end of URI query string")
    void maskPasswordsIn_withUriQueryStringPasswordAtEnd_shouldMaskPassword() {
        // Given: A PasswordMasker and URI query string with password at the end
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "username=john&password=secret123";

        // When: Masking passwords in the query string
        final String result = masker.maskPasswordsIn(input);

        // Then: Password parameter value is masked at the end
        assertThat(result).isEqualTo("username=john&password=***");
    }

    @Test
    @DisplayName("Should mask multiple occurrences of password")
    void maskPasswordsIn_withMultiplePasswords_shouldMaskAll() {
        // Given: A PasswordMasker and string with multiple password occurrences
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"password\":\"secret1\"} and {\"password\":\"secret2\"}";

        // When: Masking all password occurrences
        final String result = masker.maskPasswordsIn(input);

        // Then: All password values are masked
        assertThat(result).isEqualTo("{\"password\":\"***\"} and {\"password\":\"***\"}");
    }

    @Test
    @DisplayName("Should mask multiple field names")
    void maskPasswordsIn_withMultipleFieldNames_shouldMaskAllFields() {
        // Given: A PasswordMasker with multiple field names to mask
        final PasswordMasker masker = new PasswordMasker(Arrays.asList("password", "apiKey", "token"));
        final String input = "{\"password\":\"secret\",\"apiKey\":\"key123\",\"token\":\"tok456\"}";

        // When: Masking all configured field names
        final String result = masker.maskPasswordsIn(input);

        // Then: All configured fields are masked
        assertThat(result).isEqualTo("{\"password\":\"***\",\"apiKey\":\"***\",\"token\":\"***\"}");
    }

    @Test
    @DisplayName("Should use default field name 'password' when null fields provided")
    void maskPasswordsIn_withNullFieldsToMask_shouldUseDefaultPassword() {
        // Given: A PasswordMasker with null fields to mask
        final PasswordMasker masker = new PasswordMasker(null);
        final String input = "{\"password\":\"secret\"}";

        // When: Masking with default field name
        final String result = masker.maskPasswordsIn(input);

        // Then: Password field is masked
        assertThat(result).isEqualTo("{\"password\":\"***\"}");
    }

    @Test
    @DisplayName("Should use default field name 'password' when empty fields provided")
    void maskPasswordsIn_withEmptyFieldsToMask_shouldUseDefaultPassword() {
        // Given: A PasswordMasker with empty fields to mask
        final PasswordMasker masker = new PasswordMasker(Collections.emptyList());
        final String input = "{\"password\":\"secret\"}";

        // When: Masking with default field name
        final String result = masker.maskPasswordsIn(input);

        // Then: Password field is masked
        assertThat(result).isEqualTo("{\"password\":\"***\"}");
    }

    @Test
    @DisplayName("Should return unchanged string when no password fields found")
    void maskPasswordsIn_withNoPasswordFields_shouldReturnUnchanged() {
        // Given: A PasswordMasker and string without password fields
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"username\":\"john\",\"email\":\"john@example.com\"}";

        // When: Masking passwords in string without password fields
        final String result = masker.maskPasswordsIn(input);

        // Then: String is returned unchanged
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("Should handle empty string")
    void maskPasswordsIn_withEmptyString_shouldReturnEmpty() {
        // Given: A PasswordMasker and empty string
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "";

        // When: Masking passwords in empty string
        final String result = masker.maskPasswordsIn(input);

        // Then: Empty string is returned
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should mask password with special characters in JSON")
    void maskPasswordsIn_withSpecialCharactersInPassword_shouldMaskPassword() {
        // Given: A PasswordMasker and JSON with password containing special characters
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"password\":\"p@ssw0rd!#$%\"}";

        // When: Masking password with special characters
        final String result = masker.maskPasswordsIn(input);

        // Then: Password with special characters is masked
        assertThat(result).isEqualTo("{\"password\":\"***\"}");
    }

    @Test
    @DisplayName("Should mask password with escaped quotes in JSON")
    void maskPasswordsIn_withEscapedQuotesInPassword_shouldMaskPassword() {
        // Given: A PasswordMasker and JSON with password containing escaped quotes
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"password\":\"pass\\\"word\"}";

        // When: Masking password with escaped quotes
        final String result = masker.maskPasswordsIn(input);

        // Then: Password with escaped quotes is properly masked
        assertThat(result).isEqualTo("{\"password\":\"***\"}");
    }

    @Test
    @DisplayName("Should mask password in POST body format")
    void maskPasswordsIn_withPostBodyFormat_shouldMaskPassword() {
        // Given: A PasswordMasker and POST body format string
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "username=john&password=secret123&email=john@example.com";

        // When: Masking password in POST body format
        final String result = masker.maskPasswordsIn(input);

        // Then: Password in POST body is masked
        assertThat(result).isEqualTo("username=john&password=***&email=john@example.com");
    }

    @Test
    @DisplayName("Should handle mixed JSON and query string formats")
    void maskPasswordsIn_withMixedFormats_shouldMaskBoth() {
        // Given: A PasswordMasker and string with mixed formats
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "Request: username=john&password=secret1 Response: {\"password\":\"secret2\"}";

        // When: Masking passwords in mixed format string
        final String result = masker.maskPasswordsIn(input);

        // Then: Passwords in both formats are masked
        assertThat(result).contains("password=***");
        assertThat(result).contains("\"password\":\"***\"");
    }

    @Test
    @DisplayName("Should mask field names in case-insensitive manner")
    void maskPasswordsIn_withDifferentCases_shouldMaskBothCases() {
        // Given: A PasswordMasker configured for lowercase 'password'
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"password\":\"secret\",\"Password\":\"secret2\"}";

        // When: Masking field names
        final String result = masker.maskPasswordsIn(input);

        // Then: Both password and Password are masked (case-insensitive matching)
        assertThat(result).isEqualTo("{\"password\":\"***\",\"Password\":\"***\"}");
    }

    @Test
    @DisplayName("Should handle password field in nested JSON")
    void maskPasswordsIn_withNestedJson_shouldMaskPassword() {
        // Given: A PasswordMasker and nested JSON with password
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "{\"user\":{\"name\":\"john\",\"password\":\"secret\"}}";

        // When: Masking password in nested JSON structure
        final String result = masker.maskPasswordsIn(input);

        // Then: Password in nested structure is masked
        assertThat(result).isEqualTo("{\"user\":{\"name\":\"john\",\"password\":\"***\"}}");
    }

    @Test
    @DisplayName("Should handle query string with quotes as delimiter")
    void maskPasswordsIn_withQueryStringQuoteDelimiter_shouldMaskPassword() {
        // Given: A PasswordMasker and query string with quote as delimiter
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "username=john&password=secret\"extra";

        // When: Masking password with quote delimiter
        final String result = masker.maskPasswordsIn(input);

        // Then: Password is masked up to the quote
        assertThat(result).isEqualTo("username=john&password=***\"extra");
    }

    @Test
    @DisplayName("Should handle password with newline delimiter")
    void maskPasswordsIn_withNewlineDelimiter_shouldMaskPassword() {
        // Given: A PasswordMasker and string with newline after password
        final PasswordMasker masker = new PasswordMasker(List.of("password"));
        final String input = "password=secret\nusername=john";

        // When: Masking password with newline delimiter
        final String result = masker.maskPasswordsIn(input);

        // Then: Password is masked up to newline
        assertThat(result).isEqualTo("password=***\nusername=john");
    }
}
