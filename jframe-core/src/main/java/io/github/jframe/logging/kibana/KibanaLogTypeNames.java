package io.github.jframe.logging.kibana;

/**
 * Enumeration for values of the Kibana Log Field LOG_TYPE.
 */
public enum KibanaLogTypeNames {
    /** The start of a transaction. */
    START,
    /** The end of a transaction. */
    END,
    /** The request body. (incoming) */
    REQUEST_BODY,
    /** The response body. (returning) */
    RESPONSE_BODY,
    /** The start of a call to another system. */
    CALL_START,
    /** The call requests body. */
    CALL_REQUEST_BODY,
    /** The call response body. */
    CALL_RESPONSE_BODY,
    /** The end of a call to another system. */
    CALL_END
}
