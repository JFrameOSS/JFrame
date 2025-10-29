package io.github.jframe.logging.voter;

import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MediaTypeVoter}.
 *
 * <p>Verifies the MediaTypeVoter functionality including:
 * <ul>
 * <li>Matching allowed media types</li>
 * <li>Matching wildcard media types</li>
 * <li>Handling null and empty content types</li>
 * <li>Handling invalid media type strings</li>
 * <li>matchIfEmpty behavior</li>
 * </ul>
 */
@DisplayName("Logging - MediaTypeVoter")
class MediaTypeVoterTest extends UnitTest {

    @Test
    @DisplayName("Should match when media type is in allowed list")
    void mediaTypeMatches_withAllowedMediaType_shouldReturnTrue() {
        // Given: A voter configured with JSON media type
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if JSON media type matches
        final boolean matches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);

        // Then: Match is found
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should match when media type string is in allowed list")
    void mediaTypeMatches_withAllowedMediaTypeString_shouldReturnTrue() {
        // Given: A voter configured with JSON media type
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if JSON media type string matches
        final boolean matches = voter.mediaTypeMatches("application/json");

        // Then: Match is found
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match when media type is not in allowed list")
    void mediaTypeMatches_withDisallowedMediaType_shouldReturnFalse() {
        // Given: A voter configured with only JSON media type
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if XML media type matches
        final boolean matches = voter.mediaTypeMatches(MediaType.APPLICATION_XML);

        // Then: No match is found
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match when wildcard media type includes specific type")
    void mediaTypeMatches_withWildcardMediaType_shouldMatchSpecificTypes() {
        // Given: A voter configured with wildcard application/* media type
        final List<MediaType> allowedTypes = Collections.singletonList(new MediaType("application", "*"));
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if specific application types match
        final boolean jsonMatches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);
        final boolean xmlMatches = voter.mediaTypeMatches(MediaType.APPLICATION_XML);
        final boolean textMatches = voter.mediaTypeMatches(MediaType.TEXT_PLAIN);

        // Then: Application types match, but text type does not
        assertThat(jsonMatches).isTrue();
        assertThat(xmlMatches).isTrue();
        assertThat(textMatches).isFalse();
    }

    @Test
    @DisplayName("Should match when media type with charset matches base type")
    void mediaTypeMatches_withMediaTypeWithCharset_shouldMatchBaseType() {
        // Given: A voter configured with JSON media type
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if JSON with UTF-8 charset matches
        final boolean matches = voter.mediaTypeMatches("application/json;charset=UTF-8");

        // Then: Match is found despite charset parameter
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should match any of multiple allowed media types")
    void mediaTypeMatches_withMultipleAllowedTypes_shouldMatchAny() {
        // Given: A voter configured with multiple media types
        final List<MediaType> allowedTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.TEXT_PLAIN
        );
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if various media types match
        final boolean jsonMatches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);
        final boolean xmlMatches = voter.mediaTypeMatches(MediaType.APPLICATION_XML);
        final boolean textMatches = voter.mediaTypeMatches(MediaType.TEXT_PLAIN);
        final boolean htmlMatches = voter.mediaTypeMatches(MediaType.TEXT_HTML);

        // Then: Configured types match, unconfigured type does not
        assertThat(jsonMatches).isTrue();
        assertThat(xmlMatches).isTrue();
        assertThat(textMatches).isTrue();
        assertThat(htmlMatches).isFalse();
    }

    @Test
    @DisplayName("Should return matchIfEmpty when media type is null and matchIfEmpty is true")
    void mediaTypeMatches_withNullMediaTypeAndMatchIfEmptyTrue_shouldReturnTrue() {
        // Given: A voter with matchIfEmpty set to true
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, true);

        // When: Checking if null media type matches
        final boolean matches = voter.mediaTypeMatches((MediaType) null);

        // Then: Match is found because matchIfEmpty is true
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should return false when media type is null and matchIfEmpty is false")
    void mediaTypeMatches_withNullMediaTypeAndMatchIfEmptyFalse_shouldReturnFalse() {
        // Given: A voter with matchIfEmpty set to false
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if null media type matches
        final boolean matches = voter.mediaTypeMatches((MediaType) null);

        // Then: No match is found because matchIfEmpty is false
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should return matchIfEmpty when allowed types list is empty and matchIfEmpty is true")
    void mediaTypeMatches_withEmptyAllowedListAndMatchIfEmptyTrue_shouldReturnTrue() {
        // Given: A voter with empty allowed types and matchIfEmpty set to true
        final MediaTypeVoter voter = new MediaTypeVoter(new ArrayList<>(), true);

        // When: Checking if any media type matches
        final boolean matches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);

        // Then: Match is found because matchIfEmpty is true
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should return false when allowed types list is empty and matchIfEmpty is false")
    void mediaTypeMatches_withEmptyAllowedListAndMatchIfEmptyFalse_shouldReturnFalse() {
        // Given: A voter with empty allowed types and matchIfEmpty set to false
        final MediaTypeVoter voter = new MediaTypeVoter(new ArrayList<>(), false);

        // When: Checking if any media type matches
        final boolean matches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);

        // Then: No match is found because matchIfEmpty is false
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should return matchIfEmpty when allowed types list is null and matchIfEmpty is true")
    void mediaTypeMatches_withNullAllowedListAndMatchIfEmptyTrue_shouldReturnTrue() {
        // Given: A voter with null allowed types and matchIfEmpty set to true
        final MediaTypeVoter voter = new MediaTypeVoter(null, true);

        // When: Checking if any media type matches
        final boolean matches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);

        // Then: Match is found because matchIfEmpty is true
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should return false when allowed types list is null and matchIfEmpty is false")
    void mediaTypeMatches_withNullAllowedListAndMatchIfEmptyFalse_shouldReturnFalse() {
        // Given: A voter with null allowed types and matchIfEmpty set to false
        final MediaTypeVoter voter = new MediaTypeVoter(null, false);

        // When: Checking if any media type matches
        final boolean matches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);

        // Then: No match is found because matchIfEmpty is false
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should return false when media type string is invalid")
    void mediaTypeMatches_withInvalidMediaTypeString_shouldReturnFalse() {
        // Given: A voter configured with JSON media type
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if invalid media type string matches
        final boolean matches = voter.mediaTypeMatches("invalid-media-type");

        // Then: No match is found due to invalid format
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should return matchIfEmpty when media type string is blank and matchIfEmpty is true")
    void mediaTypeMatches_withBlankMediaTypeStringAndMatchIfEmptyTrue_shouldReturnTrue() {
        // Given: A voter with matchIfEmpty set to true
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, true);

        // When: Checking if blank media type string matches
        final boolean matches = voter.mediaTypeMatches("   ");

        // Then: Match is found because matchIfEmpty is true
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should return false when media type string is blank and matchIfEmpty is false")
    void mediaTypeMatches_withBlankMediaTypeStringAndMatchIfEmptyFalse_shouldReturnFalse() {
        // Given: A voter with matchIfEmpty set to false
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if blank media type string matches
        final boolean matches = voter.mediaTypeMatches("   ");

        // Then: No match is found because matchIfEmpty is false
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match case-insensitively for media type")
    void mediaTypeMatches_withDifferentCase_shouldMatchCaseInsensitively() {
        // Given: A voter configured with JSON media type
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if media type with different case matches
        final boolean matches = voter.mediaTypeMatches("APPLICATION/JSON");

        // Then: Match is found regardless of case
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should match all types when wildcard */* is configured")
    void mediaTypeMatches_withAllWildcard_shouldMatchAllTypes() {
        // Given: A voter configured with */* wildcard
        final List<MediaType> allowedTypes = Collections.singletonList(MediaType.ALL);
        final MediaTypeVoter voter = new MediaTypeVoter(allowedTypes, false);

        // When: Checking if various media types match
        final boolean jsonMatches = voter.mediaTypeMatches(MediaType.APPLICATION_JSON);
        final boolean xmlMatches = voter.mediaTypeMatches(MediaType.APPLICATION_XML);
        final boolean textMatches = voter.mediaTypeMatches(MediaType.TEXT_PLAIN);

        // Then: All media types match
        assertThat(jsonMatches).isTrue();
        assertThat(xmlMatches).isTrue();
        assertThat(textMatches).isTrue();
    }
}
