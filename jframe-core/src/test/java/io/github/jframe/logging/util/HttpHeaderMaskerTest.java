package io.github.jframe.logging.util;

import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link HttpHeaderMasker}.
 */
@DisplayName("Logging - HttpHeaderMasker")
class HttpHeaderMaskerTest extends UnitTest {

    private final PasswordMasker passwordMasker = new PasswordMasker(List.of("password", "apiKey"));

    @Test
    @DisplayName("Should mask credential headers regardless of fields-to-mask")
    void shouldMaskCredentialHeaders() {
        // Given / When / Then: credential headers are fully masked, case-insensitively
        assertThat(HttpHeaderMasker.mask("Authorization", "Bearer abc", passwordMasker), is("***"));
        assertThat(HttpHeaderMasker.mask("proxy-authorization", "Basic abc", passwordMasker), is("***"));
        assertThat(HttpHeaderMasker.mask("Cookie", "SESSION=abc", passwordMasker), is("***"));
        assertThat(HttpHeaderMasker.mask("Set-Cookie", "SESSION=abc; HttpOnly", passwordMasker), is("***"));
    }

    @Test
    @DisplayName("Should mask header whose name is a configured field to mask")
    void shouldMaskHeaderNamedAfterConfiguredField() {
        // Given / When / Then: header names matching fields-to-mask are masked
        assertThat(HttpHeaderMasker.mask("X-ApiKey", "abc", passwordMasker), is("***"));
    }

    @Test
    @DisplayName("Should keep other headers but mask embedded passwords")
    void shouldKeepOtherHeaders() {
        // Given / When / Then: non-sensitive headers pass through the password masker only
        assertThat(HttpHeaderMasker.mask("Content-Type", "application/json", passwordMasker), is("application/json"));
        assertThat(HttpHeaderMasker.mask("X-Auth", "user=a&password=b", passwordMasker), is("user=a&password=***"));
    }
}
