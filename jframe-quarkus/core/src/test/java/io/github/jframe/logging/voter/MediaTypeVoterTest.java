package io.github.jframe.logging.voter;

import io.github.support.UnitTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link MediaTypeVoter}.
 *
 * <p>Verifies MediaTypeVoter decision logic for content type matching including:
 * <ul>
 * <li>Exact content type matching</li>
 * <li>Case-insensitive matching</li>
 * <li>Wildcard type matching (e.g. {@code application/*})</li>
 * <li>Charset parameter stripping (e.g. {@code application/json;charset=UTF-8})</li>
 * <li>Null / blank / empty fallback via {@code matchIfEmpty}</li>
 * <li>Invalid / malformed content type graceful handling</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Voters - MediaTypeVoter")
public class MediaTypeVoterTest extends UnitTest {

    // ---------------------------------------------------------------------------
    // Exact matching
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when content type is in allowed list")
    public void shouldReturnTrueWhenContentTypeIsInAllowedList() {
        // Given: A voter configured with JSON content type
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if application/json matches
        final boolean result = voter.matches("application/json");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when content type is not in allowed list")
    public void shouldReturnFalseWhenContentTypeIsNotInAllowedList() {
        // Given: A voter configured with only JSON content type
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if application/xml matches
        final boolean result = voter.matches("application/xml");

        // Then: No match is found
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true when content type matches any of multiple allowed types")
    public void shouldReturnTrueWhenContentTypeMatchesAnyOfMultipleAllowedTypes() {
        // Given: A voter configured with JSON, XML, and plain-text content types
        final List<String> allowedTypes = Arrays.asList("application/json", "application/xml", "text/plain");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking each of the allowed types
        final boolean jsonMatches = voter.matches("application/json");
        final boolean xmlMatches = voter.matches("application/xml");
        final boolean textMatches = voter.matches("text/plain");
        final boolean htmlMatches = voter.matches("text/html");

        // Then: Configured types match, unconfigured type does not
        assertThat(jsonMatches, is(true));
        assertThat(xmlMatches, is(true));
        assertThat(textMatches, is(true));
        assertThat(htmlMatches, is(false));
    }

    // ---------------------------------------------------------------------------
    // Case-insensitive matching
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when content type matches case-insensitively")
    public void shouldReturnTrueWhenContentTypeMatchesCaseInsensitively() {
        // Given: A voter configured with lower-case JSON content type
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking with upper-case variant
        final boolean result = voter.matches("APPLICATION/JSON");

        // Then: Match is found regardless of case
        assertThat(result, is(true));
    }

    // ---------------------------------------------------------------------------
    // Charset / parameter stripping
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when content type has charset parameter matching allowed base type")
    public void shouldReturnTrueWhenContentTypeHasCharsetParameterMatchingAllowedBaseType() {
        // Given: A voter configured with plain JSON (no parameters)
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking content type with charset parameter
        final boolean result = voter.matches("application/json;charset=UTF-8");

        // Then: Match is found because base type matches after stripping parameters
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true when content type has multiple parameters matching allowed base type")
    public void shouldReturnTrueWhenContentTypeHasMultipleParametersMatchingAllowedBaseType() {
        // Given: A voter configured with plain JSON
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking content type with multiple parameters
        final boolean result = voter.matches("application/json; charset=UTF-8; boundary=something");

        // Then: Match is found after stripping all parameters
        assertThat(result, is(true));
    }

    // ---------------------------------------------------------------------------
    // Wildcard matching
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when wildcard subtype application/* matches application/json")
    public void shouldReturnTrueWhenWildcardSubtypeMatchesApplicationJson() {
        // Given: A voter configured with application/* wildcard
        final List<String> allowedTypes = Collections.singletonList("application/*");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking specific application subtypes
        final boolean jsonMatches = voter.matches("application/json");
        final boolean xmlMatches = voter.matches("application/xml");
        final boolean textMatches = voter.matches("text/plain");

        // Then: Application subtypes match; text type does not
        assertThat(jsonMatches, is(true));
        assertThat(xmlMatches, is(true));
        assertThat(textMatches, is(false));
    }

    @Test
    @DisplayName("Should return true when wildcard */* matches any content type")
    public void shouldReturnTrueWhenFullWildcardMatchesAnyContentType() {
        // Given: A voter configured with */* wildcard
        final List<String> allowedTypes = Collections.singletonList("*/*");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking various content types
        final boolean jsonMatches = voter.matches("application/json");
        final boolean xmlMatches = voter.matches("application/xml");
        final boolean textMatches = voter.matches("text/plain");

        // Then: All content types match
        assertThat(jsonMatches, is(true));
        assertThat(xmlMatches, is(true));
        assertThat(textMatches, is(true));
    }

    // ---------------------------------------------------------------------------
    // matchIfEmpty — null contentType
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return matchIfEmpty when content type is null and matchIfEmpty is true")
    public void shouldReturnMatchIfEmptyWhenContentTypeIsNullAndMatchIfEmptyIsTrue() {
        // Given: A voter with matchIfEmpty = true
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, true);

        // When: Checking null content type
        final boolean result = voter.matches(null);

        // Then: matchIfEmpty value (true) is returned
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when content type is null and matchIfEmpty is false")
    public void shouldReturnFalseWhenContentTypeIsNullAndMatchIfEmptyIsFalse() {
        // Given: A voter with matchIfEmpty = false
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking null content type
        final boolean result = voter.matches(null);

        // Then: matchIfEmpty value (false) is returned
        assertThat(result, is(false));
    }

    // ---------------------------------------------------------------------------
    // matchIfEmpty — blank contentType
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return matchIfEmpty when content type is blank and matchIfEmpty is true")
    public void shouldReturnMatchIfEmptyWhenContentTypeIsBlankAndMatchIfEmptyIsTrue() {
        // Given: A voter with matchIfEmpty = true
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, true);

        // When: Checking blank content type
        final boolean result = voter.matches("   ");

        // Then: matchIfEmpty value (true) is returned
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when content type is blank and matchIfEmpty is false")
    public void shouldReturnFalseWhenContentTypeIsBlankAndMatchIfEmptyIsFalse() {
        // Given: A voter with matchIfEmpty = false
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking blank content type
        final boolean result = voter.matches("   ");

        // Then: matchIfEmpty value (false) is returned
        assertThat(result, is(false));
    }

    // ---------------------------------------------------------------------------
    // matchIfEmpty — empty allowed list
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return matchIfEmpty when allowed list is empty and matchIfEmpty is true")
    public void shouldReturnMatchIfEmptyWhenAllowedListIsEmptyAndMatchIfEmptyIsTrue() {
        // Given: A voter with empty allowed list and matchIfEmpty = true
        final MediaTypeVoter voter = new MediaTypeVoter(Collections.emptyList(), true);

        // When: Checking any content type
        final boolean result = voter.matches("application/json");

        // Then: matchIfEmpty value (true) is returned because list is empty
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when allowed list is empty and matchIfEmpty is false")
    public void shouldReturnFalseWhenAllowedListIsEmptyAndMatchIfEmptyIsFalse() {
        // Given: A voter with empty allowed list and matchIfEmpty = false
        final MediaTypeVoter voter = new MediaTypeVoter(Collections.emptyList(), false);

        // When: Checking any content type
        final boolean result = voter.matches("application/json");

        // Then: matchIfEmpty value (false) is returned because list is empty
        assertThat(result, is(false));
    }

    // ---------------------------------------------------------------------------
    // matchIfEmpty — null allowed list
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return matchIfEmpty when allowed list is null and matchIfEmpty is true")
    public void shouldReturnMatchIfEmptyWhenAllowedListIsNullAndMatchIfEmptyIsTrue() {
        // Given: A voter with null allowed list and matchIfEmpty = true
        final MediaTypeVoter voter = new MediaTypeVoter(null, true);

        // When: Checking any content type
        final boolean result = voter.matches("application/json");

        // Then: matchIfEmpty value (true) is returned because list is null
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when allowed list is null and matchIfEmpty is false")
    public void shouldReturnFalseWhenAllowedListIsNullAndMatchIfEmptyIsFalse() {
        // Given: A voter with null allowed list and matchIfEmpty = false
        final MediaTypeVoter voter = new MediaTypeVoter(null, false);

        // When: Checking any content type
        final boolean result = voter.matches("application/json");

        // Then: matchIfEmpty value (false) is returned because list is null
        assertThat(result, is(false));
    }

    // ---------------------------------------------------------------------------
    // Invalid / malformed content type
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return false when content type string is malformed and does not match")
    public void shouldReturnFalseWhenContentTypeStringIsMalformedAndDoesNotMatch() {
        // Given: A voter configured with JSON content type
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking a malformed content type string
        final boolean result = voter.matches("invalid-media-type");

        // Then: No match is found and no exception is thrown
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return false when content type is empty string and matchIfEmpty is false")
    public void shouldReturnFalseWhenContentTypeIsEmptyStringAndMatchIfEmptyIsFalse() {
        // Given: A voter with matchIfEmpty = false
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking empty string content type
        final boolean result = voter.matches("");

        // Then: matchIfEmpty value (false) is returned
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true when content type is empty string and matchIfEmpty is true")
    public void shouldReturnTrueWhenContentTypeIsEmptyStringAndMatchIfEmptyIsTrue() {
        // Given: A voter with matchIfEmpty = true
        final List<String> allowedTypes = Collections.singletonList("application/json");
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, true);

        // When: Checking empty string content type
        final boolean result = voter.matches("");

        // Then: matchIfEmpty value (true) is returned
        assertThat(result, is(true));
    }
}
