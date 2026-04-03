package io.github.jframe.autoconfigure;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.model.PathDefinition;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.voter.RequestVoter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI producer that creates a {@link FilterVoter} bean configured with the
 * allowed content types and exclude paths from {@link LoggingConfig}.
 *
 * <p>Quarkus equivalent of Spring's {@code CoreAutoConfiguration.filterVoter()}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class FilterVoterProducer {

    private final LoggingConfig loggingConfig;

    /**
     * Produces a {@link FilterVoter} configured with the allowed content types and
     * exclude paths from {@link LoggingConfig}.
     *
     * @return the configured {@link FilterVoter}
     */
    @Produces
    @ApplicationScoped
    public FilterVoter filterVoter() {
        final List<String> allowedContentTypes = loggingConfig.allowedContentTypes();
        final List<String> excludePaths = loggingConfig.excludePaths();

        final MediaTypeVoter mediaTypeVoter = new MediaTypeVoter(allowedContentTypes, true);
        final RequestVoter requestVoter = new RequestVoter(
            excludePaths.stream().map(PathDefinition::new).toList()
        );

        return new FilterVoter(mediaTypeVoter, requestVoter);
    }
}
