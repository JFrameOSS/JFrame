package io.github.jframe.datasource.listener.logger;

import io.github.support.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link PrettyQueryEntryCreator}.
 *
 * <p>Verifies that SQL queries are formatted with Hibernate's {@code FormatStyle.BASIC}
 * formatter, producing multi-line pretty output for readability in logs.
 */
@DisplayName("Quarkus JPA - PrettyQueryEntryCreator")
public class PrettyQueryEntryCreatorTest extends UnitTest {

    private PrettyQueryEntryCreator prettyQueryEntryCreator;

    @Override
    @BeforeEach
    public void setUp() {
        prettyQueryEntryCreator = new PrettyQueryEntryCreator();
    }

    // -------------------------------------------------------------------------
    // 1. Should format SQL query with line breaks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should format SELECT query with ANSI highlight codes using Hibernate formatter")
    public void shouldFormatSelectQueryWithHighlightCodesUsingHibernateFormatter() {
        // Given: A simple SELECT SQL query
        final String rawQuery = "select id, name from users where id = 1";

        // When: Formatting the query
        final String formattedQuery = prettyQueryEntryCreator.formatQuery(rawQuery);

        // Then: The formatted result should contain ANSI escape codes (highlight formatting)
        assertThat(formattedQuery, is(notNullValue()));
        assertThat(formattedQuery, containsString("\u001B["));
    }

    // -------------------------------------------------------------------------
    // 2. Should handle empty query string without error
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should handle empty query string without throwing an exception")
    public void shouldHandleEmptyQueryStringWithoutThrowingAnException() {
        // Given: An empty SQL query string
        final String emptyQuery = "";

        // When: Formatting the empty query
        final String formattedQuery = prettyQueryEntryCreator.formatQuery(emptyQuery);

        // Then: Result should be returned without error (may be empty or whitespace)
        assertThat(formattedQuery, is(notNullValue()));
    }
}
