package io.github.jframe.exception.fallback;

import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import io.github.jframe.logging.model.TransactionId;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;

/**
 * Renders jFrame Problem Details for errors reaching the container {@code /error} page (filters, {@code sendError}).
 */
@Slf4j
@Hidden
@RestController
public class JFrameErrorController implements ErrorController {

    private final ErrorResponseEntityBuilder errorResponseEntityBuilder;

    /** The constructor. */
    public JFrameErrorController(final ErrorResponseEntityBuilder errorResponseEntityBuilder) {
        this.errorResponseEntityBuilder = errorResponseEntityBuilder;
    }

    /**
     * Builds the Problem Details body; never exposes the exception.
     *
     * @param request  the error dispatch request
     * @param response the error response
     * @return the error response
     */
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ErrorResponseResource> error(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpStatus status = status(request);
        final ErrorResponseResource body = errorResponseEntityBuilder.buildErrorResponseBody(
            null,
            status,
            new ServletWebRequest(request, response)
        );
        body.setInstance(originalPath(request));
        if (body.getTxId() == null) {
            body.setTxId(txId(request, response));
        }
        if (status.is5xxServerError()) {
            final Object throwable = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            log.error(
                "Error dispatch {} for {} (txId {})",
                status.value(),
                body.getInstance(),
                body.getTxId(),
                throwable instanceof final Throwable t ? t : null
            );
        }
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType(ProblemDetails.negotiateMediaType(accept(request)))).body(
            body
        );
    }

    private static HttpStatus status(final HttpServletRequest request) {
        final Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        final HttpStatus status = code instanceof final Integer value ? HttpStatus.resolve(value) : null;
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    /** Existing id from response header, request header or thread context; {@code null} when absent. */
    private static String txId(final HttpServletRequest request, final HttpServletResponse response) {
        final String responseHeader = response.getHeader(TX_ID_HEADER);
        final String requestHeader = request.getHeader(TX_ID_HEADER);
        final String txId;
        if (responseHeader != null && !responseHeader.isBlank()) {
            txId = responseHeader;
        } else if (requestHeader != null && !requestHeader.isBlank()) {
            txId = requestHeader;
        } else {
            txId = TransactionId.get();
        }
        return txId;
    }

    private static String originalPath(final HttpServletRequest request) {
        final Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return uri instanceof final String path ? path : request.getRequestURI();
    }

    private static String accept(final HttpServletRequest request) {
        final String accept = String.join(",", Collections.list(request.getHeaders(HttpHeaders.ACCEPT)));
        return accept.isEmpty() ? null : accept;
    }
}
