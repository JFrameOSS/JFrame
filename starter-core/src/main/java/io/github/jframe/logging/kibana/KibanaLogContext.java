package io.github.jframe.logging.kibana;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;

import static java.util.Objects.nonNull;

/** Utility to copy the Kibana Log Fields. */
public final class KibanaLogContext {

    /** The MDC map. */
    private final Map<String, String> contextMap = new HashMap<>();

    /** Create a new instance, copying the MDC (context map). */
    public KibanaLogContext() {
        final Map<String, String> copyContextMap = MDC.getCopyOfContextMap();
        if (nonNull(copyContextMap)) {
            contextMap.putAll(copyContextMap);
        }
    }

    /**
     * Returns the copied context map.
     *
     * @return The context map.
     */
    public Map<String, String> getContextMap() {
        return contextMap;
    }

    /**
     * Registers the log fields of this {@link KibanaLogContext} into the KibanaLogFields.
     *
     * <p>See {@link KibanaLogFields#populateFromContext(KibanaLogContext)}.
     */
    public void registerKibanaLogFieldsInThisThread() {
        KibanaLogFields.populateFromContext(this);
    }
}
