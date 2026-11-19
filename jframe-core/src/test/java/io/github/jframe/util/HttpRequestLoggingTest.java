package io.github.jframe.util;

import io.github.support.UnitTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("Util - HttpRequestLogging")
public class HttpRequestLoggingTest extends UnitTest {

    @AfterEach
    public void tearDown() {
        HttpRequestLogging.clear();
    }

    @Test
    @DisplayName("Should keep outer suppression after nested suppression closes")
    public void shouldKeepOuterSuppressionAfterNestedClose() {
        // Given: An active outer suppression
        final HttpRequestLoggingState outer = HttpRequestLogging.suppress();

        // When: A nested suppression is opened and closed
        HttpRequestLogging.suppress().close();

        // Then: The outer suppression is still active until it closes
        assertThat(HttpRequestLogging.isSuppressed(), is(true));
        outer.close();
        assertThat(HttpRequestLogging.isSuppressed(), is(false));
    }

    @Test
    @DisplayName("Should keep outer suppression after nested suppress(false) closes")
    public void shouldKeepOuterSuppressionAfterNestedNonSuppressingClose() {
        // Given: An active outer suppression
        final HttpRequestLoggingState outer = HttpRequestLogging.suppress();

        // When: A nested non-suppressing state is closed
        HttpRequestLogging.suppress(false).close();

        // Then: The outer suppression is still active
        assertThat(HttpRequestLogging.isSuppressed(), is(true));
        outer.close();
    }
}
