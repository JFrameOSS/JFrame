package io.github.jframe.logging.filter;

import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

/** Utility for registering filter registration beans for servlet filters. */
@Slf4j
public final class FilterRegistrator {

    private static final EnumSet<DispatcherType> ALL_DISPATCHER_TYPES = EnumSet.allOf(DispatcherType.class);

    /** Utility constructor. */
    private FilterRegistrator() {
        // Do nothing.
    }

    /**
     * Helper method to wrap a filter in a {@link FilterRegistrationBean} with the configured order.
     *
     * @param filter      the filter.
     * @param filterOrder the filter's order.
     * @param <T>         the specific filter type.
     * @return the filter registration.
     */
    public static <T extends Filter> FilterRegistrationBean<T> register(final T filter, final int filterOrder) {
        return register(filter, filterOrder, ALL_DISPATCHER_TYPES);
    }

    /**
     * Helper method to wrap a filter in a {@link FilterRegistrationBean} with the configured order.
     *
     * @param filter          the filter
     * @param filterOrder     the filter's order.
     * @param dispatcherTypes the request dispatcher types the filter is used for
     * @param <T>             the specific filter type.
     * @return the filter registration.
     */
    public static <T extends Filter> FilterRegistrationBean<T> register(final T filter, final int filterOrder,
        final EnumSet<DispatcherType> dispatcherTypes) {
        log.trace("Setting filter order for '{}' to '{}'.", filter, filterOrder);
        final FilterRegistrationBean<T> result = new FilterRegistrationBean<>(filter);
        result.setOrder(filterOrder);
        result.setDispatcherTypes(dispatcherTypes);
        return result;
    }
}
