package io.github.jframe;

import lombok.experimental.UtilityClass;

/**
 * Container class for OpenTelemetry constants organized by category.
 */
public final class OpenTelemetryConstants {

    private OpenTelemetryConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Constants related to logging.
     */
    @UtilityClass
    public static final class Logging {

        public static final String REQUEST_PREFIX = "[EXTERNAL] Outbound request is:";
        public static final String RESPONSE_PREFIX = "[EXTERNAL] Incoming response is:";
        public static final String LINE_BREAK = "\n";
        public static final String TAB = "\t";

    }


    /**
     * Constants for OpenTelemetry attribute keys.
     */
    @UtilityClass
    public static final class Attributes {

        // Application attributes
        public static final String SERVICE_NAME = "service.name";
        public static final String SERVICE_METHOD = "service.method";
        public static final String EXCLUDE_TRACING = "otel.exclude";

        // API Request attributes
        public static final String HTTP_REMOTE_USER = "http.remote_user";
        public static final String HTTP_TRANSACTION_ID = "http.transaction_id";
        public static final String HTTP_REQUEST_ID = "http.request_id";
        public static final String HTTP_URI = "http.uri";
        public static final String HTTP_QUERY = "http.query";
        public static final String HTTP_METHOD = "http.method";
        public static final String HTTP_STATUS_CODE = "http.status_code";
        public static final String HTTP_CONTENT_TYPE = "http.content_type";
        public static final String HTTP_CONTENT_LENGTH = "http.content_length";

        // External request attributes
        public static final String PEER_SERVICE = "peer.service";
        public static final String TRACING_SPAN = "tracing.span";
        public static final String EXT_REQUEST_URI = "ext.request.uri";
        public static final String EXT_REQUEST_QUERY = "ext.request.query";
        public static final String EXT_REQUEST_METHOD = "ext.request.method";

        // External response attributes
        public static final String EXT_RESPONSE_STATUS_CODE = "ext.response.status_code";
        public static final String EXT_RESPONSE_CONTENT_LENGTH = "ext.response.content_length";
        public static final String EXT_RESPONSE_CONTENT_TYPE = "ext.response.content_type";
        public static final String EXT_RESPONSE_L7_REQUEST_ID = "ext.response.l7_request_id";

        // Error attributes
        public static final String ERROR = "error";
        public static final String ERROR_TYPE = "error.type";
        public static final String ERROR_MESSAGE = "error.message";
    }
}
