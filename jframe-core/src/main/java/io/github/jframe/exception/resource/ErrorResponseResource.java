package io.github.jframe.exception.resource;

import io.github.jframe.exception.ApiError;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * This class represents the body of an error response.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseResource {

    private int statusCode;

    private String method;
    private String uri;
    private String query;
    private String contentType;

    private String errorCode;
    private String errorReason;

    /** String representation of the exception cause, if present. */
    private String cause;

    private String txId;
    private String traceId;
    private String spanId;

    /** The throwable this resource was created for. */
    @JsonIgnore
    private final Throwable throwable;

    /** Constructs a new {@code ErrorResponseResource} with no throwable. */
    public ErrorResponseResource() {
        this(null);
    }

    /** Constructs a new {@code ErrorResponseResource} with the given throwable. */
    public ErrorResponseResource(final Throwable throwable) {
        this.throwable = throwable;
        this.cause = extractMessage(throwable);
    }

    private static String extractMessage(final Throwable throwable) {
        String message = null;
        if (throwable != null) {
            try {
                message = throwable.getMessage();
            } catch (final Exception ignored) {
                message = throwable.getClass().getSimpleName();
            }
        }
        return message;
    }

    /** Sets {@code errorCode} and {@code errorReason} from the given {@link ApiError}. */
    public void setError(final ApiError apiError) {
        this.errorCode = apiError.getErrorCode();
        this.errorReason = apiError.getReason();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
