package io.github.jframe.autoconfigure.factory;

import java.util.Properties;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import static java.util.Objects.*;
import static org.springframework.beans.factory.config.YamlProcessor.ResolutionMethod.OVERRIDE_AND_IGNORE;

/**
 * Factory to load YAML property sources with proper property resolution.
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @NonNull
    @Override
    public PropertySource<?> createPropertySource(@Nullable final String name, final EncodedResource encodedResource) {
        final YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());
        factory.setResolutionMethod(OVERRIDE_AND_IGNORE);

        final Properties properties = factory.getObject();
        if (isNull(properties)) {
            throw new IllegalStateException("Failed to load properties from " + encodedResource.getResource());
        }

        final String sourceName = nonNull(name)
            ? name
            : requireNonNull(encodedResource.getResource().getFilename());

        return new PropertiesPropertySource(sourceName, properties);
    }
}
