package io.github.jframe.autoconfigure;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilterVoterProducer}.
 *
 * <p>Verifies that the CDI producer creates a {@link FilterVoter} configured with
 * exclude paths and allowed content types from {@link LoggingConfig}.
 */
@DisplayName("Autoconfigure - FilterVoterProducer")
public class FilterVoterProducerTest extends UnitTest {

    @Mock
    private LoggingConfig loggingConfig;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    @Test
    @DisplayName("Should produce a non-null FilterVoter when LoggingConfig is provided")
    public void shouldProduceNonNullFilterVoterWhenLoggingConfigProvided() {
        // Given: A LoggingConfig with allowed content types and exclude paths configured
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.excludePaths()).thenReturn(List.of("/actuator/*"));
        final FilterVoterProducer producer = new FilterVoterProducer(loggingConfig);

        // When: Producing the FilterVoter
        final FilterVoter filterVoter = producer.filterVoter();

        // Then: A non-null FilterVoter is produced
        assertThat(filterVoter, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create FilterVoter that excludes configured paths")
    public void shouldCreateFilterVoterWithConfiguredExcludePaths() {
        // Given: A LoggingConfig excluding /actuator/* and /health, allowing application/json
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.excludePaths()).thenReturn(List.of("/actuator/*", "/health"));
        final FilterVoterProducer producer = new FilterVoterProducer(loggingConfig);
        final FilterVoter filterVoter = producer.filterVoter();

        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/actuator/health");

        // When: Checking if filter is enabled for an excluded path
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter should be disabled because /actuator/health matches the exclusion pattern
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should create FilterVoter that allows requests matching configured content types")
    public void shouldCreateFilterVoterWithConfiguredAllowedContentTypes() {
        // Given: A LoggingConfig allowing only application/json and no excluded paths
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.excludePaths()).thenReturn(Collections.emptyList());
        final FilterVoterProducer producer = new FilterVoterProducer(loggingConfig);
        final FilterVoter filterVoter = producer.filterVoter();

        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/orders");

        // When: Checking if filter is enabled for an application/json request
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter should be enabled because content type matches and path is not excluded
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should create FilterVoter that rejects requests with non-allowed content types")
    public void shouldCreateFilterVoterThatRejectsNonAllowedContentTypes() {
        // Given: A LoggingConfig allowing only application/json and no excluded paths
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.excludePaths()).thenReturn(Collections.emptyList());
        final FilterVoterProducer producer = new FilterVoterProducer(loggingConfig);
        final FilterVoter filterVoter = producer.filterVoter();

        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_HTML_TYPE);
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/users");

        // When: Checking if filter is enabled for a text/html request
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter should be disabled because text/html is not in the allowed types
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should produce FilterVoter that allows all paths when exclude paths list is empty")
    public void shouldProduceFilterVoterThatAllowsAllPathsWhenExcludePathsListIsEmpty() {
        // Given: A LoggingConfig with empty exclude paths list
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.excludePaths()).thenReturn(Collections.emptyList());
        final FilterVoterProducer producer = new FilterVoterProducer(loggingConfig);
        final FilterVoter filterVoter = producer.filterVoter();

        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/actuator/health");

        // When: Checking if filter is enabled for any path
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter should be enabled because no paths are excluded
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should produce FilterVoter that allows requests when allowed content types list is empty (matchIfEmpty=true)")
    public void shouldProduceFilterVoterThatAllowsRequestsWhenAllowedContentTypesListIsEmpty() {
        // Given: A LoggingConfig with empty allowed content types list
        when(loggingConfig.allowedContentTypes()).thenReturn(Collections.emptyList());
        when(loggingConfig.excludePaths()).thenReturn(Collections.emptyList());
        final FilterVoterProducer producer = new FilterVoterProducer(loggingConfig);
        final FilterVoter filterVoter = producer.filterVoter();

        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/users");

        // When: Checking if filter is enabled with no allowed content types
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter should be enabled (matchIfEmpty=true matches Spring parity)
        assertThat(result, is(true));
    }
}
