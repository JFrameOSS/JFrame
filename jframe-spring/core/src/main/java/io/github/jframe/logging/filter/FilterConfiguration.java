package io.github.jframe.logging.filter;

import io.github.jframe.logging.filter.config.RequestDurationFilterConfiguration;
import io.github.jframe.logging.filter.config.RequestIdFilterConfiguration;
import io.github.jframe.logging.filter.config.RequestResponseLogFilterConfiguration;
import io.github.jframe.logging.filter.config.TransactionIdFilterConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Configuration that includes all filter configurations. */
@Configuration
@Import(
    {
        RequestDurationFilterConfiguration.class,
        RequestIdFilterConfiguration.class,
        RequestResponseLogFilterConfiguration.class,
        TransactionIdFilterConfiguration.class
    }
)
public class FilterConfiguration {}
