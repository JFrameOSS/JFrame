package io.github.jframe.exception.resource;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("Unit Test - ProblemDetails title")
class ProblemDetailsTitleTest extends UnitTest {

    @ParameterizedTest
    @CsvSource(
        {
            "404, Not Found",
            "199, Informational",
            "299, Success",
            "399, Redirection",
            "499, Client Error",
            "599, Server Error",
            "999, Unknown Status"
        }
    )
    @DisplayName("Should return reason phrase or human-readable family label")
    void shouldReturnTitle(final int status, final String expected) {
        // When / Then
        assertThat(ProblemDetails.title(status), is(expected));
    }
}
