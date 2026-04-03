package io.github.jframe.logging.ecs;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;

import static java.util.Objects.nonNull;

/** Utility to copy the ECS Log Fields. */
@Getter
public final class MdcLogContext {

    private final Map<String, String> contextMap = new HashMap<>();

    /** Create a new instance, copying the MDC (context map). */
    public MdcLogContext() {
        final Map<String, String> copyContextMap = MDC.getCopyOfContextMap();
        if (nonNull(copyContextMap)) {
            contextMap.putAll(copyContextMap);
        }
    }

    /**
     * Registers the log fields of this {@link MdcLogContext} into the EcsFields.
     *
     * <p>See {@link EcsFields#populateFromContext(MdcLogContext)}.
     */
    public void registerEcsFieldsInThisThread() {
        EcsFields.populateFromContext(this);
    }
}
