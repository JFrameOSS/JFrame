package io.github.jframe.datasource.logging;

import io.github.support.UnitTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("Datasource - SqlStatementLogging")
public class SqlStatementLoggingTest extends UnitTest {

    @AfterEach
    public void tearDown() {
        SqlStatementLogging.clear();
    }

    @Test
    @DisplayName("Should keep outer suppression after nested suppression closes")
    public void shouldKeepOuterSuppressionAfterNestedClose() {
        // Given: An active outer suppression
        final SqlLoggingState outer = SqlStatementLogging.suppress();

        // When: A nested suppression is opened and closed
        SqlStatementLogging.suppress().close();

        // Then: The outer suppression is still active until it closes
        assertThat(SqlStatementLogging.isSuppressed(), is(true));
        outer.close();
        assertThat(SqlStatementLogging.isSuppressed(), is(false));
    }

    @Test
    @DisplayName("Should keep outer suppression after nested suppress(false) closes")
    public void shouldKeepOuterSuppressionAfterNestedNonSuppressingClose() {
        // Given: An active outer suppression
        final SqlLoggingState outer = SqlStatementLogging.suppress();

        // When: A nested non-suppressing state is closed
        SqlStatementLogging.suppress(false).close();

        // Then: The outer suppression is still active
        assertThat(SqlStatementLogging.isSuppressed(), is(true));
        outer.close();
    }
}
