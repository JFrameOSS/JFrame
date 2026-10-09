package io.github.jframe.exception.resource;

import io.github.jframe.exception.ApiError;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * RFC 9457 Problem Details body of an error response, with jFrame extension members.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    {
        "type",
        "title",
        "status",
        "detail",
        "instance",
        "errorCode",
        "txId",
        "traceId",
        "spanId"
    }
)
public class ErrorResponseResource {

    private static final ClassValue<Set<String>> PROPERTY_NAMES = new ClassValue<>() {

        @Override
        protected Set<String> computeValue(final Class<?> type) {
            return propertyNames(type);
        }
    };

    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;

    private String errorCode;

    private String txId;
    private String traceId;
    private String spanId;

    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private final Map<String, Object> extensions = new LinkedHashMap<>();

    /** The throwable this resource was created for; never serialised. */
    @JsonIgnore
    @Setter(lombok.AccessLevel.NONE)
    private final Throwable throwable;

    /** Constructs a new {@code ErrorResponseResource} with no throwable. */
    public ErrorResponseResource() {
        this(null);
    }

    /** Constructs a new {@code ErrorResponseResource} with the given throwable. */
    public ErrorResponseResource(final Throwable throwable) {
        this.throwable = throwable;
    }

    /** Sets {@code errorCode} and {@code detail} from the given {@link ApiError}. */
    public void setError(final ApiError apiError) {
        this.errorCode = apiError.getErrorCode();
        this.detail = apiError.getReason();
    }

    /** Adds a top-level extension member; ignored when {@code null} or the name clashes with a typed property. */
    public void addExtension(final String name, final Object value) {
        if (value != null && !PROPERTY_NAMES.get(getClass()).contains(name)) {
            extensions.put(name, value);
        }
    }

    private static Set<String> propertyNames(final Class<?> type) {
        final Set<String> names = new HashSet<>();
        for (final Method method : type.getMethods()) {
            final String name = method.getName();
            final int prefix = name.startsWith("get") ? 3 : name.startsWith("is") ? 2 : 0;
            if (prefix > 0 && name.length() > prefix && method.getParameterCount() == 0
                && !method.isAnnotationPresent(JsonIgnore.class)) {
                names.add(Character.toLowerCase(name.charAt(prefix)) + name.substring(prefix + 1));
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /** Returns the custom extension members, serialised at top level. */
    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, SHORT_PREFIX_STYLE).setExcludeFieldNames("throwable").toString();
    }
}
