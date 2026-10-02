package io.github.jframe.datasource.search.model.resource;

import io.github.support.UnitTest;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@DisplayName("Unit Test - AppliedSort")
class AppliedSortTest extends UnitTest {

    @Test
    @DisplayName("Should store field, direction, and isDefault via constructor")
    void shouldStoreFieldDirectionAndIsDefault() {
        // Given / When
        final var sort = new AppliedSort("name", "ASC", false);

        // Then
        assertThat(sort.getField(), is(equalTo("name")));
        assertThat(sort.getDirection(), is(equalTo("ASC")));
        assertThat(sort.isDefault(), is(false));
    }

    @Test
    @DisplayName("Should report isDefault true when applied from default sort")
    void shouldReportIsDefaultTrue() {
        // Given / When
        final var sort = new AppliedSort("createdAt", "DESC", true);

        // Then
        assertThat(sort.isDefault(), is(true));
    }

    @Test
    @DisplayName("Should serialise isDefault as 'isDefault' JSON property")
    void shouldSerialiseIsDefaultProperty() throws Exception {
        // Given
        final var mapper = new ObjectMapper();
        final var sort = new AppliedSort("name", "ASC", true);

        // When
        final String json = mapper.writeValueAsString(sort);

        // Then
        assertThat(json, containsString("\"isDefault\""));
        assertThat(json, not(containsString("\"default\"")));
    }

    @Test
    @DisplayName("Should include field and direction in JSON output")
    void shouldIncludeFieldAndDirectionInJson() throws Exception {
        // Given
        final var mapper = new ObjectMapper();
        final var sort = new AppliedSort("email", "DESC", false);

        // When
        final String json = mapper.writeValueAsString(sort);

        // Then
        assertThat(json, containsString("\"field\""));
        assertThat(json, containsString("\"email\""));
        assertThat(json, containsString("\"direction\""));
        assertThat(json, containsString("\"DESC\""));
    }
}
