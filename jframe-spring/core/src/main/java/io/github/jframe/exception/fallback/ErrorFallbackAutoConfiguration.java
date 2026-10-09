package io.github.jframe.exception.fallback;

import io.github.jframe.exception.ExceptionConfiguration;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Bean;

/**
 * Replaces Boot's {@code BasicErrorController} with {@link JFrameErrorController}; runs before Boot's error auto-config.
 */
@AutoConfiguration(
    afterName = "io.github.jframe.autoconfigure.CoreAutoConfiguration",
    beforeName = "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration"
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(
    name = ExceptionConfiguration.ENABLED_PROPERTY,
    matchIfMissing = true
)
public class ErrorFallbackAutoConfiguration {

    /**
     * The jFrame {@code /error} controller; backs off for any application {@link ErrorController}.
     *
     * @param errorResponseEntityBuilder the builder
     * @return the controller
     */
    @Bean
    @ConditionalOnBean(ErrorResponseEntityBuilder.class)
    @ConditionalOnMissingBean(ErrorController.class)
    public JFrameErrorController jFrameErrorController(final ErrorResponseEntityBuilder errorResponseEntityBuilder) {
        return new JFrameErrorController(errorResponseEntityBuilder);
    }
}
