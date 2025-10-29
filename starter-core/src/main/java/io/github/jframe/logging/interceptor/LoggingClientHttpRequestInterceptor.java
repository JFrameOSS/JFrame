package io.github.jframe.logging.interceptor;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.util.HttpRequestLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import static io.github.jframe.logging.kibana.KibanaLogCallResultTypes.FAILURE;
import static io.github.jframe.logging.kibana.KibanaLogCallResultTypes.TIMEOUT;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.CALL_STATUS;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.LOG_TYPE;
import static io.github.jframe.logging.kibana.KibanaLogTypeNames.CALL_END;

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

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body, final ClientHttpRequestExecution execution)
        throws IOException {
        try {
            if (HttpRequestLogging.isEnabled()) {
                requestResponseLogger.logRequest(request, body);
            }
            final ClientHttpResponse response = execution.execute(request, body);
            if (HttpRequestLogging.isEnabled()) {
                requestResponseLogger.logResponse(response);
            }
            return response;
        } catch (final IOException exception) {
            KibanaLogFields.tag(CALL_STATUS, TIMEOUT);
            KibanaLogFields.tag(LOG_TYPE, CALL_END);
            log.info("Got IO exception during call, most likely a timeout from backend.", exception);
            throw exception;
        } catch (final Throwable throwable) {
            KibanaLogFields.tag(CALL_STATUS, FAILURE);
            KibanaLogFields.tag(LOG_TYPE, CALL_END);
            log.info("Got exception during call, most likely a configuration issue.", throwable);
            throw throwable;
        } finally {
            KibanaLogFields.clear(CALL_STATUS, LOG_TYPE);
        }
    }
}
