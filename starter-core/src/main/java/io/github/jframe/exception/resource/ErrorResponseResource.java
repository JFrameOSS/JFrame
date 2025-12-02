package io.github.jframe.exception.resource;

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

    private String method;
    private String uri;
    private String query;
    private String contentType;
    private int statusCode;
    private String statusMessage;
    private String errorMessage;
    private String txId;
    private String traceId;
    private String spanId;


    /** The throwable this resource was created for. */
    @JsonIgnore
    private final Throwable throwable;

    /** Default constructor. */
    public ErrorResponseResource() {
        this(null);
    }

    /**
     * Construct an error resource with a throwable.
     *
     * @param throwable the throwable
     */
    public ErrorResponseResource(final Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
