package io.github.jframe.logging;

import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.support.UnitTest;
import reactor.core.publisher.Sinks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HttpLogger}.
 */
@DisplayName("Logging - HttpLogger")
class HttpLoggerTest extends UnitTest {

    @Mock
    private PasswordMasker passwordMasker;

    @Test
    @DisplayName("Should keep reactive response body intact when logging fails")
    void shouldKeepResponseBodyWhenLoggingFails() {
        // Given: A single-subscription response body (like Netty) and a masker that fails
        final Sinks.Many<DataBuffer> body = Sinks.many().unicast().onBackpressureBuffer();
        body.tryEmitNext(DefaultDataBufferFactory.sharedInstance.wrap("payload".getBytes(UTF_8)));
        body.tryEmitComplete();
        final ClientResponse response = ClientResponse.create(HttpStatus.OK).body(body.asFlux()).build();
        when(passwordMasker.maskPasswordsIn(anyString())).thenThrow(new IllegalStateException("boom"));

        // When: Logging the reactive response
        final ClientResponse logged = HttpLogger.logReactiveResponse(response, RESPONSE_LENGTH, passwordMasker).block();

        // Then: The downstream consumer still receives the body
        assertThat(logged.bodyToMono(String.class).block(), is("payload"));
    }
}
