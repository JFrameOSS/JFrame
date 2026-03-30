package io.github.jframe.logging.interceptor;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.wrapper.BufferedClientHttpResponse;
import io.github.jframe.util.HttpRequestLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import static io.github.jframe.logging.ecs.CallResultTypes.FAILURE;
import static io.github.jframe.logging.ecs.CallResultTypes.TIMEOUT;
import static io.github.jframe.logging.ecs.EcsFieldNames.CALL_STATUS;
import static io.github.jframe.logging.ecs.EcsFieldNames.LOG_TYPE;
import static io.github.jframe.logging.ecs.LogTypeNames.CALL_END;

/**
 * A logging client http request interceptor.
 *
 * <p>This logs the input and output of each call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final RequestResponseLogger requestResponseLogger;

    @NonNull
    @Override
    public ClientHttpResponse intercept(
        @NonNull final HttpRequest request,
        @NonNull final byte[] body,
        @NonNull final ClientHttpRequestExecution execution) throws IOException {
        try (BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(execution.execute(request, body))) {
            if (HttpRequestLogging.isEnabled()) {
                requestResponseLogger.logRequest(request, body);
            }
            if (HttpRequestLogging.isEnabled()) {
                requestResponseLogger.logResponse(bufferedResponse);
            }
            return bufferedResponse;
        } catch (final IOException exception) {
            EcsFields.tag(CALL_STATUS, TIMEOUT);
            EcsFields.tag(LOG_TYPE, CALL_END);
            log.info("Got IO exception during call, most likely a timeout from backend.", exception);
            throw exception;
        } catch (final Exception exception) {
            EcsFields.tag(CALL_STATUS, FAILURE);
            EcsFields.tag(LOG_TYPE, CALL_END);
            log.info("Got exception during call, most likely a configuration issue.", exception);
            throw exception;
        } finally {
            EcsFields.clear(CALL_STATUS, LOG_TYPE);
        }
    }
}
